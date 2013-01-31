package com.darrenmowat.gdcu.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.preference.PreferenceManager;

import com.darrenmowat.gdcu.R;
import com.darrenmowat.gdcu.otto.DataBus;
import com.darrenmowat.gdcu.otto.UploadStatusEvent;

public class ServiceUtils {

	public static boolean shouldUpload(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		boolean shouldOnlyUploadWifi = prefs.getBoolean("wifiOnly", false);
		if (shouldOnlyUploadWifi) {
			if (!isOnWiFi(context)) {
				String title = context.getResources().getString(
						R.string.pref_title_upload_status_uploads_pending);
				String msg = context.getResources().getString(
						R.string.pref_summary_upload_status_uploading_pending_for_wifi);
				sendServiceUpdate(title, msg);
				return false;
			}
		}
		boolean shouldOnlyUploadCharging = prefs.getBoolean("chargingOnly", false);
		if (shouldOnlyUploadCharging) {
			if (!isCharging(context)) {
				String title = context.getResources().getString(
						R.string.pref_title_upload_status_uploads_pending);
				String msg = context.getResources().getString(
						R.string.pref_summary_upload_status_uploading_pending_for_power);
				sendServiceUpdate(title, msg);
				return false;
			}
		}
		return true;
	}

	public static boolean isOnWiFi(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		boolean isOnWiFi = activeNetwork != null
				&& activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
		return isOnWiFi;
	}

	public static boolean isCharging(Context context) {
		Intent intent = context.registerReceiver(null, new IntentFilter(
				Intent.ACTION_BATTERY_CHANGED));
		int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		return plugged == BatteryManager.BATTERY_PLUGGED_AC
				|| plugged == BatteryManager.BATTERY_PLUGGED_USB;
	}
	
	public static void sendServiceUpdate(String title, String msg) {
		DataBus.postUploadStatusEventProducer(new UploadStatusEvent(title, msg));
	}
}
