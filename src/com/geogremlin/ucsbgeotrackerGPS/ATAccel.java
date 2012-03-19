/*
 * Project: UCSBActivityTrackr
 * Author: Grant McKenzie
 * Date: May 2011
 * Client: GeoTrans Lab @ UCSB
 * 
 */

package com.geogremlin.ucsbgeotrackerGPS;

import java.util.Calendar;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ATAccel extends Service {

    private PendingIntent pendingIntent;
    private AlarmManager alarmManager;
    
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		Intent myIntent = new Intent(ATAccel.this, ATAlarmService.class);
		pendingIntent = PendingIntent.getBroadcast(ATAccel.this, 123321, myIntent, 0);
		alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		calendar.add(Calendar.SECOND, 5);
		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 5*1000, pendingIntent);
	}
	
	@Override
	public void onDestroy() {
		alarmManager.cancel(pendingIntent);
	}
	
}

