package com.darrenmowat.gdcu.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.darrenmowat.gdcu.R;
import com.darrenmowat.gdcu.utils.Preconditions;

public class Preferences {

	public final static String EMAIL = "email";
	public final static String UPLOADS_FOLDER = "uploads_folder";
	public final static String UPLOADS_FOLDER_NAME = "uploads_folder_name";

	private static SharedPreferences getSharedPreferences(Context context) {
		Preconditions.checkNotNull(context, "Context passed into Preferences is null");
		return PreferenceManager.getDefaultSharedPreferences(context);
	}

	public static String getEmail(Context context) {
		SharedPreferences sp = getSharedPreferences(context);
		return sp.getString(EMAIL, null);
	}

	public static void storeEmail(Context context, String email) {
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		if (email == null) {
			editor.remove(EMAIL);
		} else {
			editor.putString(EMAIL, email);
		}
		editor.commit();
	}

	public static String getUploadsFolderName(Context context) {
		SharedPreferences sp = getSharedPreferences(context);
		String def = context.getString(R.string.uploads_folder_name);
		String uploadsFolderName = sp.getString(UPLOADS_FOLDER_NAME, def);
		// If stored vaule is empty, return default
		if(uploadsFolderName == null || uploadsFolderName.trim().equals("")) {
			storeUploadsFolderName(context, null);
			return def;
		}
		// If stored value isnt valid, return default
		if(uploadsFolderName.contains("/")) {
			storeUploadsFolderName(context, null);
			return def;
		}
		return uploadsFolderName;
	}

	public static void storeUploadsFolderName(Context context, String uploadsFolderName) {
		SharedPreferences sp = getSharedPreferences(context);
		Editor editor = sp.edit();
		if (uploadsFolderName == null) {
			editor.remove(UPLOADS_FOLDER_NAME);
		} else {
			editor.putString(UPLOADS_FOLDER_NAME, uploadsFolderName);
		}
		editor.commit();
	}

	public static void clearPreferences(Context context) {
		Preferences.storeEmail(context, null);
		Preferences.storeUploadsFolderName(context, null);
	}

}
