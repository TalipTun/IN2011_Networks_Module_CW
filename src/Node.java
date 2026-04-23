// IN2011 Computer Networks
// Coursework 2024/2025
//
// Submission by
//  Student Name : Talip Tun
//  Student Number : 240014310
//  Student Email : Talip.Tun@city.ac.uk


// DO NOT EDIT starts
// This gives the interface that your code must implement.
// These descriptions are intended to help you understand how the interface
// will be used. See the RFC for how the protocol works.
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

interface NodeInterface {
    /* These methods configure your node.
     * They must both be called once after the node has been created but
     * before it is used. */
    
    // Set the name of the node.
    public void setNodeName(String nodeName) throws Exception;

    // Open a UDP port for sending and receiving messages.
    public void openPort(int portNumber) throws Exception;


    /*
     * These methods query and change how the network is used.
     */

    // Handle all incoming messages.
    // If you wait for more than delay miliseconds and
    // there are no new incoming messages return.
    // If delay is zero then wait for an unlimited amount of time.
    public void handleIncomingMessages(int delay) throws Exception;
    
    // Determines if a node can be contacted and is responding correctly.
    // Handles any messages that have arrived.
    public boolean isActive(String nodeName) throws Exception;

    // You need to keep a stack of nodes that are used to relay messages.
    // The base of the stack is the first node to be used as a relay.
    // The first node must relay to the second node and so on.
    
    // Adds a node name to a stack of nodes used to relay all future messages.
    public void pushRelay(String nodeName) throws Exception;

    // Pops the top entry from the stack of nodes used for relaying.
    // No effect if the stack is empty
    public void popRelay() throws Exception;
    

    /*
     * These methods provide access to the basic functionality of
     * CRN-25 network.
     */

    // Checks if there is an entry in the network with the given key.
    // Handles any messages that have arrived.
    public boolean exists(String key) throws Exception;
    
    // Reads the entry stored in the network for key.
    // If there is a value, return it.
    // If there isn't a value, return null.
    // Handles any messages that have arrived.
    public String read(String key) throws Exception;

    // Sets key to be value.
    // Returns true if it worked, false if it didn't.
    // Handles any messages that have arrived.
    public boolean write(String key, String value) throws Exception;

    // If key is set to currentValue change it to newValue.
    // Returns true if it worked, false if it didn't.
    // Handles any messages that have arrived.
    public boolean CAS(String key, String currentValue, String newValue) throws Exception;

}
// DO NOT EDIT ends

// Complete this!
public class Node implements NodeInterface {
    static final HashMap<String, String> nodeMap = new HashMap<>();
    boolean isAddress;
    String key;
    String value;
    String hashedID;
    DatagramSocket socket;
    HashSet<Node> knownNodes = new HashSet<>(); 

    //pq should store Node and Distance
    PriorityQueue<NodeInfo> pq = new PriorityQueue<>(
        3, (a, b) -> Integer.compare(b.getDistance(), a.getDistance())
    );
    Deque<String> relayStack = new ArrayDeque<>();

    public Node() {}

    public String getNodeName() {
        return key;
    }

    public String getNodeValue() {
        return value;
    }

    public String getNodeHashedID() {
        return hashedID;
    }

    public HashSet<Node> getKnownNodes() {
        return knownNodes;
    }

    public DatagramSocket getDatagramSocket() {
        return socket;
    }

    static class RelayContext {
        InetAddress originalAddr;
        int originalPort;
        byte outerTx1;
        byte outerTx2;
        long createdAtMs;

        RelayContext(InetAddress originalAddr, int originalPort, byte outerTx1, byte outerTx2) {
            this.originalAddr = originalAddr;
            this.originalPort = originalPort;
            this.outerTx1 = outerTx1;
            this.outerTx2 = outerTx2;
            this.createdAtMs = System.currentTimeMillis();
        }
    }

    Map<Integer, RelayContext> relayPending = new HashMap<>();

    static class ParsedCrnString {
        String value;
        int nextOffset;

        ParsedCrnString(String value, int nextOffset) {
            this.value = value;
            this.nextOffset = nextOffset;
        }
    }

    static class RoutedMessage {
        InetAddress targetAddress;
        int targetPort;
        byte[] bytes;

        RoutedMessage(InetAddress targetAddress, int targetPort, byte[] bytes) {
            this.targetAddress = targetAddress;
            this.targetPort = targetPort;
            this.bytes = bytes;
        }
    }

    HashMap<String, String> dataStore = new HashMap<>();

    // A node MUST store at most three address key/value pairs for each distance.
    // If more than three exist at the same distance, the node MUST keep only three and SHOULD
    // prefer stable nodes.

    // FOR ADDRESS KEY/VALUE PAIRS
    public void setNodeName(String nodeName) throws Exception {
        try {
            if (!nodeMap.containsKey(nodeName)) {
                nodeMap.put(nodeName, "");
                this.key = nodeName;
                this.hashedID = HashID.getHashedId(key);
            } else {
                throw new Exception("Name already exists");
            }
        } catch (Exception e) {
            throw new Exception("Not implemented" + e);
        }
    }

