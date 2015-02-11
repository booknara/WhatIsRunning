package com.booknara.whatisrunning;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.booknara.whatisrunning.models.PackageHistory;
import com.booknara.whatisrunning.utils.ShareUtils;

import org.w3c.dom.Text;

import io.realm.Realm;
import io.realm.RealmResults;

public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getSimpleName();
	private final static int RUNNING_TIME_SEC = 60 * 3; // 3 mins

	// UI Component
	private TextView historyView;
	private MenuItem startMenuItem;
	private MenuItem clearMenuItem;

	private Realm realm;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		realm = Realm.getInstance(this);
		historyView = (TextView) findViewById(R.id.status_text);
		historyView.setMovementMethod(new ScrollingMovementMethod());

		historyView.setText(R.string.click_start);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		realm.close(); // Remember to close Realm when done.
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		// This does work
		startMenuItem = menu.findItem(R.id.action_start);
		clearMenuItem = menu.findItem(R.id.action_clear);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
			case R.id.action_view:
				historyView.setText("");
				displayRunningAppInfo();
				break;
			case R.id.action_clear:
				historyView.setText("");
				clearRunningAppInfo();
				break;
			case R.id.action_start:
				new ExecutePackageTask(ctx()).execute(ctx());
				break;
			case R.id.action_share:
				String body = historyView.getText().toString();
				if (TextUtils.isEmpty(body))
					Toast.makeText(this, "No contents", Toast.LENGTH_LONG).show();
				else
					ShareUtils.share(this, body);
				break;
		}

		return true;
	}

	private void clearRunningAppInfo() {
		realm.beginTransaction();
		RealmResults<PackageHistory> histories = realm.where(PackageHistory.class).findAll();
		histories.clear();
		realm.commitTransaction();
	}

	private void displayRunningAppInfo() {
		RealmResults<PackageHistory> histories = realm.where(PackageHistory.class).findAll();
		for(PackageHistory p: histories) {
			historyView.append(p.getDate() + ", " + p.getPackageName() + ", " + p.getAppName() + " \n");
		}
	}


	public class ExecutePackageTask extends AsyncTask<Context, PackageHistory, Boolean> {
		private final String TAG = ExecutePackageTask.class.getSimpleName();

		private Context context;

		public ExecutePackageTask(Context context) {
			this.context = context;
		}

		@Override
		protected void onPreExecute() {
//			historyView.setText(getString(R.string.app_manual, RUNNING_TIME_SEC));
//			startBtn.setEnabled(false);
			startMenuItem.setEnabled(false);
			clearMenuItem.setEnabled(false);
		}

		@Override
		protected void onProgressUpdate(PackageHistory... history) {
			super.onProgressUpdate(history[0]);
			insertPacakgeHistory(history[0].getDate(), history[0].getPackageName(), history[0].getAppName());
		}

		@Override
		protected Boolean doInBackground(Context... params) {
			final Context context = params[0].getApplicationContext();
			for (int i = 0; i < RUNNING_TIME_SEC; i++) {
				PackageHistory history;
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					history = getAppOnForegroundProcess(context);
				} else {
					history = getAppOnForeground(context);
				}

				try {
					// 2 sec timesleep
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				publishProgress(history);
			}

			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			try {
				if (!result) {
					Toast.makeText(ctx(), R.string.err_unexpected_error, Toast.LENGTH_LONG).show();
					Log.e(TAG, "Execution Error");
				} else {
					Toast.makeText(ctx(), R.string.finished, Toast.LENGTH_LONG).show();
					Log.i(TAG, "Execution Success");
				}

				startMenuItem.setEnabled(true);
				clearMenuItem.setEnabled(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private PackageHistory getAppOnForeground(Context context) {
			ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
			List<RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
			if (appProcesses == null) {
				return null;
			}

			ComponentName topActivity;

			List<ActivityManager.RunningTaskInfo> runningTasks = activityManager.getRunningTasks(10);
			ActivityManager.RunningTaskInfo currentTask;

			if (runningTasks == null || runningTasks.size() <= 0)
				return null;

			currentTask = runningTasks.get(0);
			topActivity = currentTask.topActivity;

			if (topActivity == null)
				return null;

			String currentTime = getCurrentTime();
			String packageName = topActivity.getPackageName();
			String appName = getAppName(packageName);
			Log.i(TAG, "Running package name : " + packageName + ", app name : " + appName + "Top Activity class name : " + topActivity.getClassName());

			return new PackageHistory(currentTime, packageName, appName);
		}

		private PackageHistory getAppOnForegroundProcess(Context context) {
			ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		    List list;
		    int i1 = 0;

		    try {
		        list = activityManager.getRunningAppProcesses();
		    } catch (Exception exception) {
		        return null;
		    }

		    if (((RunningAppProcessInfo)list.get(i1)).pkgList.length != 1) {
		        return null;
		    }

			String currentTime = getCurrentTime();
		    String packageName = ((android.app.ActivityManager.RunningAppProcessInfo)list.get(i1)).pkgList[0];
			String appName = getAppName(packageName);
			Log.i(TAG, "Running package name : " + packageName + ", app name : " + appName);

			return new PackageHistory(currentTime, packageName, appName);
		}

		private String getAppName(String packageName) {
			final PackageManager pm = getApplicationContext().getPackageManager();
			ApplicationInfo ai;
			try {
				ai = pm.getApplicationInfo(packageName, 0);
			} catch (NameNotFoundException e) {
				ai = null;
			}

			return (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");
		}

		private void insertPacakgeHistory(String date, String packageName, String appName) {
			realm.beginTransaction();
			PackageHistory history = realm.createObject(PackageHistory.class);
			history.setDate(date);
			history.setPackageName(packageName);
			history.setAppName(appName);
			realm.commitTransaction();
		}

		private String getCurrentTime() {
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			Date date = new Date();

			return dateFormat.format(date);
		}

	}

	public Context ctx() {
		return this;
	}
}