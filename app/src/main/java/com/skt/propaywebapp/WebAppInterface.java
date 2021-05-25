package com.skt.propaywebapp;

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
import com.anywherecommerce.android.sdk.Terminal;
import com.anywherecommerce.android.sdk.devices.CardReader;
import com.anywherecommerce.android.sdk.devices.CardReaderController;
import com.anywherecommerce.android.sdk.devices.bbpos.BBPOSDevice;

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
    public void tryLogin(String terminalId, String password) {

        mContext.getApplicationContext();
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
