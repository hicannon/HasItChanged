package comx.detian.hasitchanged;

import android.accounts.Account;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.TimeZone;

public class HSCSyncAdapter extends AbstractThreadedSyncAdapter {
    Gson gson;

    public HSCSyncAdapter(Context context, boolean autoInitialize){
        super(context, autoInitialize);

        initialize();
    }

    public HSCSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs){
        super(context, autoInitialize, allowParallelSyncs);

        initialize();
    }

    private void initialize(){
        gson= new GsonBuilder().create();
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, final ContentProviderClient contentProviderClient, SyncResult syncResult) {
        //TODO
        //Log.d("Sync: onPerform", "called");

        ConnectivityManager cm =
                (ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork==null || !activeNetwork.isConnectedOrConnecting()){
            Log.d("SyncAdapter: onPerform", "No active network");
            return;
        }

        try {
            Cursor cursor = contentProviderClient.query(DatabaseOH.getBaseURI(), null, null, null, null);
            Log.d("SyncAdapter: onPerform", "Iterating....");

            //Skip first dummy
            cursor.moveToNext();

            while (cursor.moveToNext()){
                long id = cursor.getLong(DatabaseOH.COLUMNS._id.ordinal());
                Log.d("SyncAdapter: onPerform", "Loading preference " + HSCMain.PREFERENCE_PREFIX + id);
                SharedPreferences sitePreference = getContext().getSharedPreferences(HSCMain.PREFERENCE_PREFIX+id, Context.MODE_MULTI_PROCESS);

                if (sitePreference.getBoolean("pref_site_wifi_only", false) && activeNetwork.getType() != ConnectivityManager.TYPE_WIFI){
                    Log.d("SyncAdapter: onPerform", "Skipping due to not on wifi");
                    continue;
                }

                String url = sitePreference.getString("pref_site_protocol", "http") + "://" +sitePreference.getString("pref_site_url", null);
                //String url = cursor.getString(DatabaseOH.COLUMNS.PROTOCOL.ordinal()) +"://"+cursor.getString(DatabaseOH.COLUMNS.URL.ordinal());
                if (url==null || url.trim().length()==0){
                    continue;
                }
                if (sitePreference.getString("pref_site_protocol", null).equals("ftp")){
                    //TODO check if actually directory, maybe use trailing slash
                    url+="type=d";
                }
                int lastHash = cursor.getInt(DatabaseOH.COLUMNS.HASH.ordinal());

                String ldate = null, eTag = null;
                if (sitePreference.getBoolean("pref_site_allow_server_not_modified", false)){
                    ldate = cursor.getString(DatabaseOH.COLUMNS.LUDATE.ordinal());
                    eTag = cursor.getString(DatabaseOH.COLUMNS.ETAG.ordinal());
                }

                int readTimeout = Integer.parseInt(sitePreference.getString("pref_site_read_timeout", "15000"));
                int connectTimeout = Integer.parseInt(sitePreference.getString("pref_site_connection_timeout", "10000"));

                SiteResponse response = downloadUrl(url, connectTimeout, readTimeout, "GET", ldate, eTag);

                ContentValues updateValues = new ContentValues();
                LinkedHashMap<Long, String> history;

                String historyRaw = cursor.getString(DatabaseOH.COLUMNS.HISTORY.ordinal());
                if (historyRaw==null || historyRaw.length()==0){
                    history = new LinkedHashMap<Long, String>();
                }else{
                    history = gson.fromJson(historyRaw, DatabaseOH.historyType);
                }

                if (response.responseCode==200) {
                    if (response.payload == null) {
                        //TODO log/handle this error, handle unchanged
                        Log.e("SyncAdapter: " + url, "Response is 200 but payload is null");
                        continue;
                    }
                    String data = new String(response.payload);
                    int hashCode = data.hashCode();

                    if (lastHash != hashCode) {
                        createNotification(url, "Has changed.", cursor.getBlob(DatabaseOH.COLUMNS.FAVICON.ordinal()));

                        updateValues.put("LUDATE", HSCMain.df.format(new Date()) + " GMT");
                        updateValues.put("HASH", hashCode);
                        if (response.eTag != null)
                            updateValues.put("ETAG", response.eTag);
                        if (sitePreference.getBoolean("pref_site_download_favicon", false))
                            updateValues.put("FAVICON", downloadUrl("http://www.google.com/s2/favicons?domain=" + sitePreference.getString("pref_site_url", null), connectTimeout, readTimeout, "GET", ldate, null).payload);
                        //updateValues.put("FAVICON", (byte[]) downloadUrl(cursor.getString(1).substring(0, cursor.getString(1).indexOf("/"))+"/favicon.ico", 15000, 10000, "GET", DesiredType.RAW));
                        history.put(System.currentTimeMillis(), "C"+response.responseCode);
                    }else{
                        history.put(System.currentTimeMillis(), "S"+response.responseCode);
                    }
                    Log.d("SyncAdapter: " + url, "Changed? Hash is " + hashCode + " vs " + lastHash);
                }else{
                    history.put(System.currentTimeMillis(), "O"+response.responseCode);
                }

                updateValues.put("HISTORY", gson.toJson(history));

                try {
                    contentProviderClient.update(ContentUris.withAppendedId(DatabaseOH.getBaseURI(), id), updateValues, "_id=?", new String[]{"" + id});
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }

            cursor.close();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // Given a URL, establishes an HttpUrlConnection and retrieves
    // the web page content as a InputStream, which it returns as
    // a string.
    private static SiteResponse downloadUrl(String myurl, int connectTimeout, int readTimeout, String method, String fromDate, String eTag) {
        InputStream is = null;

        SiteResponse out = new SiteResponse();
        try {
            URL url = new URL(myurl);
            URLConnection conn = (URLConnection) url.openConnection();
            //RESOLVED
            //Don't rely on content length and re-enable gzip compression (maybe use AndroidHTTPClient
            //conn.setRequestProperty("Accept-Encoding", "identity");

            //conn.setRequestProperty("User-Agent","Mozilla/5.0 Gecko Firefox");
            conn.setReadTimeout(readTimeout /* milliseconds */);
            conn.setConnectTimeout(connectTimeout /* milliseconds */);
            if (conn instanceof HttpURLConnection) {
                ((HttpURLConnection) conn).setRequestMethod(method);
                if (fromDate!=null) {
                    HSCMain.df.setTimeZone(TimeZone.getTimeZone("GMT"));
                    Log.d("SyncAdapter: DownloadURL", fromDate + HSCMain.df.format(new Date()));
                    conn.setRequestProperty("If-Modified-Since", fromDate);
                    if (eTag!=null && eTag.length()!=0)
                        conn.setRequestProperty("If-None-Match", eTag);
                    conn.setUseCaches(true);
                }else{
                    conn.setUseCaches(false);
                }
            }
            conn.setDoInput(true);
            // Starts the query
            conn.connect();

            if (conn instanceof HttpURLConnection) {
                int response = ((HttpURLConnection)conn).getResponseCode();
                String eTagNew = conn.getHeaderField("ETag");
                Log.d("SyncAdapter: DownloadURL", "The response code is: " + response + " " + eTagNew);
                out.responseCode = response;
                out.eTag = eTag;
            }

            is = conn.getInputStream();
            Log.d("SyncAdapter: DownloadURL", "The Content-Length is: " + conn.getContentLength());

            if (conn.getContentLength()>0) {
                out.payload = new byte[conn.getContentLength()];
                is.read(out.payload);
            }else{
                out.payload = readFully(is);
                Log.d("SyncAdapter: DownloadURL:", "Read " + out.payload.length);
            }

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } catch (MalformedURLException e) {
            out.responseCode = SiteResponse.MALFORMEDURL;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            out.responseCode = SiteResponse.IOEXCEPTION;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return out;
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    static byte[] readFully(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BufferedOutputStream bout = new BufferedOutputStream(out);
        BufferedInputStream bis = new BufferedInputStream(is);
        int data = 0;
        while ((data = bis.read())!=-1){
            bout.write(data);
        }
        bout.close();
        bis.close();
        return out.toByteArray();
    }

    private void createNotification(String title, String content, byte[] icon) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this.getContext())
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher);
        if (icon!=null)
                mBuilder.setLargeIcon(BitmapFactory.decodeByteArray(icon, 0, icon.length));

        Intent intent = new Intent(this.getContext(), HSCMain.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this.getContext());
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(HSCMain.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) this.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(42, mBuilder.build());
    }
}
