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

    // STATIC VARIABLES AND FUNCTIONS
    // Streams variables
    private static InputStream inputStream = null;
    private static DataInputStream dataInputStream = null;
    private static OutputStream outputStream;
    private static DataOutputStream dataOutputStream = null;

    private static Map<String, String> dictionary;

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

        // Connection between server and client
        Socket clientSocket = null;
        try
        {
            // requested maximum length of the queue of incoming connections
            // if a connection indication arrives when the queue is full, the connection is refused.
            ServerSocket serverSocket = new ServerSocket(portNumber);
            System.out.println("Waiting connection");
            // listens for a connection to be made to this socket and accepts it. The method blocks until a connection is made.
            clientSocket = serverSocket.accept();
            System.out.println("Connection from: " + clientSocket);
        }
        catch (IOException ioException)
        {
            System.err.println("unable to connect to connect client and server");
            ioException.printStackTrace();
        }

        // Streams creation
        try
        {
            // Stream to read request from socket
            inputStream = clientSocket.getInputStream();
            dataInputStream = new DataInputStream(inputStream);

            // Stream to write response to socket
            outputStream = clientSocket.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);
        }
        catch (IOException e)
        {
            System.err.println("Could not create required streams");
            System.exit(-1);
        }

        // Request handling
        try
        {
            while(true)
            {
                Request request = readRequest(dataInputStream);
                int requestId = request.getRequestId();
                byte[] hashPwd = request.getHashPassword();
                int pwdLength = request.getLengthPwd();
                long fileLength = request.getLengthFile();
                System.out.println("Server receives : (requestId, hashPwd, pwdLength, fileLength) = (" + requestId + ", " + hashPwd + ", " + pwdLength + ", " + fileLength + ")");

                // Stream to write the file to decrypt
                File networkFile = new File("tmp/temp-server-id" + request.getRequestId() + ".bin");
                OutputStream outFile = new FileOutputStream(networkFile);
                FileManagement.receiveFile(inputStream, outFile, request.getLengthFile());

                RequestHandler requestHandler = new RequestHandler(request, networkFile, outFile, isSmart); // TODO: queue ? See coco doesn't like that
                requestHandler.start();
            }
        }
        catch ( IOException e)
        {
            e.printStackTrace();
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
        private Request request;
        private File networkFile;
        private OutputStream outFile;
        private boolean isSmart;

        public RequestHandler(Request request,File networkFile, OutputStream outFile, boolean isSmart)
        {
            super("RequestHandlerThread");
            this.request = request;
            this.networkFile = networkFile;
            this.outFile = outFile;
            this.isSmart = isSmart;
        }

        @Override
        public void run()
        {
            try
            {
                int requestId = request.getRequestId();
                byte[] hashPwd = request.getHashPassword();
                int pwdLength = request.getLengthPwd();

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
                File decryptedFile = new File("tmp/file-" + requestId + "-decrypted-server" + ".bin");
                SecretKey serverKey = CryptoUtils.getKeyFromPassword(pwdFound);
                CryptoUtils.decryptFile(serverKey, networkFile, decryptedFile);
                InputStream inDecrypted = new FileInputStream(decryptedFile);

                sendResponse(dataOutputStream, requestId, decryptedFile.length());
                dataOutputStream.flush();
                FileManagement.sendFile(inDecrypted, dataOutputStream);
                System.out.println("Server responses to request: " + requestId);

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

    public static String getIpAddress() { return serverIpAddress; }
}
