/*
 * Project: UCSBActivityTrackr
 * Author: Grant McKenzie
 * Date: May 2011
 * Client: GeoTrans Lab @ UCSB
 * 
 */

package com.geogremlin.ucsbgeotrackerGPS;

import java.io.IOException;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class ATLocation extends Service implements SensorEventListener{
	private NotificationManager mNM;
	private int NOTIFICATION = R.string.local_service_started;
	
	private LocationManager locationManager;
	private LocationListener locationListener;
	private String best;
	private Location currentLocation;
	private double accelx = 0;
	private double accely = 0;
	private double accelz = 0;
	
	Criteria crit = null;
	
	private TelephonyManager tm;
	private ConnectivityManager connectivity;
    private String deviceId;
    
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		
		showNotification();
		
		tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		connectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		// Get unique device ID
		String tmDevice, tmSerial, androidId;
	    tmDevice = "" + tm.getDeviceId();
	    tmSerial = "" + tm.getSimSerialNumber();
	    androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
	    UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
	    deviceId = deviceUuid.toString();
		
	    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	    mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    
	    
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		crit = new Criteria();
		// crit.setAccuracy(Criteria.ACCURACY_FINE);
		best = locationManager.getBestProvider(crit, true);
		// best = LocationManager.GPS_PROVIDER;
		currentLocation = locationManager.getLastKnownLocation(best);
		// String Text = "Last Latitude = " + currentLocation.getLatitude() + "\nLast Longitude = " + currentLocation.getLongitude();
		// Toast.makeText(this, Text, Toast.LENGTH_SHORT).show();
		
		// Timestamp ts = new Timestamp(Calendar.getInstance().getTime().getTime());
		// storeData(""+currentLocation.getLatitude(), ""+currentLocation.getLongitude(), ""+ts);
		
	    locationListener = new MyLocationListener();
	}
	
	@Override
	public void onDestroy() {
		// Toast.makeText(this, "GPS Tracker Stopped", Toast.LENGTH_SHORT).show();
		mNM.cancel(NOTIFICATION);
		locationManager.removeUpdates(locationListener);
		mSensorManager.unregisterListener(this);
	}
	
	@Override
	public void onStart(Intent intent, int startid) {
		// Toast.makeText(this, "GPS Tracker Started", Toast.LENGTH_SHORT).show();
		locationManager.requestLocationUpdates(best,0, 0, locationListener);
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	}
	private void showNotification() {
	      // In this sample, we'll use the same text for the ticker and the expanded notification
	      CharSequence text = getText(R.string.local_service_started);
	
	      // Set the icon, scrolling text and timestamp
	      Notification notification = new Notification(R.drawable.iconnotification, text, System.currentTimeMillis());
	
	      Intent notifyIntent = new Intent(Intent.ACTION_MAIN);
	      notifyIntent.setClass(getApplicationContext(), UCSBGeoTrackerGPSActivity.class);
	      notifyIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
	      
	      // The PendingIntent to launch our activity if the user selects this notification
	      PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notifyIntent, PendingIntent.FLAG_CANCEL_CURRENT);
	
	      // Set the info for the views that show in the notification panel.
	      notification.setLatestEventInfo(this, getText(R.string.local_service_label), text, contentIntent);
	
	      notification.flags|=Notification.FLAG_NO_CLEAR;
	      // Send the notification.
	      // mNM.notify(NOTIFICATION, notification);
	      startForeground(1337, notification);
	}
	
	public class MyLocationListener implements LocationListener {
		
		@Override
		public void onLocationChanged(Location loc) {
			// Toast.makeText(getApplicationContext(), "Location Changed\nAttempting to send fix to DB", Toast.LENGTH_SHORT).show();
			
			Timestamp ts = new Timestamp(Calendar.getInstance().getTime().getTime());	
			storeData(""+loc.getLatitude(), ""+loc.getLongitude(), ""+ts);
			
		}

		@Override
		public void onProviderDisabled(String provider) {
			// Toast.makeText( getApplicationContext(),"Gps Disabled",Toast.LENGTH_SHORT ).show();
		}

		@Override
		public void onProviderEnabled(String provider) {
			// Toast.makeText( getApplicationContext(),"Gps Enabled",Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// Toast.makeText( getApplicationContext(),"Location Service changed to "+provider,Toast.LENGTH_SHORT).show();
		}

	}

	
	private void storeData(String lat, String lon, String timest) {
		// Toast.makeText( getApplicationContext(),"Entered storeData function",Toast.LENGTH_SHORT ).show();
		 if (isNetworkAvailable(getApplicationContext())) {
			 String response = sendLocation(deviceId, lat, lon, timest);
			 // String lines[] = response.split("\\r?\\n");
			 if (response != "0") {
				 // Toast.makeText( getApplicationContext(),"GPS fix successfully stored in the database.",Toast.LENGTH_SHORT).show();
			 } else {
				 Toast.makeText( getApplicationContext(),"There was an error pushing your GPS fix to the database.",Toast.LENGTH_SHORT).show();
			 }
		 } else {
			 Toast.makeText( getApplicationContext(),"No Data Connection.\nData not sent to server.",Toast.LENGTH_SHORT).show();
		 }
		 
	}
	

	
	public boolean isNetworkAvailable(Context context) {
		try {
		    if (connectivity != null) {
		       NetworkInfo[] info = connectivity.getAllNetworkInfo();
		       if (info != null) {
		          for (int i = 0; i < info.length; i++) {
		             if (info[i].getState() == NetworkInfo.State.CONNECTED) {
		                return true;
		             }
		          }
		       }
		    } 
		    return false;
		} catch (Exception e) {
			return false;
		}
	 }
	
	private String sendLocation(String uid, String lat, String lon, String timest) {
		String handler = "http://geogremlin.geog.ucsb.edu/android/tracker-gps/store_fix.php";

		HttpClient httpclient = new DefaultHttpClient();  
	    HttpPost httppost = new HttpPost(handler);  
	    HttpResponse response = null;
	    try {  
	        // Add your data  
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();  
	        nameValuePairs.add(new BasicNameValuePair("devid", uid));  
	        nameValuePairs.add(new BasicNameValuePair("lat", lat));
	        nameValuePairs.add(new BasicNameValuePair("lng", lon));
	        nameValuePairs.add(new BasicNameValuePair("t", timest));
	        nameValuePairs.add(new BasicNameValuePair("source", best));
	        nameValuePairs.add(new BasicNameValuePair("app", "Test"));
	        nameValuePairs.add(new BasicNameValuePair("accelx", accelx+""));
	        nameValuePairs.add(new BasicNameValuePair("accely", accely+""));
	        nameValuePairs.add(new BasicNameValuePair("accelz", accelz+""));
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));  
	  
	        // Execute HTTP Post Request  
	        response = httpclient.execute(httppost);  
	          
	    } catch (ClientProtocolException e) {  
	        // TODO Auto-generated catch block  
	    } catch (IOException e) {  
	        // TODO Auto-generated catch block  
	    }  
	    
	    return HTTPHelper.request(response);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		accelx = event.values[0];
		accely = event.values[1];
		accelz = event.values[2];
		// Toast.makeText( getApplicationContext(),event.values[0]+","+event.values[1]+","+event.values[2],Toast.LENGTH_SHORT).show();
	}
	
}

