package comx.detian.hasitchanged;

public class SiteResponse {
    public static final int MALFORMEDURL = -4545;
    final static int IOEXCEPTION = -4444;
    int responseCode;
    String eTag;
    byte[] payload;

    SiteResponse() {
        responseCode = -1;
        eTag = null;
        payload = null;
    }
}
