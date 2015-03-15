package com.example.bluetooth.le;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class BluetoothAcceptThreadActivity extends Activity {

	public static String msgToSend;
	public static final int STATE_CONNECTION_STARTED = 0;
	public static final int STATE_CONNECTION_LOST = 1;
	public static final int READY_TO_CONN = 2;
	ConnectedThread mConnectedThread;
	BluetoothAdapter myBt;
	public String TAG = "log";
	public String NAME =" BLE";
	Handler handle;
	BroadcastReceiver receiver;
	
	ArrayList<BluetoothSocket> mSockets = new ArrayList<BluetoothSocket>();
	// list of addresses for devices we've connected to
	ArrayList<String> mDeviceAddresses = new ArrayList<String>();

	// We can handle up to 7 connections... or something...
	UUID[] uuids = new UUID[2];
	// some uuid's we like to use..
	String uuid1 = "00001101-0000-1000-8000-00805F9B34FB";
	String uuid2 = "c2911cd0-5c3c-11e3-949a-0800200c9a66";
	
	int REQUEST_ENABLE_BT = 1;
	AcceptThread accThread;
	TextView connectedDevice;
	
	public final BroadcastReceiver scaleDataReceiver = new BroadcastReceiver() {
	        @Override
	        public void onReceive(Context context, Intent intent) {
	        	
	            final String action = intent.getAction();
	            if("scale".equals(action)){
		            Log.d("accept","data in accept thread = " + intent.getStringExtra("data"));
		            Log.d("accept","------------------------------------------------");
		            mConnectedThread.write(ByteBuffer.allocate(4).putInt(5).array());
	            }
	        }
	  };
	
	private  IntentFilter makeUpdateIntentFilter() {
	        final IntentFilter intentFilter = new IntentFilter();
	        intentFilter.addAction("scale");
	        return intentFilter;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bluetooth_accept_thread);
		
		uuids[0] = UUID.fromString(uuid1);
		uuids[1] = UUID.fromString(uuid2);
		
		connectedDevice = (TextView)findViewById(R.id.connectedDevice);
		
		registerReceiver(scaleDataReceiver, makeUpdateIntentFilter());
		
		handle = new Handler(Looper.getMainLooper()) {

			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				// if connection is built.....
				case STATE_CONNECTION_STARTED:
					connectedDevice.setText("paired!");				
					 Toast.makeText(getApplicationContext(), "bluetooth connected",
					 2).show();
					 final Intent intent = new Intent(getApplicationContext(), DeviceScanActivity.class);
					 startActivity(intent);	
					 break;
				case STATE_CONNECTION_LOST:
					connectedDevice.setText("");
					// if the connection is broken, listening the device again
					startListening();
					break;
				case READY_TO_CONN:
					// if the connection is ready to go, start listening the
					// device
					startListening();
					break;
				case 5:
					 Toast.makeText(getApplicationContext(), "data get!",
							 2).show();
					 break;
					 
				default:
					break;
				}
			}

		};
		
		
		Button bt = new Button(this);
        bt.setText("next");
        bt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                final Intent intent = new Intent(getApplicationContext(), DeviceControlActivity.class);
                
                startActivity(intent);	
            }
        });
		
		
		myBt = BluetoothAdapter.getDefaultAdapter();
		// run the "go get em" thread..
		accThread = new AcceptThread();
		accThread.start();
		
	}
	
	
	public void startListening() {
		if (accThread != null) {
			accThread.cancel();
		} else if (mConnectedThread != null) {
			mConnectedThread.cancel();
		} else {
			accThread = new AcceptThread();
			accThread.start();
		}
	}
	
	public static class HostBroadRec extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle b = intent.getExtras();
			String vals = "";
			for (String key : b.keySet()) {
				vals += key + "&" + b.getString(key) + "Z";
			}
			BluetoothAcceptThreadActivity.setMsg(vals);
		}
	}
	
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			
			Log.d(TAG, "create ConnectedThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}
			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectedThread");
			byte[] buffer = new byte[4];
			int bytes;
		}

		public void connectionLost() {
			Message msg = handle.obtainMessage(STATE_CONNECTION_LOST);
			handle.sendMessage(msg);
		}

		/**
		 * Write to the connected OutStream.
		 * 
		 * @param buffer
		 *            The bytes to write
		 */
		public void write(byte[] buffer) {
			try {
				// Toast.makeText(getApplicationContext(), "write buffer!",
				// 1).show();
				mmOutStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
				connectionLost();
			}
		}

		public void cancel() {
			try {
				mmSocket.close();
				Message msg = handle.obtainMessage(READY_TO_CONN);
				handle.sendMessage(msg);
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	public static synchronized void setMsg(String newMsg) {
		msgToSend = newMsg;
	}
	
	public class AcceptThread extends Thread {
		private BluetoothServerSocket mmServerSocket;
		BluetoothServerSocket tmp;

		public AcceptThread() {
			BluetoothServerSocket tmp = null;
			try {
				tmp = myBt.listenUsingRfcommWithServiceRecord(NAME, uuids[0]);
			} catch (IOException e) {
			}
			mmServerSocket = tmp;
		}

		public void run() {
			Log.e(TAG, "Running");
			BluetoothSocket socket = null;
			// Keep listening until exception occurs or a socket is returned
			while (true) {

				try {
					socket = mmServerSocket.accept();
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
				// If a connection was accepted

				if (socket != null) {
					// if the connection has been built, then close the server
					// socket..
					try {
						mmServerSocket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// Do work to manage the connection (in a separate thread)
					manageConnectedSocket(socket);
					break;
				}
			}
		}

		/** Will cancel the listening socket, and cause the thread to finish */
		public void cancel() {
			try {
				mmServerSocket.close();
				Message msg = handle.obtainMessage(READY_TO_CONN);
				handle.sendMessage(msg);

			} catch (IOException e) {
			}
		}
	}

	private void manageConnectedSocket(BluetoothSocket socket) {
		// start our connection thread
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();
		int a = 5;
		//mConnectedThread.write(ByteBuffer.allocate(4).putInt(a).array());
		// Send the name of the connected device back to the UI Activity
		// so the HH can show you it's working and stuff...
		String devs = "";
		for (BluetoothSocket sock : mSockets) {
			devs += sock.getRemoteDevice().getName() + "\n";
		}
		// pass it to the pool....
		Message msg = handle.obtainMessage(STATE_CONNECTION_STARTED);
		Bundle bundle = new Bundle();
		bundle.putString("NAMES", devs);
		msg.setData(bundle);
		handle.sendMessage(msg);
	}
}
