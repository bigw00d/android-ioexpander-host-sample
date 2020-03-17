package com.example.homegateway;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.content.Context;

import java.io.IOException;
import java.sql.Connection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.AwesomeTextView;
import com.beardedhen.androidbootstrap.BootstrapButton;
import com.beardedhen.androidbootstrap.BootstrapText;
import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

public class MainActivity extends AppCompatActivity {

    private FT_Device ftDev = null;
    private static Context mContext;
    private D2xxManager ftdid2xx;
    private static final String TAG = "IOEX_HOST";
    private int iavailable = 0;
    private static final int readLength = 512;
    byte[] readData;
    char[] readDataToText;
    private boolean isReading = false;
    private ReadThread mReadThread;

//    private TextView mInputValue;
//    private Button mOutputButton;
    private AwesomeTextView tv;

    /**
     * 描画処理はHandlerでおこなう
     */
    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String mData = (String)msg.obj;
//            Toast.makeText(MainActivity.this, "Received USB Data: " + mData, Toast.LENGTH_LONG).show();
            tv.append("RX <- " + mData + "\n");
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            ftdid2xx = D2xxManager.getInstance(this);
        } catch (D2xxManager.D2xxException ex) {
            Log.e(TAG,ex.toString());
        }

        mContext = this.getBaseContext();


//        final AwesomeTextView tv = findViewById(R.id.logTextView);
        tv = findViewById(R.id.logTextView);
        tv.setText("");

        openUsb();

        TextView textVersion = findViewById(R.id.textVersion);
        textVersion.setText("v0.0.0.1");

//        mInputValue = (TextView)findViewById(R.id.inputValue);

        BootstrapButton btnClear = findViewById(R.id.button_clear);
        // リスナーをボタンに登録
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv.setText("");
//                awesomeTextView.setBootstrapText(bootstrapText1);
            }
        });


        BootstrapButton btnSend = findViewById(R.id.button_send);
        // リスナーをボタンに登録
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendUsb("1");
            }
        });

        //        // ボタンが押されたらUSBに値を送り込む
//        mOutputButton = (Button)findViewById(R.id.outputButton);
//        mOutputButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                SendMessage("1");
//            }

    }

    public void openUsb(){
        int devCount = 0;
        devCount = ftdid2xx.createDeviceInfoList(this);
        if (devCount <= 0)
        {
            Toast.makeText(this, "デバイスが発見できません。" + Integer.toString(devCount), Toast.LENGTH_LONG).show();
            Log.i(TAG,"デバイスが発見できましせんでした。");
            return;
        }
        else{
            Toast.makeText(this, "" + devCount + "個のデバイスを発見しました。", Toast.LENGTH_LONG).show();
            Log.i(TAG,"" + devCount + "個のデバイスを発見しました。");
        }

        if(null == ftDev)
        {
            ftDev = ftdid2xx.openByIndex(mContext, 0);
        }
        else
        {
            synchronized(ftDev)
            {
                ftDev = ftdid2xx.openByIndex(mContext, 0);
            }
        }

        //ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);
        ftDev.setBaudRate(115200);
        ftDev.setDataCharacteristics(D2xxManager.FT_DATA_BITS_8, D2xxManager.FT_STOP_BITS_1, D2xxManager.FT_PARITY_NONE);
        ftDev.setFlowControl(D2xxManager.FT_FLOW_NONE, (byte) 0x0b, (byte) 0x0d);
        ftDev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));
        ftDev.restartInTask();

        readData = new byte[readLength];
        readDataToText = new char[readLength];
        mReadThread = new ReadThread(mHandler);
        mReadThread.start();
        isReading = true;

    }

    public void SendUsb(String msg) {

        if(ftDev == null){
            return;
        }

        synchronized (ftDev) {
            if (ftDev.isOpen() == false) {
                Log.e("j2xx", "SendMessage: device not open");
                return;
            }

            ftDev.setLatencyTimer((byte) 16);

            if (msg != null) {
                byte[] OutData = msg.getBytes();
                ftDev.write(OutData, msg.length());
                StringBuilder sb = new StringBuilder();
                for (int idx = 0; idx < msg.length(); idx++) {
                    sb.append(String.format(Locale.getDefault(), "%02x", OutData[idx])).append(" ");
                }
                String mData = sb.toString();
                tv.append("TX -> " + mData + "\n");
            }
        }
    }

    private class ReadThread  extends Thread
    {
        Handler mHandler;

        ReadThread(Handler h){
            mHandler = h;
            this.setPriority(Thread.MIN_PRIORITY);
        }

        @Override
        public void run()
        {

            while(true == isReading)
            {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
                int len=0;

                synchronized (ftDev) {
                    len = ftDev.getQueueStatus();
                }

                if(len > 0) {
                    if(len > readLength) len = readLength;
                    synchronized (ftDev) {
                        len = ftDev.read(readData, len, 10); // timeout 10ms
//                        len = ftDev.read(readData, len*2, 10); // timeout 10ms
                    }

                    StringBuilder sb = new StringBuilder();
                    for (int idx = 0; idx < len; idx++) {
                        sb.append(String.format(Locale.getDefault(), "%02x", readData[idx])).append(" ");
                    }
                    String mData = sb.toString();

                    Message msg = mHandler.obtainMessage();
                    msg.obj = mData;
                    mHandler.sendMessage(msg);

                }
                else {
                    synchronized (ftDev) {
                        ftDev.read(readData, 1, 1); // dummy read(1 byte), timeout 1ms
                    }
                }

            }
        }
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                // never come here(when attached, go to onNewIntent)
                openUsb();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                if(ftDev != null) {
                    ftDev.close();
                    isReading = false;
                }
            }
        }
    };

}
