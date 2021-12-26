import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;


public class Main
{
    // SERVER PARAMETERS
    private static final String serverIpAddress = "localhost";//"81.241.22.42";
    public static final int portNumber = 3333;

    // CLIENT PARAMETERS
    // Measure parameters
    public static final boolean SMART = false;
    private static final double RATE = 1;          // # request/s
    public static final int PWDLEN = 3;
    private static final int FOLDIDX = 0;       // index of folder to encrypt
    private static final int nClients = 100;

    // STATIC VARIABLES
    private static final HashMap<Integer, Long> startTimes = new HashMap<>();  // dictionary to store the time taken for each request (dico<requestID, send time>)
    private static FileWriter fileWriter = null;                               // fileWriter used to export measurements
    private static final Encryptor.Folder foldToSend = Encryptor.folders[FOLDIDX]; // folder encrypted
    private static int nReceived = 0;                        // Number of responses received (used to turn off the client)
    private static ReentrantLock lock = new ReentrantLock(); // Lock to protect the variable nReceived shared between threads

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
     * This function instantiates the static fileWriter to store future measures in the filePath
     * @param filePath FilePath is the path towards the file to store into the response times.
     */
    public static void measureSetup(String filePath)
    {
        File file = new File(filePath);
        try
        {
            fileWriter = new FileWriter(file);
        }
        catch (IOException e)
        {
            System.err.println("ERROR file creation");
            e.printStackTrace();
        }
    }

    /**
     * This function saves the content of the static string array passwords (in the Encryptor class) into a new txt file.
     * This allows to redo test with exactly the same encrypted files.
     * @param passwords the static array string in Encryptor class storing the passwords used to encrypt files
     */
    public static void savePasswords(String[] passwords)
    {
        try
        {
            PrintWriter pr = new PrintWriter( "measures/passwords" + PWDLEN + ".txt");

            for (int i=0; i<passwords.length ; i++)
            {
                pr.println(passwords[i]);
            }
            pr.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("No such file exists.");
        }
    }

    /**
     * This function reads the content of the file created by the function savePasswords() and loads it
     * into the string array password in the Encryptor class.
     */
    public static void loadPasswords()
    {
        try
        {
            Reader reader = new FileReader("measures/passwords" + PWDLEN + ".txt");
            BufferedReader bufferedReader = new BufferedReader(reader);
            for (int i = 0; i < nClients; i++)
            {
                foldToSend.passwords[i] = bufferedReader.readLine();
            }
            reader.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Returns a random double sample following an exponential distribution
     * @param rate rate of the exponential distribution (number of events per second)
     */
    public static double nextExp(double rate)
    {
        Random rnd = new Random();
        return -(1/rate)*Math.log(rnd.nextDouble());
    }

    /**
     * This function returns the current time in seconds.
     */
    public static double getCurrentTime() {
        return System.currentTimeMillis()/1000.0;
    }

    public static void main(String[] args)
    {
        measureSetup("measures/" + "measure-smart" + (SMART ? 1 : 0) + "-rate" + RATE + "-pwdLen" + PWDLEN + ".csv");

        // Encryption
        Encryptor.main(new String[]{FOLDIDX + ""});
        savePasswords(foldToSend.passwords);

        // Uncomment following line and comment the 2 previous lines to load previously passwords used to encrypt the foldIdx th folder.
        //loadPasswords();

        double start_time = getCurrentTime();
        double deltaTime;
        double inter_request_time = nextExp(RATE);

        int iClient =0;
        while(iClient < nClients)
        {
            deltaTime = getCurrentTime() - start_time;
            if (deltaTime >= inter_request_time)
            {
                // Connection with server
                try
                {
                    Socket clientSocket = new Socket(serverIpAddress, portNumber);
                    ClientThread clientThread = new ClientThread(iClient, clientSocket);
                    clientThread.start();
                    inter_request_time = nextExp(RATE);
                    start_time = getCurrentTime();
                    iClient++;
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    System.err.println("Failed to connect to server");
                }
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

        /**
         * This method is called when clientThread.start() is executed. Each thread deals with the sending
         * of a request and the reception from the server.
         */
        @Override
        public void run()
        {
            // ==========================================================
            //                          SENDING
            // ==========================================================
            try
            {
                String password = foldToSend.passwords[iClient];
                File encryptedFile = new File( foldToSend.getPath("files-encrypted", iClient));
                InputStream inFile = new FileInputStream(encryptedFile);

                // SEND THE PROCESSING INFORMATION AND FILE
                byte[] hashPwd = hashSHA1(password);
                int pwdLength = password.length();
                long fileLength = encryptedFile.length();

                sendRequest(dataOutputStream, iClient, hashPwd, pwdLength, fileLength);
                dataOutputStream.flush();
                FileManagement.sendFile(inFile, dataOutputStream);
                startTimes.put(iClient, System.currentTimeMillis()); // Place the sending time in the dictionary
                System.out.println("Client sends : (requestId, hashPwd, pwdLength, fileLength) = (" + iClient + ", " + hashPwd + ", " + pwdLength + ", " + fileLength + ")");
                inFile.close();
            }
            catch (NoSuchAlgorithmException | IOException e)
            {
                e.printStackTrace();
            }

            // ==========================================================
            //                          RECEIVING
            // ==========================================================
            try
            {
                int requestId = dataInputStream.readInt();
                long fileLengthServer = dataInputStream.readLong();
                System.out.println("Client receives : (requestId, fileLength) = (" + requestId + ", " + fileLengthServer + ")");

                File decryptedClient = new File("tmp/file-" + requestId % foldToSend.nbFiles + "-decrypted-client" + ".bin");
                OutputStream outFile = new FileOutputStream(decryptedClient);
                FileManagement.receiveFile(inputStream, outFile, fileLengthServer);


                long deltaTime = System.currentTimeMillis() - startTimes.get(requestId); // Compute delay
                System.out.println("\t Time observed by the client "+requestId+": " + deltaTime + "ms");
                fileWriter.write(deltaTime + ",");

                lock.lock();
                try { nReceived++; }
                finally { lock.unlock(); }

                inputStream.close();
                outputStream.close();
                clientSocket.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }


            if(nReceived == nClients)
            {
                try
                {
                    fileWriter.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}