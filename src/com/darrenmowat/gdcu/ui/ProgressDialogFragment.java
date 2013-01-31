package com.darrenmowat.gdcu.ui;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.darrenmowat.gdcu.R;

/**
 * Basic DialogFragment which displays a progress spinner
 * 
 * A custom message can be defined via newInstance(String message)
 * 
 * @author Darren Mowat
 * 
 */
public class ProgressDialogFragment extends SherlockDialogFragment {

	/**
	 * 
	 * @param message - The message to display in the progress dialog
	 * @return a new custom instance of ProgressDialogFragment
	 */
	public static ProgressDialogFragment newInstance(String message) {
		ProgressDialogFragment frag = new ProgressDialogFragment();
		Bundle b = new Bundle();
		b.putString("msg", message);
		frag.setArguments(b);
		return frag;
	}


	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle b = getArguments();
		String message = b.getString("msg");
		if(message == null) {
			message = getResources().getString(R.string.rename_progress);
		}
		final ProgressDialog dialog = new ProgressDialog(getActivity());
		dialog.setMessage(message);
		dialog.setIndeterminate(true);
		dialog.setCancelable(false);
		return dialog;
	}

}
