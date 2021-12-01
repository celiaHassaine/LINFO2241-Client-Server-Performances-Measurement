import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

abstract class BruteForce {
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

    /**
     * This function will serve in the SmarterBruteForce
     * Adds the pair (hash(str), str) to the dictionary
     * @param hash hash(str) to add as key in the dictionary
     * @param str the string to add as value in the dictionary
     */
    protected void putDictionary(byte[] hash, String str)
    {
        return;
    }

    /**
     * This function will serve in the SmarterBruteForce
     * Check if the given hashPwd is in the dictionary
     * @return A string containing "" if hashPwd is not in the dictionary,
     * A string str such as hash(str) = hashPwd if hashPwd is in the dictionary.
     */
    protected String checkDictionary() {
        return "";
    }

    /**
     * Returns a string containing the password corresponding to hashPwd
     */
    public String bruteForce() throws PasswordNotFoundException
    {
        String d = checkDictionary();
        this.password = (d.equals("")) ? this.bruteForce(0) : d;
        return this.password;
    }

    /**
     * This function attempt to discover the password that gives the same SHA-1 as hashPwd
     * During his computation
     * @param i index of the character to increment in the array guess
     * @return
     */
    private String bruteForce(int i)
    {
        if (i == pwdLength)
        {
            try
            {
                String str = String.copyValueOf(guess);
                byte[] hashGuessed = hashSHA1(str);
                this.putDictionary(hashGuessed, str); // Active only for SmarterBruteForce
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
            //
            for(char c = 'a'; c <= 'z' && !found; c++)
            {
                guess[i] = c;
                bruteForce(i+1); //TODO: multithreader ici pour la smartest version
            }
        }
        return this.password;
    }
}
