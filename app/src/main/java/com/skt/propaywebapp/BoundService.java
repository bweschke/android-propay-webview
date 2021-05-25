package com.skt.propaywebapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.anywherecommerce.android.sdk.Terminal;
import com.anywherecommerce.android.sdk.devices.CardReaderController;
import com.anywherecommerce.android.sdk.devices.bbpos.BBPOSDevice;

import java.util.Random;

public class BoundService extends Service {

    protected CardReaderController cardReaderController;

    public BoundService() {
    }
    private final IBinder localBinder = new MyBinder();

    @Override
    public IBinder onBind(Intent intent) {

        return localBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void initTerminal() {
        try {
            Terminal.restoreState();
            Log.i("Terminal restoreState Complete","Terminal restore Saved State Complete");

        } catch (Exception ex) {
            Log.i("TerminalRestore Exception", "Terminal restore Saved State Not Completed ", ex.fillInStackTrace());
        }

        try {
            cardReaderController = CardReaderController.getControllerFor(BBPOSDevice.class);
        } catch (Exception exc) {
            Log.i("cardReaderController Exception", "Exception: ", exc.fillInStackTrace());
        }

    }

    public class MyBinder extends Binder {

      public   BoundService getService() {
            return BoundService.this;

        }
    }
}