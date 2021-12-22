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
import java.util.concurrent.locks.ReentrantLock;


public class ServerMain
{
    // SERVER PARAMETERS
    private static final String serverIpAddress = "localhost";
    private static final int portNumber = 3333;
    private static final boolean isSmart = false;
    private static final int N_THREADS = 6;

    // STATIC VARIABLES AND FUNCTIONS
    // Streams variables
    private static InputStream inputStream = null;
    private static DataInputStream dataInputStream = null;
    private static OutputStream outputStream;
    private static DataOutputStream dataOutputStream = null;

    private static Map<String, String> dictionary;

    // LOCKS FOR THREADS
    private static final ReentrantLock lockInput = new ReentrantLock();
    private static final ReentrantLock lockOutput = new ReentrantLock();

    /**
     * This function reads a stream and creates an object Request
     * @param in data stream from which to read the request
     * @return Request with information required by the server to process encrypted file
     */
    public static Request readRequest(DataInputStream in) throws IOException
    {
        int requestId = in.readInt();
        byte [] hashPwd = new byte[20];
        int count = in.read(hashPwd,0, 20);
        if (count < 0)
        {
            throw new IOException("Server could not read from the stream");
        }

        int pwdLength = in.readInt();
        long fileLength = in.readLong();
        return new Request(requestId, hashPwd, pwdLength, fileLength);
    }

    public static void main(String[] args)
    {
        // ThreadPool creation
        ExecutorService threadPool = Executors.newFixedThreadPool(N_THREADS);

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
            ServerSocket serverSocket = new ServerSocket(portNumber);
            System.out.println("Waiting connection");
            clientSocket = serverSocket.accept(); // listens for a connection to be made to this socket and accepts it. The method blocks until a connection is made.
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
                Request request;
                lockInput.lock();
                try {
                    request = readRequest(dataInputStream);
                }
                finally {
                    lockInput.unlock();
                }
                int requestId = request.getRequestId();         if(requestId < 0 || requestId > 700) { System.err.println("RECEIVE CORRUPTED REQUEST");}
                byte[] hashPwd = request.getHashPassword();
                int pwdLength = request.getLengthPwd();
                long fileLength = request.getLengthFile();
                System.out.println("Server receives : (requestId, hashPwd, pwdLength, fileLength) = (" + requestId + ", " + hashPwd + ", " + pwdLength + ", " + fileLength + ")");

                // Stream to write the file to decrypt
                File networkFile = new File("tmp/temp-server-id" + requestId + ".bin");
                OutputStream outFile = new FileOutputStream(networkFile);
                lockInput.lock();
                try {
                    FileManagement.receiveFile(inputStream, outFile, fileLength);
                }
                finally {
                    lockInput.unlock();
                }
                RequestHandler req = new RequestHandler(request, networkFile, isSmart);
                threadPool.execute(req);
                outFile.close();
            }
        }
        catch ( IOException e)
        {
            e.printStackTrace();
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
        private final Request request;
        private final File networkFile;
        private final boolean isSmart;

        public RequestHandler(Request request, File networkFile, boolean isSmart)
        {
            this.request = request;
            this.networkFile = networkFile;
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
                lockOutput.lock();
                try {
                    sendResponse(dataOutputStream, requestId, fileLen2);
                    dataOutputStream.flush();
                    FileManagement.sendFile(inDecrypted, dataOutputStream);
                }
                finally {
                    lockOutput.unlock();
                }


                System.out.println("Server replies : (requestId, fileLength) = (" + requestId + ", " + fileLen2 + ")");
                inDecrypted.close();

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
