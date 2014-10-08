package comx.detian.hasitchanged;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

public class HSCRequest extends StringRequest{
    private int statusCode;

    public HSCRequest(int method, String url, Response.Listener<String> listener, Response.ErrorListener errorListener){
        super(method ,url, listener, errorListener);
    }

    public int getStatusCode(){
        return statusCode;
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response){
        statusCode = response.statusCode;
        return super.parseNetworkResponse(response);
    }

}