    // FOR ADDRESS KEY/VALUE PAIRS
    public void openPort(int portNumber) throws Exception {
        try {
            socket = new DatagramSocket(portNumber); 
            String address = InetAddress.getByName("localhost").getHostAddress(); 
            this.value = "" + address + ":" + portNumber;
            applyAddressStoragePolicy(key, value);
        } catch (Exception e) {
            throw new Exception("Port could not be opened: " + e);
        }
    }

    public void handleIncomingMessages(int delay) throws Exception {
        long end = System.currentTimeMillis() + delay;

        while (delay == 0 || System.currentTimeMillis() < end) {
            long remaining = end - System.currentTimeMillis();
            if (delay != 0 && remaining <= 0) {
                break;
            }

            socket.setSoTimeout(delay == 0 ? 0 : (int) remaining);

            byte[] buf = new byte[65535];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            try {
                socket.receive(packet);

                System.out.println("len=" + packet.getLength());
                System.out.println("from=" + packet.getAddress() + ":" + packet.getPort());
                System.out.println("raw=" + Arrays.toString(Arrays.copyOf(packet.getData(), packet.getLength())));

                // parse + dispatch packet here
                ByteBuffer bb = ByteBuffer.wrap(buf, 0, packet.getLength());
                bb.order(ByteOrder.BIG_ENDIAN);

                if (packet.getLength() < 4) {
                    System.err.println("Ignoring malformed packet: shorter than minimum required length");
                    continue;
                }
                byte b1 = bb.get();
                byte b2 = bb.get();

                int Tx = ((b1 & 0xFF) << 8) | (b2 & 0xFF);
                if (relayPending.containsKey(Tx)) {
                    RelayContext context = relayPending.get(Tx);

                    byte[] copy = Arrays.copyOf(packet.getData(), packet.getLength());
                    copy[0] = context.outerTx1;
                    copy[1] = context.outerTx2;

                    DatagramPacket relayResponse = new DatagramPacket(
                        copy, copy.length, context.originalAddr, context.originalPort
                    );
                    
                    socket.send(relayResponse);
                    relayPending.remove(Tx);
                    continue;
                }

                if ((b1 & 0xFF) == 0x20 || (b2 & 0xFF) == 0x20) {
                    System.err.println("Ignoring malformed packet: transaction ID contains space");
                    continue;
                }

                byte space = bb.get();
                if ((space & 0xFF) != 0x20) {
                    System.err.println("Ignoring malformed packet: missing space after transaction ID");
                    continue;
                }

                byte type = bb.get();
                switch((char) type) {
                    case 'N': {
                        // Remaining bytes after type are UTF-8 payload
                        String payloadIn = new String(
                            packet.getData(),
                            4,                              // txid(2) + space(1) + type(1)
                            packet.getLength() - 4,
                            java.nio.charset.StandardCharsets.UTF_8
                        );

                        // Decode requested hashID (CRN string format)
                        String targetHashId = decodeCrnString(payloadIn);

                        // Find up to 3 closest nodes to requested hash
                        List<NodeInfo> closest = getClosestNodes(targetHashId, this.getKnownNodes());

                        // Build response payload: O + pairs of (nodeName, address), each CRN-encoded
                        StringBuilder out = new StringBuilder("O");
                        for (NodeInfo n : closest) {
                            out.append(encodeCrnString(n.getName()));
                            out.append(encodeCrnString(n.getAddress()));
                        }

                        byte[] outBytes = out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

                        // txid + space + payload
                        byte[] sendBuf = new byte[3 + outBytes.length];
                        sendBuf[0] = b1;
                        sendBuf[1] = b2;
                        sendBuf[2] = 0x20; // space
                        System.arraycopy(outBytes, 0, sendBuf, 3, outBytes.length);

                        DatagramPacket response = new DatagramPacket(
                            sendBuf, sendBuf.length, packet.getAddress(), packet.getPort()
                        );
                        socket.send(response);
                        break;
                    }
                    case 'G' : {
                        //Response: H + node name

                        String payload = "H" + encodeCrnString(this.getNodeName());
                        byte[] payloadBytes = payload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                        byte[] sendBuf = new byte[3 + payloadBytes.length];

                        sendBuf[0] = b1;
                        sendBuf[1] = b2;
                        sendBuf[2] = ' ';
                        
                        for(int i = 0; i < payloadBytes.length; i++) {
                            sendBuf[i + 3] = payloadBytes[i];
                        }

                        DatagramPacket DpSend = new DatagramPacket(sendBuf, sendBuf.length, packet.getAddress(), packet.getPort());
                        socket.send(DpSend);
                        break;
                    }
                    case 'V' : {
                        // Type: messageType + node name + embedded message
                        // A node receiving a relay MUST forward the embedded message to the named node.
                        // If the embedded message is a request, the response MUST be returned to the original sender
                        // using the relay transaction ID.
                        // Relay handling MUST NOT prevent processing of other messages.
                        // <txid(2 bytes)> <space> V<enc(targetNodeName)><embeddedMessageBytes>
                        ParsedCrnString targetNodeNameEncoded;
                        try {
                            targetNodeNameEncoded = parseCrnString(packet.getData(), 4, packet.getLength());
                        } catch (IllegalArgumentException e) {
                            break;
                        }
                        String targetNodeName = targetNodeNameEncoded.value;
                        System.out.println("[V] targetNodeName=" + targetNodeName);

                        // Embedded message bytes start immediately after encoded target node name.
                        byte[] embeddedMessageBytes = java.util.Arrays.copyOfRange(
                            packet.getData(), targetNodeNameEncoded.nextOffset, packet.getLength()
                        );
                        System.out.println("[V] embeddedLength=" + embeddedMessageBytes.length);

                        if (embeddedMessageBytes.length < 4) throw new Exception("message length is less than 4");

                        byte messageType = embeddedMessageBytes[3];
                        char t = (char) messageType;
                        boolean isRequest =
                            t == 'G' || t == 'N' || t == 'E' || t == 'R' || t == 'W' || t == 'C' || t == 'V';
                        System.out.println("[V] embeddedType=" + t + ", isRequest=" + isRequest);

                        String addr = nodeMap.get(targetNodeName);
                        if (addr == null) {
                            System.out.println("[V] target address not found in nodeMap");
                            break;
                        }
                        String[] parts = addr.split(":");
                        if (parts.length != 2) {
                            System.out.println("[V] malformed target address: " + addr);
                            break;
                        }
                        InetAddress targetIp = InetAddress.getByName(parts[0]);
                        int targetPort = Integer.parseInt(parts[1]);
                        System.out.println("[V] forwarding to " + targetIp.getHostAddress() + ":" + targetPort);

                        DatagramPacket forward = new DatagramPacket(
                            embeddedMessageBytes,
                            embeddedMessageBytes.length,
                            targetIp,      // resolved from target node name
                            targetPort
                        );
                        socket.send(forward);

                        if (isRequest) {
                            // Store mapping (inner txid -> original sender + outer txid)
                            // so response can be returned using relay transaction ID.
                            int innerTx = ((embeddedMessageBytes[0] & 0xFF) << 8) | (embeddedMessageBytes[1] & 0xFF);
                            relayPending.put(innerTx, new RelayContext(packet.getAddress(), packet.getPort(), b1, b2));
                        }

                        break;
                    }
                    case 'I' : {
                        // Used to communicate information to the user. Nodes MAY discard these messages.
                        break;
                    }
                    case 'R' : {
                        String payloadIn = new String(
                            packet.getData(),
                            4,                              // txid(2) + space(1) + type(1)
                            packet.getLength() - 4,
                            java.nio.charset.StandardCharsets.UTF_8
                        );

                        String requestedKey;
                        try {
                            requestedKey = decodeCrnString(payloadIn);
                        } catch (IllegalArgumentException e) {
                            break;
                        }

                        boolean a = localHasKey(requestedKey);
                        boolean b = isOneOfThreeClosest(requestedKey);

                        byte[] responsePayload;
                        if (a && requestedKey.startsWith("D:")) {
                            String stored = dataStore.get(requestedKey);
                            byte[] valueBytes = encodeCrnString(stored).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                            responsePayload = new byte[5 + valueBytes.length];
                            responsePayload[0] = b1;
                            responsePayload[1] = b2;
                            responsePayload[2] = 0x20;
                            responsePayload[3] = (byte) 'S';
                            responsePayload[4] = (byte) 'Y';
                            System.arraycopy(valueBytes, 0, responsePayload, 5, valueBytes.length);
                        } else {
                            byte code = b ? (byte) 'N' : (byte) 'X';
                            responsePayload = new byte[] { b1, b2, 0x20, (byte) 'S', code };
                        }

                        DatagramPacket currPacket = new DatagramPacket(
                            responsePayload,
                            responsePayload.length,
                            packet.getAddress(),
                            packet.getPort()
                        );
                        socket.send(currPacket);
                        break;
                    }
                    case 'E' : {
                        String payloadIn = new String(
                            packet.getData(),
                            4,
                            packet.getLength() - 4,
                            java.nio.charset.StandardCharsets.UTF_8
                        );
                        String requestedKey;
                        try {
                            requestedKey = decodeCrnString(payloadIn);
                        } catch (IllegalArgumentException e) {
                            break;
                        }

                        boolean a = localHasKey(requestedKey);
                        boolean b = isOneOfThreeClosest(requestedKey);
                        byte code = a ? (byte) 'Y' : (b ? (byte) 'N' : (byte) 'X');
                        byte[] resp = new byte[] { b1, b2, 0x20, (byte) 'F', code };
                        DatagramPacket reply = new DatagramPacket(resp, resp.length, packet.getAddress(), packet.getPort());
                        socket.send(reply);
                        break;
                    }
                    case 'W': {
                        ParsedCrnString k;
                        ParsedCrnString v;
                        try {
                            k = parseCrnString(packet.getData(), 4, packet.getLength());
                            v = parseCrnString(packet.getData(), k.nextOffset, packet.getLength());
                        } catch (IllegalArgumentException e) {
                            break;
                        }

                        String writeKey = k.value;
                        String writeValue = v.value;

                        byte code;
                        if (writeKey.startsWith("D:")) {
                            boolean a = dataStore.containsKey(writeKey);
                            boolean b = isOneOfThreeClosest(writeKey);
                            if (a) {
                                dataStore.put(writeKey, writeValue);
                                code = (byte) 'R';
                            } else if (b && !hasThreeStrictlyCloserNodes(writeKey)) {
                                dataStore.put(writeKey, writeValue);
                                code = (byte) 'A';
                            } else {
                                code = (byte) 'X';
                            }
                        } else if (writeKey.startsWith("N:")) {
                            code = applyAddressStoragePolicy(writeKey, writeValue);
                        } else {
                            break;
                        }

                        byte[] ack = new byte[] { b1, b2, 0x20, (byte) 'X', code };
                        DatagramPacket reply = new DatagramPacket(ack, ack.length, packet.getAddress(), packet.getPort());
                        socket.send(reply);
                        break;
                    }
                    case 'C' : {
                        ParsedCrnString key;
                        ParsedCrnString value;
                        ParsedCrnString newValue;
                        try {
                            key = parseCrnString(packet.getData(), 4, packet.getLength());
                            value = parseCrnString(packet.getData(), key.nextOffset, packet.getLength());
                            newValue = parseCrnString(packet.getData(), value.nextOffset, packet.getLength());
                        } catch (IllegalArgumentException e) {
                            break;
                        }

                        String cKey = key.value;
                        String cValue = value.value;
                        String cNewValue = newValue.value;

                        if(!cKey.startsWith("D:")){
                            break;
                        }

                        String keyHash = HashID.getHashedId(cKey);
                        HashSet<Node> candidateNodes = new HashSet<>(knownNodes);
                        candidateNodes.add(this);
                        List<NodeInfo> closestNodes = getClosestNodes(keyHash, candidateNodes);
                        boolean isClosestNode = false;
                        for (NodeInfo curNodeInfo : closestNodes) {
                            if (this.getNodeName() != null && this.getNodeName().equals(curNodeInfo.getName())) {
                                isClosestNode = true;
                                break;
                            }
                        }

                        String stored = dataStore.get(cKey);
                        byte[] cPayload = new byte[]{b1, b2, 0x20, (byte) 'R'};

                        if(stored != null && stored.equals(cValue)) {
                            dataStore.put(cKey, cNewValue);
                        } else if (stored != null && !stored.equals(cValue)) {
                            cPayload[3] = (byte) 'N';
                        } else if (stored == null && isClosestNode) {
                            cPayload[3] = (byte) 'A';
                        } else {
                            cPayload[3] = (byte) 'X';
                        }

                        DatagramPacket cResponse = new DatagramPacket(cPayload, cPayload.length, packet.getSocketAddress());
                        socket.send(cResponse);

                        break;
                    }
                    default : {
                        System.err.println("" + type);
                    }
                }

            } catch (java.net.SocketTimeoutException e) {
                // no packet arrived before timeout; loop will end if delay expired
            } catch (Exception e) {
                // Malformed/unsupported packets must not crash the receiver loop.
                System.err.println("Ignoring malformed packet: " + e.getMessage());
            }
        }
    }

