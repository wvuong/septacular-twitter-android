package com.willvuong.septacular.android;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.google.android.divideandconquer.Eula;
import com.willvuong.septacular.android.DatabaseHelper.RssItemColumns;

public class FeedActivity extends ListActivity {
	private static final String TAG = "FeedActivity";
	
	// menu constants
	private static final int MENU_REFRESH = Menu.FIRST;
	private static final int MENU_PREFS = Menu.FIRST+1;
	private static final int MENU_ABOUT = Menu.FIRST+2;
	private static final int MENU_DEBUG = Menu.FIRST+3;
	
	// context constants
	private static final int CONTEXT_COPYTEXT = Menu.FIRST;
	private static final int CONTEXT_COPYLINK = Menu.FIRST+1;
	
	private static final SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss");
	
	// receive database has been updated intent broadcasts
	private DatabaseUpdatedReceiver receiver = new DatabaseUpdatedReceiver();

	private SharedPreferences prefs;
	private PackageInfo packageInfo;

	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        registerForContextMenu(getListView());
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // get version from manifest
		try {
			this.packageInfo = getPackageManager().getPackageInfo("com.willvuong.septacular.android",
					PackageManager.GET_META_DATA);

		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
        
        // show eula
        Eula.show(this);
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	
    	updateView();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	// register receiver for database update event to update ui while app is in foreground
    	registerReceiver(receiver, new IntentFilter(UpdaterService.DATABASE_UPDATED));
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
    	// unregister receiver
    	unregisterReceiver(receiver);
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    }
    
    
    private void manualUpdateView() {
    	startService(new Intent(this, UpdaterService.class));
    }
    
    private void updateView() {
    	Log.d(TAG, "updateView()");
    	
    	DatabaseHelper helper = new DatabaseHelper(this);
    	helper.open();
    	
    	Cursor cursor = helper.getItems();
    	startManagingCursor(cursor);
    	
    	String[] from = new String[] {RssItemColumns.TITLE, RssItemColumns.PUBLISHED};
    	int[] to = new int[] {R.id.text1, R.id.text2};
    	
    	FeedListAdapter adapter = new FeedListAdapter(this, R.layout.row, cursor, from, to);
    	setListAdapter(adapter);
    	
    	long ts = prefs.getLong("lastupdatets", 0);
    	if (ts != 0) {
    		setTitle("Septacular - updated " + f.format(new Date(ts)));
    	}
    	else {
    		setTitle("Septacular - not updated yet");
    	}
    	
    	helper.close();
    	
    	Toast.makeText(this, R.string.toast_status_updating, Toast.LENGTH_LONG);
    }
    
    // create menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	boolean result = super.onCreateOptionsMenu(menu);
		MenuItem mi = menu.add(0, MENU_REFRESH, 0, R.string.menu_refresh);
		mi.setIcon(R.drawable.refresh);
		
		mi = menu.add(0, MENU_PREFS, 1, R.string.menu_prefs);
		mi.setIcon(android.R.drawable.ic_menu_preferences);
		
		mi = menu.add(0, MENU_ABOUT, 2, R.string.menu_about);
		mi.setIcon(android.R.drawable.ic_menu_info_details);
		
		mi = menu.add(0, MENU_DEBUG, 3, R.string.menu_debug);
		mi.setIcon(android.R.drawable.ic_menu_more);
		
    	return result;
    }
    
    private void showAboutDialog() {
    	Dialog d = new Dialog(this);
		d.setContentView(R.layout.about);
		d.setTitle("About Septacular App");
		TextView view = (TextView) d.findViewById(R.id.ApplicationVersionTextView);
		view.setText(this.packageInfo.versionName + "; " + this.packageInfo.versionCode);
		d.setCancelable(true);
		d.setCanceledOnTouchOutside(true);
		d.show();
    }
    
    private void showDebugDialog() {
    	Dialog d = new Dialog(this);
    	d.setContentView(R.layout.debug);
    	d.setTitle("Debug");
    	TextView view = (TextView) d.findViewById(R.id.DebugTextView);
    	StringBuilder text = new StringBuilder();
    	text.append("last: " + new Date(prefs.getLong("lastupdatets", 0)));
    	text.append("; next: " + new Date(prefs.getLong("next_auto_update", 0)).toString());
    	text.append("; rushhour: " + prefs.getBoolean("is_rush_hour", false));
    	
		view.setText(text);
    	d.setCancelable(true);
    	d.setCanceledOnTouchOutside(true);
    	d.show();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	boolean result = super.onOptionsItemSelected(item);
    	switch (item.getItemId()) {
    		case MENU_REFRESH:
    			manualUpdateView();
    			return true;
    		case MENU_PREFS:
    			startActivity(new Intent(this, EditPreferences.class));
    			return true;
    		case MENU_ABOUT:
    			showAboutDialog();
    			return true;
    		case MENU_DEBUG:
    			showDebugDialog();
    			return true;
    	}
    	
		return result;
    }
    
    // context menu stuff
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
    		ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	menu.add(0, CONTEXT_COPYTEXT, 0, R.string.context_copytext);
    	menu.add(0, CONTEXT_COPYLINK, 1, R.string.context_copylink);
    }
    
	@Override
    public boolean onContextItemSelected(MenuItem item) {
    	boolean result = super.onContextItemSelected(item);
    	switch (item.getItemId()) {
    		case CONTEXT_COPYTEXT:
    			ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    			AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    			SQLiteCursor selectedItem = (SQLiteCursor) getListAdapter().getItem(info.position);
				clipboard.setText(selectedItem.getString(selectedItem.getColumnIndex(RssItemColumns.TITLE)));
				Toast.makeText(this, R.string.toast_copytext, Toast.LENGTH_SHORT).show();
				break;
    		case CONTEXT_COPYLINK:
    			clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    			info = (AdapterContextMenuInfo) item.getMenuInfo();
				selectedItem = (SQLiteCursor) getListAdapter().getItem(info.position);
				clipboard.setText(selectedItem.getString(selectedItem.getColumnIndex(RssItemColumns.LINK)));
				Toast.makeText(this, R.string.toast_copylink, Toast.LENGTH_SHORT).show();
    	}
    	
		return result;
    }
	
	private class DatabaseUpdatedReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(DatabaseUpdatedReceiver.class.getName(), "received intent " + intent.getAction());
			updateView();
		}
		
	}
}