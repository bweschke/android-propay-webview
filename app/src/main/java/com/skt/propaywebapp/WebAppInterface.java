package com.skt.propaywebapp;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.audiofx.DynamicsProcessing;
import android.os.Build;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.anywherecommerce.android.sdk.AnyPay;
import com.anywherecommerce.android.sdk.CloudPosTerminalMessage;
import com.anywherecommerce.android.sdk.CloudPosTerminalMessageQueue;
import com.anywherecommerce.android.sdk.GenericEventListener;
import com.anywherecommerce.android.sdk.GenericEventListenerWithParam;
import com.anywherecommerce.android.sdk.MeaningfulError;
import com.anywherecommerce.android.sdk.MeaningfulErrorListener;
import com.anywherecommerce.android.sdk.MeaningfulMessage;
import com.anywherecommerce.android.sdk.RequestListener;
import com.anywherecommerce.android.sdk.TaskListener;
import com.anywherecommerce.android.sdk.Terminal;
import com.anywherecommerce.android.sdk.devices.BluetoothCardReaderConnectionListener;
import com.anywherecommerce.android.sdk.devices.CardReader;
import com.anywherecommerce.android.sdk.devices.CardReaderController;
import com.anywherecommerce.android.sdk.devices.MultipleBluetoothDevicesFoundListener;
import com.anywherecommerce.android.sdk.devices.bbpos.BBPOSDevice;
import com.anywherecommerce.android.sdk.endpoints.AnyPayTransaction;
import com.anywherecommerce.android.sdk.endpoints.anywherecommerce.CloudAPI;
import com.anywherecommerce.android.sdk.models.CloudPosTerminalConnectionStatus;
import com.anywherecommerce.android.sdk.models.TransactionStatus;
import com.anywherecommerce.android.sdk.models.TransactionType;
import com.anywherecommerce.android.sdk.transactions.listener.CardTransactionListener;
import com.anywherecommerce.android.sdk.transactions.listener.TransactionListener;

import java.util.List;
import java.util.Map;

public class WebAppInterface {
    Context mContext;
    WebView webView;
    protected CardReaderController cardReaderController;
    protected String sessionKey;
    protected String termId;
    protected CloudPosTerminalMessage message;
    protected AnyPayTransaction transaction;


    WebAppInterface(Context c, WebView wv) {
        mContext = c;
        webView = wv;
    }

    @JavascriptInterface
    public String getSessionKey() {
        return sessionKey;
    }

    @JavascriptInterface
    public String getTerminalId() {
        return termId;
    }

    public void appendToWebView(String str) {
        webView.loadUrl("javascript:appendFromAndroid('"+ str +"');");
    }

