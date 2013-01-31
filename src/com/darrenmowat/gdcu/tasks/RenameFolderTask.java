package com.darrenmowat.gdcu.tasks;

import java.io.IOException;

import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;

import com.darrenmowat.gdcu.activity.RenameFolderActivity;
import com.darrenmowat.gdcu.data.Database;
import com.darrenmowat.gdcu.data.Preferences;
import com.darrenmowat.gdcu.drive.DriveApi;
import com.darrenmowat.gdcu.service.MediaService;
import com.darrenmowat.gdcu.service.UploadService;
import com.google.api.services.drive.model.File;

public class RenameFolderTask extends AsyncTask<Void, Void, Void> {

	protected RenameFolderActivity mActivity;
	protected String mToken;
	protected String mCurrentName;
	protected String mNewName;

	public RenameFolderTask(RenameFolderActivity activity, String token, String currentName, String newName) {
		this.mActivity = activity;
		this.mToken = token;
		this.mCurrentName = currentName;
		this.mNewName = newName;
	}

	@Override
	protected Void doInBackground(Void... params) {
		DriveApi drive = new DriveApi(mToken, mCurrentName);
		try {
			// Really need to kill any background service before we continue
			Intent scannerService = new Intent(mActivity, MediaService.class);
			mActivity.stopService(scannerService);
			Intent uploadService = new Intent(mActivity, UploadService.class);
			mActivity.stopService(uploadService);
			// Now actually rename the folder
			File newUploads = drive.renameUploadsDir(mNewName);
			Preferences.storeUploadsFolderName(mActivity, mNewName);
			// Clear the database as we may have pending uploads to a old folder
			Database database = new Database(mActivity);
			database.connect();
			database.clearDatabase();
			database.disconnect();
			mActivity.onDone(newUploads);
		} catch (IOException e) {
			mActivity.onIOException(e);
		} catch (JSONException e) {
			mActivity.onJSONException(e);
		}
		return null;
	}


}