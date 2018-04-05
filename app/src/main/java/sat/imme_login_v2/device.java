package sat.imme_login_v2;

import android.content.Intent;
import android.graphics.Color;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import com.lochbridge.oath.otp.HmacShaAlgorithm;
import com.lochbridge.oath.otp.TOTP;
import com.lochbridge.oath.otp.TOTPBuilder;
import com.lochbridge.oath.otp.TOTPValidator;

public class device extends AppCompatActivity implements NfcAdapter.CreateNdefMessageCallback,
        NfcAdapter.OnNdefPushCompleteCallback {
    NfcAdapter mNfcAdapter;
    TextView mInfoText;
    Boolean valid =true;
    private static final int MESSAGE_SENT = 1;
    private String Key;
    byte[] webKeybytes = new byte[0];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("***********************************");
        setContentView(R.layout.activity_device);
        Key = getIntent().getStringExtra("otp");
        String mKey = "Habhdjd8587585";
        try {
            webKeybytes = mKey.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        mInfoText = (TextView)findViewById(R.id.device_textView);
        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            mInfoText = (TextView)findViewById(R.id.device_textView);
            mInfoText.setText("NFC is not available on this device.");
        }
        // Register callback to set NDEF message
        mNfcAdapter.setNdefPushMessageCallback(this, this);
        // Register callback to listen for message-sent success
        mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
    }


    /**
     * Implementation for the CreateNdefMessageCallback interface
     */
    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        NdefMessage msg = new NdefMessage(
                new NdefRecord[] { createMimeRecord(
                        "application/nfc.beam", Key.getBytes())

                });
        return msg;
    }

    /**
     * Implementation for the OnNdefPushCompleteCallback interface
     */
    @Override
    public void onNdefPushComplete(NfcEvent arg0) {
        // A handler is needed to send messages to the activity when this
        // callback occurs, because it happens from a binder thread
        mHandler.obtainMessage(MESSAGE_SENT).sendToTarget();
    }

    /** This handler receives a message from onNdefPushComplete */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SENT:
                    Toast.makeText(getApplicationContext(), "OTP sent!", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {

            try {
                processIntentDevice(getIntent());
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }


    void processIntentDevice(Intent intent) throws Exception{
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type
        Time time = new Time();
        time.setToNow();
        String mKey = new String(msg.getRecords()[0].getPayload());
        valid = TOTPValidator.window(2).isValid(webKeybytes, TimeUnit.SECONDS.toMillis(30), 8,
                HmacShaAlgorithm.HMAC_SHA_512, mKey);

        System.out.println("********************************" +valid);


        if (true){
            mInfoText.setText("Verification is successful!\n\n"
                    + "Time: " + time.format("%H:%M:%S"));
            mInfoText.setTextColor(Color.parseColor("#d67601"));
            //mInfoText.setBackgroundColor(Color.parseColor("#5fb0c9"));
        }else{
            mInfoText.setText("Verification is haha!\n\n"
                    + "Time: " + time.format("%H:%M:%S"));
            mInfoText.setTextColor(Color.RED);
        }
    }


    public NdefRecord createMimeRecord(String mimeType, byte[] payload) {
        byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
        NdefRecord mimeRecord = new NdefRecord(
                NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
        return mimeRecord;
    }



}


