package com.geogremlin.ucsbgeotrackerGPS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class AtGetAccel extends Service implements SensorEventListener {
	
	private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private double accelx = 0;
	private double accely = 0;
	private double accelz = 0;
	private SharedPreferences settings;
    private SharedPreferences.Editor editor;
    private String deviceId;
    private TelephonyManager tm;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void onCreate() {
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		editor = settings.edit();
		
		tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String tmDevice, tmSerial, androidId;
	    tmDevice = "" + tm.getDeviceId();
	    tmSerial = "" + tm.getSimSerialNumber();
	    androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
	    UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
	    deviceId = deviceUuid.toString();
	}
	@Override
	public void onStart(Intent intent, int startid) {
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
	    mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	}
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onSensorChanged(SensorEvent event) {
		accelx = event.values[0];
		accely = event.values[1];
		accelz = event.values[2];
		String accel = settings.getString("AT_ACCEL", "");
		Integer accelcount = settings.getInt("AT_ACCELCOUNT", 0);
		accelcount++;
		Long tsLong = System.currentTimeMillis()/1000;
		String ts = tsLong.toString();
		String all = "|"+ts+","+accelx+","+accely+","+accelz;
		editor.putString("AT_ACCEL", accel+all);
		editor.putInt("AT_ACCELCOUNT", accelcount);
		editor.commit();
		mSensorManager.unregisterListener(this);
		if (accelcount > 9) {
			Toast.makeText(this, "Sending Accelerometer data to server", Toast.LENGTH_SHORT).show();
			storeData(accel);
		}
			
	}
	
	private void storeData(String accel) {
		 if (isNetworkAvailable()) {
			 try {
				 String response = sendAccel(deviceId, accel);
				 
				 int resultint = Integer.parseInt(response.replace("\n","").trim());
				 if (resultint == 1) {
					 // Toast.makeText( getApplicationContext(),"Accelerometer data successfully stored in the database.",Toast.LENGTH_SHORT).show();
					 editor.putString("AT_ACCEL", "");
					 editor.putInt("AT_ACCELCOUNT", 0);
					 editor.commit();
				 } else {
					 Toast.makeText( getApplicationContext(),"There was an error pushing your Accelerometer data to the database.",Toast.LENGTH_SHORT).show();
				 }
			 } catch(Exception e) {
				 	Toast.makeText(this, "Sorry there was an error sending the data to the database", Toast.LENGTH_SHORT).show();
			 }
		 } else {
			 Toast.makeText( getApplicationContext(),"No Data Connection.\nData not sent to server.",Toast.LENGTH_SHORT).show();
		 }
		 
	}
	
	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    if (activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting()) {
	        return true;
	    }
	    return false;
	}
	
	private String sendAccel(String uid, String accel) {
		
		   String handler = "http://geogremlin.geog.ucsb.edu/android/tracker-gps/store_accel.php";
		   WebService webService = new WebService(handler);
			   
		   //Pass lat/lng params
		   Map<String, String> params = new HashMap<String, String>();
		   params.put("devid", uid);
		   params.put("accel", accel);
		   
	   try {
		   String response = webService.webGet("", params);
		   return response;
	   } catch(Exception e) {
 	   return "error";
	   }
	}
}
