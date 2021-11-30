import java.io.File;
import java.io.FileNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class SmarterBruteForce extends BruteForce {
    private int pwdLength;
    private byte[] hashPwd;
    private char[] guess;
    private boolean found;
    private String password;
    private Map<byte[], String> dictionary;
    public SmarterBruteForce(int pwdLength, byte[] hashPwd, File file) throws FileNotFoundException, NoSuchAlgorithmException
    {
        super(pwdLength, hashPwd);
        this.dictionary = new HashMap<>();
        this.computeDictionary(file);
    }

    private void computeDictionary(File file) throws FileNotFoundException, NoSuchAlgorithmException {
        Scanner myReader = new Scanner(file);
        while (myReader.hasNextLine())
        {
            String data = myReader.nextLine();
            this.dictionary.put(hashSHA1(data), data);
        }
        myReader.close();
    }

    @Override
    // TODO : make bruteForce returns sth instead of just changing instances variables
    public void bruteForce() {
        if (this.dictionary.containsKey(hashPwd))
        {
            this.found = true;
            this.password = this.dictionary.get(hashPwd);
        }

        else
        {
            super.bruteForce();
        }
    }

    public static void main(String[] args)
    {
        System.out.println("Hello world");
    }
}
