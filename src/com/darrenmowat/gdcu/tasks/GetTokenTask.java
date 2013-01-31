package com.darrenmowat.gdcu.tasks;

import java.io.IOException;

import android.os.AsyncTask;

import com.darrenmowat.gdcu.GDCU;
import com.darrenmowat.gdcu.Keys;
import com.darrenmowat.gdcu.activity.AuthActivity;
import com.darrenmowat.gdcu.data.Preferences;
import com.darrenmowat.gdcu.security.PackageSecurity;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;

public class GetTokenTask extends AsyncTask<Void, Void, Void> {

	protected AuthActivity mActivity;

	protected String mScope;
	protected String mEmail;
	private int mRequestCode;

	public GetTokenTask(AuthActivity activity, String email, String scope, int requestCode) {
		this.mActivity = activity;
		this.mScope = scope;
		this.mEmail = email;
		this.mRequestCode = requestCode;
	}

	@Override
	protected Void doInBackground(Void... params) {
		String token = fetchToken();
		if (token == null) {
			// error has already been handled in fetchToken()
			return null;
		}
		Preferences.storeEmail(mActivity, mEmail);
		if (!GDCU.DEVEL_BUILD) {
			// Now we'll check if this package is signed correctly
			// If it aint we'll crash and log the culprit!
			//PackageSecurity.checkPackageCertificate(mActivity, "com.darrenmowat.gdcu",
			//		Keys.PACKAGE_SIGNATURE, mEmail);
		}
		mActivity.passToken(token);
		return null;
	}

	/**
	 * Get a authentication token if one is not available. If the error is not
	 * recoverable then it displays the error message on parent activity.
	 */
	private String fetchToken() {
		try {
			String token = GoogleAuthUtil.getToken(mActivity, mEmail, mScope);
			return token;
		} catch (GooglePlayServicesAvailabilityException playEx) {
			// GooglePlayServices.apk is either old, disabled, or not present.
			mActivity.showErrorDialog(playEx.getConnectionStatusCode());
		} catch (UserRecoverableAuthException userRecoverableException) {
			// Unable to authenticate, but the user can fix this.
			// Forward the user to the appropriate activity.
			mActivity.startActivityForResult(userRecoverableException.getIntent(), mRequestCode);
		} catch (GoogleAuthException fatalException) {
			mActivity.showErrorDialog(ConnectionResult.INTERNAL_ERROR);
		} catch (IOException e) {
			mActivity.showErrorDialog(ConnectionResult.NETWORK_ERROR);
		}
		return null;
	}

}