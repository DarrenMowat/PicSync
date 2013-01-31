package com.darrenmowat.gdcu.service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.json.JSONException;

import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.darrenmowat.gdcu.GDCU;
import com.darrenmowat.gdcu.R;
import com.darrenmowat.gdcu.data.Database;
import com.darrenmowat.gdcu.data.Preferences;
import com.darrenmowat.gdcu.drive.DriveApi;
import com.darrenmowat.gdcu.utils.MD5Utils;
import com.flurry.android.FlurryAgent;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.services.drive.model.File;

public class UploadThread extends Thread {

	private DriveApi drive;
	private Database database;

	private String email;

	private UploadService service;

	public boolean uploading = false;
	
	private boolean shouldRequery = false;
	private boolean shouldStop = false;

	public UploadThread(UploadService service, String email) {
		this.service = service;
		this.email = email;
	}

	@Override
	public void run() {
		try {
			uploading = true;
			// Connect to the database
			database = new Database(service);
			database.connect();
			try {
				email = Preferences.getEmail(service);
				if (email == null) {
					log("No user signed in. Stopping Service.");
					return;
				}
				String userToken = GoogleAuthUtil.getToken(service, email, DriveApi.DRIVE_SCOPE);
				String uploadsDirName = Preferences.getUploadsFolderName(service);
				drive = new DriveApi(userToken, uploadsDirName);
				uploadPendingFiles();
			} catch (NoSuchAlgorithmException e) {
				threadError(e);
			} catch (UserRecoverableAuthException e) {
				threadError(e);
			} catch (IOException e) {
				threadError(e);
			} catch (GoogleAuthException e) {
				threadError(e);
			} catch (JSONException e) {
				threadError(e);
			} 
		} finally {
			uploading = false;
			// Disconnect from the database
			database.disconnect();
			log("Stopping file uploader.");
		}
	}

	private void uploadPendingFiles() throws IOException, NoSuchAlgorithmException, JSONException {
		Cursor files = database.getPendingUploads(null);
		int count = files.getCount();
		int current = 0;
		try {
			while (files.moveToNext() && ServiceUtils.shouldUpload(service) && !shouldStop) {
				current++;

				String title = service.getResources().getString(
						R.string.pref_title_upload_status_uploading);
				String msg = String.format(
						service.getResources().getString(
								R.string.pref_summary_upload_status_uploading_x_of_y), current,
						count);

				ServiceUtils.sendServiceUpdate(title, msg);

				int id = files.getInt(files.getColumnIndex(Database.PENDING_UPLOADS_ID));
				String path = files.getString(files.getColumnIndex(Database.PENDING_UPLOADS_PATH));
				String md5 = files.getString(files.getColumnIndex(Database.PENDING_UPLOADS_MD5));
				String mime = files.getString(files.getColumnIndex(Database.PENDING_UPLOADS_MIME));
				java.io.File file = new java.io.File(path);
				if (!file.exists()) {
					// File has been deleted
					log("File has been deleted " + path);
					database.removeFromPendingById(id);
				} else {
					String _md5 = MD5Utils.getFileMd5(file);
					if (!md5.equals(_md5)) {
						md5 = _md5;
					}
					log("Started upload of " + path);
					boolean onWifi = ServiceUtils.isOnWiFi(service);
					File uploaded = drive.uploadFile(file.getName(), mime, file, onWifi);
					database.addToUploaded(uploaded.getTitle(), uploaded.getMd5Checksum(),
							uploaded.getId(), uploaded.getCreatedDate().getValue(),
							uploaded.getMimeType());
					database.removeFromPendingById(id);

					if (!GDCU.DEVEL_BUILD) {
						FlurryAgent.logEvent("File Uploaded");
					}

					log("Finished upload of " + path);
					
					if(shouldRequery) {
						
					}
				}
			}
		} finally {
			files.close();
			if (count != current) {
				String title = service.getResources().getString(
						R.string.pref_title_upload_status_uploads_pending);
				String msg = service.getResources().getString(
						R.string.pref_summary_upload_status_uploading_pending_for_network);
				ServiceUtils.sendServiceUpdate(title, msg);
			} else {
				String title = service.getResources().getString(
						R.string.pref_title_upload_status_uploads_finished);
				String msg = "";
				ServiceUtils.sendServiceUpdate(title, msg);
			}
			threadFinished();
		}
	}

	private void threadFinished() {
		service.onThreadFinished();
	}

	private void threadError(Exception e) {
		service.onThreadError(e);
	}

	public synchronized void touch() {
		shouldRequery = true;
	}
	
	public void stopSoon() {
		shouldStop = true;
	}

	public void handleAuthError(String email, Intent intent) {
		Notifier.notifyAuthError(service, email, intent);
	}

	private static void log(String msg) {
		Log.v("GDCU::UploadThread", msg);
	}
	
}