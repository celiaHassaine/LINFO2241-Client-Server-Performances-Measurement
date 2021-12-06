import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public class DumbBruteForce extends BruteForce
// TODO: check extends Callable for the smarter version of the server
{
    public DumbBruteForce(int pwdLength, byte[] hashPwd)
    {
        super(pwdLength, hashPwd);
        System.out.println("Started DumbBruteForce");
        super.bruteForce(0);
        System.out.println("Ended DumbBruteForce");
    }

    /**
     * This function is used to test the implementation of the bruteforce method
     * @param args
     */
    public static void main(String[] args)
    {
        try
        {
            String password = "test";
            byte[] hashPwd = hashSHA1(password);
            BruteForce bruteForce = new DumbBruteForce(password.length(),hashPwd);

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
