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
    private static UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //SerialPortService ID // MY_UUID is the app's UUID string, also used by the client code.

    EditText et;
    ListView lv;
    CheckBox cbServer;

    /*
    ArrayList<String> chatHistory;
    //ArrayAdapter<String> chatHistory;
    //ArrayList<String> listItems=new ArrayList<String>();
    */

    private BluetoothAdapter adapter; //local adapter
    private BluetoothSocket socket; //local socket
    private InputStream is;  //data streams incoming
    private OutputStream os; //data stream outcoming
    private BluetoothDevice remoteDevice; //
    private Set<BluetoothDevice> pairedDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //UI
        lv = (ListView)findViewById(R.id.listView);
        et=(EditText) findViewById(R.id.txtMessage);
        cbServer = (CheckBox)findViewById(R.id.cbServer);
        cbServer.setChecked(true);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() //list onclick selection process
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                //handles listview onclick event
                String name = (String) parent.getItemAtPosition(position);

                selectBTdevice(name); //selected device will be set globally
                openBT(null); //starting to listen on port
            }
        });

        adapter = BluetoothAdapter.getDefaultAdapter(); //enables devices bluetooth
        if (adapter == null) //If the adapter is null, then Bluetooth is not supported
        {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        list(null); //shows already paired devices
        Toast.makeText(getApplicationContext(), "Pairing more devices through system" ,Toast.LENGTH_SHORT).show();
    }

    public void openBT(View v) //opening communication
    {
        if(adapter == null)
        {
            adapter = BluetoothAdapter.getDefaultAdapter();
        }

        /*
        final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, chatHistory);
        lv.setAdapter(adapter);
        */

        if(pairedDevices.isEmpty() || remoteDevice == null) //testing if devices are selected
        {
            Toast.makeText(getApplicationContext(), "Choose partner's device first" ,Toast.LENGTH_SHORT).show();
            return;
        }

        if(cbServer.isChecked())
        {
            //Toast.makeText(getApplicationContext(), "This device is server" ,Toast.LENGTH_SHORT).show();
            new Thread(serverListener).start(); //needs to run in different thread than UI

        }
        else //CLIENT
        {
            //Toast.makeText(getApplicationContext(), "This device is client" ,Toast.LENGTH_SHORT).show();
            new Thread(clientConnecter).start();
        }
    }

    private Runnable serverListener = new Runnable()
    {
        public void run()
        {
            adapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothServerSocket tmpsocket = null; //otice that the method “accept” in the BluetoothServerSocket class blocks the current execution thread until a client successfully connects, returning a BluetoothSocket instance that will be used for further communication between them.

            try
            {
                tmpsocket = adapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
                android.util.Log.e("TrackingFlow", "Listening...");
            }
            catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }

            try
            {
                socket = tmpsocket.accept();
                android.util.Log.e("TrackingFlow", "Socket accepted...");
            }
            catch (IOException e) {
                Log.e(TAG, "Socket's accept() method failed", e);
                e.printStackTrace();
            }

            try
            {
                is = socket.getInputStream();
                os = socket.getOutputStream();
                new Thread(writter).start();


                int bufferSize = 1024;
                int bytesRead = -1;
                byte[] buffer = new byte[bufferSize];

                while(CONTINUE_READ_WRITE) //Keep reading the messages while connection is open...
                {
                    final StringBuilder sb = new StringBuilder();
                    bytesRead = is.read(buffer);
                    if (bytesRead != -1) //making magic with byte transfer
                    {
                        String result = "";
                        while ((bytesRead == bufferSize) && (buffer[bufferSize-1] != 0))
                        {
                            result = result + new String(buffer, 0, bytesRead - 1);
                            bytesRead = is.read(buffer);
                        }
                        result = result + new String(buffer, 0, bytesRead - 1);
                        sb.append(result);
                    }
                    android.util.Log.e("TrackingFlow", "Read: " + sb.toString());

                    runOnUiThread(new Runnable() //Show message on UIThread
                    {
                        @Override
                        public void run()
                        {
                            //chatHistory.add(sb.toString());
                            //adapter.notifyDataSetChanged();
                            //adapter.add

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
            try
            {
                socket = remoteDevice.createRfcommSocketToServiceRecord(MY_UUID);
                socket.connect();
                android.util.Log.e("TrackingFlow", "Connected...");

                os = socket.getOutputStream();
                is = socket.getInputStream();
                new Thread(writter).start();

                int bufferSize = 1024;
                int bytesRead = -1;
                byte[] buffer = new byte[bufferSize];

                while(CONTINUE_READ_WRITE) //Keep reading the messages while connection is open...
                {
                    final StringBuilder sb = new StringBuilder();
                    bytesRead = is.read(buffer);
                    if (bytesRead != -1) //magic with bytes transfer
                    {
                        String result = "";
                        while ((bytesRead == bufferSize) && (buffer[bufferSize-1] != 0))
                        {
                            result = result + new String(buffer, 0, bytesRead - 1);
                            bytesRead = is.read(buffer);
                        }
                        result = result + new String(buffer, 0, bytesRead - 1);
                        sb.append(result);
                    }

                    android.util.Log.e("TrackingFlow", "Read: " + sb.toString());

                    runOnUiThread(new Runnable() //Show message on UIThread
                    {
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
            while(CONTINUE_READ_WRITE)
            {
                try
                {
                  //os.write("Message " + (index++) + "\n");
                    os.flush();
                    Thread.sleep(2000);
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Channel cleaning failed", e);
                    //e.printStackTrace();
                }
            }
        }
    };

    public void sendBtnClick(View v) //sending message to open output stream
    {
        String textToSend = et.getText().toString();
        byte[] b = textToSend.getBytes();

        try
        {
            os.write(b);
        }
        catch (IOException e){
            Toast.makeText(getApplicationContext(), "Not sent",Toast.LENGTH_SHORT).show();
        }
        et.setText(""); //reset input field after sending
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

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if(socket != null)
        {
            try
            {
                is.close();
                os.close();
                socket.close();
            }
            catch(Exception e)
            {
                android.util.Log.e("TrackingFlow", "Closing of streams failed when program is exiting.");
            }
            CONTINUE_READ_WRITE = false;
        }
    }
}

