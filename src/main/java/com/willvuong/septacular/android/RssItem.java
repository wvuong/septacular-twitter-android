package com.willvuong.septacular.android;

import java.util.Date;

public class RssItem {
	private long id;
	private String title;
	private String link;
	private Date published;
	private boolean old = false;
	
	public RssItem() {
	}

	public void setId(long id) {
		this.id = id;
	}
	
	public long getId() {
		return id;
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public Date getPublished() {
		return published;
	}

	public void setPublished(Date published) {
		this.published = published;
	}
	
	public boolean isOld() {
		return old;
	}
	
	public void setOld(boolean old) {
		this.old = old;
	}
	
	@Override
	public String toString() {
		return this.title + "(" + this.title.length() + ")";
	}
}
