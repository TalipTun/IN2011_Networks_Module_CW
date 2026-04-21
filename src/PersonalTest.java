class PersonalTest {
    public static void main(String[] args) throws Exception {
        Node testNode = new Node();
        testNode.setNodeName("N:test0");
        testNode.openPort(12345);
        testNode.handleIncomingMessages(0);
    }
}