    // Build one shared sendRequest(...) helper:
    // Takes messageType + payload + target node.
    // Applies relay wrapping from the stack (V chain) exactly like isActive.
    // Handles timeout/retry rules (up to 3 sends, 5s timeout).

    public byte[] sendRequest(byte type, byte[] payload, String target, byte expRespType) throws Exception {
        Set<Byte> expectedRespTypes = new HashSet<>();
        expectedRespTypes.add(expRespType);
        return sendRequest(type, payload, target, expectedRespTypes);
    }

    public byte[] sendRequest(byte type, byte[] payload, String target, Set<Byte> expectedRespTypes) throws Exception {
        if(target == null) {
            throw new Exception("nodename does not exist");
        }
        if (expectedRespTypes == null || expectedRespTypes.isEmpty()) {
            throw new Exception("expected response type set is empty");
        }

        byte[] outerTx = generateTransactionId();
        byte[] request = new byte[4 + payload.length];

        request[0] = outerTx[0];
        request[1] = outerTx[1];
        request[2] = 0x20;
        request[3] = type;
        
        for(int i = 0; i < payload.length; i++) {
            request[i + 4] = payload[i];
        }

        String addr = nodeMap.get(target);

        if (addr == null) {
            throw new Exception("address is null");
        }

        if (addr.indexOf(":") == -1) {
            throw new Exception("port does not exist");
        }

        String[] ipPort = addr.split(":");

        if (ipPort.length != 2) throw new Exception("ip or port missing");

        InetAddress requestIp = InetAddress.getByName(ipPort[0]);
        int requestPort = Integer.parseInt(ipPort[1]);

        if (requestIp == null || requestPort <= 0) {
            throw new Exception("ip or port does not exist");
        }

        DatagramPacket requestDgPacket = new DatagramPacket(
            request,
            request.length,
            requestIp,
            requestPort
        );

        for (int attempt = 0; attempt < 3; attempt++) {
            //send it through relays is there are any:
            // arraylist has elements such as N:r1
            byte[] embedded = Arrays.copyOf(request, request.length);
            String nextHopName = target;
            ArrayList<String> relays = new ArrayList<>(relayStack);
            for (int i = relays.size() - 1; i >= 0; i--) {
                String relayName = relays.get(i);
                boolean isOutermostRelay = (i == 0);

                byte tx1;
                byte tx2;
                if (isOutermostRelay) {
                    tx1 = outerTx[0];
                    tx2 = outerTx[1];
                } else {
                    byte[] relayTx = generateTransactionId();
                    tx1 = relayTx[0];
                    tx2 = relayTx[1];
                }

                byte[] targetField = encodeCrnString(nextHopName).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                byte[] wrapped = new byte[4 + targetField.length + embedded.length];
                wrapped[0] = tx1;
                wrapped[1] = tx2;
                wrapped[2] = 0x20;
                wrapped[3] = (byte) 'V';
                System.arraycopy(targetField, 0, wrapped, 4, targetField.length);
                System.arraycopy(embedded, 0, wrapped, 4 + targetField.length, embedded.length);

                String relayAddr = nodeMap.get(relayName);

                if (relayAddr == null) {
                    throw new Exception("address is null");
                }

                if (relayAddr.indexOf(":") == -1) {
                    throw new Exception("port does not exist");
                }

                String[] relayIpPort = relayAddr.split(":");

                if (relayIpPort.length != 2) throw new Exception("ip or port missing");

                if(isOutermostRelay) {
                    requestIp = InetAddress.getByName(relayIpPort[0]);
                    requestPort = Integer.parseInt(relayIpPort[1]);
                }

                embedded = wrapped;
                nextHopName = relayName;
            }

            if(!relays.isEmpty()) {
                DatagramPacket relayDgPacket = new DatagramPacket(
                    embedded,
                    embedded.length,
                    requestIp,
                    requestPort
                );
                socket.send(relayDgPacket);
            } else {
                socket.send(requestDgPacket);
            }

            long deadline = System.currentTimeMillis() + 5000L;
            while (System.currentTimeMillis() < deadline) {
                int remaining = (int) (deadline - System.currentTimeMillis());
                if (remaining <= 0) {
                    break;
                }

                socket.setSoTimeout(remaining);
                byte[] recvBuf = new byte[65535];
                DatagramPacket response = new DatagramPacket(recvBuf, recvBuf.length);

                try {
                    socket.receive(response);
                } catch (java.net.SocketTimeoutException e) {
                    break;
                }

                if (response.getLength() < 4) {
                    continue;
                }

                byte[] data = response.getData();
                if (data[0] != outerTx[0] || data[1] != outerTx[1]) {
                    continue;
                }

                if ((data[2] & 0xFF) != 0x20) {
                    continue;
                }

                byte responseType = data[3];
                if (!expectedRespTypes.contains(responseType)) {
                    continue;
                }

                return Arrays.copyOfRange(data, 0, response.getLength());
            }
        }

        if (target != null && target.startsWith("N:")) {
            nodeMap.remove(target);
            removeKnownNodeByName(target);
        }
        return null;
    }

    
    public boolean isActive(String nodeName) throws Exception {
        if (nodeName == null || nodeName.isEmpty() || !nodeName.startsWith("N:")) {
            throw new Exception("Node name must be a non-empty CRN node name (N:...)");
        }

        byte[] isActivePayload = new byte[0];
        byte[] response = sendRequest((byte) 'G', isActivePayload, nodeName, (byte) 'H');

        if (response == null) {
            return false;
        }   

        String responsePayload = new String(
            response,
            4,
            response.length - 4,
            java.nio.charset.StandardCharsets.UTF_8
        );

        return nodeName.equals(decodeCrnString(responsePayload));
    }

    
    public void pushRelay(String nodeName) throws Exception {
        if (nodeName == null || nodeName.isEmpty()) {
            throw new Exception("Relay node name cannot be null or empty");
        }
        if (!nodeName.startsWith("N:")) {
            throw new Exception("Relay node name must start with 'N:'");
        }

        relayStack.addLast(nodeName);
    }

