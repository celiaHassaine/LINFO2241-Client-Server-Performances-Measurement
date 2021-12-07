import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;



public class ServerMain
{
    // SERVER PARAMETERS
    private static final String serverIpAddress = "localhost";
    private static final int portNumber = 3333;
    private static final boolean isSmart = true;
    private static final String folderNameIn = "files/";
    private static final String folderNameOut = "tmp/";

    // STATIC VARIABLES AND FUNCTIONS
    private static Map<String, String> dictionary;
    private static boolean listening = true;
    /**
     * This function reads a stream and creates an object Request
     * @param in Stream from which to read the request
     * @return Request with information required by the server to process encrypted file
     */
    public static Request readRequest(DataInputStream in) throws IOException
    {
        int requestId = in.readInt();
        byte [] hashPwd = new byte[20];
        int count = in.read(hashPwd,0, 20);
        if (count < 0){
            throw new IOException("Server could not read from the stream");
        }

        int pwdLength = in.readInt();
        long fileLength = in.readLong();

        return new Request(requestId, hashPwd, pwdLength, fileLength);
    }

    public static void main(String[] args)
    {
        // Dictionary creation
        String filename = "files/10k-most-common_filered.txt";
        try
        {
            dictionary = SmarterBruteForce.createDictionary(filename);
        }
        catch (DictionaryCreationException e)
        {
            e.printStackTrace();
            e.details();
            System.exit(-1);
        }

        // Initialization the connection of the server
        ServerSocket serverSocket;
        try
        // TODO: add second arg backlog to record maximum number of incoming traffic
        {
            serverSocket = new ServerSocket(portNumber);

            while (listening) // while listening and maximal number of queuing capacity of the OS's queue for incoming TCP
            // TODO: check if less than maximal number of queuing capacity of the OS's queue for incoming TCP
            {
                System.out.println("Waiting connection");
                // Accept a connection
                Socket requestSocket = serverSocket.accept();
                System.out.println("Connection from: " + requestSocket);

                // create a thread for each request;
                RequestHandler requestHandler = new RequestHandler(requestSocket, isSmart);
                requestHandler.start();
            }
            serverSocket.close();
        }
        catch (IOException e)
        {
            System.err.println("Could not listen on port " + portNumber);
            System.exit(-1);
        }

    }

    private static class RequestHandler extends Thread
    {
        // STATIC FUNCTION
        /**
         * This function is used by the server to response the decrypted file and the information needed by the client to process the result
         * @param out Socket stream connected to the server where the data are written
         * @param requestId ID of the request currently handled
         * @param fileLength Length of the encrypted file
         */
        public static void sendResponse(DataOutputStream out, int requestId, long fileLength)
                throws IOException
        {
            out.writeInt(requestId);
            out.writeLong(fileLength);
        }

        // INSTANCE VARIABLES
        private Socket requestSocket;
        private boolean isSmart;

        public RequestHandler(Socket requestSocket, boolean isSmart)
        {
            super("RequestHandlerThread");
            this.requestSocket = requestSocket;
            this.isSmart = isSmart;
        }

        @Override
        public void run()
        {
            try
            {
                // Stream to read request from socket
                InputStream inputStream = requestSocket.getInputStream();
                DataInputStream dataInputStream = new DataInputStream(inputStream);

                Request request = readRequest(dataInputStream);
                int requestId = request.getRequestId();
                byte[] hashPwd = request.getHashPassword();
                int pwdLength = request.getLengthPwd();
                long fileLength = request.getLengthFile();

                System.out.println("Server receives : (requestId, hashPwd, pwdLength, fileLength) = (" + requestId + ", " + hashPwd + ", " + pwdLength + ", " + fileLength + ")");

                // Stream to write the file to decrypt
                File networkFile = new File(folderNameOut + "temp-server-id" + requestId + ".pdf");
                OutputStream outFile = new FileOutputStream(networkFile);
                FileManagement.receiveFile(inputStream, outFile, fileLength);

                // BRUTEFORCE:
                // Password is determined by using a bruteforce method implemented in the classes: BruteForce, DumbBruteForce, SmarterBruteForce
                System.out.println("-- Starting bruteforce -- ");
                BruteForce BF;
                if(this.isSmart)
                    BF = new SmarterBruteForce(pwdLength, hashPwd, dictionary);
                else
                    BF = new DumbBruteForce(pwdLength, hashPwd);

                String pwdFound;
                try
                {
                    pwdFound = BF.getPassword();
                }
                catch (PasswordNotFoundException e)
                {
                    e.printStackTrace();
                    e.details();
                    return;
                }
                System.out.println("PASSWORD FOUND: " + pwdFound);
                System.out.println("-- End bruteforce -- ");


                // Send the decryptedFile
                File decryptedFile = new File( folderNameOut + "test_file-decrypted-server-id" + requestId +".pdf");
                SecretKey serverKey = CryptoUtils.getKeyFromPassword(pwdFound);
                CryptoUtils.decryptFile(serverKey, networkFile, decryptedFile);
                InputStream inDecrypted = new FileInputStream(decryptedFile);

                // Stream to write response to socket
                DataOutputStream outSocket = new DataOutputStream(requestSocket.getOutputStream());
                sendResponse(outSocket, requestId, decryptedFile.length());
                outSocket.flush();
                FileManagement.sendFile(inDecrypted, outSocket);
                System.out.println("Server responses to request: " + requestId);

                inputStream.close();
                dataInputStream.close();
                inDecrypted.close();
                outFile.close();

            }
            catch (IOException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | InvalidKeySpecException | BadPaddingException | InvalidKeyException e)
            {
                e.printStackTrace();
            }
        }
    }


    public static int getPortNumber() { return portNumber; }

    public static String getServerIpAddress() { return serverIpAddress; }
}
