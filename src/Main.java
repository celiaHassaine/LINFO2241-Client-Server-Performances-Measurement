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


//TODO: TASK 1
//TODO: Déplacer la création du dictionnaire en dehors des threads: Corentin
//TODO: communiquer entre 2 pc différents sur un réseaux non local: Merlin
//TODO: SmartestBruteForce ? Merlin & Corentin


//TODO: TASK 2
//TODO: implement a small client ap sending request at random times (choose some distribution) : Corentin
//TODO: implement client application that open multiple TCP connections with server: Merlin
//TODO: make some measurements with different request rate, difficulities : Corentin
//TODO: compare with the 2 implementations of the bruteforce: Merlin

//TODO: TASK 3


//TODO: TASK 4


public class Main {

    /**
     * This function hashes a string with the SHA-1 algorithm
     * @param data The string to hash
     * @return An array of 20 bytes which is the hash of the string
     */
    public static byte[] hashSHA1(String data) throws NoSuchAlgorithmException{
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return md.digest(data.getBytes());
    }

    /**
     * This function is used by a client to send the information needed by the server to process the file
     * @param out Socket stream connected to the server where the data are written
     * @param hashPwd SHA-1 hash of the password used to derive the key of the encryption
     * @param pwdLength Length of the clear password
     * @param fileLength Length of the encrypted file
     */
    public static void sendRequest(DataOutputStream out, byte[] hashPwd, int pwdLength,long fileLength)
            throws IOException
    {
        out.write(hashPwd,0, 20);
        out.writeInt(pwdLength);
        out.writeLong(fileLength);
    }

    public static void main(String[] args) {

        boolean creating = true;
        int portNumber = 3333;
        int nbThreads = 1;
        int iThread = 0;

        while(creating && iThread < nbThreads)
        {
            System.out.println("Client creation");

            // create a thread for each client;
            ClientThread clientHandler = new ClientThread(portNumber);
            clientHandler.start();
            iThread++;
        }

    }

    private static class ClientThread extends Thread
    {
        private int portNumber;
        //TODO: Create a decryptedFile and networkFile for each client

        public ClientThread(int portNumber)
        {
            super("ClientHandlerThread");
            this.portNumber = portNumber;
        }

        @Override
        public void run()
        {
            // TODO: change file from moodle example
            try{
                String password = "test";
                SecretKey keyGenerated = CryptoUtils.getKeyFromPassword(password);

                File inputFile = new File("files/test_file.pdf");
                File encryptedFile = new File("files/test_file-encrypted-client.pdf");
                File decryptedClient = new File("files/test_file-decrypted-client.pdf");

                // This is an example to help you create your request
                CryptoUtils.encryptFile(keyGenerated, inputFile, encryptedFile);
                System.out.println("Encrypted file length: " + encryptedFile.length());


                // Creating socket to connect to server (in this example it runs on the localhost on port 3333)
                Socket socket = new Socket("localhost", this.portNumber);
                System.out.println("Socket created");


                // For any I/O operations, a stream is needed where the data are read from or written to. Depending on
                // where the data must be sent to or received from, different kind of stream are used.
                OutputStream outSocket = socket.getOutputStream();
                DataOutputStream out = new DataOutputStream(outSocket);

                InputStream inFile = new FileInputStream(encryptedFile);
                DataInputStream inSocket = new DataInputStream(socket.getInputStream());


                // SEND THE PROCESSING INFORMATION AND FILE
                byte[] hashPwd = hashSHA1(password);
                int pwdLength = password.length();
                long fileLength = encryptedFile.length();
                sendRequest(out, hashPwd, pwdLength, fileLength);
                out.flush();

                FileManagement.sendFile(inFile, out);
                /*
                int readCount;
                byte[] buffer = new byte[64];
                //read from the file and send it in the socket
                while ((readCount = inFile.read(buffer)) > 0){
                    out.write(buffer, 0, readCount);
                }*/

                // GET THE RESPONSE FROM THE SERVER
                OutputStream outFile = new FileOutputStream(decryptedClient);
                long fileLengthServer = inSocket.readLong();
                System.out.println("Length from the server: "+ fileLengthServer);
                FileManagement.receiveFile(inSocket, outFile, fileLengthServer);

                /*
                int readFromSocket = 0;
                int byteRead;
                byte[] readBuffer = new byte[64];
                while(readFromSocket < fileLengthServer){
                    byteRead = inSocket.read(readBuffer);
                    readFromSocket += byteRead;
                    outFile.write(readBuffer, 0, byteRead);
                }*/

                out.close();
                outSocket.close();
                outFile.close();
                inFile.close();
                inSocket.close();
                socket.close();

            } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidAlgorithmParameterException |
                    NoSuchPaddingException | IllegalBlockSizeException | IOException | BadPaddingException |
                    InvalidKeyException e) {
                e.printStackTrace();
            }
        }
    }
}