import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.LinkedList;
import java.util.Random;

public class Encryptor {
    public static final Folder[] folders = new Folder[] {new Folder("20KB", 5),
            new Folder("50KB", 5), new Folder("100KB", 5),
            new Folder("5MB", 5), new Folder("50MB", 2)};
    public static final double dicRatio = 0.2;

    public static class Folder
    {
        public final String size;
        public final int nbFiles;
        public String[] passwords;
        public static final int nRep = 20;
        public Folder(String size, int nbFiles)
        {
            this.size = size;
            this.nbFiles = nbFiles;
            this.passwords = new String[nbFiles*nRep];
        }

        /**
         * Returns the path to the file rootFolder+"/Files-"+size+"/"+"file-"+i
         * @param rootFolder files or files-encrypted
         * @param i index of the file
         */
        public String getPath(String rootFolder, int i)
        {
            return rootFolder+"/Files-"+size+"/"+"file-"+i+".bin";
        }

        public void computePasswords()
        {
            Random rnd = new Random(42);
            for (int i = 0; i < this.nbFiles * nRep; i++)
            {
                Double d = rnd.nextDouble();
                System.out.println(d);
                if (d < dicRatio)
                {
                    this.passwords[i] = "hello";  // Word from the dictionary
                }
                else
                {
                    this.passwords[i] = randomStringGenerator(5);
                }
            }
        }
    }


    public static void main(String[] args) {
        //encryptFolder(folders[0]);
        encryptFolder(folders[Integer.parseInt(args[0])]);
        System.out.println("Encryption finished");
    }

    /**
     * This function encrypt the nbFiles contained in the folder f and places the encrypted files
     * in the good folder
     */
    public static void encryptFolder(Folder f)
    {
        f.computePasswords();
        for(int i = 0; i < f.nbFiles * Folder.nRep; i++)
        {
            try
            {
                String password = f.passwords[i];
                int fileIdx = 1+(i % f.nbFiles);
                File fileToEncrypt = new File(f.getPath("files", fileIdx));
                SecretKey keyGenerated = CryptoUtils.getKeyFromPassword(password);
                File encryptedFile = new File(f.getPath("files-encrypted", i));

                CryptoUtils.encryptFile(keyGenerated, fileToEncrypt, encryptedFile);
                System.out.println(encryptedFile.getName() + " of size " + encryptedFile.length() + " encrypted with password: " + password);
            }
            catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException | IOException | BadPaddingException | InvalidKeyException e)
            {
                e.printStackTrace();
                System.err.println("EncryptFolder exception: unable to encrypt files in " + f.getPath("files-encrypted", i));
            }
        }
    }

    /**
     * Encrypt all files that are in the folders
     */
    public static void encryptFolders() {
        for (Folder f: folders) {
            encryptFolder(f);
        }
    }

    /**
     * This function is used to generate random password in order to encrypt files.
     * @param length Length of the string to generate
     * @return A random string of length specified as argument
     */
    public static String randomStringGenerator(int length)
    {
        StringBuilder seedChars= new StringBuilder();
        // TODO: change the for loop by : for(char c = ' '; c <= '~' && !found; c++) to handle all characters https://www.javatpoint.com/java-ascii-table
        for(char c = 'a'; c <= 'z'; c++)
        {
            seedChars.append(c);
        }

        StringBuilder sb = new StringBuilder();
        Random rand = new Random();
        for(int i = 0; i< length; i++)
        {
            sb.append(seedChars.charAt(rand.nextInt(seedChars.length())));
        }
        return sb.toString();
    }
}
