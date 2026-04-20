class PersonalTest {
    public static void main(String[] args) throws Exception {
        Node testNode = new Node();
        testNode.openPort(12345);
        System.err.println(testNode.getNodeValue());
    }
}