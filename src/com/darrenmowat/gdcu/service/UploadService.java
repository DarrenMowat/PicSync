package com.darrenmowat.gdcu.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.darrenmowat.gdcu.R;
import com.darrenmowat.gdcu.data.Preferences;
import com.darrenmowat.gdcu.otto.DataBus;
import com.darrenmowat.gdcu.otto.UploadStatusEvent;
import com.darrenmowat.gdcu.service.helpers.ThreadCallbacks;
import com.google.android.gms.auth.UserRecoverableAuthException;

public class UploadService extends Service implements ThreadCallbacks {

	public static boolean uploading = false;
	
	private WakeLock wakelock;
	private UploadThread thread;

	private String email;

	@Override
	public void onStart(Intent intent, int startId) {
		handleCommand(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleCommand(intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		if (thread != null && thread.uploading) {
			thread.stopSoon();
			// The thread will notify this once it has actually stopped running
			return;
		}
		// Ensure the thread has been stopped
		// Ensure the WakeLock has been given up
		if (wakelock != null && wakelock.isHeld()) {
			wakelock.release();
		}
		uploading = false;
		super.onDestroy();

	}

	private void handleCommand(Intent intent) {
		uploading = true;
		email = Preferences.getEmail(UploadService.this);
		if (email == null || email.trim().equals("")) {
			log("No user stored. Stopping service.");
			stopSelf();
			return;
		}

		if (wakelock == null) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MediaServiceWakeLock");
		}

		if (!wakelock.isHeld()) {
			wakelock.acquire();
		}

		if (thread == null) {
			thread = new UploadThread(UploadService.this, email);
		}

		synchronized (this) {
			if (!thread.uploading) {
				thread.start();
			} else {
				thread.touch();
			}
		}

	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void log(String msg) {
		Log.v("GDCU::UploadService", msg);
	}

	@Override
	public void onThreadFinished() {
		thread = null;
		if (wakelock != null && wakelock.isHeld()) {
			wakelock.release();
		}
		stopSelf();
	}

	@Override
	public void onThreadError(Exception e) {
		log("UploadService crashed");
		e.printStackTrace();
		thread = null;
		if (e instanceof UserRecoverableAuthException) {
			UserRecoverableAuthException userException = (UserRecoverableAuthException) e;
			Notifier.notifyAuthError(UploadService.this, email, userException.getIntent());
		} else {
			e.printStackTrace();
			String title = getResources().getString(
					R.string.pref_title_upload_status_uploads_pending);
			String msg = getResources().getString(
					R.string.pref_summary_upload_status_uploads_failed);
			DataBus.getInstance().post(new UploadStatusEvent(title, msg));
		}

		if (wakelock != null && wakelock.isHeld()) {
			wakelock.release();
		}
		stopSelf();
	}


}
