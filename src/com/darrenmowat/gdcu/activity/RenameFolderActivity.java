package com.darrenmowat.gdcu.activity;

import java.io.IOException;

import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.darrenmowat.gdcu.GDCU;
import com.darrenmowat.gdcu.R;
import com.darrenmowat.gdcu.data.Preferences;
import com.darrenmowat.gdcu.service.MediaService;
import com.darrenmowat.gdcu.tasks.RenameFolderTask;
import com.darrenmowat.gdcu.ui.ProgressDialogFragment;
import com.google.api.services.drive.model.File;

public class RenameFolderActivity extends SherlockFragmentActivity {

	private EditText newNameET;
	private RenameFolderTask mTask;
	
	private ProgressDialogFragment progressFragment;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rename_folder_activtiy);

		newNameET = (EditText) findViewById(R.id.renameFolderEditText);
		
		final String currentName = Preferences.getUploadsFolderName(RenameFolderActivity.this);

		newNameET.getText().append(currentName);
		
		Button renameButton = (Button) findViewById(R.id.renameFolderButton);
		renameButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				final Context context = RenameFolderActivity.this;
				String newName = newNameET.getText().toString();
				if (isValidName(newName)) {
					String token = GDCU.getGDCU(context).getUserToken();
					mTask = new RenameFolderTask(RenameFolderActivity.this, token, currentName, newName);
					mTask.execute();
					showProgressFragment();
				} else {
					Toast.makeText(RenameFolderActivity.this, "Invalid folder name",
							Toast.LENGTH_SHORT).show();
				}

			}

		});
	}

	private boolean isValidName(String name) {
		return true;
	}
	
	public void onDone(final File newUploads) {
		final Activity activity = RenameFolderActivity.this;
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				removeProgressFragment();
				Toast.makeText(RenameFolderActivity.this, "Renamed to " + newUploads.getTitle(),
						Toast.LENGTH_SHORT).show();
				Intent service = new Intent(activity, MediaService.class);
				service.setAction(MediaService.NO_WAIT);
				activity.startService(service);
			}
			
		});
	}

	public void onIOException(final IOException e) {
		final Activity activity = RenameFolderActivity.this;
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				removeProgressFragment();
				Toast.makeText(RenameFolderActivity.this, "IOException " + e.getMessage(),
						Toast.LENGTH_SHORT).show();
			}
			
		});
	}
	
	public void onJSONException(final JSONException e) {
		final Activity activity = RenameFolderActivity.this;
		activity.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				removeProgressFragment();
				Toast.makeText(RenameFolderActivity.this, "JSONException " + e.getMessage(),
						Toast.LENGTH_SHORT).show();
			}
			
		});
	}
	
	private void removeProgressFragment() {
		if (progressFragment != null) {
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.remove(progressFragment);
			ft.commit();
			progressFragment = null;
		}
	}
	
	private void showProgressFragment() {
		if(progressFragment == null) { 
		progressFragment = ProgressDialogFragment.newInstance(getResources().getString(
				R.string.rename_progress));
		progressFragment.setCancelable(false);
		progressFragment.show(getSupportFragmentManager(), "renamingProgressDialog");
		}
	}
	
}
