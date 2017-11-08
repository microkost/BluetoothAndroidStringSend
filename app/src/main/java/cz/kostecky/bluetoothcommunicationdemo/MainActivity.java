//DEMO http://www.doepiccoding.com/blog/?p=232

package cz.kostecky.bluetoothcommunicationdemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity
{
    private boolean CONTINUE_READ_WRITE = true;

    private static String NAME = "cz.kostecky.bluetoothcommunicationdemo"; //id of app
    private static UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //SerialPortService ID

    Button b1,b2,b3,b4;
    EditText et;
    ListView lv;
    CheckBox cbServer;

    private BluetoothAdapter adapter;
    private BluetoothSocket socket;
    private InputStream is;
    private OutputStream os;
    private BluetoothDevice remoteDevice;
    private Set<BluetoothDevice> pairedDevices;

    private BroadcastReceiver discoveryResult = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            android.util.Log.e("TrackingFlow", "WWWTTTFFF");
            unregisterReceiver(this);
            remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            //new Thread(readerClient).start();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //UI
        //b1 = (Button) findViewById(R.id.button1);
        //b2=(Button)findViewById(R.id.button2);
        //b3=(Button)findViewById(R.id.button3);
        //b4=(Button)findViewById(R.id.button4);
        lv = (ListView)findViewById(R.id.listView);
        et=(EditText) findViewById(R.id.txtMessage);
        cbServer = (CheckBox)findViewById(R.id.cbServer);
        cbServer.setChecked(true);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() //list onclick selection process
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                String name = (String) parent.getItemAtPosition(position);
                //Toast.makeText(getApplicationContext(), "Selected " + name, Toast.LENGTH_LONG).show();
                selectBTdevice(name); //selected device will be set globally
            }
        });

        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) //If the adapter is null, then Bluetooth is not supported
        {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        list(null);
    }

    /*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        android.util.Log.e("TrackingFlow", "Creating thread to start listening...");
        new Thread(readerServer).start();
    }
    */

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        try {unregisterReceiver(discoveryResult);}catch(Exception e){e.printStackTrace();}
        if(socket != null){
            try
            {
                is.close();
                os.close();
                socket.close();
                CONTINUE_READ_WRITE = false;
            }
            catch(Exception e){}
            CONTINUE_READ_WRITE = false;
        }
    }

    private Runnable serverListener = new Runnable()
    {
        public void run()
        {
            //Toast.makeText(getApplicationContext(), "readerServer is running" ,Toast.LENGTH_SHORT).show();

            adapter = BluetoothAdapter.getDefaultAdapter();

            //otice that the method “accept” in the BluetoothServerSocket class blocks the current execution thread until a client successfully connects, returning a BluetoothSocket instance that will be used for further communication between them.

            BluetoothServerSocket tmpsocket = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmpsocket = adapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            }
            catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }

            android.util.Log.e("TrackingFlow", "Listening...");

            try {
                socket = tmpsocket.accept();
            }
            catch (IOException e) {
                Log.e(TAG, "Socket's accept() method failed", e);
                e.printStackTrace();
            }


            try{
                android.util.Log.e("TrackingFlow", "Socket accepted...");
                is = socket.getInputStream();
                os = socket.getOutputStream();
                new Thread(writter).start();


                int bufferSize = 1024;
                int bytesRead = -1;
                byte[] buffer = new byte[bufferSize];
                //Keep reading the messages while connection is open...
                while(CONTINUE_READ_WRITE){
                    final StringBuilder sb = new StringBuilder();
                    bytesRead = is.read(buffer);
                    if (bytesRead != -1) {
                        String result = "";
                        while ((bytesRead == bufferSize) && (buffer[bufferSize-1] != 0)){
                            result = result + new String(buffer, 0, bytesRead - 1);
                            bytesRead = is.read(buffer);
                        }
                        result = result + new String(buffer, 0, bytesRead - 1);
                        sb.append(result);
                    }
                    android.util.Log.e("TrackingFlow", "Read: " + sb.toString());
                    //Show message on UIThread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
            catch(IOException e){
                Log.e(TAG, "Error with...", e);
                e.printStackTrace();
            }



        }
    };


    private Runnable clientConnecter = new Runnable()
    {
        @Override
        public void run()
        {
            //Toast.makeText(getApplicationContext(), "readerClient is running" ,Toast.LENGTH_SHORT).show();

            try
            {
                android.util.Log.e("TrackingFlow", "Found: " + remoteDevice.getName());

                socket = remoteDevice.createRfcommSocketToServiceRecord(MY_UUID);
                socket.connect();
                android.util.Log.e("TrackingFlow", "Connected...");

                os = socket.getOutputStream();
                is = socket.getInputStream();
                android.util.Log.e("TrackingFlow", "WWWTTTFFF34243");
                new Thread(writter).start();
                android.util.Log.e("TrackingFlow", "WWWTTTFFF3wwgftggggwww4243: " + CONTINUE_READ_WRITE);
                int bufferSize = 1024;
                int bytesRead = -1;
                byte[] buffer = new byte[bufferSize];
                //Keep reading the messages while connection is open...
                while(CONTINUE_READ_WRITE){
                    android.util.Log.e("TrackingFlow", "WWWTTTFFF3wwwww4243");
                    final StringBuilder sb = new StringBuilder();
                    bytesRead = is.read(buffer);
                    if (bytesRead != -1) {
                        String result = "";
                        while ((bytesRead == bufferSize) && (buffer[bufferSize-1] != 0)){
                            result = result + new String(buffer, 0, bytesRead - 1);
                            bytesRead = is.read(buffer);
                        }
                        result = result + new String(buffer, 0, bytesRead - 1);
                        sb.append(result);
                    }

                    android.util.Log.e("TrackingFlow", "Read: " + sb.toString());

                    //Show message on UIThread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, sb.toString(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
            catch (IOException e) {e.printStackTrace();}
        }
    };


    private Runnable writter = new Runnable() {

        @Override
        public void run() {
            int index = 0;
            while (CONTINUE_READ_WRITE) {
                try {
                    //os.write("Message " + (index++) + "\n");
                    os.flush();
                    Thread.sleep(2000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public void openBT(View v)
    {
        //TODO if selected device empty - take action
        //TODO if bt is not allowed!

        if(cbServer.isChecked())
        {
            Toast.makeText(getApplicationContext(), "This device is server" ,Toast.LENGTH_SHORT).show();

            //Always make sure that Bluetooth server is discoverable during listening...
            //visible(null);

            //run action
            //AsyncTask.execute(readerServer); //no UI interaction
            new Thread(serverListener).start();

        }
        else //CLIENT
        {
            Toast.makeText(getApplicationContext(), "This device is client" ,Toast.LENGTH_SHORT).show();
            new Thread(clientConnecter).start();
        }

    }


    public void sendBtnClick(View v){
        String textToSend = et.getText().toString();
        byte[] b = textToSend.getBytes();

        try {
            os.write(b);
        } catch (IOException e) { }
    }

    public void on(View v) //turning on BT when is off
    {
        if (!adapter.isEnabled())
        {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on",Toast.LENGTH_LONG).show();
        } else
            {
                Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
            }

        list(null); //list devices automatically to UI
    }

    public void off(View v) //turning off BT device on phone
    {
        adapter.disable();
        Toast.makeText(getApplicationContext(), "Turned off" ,Toast.LENGTH_LONG).show();
    }

    public void visible(View v) //BT device discoverable
    {
        //private static final int DISCOVERABLE_REQUEST_CODE = 0x1;
        Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(getVisible, 0);
    }

    public void list(View v)
    {
        pairedDevices = adapter.getBondedDevices(); //list of devices
        ArrayList list = new ArrayList(); //for UI

        for(BluetoothDevice bt : pairedDevices) //foreach
        {
            list.add(bt.getName());
        }
        Toast.makeText(getApplicationContext(), "Showing Paired Devices", Toast.LENGTH_SHORT).show();

        final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        lv.setAdapter(adapter);
    }

    public void selectBTdevice(String name) //for selecting device from list which is used in procedures
    {
        if(pairedDevices.isEmpty()) {
            list(null);
            Toast.makeText(getApplicationContext(), "Selecting was unsucessful, no devices in list." ,Toast.LENGTH_SHORT ).show();
        }

        for(BluetoothDevice bt : pairedDevices) //foreach
        {
            if(name.equals(bt.getName()))
            {
                remoteDevice = bt;
                Toast.makeText(getApplicationContext(), "Selected " + remoteDevice.getName(), Toast.LENGTH_SHORT ).show();
            }
        }
    }
}

