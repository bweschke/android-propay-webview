package com.skt.propaywebapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.anywherecommerce.android.sdk.AnyPay;
import com.anywherecommerce.android.sdk.AuthenticationListener;
import com.anywherecommerce.android.sdk.CloudPosTerminalMessage;
import com.anywherecommerce.android.sdk.CloudPosTerminalMessageQueue;
import com.anywherecommerce.android.sdk.CommonErrors;
import com.anywherecommerce.android.sdk.GenericEventListener;
import com.anywherecommerce.android.sdk.GenericEventListenerWithParam;
import com.anywherecommerce.android.sdk.Logger;
import com.anywherecommerce.android.sdk.MeaningfulError;
import com.anywherecommerce.android.sdk.MeaningfulErrorListener;
import com.anywherecommerce.android.sdk.MeaningfulMessage;
import com.anywherecommerce.android.sdk.RequestListener;
import com.anywherecommerce.android.sdk.TaskListener;
import com.anywherecommerce.android.sdk.Terminal;
import com.anywherecommerce.android.sdk.devices.CardReader;
import com.anywherecommerce.android.sdk.devices.CardReaderController;
import com.anywherecommerce.android.sdk.devices.MultipleBluetoothDevicesFoundListener;
import com.anywherecommerce.android.sdk.devices.bbpos.BBPOSDevice;
import com.anywherecommerce.android.sdk.endpoints.AnyPayTransaction;
import com.anywherecommerce.android.sdk.endpoints.anywherecommerce.CloudAPI;
import com.anywherecommerce.android.sdk.endpoints.worldnet.WorldnetEndpoint;
import com.anywherecommerce.android.sdk.models.CloudPosTerminalConnectionStatus;
import com.anywherecommerce.android.sdk.models.TransactionStatus;
import com.anywherecommerce.android.sdk.models.TransactionType;
import com.anywherecommerce.android.sdk.transactions.listener.CardTransactionListener;
import com.anywherecommerce.android.sdk.transactions.listener.TransactionListener;


public class MainActivity extends AppCompatActivity {

    protected WebView webView;
    BoundService boundService;
    boolean isBound = false;

    public MainActivity activity;

    public void runAppendFromAndroid() {
        webView.loadUrl("javascript:appendFromAndroid();");
    }

    public void initializeTerminal(String terminalId, String terminalpass) {
        Log.i("initializeTerminal", "Terminal initializing for ID: "+terminalId);
        try {
            Terminal.initializeFromCloud(terminalId, terminalpass, new TaskListener() {
                @Override
                public void onTaskComplete() {
                    Log.i("initializeTerminal", "Cloud Terminal initialized for ID: " + terminalId + " Endpoint details " + Terminal.getInstance().getEndpoint().getProvider());
                    //initializeTerminalBtn.setText("Initialize Cloud Terminal");
                    //initializeTerminalBtn.setEnabled(true);

                    //addText("Cloud Terminal Initialized. Endpoint details " + Terminal.getInstance().getEndpoint().getProvider());
                    //changeState(getCloudMsgBtn, true);

                    //loginLayout.setVisibility(View.GONE);
                }

                @Override
                public void onTaskFailed(MeaningfulError meaningfulError) {
                    Log.i("initializeTerminal", "Cloud Terminal initialization for ID: " + terminalId + " failed. " + meaningfulError.message);
                    //initializeTerminalBtn.setText("Initialize Cloud Terminal");
                    //initializeTerminalBtn.setEnabled(true);

                    //Toast.makeText(MainActivity.this, "Terminal Initialization failed " + meaningfulError.message, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception ex) {
            Log.w("initializeTerminal", "Exception: ", ex.fillInStackTrace());
            }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String appCachePath = getApplicationContext().getCacheDir().getAbsolutePath();
        if (!PermissionsController.verifyAppPermissions(this)) {
            PermissionsController.requestAppPermissions(this, PermissionsController.permissions, 1001);
        }

        webView = (WebView) findViewById(R.id.webapp);
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        WebAppInterface wva = new WebAppInterface(this, webView);
        webView.addJavascriptInterface(wva, "droid");
        webView.loadUrl("file:///android_asset/index.html");
        // Caching
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAppCachePath(appCachePath);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAppCacheEnabled(true);

    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this , BoundService.class);
        startService(intent);
        bindService(intent , boundServiceConnection,BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(isBound){
            unbindService(boundServiceConnection);
            isBound = false;
        }
    }

    private ServiceConnection boundServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            BoundService.MyBinder binderBridge = (BoundService.MyBinder) service ;
            boundService = binderBridge.getService();
            isBound = true;
            if (boundService != null) {
                Log.i("boundServiceInit","Bound service is not null trying to initialize the terminal.");
            }
            boundService.initTerminal();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            isBound = false;
            boundService= null;

        }
    };

}

