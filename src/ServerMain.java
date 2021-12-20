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
    private static final String folderNameOut = "tmp/decrypted/";

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
        try
        {
            String filename = "files/10k-most-common_filtered.txt";
            dictionary = SmarterBruteForce.createDictionary(filename);
        }
        catch (DictionaryCreationException e)
        {
            e.printStackTrace();
            e.details();
            System.exit(-1);
        }

        // Initialization the connection of the server
        try
        {
            // TODO: TASK3: adjust backlog for measurements
            int backlog = 10;
            // requested maximum length of the queue of incoming connections
            // if a connection indication arrives when the queue is full, the connection is refused.
            ServerSocket serverSocket = new ServerSocket(portNumber, backlog);

            while (listening) // while listening
            {
                System.out.println("Waiting connection");
                // listens for a connection to be made to this socket and accepts it. The method blocks until a connection is made.
                Socket requestSocket = serverSocket.accept();
                System.out.println("Connection from: " + requestSocket);

                // TODO: TASK1: use thread pool instead to be more efficient
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
                // File networkFile = new File(folderNameOut + "temp-server-id" + requestId + ".pdf");
                File networkFile = new File(folderNameOut + "temp-server-id" + requestId + ".bin");
                OutputStream outFile = new FileOutputStream(networkFile);
                FileManagement.receiveFile(inputStream, outFile, fileLength);

                // BRUTEFORCE:
                // Password is determined by using a bruteforce method implemented in the classes: BruteForce, DumbBruteForce, SmarterBruteForce
                System.out.println("-- Starting bruteforce -- ");
                BruteForce BF = isSmart ? new SmarterBruteForce(pwdLength, hashPwd, dictionary): new DumbBruteForce(pwdLength, hashPwd);

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
                //String filenameOut = folderNameOut + "test_file-decrypted-server-id" + requestId +".pdf";
                String filenameOut = folderNameOut + "file-" + requestId + "-decrypted-server-" + pwdFound + ".bin";
                File decryptedFile = new File(filenameOut);

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
                outFile.close();
                inDecrypted.close();
                // outSocket.close(); ????


            }
            catch (IOException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | InvalidKeySpecException | BadPaddingException | InvalidKeyException e)
            {
                e.printStackTrace();
            }
        }
    }


    public static int getPortNumber() { return portNumber; }

    public static String getIpAddress() { return serverIpAddress; }
}