    @JavascriptInterface
    public void activateCloudTerminal() {
        CloudPosTerminalMessageQueue.getInstance().subscribeToMessagesOfType("NEW_TRANSACTION", new GenericEventListenerWithParam<CloudPosTerminalMessage>() {
            @Override
            public void onEvent(CloudPosTerminalMessage message) {
                //addText("Transaction available to process. Please accept the transaction and process it");
                Log.i("newTransaction", "newTransaction received for processing");
                transaction = message.transaction;

                message.accept();

                acceptTransaction();
            }
        });

        CloudPosTerminalMessageQueue.getInstance().subscribeToMessagesOfType("CANCEL_TRANSACTION", new GenericEventListenerWithParam<CloudPosTerminalMessage>() {
            @Override
            public void onEvent(CloudPosTerminalMessage message) {
                Log.i("cancelTransaction", "cancelTransaction received for processing");

                if (transaction == null) {
                    //addText("Request Rejected");
                    Log.i("cancelTransaction", "no transaction in progress");
                    message.reason = "No transaction in progress";
                    message.reject();
                }
                else if ((boolean)(transaction.getCustomField("ReaderProcessingStarted", false))) {
                    Log.i("cancelTransaction", "transaction cannot be cancelled at this stage");
                    message.reason = "Transaction cannot be cancelled at this stage";
                    message.reject();
                }
                else if (transaction.getUuid().equalsIgnoreCase(message.transactionUUID))  // Same transaction as the one in progress.
                {
                    try {
                        transaction.cancel();
                        message.accept();

                        transaction.setStatus(TransactionStatus.CANCELLED);

                    } catch (Exception ex) {
                        message.fail(new MeaningfulError(ex));
                    }
                } else {
                    message.reason = "Wrong transaction";
                    message.reject();
                }
            }
        });

        CloudPosTerminalMessageQueue.getInstance().subscribeToMessagesOfType("CONFIG_CHANGED", new GenericEventListenerWithParam<CloudPosTerminalMessage>() {
            @Override
            public void onEvent(CloudPosTerminalMessage message) {
                Terminal.getInstance().overwriteConfiguration(message.terminal);
                message.accept();

                Log.i("terminalConfigUpdated", "terminalConfigurationUpdated");
            }
        });


        CloudPosTerminalMessageQueue.getInstance().OnConnectionStatusChanged = new GenericEventListenerWithParam<CloudPosTerminalConnectionStatus>() {
            @Override
            public void onEvent(CloudPosTerminalConnectionStatus cloudPosTerminalConnectionStatus) {
                if (transaction == null) {
                    switch (cloudPosTerminalConnectionStatus) {
                        case CONNECTED:
                            Log.i("cloudTerminalConnected", "waiting for transaction...");
                            break;

                        case CONNECTING:
                            Log.i("cloudTerminalConnecting", "cloud terminal connecting...");
                            break;
                        case RECONNECTING:
                            Log.i("cloudTerminalReconnecting", "reconnecting after interruption...");
                            break;

                        case DISCONNECTED:
                        case DISCONNECTING:
                            Log.i("cloudTerminalDisconnected", "cloud terminal connection interrupted...");
                            break;
                    }
                }
            }
        };

        CloudPosTerminalMessageQueue.getInstance().start();
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
                    sessionKey = Terminal.getInstance().getSessionKey();
                    termId = Terminal.getInstance().getId();

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

    private void acceptTransaction() {
        Log.i("acceptTransaction", "accepting cloud transaction...");

        this.transaction.setStatus(TransactionStatus.PROCESSING);

        CloudAPI.acceptTransaction(Terminal.getInstance(), this.transaction, new RequestListener<Void>() {
            @Override
            public void onRequestComplete(Void aVoid) {
                Log.i("acceptTransaction", "transaction accepted - beginning processing");
                //changeState(processTransacationBtn, true);
                processTransaction();
            }

            @Override
            public void onRequestFailed(MeaningfulError meaningfulError) {
                Log.w("acceptTransaction", "transaction not accepted - message: "+meaningfulError.message);            }
        });
    }

    private void processTransaction() {
        if (!CardReaderController.isCardReaderConnected()) {
            Log.w("acceptTransaction", "transaction cannot proceed - no card reader connected");
            appendToWebView("transaction cannot proceed - no card reader connected");

            this.transaction.setStatus(TransactionStatus.FAILED);
            updateTransaction(this.transaction);

            this.transaction = null;

            return;
        }

        if (transaction.getTransactionType() != TransactionType.SALE && transaction.getTransactionType() != TransactionType.AUTHONLY) {
            this.transaction.execute(new TransactionListener() {
                @Override
                public void onTransactionCompleted() {
                    updateTransaction(transaction);

                    if (transaction.isApproved()) {
                        Log.i("transactionCompleted", "Transaction Approved");
                        appendToWebView("Transaction Approved");

                    }
                    else {
                        Log.i("transactionCompleted", "transaction declined: " + transaction.getResponseText());
                        appendToWebView("Transaction Declined: "+transaction.getResponseText());
                    }

                    transaction = null;

                }

                @Override
                public void onTransactionFailed(MeaningfulError meaningfulError) {
                    Log.w("transactionFailed", "transaction failed - message: "+meaningfulError.message);
                    appendToWebView("Transaction Failed: "+meaningfulError.message);

                    updateTransaction(transaction);
                    transaction = null;
                }
            });
        }
        else {
            this.transaction.useCardReader(CardReaderController.getConnectedReader());

            this.transaction.execute(new CardTransactionListener() {
                @Override
                public void onCardReaderEvent(MeaningfulMessage meaningfulMessage) {
                    Log.i("inTransaction reader event", meaningfulMessage.message);
                    appendToWebView(meaningfulMessage.message);
                }

                @Override
                public void onTransactionCompleted() {
                    updateTransaction(transaction);

                    if (transaction.isApproved()) {
                        Log.i("transactionCompleted", "Transaction Approved");
                        appendToWebView("Transaction Approved");
                    }
                    else {
                        Log.i("transactionCompleted", "transaction declined: " + transaction.getResponseText());
                        appendToWebView("Transaction Declined: "+transaction.getResponseText());
                    }

                    transaction = null;

                }

                @Override
                public void onTransactionFailed(MeaningfulError meaningfulError) {
                    Log.w("transactionFailed", "transaction failed - message: "+meaningfulError.message);
                    appendToWebView("Transaction Failed: "+meaningfulError.message);

                    updateTransaction(transaction);
                    transaction = null;
                }
            });
        }

    }

    private void updateTransaction(AnyPayTransaction tr) {
        Terminal.getInstance().updateCloudTransaction(tr, new RequestListener<Void>() {
            @Override
            public void onRequestComplete(Void aVoid) {
                Log.i("updateTransaction", "transaction successfully updated");
            }

            @Override
            public void onRequestFailed(MeaningfulError meaningfulError) {
                Log.w("updateTransaction", "transaction not updated - message: "+meaningfulError.message);
            }
        });
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
