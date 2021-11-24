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


public class ServerMain
{

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

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException
    {
        // TODO: Change hardcoded password
        boolean listening = true;
        int portNumber = 3333; // default port number
        if(args.length == 1) // port number specified in command line
        {
            portNumber = Integer.parseInt(args[0]);
        }

        try (ServerSocket serverSocket = new ServerSocket(portNumber))
        {
            while (listening)
            // while listening and maximal number of queuing capacity of the OS's queue for incoming TCP
            {
                System.out.println("Waiting connection");

                // Accept a connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connection from: " + clientSocket);

                // create a thread to deal with each client;
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.start();
            }
        }
        catch (IOException e)
        {
            System.err.println("Could not listen on port " + portNumber);
            System.exit(-1);
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket = null;
        //TODO: Create a decryptedFile and networkFile for each client
        File decryptedFile = new File("test_file-decrypted-server.pdf");
        File networkFile = new File("temp-server.pdf");

        public ClientHandler(Socket socket)
        {
            super("ClientHandlerThread");
            this.socket = socket;
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
                while((readFromFile < fileLength)){
                bytesRead = inputStream.read(readBuffer);
                readFromFile += bytesRead;
                outFile.write(readBuffer, 0, bytesRead);
                }*/

                System.out.println("File length: " + networkFile.length());

                //TODO: HERE THE PASSWORD IS HARDCODED, YOU MUST REPLACE THAT WITH THE BRUTEFORCE PROCESS
                //String password = "test";
                String password = guessPassword(request.getHashPassword(), request.getLengthPwd());

                SecretKey serverKey = CryptoUtils.getKeyFromPassword(password);
                CryptoUtils.decryptFile(serverKey, networkFile, decryptedFile);


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



    // TODO: IMPLEMENT BRUTE FORCE
    // TODO: USE most commonly used passwords list given to be optimal
    /**
     * This function try to retrieve the initial string before beeing hashed. It uses a brute-force method
     * to accomplish it
     * @param hashPwd SHA-1 hash of the password used to derive the key of the encryption
     * @param pwdLength Length of the clear password
     */
    public static String guessPassword(byte[] hashPwd, int pwdLength)
    {
        char[] guess = new char[pwdLength];
        byte[] hashGuess;

        for(int i = 32; i < 128; i++) // usual characters begin at number 32 in ASCII table
        {
            for(int j=0; j < pwdLength; j++)
            {
                guess[j] = (char) i;
            }
        }


            hashGuess = ???;
            if(hashGuess.equals(hashPwd))
            {
                return guess.toString();
            }
        }
        return "";
    }
}
