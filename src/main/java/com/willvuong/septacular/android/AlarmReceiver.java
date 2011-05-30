package com.willvuong.septacular.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {

	public static final String ACTION_UPDATE_DATABASE_ALARM = "com.willvuong.septacular.android.ACTION_UPDATE_DATABASE_ALARM";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Intent start = new Intent(context, UpdaterService.class);
		context.startService(start);
	}

}
