class PersonalTest {
    public static void main(String[] args) throws Exception {
        String bnryString1 = hexToBin(getHashedId("nodeName"));
        String bnryString2 = hexToBin(getHashedId("randomName"));
        System.out.println(getNodeDistance(bnryString1, bnryString2));
    }

    public static String getHashedId(String nodeName) throws Exception {
        byte[] hashedID = HashID.computeHashID(nodeName);

        // temporary debug to check hash length
        //System.out.println("Hash length: " + hashedID.length);

        StringBuilder sb = new StringBuilder();
        for(byte b : hashedID) {
            sb.append(String.format("%02x", b));
        }

        // temporary debug to check hashed string
        // System.out.println(sb.toString());
        return sb.toString();
    }

    private static String hexToBin(String hex){
        String bin = "";
        String binFragment = "";
        int iHex;
        hex = hex.trim();
        hex = hex.replaceFirst("0x", "");

        for(int i = 0; i < hex.length(); i++){
            iHex = Integer.parseInt("" + hex.charAt(i), 16);
            binFragment = Integer.toBinaryString(iHex);

            while(binFragment.length() < 4){
                binFragment = "0" + binFragment;
            }
            bin += binFragment;
        }
        return bin;
    }

    private static int getNodeDistance(String homeBinaryStr, String neighborBinaryStr) {
        int equalCount = 0;
        
        for(int i = 0; i < homeBinaryStr.length(); i++) {
            if (homeBinaryStr.charAt(i) == neighborBinaryStr.charAt(i)) {
                equalCount++;
            } else {
                return 256 - equalCount;
            }
        }

        return 256 - equalCount;
    }
}

//format matches
// example output : c22e1d650c0b6ff53d9f72bc5dbeb06e07dadba6dde7ae554fe5904cad31a518
// my output : 72ccb58a1d3cb9d3cd7fa004f8cb58b8348519b6afb40d59858da6aee71888a7