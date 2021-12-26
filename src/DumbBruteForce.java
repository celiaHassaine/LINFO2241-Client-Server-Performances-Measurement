import java.security.NoSuchAlgorithmException;

public class DumbBruteForce extends BruteForce
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
            String password = "linfoo";
            byte[] hashPwd = hashSHA1(password);
            BruteForce bruteForce = new DumbBruteForce(password.length(),hashPwd);

            String pwd = "";
            try{
                pwd = bruteForce.getPassword();
                System.out.println("PASSWORD FOUND: " + pwd);
            }
            catch (PasswordNotFoundException exception)
            {
                exception.details();
            }

        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
    }
}
