import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Random;


public class Main
{
    // SERVER
    private static final String serverIpAddress = "localhost";
    public static final int portNumber = 3333;

    // CLIENT PARAMETERS
    // Measure parameter
    private static final double rate = 10; // # request/s
    private static FileWriter fileWriter = null;
    // Encryption parameters
    private static final int foldIdx = 0; // index of folder to encrypt
    private static final Encryptor.Folder foldToSend = Encryptor.folders[foldIdx];
    // Request parameters
    private static final int nbClients = 100;

    // STATIC VARIABLES AND FUNCTIONS
    // Timer variables
    private static final HashMap<Integer, Long> startTimes = new HashMap<>();  // (requestID, send time)

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

    public static void measureSetup(String filePath)
    {
        File file = new File(filePath); // first create file object for file placed at filepath
        try
        {
            fileWriter = new FileWriter(file); // create FileWriter object with file as parameter

            // adding header to csv
            String[] header = { };
            fileWriter.write("Request, Response Time [s] \n");
        }
        catch (IOException e)
        {
            System.err.println("ERROR CSV file creation");
            e.printStackTrace();
        }
    }

    /**
     * Returns a random double sample following a exponential distribution
     * @param rate rate of the exponential distribution (number of events per second)
     */
    public static double nextExp(double rate)
    {
        Random rnd = new Random();
        return -(1/rate)*Math.log(rnd.nextDouble());
    }

    /**
     * Returns current time in seconds
     */
    public static double getCurrentTime() {
        return System.currentTimeMillis()/1000.0;
    }

    public static void main(String[] args)
    {
        measureSetup("measures/Files-20KB.csv");

        // Encryption of folder foldIdx
        Encryptor.main(new String[] {foldIdx+""});

        double start_time = getCurrentTime(); // Start time in seconds
        double deltaTime = 0.0;
        double inter_request_time = nextExp(rate);

        int iClient =0;
        while(iClient < nbClients)
        {
            deltaTime = getCurrentTime() - start_time;
            if (deltaTime >= inter_request_time)
            {
                // Connection between with server
                try
                {
                    Socket clientSocket = new Socket(serverIpAddress, portNumber);
                    ClientThread clientThread = new ClientThread(iClient, clientSocket);
                    // Sender thread: send requests to the server
                    clientThread.start();
                    inter_request_time = nextExp(rate);
                    start_time = getCurrentTime();
                    iClient++;
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    System.err.println("Failed to connect to server");
                    System.exit(-1);
                }
                System.out.println("Socket created");
            }

        }
    }

    private static class ClientThread extends Thread
    {
        // INSTANCE VARIABLES
        private int iClient;
        private Socket clientSocket;
        // Streams variables
        private InputStream inputStream = null;
        private DataInputStream dataInputStream = null;
        private OutputStream outputStream = null;
        private DataOutputStream dataOutputStream = null;

        // STATIC FUNCTION
        /**
         * This function is used by a client to send the information needed by the server to process the file
         * @param out Socket stream connected to the server where the data are written
         * @param requestId unique ID of the request
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

        public ClientThread(int iClient, Socket clientSocket)
        {
            this.iClient = iClient;
            this.clientSocket = clientSocket;
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
        }


        @Override
        public void run()
        {
            // ==========================================================
            //                          SENDING
            // ==========================================================
            System.out.println("Run ClientSender");
            try
            {
                int fileIdx = 0;
                String password = foldToSend.passwords[fileIdx];
                File encryptedFile = new File( foldToSend.getPath("files-encrypted", fileIdx));
                InputStream inFile = new FileInputStream(encryptedFile);

                // SEND THE PROCESSING INFORMATION AND FILE
                byte[] hashPwd = hashSHA1(password);
                int pwdLength = password.length();
                long fileLength = encryptedFile.length();

                sendRequest(dataOutputStream, iClient, hashPwd, pwdLength, fileLength);
                dataOutputStream.flush();
                FileManagement.sendFile(inFile, dataOutputStream);
                startTimes.put(iClient, System.currentTimeMillis());
                System.out.println("Client sends : (requestId, hashPwd, pwdLength, fileLength) = (" + iClient + ", " + hashPwd + ", " + pwdLength + ", " + fileLength + ")");
                inFile.close();
            }
            catch (NoSuchAlgorithmException | IOException e)
            {
                e.printStackTrace();
            }
            //System.out.println("End ClientSender");

            // ==========================================================
            //                          RECEIVING
            // ==========================================================

            //System.out.println("Run ClientReceiver");
            try
            {
                int requestId = dataInputStream.readInt();
                long fileLengthServer = dataInputStream.readLong();
                System.out.println("Client receives : (requestId, fileLength) = (" + requestId + ", " + fileLengthServer + ")");

                File decryptedClient = new File("tmp/file-" + requestId % foldToSend.nbFiles + "-decrypted-client" + ".bin");
                OutputStream outFile = new FileOutputStream(decryptedClient);
                FileManagement.receiveFile(inputStream, outFile, fileLengthServer);

                long deltaTime = System.currentTimeMillis() - startTimes.get(requestId);
                System.out.println("\t Time observed by the client "+requestId+": " + deltaTime + "ms");

                inputStream.close();
                outputStream.close();
                clientSocket.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            //System.out.println("End ClientReceiver");
        }
    }
}