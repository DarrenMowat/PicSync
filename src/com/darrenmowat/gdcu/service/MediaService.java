package com.darrenmowat.gdcu.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.darrenmowat.gdcu.R;
import com.darrenmowat.gdcu.data.Preferences;
import com.darrenmowat.gdcu.otto.DataBus;
import com.darrenmowat.gdcu.otto.UploadStatusEvent;
import com.darrenmowat.gdcu.service.helpers.ThreadCallbacks;
import com.google.android.gms.auth.UserRecoverableAuthException;

public class MediaService extends Service implements ThreadCallbacks {

	public static final String NO_WAIT = "com.darrenmowat.gdcu.service.MediaService.NO_WAIT";

	private WakeLock wakelock;
	private MediaThread thread;

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
		if (thread != null && thread.running) {
			thread.interrupt();
			// The thread will notify this once it has actually stopped running
			return;
		}
		// Ensure the thread has been stopped
		// Ensure the WakeLock has been given up
		if (wakelock != null && wakelock.isHeld()) {
			wakelock.release();
		}
		super.onDestroy();

	}

	private void handleCommand(Intent intent) {
		if(!ServiceUtils.shouldUpload(this)) {
			log("Not syncing just now!");
			return;
		}
		// If we are currently uploading return;
		if(UploadService.uploading) {
			log("Not scanning for media just now, currently uploading!");
			return;
		}
		boolean should_wait = true;
		if (intent != null) {
			if (intent.getAction() != null) {
				should_wait = !intent.getAction().equals(NO_WAIT);
			}
		}
		email = Preferences.getEmail(MediaService.this);
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
			thread = new MediaThread(MediaService.this, email, should_wait);
		}

		synchronized (this) {
			if (!thread.running) {
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
		Log.v("GDCU::MediaService", msg);
	}

	@Override
	public void onThreadFinished() {
		thread = null;
		if (wakelock != null && wakelock.isHeld()) {
			wakelock.release();
		}
		stopSelf();
		// Start the upload service
		final Context context = MediaService.this;
		Intent service = new Intent(context, UploadService.class);
		context.startService(service);
	}

	@Override
	public void onThreadError(Exception e) {
		thread = null;
		if (e instanceof UserRecoverableAuthException) {
			UserRecoverableAuthException userException = (UserRecoverableAuthException) e;
			Notifier.notifyAuthError(MediaService.this, email, userException.getIntent());
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
