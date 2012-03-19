package com.geogremlin.ucsbgeotrackerGPS;
/*
 * Project: UCSBGeoTrackerGPS
 * Author: Grant McKenzie
 * Date: October 2011
 * Client: GeoTrans Lab @ UCSB
 * 
 */

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


import com.geogremlin.ucsbgeotrackerGPS.WebService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class UCSBGeoTrackerGPSActivity extends Activity implements OnClickListener {

  Button buttonLogin;
  EditText username;
  EditText password;
  TextView textusername;
  TextView textpassword;
  private TelephonyManager tm;
  private String deviceId;
  private String handler = "http://geogremlin.geog.ucsb.edu/android/tracker-gps/login.php";
  private SharedPreferences settings;
  private int at_login;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main); 
    
    
 // Display a notification about us starting.  We put an icon in the status bar.
    
    
    settings = getPreferences(MODE_PRIVATE);
    
    tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
	// Get unique device ID
	String tmDevice, tmSerial, androidId;
    tmDevice = "" + tm.getDeviceId();
    tmSerial = "" + tm.getSimSerialNumber();
    androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
    UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
    deviceId = deviceUuid.toString();
    
    buttonLogin = (Button) findViewById(R.id.Login);
    buttonLogin.setOnClickListener(this);
    
    textusername = (TextView) findViewById(R.id.textUsername);
    textpassword = (TextView) findViewById(R.id.txtPassword);
    
    
    username = (EditText) findViewById(R.id.Username);
    password = (EditText) findViewById(R.id.Password);
    
    username.setText(settings.getString("Name", null));
    password.setText(settings.getString("Password", null));
    

    at_login = settings.getInt("AT_LOGINSET", 0);
    
    if (at_login == 1) {
    	buttonLogin.setText("Logout");
	 	// Hide username/password fields and keyboard
		username.setVisibility(View.INVISIBLE);
		password.setVisibility(View.INVISIBLE);
		textusername.setVisibility(View.INVISIBLE);
		textpassword.setVisibility(View.INVISIBLE);
    } else {
    	buttonLogin.setText("Login");
		username.setVisibility(View.VISIBLE);
		password.setVisibility(View.VISIBLE);
		textusername.setVisibility(View.VISIBLE);
		textpassword.setVisibility(View.VISIBLE);
    }
    
  }
  public void onSaveInstanceState(Bundle savedInstanceState) {
	  
	  super.onSaveInstanceState(savedInstanceState);
	  saveState();
  }
  
  @Override
  protected void onPause() {
      super.onPause();
      saveState();
  }
  
  @Override
  public void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    settings = PreferenceManager.getDefaultSharedPreferences(this);
    String something = settings.getString("AT_something", "0");
    username.setText(something);
    
  }
  private void saveState() {
	  SharedPreferences preferences = getPreferences(MODE_PRIVATE);
	  SharedPreferences.Editor editor = preferences.edit();
	  
	  EditText txtName = (EditText)findViewById(R.id.Username);
	  String strName = txtName.getText().toString();
	  
	  EditText txtPass = (EditText)findViewById(R.id.Password);
	  String strPass = txtPass.getText().toString();
	  
	  editor.putString("Name", strName); // value to store
	  editor.putString("Password", strPass); // value to store  
	  
	  editor.putInt("AT_LOGINSET", at_login);

	  editor.commit();
  }
  

 
  public void onClick(View src) {
	  if (src.getId() == R.id.Login) {
		  	buttonLogin.setEnabled(false);
		  	username.setEnabled(false);
			password.setEnabled(false);
			String d[] = { handler, ""+username.getText().toString(), ""+password.getText().toString() };
			new DownloadDataTask().execute(d);
			buttonLogin.setEnabled(true);
		    username.setEnabled(true);
		    password.setEnabled(true);
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
	
	public void errorDialog(String msg) {
		AlertDialog.Builder adb=new AlertDialog.Builder(UCSBGeoTrackerGPSActivity.this);
        adb.setTitle("UCSB GeoTracker");
        adb.setMessage(msg);
        adb.setNegativeButton("OK", new DialogInterface.OnClickListener() {  
  	      public void onClick(DialogInterface dialog, int which) {  
  	    	finish();
  	        return;  
  	   } });
        adb.show(); 
	}
	
  private class DownloadDataTask extends AsyncTask<String, Void, String> {
		private final ProgressDialog dialog = new ProgressDialog(UCSBGeoTrackerGPSActivity.this);
	     
	     protected void onPreExecute() {
	    	 this.dialog.setTitle("UCSB ");
	    	 this.dialog.setMessage("Logging in...");
	         this.dialog.show();
	     }

	     protected void onPostExecute(String response) {
	    	 if (response != "error") {
		    	 if(isNetworkAvailable()) {
				       try {
							SharedPreferences.Editor editor = settings.edit();
							if (at_login == 0) {
						    	int resultint = Integer.parseInt(response.replace("\n","").trim());
						    	if (resultint == 1) {

									startService(new Intent(getApplicationContext(), ATLocation.class));
									startService(new Intent(getApplicationContext(), ATAccel.class));

									buttonLogin.setText("Logout");
									editor.putInt("AT_LOGINSET", 1);
									at_login = 1;
									editor.commit();
									
									// Hide username/password fields and keyboard
									username.setVisibility(View.INVISIBLE);
									password.setVisibility(View.INVISIBLE);
									textusername.setVisibility(View.INVISIBLE);
									textpassword.setVisibility(View.INVISIBLE);
									InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
									imm.hideSoftInputFromWindow(password.getWindowToken(), 0);
									
						    	} else {
						    		Toast.makeText(getApplicationContext(), "There was an error logging you in.  Please try again.", Toast.LENGTH_SHORT).show();
						    	}
							} else {
					    	    Toast.makeText(getApplicationContext(), "Service Stopped", Toast.LENGTH_SHORT).show();
								editor.putInt("AT_LOGINSET", 0);
								at_login = 0;
								editor.commit();
								stopService(new Intent(getApplicationContext(), ATLocation.class));
								stopService(new Intent(getApplicationContext(), ATAccel.class));
								
								buttonLogin.setText("Login");
								username.setVisibility(View.VISIBLE);
								password.setVisibility(View.VISIBLE);
								textusername.setVisibility(View.VISIBLE);
								textpassword.setVisibility(View.VISIBLE);
								username.setText("");
								password.setText("");
							}
					   		
				       } catch(Exception e) {
				    	   // Log.v("tag", "test: "+e);
				    	   this.dialog.cancel();
						   errorDialog("Sorry, there was an error parsing the border data.  Please try again.");
					   }
				       this.dialog.cancel();
		    	 } else {
		    		 this.dialog.cancel();
					 errorDialog("Sorry, there was an error connecting to the database.  Please check your network connection and try again.");
		    	 }
	    	 } else {
	    		 this.dialog.cancel();
				 errorDialog("Sorry, there was an error connecting to the database.  Please check your network connection and try again.");
	    	 }
	     }

		@Override
		protected String doInBackground(String... url) {
			// TODO Auto-generated method stub
	
			WebService webService = new WebService(url[0]);
			   String username = url[1];
			   String password = url[2];
				   
			   //Pass lat/lng params
			   Map<String, String> params = new HashMap<String, String>();
			   params.put("devid", deviceId);
			   params.put("u", username);
			   params.put("p", password);
			   
		   try {
			   String response = webService.webGet("", params);
			   return response;
		   } catch(Exception e) {
	    	   return "error";
		   }
		   
		}
	 }	
}