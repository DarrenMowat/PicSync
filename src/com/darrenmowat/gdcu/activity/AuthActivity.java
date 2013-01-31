package com.darrenmowat.gdcu.activity;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.darrenmowat.gdcu.GDCU;
import com.darrenmowat.gdcu.Keys;
import com.darrenmowat.gdcu.R;
import com.darrenmowat.gdcu.data.Preferences;
import com.darrenmowat.gdcu.drive.DriveApi;
import com.darrenmowat.gdcu.tasks.GetTokenTask;
import com.flurry.android.FlurryAgent;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class AuthActivity extends SherlockActivity {

	public static final String KEY_EMAIL = "email";
	public static final String KEY_INTENT = "intent";

	static final int REQUEST_CODE_CHOOSE_ACCOUNT = 1000;
	static final int REQUEST_CODE_RECOVER_FROM_AUTH_ERROR = 1001;
	static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1002;
	static final int REQUEST_CODE_ERROR_DIALOG = 1003;

	private String mEmail;

	// Views
	private TextView descTextView;
	private Button signInButton;

	private TextView signInTextView;
	private ProgressBar signInProgress;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Setup the View
		setContentView(R.layout.activity_auth);
		descTextView = (TextView) findViewById(R.id.authDescText);
		signInButton = (Button) findViewById(R.id.authSignInButton);
		signInTextView = (TextView) findViewById(R.id.authSigningInText);
		signInProgress = (ProgressBar) findViewById(R.id.authProgressSpinner);

		mEmail = Preferences.getEmail(AuthActivity.this);

		Intent intent = getIntent();
		Intent intentToLaunch = intent.getParcelableExtra(KEY_INTENT);
		if (intentToLaunch != null) {
			startActivityForResult(intentToLaunch, REQUEST_CODE_RECOVER_FROM_AUTH_ERROR);
		} else if (mEmail != null) {
			// We've already stored this users email address
			new GetTokenTask(this, mEmail, DriveApi.DRIVE_SCOPE,
					REQUEST_CODE_RECOVER_FROM_AUTH_ERROR).execute();
			descTextView.setVisibility(View.GONE);
			signInButton.setVisibility(View.GONE);
			signInTextView.setVisibility(View.VISIBLE);
			signInProgress.setVisibility(View.VISIBLE);
			return;
		}
		// This user will have to pick a com.google account
		descTextView.setVisibility(View.VISIBLE);
		signInButton.setVisibility(View.VISIBLE);
		signInTextView.setVisibility(View.GONE);
		signInProgress.setVisibility(View.GONE);
		// new GetTokenTask(this, mEmail, SCOPE,
		// REQUEST_CODE_RECOVER_FROM_AUTH_ERROR).execute();
		signInButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = AccountPicker.newChooseAccountIntent(null, null,
						new String[] { GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE }, false, null, null,
						null, null);
				startActivityForResult(intent, REQUEST_CODE_CHOOSE_ACCOUNT);
			}

		});
	}

	@Override
	public void onStart() {
		super.onStart();
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (status != ConnectionResult.SUCCESS) {
			Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(status, this,
					REQUEST_CODE_ERROR_DIALOG);
			errorDialog.show();
			// Don't allow the user to cancel the dialog
			// Or close the activity on close
		}
		if (!GDCU.DEVEL_BUILD) {
			FlurryAgent.onStartSession(this, Keys.FLURRY_KEY);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if (!GDCU.DEVEL_BUILD) {
			FlurryAgent.onEndSession(this);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.activity_auth_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final Context context = AuthActivity.this;
		String title = null;
		switch (item.getItemId()) {
		case R.id.menu_auth_terms:
			title = context.getResources().getString(R.string.title_activity_webview_terms);
			Intent terms = new Intent(context, WebviewActivity.class);
			terms.putExtra("title", title);
			terms.putExtra("url", "file:///android_asset/terms.html");
			context.startActivity(terms);
			return true;
		case R.id.menu_auth_privacy:
			title = context.getResources()
					.getString(R.string.title_activity_webview_privacy_policy);
			Intent privacy = new Intent(context, WebviewActivity.class);
			privacy.putExtra("title", title);
			privacy.putExtra("url", "file:///android_asset/privacy.html");
			context.startActivity(privacy);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE_RECOVER_FROM_AUTH_ERROR) {
			handleAuthorizeResult(resultCode, data);
			return;
		} else if (requestCode == REQUEST_CODE_CHOOSE_ACCOUNT && resultCode == RESULT_OK) {
			String accEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
			if (accEmail != null) {
				mEmail = accEmail;
				new GetTokenTask(this, mEmail, DriveApi.DRIVE_SCOPE,
						REQUEST_CODE_RECOVER_FROM_AUTH_ERROR).execute();
				descTextView.setVisibility(View.GONE);
				signInButton.setVisibility(View.GONE);
				signInTextView.setVisibility(View.VISIBLE);
				signInProgress.setVisibility(View.VISIBLE);
			}
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * This method is a hook for background threads and async tasks that need to
	 * launch a dialog. It does this by launching a runnable under the UI
	 * thread.
	 */
	public void showErrorDialog(final int code) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Dialog d = GooglePlayServicesUtil.getErrorDialog(code, AuthActivity.this,
						REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
				d.show();
				// Reset the activity
				descTextView.setVisibility(View.VISIBLE);
				signInButton.setVisibility(View.VISIBLE);
				signInTextView.setVisibility(View.GONE);
				signInProgress.setVisibility(View.GONE);
			}
		});
	}

	public void passToken(final String token) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				final Activity context = AuthActivity.this;
				GDCU.getGDCU(context).setUserToken(token);
				log("Loaded User Token: " + token);
				Intent parentActivityIntent = new Intent(context, SettingsActivity.class);
				parentActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
						| Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(parentActivityIntent);
				context.finish();
			}
		});
	}

	private void handleAuthorizeResult(int resultCode, Intent data) {
		if (data == null) {
			log("Unknown error, click the button again");
			return;
		}
		if (resultCode == RESULT_OK) {
			log("Retrying");
			new GetTokenTask(this, mEmail, DriveApi.DRIVE_SCOPE,
					REQUEST_CODE_RECOVER_FROM_AUTH_ERROR).execute();
			return;
		}
		if (resultCode == RESULT_CANCELED) {
			log("User rejected authorization.");
			descTextView.setVisibility(View.VISIBLE);
			signInButton.setVisibility(View.VISIBLE);
			signInTextView.setVisibility(View.GONE);
			signInProgress.setVisibility(View.GONE);
			return;
		}
		log("Unknown error, click the button again");
		descTextView.setVisibility(View.VISIBLE);
		signInButton.setVisibility(View.VISIBLE);
		signInTextView.setVisibility(View.GONE);
		signInProgress.setVisibility(View.GONE);
	}

	private void log(String msg) {
		Log.v("AuthActivity", msg);
	}

}
