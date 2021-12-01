import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class DumbBruteForce extends BruteForce
// TODO: check extends Callable for the smarter version of the server
{
    public DumbBruteForce(int pwdLength, byte[] hashPwd)
    {
        super(pwdLength, hashPwd);
    }

    /**
     * This function is used to test the implementation of the bruteforce method
     * @param args
     */
    public static void main(String[] args) throws NoSuchAlgorithmException {
        try
        {
            String password = "test";
            byte[] hashPwd = hashSHA1(password);
            BruteForce bruteForce = new DumbBruteForce(password.length(),hashPwd);
            bruteForce.bruteForce();
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
        catch (NoSuchAlgorithmException | PasswordNotFoundException e)
        {
            e.printStackTrace();
        }
    }

}
