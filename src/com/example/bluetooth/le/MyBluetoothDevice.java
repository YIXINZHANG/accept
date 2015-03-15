package com.example.bluetooth.le;

import java.util.UUID;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class MyBluetoothDevice implements Parcelable{
	private String deviceName;
	private String macAddress;
	private UUID serviceUUID = UUID.fromString("0000780a-0000-1000-8000-00805f9b34fb");
	private UUID charUUID = UUID.fromString("00008aa2-0000-1000-8000-00805f9b34fb");
	
	public UUID getuUid() {
		return serviceUUID;
	}
	public void setuUid(UUID uUid) {
		this.serviceUUID = uUid;
	}
	private String getDeviceName() {
		return deviceName;
	}
	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}
	public String getMacAddress() {
		return macAddress;
	}
	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeString(this.getMacAddress());
		dest.writeString(this.getDeviceName());
	}
	public static final Parcelable.Creator<MyBluetoothDevice> CREATOR = new Parcelable.Creator<MyBluetoothDevice>() {
	        public MyBluetoothDevice createFromParcel(Parcel source) {
	            return new MyBluetoothDevice(source);
	        }
	 
	        public MyBluetoothDevice[] newArray(int size) {
	            return new MyBluetoothDevice[size];
	        }
	};
	
	public MyBluetoothDevice(Parcel source) {
        this.setMacAddress(source.readString());
        this.setDeviceName(source.readString()); 
    }
    
    public MyBluetoothDevice() {

    }
}
