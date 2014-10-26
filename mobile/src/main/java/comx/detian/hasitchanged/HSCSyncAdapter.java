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
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

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
    static int numMessages = 0;

    public HSCSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    public HSCSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String authority, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        Log.d("Sync: onPerformSync", "Auto");
        performSyncNow(getContext(), bundle, contentProviderClient);
        Log.d("Sync: onPerformSync", "Finished");
    }

    public synchronized static void performSyncNow(Context context, Bundle bundle, final ContentProviderClient contentProviderClient) {
        Log.d("Sync: performSyncNow", "called");
        numMessages = 0;
        Gson gson = new GsonBuilder().create();

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnectedOrConnecting()) {
            Log.d("SyncAdapter: onPerform", "No active network");
            return;
        }

        try {
            boolean forceSyncAll = bundle.getBoolean("FORCE_SYNC_ALL");

            Cursor cursor = contentProviderClient.query(DatabaseOH.getBaseURI(), null, null, null, null);

            //ArrayList<String> targetTimes = new ArrayList<String>();
            //ArrayList<String> syncTimes = new ArrayList<String>();

            Log.d("SyncAdapter: onPerform", "Iterating....");

            //Skip first dummy
            cursor.moveToNext();

            while (cursor.moveToNext()) {
                long id = cursor.getLong(DatabaseOH.COLUMNS._id.ordinal());
                Log.d("SyncAdapter: onPerform", "Loading preference " + HSCMain.PREFERENCE_PREFIX + id);
                SharedPreferences sitePreference = context.getSharedPreferences(HSCMain.PREFERENCE_PREFIX + id, Context.MODE_MULTI_PROCESS);

                /*//Keep track of data to calculate time to next sync
                if (sitePreference.getString("pref_sync_method", "sync").equals("sync")) {
                    if (!sitePreference.getString("pref_site_sync_time_elapsed", "never").equals("never")) {
                        targetTimes.add(sitePreference.getString("pref_site_sync_time_elapsed", "never"));
                        //System.out.println(targetTimes[i-1]);
                        syncTimes.add(cursor.getString(DatabaseOH.COLUMNS.LUDATE.ordinal()));
                    }
                }*/

                if (sitePreference.getBoolean("pref_site_wifi_only", false) && activeNetwork.getType() != ConnectivityManager.TYPE_WIFI) {
                    Log.d("SyncAdapter: onPerform", "Skipping due to not on wifi");
                    continue;
                }

                //Only sync those whose time is up with grace period of 1 min
                if (!forceSyncAll && !bundle.getBoolean("FORCE_SYNC_" + id) && HSCMain.calcTimeDiff(cursor.getString(DatabaseOH.COLUMNS.LUDATE.ordinal()), sitePreference.getString("pref_site_sync_time_elapsed", "never")) > 60) {
                    continue;
                }

                String url = sitePreference.getString("pref_site_protocol", "http") + "://" + sitePreference.getString("pref_site_url", "");
                //String url = cursor.getString(DatabaseOH.COLUMNS.PROTOCOL.ordinal()) +"://"+cursor.getString(DatabaseOH.COLUMNS.URL.ordinal());
                if (sitePreference.getString("pref_site_url", "").trim().length() == 0) {
                    continue;
                }
                if (sitePreference.getString("pref_site_protocol", null).equals("ftp")) {
                    //TODO check if actually directory, maybe use trailing slash
                    url += "type=d";
                }
                int lastHash = cursor.getInt(DatabaseOH.COLUMNS.HASH.ordinal());

                String ldate = null, eTag = null;
                if (sitePreference.getBoolean("pref_site_allow_server_not_modified", false)) {
                    ldate = cursor.getString(DatabaseOH.COLUMNS.LUDATE.ordinal());
                    eTag = cursor.getString(DatabaseOH.COLUMNS.ETAG.ordinal());
                }

                int readTimeout = Integer.parseInt(sitePreference.getString("pref_site_read_timeout", "15000"));
                int connectTimeout = Integer.parseInt(sitePreference.getString("pref_site_connection_timeout", "10000"));

                SiteResponse response = downloadUrl(url, connectTimeout, readTimeout, "GET", ldate, eTag);

                ContentValues updateValues = new ContentValues();
                updateValues.put("LUDATE", HSCMain.df.format(new Date()) + " GMT");

                LinkedHashMap<Long, String> history;

                String historyRaw = cursor.getString(DatabaseOH.COLUMNS.HISTORY.ordinal());
                if (historyRaw == null || historyRaw.length() == 0) {
                    history = new LinkedHashMap<Long, String>();
                } else {
                    history = gson.fromJson(historyRaw, DatabaseOH.historyType);
                }

                if (response.responseCode == 200) {
                    if (response.payload == null) {
                        //TODO log/handle this error
                        Log.e("SyncAdapter: " + url, "Response is 200 but payload is null");
                        continue;
                    }
                    String data = new String(response.payload);

                    if (sitePreference.getBoolean("pref_site_use_smart_compare", false)) {
                        Document doc = Jsoup.parse(data);
                        Element e = doc.body().select("#b_results, #ires, #content, .content, content, [ID*=content]").first();
                        if (e == null) {
                            e = doc.body();
                        }
                        String temp = "";
                        for (Element ele : e.getAllElements()) {
                            temp += ele.ownText();
                        }
                        data = temp;
                    }

                    int hashCode = data.hashCode();

                    if (lastHash != hashCode) {
                        createNotification(context, url, "Has changed.", cursor.getBlob(DatabaseOH.COLUMNS.FAVICON.ordinal()), sitePreference.getString("pref_site_notification_sound", ""));

                        updateValues.put("HASH", hashCode);
                        if (response.eTag != null)
                            updateValues.put("ETAG", response.eTag);
                        if (sitePreference.getBoolean("pref_site_download_favicon", false))
                            updateValues.put("FAVICON", downloadUrl("http://www.google.com/s2/favicons?domain=" + sitePreference.getString("pref_site_url", null), connectTimeout, readTimeout, "GET", ldate, null).payload);
                        //updateValues.put("FAVICON", (byte[]) downloadUrl(cursor.getString(1).substring(0, cursor.getString(1).indexOf("/"))+"/favicon.ico", 15000, 10000, "GET", DesiredType.RAW));
                        history.put(System.currentTimeMillis(), "C" + response.responseCode);
                    } else {
                        history.put(System.currentTimeMillis(), "S" + response.responseCode);
                    }
                    Log.d("SyncAdapter: " + url, "Changed? Hash is " + hashCode + " vs " + lastHash);
                } else {
                    history.put(System.currentTimeMillis(), "O" + response.responseCode);
                    if (response.responseCode != 304) {
                        //TODO check pref_site_timeout_notify
                        createNotification(context, url, "Is Down!.", cursor.getBlob(DatabaseOH.COLUMNS.FAVICON.ordinal()), sitePreference.getString("pref_site_notification_sound", ""));
                    }
                }

                updateValues.put("HISTORY", gson.toJson(history));

                try {
                    contentProviderClient.update(ContentUris.withAppendedId(DatabaseOH.getBaseURI(), id), updateValues, "_id=?", new String[]{"" + id});

                    //This item was synced, so use the new timestamp
                    /*syncTimes.remove(syncTimes.size()-1);
                    syncTimes.add(updateValues.getAsString("LUDATE"));*/
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
            cursor.close();
            //Update the UI
            //TODO change notification mechanism
            Intent intent = new Intent("comx.detian.hasitchanged.SYNC_COMPLETE");
            context.sendBroadcast(intent);

            //Schedule next sync
            HSCMain.updateNextSyncTime(context);
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
            URLConnection conn = url.openConnection();
            //RESOLVED
            //Don't rely on content length and re-enable gzip compression (maybe use AndroidHTTPClient
            //conn.setRequestProperty("Accept-Encoding", "identity");

            //conn.setRequestProperty("User-Agent","Mozilla/5.0 Gecko Firefox");
            conn.setReadTimeout(readTimeout /* milliseconds */);
            conn.setConnectTimeout(connectTimeout /* milliseconds */);
            if (conn instanceof HttpURLConnection) {
                ((HttpURLConnection) conn).setRequestMethod(method);
                if (fromDate != null) {
                    HSCMain.df.setTimeZone(TimeZone.getTimeZone("GMT"));
                    Log.d("SyncAdapter: DownloadURL", fromDate);
                    conn.setRequestProperty("If-Modified-Since", fromDate);
                    if (eTag != null && eTag.length() != 0)
                        conn.setRequestProperty("If-None-Match", eTag);
                    conn.setUseCaches(true);
                } else {
                    conn.setUseCaches(false);
                }
            }
            conn.setDoInput(true);
            // Starts the query
            conn.connect();

            if (conn instanceof HttpURLConnection) {
                int response = ((HttpURLConnection) conn).getResponseCode();
                String eTagNew = conn.getHeaderField("ETag");
                Log.d("SyncAdapter: DownloadURL", "The response code is: " + response + " " + eTagNew);
                out.responseCode = response;
                out.eTag = eTag;
            }

            is = conn.getInputStream();
            Log.d("SyncAdapter: DownloadURL", "The Content-Length is: " + conn.getContentLength());

            if (conn.getContentLength() > 0) {
                out.payload = new byte[conn.getContentLength()];
                is.read(out.payload);
            } else {
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
        while ((data = bis.read()) != -1) {
            bout.write(data);
        }
        bout.close();
        bis.close();
        return out.toByteArray();
    }

    private static void createNotification(Context context, String title, String content, byte[] icon, String sound) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_notify_change)
                .setNumber(++numMessages);
        if (sound!=null && sound.length()>0){
            mBuilder.setSound(Uri.parse(sound));
        }
        if (numMessages>1){
            mBuilder.setContentTitle("HasItChanged?").setContentText("Yep. Multiple sites have changed.");
        }
        if (icon != null)
            mBuilder.setLargeIcon(BitmapFactory.decodeByteArray(icon, 0, icon.length));

        Intent intent = new Intent(context, HSCMain.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
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
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(42, mBuilder.build());
    }
}
