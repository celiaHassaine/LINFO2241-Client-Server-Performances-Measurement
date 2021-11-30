import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class BruteForce
// TODO: check extends Callable for the smarter version of the server
{
    private int pwdLength;
    private byte[] hashPwd;
    private char[] guess;
    private boolean found;
    private String password;

    public BruteForce(int pwdLength, byte[] hashPwd)
    {
        this.pwdLength = pwdLength;
        this.hashPwd = hashPwd;

        this.guess = new char[pwdLength];
        this.found = false;
        this.password = "";
    }

    /**
     * This function hashes a string with the SHA-1 algorithm
     * @param data The string to hash
     * @return An array of 20 bytes which is the hash of the string
     * provided in the template project
     */
    public static byte[] hashSHA1(String data) throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        return md.digest(data.getBytes());
    }

    public void bruteForce()
    {
        bruteForce(0);
    }

    /**
     * This function attempt to discover the password that gives the same SHA-1 as the one in arguments
     * @param i index of the character to increment in the array guess
     */
    private void bruteForce(int i)
    {
        if (i == pwdLength)
        {
            try
            {
                String str = String.copyValueOf(guess);
                byte[] hashGuessed = hashSHA1(str);
                if (Arrays.equals(hashGuessed,hashPwd))
                {
                    this.found = true;
                    this.password = str;
                }
                return;

            }
            catch (NoSuchAlgorithmException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            // TODO: https://www.javatpoint.com/java-ascii-table
            // change the for loop by : for(char c = ' '; c <= '~' && !found; c++) to handle all characters
            //
            for(char c = 'a'; c <= 'z' && !found; c++)
            {
                guess[i] = c;
                bruteForce(i+1);
            }
        }
    }

    /**
     * This function returns the password found by bruteforce method. It throws a PasswordNotFoundException
     * if the password could not be found.
     * @return String password found of throws an exception otherwise
     * @throws PasswordNotFoundException
     */
    public String getPassword() throws PasswordNotFoundException
    {
        if(password.equals(""))
            throw new PasswordNotFoundException();
        else
            return password;
    }

    /**
     * This function is used to test the implementation of the bruteforce method
     * @param args
     */
    public static void main(String[] args)
    {
        try
        {
            String password = "abcd";
            byte[] hashPwd = hashSHA1(password);
            BruteForce bruteForce = new BruteForce(password.length(),hashPwd);
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
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
    }

}