    public void popRelay() throws Exception {
        if (!relayStack.isEmpty()) {
            relayStack.removeLast();
        }
    }

    public boolean exists(String key) throws Exception {
        if (key == null) {
            return false;
        }

        if (localHasKey(key)) {
            return true;
        }

        String encodedKey = encodeCrnString(key);
        byte[] payload = encodedKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Set<Byte> expectedTypes = new HashSet<>();
        expectedTypes.add((byte) 'F');

        List<NodeInfo> closestNodesList = getClosestNodes(HashID.getHashedId(key), knownNodes);
        if (closestNodesList.isEmpty()) {
            return false;
        }

        for (NodeInfo node : closestNodesList) {
            try {
                byte[] response = sendRequest((byte) 'E', payload, node.getName(), expectedTypes);
                if (response == null || response.length < 5) {
                    continue;
                }

                byte code = response[4];
                if (code == (byte) 'Y') {
                    return true;
                }
            } catch (Exception e) {
                // try next closest node
            }
        }

        return false;
    }
    
    public String read(String key) throws Exception {
        if(key == null) {
            return null;
        }

        if(!key.startsWith("D:"))
        throw new Exception("Key is not data");

        if (dataStore.containsKey(key)) {
            return dataStore.get(key);
        }

        byte requestType = 'R';
        byte expectedReadHitType = 'S';
        String encodedKey = encodeCrnString(key);
        byte[] encodedKeyBytes = encodedKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        List<NodeInfo> closestNodesList = getClosestNodes(HashID.getHashedId(key), knownNodes);
        if (closestNodesList.isEmpty()) {
            return null;
        }

        for (NodeInfo node : closestNodesList) {
            String target = node.getName();

            byte[] response = sendRequest(requestType, encodedKeyBytes, target, expectedReadHitType);
            if (response == null) {
                continue; // try next closest node
            }

            try {
                if (response.length < 5) {
                    continue;
                }

                byte code = response[4];
                if (code == (byte) 'Y') {
                    String responsePayload = new String(
                        response, 5, response.length - 5, java.nio.charset.StandardCharsets.UTF_8
                    );
                    return decodeCrnString(responsePayload);
                }
                if (code == (byte) 'N' || code == (byte) 'X' || code == (byte) '?') {
                    continue;
                }

                // Backward-compatibility: some nodes may return only encoded value after S
                String responsePayload = new String(
                    response, 4, response.length - 4, java.nio.charset.StandardCharsets.UTF_8
                );
                return decodeCrnString(responsePayload);
            } catch (Exception e) {
                // malformed/unexpected response from this node; try next one
            }
        }

        return null;
    }

