package com.booknara.whatisrunning.models;

/**
 * @author : Daehee Han(@daniel_booknara)
 */
import android.app.usage.UsageEvents;

public class AppUsageEvent {

    private String className;
    private String pkgName;
    private int type;
    private long timestamp;

    public AppUsageEvent(String pkgName, String className, int type, long timestamp) {
        super();
        this.className = className;
        this.pkgName = pkgName;
        this.type = type;
        this.timestamp = timestamp;
    }

    public String getClassName() {
        return className;
    }
    public void setClassName(String className) {
        this.className = className;
    }
    public String getPkgName() {
        return pkgName;
    }
    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }
    public int getType() {
        return type;
    }
    public void setType(int type) {
        this.type = type;
    }
    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    protected String getTypeStr(int eventType) {
        switch (eventType) {
            case UsageEvents.Event.NONE:
                return "none";
            case UsageEvents.Event.MOVE_TO_FOREGROUND:
                return "to foreground";
            case UsageEvents.Event.MOVE_TO_BACKGROUND:
                return "to background";
            case UsageEvents.Event.CONFIGURATION_CHANGE:
                return "config change";
            default:
                return "unknown";
        }
    }

    /**
     * This convention comes from the Android pre-Lollipop process monitor reporting
     * that was in the Recent Tasks List. We use it here to preserve continuity for
     * experiment creators.
     *
     * @return
     */
    public String getAppIdentifier() {
        return getPkgName() + "/" + getClassName();
    }
}