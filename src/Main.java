import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;


public class Main
{
    // SERVER
    private static final String serverIpAddress = "81.241.22.42"; //"localhost";
    public static final int portNumber = 3333;

    // CLIENT PARAMETERS
    // Measure parameter
    public static final boolean SMART = true;
    private static final int RATE = 5;   // # request/s
    public static final int PWDLEN = 4;     // 4+rnd.nextInt(2);
    private static final int FOLDIDX = 3; // index of folder to encrypt


    // Measure parameter
    private static FileWriter fileWriter = null;
    // Encryption parameters
    private static final Encryptor.Folder foldToSend = Encryptor.folders[FOLDIDX];
    // Request parameters
    private static final int nbClients = 100;
    private static int nReceived = 0;
    private static ReentrantLock lock = new ReentrantLock();

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
        }
        catch (IOException e)
        {
            System.err.println("ERROR CSV file creation");
            e.printStackTrace();
        }
    }

    public static void savePasswords(String[] passwords)
    {
        try
        {
            PrintWriter pr = new PrintWriter( "measures/passwords.txt");

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

    public static void loadPasswords()
    {
        try
        {
            Reader reader = new FileReader("measures/passwords.txt");
            BufferedReader bufferedReader = new BufferedReader(reader);
            for (int i = 0; i < nbClients; i++)
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
        measureSetup("measures/" + "measure-smart" + (SMART ? 1 : 0) + "-rate" + RATE + "-pwdLen" + PWDLEN + ".csv");

        // Encryption of folder foldIdx
        //Encryptor.main(new String[]{FOLDIDX + ""});
        // savePasswords(foldToSend.passwords);

        loadPasswords();

        try
        {
            Reader reader = new FileReader("measures/passwords.txt");
            BufferedReader bufferedReader = new BufferedReader(reader);
            for (int i = 0; i < nbClients; i++)
            {
                foldToSend.passwords[i] = bufferedReader.readLine();
            }
            reader.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }


        double start_time = getCurrentTime(); // Start time in seconds
        double deltaTime = 0.0;
        double inter_request_time = nextExp(RATE);

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
                    inter_request_time = nextExp(RATE);
                    start_time = getCurrentTime();
                    iClient++;
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    System.err.println("Failed to connect to server");
                    System.exit(-1);
                }
                //System.out.println("Socket created");
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
            //System.out.println("Run ClientSender");
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
                fileWriter.write(deltaTime + ",");

                lock.lock();
                try
                {
                    nReceived++;
                }
                finally
                {
                    lock.unlock();
                }

                inputStream.close();
                outputStream.close();
                clientSocket.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }


            if(nReceived == nbClients)
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
            //System.out.println("End ClientReceiver");
        }
    }
}