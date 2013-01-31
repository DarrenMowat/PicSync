package com.darrenmowat.gdcu.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Database {

	public static final String DATABASE_NAME = "gdcu_data";
	public static final int DATABASE_VERSION = 10;

	public static final String PENDING_UPLOADS_TABLE = "pending_uploads";
	public static final String PENDING_UPLOADS_ID = "_id";
	public static final String PENDING_UPLOADS_PATH = "path";
	public static final String PENDING_UPLOADS_TITLE = "title";
	public static final String PENDING_UPLOADS_MD5 = "md5";
	public static final String PENDING_UPLOADS_ADDED = "added_at";
	public static final String PENDING_UPLOADS_MIME = "mime";

	public static final String PENDING_UPLOADS_TABLE_CREATE = "CREATE TABLE " //
			+ PENDING_UPLOADS_TABLE + " (" //
			+ PENDING_UPLOADS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," //
			+ PENDING_UPLOADS_PATH + " TEXT NOT NULL UNIQUE," //
			+ PENDING_UPLOADS_TITLE + " TEXT NOT NULL UNIQUE," //
			+ PENDING_UPLOADS_MD5 + " TEXT," //
			+ PENDING_UPLOADS_ADDED + " INTEGER NOT NULL," //
			+ PENDING_UPLOADS_MIME + " TEXT NOT NULL" //
			+ ");"; //

	public static final String UPLOADED_TABLE = "uploaded";
	public static final String UPLOADED_ID = "_id";
	public static final String UPLOADED_TITLE = "title";
	public static final String UPLOADED_MD5 = "md5";
	public static final String UPLOADED_DRIVE_ID = "drive_id";
	public static final String UPLOADED_ADDED = "uploaded_at";
	public static final String UPLOADED_MIME = "mime";

	public static final String UPLOADED_TABLE_CREATE = "CREATE TABLE " //
			+ UPLOADED_TABLE + " (" //
			+ UPLOADED_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," //
			+ UPLOADED_TITLE + " TEXT NOT NULL," //
			+ UPLOADED_MD5 + " TEXT NOT NULL," //
			+ UPLOADED_DRIVE_ID + " TEXT NOT NULL UNIQUE," //
			+ UPLOADED_ADDED + " INTEGER NOT NULL," //
			+ UPLOADED_MIME + " TEXT NOT NULL" //
			+ ");"; //

	public static final String SYNCS_TABLE = "syncs";
	public static final String SYNCS_ID = "_id";
	public static final String SYNCS_DATE = "date";

	public static final String SYNCS_TABLE_CREATE = "CREATE TABLE " //
			+ SYNCS_TABLE + " (" //
			+ SYNCS_ID + " INTEGER NOT NULL," //
			+ SYNCS_DATE + " INTEGER NOT NULL" //
			+ ");"; //

	private DatabaseHelper dbHelper;
	private SQLiteDatabase db;

	public Database(Context context) {
		dbHelper = new DatabaseHelper(context);
	}

	public void connect() {
		if (db == null) {
			db = dbHelper.getWritableDatabase();
		}
	}

	public void disconnect() {
		db.close();
		db = null;
	}

	private class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(PENDING_UPLOADS_TABLE_CREATE);
			db.execSQL(UPLOADED_TABLE_CREATE);
			db.execSQL(SYNCS_TABLE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("drop table if exists " + PENDING_UPLOADS_TABLE);
			db.execSQL("drop table if exists " + UPLOADED_TABLE);
			db.execSQL("drop table if exists " + SYNCS_TABLE);
			onCreate(db);
		}

	}

	public Cursor getPendingUploads(String[] columns) {
		return db.query(PENDING_UPLOADS_TABLE, columns, null, null, null, null, null);
	}

	public void addToPendingUploads(String path, String title, String md5, String mime) {
		synchronized (db) {
			ContentValues cv = new ContentValues();
			cv.put(PENDING_UPLOADS_PATH, path);
			cv.put(PENDING_UPLOADS_TITLE, title);
			cv.put(PENDING_UPLOADS_MD5, md5);
			cv.put(PENDING_UPLOADS_ADDED, System.currentTimeMillis());
			cv.put(PENDING_UPLOADS_MIME, mime);
			try {
				db.insertOrThrow(PENDING_UPLOADS_TABLE, null, cv);
			} catch (SQLException e) {
				log("Couldn't insert into " + PENDING_UPLOADS_TABLE + ": " + e.getMessage());
			}
		}
	}

	private void removeFromPending(String where, String[] args) {
		synchronized (db) {
			db.delete(PENDING_UPLOADS_TABLE, where, args);
		}
	}

	public void removeFromPendingByPath(String path) {
		removeFromPending(PENDING_UPLOADS_PATH + "=?", new String[] { path });
	}

	public void removeFromPendingById(int id) {
		removeFromPending(PENDING_UPLOADS_ID + "=?", new String[] { String.valueOf(id) });
	}

	public void removeFromPendingByMd5(String md5) {
		removeFromPending(PENDING_UPLOADS_MD5 + "=?", new String[] { md5 });
	}

	public boolean addToUploaded(String title, String md5, String drive_id, long uploaded_at,
			String mime) {
		synchronized (db) {
			ContentValues cv = new ContentValues();
			cv.put(UPLOADED_TITLE, title);
			cv.put(UPLOADED_MD5, md5);
			cv.put(UPLOADED_DRIVE_ID, drive_id);
			cv.put(UPLOADED_ADDED, uploaded_at);
			cv.put(UPLOADED_MIME, mime);
			try {
				db.insertOrThrow(UPLOADED_TABLE, null, cv);
				return true;
			} catch (SQLException e) {
				log("Couldn't insert into " + UPLOADED_TABLE + ": " + e.getMessage());
				return false;
			}
		}
	}

	public Cursor getUploaded(String[] columns) {
		return db.query(UPLOADED_TABLE, columns, null, null, null, null, null);
	}

	public void setLastSyncedTime(long time) {
		synchronized (db) {
			ContentValues cv = new ContentValues();
			cv.put(SYNCS_ID, 1);
			cv.put(SYNCS_DATE, time);
			try {
				db.delete(SYNCS_TABLE, null, null);
				db.insertOrThrow(SYNCS_TABLE, null, cv);
			} catch (SQLException e) {
				log("Couldn't insert into " + SYNCS_TABLE + ": " + e.getMessage());
			}
		}
	}

	public long getLastSyncedTime() {
		Cursor syncs = null;
		long time = 0;
		try {
			syncs = db.query(SYNCS_TABLE, null, null, null, null, null, null);
			if (syncs.moveToFirst()) {
				time = syncs.getLong(syncs.getColumnIndex(SYNCS_DATE));
			}
		} finally {
			if (syncs != null) {
				syncs.close();
			}
		}
		return time;
	}

	public boolean clearDatabase() {
		synchronized (db) {
			try {
				db.delete(PENDING_UPLOADS_TABLE, null, null);
				db.delete(UPLOADED_TABLE, null, null);
				db.delete(SYNCS_TABLE, null, null);
				return true;
			} catch (SQLException e) {
				log(e.getMessage());
				return false;
			}
		}
	}

	private void log(String msg) {
		Log.v("GDCU::Database", msg);
	}

}
