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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ServerMain
{
    // SERVER PARAMETERS
    private static final boolean isSmart = Main.SMART;
    private static final int N_THREADS = 12;

    // STATIC VARIABLES AND FUNCTIONS
    private static Map<String, String> dictionary;

    /**
     * This function reads a stream and creates an object Request
     * @param in data stream from which to read the request
     * @return Request with information required by the server to process encrypted file
     */
    public static Request readRequest(DataInputStream in) throws IOException
    {
        int requestId = in.readInt();
        byte [] hashPwd = new byte[20];
        int i = 0;
        while (i < 20) {
            hashPwd[i] = (byte) in.read();
            i++;
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

        // ThreadPool creation
        ExecutorService threadPool = Executors.newFixedThreadPool(N_THREADS);


        // Connection between server and client
        try
        {
            ServerSocket serverSocket = new ServerSocket(Main.portNumber, 100);
            System.out.println("Waiting connection");
            while(true)
            {
                // listens for a connection to be made to this socket and accepts it. The method blocks until a connection is made.
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connection from: " + clientSocket);
                RequestHandler req = new RequestHandler(clientSocket, isSmart);
                threadPool.execute(req);
            }
        }
        catch (IOException ioException)
        {
            System.err.println("unable to connect to connect client and server");
            ioException.printStackTrace();
        }
    }

    private static class RequestHandler implements Runnable
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
        private final Socket clientSocket;
        private final boolean isSmart;

        public RequestHandler(Socket clientSocket, boolean isSmart)
        {
            this.clientSocket = clientSocket;
            this.isSmart = isSmart;
        }

        @Override
        public void run()
        {
            // Streams creation
            InputStream inputStream = null;
            DataInputStream dataInputStream = null;

            // Stream to write response to socket
            OutputStream outputStream = null;
            DataOutputStream dataOutputStream = null;

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
                Request request = readRequest(dataInputStream);

                int requestId = request.getRequestId();
                byte[] hashPwd = request.getHashPassword();
                int pwdLength = request.getLengthPwd();
                long fileLength = request.getLengthFile();
                System.out.println("Server receives : (requestId, hashPwd, pwdLength, fileLength) = (" + requestId + ", " + hashPwd + ", " + pwdLength + ", " + fileLength + ")");

                // Stream to write the file to decrypt
                File networkFile = new File("tmp/temp-server-id" + requestId + ".bin");
                OutputStream outFile = new FileOutputStream(networkFile);
                FileManagement.receiveFile(inputStream, outFile, fileLength);

                // BRUTEFORCE:
                // Password is determined by using a bruteforce method implemented in the classes: BruteForce, DumbBruteForce, SmarterBruteForce
                System.out.println("-- Starting bruteforce for request " + requestId + " -- ");
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
                System.out.println("-- End bruteforce for request " + requestId + "-- ");

                // Send the decryptedFile
                File decryptedFile = new File("tmp/file-" + requestId + "-decrypted-server" + ".bin");
                SecretKey serverKey = CryptoUtils.getKeyFromPassword(pwdFound);
                CryptoUtils.decryptFile(serverKey, networkFile, decryptedFile);
                InputStream inDecrypted = new FileInputStream(decryptedFile);

                // SEND THE PROCESSING INFORMATION AND FILE
                long fileLen2 = decryptedFile.length();
                sendResponse(dataOutputStream, requestId, fileLen2);
                dataOutputStream.flush();
                FileManagement.sendFile(inDecrypted, dataOutputStream);

                System.out.println("Server replies : (requestId, fileLength) = (" + requestId + ", " + fileLen2 + ")");
                inDecrypted.close();
            }
            catch (IOException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | InvalidKeySpecException | BadPaddingException | InvalidKeyException e)
            {
                e.printStackTrace();
            }
        }
    }

}
