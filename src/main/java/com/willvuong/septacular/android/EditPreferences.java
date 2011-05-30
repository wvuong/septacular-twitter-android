package com.willvuong.septacular.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;

public class EditPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	private ListPreference interval;
	private ListPreference rh_interval;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// configure prefs screen
		addPreferencesFromResource(R.xml.prefs);
		
		// retrieve reference
		interval = (ListPreference) getPreferenceScreen().findPreference("pref_interval");
		rh_interval = (ListPreference) getPreferenceScreen().findPreference("pref_rushhour_interval");
		updateIntervalPreference(interval);
		updateIntervalPreference(rh_interval);
		
		// attach this change handler
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		
		// attach listener
		Preference pref = getPreferenceScreen().findPreference("pref_test_notify");
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference prefs) {
				Notification notification = new Notification(R.drawable.stat_sys_warning, "Septacular", System.currentTimeMillis());
				notification.setLatestEventInfo(getApplicationContext(), "Septacular Alerts", "Test Notification", 
						PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), 0));
				notification.tickerText = "Test Notification";
				notification.flags |= Notification.FLAG_AUTO_CANCEL;
				notification.when = System.currentTimeMillis();
				
				NotificationUtils.configureNotificationFromPrefs(notification, getPreferenceScreen().getSharedPreferences());
				
				NotificationManager	notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
				notificationManager.notify(2, notification);
				
				return false;
			}
		});
	}

	private void updateIntervalPreference(ListPreference intervalPref) {
		int value = Utils.toInt(intervalPref.getValue(), -1);
		if (value == -1) {
			intervalPref.setSummary("Automatic updates disabled");
		}
		else {
			intervalPref.setSummary("Update every " + value + " minutes");
		}
	}
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if ("pref_interval".equals(key) || "pref_rushhour_interval".equals(key)) {
			updateIntervalPreference(interval);
			updateIntervalPreference(rh_interval);
		}
	}
}
