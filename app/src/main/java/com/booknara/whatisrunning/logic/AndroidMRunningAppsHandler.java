package com.booknara.whatisrunning.logic;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.PowerManager;
import android.view.Display;

import com.booknara.whatisrunning.models.AppUsageEvent;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * @author : Daehee Han(@daniel_booknara)
 */
public class AndroidMRunningAppsHandler implements IRunningAppsHandler {
    private static final String TAG = AndroidMRunningAppsHandler.class.getSimpleName();

    private Context mContext;
    private final UsageStatsManager mUsageStatsManager;
    private ComponentName mLastComponent = null;

    @SuppressWarnings("ResourceType")
    public AndroidMRunningAppsHandler(Context context) {
        mContext = context;
        mUsageStatsManager = (UsageStatsManager) context.getSystemService("usagestats");
    }

    @Override
    public ComponentName getRunningApplication() {
        long currentTime = System.currentTimeMillis();
        UsageEvents events = mUsageStatsManager.queryEvents(currentTime - 2000L, currentTime);

        List<AppUsageEvent> result = convertToFriendlyEvents(events);
        AppUsageEvent lastEvent = getLastUsageEvent(result);

        if (lastEvent == null) {
            if (mLastComponent == null)
                return null;

            if (!isScreenOn(mContext))
                return null;
        } else {
            mLastComponent = new ComponentName(lastEvent.getPkgName(), lastEvent.getPkgName());
        }

        return mLastComponent;
    }

    private List<AppUsageEvent> convertToFriendlyEvents(UsageEvents ls) {
        List<AppUsageEvent> usageEventsFriendly = Lists.newArrayList();
        while (ls.hasNextEvent()) {
            android.app.usage.UsageEvents.Event eventOut = new android.app.usage.UsageEvents.Event();
            ls.getNextEvent(eventOut);
            if (eventOut.getEventType() != UsageEvents.Event.MOVE_TO_FOREGROUND)
                continue;

            usageEventsFriendly.add(new AppUsageEvent(eventOut.getPackageName(),
                    eventOut.getClassName(),
                    eventOut.getEventType(),
                    eventOut.getTimeStamp()));
        }
        return usageEventsFriendly;
    }

    private AppUsageEvent getLastUsageEvent(List<AppUsageEvent> list) {
        if (list == null || list.size() == 0)
            return null;

        long lastTimeStamp = 0L;
        AppUsageEvent lastAppUsageEvent = null;
        // Depending on the checking interval, the number of results can be one or multiple. So, get the latest timestamp AppUsageEvent.
        for (AppUsageEvent event : list) {
            if (event.getTimestamp() < lastTimeStamp)
                continue;

            lastTimeStamp = event.getTimestamp();
            lastAppUsageEvent = event;
        }

        return lastAppUsageEvent;
    }

    private boolean isScreenOn(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            boolean screenOn = false;
            for (Display display : dm.getDisplays()) {
                if (display.getState() != Display.STATE_OFF) {
                    screenOn = true;
                }
            }
            return screenOn;
        } else {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            //noinspection deprecation
            return pm.isScreenOn();
        }
    }
}
