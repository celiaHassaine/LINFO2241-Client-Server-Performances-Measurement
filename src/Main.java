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
//TODO: déplacer l'encryption en dehors du thread ClientSender


//TODO: TASK 2
//TODO: implement a small client ap sending request at random times (choose some distribution) : Corentin
//TODO: make some measurements with different request rate, difficulities : Corentin
//TODO: compare with the 2 implementations of the bruteforce: Merlin

//TODO: TASK 3


public class Main
{
    // CLIENT PARAMETERS
    private static final int nbRequest = 1;
    private static final int pwdLength = 4;

    private static final String folderNameIn = "files/";
    private static final String folderNameOut = "tmp/";


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


    public static void main(String[] args)
    {
        System.out.println("Client creation");

        // Creating socket to connect to server
        Socket socket = null;
        try
        {
            socket = new Socket(ServerMain.getServerIpAddress(), ServerMain.getPortNumber());
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.err.println("Failed to connect to server");
            System.exit(-1);
        }
        System.out.println("Socket created");

        // Sender thread
        ClientSender clientSender = new ClientSender(socket);
        clientSender.start();

        // Receiver thread
        ClientReceiver clientReceiver = new ClientReceiver(socket);
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

        // INSTANCE VARIABLE
        private Socket socket;

        public ClientSender(Socket socket)
        {
            super("ClientSenderThread");
            this.socket = socket;
        }

        @Override
        public void run()
        {
            System.out.println("Run ClientSender");
            try
            {
                for(int requestId = 0; requestId < nbRequest; requestId++)
                {
                    String password = randomStringGenerator(pwdLength);
                    SecretKey keyGenerated = CryptoUtils.getKeyFromPassword(password);

                    // TODO: change file by those in the folder files/files-50MB
                    String filenameIn = folderNameIn + "test_file.pdf";
                    File originalFile = new File(filenameIn);
                    String filenameOut = folderNameOut + "test_file-encrypted-client-id" + requestId + ".pdf";
                    File encryptedFile = new File(filenameOut);

                    CryptoUtils.encryptFile(keyGenerated, originalFile, encryptedFile);
                    System.out.println(filenameIn + " of size " + encryptedFile.length() + " encrypted with password: " + password);

                    // For any I/O operations, a stream is needed where the data are read from or written to. Depending on
                    // where the data must be sent to or received from, different kind of stream are used.
                    OutputStream outSocket = socket.getOutputStream();
                    DataOutputStream out = new DataOutputStream(outSocket);

                    InputStream inFile = new FileInputStream(encryptedFile);

                    // SEND THE PROCESSING INFORMATION AND FILE
                    byte[] hashPwd = hashSHA1(password);
                    int pwdLength = password.length();
                    long fileLength = encryptedFile.length();

                    sendRequest(out, requestId, hashPwd, pwdLength, fileLength);
                    out.flush();
                    FileManagement.sendFile(inFile, out);
                    System.out.println("Client sends : (requestId, hashPwd, pwdLength, fileLength) = (" + requestId + ", " + hashPwd + ", " + pwdLength + ", " + fileLength + ")");

/*
                    // ??? SHOULD WE ?
                    outSocket.close();
                    out.close();
                    inFile.close();
*/
                }
            }
            catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidAlgorithmParameterException |
                    NoSuchPaddingException | IllegalBlockSizeException | IOException | BadPaddingException |
                    InvalidKeyException e)
            {
                e.printStackTrace();
            }

            System.out.println("End ClientSender");
        }
    }

    private static class ClientReceiver extends Thread
    {

        // INSTANCE VARIABLES
        private Socket socket;

        public ClientReceiver(Socket socket)
        {
            super("ClientReceiverThread");
            this.socket = socket;
        }

        @Override
        public void run()
        {
            System.out.println("Run ClientReceiver");

            try
            {
                for(int iRequest = 0; iRequest < nbRequest; iRequest++)
                {
                    DataInputStream inSocket = new DataInputStream(socket.getInputStream());

                    int requestId = inSocket.readInt();
                    long fileLengthServer = inSocket.readLong();
                    System.out.println("Client receives : (requestId, fileLength) = (" + requestId + ", " + fileLengthServer + ")");

                    File decryptedClient = new File(folderNameOut + "test_file-decrypted-client-id" + requestId + ".pdf");

                    OutputStream outFile = new FileOutputStream(decryptedClient);
                    FileManagement.receiveFile(inSocket, outFile, fileLengthServer);

                    inSocket.close();
                    outFile.close();
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