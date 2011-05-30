package com.willvuong.septacular.android;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

public class UpdaterService extends Service implements OnSharedPreferenceChangeListener {

	private static final String TAG = "UpdaterService";
	
	public static final String DATABASE_UPDATED = "com.willvuong.septacular.android.DATABASE_UPDATED";

	private UpdaterAsyncTask updaterTask = null;

	private AlarmManager alarmManager = null;
	private PendingIntent alarmIntent = null;
	
	private SharedPreferences prefs = null;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		this.alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
		this.alarmIntent = PendingIntent.getBroadcast(this, 0, new Intent(AlarmReceiver.ACTION_UPDATE_DATABASE_ALARM), 0);
		
		this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
		this.prefs.registerOnSharedPreferenceChangeListener(this);
	}
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// only update the service if the notification interval pref changes
		if ("pref_interval".equals(key)) {
			Log.d(TAG, "sending update db alarm intent");
			sendBroadcast(new Intent(AlarmReceiver.ACTION_UPDATE_DATABASE_ALARM));
		}
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		// run the updater task
		if (updaterTask == null || updaterTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
			updaterTask = new UpdaterAsyncTask(getApplicationContext());
			updaterTask.execute();
		}
		
		// stop this service
		stopSelf();
	}
	
	private void postExecute(List<RssItem> items) {

		boolean do_alert = true;
		
		// is it the weekend
		int d = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
		boolean we = (d == Calendar.SATURDAY || d == Calendar.SUNDAY);
		boolean notify_weekend = prefs.getBoolean("pref_weekend", false);
		if (we) {
			do_alert = notify_weekend;
		}
		
		// items can be null
		if (items != null) {
		
			// alert the ui (if running) that the database has been updated
			sendBroadcast(new Intent(DATABASE_UPDATED));
			
			Set<String> types = new HashSet<String>();
			
			// post process items for notifications
			boolean rr = prefs.getBoolean("pref_rr", false);
			boolean mfl = prefs.getBoolean("pref_mfl", false);
			boolean bsl = prefs.getBoolean("pref_bsl", false);
			boolean trl = prefs.getBoolean("pref_trl", false);
			boolean nhsl = prefs.getBoolean("pref_nhsl", false);
			boolean bus = prefs.getBoolean("pref_bus", false);
			String watchlist = prefs.getString("pref_watchlist", null);
			
			for (RssItem item : items) {
				if (!item.isOld()) {
					// check for specific routes
					String title = item.getTitle();
					
					if (rr && title.startsWith("RRD")) {
						types.add("RRD");
					}
					
					if (mfl && title.startsWith("MFL")) {
						types.add("MFL");
					}
					
					if (bsl && title.startsWith("BSL")) {
						types.add("BSL");
					}
					
					if (trl && title.startsWith("TRL")) {
						types.add("TRL");
					}
					
					if (nhsl && title.startsWith("NHSL")) {
						types.add("NHSL");
					}
	
					if (bus && title.startsWith("BUS")) {
						types.add("BUS");
					}
					
					// check for watch list
					if (watchlist != null && watchlist.length() != 0) {
						String[] split = watchlist.split("\\s"); // split on whitespace
						for (String string : split) {
							if (title.indexOf(string) != -1) {
								types.add(string);
							}
						}
					}
				}
			}
	
			if (!types.isEmpty() && do_alert) {

				Notification notification = new Notification(R.drawable.stat_sys_warning, "Septacular", System.currentTimeMillis());
				PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(this, FeedActivity.class), 0);
				String string = TextUtils.join(", ", types);
				notification.setLatestEventInfo(this, "Septacular Alerts", string, intent);
				notification.tickerText = string;
				notification.flags |= Notification.FLAG_AUTO_CANCEL;
				notification.when = System.currentTimeMillis();
				
				NotificationUtils.configureNotificationFromPrefs(notification, prefs);
				
				NotificationManager	notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
				notificationManager.notify(1, notification);
			}
		}
		
		// schedule next update
		// get prefs
		boolean auto = this.prefs.getBoolean("pref_automatic_updates", false);
		int interval = Utils.toInt(this.prefs.getString("pref_interval", null), 15);
		int rush_interval = Utils.toInt(this.prefs.getString("pref_rushhour_interval", null), 15);
		int target_interval = interval;
		boolean rush_we = this.prefs.getBoolean("pref_rushhour_weekends", false);
		
		// is this rush hour
		int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		boolean rh = (h >= 7 && h <= 9) || (h >= 16 && h <= 18);
		
		long time = System.currentTimeMillis() + (interval * 60 * 1000);
		String msg = "setting alarm interval for " + time + " in " + interval;
		
		if (auto) {
			// if its rush hour
			if (rh) {
				// if its the weekend
				if (we) {
					if (rush_we) { // and weekend rush hour is enabled
						msg = "setting alarm interval for " + time + " in " + rush_interval + " (we rush hour)";
						time = System.currentTimeMillis() + (rush_interval * 60 * 1000);
						target_interval = rush_interval;
					}
				}
				// if its not the weekend
				else {
					msg = "setting alarm interval for " + time + " in " + rush_interval + " (rush hour)";
					time = System.currentTimeMillis() + (rush_interval * 60 * 1000);
					target_interval = rush_interval;
				}
			}
			
			Log.d(TAG, msg);
			
			boolean inexact = this.prefs.getBoolean("pref_inexact_interval", true);
			if (inexact) {
				this.alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, 
						time, target_interval * 60 * 1000, alarmIntent);
			}
			else {
				this.alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, 
						time, target_interval * 60 * 1000, alarmIntent);
			}
			
			Editor editor = this.prefs.edit();
			editor.putLong("next_auto_update", time);
			editor.putBoolean("is_rush_hour", rh);
			editor.commit();
		}
		else {
			this.alarmManager.cancel(alarmIntent);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	// params, progress, result
	private class UpdaterAsyncTask extends AsyncTask<Void, Void, List<RssItem>> {
		private final static String TAG = "Updater";
		
		private RssHandler rssHandler;
		private SharedPreferences prefs;

		private int timeoutConnection = 3000;

		private int timeoutSocket = 5000;
		
		public UpdaterAsyncTask(Context context) {
			super();
			rssHandler = new RssHandler(context);
			prefs = PreferenceManager.getDefaultSharedPreferences(context);
		}

		public boolean isOnline() {
			ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo info = cm.getActiveNetworkInfo();
			return info != null && info.isConnected() && info.isAvailable();
		}
		
		@Override
		protected List<RssItem> doInBackground(Void... params) {
			if (isOnline()) {
				HttpParams httpParameters = new BasicHttpParams();
				// HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
				// HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
				
				DefaultHttpClient client = new DefaultHttpClient(httpParameters);
				
				try {
					Log.d(TAG, "run() starting");
					
					HttpGet get = new HttpGet(Constants.RSS_URL);
					// pre update timestamp
					long ts = this.prefs.getLong("lastupdatets", 0);
					rssHandler.setEpoch(ts);
					
					ResponseHandler<String> handler = new BasicResponseHandler();
					String xml = client.execute(get, handler);
					
					// parse results for items
					Xml.parse(xml, rssHandler);
					List<RssItem> items = rssHandler.getItems();
					
					// log the update time so we can tell what is new
					Editor editor = this.prefs.edit();
					editor.putLong("lastupdatets", System.currentTimeMillis());
					editor.commit();
					
					Log.d(TAG, "run() finished");
					
					return items;
				}
				catch (HttpResponseException e) {
					Log.w(TAG, "http response exception", e);
				}
				catch (Exception e) {
					Log.e(TAG, e.getMessage(), e);
	
					PrintWriter pw;
					try {
						pw = new PrintWriter(new FileWriter(Environment.getExternalStorageDirectory() + 
								"/com.septacular/septacular.log", true));
						pw.println(new Date());
						e.printStackTrace(pw);
						pw.flush();
						pw.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				} 
				finally {
					// release resources
					client.getConnectionManager().shutdown();
				}
			}
			
			Log.d(TAG, "run() finished with noop; thinks we are offline");
			
			return null;
		}
		
		@Override
		protected void onPostExecute(List<RssItem> result) {
			super.onPostExecute(result);
			postExecute(result);
		}
	}
}
