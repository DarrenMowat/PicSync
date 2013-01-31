package com.darrenmowat.gdcu.service;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.json.JSONException;

import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;

import com.darrenmowat.gdcu.GDCU;
import com.darrenmowat.gdcu.R;
import com.darrenmowat.gdcu.data.Database;
import com.darrenmowat.gdcu.drive.DriveApi;
import com.darrenmowat.gdcu.service.helpers.CloudSyncer;
import com.darrenmowat.gdcu.service.helpers.Media;
import com.darrenmowat.gdcu.utils.MD5Utils;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

public class MediaThread extends Thread {

	private Database database;
	private CloudSyncer syncer;

	private MediaService service;

	private String email;
	private boolean shouldWait;

	private long lastTouchedAt;
	private boolean waiting;

	public boolean running = false;

	public MediaThread(MediaService service, String email, boolean shouldWait) {
		this.service = service;
		this.email = email;
		this.shouldWait = shouldWait;
		// Init Variables
		database = new Database(service);
	}

	@Override
	public void run() {
		try {
			running = true;
			// Connect to the database
			database.connect();

			lastTouchedAt = System.currentTimeMillis();
			try {
				String userToken = GoogleAuthUtil.getToken(service, email, DriveApi.DRIVE_SCOPE);
				syncer = new CloudSyncer(service, userToken);
				// We are going to wait for a bit before actually doing anything
				// Users tend to use there camera in bursts. So they may take 10
				// photos in 1 session.
				// Pause this thread for 1 - 2 minutes before proceeding
				boolean waiting = shouldWait;
				while (waiting) {
					log("Waiting for more photos before scanning for media");
					try {
						Thread.sleep(60 * 1000);
					} catch (InterruptedException e) {
						log("Interrupted whilst waiting");
						threadError(e);
						return;
					}
					long now = System.currentTimeMillis();
					long temp = lastTouchedAt - 60000;
					waiting = temp > now;
				}
				log("Stopped waiting for photos. Scanning Media.");
				findMediaForUpload();
				threadFinished();
			} catch (NoSuchAlgorithmException e) {
				threadError(e);
			} catch (UserRecoverableAuthException e) {
				threadError(e);
				return;
			} catch (IOException e) {
				threadError(e);
				return;
			} catch (GoogleAuthException e) {
				threadError(e);
				return;
			} catch (JSONException e) {
				threadError(e);
			}
		} finally {
			// Disconnect from the database
			database.disconnect();
		}
	}

	private void threadFinished() {
		running = false;
		service.onThreadFinished();
	}

	private void threadError(Exception e) {
		running = false;
		service.onThreadError(e);
	}

	public boolean isWaiting() {
		return waiting;
	}

	public synchronized void touch() {
		lastTouchedAt = System.currentTimeMillis();
		log("Last Touched At: " + new Date(lastTouchedAt).toString());
	}

	private static final String[] projection = { MediaColumns.DATA, MediaColumns.MIME_TYPE };

	private static final Uri PHOTO_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
	private static final Uri VIDEO_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

	private void findMediaForUpload() throws IOException, NoSuchAlgorithmException, JSONException {

		String title = service.getResources().getString(
				R.string.pref_title_upload_status_uploading_scanning);
		String msg = service.getResources().getString(
				R.string.pref_summary_upload_status_uploading_scanning);

		ServiceUtils.sendServiceUpdate(title, msg);
		
		// First populate the hashes table in the database
		ArrayList<Media> media = new ArrayList<Media>();
		ArrayList<Media> temp = getAllMediaFromUri(PHOTO_URI);
		if (temp != null && temp.size() > 0) {
			for (Media m : temp) {
				if (m.getFile().exists() && m.getFile().length() > 0) {
					media.add(m);
				}
			}
			temp.clear();
		}
		temp = getAllMediaFromUri(VIDEO_URI);
		if (temp != null && temp.size() > 0) {
			for (Media m : temp) {
				if (m.getFile().exists() && m.getFile().length() > 0) {
					media.add(m);
				}
			}
			temp.clear();
		}

		// Get the MD5 hash of every file
		HashMap<String, Media> mediaHash = new HashMap<String, Media>();
		HashMap<String, Media> uploadedHash = new HashMap<String, Media>();
		for (int i = 0; i < media.size(); i++) {
			File f = media.get(i).getFile();
			String md5 = MD5Utils.getFileMd5(f);
			media.get(i).setMD5(md5);
			mediaHash.put(md5, media.get(i));
		}

		// This will ensure we have the latest
		// copy of the dir in our db
		syncer.sync();
		// Now figure out whats new on the device
		Cursor uploaded = database.getUploaded(new String[] { Database.UPLOADED_MD5 });
		while (uploaded.moveToNext()) {
			int md5_col = uploaded.getColumnIndexOrThrow(Database.UPLOADED_MD5);
			String md5 = uploaded.getString(md5_col);
			if (mediaHash.containsKey(md5)) {
				// This file has already been uploaded
				Media m = mediaHash.remove(md5);
				uploadedHash.put(md5, m);
			}
		}
		uploaded.close();

		// What ever is left in the mediaHash collection needs uploaded
		for (String key : mediaHash.keySet()) {
			Media m = mediaHash.get(key);
			database.addToPendingUploads(m.getFile().getAbsolutePath(), m.getFile().getName(),
					m.getMD5(), m.getType());
		}

		// Ensure we haven't added a duplicate to the database
		for (String key : uploadedHash.keySet()) {
			Media m = uploadedHash.get(key);
			database.removeFromPendingByMd5(m.getMD5());
		}

		// Lastly remove deleted files from pending uploads
		// Or remove files that are empty now
		Cursor pending = database.getPendingUploads(new String[] { Database.PENDING_UPLOADS_ID,
				Database.PENDING_UPLOADS_PATH });
		while (pending.moveToNext()) {
			int pathCol = pending.getColumnIndexOrThrow(Database.PENDING_UPLOADS_PATH);
			String path = pending.getString(pathCol);
			File f = new File(path);
			if (!f.exists() || f.length() == 0) {
				// This file has been deleted
				int idCol = pending.getColumnIndexOrThrow(Database.PENDING_UPLOADS_ID);
				database.removeFromPendingById(pending.getInt(idCol));
				log("File has been deleted: " + path);
			}
		}
		pending.close();

		// Print out pending uploads in development mode
		if (GDCU.DEVEL_BUILD) {
			pending = database.getPendingUploads(new String[] { Database.PENDING_UPLOADS_ID,
					Database.PENDING_UPLOADS_PATH });
			while (pending.moveToNext()) {
				int pathCol = pending.getColumnIndexOrThrow(Database.PENDING_UPLOADS_PATH);
				int idCol = pending.getColumnIndexOrThrow(Database.PENDING_UPLOADS_ID);
				String path = pending.getString(pathCol);
				int id = pending.getInt(idCol);
				log("Pending Upload: " + id + " " + path);
			}
			pending.close();
		}
	}

	private ArrayList<Media> getAllMediaFromUri(Uri uri) {
		ArrayList<Media> media = new ArrayList<Media>();
		Cursor cursor = service.getContentResolver().query(uri, projection, null, null, null);
		if (cursor == null) {
			return null;
		}
		while (cursor.moveToNext()) {
			int dataColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
			String filePath = cursor.getString(dataColumn);
			int mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE);
			String mimeType = cursor.getString(mimeTypeColumn);
			Media m = new Media(new java.io.File(filePath), mimeType);
			media.add(m);
		}
		cursor.close();
		return media;
	}

	private static void log(String msg) {
		Log.v("GDCU::MediaThread", msg);
	}
}
