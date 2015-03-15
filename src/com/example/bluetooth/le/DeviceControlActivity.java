/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bluetooth.le;

import android.R.integer;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    //public int total;
    public int counter = 0;
//    public int[] vals = new int[3];
    private TextView mConnectionState;
    private TextView mDataField;
    private TextView mDataField1;
    private TextView mDataField2;
    //for synchronization
    public int monitor = 2; 
    private String mDeviceName;
    private String mDeviceAddress;
    private Thread t;
    private ExpandableListView mGattServicesList;
    public ArrayList<BLEThread> threads = new ArrayList<BLEThread>();
    //added lists
    //private List<BluetoothLeService> mBluetoothLeServices=null;
    //private List<ServiceConnection> mServiceConnections=null;
    //private List<BroadcastReceiver> mGattUpdateReceivers=null;
   // private BluetoothLeService mBluetoothLeService;
    //private BluetoothLeService mBluetoothLeService1;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    
    public int index = 0;
    //add function later: scan through device list and add mac address to bd list.
    public String[] bd = {"78:A5:04:3F:61:7B","78:A5:04:3F:B9:18","78:A5:04:3F:70:B6"};
    public int len = bd.length;
    public int[] vals = new int[len];
    public ExecutorService executor = Executors.newFixedThreadPool(len);
    
    public ArrayList<Future> f = new ArrayList<Future>();
    
    public ArrayList<TextView> textViews = new ArrayList<TextView>();
    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                	
//                	 connectCharacteristic(setupAllCharacteristics(mBluetoothLeService1.getSupportedGattServices()),mBluetoothLeService1);
//                	 connectCharacteristic(setupAllCharacteristics(mBluetoothLeService.getSupportedGattServices()),mBluetoothLeService);
                     return false;
                }
    };

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        
//        View v = findViewById(R.layout.gatt_services_characteristics);
//        v.setBackgroundColor(Color.BLACK);
        
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setVisibility(View.GONE);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        
        mDataField = (TextView) findViewById(R.id.data_value);
        mDataField1 = (TextView) findViewById(R.id.data_value1);
        mDataField1.setVisibility(View.GONE);
        mDataField2 = (TextView) findViewById(R.id.data_value2);
        mDataField2.setVisibility(View.GONE);
        
        textViews.add(mDataField);
        textViews.add(mDataField1);
        textViews.add(mDataField2);
        
        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        BLEThread ble = new BLEThread();
        threads.add(ble);
        ble.run();
		
		t = new Thread(){
		    public void run(){
		      Log.d("data","Thread Running!");
		    }
		  };
		  t.start();
//		  
//	      BLEThread ble = new BLEThread();
//	      threads.add(ble);
//        Future f1 = executor.submit(ble);
//	      ble.run();
		  
//        f.add(f1);
        //executor.invokeAny(1);
        
        final Button btn = (Button)findViewById(R.id.thread2);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
 
        		  for(int i = 0; i < textViews.size(); i++){
        			  if(i == Integer.parseInt((String)(((Button) v).getText()))){
        				  textViews.get(i).setVisibility(View.VISIBLE);
        			  }else{
        				  textViews.get(i).setVisibility(View.GONE);
        			  }
        		  }
            	  
            }
        });
        
        Button btn1 = (Button)findViewById(R.id.thread1);
        btn1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
//            	System.out.print(threads.size());
//	            if(threads.size()>=2){
//	            	threads.get(1).revoke();
//	            }
            	 for(int i = 0; i < textViews.size(); i++){
       			  if(i == Integer.parseInt((String)(((Button) v).getText()))){
       				  textViews.get(i).setVisibility(View.VISIBLE);
       			  }else{
       				  textViews.get(i).setVisibility(View.GONE);
       			  }
       		  }
            }
        });
    
        Button btn2 = (Button)findViewById(R.id.thread3);
        btn2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
