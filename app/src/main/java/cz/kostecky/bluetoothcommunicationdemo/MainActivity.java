package cz.kostecky.bluetoothcommunicationdemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
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
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity
{
    private BluetoothAdapter mBluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;

    private BluetoothSocket mmSocket;
    //private BluetoothServerSocket mmServerSocket;
    private BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;

    private static String NAME = "cz.kostecky.bluetoothcommunicationdemo"; //id of app
    private static UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fc"); //SerialPortService ID

    Button b1,b2,b3,b4;
    EditText et;
    ListView lv;
    CheckBox cbServer;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) //If the adapter is null, then Bluetooth is not supported
        {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        //UI
        b1 = (Button) findViewById(R.id.button1);
        b2=(Button)findViewById(R.id.button2);
        b3=(Button)findViewById(R.id.button3);
        b4=(Button)findViewById(R.id.button4);
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

        list(null); //list devices automatically to UI
    }

    public void on(View v) //turning on BT when is off
    {
        if (!mBluetoothAdapter.isEnabled())
        {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on",Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
        }
    }

    public void off(View v) //turning off BT device on phone
    {
        mBluetoothAdapter.disable();
        Toast.makeText(getApplicationContext(), "Turned off" ,Toast.LENGTH_LONG).show();
    }

    public void visible(View v) //BT device discoverable
    {
        Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(getVisible, 0);
    }

    public void list(View v)
    {
        pairedDevices = mBluetoothAdapter.getBondedDevices(); //list of devices
        ArrayList list = new ArrayList(); //for UI

        for(BluetoothDevice bt : pairedDevices) //foreach
        {
            list.add(bt.getName());
        }
        Toast.makeText(getApplicationContext(), "Showing Paired Devices", Toast.LENGTH_SHORT).show();

        final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        lv.setAdapter(adapter);
    }

    public String GetNameOfDevice()  //returning name of this device
    {
        String status;
        if (mBluetoothAdapter.isEnabled())
        {
            String mydeviceaddress = mBluetoothAdapter.getAddress();
            String mydevicename = mBluetoothAdapter.getName();
            status = mydevicename + " : " + mydeviceaddress;
        }
        else
        {
            status = "Bluetooth is not Enabled.";
        }

        //Toast.makeText(this, status, Toast.LENGTH_LONG).show();
        return status;
    }

    public void selectBTdevice(String name) //for selecting device which is used in procedures
    {
        if(pairedDevices.isEmpty()){
            list(null);
            Toast.makeText(getApplicationContext(), "Selecting was unsucessful, no devices in list." ,Toast.LENGTH_SHORT ).show();
        }

        for(BluetoothDevice bt : pairedDevices) //foreach
        {
            if(name.equals(bt.getName()))
            {
                mmDevice = bt;
                Toast.makeText(getApplicationContext(), "Selected " + mmDevice.getName() + " permanently", Toast.LENGTH_SHORT ).show();
            }
        }
    }

    public void openBT(View v)
    {
        //TODO if selected device empty - take action

        if(cbServer.isChecked())
        {
            Toast.makeText(getApplicationContext(), "This device is server" ,Toast.LENGTH_SHORT).show();
            new AcceptThread().run();

        }
        else
        {
            Toast.makeText(getApplicationContext(), "This device is client" ,Toast.LENGTH_SHORT).show();
            new ConnectThread(mmDevice).run();
        }

        /*
        try
        {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard //SerialPortService ID
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            mmSocket.connect();
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();
            //beginListenForData();
            Toast.makeText(getApplicationContext(), "Bluetooth Opened" ,Toast.LENGTH_SHORT).show();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Bluetooth connection failed" ,Toast.LENGTH_SHORT).show();
            //throw new RuntimeException(e);
        }
        */
    }

    public void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        final boolean[] stopWorker = {false};
        final int[] readBufferPosition = {0};
        final byte[] readBuffer = new byte[1024];
        Thread workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while (!Thread.currentThread().isInterrupted() && !stopWorker[0])
                {
                    try {
                        int bytesAvailable = mmInputStream.available();
                        if (bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition[0]];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition[0] = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT).show();
                                            //myLabel.setText(data);
                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition[0]++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        stopWorker[0] = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    public void sendData()
    {
        String msg = et.getText().toString();
        msg += "\n";

        try
        {
            mmOutputStream.write(msg.getBytes());
            mmOutputStream.write('A');
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        Toast.makeText(getApplicationContext(), "Data Sent" ,Toast.LENGTH_SHORT).show();
        //myLabel.setText("Data Sent");
    }

    private void manageMyConnectedSocket(BluetoothSocket mmSocket)
    {
        //my own testing way of this method

        Toast.makeText(this, "it is here", Toast.LENGTH_LONG).show();

        if(cbServer.isChecked()) {
            //server
            beginListenForData();
        }
        else {
            //client
            sendData();
        }
    }

    private class AcceptThread extends Thread //server
    {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread()
        {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try
            {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            }
            catch (IOException e)
            {
                //Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.

                    Toast.makeText(getApplicationContext(), "sucessfull server" ,Toast.LENGTH_SHORT).show(); //DEBUG
                    manageMyConnectedSocket(socket);

                    try
                    {
                        mmServerSocket.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    } //accept

    private class ConnectThread extends Thread //client
    {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device)
        {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Toast.makeText(getApplicationContext(), "mmsocket connected" ,Toast.LENGTH_SHORT).show(); //DEBUG
            }
            catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            Toast.makeText(getApplicationContext(), "sucessfull client" ,Toast.LENGTH_SHORT).show(); //DEBUG
            manageMyConnectedSocket(mmSocket);

        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }


}