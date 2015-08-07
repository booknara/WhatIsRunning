package com.booknara.whatisrunning.logic;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author : Daehee Han(@daniel_booknara)
 */
public class Android5RunningAppsHandler implements IRunningAppsHandler {
    private static final String TAG = Android5RunningAppsHandler.class.getSimpleName();

    private final ActivityManager activityManager;

    private volatile Class cachedProcessInfoClass = null;
    private volatile Field cachedProcessStateField = null;
    private final int PROCESS_STATE_TOP;

    public Android5RunningAppsHandler(Context context) {

        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        try {
            Class activityManagerClass = Class.forName(activityManager.getClass().getName());
            PROCESS_STATE_TOP = activityManagerClass.getField("PROCESS_STATE_TOP").getInt(activityManager);
        } catch (Throwable t) {
            Log.e(TAG, t.getMessage());
            throw new RuntimeException(t);
        }
    }

    @Override
    public ComponentName getRunningApplication() {

        List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfo = activityManager.getRunningAppProcesses();

        if (runningAppProcessInfo != null) {

            // if not isInCall do regular running app handling
            for (ActivityManager.RunningAppProcessInfo processInfo : runningAppProcessInfo) {
                try {
                    int processState = getProcessStateFieldValue(processInfo);

                    if (processState == PROCESS_STATE_TOP) {

                        if (!TextUtils.isEmpty(processInfo.processName)) {
                            String processName = processInfo.processName.split(":")[0];

                            for (String pkgName : processInfo.pkgList) {
                                if (processName.equalsIgnoreCase(pkgName)) {
                                    return new ComponentName(pkgName, pkgName);
                                }
                            }
                        }
                    }

                } catch (Throwable t) {
                    Log.e(TAG, t.getMessage());
                }
            }
        }

        return null;
    }

    private int getProcessStateFieldValue(ActivityManager.RunningAppProcessInfo processInfo) {

        try {
            if(processInfo.getClass().equals(cachedProcessInfoClass))
                return cachedProcessStateField.getInt(processInfo);

            cachedProcessInfoClass = Class.forName(processInfo.getClass().getName());
            cachedProcessStateField = cachedProcessInfoClass.getField("processState");

            return cachedProcessStateField.getInt(processInfo);

        } catch (Throwable t) {
            Log.e(TAG, t.getMessage());
        }

        return -1;
    }
}
