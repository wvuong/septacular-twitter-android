package com.willvuong.septacular.android;

import android.app.Notification;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.provider.Settings;

public class NotificationUtils {

	public static void configureNotificationFromPrefs(Notification notification, SharedPreferences prefs) {
		if (prefs.getBoolean("pref_vibrate", false)) {
			notification.defaults |= Notification.DEFAULT_VIBRATE;
			notification.vibrate = new long[] {0, 100, 200, 300};
		}
		
		if (prefs.getString("pref_ringtone", null) != null) {
			Uri uri = Uri.parse(prefs.getString("pref_ringtone", null));
			if (uri == null) {
				uri = Settings.System.DEFAULT_NOTIFICATION_URI;
			}
			notification.sound = uri;
		}
		
		if (prefs.getBoolean("pref_lights", false)) {
			notification.flags |= Notification.FLAG_SHOW_LIGHTS;
			notification.ledARGB = prefs.getInt("pref_lights_color", Color.WHITE);
			notification.ledOnMS = 300;
			notification.ledOffMS = 1000;
		}
	}
}
