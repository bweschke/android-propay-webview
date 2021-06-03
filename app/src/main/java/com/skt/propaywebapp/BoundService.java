package com.skt.propaywebapp;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.anywherecommerce.android.sdk.MeaningfulError;
import com.anywherecommerce.android.sdk.Terminal;
import com.anywherecommerce.android.sdk.devices.BluetoothCardReaderConnectionListener;
import com.anywherecommerce.android.sdk.devices.CardReader;
import com.anywherecommerce.android.sdk.devices.CardReaderController;
import com.anywherecommerce.android.sdk.devices.bbpos.BBPOSDevice;

import java.util.List;
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
        Log.i("bs", "initTerminal called");


        Log.i("connectBTReader", "Connecting to Bluetooth Reader");
        /*cardReaderController.connectBluetooth(new MultipleBluetoothDevicesFoundListener() {
            @Override
            public void onMultipleBluetoothDevicesFound(List<BluetoothDevice> matchingDevices) {
                Log.i("connectBTReader","Many BT devices");
            }
        }); */
        // To connect via bluetooth, simply pass the BluetoothCardReaderConnectionListener parameter.
        CardReader.connect(new BluetoothCardReaderConnectionListener() {

            @Override
            public void onCardReaderConnected(CardReader cardReader) {
                // The connected card reader will be returned in this method.  You can now use it.
                // Card reader connected, name = cardReader.getModelDisplayName()
                Log.i("BT connect", "connected: "+cardReader.getModelDisplayName());
            }

            @Override
            public void onCardReaderConnectionFailed(MeaningfulError meaningfulError) {
                // Something went wrong.  Check the error
                // meaningfulError.toString() contains error text;
                Log.i("BT connect", "not connected: "+meaningfulError.toString());
            }

            @Override
            public void onMultipleBluetoothDevicesFound(List<BluetoothDevice> matchingDevices) {

                // In the unlikely event that there are more than one bluetooth devices in range,
                // this callback will get fired.  You will need to select one of the devices and connect to it.

                // Should prompt the user to select one device to connect out of the matchingDevices candidate list.

                // For this example, we'll just take the first one in the list for simplicity and connect to it.
                BluetoothDevice device = matchingDevices.get(0);

                CardReader.connect(device, new BluetoothCardReaderConnectionListener() {

                    @Override
                    public void onCardReaderConnected(CardReader cardReader) {
                        // The connected card reader will be returned in this method.  You can now use it.
                        // Card reader connected, name = cardReader.getModelDisplayName()
                        Log.i("BT connect", "connected: "+cardReader.getModelDisplayName());
                    }

                    @Override
                    public void onCardReaderConnectionFailed(MeaningfulError meaningfulError) {
                        // Something went wrong.  Check the error
                        // meaningfulError.toString() contains error text;
                        Log.i("BT connect", "not connected: "+meaningfulError.toString());
                    }

                    @Override
                    public void onMultipleBluetoothDevicesFound(List<BluetoothDevice> matchingDevices) {
                    }
                });
            }

        });

        /* try {
            if (Terminal.getInstance() == null) {
                Log.w("terminal restoreState", "terminal instance is null!");
            } else {
                Log.i("terminal restoreState", "terminal getInstance is not null");
            }
            Terminal.restoreState();
            Log.i("Terminal restoreState Complete","Terminal restore Saved State Complete");

        } catch (Exception ex) {
            Log.i("TerminalRestore Exception", "Terminal restore Saved State Not Completed ", ex.fillInStackTrace());
        }

        try {
            cardReaderController = CardReaderController.getControllerFor(BBPOSDevice.class);
        } catch (Exception exc) {
            Log.i("cardReaderController Exception", "Exception: ", exc.fillInStackTrace());
        } */

    }

    public class MyBinder extends Binder {

      public   BoundService getService() {
            return BoundService.this;

        }
    }
}