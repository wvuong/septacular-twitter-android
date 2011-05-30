package com.willvuong.septacular.android;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;

public class RssHandler extends DefaultHandler {
	// Sat, 27 Mar 2010 18:44:38 +0000
	private static final SimpleDateFormat dateParser = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
	
	private DatabaseHelper helper = null;
	private StringBuilder builder = null;
	private List<RssItem> items = null;
	private RssItem currentItem = null;
	
	private long epoch;
	
	
	public RssHandler(Context context) {
		this.helper = new DatabaseHelper(context);
		
	}
	
	public void setEpoch(long epoch) {
		this.epoch = epoch;
	}
	
	public List<RssItem> getItems() {
		return items;
	}
	
	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		items = new ArrayList<RssItem>();
		builder = new StringBuilder();
	}
	
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);
		if (localName.equals("item")) {
			this.currentItem = new RssItem();
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		super.characters(ch, start, length);
		builder.append(ch, start, length);
	}
	
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		super.endElement(uri, localName, qName);
		if (this.currentItem != null) {
			if (localName.equals("title")) {
				this.currentItem.setTitle(builder.toString().trim().substring(7));
			}
			else if (localName.equals("link")) {
				String url = builder.toString().trim();
				this.currentItem.setLink(url);
				
				String[] split = url.split("/");
				this.currentItem.setId(Long.valueOf(split[split.length-1]));
			}
			else if (localName.equals("pubDate")) {
				try {
					Date date = dateParser.parse(builder.toString().trim());
					this.currentItem.setPublished(date);
					this.currentItem.setOld(date.getTime() <= epoch);
				} catch (ParseException e) {
				}
			}
			else if (localName.equals("item")) {
				this.items.add(this.currentItem);
			}
		}
		builder.setLength(0);
	}
	
	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		
		helper.open();
		helper.refreshFeed(this.items);
		helper.close();
	}
}
