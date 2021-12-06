import java.io.File;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class SmarterBruteForce extends BruteForce
{
    private Map<String, String> dictionary;
    public SmarterBruteForce(int pwdLength, byte[] hashPwd, Map<String, String> d) throws NoSuchAlgorithmException
    {
        super(pwdLength, hashPwd);
        this.dictionary = d;
    }

    public static HashMap<String, String> computeDictionary(File file) throws NoSuchAlgorithmException
    {
        HashMap<String, String> dictionary = new HashMap<>();
        Scanner myReader = null;
        try {
            myReader = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        while (myReader.hasNextLine())
        {
            String data = myReader.nextLine();
            byte[] hash = hashSHA1(data);
            dictionary.put(Arrays.toString(hash), data);
        }
        myReader.close();
        return dictionary;
    }

    @Override
    protected void putDictionary(byte[] hash, String str)
    {
        this.dictionary.put(Arrays.toString(hash), str);
    }

    @Override
    protected String checkDictionary()
    {
        if (this.dictionary.containsKey(Arrays.toString(hashPwd)))
        {
            System.out.println("Found in dictionary!");
            return this.dictionary.get(Arrays.toString(hashPwd));
        }
        return "";
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, PasswordNotFoundException {
        String password = "starwars"; // starwars is in the 10k-most-common file
        byte[] hashPwd = hashSHA1(password);
        File file = new File("files/10k-most-common_filered.txt");
        HashMap<String, String> dic = SmarterBruteForce.computeDictionary(file);
        SmarterBruteForce SBF = new SmarterBruteForce(password.length(), hashPwd, dic);
        String pwd_found = SBF.bruteForce();
        System.out.println("Password found : " + pwd_found);
    }
}
