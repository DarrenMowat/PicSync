package com.darrenmowat.gdcu.activity;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.darrenmowat.gdcu.GDCU;
import com.darrenmowat.gdcu.Keys;
import com.darrenmowat.gdcu.R;
import com.darrenmowat.gdcu.data.Database;
import com.darrenmowat.gdcu.data.Preferences;
import com.darrenmowat.gdcu.otto.DataBus;
import com.darrenmowat.gdcu.otto.UploadStatusEvent;
import com.darrenmowat.gdcu.service.MediaService;
import com.flurry.android.FlurryAgent;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.squareup.otto.Subscribe;

@SuppressWarnings("deprecation")
public class SettingsActivity extends SherlockPreferenceActivity {

	public static final int ERROR_DIALOG_REQUEST_CODE = 4;

	private String userEmail;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// See if a user is logged in
		if (GDCU.getGDCU(SettingsActivity.this).getUserToken() == null) {
			Intent auth = new Intent(SettingsActivity.this, AuthActivity.class);
			startActivity(auth);
			finish();
			return;
		}

		addPreferencesFromResource(R.xml.settings);
		final Context context = this;

		userEmail = Preferences.getEmail(SettingsActivity.this);
		final Preference manageAccount = findPreference("manageAccount");
		manageAccount.setTitle(R.string.pref_title_remove_account);
		String summary = String.format(
				getResources().getString(R.string.pref_summary_current_account), userEmail);
		manageAccount.setSummary(summary);

		manageAccount.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				final Context context = SettingsActivity.this;
				// Revoke this access token
				GoogleAuthUtil.invalidateToken(context, GDCU.getGDCU(context).getUserToken());
				// Delete the email from sp
				Preferences.clearPreferences(context);
				// Clear the user token from GDCU
				GDCU.getGDCU(context).setUserToken(null);
				// Clear the database
				Database db = new Database(context);
				db.connect();
				db.clearDatabase();
				db.disconnect();
				// Restart activity
				Intent intent = getIntent();
				finish();
				startActivity(intent);
				return false;
			}

		});

		Preference pending = findPreference("pending");
		pending.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent service = new Intent(context, MediaService.class);
				service.setAction(MediaService.NO_WAIT);
				context.startService(service);
				return false;
			}

		});

		Preference rename = findPreference("rename");
		rename.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				Intent rename = new Intent(context, RenameFolderActivity.class);
				context.startActivity(rename);
				return false;
			}

		});

		Preference about = findPreference("about");
		about.setSummary(GDCU.PRETTY_BUILD_STRING);

		Preference darrenmowat = findPreference("darrenmowat");
		darrenmowat.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				final Context context = SettingsActivity.this;
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri
						.parse("http://www.darrenmowat.co.uk"));
				context.startActivity(browserIntent);
				return false;
			}

		});

		Preference terms = findPreference("terms");
		terms.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				final Context context = SettingsActivity.this;
				String title = context.getResources().getString(
						R.string.title_activity_webview_terms);
				Intent p = new Intent(context, WebviewActivity.class);
				p.putExtra("title", title);
				p.putExtra("url", "file:///android_asset/terms.html");
				context.startActivity(p);
				return false;
			}

		});

		Preference privacy = findPreference("privacy");
		privacy.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				final Context context = SettingsActivity.this;
				String title = context.getResources().getString(
						R.string.title_activity_webview_privacy_policy);
				Intent p = new Intent(context, WebviewActivity.class);
				p.putExtra("title", title);
				p.putExtra("url", "file:///android_asset/privacy.html");
				context.startActivity(p);
				return false;
			}

		});

		Preference feedback = findPreference("feedback");
		feedback.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {

				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("plain/text");
				i.putExtra(Intent.EXTRA_EMAIL, new String[] { GDCU.FEEDBACK_EMAIL });
				i.putExtra(Intent.EXTRA_SUBJECT, GDCU.APP_NAME + " Feedback - "
						+ GDCU.PRETTY_BUILD_STRING);
				i.putExtra(Intent.EXTRA_TEXT, getFeedbackString());
				context.startActivity(Intent.createChooser(i, "Select email application."));
				return false;
			}

		});

		Intent service = new Intent(context, MediaService.class);
		service.setAction(MediaService.NO_WAIT);
		context.startService(service);
	}

	@Override
	public void onStart() {
		super.onStart();
		int isGooglePlayServicesAvailable = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		if (isGooglePlayServicesAvailable != ConnectionResult.SUCCESS) {
			Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
					isGooglePlayServicesAvailable, this, ERROR_DIALOG_REQUEST_CODE);
			errorDialog.show();
			// TODO: Don't allow the user to cancel the dialog
			// Or close the activity on close
		}
		DataBus.getInstance().register(this);
		if (!GDCU.DEVEL_BUILD) {
			FlurryAgent.onStartSession(this, Keys.FLURRY_KEY);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		DataBus.getInstance().unregister(this);
		if (!GDCU.DEVEL_BUILD) {
			FlurryAgent.onEndSession(this);
		}
	}


	@Subscribe
	public void onUploadStatusEvent(final UploadStatusEvent event) {
		SettingsActivity.this.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Preference pending = findPreference("pending");
				pending.setTitle(event.title);
				pending.setSummary(event.message);
			}

		});
	}

	private String getFeedbackString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		sb.append("\n");
		sb.append("Phone: ");
		sb.append(Build.MODEL);
		sb.append(" Android: ");
		sb.append(Build.VERSION.RELEASE);
		sb.append(" [");
		sb.append(Build.VERSION.SDK_INT);
		sb.append("] ");
		sb.append("\n");
		return sb.toString();
	}

}
