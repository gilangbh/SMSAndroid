package smsgratispro.bhagaskara.net.smsgratispro;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    String url = "";
    int RESULT_CONTACT;
    InterstitialAd mInterstitialAd;
    ProgressDialog progressDialog;
    String endpoint = "https://k5k34.azurewebsites.net/sms";
    private static final String TAG = "Main";
    MixpanelAPI mixpanel;

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder().build();
        this.mInterstitialAd.loadAd(adRequest);
    }

    protected void onActivityResult(int n1, int n2, Intent intent){
        if (n2 == -1) {
            try {
                Uri uri = intent.getData();
                Cursor cursor = this.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    String string2 = cursor.getString(cursor.getColumnIndex("data1")).replaceAll("[^0-9]", "");
                    WebView webView = (WebView)this.findViewById(R.id.webView);
                    webView.loadUrl(this.endpoint + "/send?hp=" + string2);
                    cursor.close();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WebView wv = (WebView)this.findViewById(R.id.webView);
        //wv.getSettings().setUserAgentString("k5k34");
        wv.getSettings().setJavaScriptEnabled(true);
        wv.setWebViewClient(new MyBrowser(this));
        wv.loadUrl(this.endpoint);

        ((AdView)this.findViewById(R.id.adView)).loadAd(new AdRequest.Builder().build());

        this.mInterstitialAd = new InterstitialAd(this);
        this.mInterstitialAd.setAdUnitId("ca-app-pub-1937227624188320/6049418099");
        this.mInterstitialAd.setAdListener(new AdListener(){
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
            }
        });

        String projectToken = "3aee00ecd3f5fdfaefa501b21ea655d9"; // e.g.: "1ef7e30d2a58d27f4b90c42e31d6d7ad"
        mixpanel = MixpanelAPI.getInstance(this, projectToken);
        mixpanel.track("Opened the app");

        requestNewInterstitial();
    }

    private class MyBrowser extends WebViewClient {
        final MainActivity mActivity;

        private MyBrowser(MainActivity mainActivity){
            mActivity = mainActivity;
        }

        @Override
        public void onPageFinished(WebView webView, String url){
            Log.i(TAG, "onPageFinished URL: " + url);
            if(mActivity.progressDialog != null){

                Log.i(TAG, "Dismissing dialog");
                mActivity.progressDialog.dismiss();
                mActivity.progressDialog = null;
                Log.i(TAG, "Dismissed dialog");
            }
        }

        @Override
        public void onReceivedError(WebView webView, int n, String string2, String string3) {
            webView.loadUrl("file:///android_asset/error.html");
        }

        @Override
        public void onPageStarted(WebView webView, String url, Bitmap bitmap) {
            Log.i(TAG, "onPageStarted URL: " + url);
            super.onPageStarted(webView, url, bitmap);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView webView, String url){
            Log.i(TAG, "shouldOverrideUrlLoading URL: " + url);

            webView.loadUrl(url);
            mActivity.progressDialog = new ProgressDialog(mActivity);
            mActivity.progressDialog.setMessage("Loading");
            mActivity.progressDialog.show();
            if(url.contains("Send")){
                try {
                    JSONObject props = new JSONObject();
                    props.put("Source", "App");
                    mixpanel.track("Send SMS Modal", props);
                } catch (JSONException e) {
                    Log.e("Main", "Unable to add properties to JSONObject", e);
                }
            }
            if(url.contains("Contact")){
                try {
                    JSONObject props = new JSONObject();
                    props.put("Source", "App");
                    mixpanel.track("Open Address Book", props);
                } catch (JSONException e) {
                    Log.e("Main", "Unable to add properties to JSONObject", e);
                }
                Intent intent = new Intent("android.intent.action.PICK", ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                this.mActivity.startActivityForResult(intent, this.mActivity.RESULT_CONTACT);
            }
            if(url.contains("Success") && this.mActivity.mInterstitialAd.isLoaded()){
                try {
                    JSONObject props = new JSONObject();
                    props.put("Source", "App");
                    mixpanel.track("Send SMS success", props);
                } catch (JSONException e) {
                    Log.e("Main", "Unable to add properties to JSONObject", e);
                }
                this.mActivity.mInterstitialAd.show();
            }

            return true;
        }
    }
}
