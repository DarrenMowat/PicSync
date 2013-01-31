package com.darrenmowat.gdcu.service.helpers;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONException;

import android.content.Context;

import com.darrenmowat.gdcu.GDCU;
import com.darrenmowat.gdcu.data.Database;
import com.darrenmowat.gdcu.data.Preferences;
import com.darrenmowat.gdcu.drive.DriveApi;
import com.flurry.android.FlurryAgent;
import com.google.api.services.drive.model.File;

/*
 * This class keeps our database in sync with 
 * all the files that have been uploaded to Cloud
 */
public class CloudSyncer {

	private Database database;
	private DriveApi drive;

	public CloudSyncer(Context context, String userToken) {
		database = new Database(context);
		String uploadsDirName = Preferences.getUploadsFolderName(context);
		drive = new DriveApi(userToken, uploadsDirName);
	}

	public void sync() throws IOException, JSONException {
		database.connect();
		long lastSync = database.getLastSyncedTime();
		ArrayList<File> files = new ArrayList<File>();
		if (lastSync == 0) {
			// We've never synced with Drive
			files = drive.retrieveAllUploadedFiles();
		} else {
			// Sync changes since our last sync
			files = drive.retrieveAllUploadedFilesSince(lastSync);
		}
		// Add all the files to the database
		// SQLite will deal with conflicts
		for (File file : files) {
			database.addToUploaded(file.getTitle(), file.getMd5Checksum(), file.getId(), file
					.getCreatedDate().getValue(), file.getMimeType());
		}
		// What if something changed whilst we were downloading this
		long timestamp = System.currentTimeMillis() - 60000;
		database.setLastSyncedTime(timestamp);
		database.disconnect();
		if (!GDCU.DEVEL_BUILD) {
			FlurryAgent.logEvent("Synced with GDrive");
		}
	}
}
