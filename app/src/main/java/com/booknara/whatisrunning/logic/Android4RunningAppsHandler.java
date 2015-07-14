package com.booknara.whatisrunning.logic;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;

import java.util.List;

/**
 * @author : Daehee Han(@daniel_booknara)
 */
public class Android4RunningAppsHandler implements RunningAppsHandler {
    private static final String TAG = Android4RunningAppsHandler.class.getSimpleName();

    private final ActivityManager activityManager;

    public Android4RunningAppsHandler(Context context) {
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    @Override
    public ComponentName getRunningApplication() {

        ComponentName result = null;

        //noinspection deprecation
        List<ActivityManager.RunningTaskInfo> runningTasks = activityManager.getRunningTasks(10);
        ActivityManager.RunningTaskInfo currentTask;

        if (runningTasks != null && runningTasks.size() > 0) {
            currentTask = runningTasks.get(0);

            result = currentTask.topActivity;
        }

        return result;
    }
}
