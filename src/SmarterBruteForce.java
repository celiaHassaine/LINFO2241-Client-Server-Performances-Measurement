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

    public SmarterBruteForce(int pwdLength, byte[] hashPwd, Map<String, String> dictionary)
    {
        super(pwdLength, hashPwd);
        this.dictionary = dictionary;
        System.out.println("Started SmarterBruteForce");
        if(dictionary.containsKey(Arrays.toString(super.hashPwd))) // password already present in the dictionary
        {
            System.out.println("Password found in the dictionary");
            super.found = true;
            super.password = dictionary.get(super.hashPwd);
        }
        else
        {
            System.out.println("Password NOT found in the dictionary");
            super.bruteForce(0);
        }
        System.out.println("Ended SmarterBruteForce");
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, PasswordNotFoundException
    {
        try
        {
            String password = "starwars";                       // starwars is in the 10k-most-common file
            byte[] hashPwd = hashSHA1(password);

            String filename = "files/10k-most-common_filered.txt";
            Map<String, String> dic = null;
            try
            {
                dic = ServerMain.createDictionary(filename);
            }
            catch (DictionaryCreationException e)
            {
                e.printStackTrace();
            }
            
            BruteForce bruteForce = new SmarterBruteForce(password.length(),hashPwd, dic);
            String pwd = "";
            try{
                pwd = bruteForce.getPassword();
                System.out.println("PASSWORD FOUND: " + pwd);
            }
            catch (PasswordNotFoundException exception)
            {
                exception.err();
            }
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
    }
}
