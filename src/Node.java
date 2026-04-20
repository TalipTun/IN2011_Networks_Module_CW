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
            nodeMap.put(key, value);
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
                    throw new Exception("Packet is shorter than minimum required length");
                }
                byte b1 = bb.get();
                byte b2 = bb.get();

                if ((b1 & 0xFF) == 0x20 || (b2 & 0xFF) == 0x20) {
                    throw new Exception("Transaction ID cannot contain any space");
                }

                byte space = bb.get();
                if ((space & 0xFF) != 0x20) {
                    throw new Exception("Transaction ID is not followed by a space");
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
                    case 'I' : {
                        // Used to communicate information to the user. Nodes MAY discard these messages.
                        break;
                    }
                    case 'V' : {
                        // Type: V + node name + embedded message
                        // A node receiving a relay MUST forward the embedded message to the named node.
                        // If the embedded message is a request, the response MUST be returned to the original sender
                        // using the relay transaction ID.
                        // Relay handling MUST NOT prevent processing of other messages.
                    }
                    default : {
                        System.err.println("" + type);
                    }
                }

            } catch (java.net.SocketTimeoutException e) {
                // no packet arrived before timeout; loop will end if delay expired
            }
        }
    }
    
    public boolean isActive(String nodeName) throws Exception {
	throw new Exception("Not implemented");
    }
    
    public void pushRelay(String nodeName) throws Exception {
	throw new Exception("Not implemented");
    }

    public void popRelay() throws Exception {
        throw new Exception("Not implemented");
    }

    public boolean exists(String key) throws Exception {
        if(nodeMap.containsKey(key)) {
            return true;
        }
        return false;
    }
    
    public String read(String key) throws Exception {
	throw new Exception("Not implemented");
    }

    // FOR DATA/VALUE PAIRS
    public boolean write(String key, String value) throws Exception {
	throw new Exception("Not implemented");
    }

    // FOR DATA/VALUE PAIRS
    public boolean CAS(String key, String currentValue, String newValue) throws Exception {
	throw new Exception("Not implemented");
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

    public List<NodeInfo> getClosestNodes(String targetHashId, HashSet<Node> knownNodes) {
        // we are emptying the PriorityQueue to start fresh and get rid of old values.
        while(!pq.isEmpty()) {pq.poll(); }
        ArrayList<NodeInfo> result = new ArrayList<>();
        
        for(Node n : knownNodes) {
            NodeInfo currNodeInfo = new NodeInfo(n.getNodeName(), n.getNodeValue(), HashID.getNodeDistance(targetHashId, n.getNodeHashedID()));

            pq.add(currNodeInfo);
            if (pq.size() > 3) {
                pq.poll();
            }
        }

        while(!pq.isEmpty()) {result.add(pq.poll()); }

        return result;
    }
}
