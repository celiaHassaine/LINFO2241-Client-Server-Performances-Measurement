public class PasswordNotFoundException extends Exception
{
    public void err()
    {
        System.err.println("PasswordNotFoundException: password not found");
        System.err.println(" \t Does the password contain unhandled characters ?");
        System.err.println(" \t Is the length of the password properly set ?");
    }
}
