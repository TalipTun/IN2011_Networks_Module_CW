class PersonalTest {
    public static void main(String[] args) throws Exception {
        Node testNode = new Node();
        testNode.openPort(12345);
        testNode.handleIncomingMessages(10000);
    }
}