//            	System.out.print(threads.size());
//	            if(threads.size()>=2){
//	            	threads.get(1).revoke();
//	            }
            	 for(int i = 0; i < textViews.size(); i++){
       			  if(i == Integer.parseInt((String)(((Button) v).getText()))){
       				  textViews.get(i).setVisibility(View.VISIBLE);
       			  }else{
       				  textViews.get(i).setVisibility(View.GONE);
       			  }
       		  }
            }
        });
        
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unbindService(mServiceConnection);
       // mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                //mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                //mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data, int index) {
        if (data != null) {
            textViews.get(index).setText(data);
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

//    private static IntentFilter makeGattUpdateIntentFilter() {
//        final IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
//        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
//        return intentFilter;
//    }
    
    /*
     * new functions
     */
    public boolean connectCharacteristic(ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics, BluetoothLeService mBluetoothLeService){
    	
        if (mGattCharacteristics != null) {
            final BluetoothGattCharacteristic characteristic =
                    mGattCharacteristics.get(2).get(1);
            //final BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.fromString("00008aa2-0000-1000-8000-00805f9b34fb"), 16, 0);
            final int charaProp = characteristic.getProperties();
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                // If there is an active notification on a characteristic, clear
                // it first so it doesn't update the data field on the user interface.
                if (mNotifyCharacteristic != null) {
                    mBluetoothLeService.setCharacteristicNotification(
                            mNotifyCharacteristic, false);
                    mNotifyCharacteristic = null;
                }
                mBluetoothLeService.readCharacteristic(characteristic);
            }
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                mNotifyCharacteristic = characteristic;
                mBluetoothLeService.setCharacteristicNotification(
                        characteristic, true);
            }
            return true;
        }
        return false;
    }
    
    //new thread that takes care of everything.
    public class BLEThread implements Runnable{
    	public int val = -1;
    	public String aName;
    	public Boolean con = false;
    	private BluetoothLeService mBluetoothLeService;
    	private final ServiceConnection mServiceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                mBluetoothLeService=((BluetoothLeService.LocalBinder) service).getService();
                if (!mBluetoothLeService.initialize()) {
                    Log.d(TAG, "Unable to initialize Bluetooth");
                    finish();
                }
                // Automatically connects to the device upon successful start-up initialization.
                registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter()); 
                Log.d("data", "connect address = " + bd[counter]);
                mBluetoothLeService.connect(bd[counter]); 
                try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                //mBluetoothLeService = null;
            }
        };

        public void sleep(){
        	synchronized(this){
        		try {
    				this.wait(10000);
    			} catch (InterruptedException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}	
        	};
        }
        
        public void revoke(){
        	synchronized(this){
        		this.notify();
        	}
        }
    	
		@Override
		public void run() {
			// TODO Auto-generated method stub
			this.aName = bd[counter];
		    final Intent gattServiceIntent = new Intent(getApplicationContext(), BluetoothLeService.class);
		    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);	       
		}
		
		   public final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		        @Override
		        public void onReceive(Context context, Intent intent) {
		            final String action = intent.getAction();
		            Log.d("data","action = " + action);
		            Log.d("data","------------------------------------------------");
		            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
		                mConnected = true;
		                //Log.d("connect", "connect " + intent.getStringExtra("device name") +"---------------------");
		                if(counter<len-1){
		                	counter++;
		                	Log.d("counter", "counter = " + Integer.toString(counter));
		                	BLEThread ble1 = new BLEThread();
		                	threads.add(ble1);
		                    ble1.run();		                 
		                }
		                updateConnectionState(R.string.connected);
		                invalidateOptionsMenu();
		            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
		                mConnected = false;
		                //Log.d("connect", "disconnect " + intent.getStringExtra("device name") +"---------------------");
		                updateConnectionState(R.string.disconnected);
		                invalidateOptionsMenu();
		                clearUI();
		            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
		                // Show all the supported services and characteristics on the user interface.
		           	    connectCharacteristic(setupAllCharacteristics(mBluetoothLeService.getSupportedGattServices()),mBluetoothLeService);
		            } else if (bd[0].equals(action)) {
		                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA), 0);
			            Log.d("data","name = "+  bd[0] + "     " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
			           Intent t = new Intent("scale");
			           t.putExtra("data", intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
			           sendBroadcast(t);
		            }
		            else if (bd[1].equals(action)) {
		                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA), 1);
			            Log.d("data","name = "+  bd[1] + "     " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
			               
		            }
		            else if (bd[2].equals(action)) {
		                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA), 2);
			            Log.d("data","name = "+  bd[2] + "     " + intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
			               
		            }
		            
		        }
		    };
		    
		    public ArrayList<ArrayList<BluetoothGattCharacteristic>> setupAllCharacteristics(List<BluetoothGattService> gattServices){
		    	
		    	ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
		    	if (gattServices == null) return mGattCharacteristics;
		        String uuid = null;
		        String unknownServiceString = getResources().getString(R.string.unknown_service);
		        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
		        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
		        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
		                = new ArrayList<ArrayList<HashMap<String, String>>>();
		    
		        // Loops through available GATT Services.
		        for (BluetoothGattService gattService : gattServices) {
		            HashMap<String, String> currentServiceData = new HashMap<String, String>();
		            uuid = gattService.getUuid().toString();
		            currentServiceData.put(
		                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
		            currentServiceData.put(LIST_UUID, uuid);
		            gattServiceData.add(currentServiceData);

		            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
		                    new ArrayList<HashMap<String, String>>();
		            List<BluetoothGattCharacteristic> gattCharacteristics =
		                    gattService.getCharacteristics();
		            ArrayList<BluetoothGattCharacteristic> charas =
		                    new ArrayList<BluetoothGattCharacteristic>();

		            // Loops through available Characteristics.
		            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
		                charas.add(gattCharacteristic);
		                HashMap<String, String> currentCharaData = new HashMap<String, String>();
		                uuid = gattCharacteristic.getUuid().toString();
		                currentCharaData.put(
		                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
		                currentCharaData.put(LIST_UUID, uuid);
		                gattCharacteristicGroupData.add(currentCharaData);
		            }
		            mGattCharacteristics.add(charas);
		            gattCharacteristicData.add(gattCharacteristicGroupData);
		        }
				return mGattCharacteristics;
		    }
		    
		    private  IntentFilter makeGattUpdateIntentFilter() {
		        final IntentFilter intentFilter = new IntentFilter();
		        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		        intentFilter.addAction(bd[0]);
		        intentFilter.addAction(bd[1]);
		        intentFilter.addAction(bd[2]);
		        return intentFilter;
		    }
    }
    
}
