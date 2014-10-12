package comx.detian.hasitchanged;

public class SiteResponse {
    int responseCode;
    String eTag;
    byte[] payload;

    SiteResponse(){
        responseCode = -1;
        eTag = null;
        payload = null;
    }
}
