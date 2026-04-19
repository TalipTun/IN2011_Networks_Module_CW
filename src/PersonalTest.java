class PersonalTest {
    public static void main(String[] args) throws Exception {
        String bnryString1 = hexToBin(getHashedId("nodeName"));
        String bnryString2 = hexToBin(getHashedId("randomName"));
        System.out.println(getNodeDistance(bnryString1, bnryString2));
    }
}