package com.example.arduinousb;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import java.sql.Connection;

import java.io.UnsupportedEncodingException;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;



/*
        1. Close Firewall
        2. Create DB user "zzz", pw: "zzz"
        3. Check IP Address
        4. Keep Data Enabled
 */



public class CommunicateActivity extends Activity {
    private static final String DB_URL = "jdbc:mysql://192.168.25.124/smart sweet co";
    private static final String USER = "zzz";
    private static final String PASS = "zzz";

    public final String ACTION_USB_PERMISSION = "com.example.arduinousb.USB_PERMISSION";
    Button startButton, /*sendButton,*/ clearButton, stopButton, btnGame, btnRecycle, btnTest, btnTrade;
    TextView textView, tvTestArea;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    boolean deviceDetected = false;

    String receivedData = "";
    boolean needToClick = false;
    WifiManager wifi;

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                String[] re = data.split(",", 2);
                receivedData = data;
                if(re.length > 1)
                    needToClick = true;
                else
                    needToClick = false;
                data.concat("/n");
                tvAppend(textView, data);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };

    Send objSend;
    String cardID = "";
    public void getReceivedMessage(View view){
        if(needToClick) {
            String dataType, data;
            String[] splitMessage = receivedData.split(",", 2);
            dataType = splitMessage[0];
            data = splitMessage[1];
            if (dataType.equals("Connected") && data.equals("Click Next")) {
                String string = "identifyUser";
                serialPort.write(string.getBytes());
                tvAppend(textView,"\n");
            } else if (data.equals("Click Next and Select an Action")) {
                tvTestArea.setText(" ");
                btnRecycle.setEnabled(true);
                btnGame.setEnabled(true);
                btnTrade.setEnabled(true);
                cardID = dataType;
                objSend = new Send("select");
                objSend.execute("");
                tvTestArea.invalidate();
            } else if (dataType.equals("Click Next to get Score")){
                userScore += Integer.parseInt(data);
                tvTestArea.setText(" ");
                objSend = new Send("update");
                objSend.execute("");
                needToClick = false;
            }
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            setUiEnabled(true);
                            btnTest.setEnabled(true);
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            tvAppend(textView,"Serial Connection Opened!\n");
                            String string = "opened";
                            serialPort.write(string.getBytes());
                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart(startButton);
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                onClickStop(stopButton);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_communicate);
        setContentView(R.layout.activity_test);
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        startButton = (Button) findViewById(R.id.buttonStart);
        clearButton = (Button) findViewById(R.id.buttonClear);
        stopButton = (Button) findViewById(R.id.buttonStop);
        btnGame = (Button) findViewById(R.id.btnGame);
        btnRecycle = (Button) findViewById(R.id.btnRecycle);
        textView = (TextView) findViewById(R.id.textView);
        tvTestArea = (TextView) findViewById(R.id.tvTestArea);
        btnTest = (Button) findViewById(R.id.btnTest);
        btnTrade = (Button) findViewById(R.id.btnTrade);
        setUiEnabled(false);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);
        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        btnRecycle.setEnabled(false);
        btnGame.setEnabled(false);
        btnTrade.setEnabled(false);
        wifi.setWifiEnabled(true);
        startButton.performClick();
    }

    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        stopButton.setEnabled(bool);
        textView.setEnabled(bool);
    }

    public void onClickStart(View view) {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x1a86)//Arduino Vendor ID 0x2341
                {
                    //request for permission
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }
                if (!keep)
                    break;
            }
        }
        else{
            deviceDetected = false;
            Toast.makeText(this, "No device is detected", Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickStop(View view) {
        setUiEnabled(false);
        serialPort.close();
        btnTest.setEnabled(false);
        btnGame.setEnabled(false);
        btnRecycle.setEnabled(false);
        tvTestArea.setText(" ");
        textView.setText(" ");
        tvAppend(textView,"Serial Connection Closed! \n");
    }

    public void onClickClear(View view) {
        textView.setText(" ");
        tvTestArea.setText(" ");
    }

    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        });
    }

    public void startRecycleActivity(View view){
        wifi.setWifiEnabled(false);

        Intent myIntent = new Intent(CommunicateActivity.this, RecognizingActivity.class);
        //  myIntent.putExtra("key", value); //Optional parameters
        CommunicateActivity.this.startActivityForResult(myIntent, 898);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==898)
        {
            tvTestArea.setText(" ");
            int score = data.getIntExtra("score", 0);
            userScore += score;
            objSend.cancel(true);
            objSend = new Send("update");
            objSend.execute("");
            btnTest.performClick();
            tvTestArea.invalidate();
            Toast.makeText(this, "Score Updated", Toast.LENGTH_SHORT).show();
        }
    }

    String userID, userName = "";
    int userScore = 0;
    double userBalance = 0;
    private class Send extends AsyncTask<String, String, String> {
        private String option;
        public Send(String option){
            this.option = option;
        }

        String msg = "";
    //    String text = etData.getText().toString();

        @Override
        protected String doInBackground(String... strings){
            try{
                Class.forName("com.mysql.jdbc.Driver");
                Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                if(conn == null)
                    msg = "Connection goes wrong";
                else if (option.equals("select")){
                    String query = "SELECT customer_id FROM membership_card WHERE id = " + cardID;
                    Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(query);
                    while (rs.next()) {
                        userID  = rs.getString("customer_id");
                    }
                 //   tvTestArea.setText("User ID: " + userID);
                    tvAppend(tvTestArea, "User ID: " + userID + "\n");
                    query = "SELECT name, score, balance FROM customer WHERE id = " + userID;
                    rs = stmt.executeQuery(query);
                    while(rs.next()){
                        userName = rs.getString("name");
                        userScore = rs.getInt("score");
                        userBalance = rs.getDouble("balance");
                    }
                    tvAppend(tvTestArea, "User Name: " + userName + "\n");
                    tvAppend(tvTestArea, "User Score: " + userScore + "\n");
                    tvAppend(tvTestArea, "User Balance: " + userBalance + "\n");
                }else if (option.equals("update")){
                    String query = "UPDATE customer SET score = " + userScore + " WHERE id = " + userID;
                    Statement stmt = conn.createStatement();
                    stmt.executeUpdate(query);
                    tvAppend(tvTestArea, "User ID: " + userID + "\n");
                    tvAppend(tvTestArea, "User Name: " + userName + "\n");
                    tvAppend(tvTestArea, "User Score: " + userScore + "\n");
                    tvAppend(tvTestArea, "User Balance: " + userBalance + "\n");
                }else if (option.equals("trade")){
                    String query = "UPDATE customer SET balance = " + userBalance + " WHERE id = " + userID;
                    Statement stmt = conn.createStatement();
                    stmt.executeUpdate(query);
                    userScore = 0;
                    query = "UPDATE customer SET score = " + userScore + " WHERE id = " + userID;
                    stmt = conn.createStatement();
                    stmt.executeUpdate(query);
                    tvAppend(tvTestArea, "User ID: " + userID + "\n");
                    tvAppend(tvTestArea, "User Name: " + userName + "\n");
                    tvAppend(tvTestArea, "User Score: " + userScore + "\n");
                    tvAppend(tvTestArea, "User Balance: " + userBalance + "\n");
                }
                conn.close();
            }
            catch (Exception e){
                msg = "Connection goes wrong!";
                e.printStackTrace();
            }
            return msg;
        }
    }

    public void startGame(View view){
        textView.setText(" ");
        tvAppend(textView,"Please put the ball\n");
        String string = "game";
        serialPort.write(string.getBytes());
    }

    public void tradeScore(View view){
        if (userScore == 0){
            Toast.makeText(this, "No score to trade", Toast.LENGTH_SHORT).show();
        }else{
            tvTestArea.setText(" ");
            userBalance += userScore;
            objSend.cancel(true);
            objSend = new Send("trade");
            objSend.execute("");
            tvTestArea.invalidate();
            Toast.makeText(this, "Traded", Toast.LENGTH_SHORT).show();
        }
    }

}


