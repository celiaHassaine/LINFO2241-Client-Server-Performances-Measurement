import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;


//TODO: TASK 1
//TODO: communiquer entre 2 pc différents sur un réseaux non local: Merlin


//TODO: TASK 2
//TODO: implement a small client ap sending request at random times (choose some distribution) : Corentin
//TODO: make some measurements with different request rate, difficulities : Corentin
//TODO: compare with the 2 implementations of the bruteforce: Merlin

//TODO: TASK 3


public class Main
{
    // CLIENT PARAMETERS
    private static final int nbRequest = 2;

    // STATIC VARIABLES AND FUNCTIONS
    // Streams variables
    private static InputStream inputStream = null;
    private static DataInputStream dataInputStream = null;
    private static OutputStream outputStream;
    private static DataOutputStream dataOutputStream = null;

    // Variables related to encryption
    private static final String[] passwords = {"linfo", "coco", "ramin", "love", "merlin"};
    private static final String srcFolderToEncrypt = "files/Files-5MB/";
    private static final int nbFilesInSrc = 5;
    private static final String destFolderEncrypted = "files-encrypted/Files-5MB/";


    // STATIC FUNCTIONS
    /**
     * This function hashes a string with the SHA-1 algorithm
     * @param data The string to hash
     * @return An array of 20 bytes which is the hash of the string
     */
    public static byte[] hashSHA1(String data) throws NoSuchAlgorithmException
    {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return md.digest(data.getBytes());
    }

    /**
     * This function is used to generate random password in order to encrypt files.
     * @param length Length of the string to generate
     * @return A random string of length specified as argument
     */
    public static String randomStringGenerator(int length)
    {
        StringBuilder seedChars= new StringBuilder();
        // TODO: change the for loop by : for(char c = ' '; c <= '~' && !found; c++) to handle all characters https://www.javatpoint.com/java-ascii-table
        for(char c = 'a'; c <= 'z'; c++)
        {
            seedChars.append(c);
        }

        StringBuilder sb = new StringBuilder();
        Random rand = new Random();
        for(int i = 0; i< length; i++)
        {
            sb.append(seedChars.charAt(rand.nextInt(seedChars.length())));
        }
        return sb.toString();
    }

    /**
     * This function encrypt the nbFiles contained in the folder srcFolder and places the encrypted files
     * in the folder dstFolder
     * @param srcFolder source folder of the files to encrypt
     * @param nbFiles number of files in the source folder
     * @param dstFolder destination folder to place the encrypted files
     */
    public static void encryptFolder(String srcFolder, int nbFiles, String dstFolder)
    {
        for(int i = 1; i<=nbFiles; i++)
        {
            try
            {
                File fileToEncrypt = new File(srcFolder + "/file-" + i + ".bin");
                String password = passwords[i-1];
                SecretKey keyGenerated = CryptoUtils.getKeyFromPassword(password);
                File encryptedFile = new File(dstFolder + "file-" + i + ".bin");

                CryptoUtils.encryptFile(keyGenerated, fileToEncrypt, encryptedFile);
                System.out.println(encryptedFile.getName() + " of size " + encryptedFile.length() + " encrypted with password: " + password);
            }
            catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException | IOException | BadPaddingException | InvalidKeyException e)
            {
                e.printStackTrace();
                System.err.println("EncryptFolder exception: unable to encrypt files in " + srcFolderToEncrypt);
            }
        }
    }


    public static void main(String[] args)
    {

        // Encrypted file creation
        System.out.println("Files in " + srcFolderToEncrypt + "encryption");
        //encryptFolder(srcFolderToEncrypt, nbFilesInSrc, destFolderEncrypted);
        System.out.println("Files encrypted in " + destFolderEncrypted);

        // Connection between server and client
        Socket clientSocket = null;
        try
        {
            clientSocket = new Socket(ServerMain.getIpAddress(), ServerMain.getPortNumber());
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.err.println("Failed to connect to server");
            System.exit(-1);
        }
        System.out.println("Socket created");

        // Streams creation
        try
        {
            // Stream to write request to socket
            outputStream = clientSocket.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);

            // Stream to read response from socket
            inputStream = clientSocket.getInputStream();
            dataInputStream = new DataInputStream(inputStream);
        }
        catch (IOException e)
        {
            System.err.println("Could not create required streams");
            System.exit(-1);
        }

        // Sender thread: send requests to the server
        ClientSender clientSender = new ClientSender();
        clientSender.start();

        // Receiver thread: handle response of previous sent requests
        ClientReceiver clientReceiver = new ClientReceiver();
        clientReceiver.start();
    }

    private static class ClientSender extends Thread
    {
        // STATIC FUNCTION
        /**
         * This function is used by a client to send the information needed by the server to process the file
         * @param out Socket stream connected to the server where the data are written
         * @param hashPwd SHA-1 hash of the password used to derive the key of the encryption
         * @param pwdLength Length of the clear password
         * @param fileLength Length of the encrypted file
         */
        public static void sendRequest(DataOutputStream out, int requestId, byte[] hashPwd, int pwdLength, long fileLength)
                throws IOException
        {
            out.writeInt(requestId);
            out.write(hashPwd,0, 20);
            out.writeInt(pwdLength);
            out.writeLong(fileLength);
        }

        public ClientSender()
        {
            super("ClientSenderThread");
        }

        @Override
        public void run()
        {
            System.out.println("Run ClientSender");
            try
            {
                for(int requestId = 0; requestId < nbRequest; requestId++)
                {
                    String password = passwords[requestId];
                    File encryptedFile = new File(destFolderEncrypted + "file-" + (1+ requestId % nbFilesInSrc) + ".bin");
                    InputStream inFile = new FileInputStream(encryptedFile);

                    // SEND THE PROCESSING INFORMATION AND FILE
                    byte[] hashPwd = hashSHA1(password);
                    int pwdLength = password.length();
                    long fileLength = encryptedFile.length();

                    sendRequest(dataOutputStream, requestId, hashPwd, pwdLength, fileLength);
                    dataOutputStream.flush();
                    FileManagement.sendFile(inFile, dataOutputStream);
                    System.out.println("Client sends : (requestId, hashPwd, pwdLength, fileLength) = (" + requestId + ", " + hashPwd + ", " + pwdLength + ", " + fileLength + ")");

                    inFile.close();
                }
            }
            catch (NoSuchAlgorithmException | IOException e)
            {
                e.printStackTrace();
            }

            System.out.println("End ClientSender");
        }
    }

    private static class ClientReceiver extends Thread
    {
        public ClientReceiver()
        {
            super("ClientHandlerThread");
        }

        @Override
        public void run()
        {
            System.out.println("Run ClientReceiver");
            try
            {
                int nbResponseReceived = 0;
                while(true) //nbResponseReceived < nbRequest)
                {
                    int requestId = dataInputStream.readInt();
                    long fileLengthServer = dataInputStream.readLong();
                    System.out.println("Client receives : (requestId, fileLength) = (" + requestId + ", " + fileLengthServer + ")");
                    File decryptedClient = new File("tmp/file-" + requestId + "-decrypted-client" + ".bin");
                    OutputStream outFile = new FileOutputStream(decryptedClient);

                    FileManagement.receiveFile(inputStream, outFile, fileLengthServer);
                    outFile.close();
                    nbResponseReceived++;
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            System.out.println("End ClientReceiver");
        }
    }
}