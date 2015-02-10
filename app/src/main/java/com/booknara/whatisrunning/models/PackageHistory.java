package com.booknara.whatisrunning.models;

import io.realm.RealmObject;

public class PackageHistory extends RealmObject {
	private String date;
    private String packageName;
	private String appName;

	public PackageHistory() {

	}

	public PackageHistory(String date, String packageName, String appName) {
		this.date = date;
		this.packageName = packageName;
		this.appName = appName;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}
    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}
}
