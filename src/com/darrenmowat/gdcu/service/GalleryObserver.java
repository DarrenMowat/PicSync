package com.darrenmowat.gdcu.service;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;

import com.darrenmowat.gdcu.service.helpers.Media;

public class GalleryObserver extends ContentObserver {

	private Context context;
	private Uri BASE_URI;

	public GalleryObserver(Context context, Uri base) {
		super(null);
		this.context = context;
		this.BASE_URI = base;
	}

	@Override
	public void onChange(boolean selfChange) {
		super.onChange(selfChange);
		Media media = readFromMediaStore(context, BASE_URI);
		if (media == null) {
			log("Media returned from database was null");
			return;
		}

		Intent service = new Intent(context, MediaService.class);
		context.startService(service);
	}

	private static final String[] projection = { MediaColumns.DATA, MediaColumns.MIME_TYPE };
	private static final String sortorder = MediaColumns.DATE_ADDED + " DESC LIMIT 1";

	private Media readFromMediaStore(Context context, Uri uri) {
		Cursor cursor = context.getContentResolver().query(uri, projection, null, null, sortorder);
		Media media = null;
		if (cursor.moveToNext()) {
			int dataColumn = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
			String filePath = cursor.getString(dataColumn);
			int mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaColumns.MIME_TYPE);
			String mimeType = cursor.getString(mimeTypeColumn);
			media = new Media(new File(filePath), mimeType);
		}
		cursor.close();
		return media;
	}

	private void log(String msg) {
		Log.v("GDCU::GalleryObserver", msg);
	}

}
