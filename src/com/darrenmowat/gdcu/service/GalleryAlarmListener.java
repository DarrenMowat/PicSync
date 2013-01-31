package com.darrenmowat.gdcu.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService.AlarmListener;

public class GalleryAlarmListener implements AlarmListener {

	@Override
	public void scheduleAlarms(AlarmManager mgr, PendingIntent pendingIntent, Context context) {
		info("Starting alarm with update interval of " + AlarmManager.INTERVAL_HOUR);
		mgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				SystemClock.elapsedRealtime() + 60000, AlarmManager.INTERVAL_HOUR, pendingIntent);
	}

	@Override
	public void sendWakefulWork(Context context) {
		info("Waking up the media service");
		Intent service = new Intent(context, MediaService.class);
		service.setAction(MediaService.NO_WAIT);
		context.startService(service);
	}

	/*
	 * If the service hasn't been triggered in 12 hours assume it has been
	 * killed (non-Javadoc)
	 * 
	 * @see
	 * com.commonsware.cwac.wakeful.WakefulIntentService.AlarmListener#getMaxAge
	 * ()
	 */
	@Override
	public long getMaxAge() {
		return Math.round(AlarmManager.INTERVAL_HALF_DAY);
	}

	private void info(String msg) {
		Log.v("GDCU::GalleryAlarmListener", msg);
	}
}
