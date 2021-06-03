package com.skt.propaywebapp;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.audiofx.DynamicsProcessing;
import android.os.Build;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.anywherecommerce.android.sdk.AnyPay;
import com.anywherecommerce.android.sdk.GenericEventListener;
import com.anywherecommerce.android.sdk.GenericEventListenerWithParam;
import com.anywherecommerce.android.sdk.MeaningfulError;
import com.anywherecommerce.android.sdk.MeaningfulErrorListener;
import com.anywherecommerce.android.sdk.RequestListener;
import com.anywherecommerce.android.sdk.TaskListener;
import com.anywherecommerce.android.sdk.Terminal;
import com.anywherecommerce.android.sdk.devices.BluetoothCardReaderConnectionListener;
import com.anywherecommerce.android.sdk.devices.CardReader;
import com.anywherecommerce.android.sdk.devices.CardReaderController;
import com.anywherecommerce.android.sdk.devices.MultipleBluetoothDevicesFoundListener;
import com.anywherecommerce.android.sdk.devices.bbpos.BBPOSDevice;

import java.util.List;
import java.util.Map;

public class WebAppInterface {
    Context mContext;
    WebView webView;
    protected CardReaderController cardReaderController;

    WebAppInterface(Context c, WebView wv) {
        mContext = c;
        webView = wv;
    }

    @JavascriptInterface
    public void connectBTReader() {
        mContext.getApplicationContext();

    }

    @JavascriptInterface
    public void tryLogin(String terminalId, String password) {

        mContext.getApplicationContext();
        Log.i("initializeTerminal", "Terminal initializing for ID: "+terminalId);
        try {
            Terminal.initializeFromCloud(terminalId, password, new TaskListener() {
                @Override
                public void onTaskComplete() {
                    Log.i("initializeTerminal", "Cloud Terminal initialized for ID: " + terminalId + " Endpoint details " + Terminal.getInstance().getEndpoint().getProvider());
                    webView.loadUrl("javascript:appendFromAndroid('Cloud Terminal initialized for ID: "+ terminalId +" Endpoint details " + Terminal.getInstance().getEndpoint().getProvider() +"');");
                    AnyPay.getSupportKey("MY_PASSPHRASE", new RequestListener<String>() {
                        @Override
                        public void onRequestComplete(String s) {
                            Log.i("support key","Support Key - " + s);
                        }

                        @Override
                        public void onRequestFailed(MeaningfulError meaningfulError) {

                        }
                    });

                    cardReaderController = CardReaderController.getControllerFor(BBPOSDevice.class);

                    subscribeCardReaderCallbacks();
                    webView.loadUrl("file:///android_asset/terminal.html");
                }

                @Override
                public void onTaskFailed(MeaningfulError meaningfulError) {
                    Log.i("initializeTerminal", "Cloud Terminal initialization for ID: " + terminalId + " failed. " + meaningfulError.message);
                    webView.loadUrl("javascript:appendFromAndroid('Cloud Terminal initialization for ID: "+ terminalId +" failed. " + meaningfulError.message+"');");
                    //initializeTerminalBtn.setText("Initialize Cloud Terminal");
                    //initializeTerminalBtn.setEnabled(true);

                    //Toast.makeText(MainActivity.this, "Terminal Initialization failed " + meaningfulError.message, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception ex) {
            Log.w("initializeTerminal", "Exception: ", ex.fillInStackTrace());
        }

    }

    private void subscribeCardReaderCallbacks() {

        cardReaderController.subscribeOnCardReaderConnected(new GenericEventListenerWithParam<CardReader>() {
            @Override
            public void onEvent(CardReader deviceInfo) {
                if (deviceInfo == null)
                    Log.i("subcardreadercallback","Unknown device connected");
                else
                    Log.i("subcardreadercallback", "Device connected " + deviceInfo.getModelDisplayName());
            }
        });

        cardReaderController.subscribeOnCardReaderDisconnected(new GenericEventListener() {
            @Override
            public void onEvent() {
                Log.i("subcardreadercallback","Device disconnected");
            }
        });

        cardReaderController.subscribeOnCardReaderConnectFailed(new MeaningfulErrorListener() {
            @Override
            public void onError(MeaningfulError error) {
                Log.i("subcardreadercallback","Device connect failed: " + error.toString());
            }
        });

        cardReaderController.subscribeOnCardReaderError(new MeaningfulErrorListener() {
            @Override
            public void onError(MeaningfulError error) {
                Log.i("subcardreadercallback","Device error: " + error.toString());
            }
        });
    }


}