    // FOR DATA/VALUE PAIRS
    public boolean write(String key, String value) throws Exception {
        if (key == null || value == null || !key.startsWith("D:")) {
            return false;
        }

        String encKey = encodeCrnString(key);
        String encVal = encodeCrnString(value);
        byte[] payload = (encKey + encVal).getBytes(java.nio.charset.StandardCharsets.UTF_8);

        List<NodeInfo> closestNodesList = getClosestNodes(HashID.getHashedId(key), knownNodes);

        if (closestNodesList.isEmpty()) {
            dataStore.put(key, value);
            return true;
        }

        for (NodeInfo node : closestNodesList) {
            String target = node.getName();

            try {
                byte[] response = sendRequest((byte) 'W', payload, target, (byte) 'X');
                if (response != null) {
                    if (response.length < 5) {
                        continue;
                    }
                    byte code = response[4];
                    if (code == (byte) 'R' || code == (byte) 'A') {
                        return true;
                    }
                    if (code == (byte) 'X') {
                        continue;
                    }
                }
            } catch (Exception e) {
                // try next closest node
            }
        }

        return false;
    }

    // FOR DATA/VALUE PAIRS
    public boolean CAS(String key, String currentValue, String newValue) throws Exception {
	    if(key == null || currentValue == null || newValue == null || !key.startsWith("D:")) {
            return false;
        }

        String encodedKey = encodeCrnString(key);
        String encodedCurrVal = encodeCrnString(currentValue);
        String encodedNewVal = encodeCrnString(newValue);
        String responseString = encodedKey + encodedCurrVal + encodedNewVal;
        byte[] payload = responseString.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        List<NodeInfo> closestNodesList = getClosestNodes(HashID.getHashedId(key), knownNodes);
        Set<Byte> expectedRespTypes = new HashSet<>(Arrays.asList(
            (byte) 'R', (byte) 'N', (byte) 'A', (byte) 'X'
        ));

        if (closestNodesList.isEmpty()) {
            return false;
        }

        for (NodeInfo node : closestNodesList) {
            String target = node.getName();

            byte[] response = sendRequest((byte) 'C', payload, target, expectedRespTypes);
            if (response == null) {
                continue; // try next closest node
            }

            if (response.length >= 4) {
                byte responseType = response[3];
                if (responseType == (byte) 'R') {
                    return true;
                }
                if (responseType == (byte) 'N' || responseType == (byte) 'A' || responseType == (byte) 'X') {
                    return false;
                }
            }
        }

        return false;
    }

