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
import android.os.Bundle;
import android.os.Handler;
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
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.getDefaultAdapter;
import static android.content.ContentValues.TAG;

public class MainActivity extends Activity
{
    private boolean CONTINUE_READ_WRITE = true;
    private boolean CONNECTION_ENSTABLISHED = false;
    private boolean DEVICES_IN_LIST = true;

    private static String NAME = "cz.kostecky.bluetoothcommunicationdemo"; //id of app
    private static UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //SerialPortService ID // MY_UUID is the app's UUID string, also used by the client code.

    Button bSend;
    EditText et;
    ListView lv;
    CheckBox cbServer;
    ArrayList<String> listItems;
    ArrayAdapter<String> listAdapter;

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
            unregisterReceiver(this);
            remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //UI
        bSend = (Button) findViewById(R.id.button7);
        lv = (ListView)findViewById(R.id.listView);
        et=(EditText) findViewById(R.id.txtMessage);
        cbServer = (CheckBox)findViewById(R.id.cbServer);
        cbServer.setChecked(true);

        listItems = new ArrayList<String>(); //shows messages in list view
        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems);
        lv.setAdapter(listAdapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() //list onclick selection process
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                if(DEVICES_IN_LIST)
                {
                    String name = (String) parent.getItemAtPosition(position);
                    selectBTdevice(name); //selected device will be set globally
                    //Toast.makeText(getApplicationContext(), "Selected " + name, Toast.LENGTH_SHORT).show();
                    //openBT(null);
                }
                else //message is selected
                {
                    String message = (String) parent.getItemAtPosition(position);
                    et.setText(message);
                }
            }
        });

        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) //If the adapter is null, then Bluetooth is not supported
        {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        list(null);
    }

    public void openBT(View v)
    {
        if(adapter == null)
        {
            adapter = getDefaultAdapter();
            Log.i(TAG, "Backup way of getting adapter was used!");
        }

        CONTINUE_READ_WRITE = true; //writer tiebreaker
        socket = null; //resetting if was used previously
        //is = null; //resetting if was used previously
        //os = null; //resetting if was used previously

        if(pairedDevices.isEmpty() || remoteDevice == null)
        {
            Toast.makeText(this, "Paired device is not selected", Toast.LENGTH_SHORT).show();
            return;
        }

        if(cbServer.isChecked())
        {
            //Toast.makeText(getApplicationContext(), "This device is server" ,Toast.LENGTH_SHORT).show();
            new Thread(serverListener).start();
        }
        else //CLIENT
        {
            //Toast.makeText(getApplicationContext(), "This device is client" ,Toast.LENGTH_SHORT).show();
            new Thread(clientConnecter).start();
        }
    }

    public void closeBT(View v) //for closing opened communications, cleaning used resources
    {
        /*if(adapter == null)
            return;*/

        CONTINUE_READ_WRITE = false;
        CONNECTION_ENSTABLISHED = false;

        if (is != null) {
            try {is.close();} catch (Exception e) {}
            is = null;
        }

        if (os != null) {
            try {os.close();} catch (Exception e) {}
            os = null;
        }

        if (socket != null) {
            try {socket.close();} catch (Exception e) {}
            socket = null;
        }

        try {
            Handler mHandler = new Handler();
            mHandler.removeCallbacksAndMessages(writter);
            mHandler.removeCallbacksAndMessages(serverListener);
            mHandler.removeCallbacksAndMessages(clientConnecter);
            Log.i(TAG, "Threads ended...");
        }catch (Exception e)
        {
            Log.e(TAG, "Attemp for closing threads was unsucessfull.");
        }

        Toast.makeText(getApplicationContext(), "Communication closed" ,Toast.LENGTH_SHORT).show();
        //list(null);
    }

    private Runnable serverListener = new Runnable()
    {
        public void run()
        {
            BluetoothServerSocket tmpsocket = null;

            /*
                try //opening of BT connection
                {
                    android.util.Log.i("TrackingFlow", "new way used...");
                    socket =(BluetoothSocket) remoteDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(remoteDevice,1);
                    socket.connect();
                    CONNECTION_ENSTABLISHED = true; //protect from failing
                    listItems.clear(); //remove chat history

                } catch(Exception e) //obsolete way how to open BT
                {
                    android.util.Log.e("TrackingFlow", "old way used...");
                    */
                    try
                    {
                        tmpsocket = adapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
                        socket = tmpsocket.accept();
                        android.util.Log.i("TrackingFlow", "Listening...");
                        CONNECTION_ENSTABLISHED = true; //protect from failing
                        listItems.clear(); //remove chat history
                    }
                    catch (IOException ie)
                    {
                        Log.e(TAG, "Socket's accept method failed", ie);
                        ie.printStackTrace();
                    }
                //}

            if(CONNECTION_ENSTABLISHED != true)
            {
                Log.e(TAG, "Server is NOT ready for listening...");
                return;
            }
            else
                Log.i(TAG, "Server is ready for listening...");

            try //reading part
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
                    if (bytesRead != -1) {
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

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() { //Show message on UIThread
                            Toast.makeText(MainActivity.this, sb.toString(), Toast.LENGTH_SHORT).show();
                            listItems.add(0, String.format("< %s", sb.toString())); //showing in history
                            listAdapter.notifyDataSetChanged();
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
                Log.i(TAG, "Socket ready...");
                socket.connect();
                Log.i(TAG, "Connect done...");
                CONNECTION_ENSTABLISHED = true; //protect from failing
                listItems.clear(); //remove chat history

                if(CONNECTION_ENSTABLISHED != true)
                {
                    Log.e(TAG, "Client is NOT ready for listening...");
                    return;
                }
                else
                    Log.i(TAG, "Client is connected...");

                try
                {
                    os = socket.getOutputStream();
                    is = socket.getInputStream();
                    new Thread(writter).start();
                    Log.i(TAG, "Preparation for reading was done");
                }
                catch (Exception e)
                {
                    Log.e(TAG, "Failed preparation for reading");
                    return;
                }

                int bufferSize = 1024;
                int bytesRead = -1;
                byte[] buffer = new byte[bufferSize];

                while(CONTINUE_READ_WRITE) //Keep reading the messages while connection is open...
                {
                    final StringBuilder sb = new StringBuilder();
                    bytesRead = is.read(buffer);
                    if (bytesRead != -1)
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


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() { //Show message on UIThread
                            Toast.makeText(MainActivity.this, sb.toString(), Toast.LENGTH_SHORT).show();
                            listItems.add(0, String.format("< %s", sb.toString()));
                            listAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }
            catch (IOException e)
            {
                Toast.makeText(MainActivity.this, "Not connected...", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    };


    private Runnable writter = new Runnable() {

        @Override
        public void run() {
            while (CONTINUE_READ_WRITE)
            {
                try
                {
                    os.flush();
                    Thread.sleep(2000);
                } catch (Exception e)
                {
                    Log.e(TAG, "Writer failed in flushing output stream...");
                    Toast.makeText(getApplicationContext(), "Not sent",Toast.LENGTH_SHORT).show();
                    //e.printStackTrace();
                }
            }
        }
    };


    public void sendBtnClick(View v) //sends text from text button
    {
        if(CONNECTION_ENSTABLISHED == false)
        {
            Toast.makeText(getApplicationContext(), "Connection between devices is not ready.", Toast.LENGTH_SHORT).show(); //usually problem server-client decision
        }
        else
        {
            String textToSend = et.getText().toString() + "X"; //method is cutting last character, so way how to cheat it...
            byte[] b = textToSend.getBytes();
            try
            {
                os.write(b);
                listItems.add(0, "> " + et.getText().toString()); //chat history
                listAdapter.notifyDataSetChanged();
                et.setText(""); //remove text after sending
            } catch (IOException e)
            {
                Toast.makeText(getApplicationContext(), "Not sent", Toast.LENGTH_SHORT).show(); //usually problem server-client decision
            }
        }
    }

    public void on(View v) //turning on BT when is off
    {
        if (!adapter.isEnabled())
        {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on",Toast.LENGTH_SHORT).show();
        } else
        {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_SHORT).show();
        }

        list(null); //list devices automatically to UI
    }

    public void off(View v) //turning off BT device on phone
    {
        adapter.disable();
        Toast.makeText(getApplicationContext(), "Turned off" ,Toast.LENGTH_SHORT).show();
    }

    public void visible(View v) //BT device discoverable
    {
        //private static final int DISCOVERABLE_REQUEST_CODE = 0x1;
        Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(getVisible, 0);
    }

    public void list(View v)
    {
        CONNECTION_ENSTABLISHED = false; //protect from failing
        listItems.clear(); //remove chat history

        pairedDevices = adapter.getBondedDevices(); //list of devices
        //ArrayList list = new ArrayList(); //for UI

        for(BluetoothDevice bt : pairedDevices) //foreach
        {
            //list.add(bt.getName());
            listItems.add(0, bt.getName());
        }
        listAdapter.notifyDataSetChanged(); //reload UI

        //final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        //listAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        //lv.setAdapter(listAdapter);
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
}

