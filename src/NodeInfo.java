public class NodeInfo {
    String nodeName;
    String address;
    int distance;
    // stability, and lastseen to be implemented later on

    public NodeInfo(String nodeName, String address, int distance) {
        this.nodeName = nodeName;
        this.address = address;
        this.distance = distance;
    }

    public String getName() {
        return nodeName;
    }

    public String getAddress() {
        return address;
    }

    public int getDistance() {
        return distance;
    }
}
