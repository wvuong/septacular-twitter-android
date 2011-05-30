package com.willvuong.septacular.android;

import java.util.Collection;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    private static final String DATABASE_NAME = "septacular.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "feed";

	private SQLiteDatabase db;

    
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	public void open() {
		db = getWritableDatabase();
	}
	
	public void close() {
		db.close();
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(String.format("create table %s (%s integer primary key," +
				"%s text, %s varchar, %s integer, %s integer, %s boolean);", 
				TABLE_NAME, RssItemColumns._ID, RssItemColumns.TITLE, RssItemColumns.LINK,
				RssItemColumns.PUBLISHED, RssItemColumns.CREATED, RssItemColumns.OLD));
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
		db.execSQL("drop table if exists " + TABLE_NAME);
		onCreate(db);
	}

	public void refreshFeed(Collection<RssItem> items) {
		db.beginTransaction();
		
		long now = System.currentTimeMillis();
		
		// drop all rows
		db.delete(TABLE_NAME, null, null);
		
		// insert items
		ContentValues v = new ContentValues();
		for (RssItem item : items) {
			v.put(RssItemColumns._ID, item.getId());
			v.put(RssItemColumns.TITLE, item.getTitle());
			v.put(RssItemColumns.LINK, item.getLink());
			v.put(RssItemColumns.PUBLISHED, item.getPublished().getTime());
			v.put(RssItemColumns.OLD, item.isOld());
			v.put(RssItemColumns.CREATED, now);
			
			db.insert(TABLE_NAME, null, v);
		}
		
		db.setTransactionSuccessful();
		
		db.endTransaction();
		
		Log.d(TAG, "refreshed db with " + items.size() + " items");
	}
	
	public Cursor getItems() {
		return db.query(TABLE_NAME, RssItemColumns.COLS, 
				null, null, 
				null, null, 
				RssItemColumns.DEFAULT_ORDERBY);
	}
	
	public Cursor getItem(long id) {
		Cursor cursor = db.query(TABLE_NAME, RssItemColumns.COLS, 
				RssItemColumns._ID + "=" + id, null, 
				null, null, 
				RssItemColumns.DEFAULT_ORDERBY);
		cursor.moveToFirst();
		return cursor;
	}
	
	public static final class RssItemColumns implements BaseColumns {
		public static final String TITLE = "title";
		
		public static final String LINK = "link";
		
		public static final String PUBLISHED = "published";
		
		public static final String CREATED = "created";
		
		public static final String DEFAULT_ORDERBY = "published desc";
		
		public static final String OLD = "old";
		
		public static final String[] COLS = new String[] {_ID, TITLE, LINK, PUBLISHED, OLD, CREATED};
	}
}