    private boolean localHasKey(String key) {
        if (key == null) {
            return false;
        }
        if (key.startsWith("D:")) {
            return dataStore.containsKey(key);
        }
        if (key.startsWith("N:")) {
            String v = nodeMap.get(key);
            return v != null && !v.isEmpty();
        }
        return false;
    }

    private void removeKnownNodeByName(String nodeName) {
        if (nodeName == null) {
            return;
        }
        Iterator<Node> it = knownNodes.iterator();
        while (it.hasNext()) {
            Node n = it.next();
            if (n != null && nodeName.equals(n.getNodeName())) {
                it.remove();
            }
        }
    }

    private boolean isValidAddressValue(String address) {
        if (address == null || address.isEmpty() || address.indexOf(":") == -1) {
            return false;
        }
        String[] parts = address.split(":");
        if (parts.length != 2 || parts[0].isEmpty()) {
            return false;
        }
        try {
            int p = Integer.parseInt(parts[1]);
            return p > 0 && p <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private int distanceToNode(String nodeName) throws Exception {
        String selfHash = this.getNodeHashedID();
        if (selfHash == null) {
            selfHash = HashID.getHashedId(this.getNodeName());
            this.hashedID = selfHash;
        }
        String otherHash = HashID.getHashedId(nodeName);
        return HashID.getNodeDistance(selfHash, otherHash);
    }

    private byte applyAddressStoragePolicy(String nodeName, String address) throws Exception {
        if (nodeName == null || !nodeName.startsWith("N:") || !isValidAddressValue(address)) {
            return (byte) 'X';
        }

        String existing = nodeMap.get(nodeName);
        boolean hadExisting = existing != null && !existing.isEmpty();
        if (hadExisting || nodeName.equals(this.getNodeName())) {
            nodeMap.put(nodeName, address);
            return hadExisting ? (byte) 'R' : (byte) 'A';
        }

        int targetDist = distanceToNode(nodeName);
        int sameDistanceCount = 0;
        for (Map.Entry<String, String> entry : nodeMap.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            if (k == null || !k.startsWith("N:") || k.equals(nodeName) || !isValidAddressValue(v)) {
                continue;
            }
            int d;
            try {
                d = distanceToNode(k);
            } catch (Exception e) {
                continue;
            }
            if (d == targetDist) {
                sameDistanceCount++;
            }
        }

        if (sameDistanceCount >= 3) {
            // Keep existing entries (treat them as more stable by default).
            return (byte) 'X';
        }

        nodeMap.put(nodeName, address);
        return (byte) 'A';
    }

    private boolean hasThreeStrictlyCloserNodes(String dataKey) throws Exception {
        if (dataKey == null || !dataKey.startsWith("D:") || this.getNodeName() == null) {
            return false;
        }
        String keyHash = HashID.getHashedId(dataKey);
        int selfDistance = HashID.getNodeDistance(keyHash, HashID.getHashedId(this.getNodeName()));
        int count = 0;

        HashSet<String> seen = new HashSet<>();
        for (Node n : knownNodes) {
            if (n == null || n.getNodeName() == null) {
                continue;
            }
            String nn = n.getNodeName();
            if (nn.equals(this.getNodeName()) || seen.contains(nn)) {
                continue;
            }
            seen.add(nn);
            int d = HashID.getNodeDistance(keyHash, HashID.getHashedId(nn));
            if (d < selfDistance) {
                count++;
                if (count >= 3) {
                    return true;
                }
            }
        }

        for (Map.Entry<String, String> entry : nodeMap.entrySet()) {
            String nn = entry.getKey();
            String addr = entry.getValue();
            if (nn == null || !nn.startsWith("N:") || nn.equals(this.getNodeName()) || seen.contains(nn)) {
                continue;
            }
            if (!isValidAddressValue(addr)) {
                continue;
            }
            seen.add(nn);
            int d;
            try {
                d = HashID.getNodeDistance(keyHash, HashID.getHashedId(nn));
            } catch (Exception e) {
                continue;
            }
            if (d < selfDistance) {
                count++;
                if (count >= 3) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isOneOfThreeClosest(String targetKey) throws Exception {
        if (this.getNodeName() == null || targetKey == null) {
            return false;
        }
        String keyHash = HashID.getHashedId(targetKey);
        HashSet<Node> candidateNodes = new HashSet<>(knownNodes);
        candidateNodes.add(this);
        List<NodeInfo> closestNodes = getClosestNodes(keyHash, candidateNodes);
        for (NodeInfo node : closestNodes) {
            if (this.getNodeName().equals(node.getName())) {
                return true;
            }
        }
        return false;
    }

    public String encodeCrnString(String message) {
        int spaceCount = 0;
        for(int i = 0; i < message.length(); i++) {
            if (message.charAt(i) == ' ') {
                spaceCount++;
            }
        }

        return "" + spaceCount + " " + message + " ";
    }

    public String decodeCrnString(String message) {
        if (message == null) {
            throw new IllegalArgumentException("Message is null");
        }

        int firstSpace = message.indexOf(' ');
        if (firstSpace < 0) {
            throw new IllegalArgumentException("Invalid CRN string: missing count separator");
        }

        int expectedInnerSpaces;
        try {
            expectedInnerSpaces = Integer.parseInt(message.substring(0, firstSpace));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid CRN string: bad space count", e);
        }

        int i = firstSpace + 1; // start of content
        int seenInnerSpaces = 0;

        while (i < message.length()) {
            if (message.charAt(i) == ' ') {
                if (seenInnerSpaces == expectedInnerSpaces) {
                    // delimiter after content
                    return message.substring(firstSpace + 1, i);
                }
                seenInnerSpaces++;
            }
            i++;
        }

        throw new IllegalArgumentException("Invalid CRN string: missing trailing delimiter");
    }

    private ParsedCrnString parseCrnString(byte[] data, int offset, int limitExclusive) {
        if (offset < 0 || offset >= limitExclusive) {
            throw new IllegalArgumentException("Invalid CRN string start");
        }

        int firstSpace = -1;
        for (int i = offset; i < limitExclusive; i++) {
            if ((data[i] & 0xFF) == 0x20) {
                firstSpace = i;
                break;
            }
        }
        if (firstSpace < 0) {
            throw new IllegalArgumentException("Invalid CRN string: missing count separator");
        }

        int expectedInnerSpaces;
        try {
            String countText = new String(
                data,
                offset,
                firstSpace - offset,
                java.nio.charset.StandardCharsets.US_ASCII
            );
            expectedInnerSpaces = Integer.parseInt(countText);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid CRN string: bad space count");
        }

        int contentStart = firstSpace + 1;
        int seenInnerSpaces = 0;
        for (int i = contentStart; i < limitExclusive; i++) {
            if ((data[i] & 0xFF) == 0x20) {
                if (seenInnerSpaces == expectedInnerSpaces) {
                    String value = new String(
                        data,
                        contentStart,
                        i - contentStart,
                        java.nio.charset.StandardCharsets.UTF_8
                    );
                    return new ParsedCrnString(value, i + 1);
                }
                seenInnerSpaces++;
            }
        }

        throw new IllegalArgumentException("Invalid CRN string: missing trailing delimiter");
    }

    private byte[] generateTransactionId() {
        Random random = new Random();
        byte b1;
        byte b2;
        do {
            b1 = (byte) random.nextInt(256);
        } while ((b1 & 0xFF) == 0x20);
        do {
            b2 = (byte) random.nextInt(256);
        } while ((b2 & 0xFF) == 0x20);
        return new byte[] {b1, b2};
    }

    private RoutedMessage buildNameRequest(String targetNodeName, byte outerTx1, byte outerTx2) throws Exception {
        if (!nodeMap.containsKey(targetNodeName)) {
            throw new Exception("Unknown target node: " + targetNodeName);
        }

        if (relayStack.isEmpty()) {
            String address = nodeMap.get(targetNodeName);
            String[] addressParts = address.split(":");
            InetAddress ip = InetAddress.getByName(addressParts[0]);
            int port = Integer.parseInt(addressParts[1]);

            byte[] direct = new byte[] {outerTx1, outerTx2, 0x20, (byte) 'G'};
            return new RoutedMessage(ip, port, direct);
        }

        byte[] embeddedTx = generateTransactionId();
        byte[] embedded = new byte[] {embeddedTx[0], embeddedTx[1], 0x20, (byte) 'G'};
        String nextHopName = targetNodeName;

        ArrayList<String> relays = new ArrayList<>(relayStack);
        for (int i = relays.size() - 1; i >= 0; i--) {
            String relayName = relays.get(i);
            boolean isOutermostRelay = (i == 0);

            byte tx1;
            byte tx2;
            if (isOutermostRelay) {
                tx1 = outerTx1;
                tx2 = outerTx2;
            } else {
                byte[] relayTx = generateTransactionId();
                tx1 = relayTx[0];
                tx2 = relayTx[1];
            }

            byte[] targetField = encodeCrnString(nextHopName).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] wrapped = new byte[4 + targetField.length + embedded.length];
            wrapped[0] = tx1;
            wrapped[1] = tx2;
            wrapped[2] = 0x20;
            wrapped[3] = (byte) 'V';
            System.arraycopy(targetField, 0, wrapped, 4, targetField.length);
            System.arraycopy(embedded, 0, wrapped, 4 + targetField.length, embedded.length);

            embedded = wrapped;
            nextHopName = relayName;
        }

        String firstRelayAddr = nodeMap.get(nextHopName);
        if (firstRelayAddr == null) {
            throw new Exception("Unknown relay node in stack: " + nextHopName);
        }
        String[] addressParts = firstRelayAddr.split(":");
        if (addressParts.length != 2) {
            throw new Exception("Malformed relay address for node: " + nextHopName);
        }
        InetAddress relayIp = InetAddress.getByName(addressParts[0]);
        int relayPort = Integer.parseInt(addressParts[1]);

        return new RoutedMessage(relayIp, relayPort, embedded);
    }

    public List<NodeInfo> getClosestNodes(String targetHashId, HashSet<Node> knownNodes) {
        // we are emptying the PriorityQueue to start fresh and get rid of old values.
        while(!pq.isEmpty()) {pq.poll(); }
        ArrayList<NodeInfo> result = new ArrayList<>();
        HashSet<String> seenNames = new HashSet<>();
        
        for(Node n : knownNodes) {
            NodeInfo currNodeInfo = new NodeInfo(n.getNodeName(), n.getNodeValue(), HashID.getNodeDistance(targetHashId, n.getNodeHashedID()));

            pq.add(currNodeInfo);
            seenNames.add(n.getNodeName());
            if (pq.size() > 3) {
                pq.poll();
            }
        }

        for (Map.Entry<String, String> entry : nodeMap.entrySet()) {
            String nodeName = entry.getKey();
            String address = entry.getValue();
            if (nodeName == null || !nodeName.startsWith("N:")) {
                continue;
            }
            if (address == null || address.isEmpty() || address.indexOf(":") == -1) {
                continue;
            }
            if (seenNames.contains(nodeName)) {
                continue;
            }

            String nodeHash;
            try {
                nodeHash = HashID.getHashedId(nodeName);
            } catch (Exception e) {
                continue;
            }
            NodeInfo currNodeInfo = new NodeInfo(
                nodeName,
                address,
                HashID.getNodeDistance(targetHashId, nodeHash)
            );
            pq.add(currNodeInfo);
            if (pq.size() > 3) {
                pq.poll();
            }
        }

        while(!pq.isEmpty()) {result.add(pq.poll()); }

        return result;
    }
}
