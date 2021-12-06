import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class ServerMain
{
    private static Map<String, String> dictionary;

    /**
     * @param in Stream from which to read the request
     * @return Request with information required by the server to process encrypted file
     */
    public static Request readRequest(DataInputStream in) throws IOException {
        byte [] hashPwd = new byte[20];
        int count = in.read(hashPwd,0, 20);
        if (count < 0){
            throw new IOException("Server could not read from the stream");
        }
        int pwdLength = in.readInt();
        long fileLength = in.readLong();

        return new Request(hashPwd, pwdLength, fileLength);
    }

    /**
     * This function hashes a string with the SHA-1 algorithm
     * @param data The string to hash
     * @return An array of 20 bytes which is the hash of the string
     * provided in the template project
     */
    public static byte[] hashSHA1(String data) throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        return md.digest(data.getBytes());
    }

    /**
     * This function read the file containing common passwords. For each of them, the function
     * computes its hash and stores it in a hashmap for further use.
     * @param filename is the filename of a txt file containing a list of common passwords
     * @return true if the creation of the dictionary succeeded, false otherwise.
     */
    public static Map<String, String> createDictionary(String filename) throws DictionaryCreationException
    {
        dictionary = new HashMap<>();

        Scanner myReader = null;
        myReader = new Scanner(filename);

        while (myReader.hasNextLine())
        {
            String data = myReader.nextLine();
            byte[] hash = new byte[20];
            try
            {
                hash = hashSHA1(data);
            }
            catch (NoSuchAlgorithmException e)
            {
                e.printStackTrace();
            }
            dictionary.put(Arrays.toString(hash), data); // add the hash-password pair in the dictionary
        }
        myReader.close();
        return dictionary;
    }


    public static void main(String[] args)
    {
        // Initialization of the dictionary
        String filename = "files/10k-most-common_filered.txt";
        try
        {
            createDictionary(filename);
        }
        catch (DictionaryCreationException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        // Initialization the connection of the server
        boolean listening = true;
        int portNumber = 3333;  // default port number
        if(args.length == 1)    // port number specified in command line
        {
            portNumber = Integer.parseInt(args[0]);
        }

        try (ServerSocket serverSocket = new ServerSocket(portNumber))
        // TODO: add second arg backlog to record maximum number of incoming traffic
        {
            while (listening)
            // while listening and maximal number of queuing capacity of the OS's queue for incoming TCP
            {
                System.out.println("Waiting connection");
                // Accept a connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connection from: " + clientSocket);

                // create a thread to deal with each client;
                ClientHandler clientHandler = new ClientHandler(clientSocket, true);
                clientHandler.start();
            }
        }
        catch (IOException e)
        {
            System.err.println("Could not listen on port " + portNumber);
            System.exit(-1);
        }
    }

    private static class ClientHandler extends Thread
    {
        private Socket socket = null;
        private boolean isSmart;


        File decryptedFile = new File("files/test_file-decrypted-server.pdf");
        File networkFile = new File("temp-server.pdf");

        public ClientHandler(Socket socket, boolean isSmart)
        {
            super("ClientHandlerThread");
            this.socket = socket;
            this.isSmart = isSmart;
        }

        @Override
        public void run()
        {
            DataOutputStream outSocket;

            try
            {
                // Stream to read request from socket
                InputStream inputStream = socket.getInputStream();
                DataInputStream dataInputStream = new DataInputStream(inputStream);

                // Stream to write response to socket
                outSocket = new DataOutputStream(socket.getOutputStream());

                // Stream to write the file to decrypt
                OutputStream outFile = new FileOutputStream(networkFile);
                Request request = readRequest(dataInputStream);
                long fileLength = request.getLengthFile();

                FileManagement.receiveFile(inputStream, outFile, fileLength);

                /*
                int readFromFile = 0;
                int bytesRead = 0;
                byte[] readBuffer = new byte[64];

                System.out.println("[Server] File length: "+ fileLength);
                while((readFromFile < fileLength))
                {
                    bytesRead = inputStream.read(readBuffer);
                    readFromFile += bytesRead;
                    outFile.write(readBuffer, 0, bytesRead);
                }
                */

                System.out.println("File length: " + networkFile.length());

                // BRUTEFORCE:
                // Password is determined by using a bruteforce method implemented in the class BruteForce
                int pwdLength = request.getLengthPwd();
                byte[] hashPwd = request.getHashPassword();

                System.out.println("-- Starting bruteforce -- ");
                BruteForce BF;
                if(this.isSmart)
                    BF = new DumbBruteForce(pwdLength, hashPwd);
                else
                    BF = new SmarterBruteForce(pwdLength, hashPwd, dictionary);
                String pwdFound = "";
                try
                {
                    pwdFound = BF.getPassword();
                    System.out.println("PASSWORD FOUND: " + pwdFound);

                    System.out.println("-- End bruteforce -- ");
                    SecretKey serverKey = CryptoUtils.getKeyFromPassword(pwdFound);
                    CryptoUtils.decryptFile(serverKey, networkFile, decryptedFile);

                }
                catch (PasswordNotFoundException e)
                {
                    e.printStackTrace();
                    e.err();
                    return;
                }


                // Send the decryptedFile
                InputStream inDecrypted = null;
                inDecrypted = new FileInputStream(decryptedFile);
                outSocket.writeLong(decryptedFile.length());
                outSocket.flush();
                FileManagement.sendFile(inDecrypted, outSocket);

                /*
                int readCount;
                byte[] buffer = new byte[64];
                //read from the file and send it in the socket
                while ((readCount = inDecrypted.read(buffer)) > 0){
                outSocket.write(buffer, 0, readCount);
                }*/

                dataInputStream.close();
                inputStream.close();
                inDecrypted.close();
                outFile.close();
                socket.close();
            }
            catch (IOException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | InvalidKeySpecException | BadPaddingException | InvalidKeyException e)
            {
                e.printStackTrace();
            }
        }
    }
}
