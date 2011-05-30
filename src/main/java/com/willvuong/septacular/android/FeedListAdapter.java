package com.willvuong.septacular.android;

import java.text.DateFormat;

import com.willvuong.septacular.android.DatabaseHelper.RssItemColumns;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.TextView.BufferType;

public class FeedListAdapter extends SimpleCursorAdapter {
	private int layout;

	public FeedListAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
		super(context, layout, c, from, to);
		
		this.layout = layout;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater inflator = LayoutInflater.from(context);
		return inflator.inflate(layout, parent, false);
	}
	
	@Override
	public void bindView(View view, Context context, Cursor cursor) {

        // Bind the data efficiently with the holder.

		TextView text1 = (TextView) view.findViewById(R.id.text1);
		TextView text2 = (TextView) view.findViewById(R.id.text2);
		
		String title = cursor.getString(cursor.getColumnIndex(RssItemColumns.TITLE));
		
		Long published = cursor.getLong(cursor.getColumnIndex(RssItemColumns.PUBLISHED));
		
		text1.setText(title, BufferType.SPANNABLE);
		Linkify.addLinks(text1, Linkify.ALL);
		
		Spannable span = (Spannable) text1.getText();
		if (title.startsWith("MFL")) {
			span.setSpan(new StyleSpan(Typeface.BOLD), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			span.setSpan(new ForegroundColorSpan(Color.BLUE), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		else if (title.startsWith("TRL")) {
			span.setSpan(new StyleSpan(Typeface.BOLD), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			span.setSpan(new ForegroundColorSpan(Color.GREEN), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		else if (title.startsWith("BSL")) {
			span.setSpan(new StyleSpan(Typeface.BOLD), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			span.setSpan(new ForegroundColorSpan(Color.rgb(255, 140, 0)), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		else if (title.startsWith("RRD")) {
			span.setSpan(new StyleSpan(Typeface.BOLD), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			span.setSpan(new ForegroundColorSpan(Color.MAGENTA), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		else if (title.startsWith("NHSL")) {
			span.setSpan(new StyleSpan(Typeface.BOLD), 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			span.setSpan(new ForegroundColorSpan(Color.YELLOW), 0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		else if (title.startsWith("BUS")) {
			span.setSpan(new StyleSpan(Typeface.BOLD), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			span.setSpan(new ForegroundColorSpan(Color.WHITE), 0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
		
		boolean old = cursor.getInt(cursor.getColumnIndex(RssItemColumns.OLD)) == 1;
		if (!old) {
			view.setBackgroundColor(Color.DKGRAY);
		}
		
        text2.setText(DateUtils.formatSameDayTime(published, System.currentTimeMillis(), DateFormat.DEFAULT, DateFormat.DEFAULT) + ", " +
        		DateUtils.getRelativeTimeSpanString(published).toString());
	}
}
