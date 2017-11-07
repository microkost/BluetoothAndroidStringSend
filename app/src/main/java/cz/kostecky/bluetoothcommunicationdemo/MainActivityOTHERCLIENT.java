package cz.kostecky.bluetoothcommunicationdemo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Toast;

import cz.kostecky.bluetoothcommunicationdemo.R;

public class MainActivityOTHERCLIENT extends Activity {
	
	private boolean CONTINUE_READ_WRITE = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		registerReceiver(discoveryResult, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if(adapter != null && adapter.isDiscovering()){
			adapter.cancelDiscovery();
		}
		adapter.startDiscovery();
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		try{unregisterReceiver(discoveryResult);}catch(Exception e){e.printStackTrace();}
		if(socket != null){
			try{
				is.close();
				os.close();
				socket.close();
				CONTINUE_READ_WRITE = false;
			}catch(Exception e){}
		}
	}
	
	private BluetoothSocket socket;
	private OutputStreamWriter os;
	private InputStream is;
	private BluetoothDevice remoteDevice;
	private BroadcastReceiver discoveryResult = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			android.util.Log.e("TrackingFlow", "WWWTTTFFF");
			unregisterReceiver(this);
			remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			new Thread(reader).start();
		}
	};
	
	private Runnable reader = new Runnable() {
		
		@Override
		public void run() {
			try {
				android.util.Log.e("TrackingFlow", "Found: " + remoteDevice.getName());
				UUID uuid = UUID.fromString("4e5d48e0-75df-11e3-981f-0800200c9a66");
				socket = remoteDevice.createRfcommSocketToServiceRecord(uuid);
				socket.connect();
				android.util.Log.e("TrackingFlow", "Connected...");
				os = new OutputStreamWriter(socket.getOutputStream());
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
							Toast.makeText(MainActivityOTHERCLIENT.this, sb.toString(), Toast.LENGTH_LONG).show();
						}
					});
				}
			} catch (IOException e) {e.printStackTrace();}
		}
	};
	
	private Runnable writter = new Runnable() {

		@Override
		public void run() {
			int index = 0;
			while (CONTINUE_READ_WRITE) {
				try {
					os.write("Message From Client" + (index++) + "\n");
					os.flush();
					Thread.sleep(2000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	};
}
