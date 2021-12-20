
public class Request
{
    private int requestId;
    private byte[] hashPwd;
    private int pwdLength;
    private long fileLength;

    public Request (int requestId, byte[] hashPwd, int pwdLength, long fileLength)
    {
        this.hashPwd = hashPwd;
        this.pwdLength= pwdLength;
        this.fileLength = fileLength;
        this.requestId = requestId;
    }

    public int getRequestId() { return requestId; }

    public byte[] getHashPassword() { return hashPwd; }

    public int getLengthPwd() { return pwdLength; }

    public long getLengthFile() { return fileLength; }
}
