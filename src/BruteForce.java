import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

abstract class BruteForce
{
    // STATIC METHODS
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

    // INSTANCE VARIABLES
    protected int pwdLength;
    protected byte[] hashPwd;
    protected char[] guess;
    protected boolean found;
    protected String password;

    public BruteForce(int pwdLength, byte[] hashPwd)
    {
        this.pwdLength = pwdLength;
        this.hashPwd = hashPwd;
        this.guess = new char[pwdLength];
        this.found = false;
        this.password = "";
    }

    public String getPassword() throws PasswordNotFoundException
    {
        if(password.equals(""))
            throw new PasswordNotFoundException();
        else
            return password;
    }

    /**
     * This function attempt to discover the password that gives the same SHA-1 as hashPwd
     * During his computation
     * @param i index of the character to increment in the array guess
     * @return
     */
    public String bruteForce(int i)
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
                    return this.password;
                }

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
            for(char c = 'a'; c <= 'z' && !found; c++)
            {
                guess[i] = c;
                bruteForce(i+1);
            }
        }
        return this.password;
    }
}
