import java.io.File;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class SmarterBruteForce extends BruteForce
{
    /**
     * This function read the file containing common passwords. For each of them, the function
     * computes its hash and stores it in a hashmap for further use.
     * @param filename is the filename of a txt file containing a list of common passwords
     * @return true if the creation of the dictionary succeeded, false otherwise.
     */
    public static Map<String, String> createDictionary(String filename) throws DictionaryCreationException
    {
        HashMap<String,String> dictionary = new HashMap<>();

        Scanner myReader = null;
        try
        {
            myReader = new Scanner(new File(filename));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

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


    private Map<String, String> dictionary;

    public SmarterBruteForce(int pwdLength, byte[] hashPwd, Map<String, String> dictionary)
    {
        super(pwdLength, hashPwd);
        this.dictionary = dictionary;

        if(this.dictionary.containsKey(Arrays.toString(super.hashPwd))) // password already present in the dictionary
        {
            System.out.println("Password found in the dictionary");
            super.found = true;
            super.password = dictionary.get(Arrays.toString(super.hashPwd));
        }
        else
        {
            System.out.println("Password NOT found in the dictionary");
            super.bruteForce(0);
        }
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, PasswordNotFoundException, DictionaryCreationException
    {

        String password = "starwars";                       // starwars is in the 10k-most-common file
        byte[] hashPwd = hashSHA1(password);

        String filename = "files/10k-most-common_filered.txt";
        Map<String, String> dic = null;
        dic = createDictionary(filename);

        BruteForce bruteForce = new SmarterBruteForce(password.length(),hashPwd, dic);
        String pwd = "";
        pwd = bruteForce.getPassword();
        System.out.println("PASSWORD FOUND: " + pwd);
    }
}
