package com.darrenmowat.gdcu;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.provider.MediaStore;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.crittercism.app.Crittercism;
import com.darrenmowat.gdcu.otto.DataBus;
import com.darrenmowat.gdcu.service.GalleryAlarmListener;
import com.darrenmowat.gdcu.service.GalleryObserver;
import com.flurry.android.FlurryAgent;
import com.google.api.services.drive.model.File;

public class GDCU extends Application {

	public static final boolean DEVEL_BUILD = true;

	public static final String APP_NAME = "PicSync Beta";

	public static int VERSION;
	public static String VERSION_STRING;
	public static final String BUILD_TYPE = DEVEL_BUILD ? "Development Build" : "Beta Build";

	public static String PRETTY_BUILD_STRING;

	public static final String FEEDBACK_EMAIL = "dmowat91@gmail.com";

	private GalleryObserver photoObserver;
	private GalleryObserver videoObserver;

	private static File uploadsDir = null;
	private static String rootId = null;

	private String userToken = null;
	private String apiToken = null;

	public static GDCU getGDCU(Context context) {
		return (GDCU) context.getApplicationContext();
	}

	@Override
	public void onCreate() {
		super.onCreate();

		try {
			PackageInfo app = getPackageManager().getPackageInfo(getPackageName(), 0);
			VERSION = app.versionCode;
			VERSION_STRING = app.versionName;
			PRETTY_BUILD_STRING = VERSION_STRING + " " + BUILD_TYPE;
		} catch (NameNotFoundException e) {
			// We should never be here, unless our package doesn't exist on the
			// system
			// But how would this be running if it didn't
		}

		if (!DEVEL_BUILD) {
			FlurryAgent.setCaptureUncaughtExceptions(false);
			Crittercism.init(getApplicationContext(), "50382024eeaf411be9000002");
		}

		DataBus.getInstance().register(this);

		// Setup our media observers
		Uri PHOTO_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		photoObserver = new GalleryObserver(this, PHOTO_URI);
		getContentResolver().registerContentObserver(PHOTO_URI, false, photoObserver);
		Uri VIDEO_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
		videoObserver = new GalleryObserver(this, VIDEO_URI);
		getContentResolver().registerContentObserver(VIDEO_URI, false, videoObserver);
		
		WakefulIntentService.scheduleAlarms(new GalleryAlarmListener(), this, false);
	}

	public static void setCachedUploadsDir(File uploadsDir) {
		GDCU.uploadsDir = uploadsDir;
	}

	public static File getCachedUploadsDir() {
		return uploadsDir;
	}

	public void setUserToken(String token) {
		userToken = token;
	}

	public String getUserToken() {
		return userToken;
	}

	public void setApiToken(String token) {
		apiToken = token;
	}

	public String getApiToken() {
		return apiToken;
	}

	public static void setCachedRootDir(String rootId) {
		GDCU.rootId = rootId;
	}

	public static String getCachedRootDir() {
		return rootId;
	}

}
