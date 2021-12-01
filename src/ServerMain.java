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

    public static void main(String[] args)
    {
        boolean listening = true;
        int portNumber = 3333;  // default port number
        if(args.length == 1)    // port number specified in command line
        {
            portNumber = Integer.parseInt(args[0]);
        }

        try (ServerSocket serverSocket = new ServerSocket(portNumber)) // TODO: add second arg backlog to record maximum number of incoming traffic
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
        //TODO: Create a decryptedFile and networkFile for each client
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
                    BF = new SmarterBruteForce(pwdLength, hashPwd, new File("")); //TODO: CHANGER DE PLACE CREATION DU DICTIONNAIRE
                String pwdFound = "";
                try
                {

                    pwdFound = BF.bruteForce();
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
