package com.booknara.whatisrunning;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.booknara.whatisrunning.R;

public class MainActivity extends Activity {
	private final static String TAG = "MainActivity";
	private final static int RUNNING_TIME_SEC = 60 * 5; // 5 mins

	// UI Component
	private TextView statusView;
	private Button startBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		statusView = (TextView) findViewById(R.id.status_text);
		startBtn = (Button) findViewById(R.id.start_btn);

		statusView.setText(R.string.click_start);
		startBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			try {
				new ExecutePackageTask().execute(ctx());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			}
		});
	}

	private class ExecutePackageTask extends AsyncTask<Context, Void, Boolean> {
		private final static String TAG = "MainActivity";

		@Override
		protected void onPreExecute() {
			statusView.setText(getString(R.string.app_manual, RUNNING_TIME_SEC));
			startBtn.setEnabled(false);
		}

		@Override
		protected Boolean doInBackground(Context... params) {
			final Context context = params[0].getApplicationContext();
			for (int i = 0; i < RUNNING_TIME_SEC; i++) {
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					getAppOnForegroundProcess(context);
				} else {
					getAppOnForeground(context);					
				}

				try {
					// 1 sec timesleep
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			try {
				if (!result) {
					statusView.setText(R.string.err_unexpected_error);
					Log.e(TAG, "Execution Error");
				} else {
					statusView.setText(R.string.finished);
					Log.i(TAG, "Execution Success");
				}

				startBtn.setEnabled(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private boolean getAppOnForeground(Context context) {
			ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
			List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
			if (appProcesses == null) {
				return false;
			}

			ComponentName topActivity;

			List<ActivityManager.RunningTaskInfo> runningTasks = activityManager.getRunningTasks(10);
			ActivityManager.RunningTaskInfo currentTask;

			if (runningTasks != null && runningTasks.size() > 0) {
				currentTask = runningTasks.get(0);
				topActivity = currentTask.topActivity;

				if (topActivity == null)
					return false;

				String packageName = topActivity.getPackageName();
				final PackageManager pm = getApplicationContext().getPackageManager();
				ApplicationInfo ai;
				try {
					ai = pm.getApplicationInfo(packageName, 0);
				} catch (NameNotFoundException e) {
					ai = null;
				}

				String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");
				Log.i(TAG, "Running package name : " + packageName + ", app name : " + applicationName + "Top Activity class name : " + topActivity.getClassName());
			}

			return true;
		}

		private boolean getAppOnForegroundProcess(Context context) {
			ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		    List list;
		    int i1 = 0;

		    try {
		        list = activityManager.getRunningAppProcesses();
		    } catch (Exception exception) {
		        return false;
		    }

		    if (((RunningAppProcessInfo)list.get(i1)).pkgList.length != 1) {
		        return false;
		    }

		    String packageName = ((android.app.ActivityManager.RunningAppProcessInfo)list.get(i1)).pkgList[0];
		    Log.d(TAG, "Running package Name : " + packageName);
		    
			return true;
		}
	}

	public Context ctx() {
		return this;
	}
}