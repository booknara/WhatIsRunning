package com.booknara.whatisrunning;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.booknara.whatisrunning.logic.Android4RunningAppsHandler;
import com.booknara.whatisrunning.logic.Android5RunningAppsHandler;
import com.booknara.whatisrunning.logic.AndroidMRunningAppsHandler;
import com.booknara.whatisrunning.logic.RunningAppsHandler;
import com.booknara.whatisrunning.models.PackageHistory;
import com.booknara.whatisrunning.utils.ShareUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmResults;

public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getSimpleName();

	private final static int RUNNING_TIME_SEC = 60 * 3; // 3 minutes
    private final static int CHECKING_INTERVAL_SEC = 1; // ? seconds

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
				new ExecutePackageTask(this).execute(this);
				break;
			case R.id.action_share:
				String body = historyView.getText().toString();
				if (TextUtils.isEmpty(body))
					Toast.makeText(this, R.string.no_history, Toast.LENGTH_LONG).show();
				else
					ShareUtils.share(this, body);
				break;
            case R.id.action_usage_access:
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                startActivity(intent);
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

		private final Context mContext;
        private final RunningAppsHandler runningAppsHandler;

		public ExecutePackageTask(Context context) {
			this.mContext = context;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                runningAppsHandler = new AndroidMRunningAppsHandler(context);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                runningAppsHandler = new Android5RunningAppsHandler(context);
            } else {
                runningAppsHandler = new Android4RunningAppsHandler(context);
            }
		}

		@Override
		protected void onPreExecute() {
			startMenuItem.setEnabled(false);
			clearMenuItem.setEnabled(false);
		}

		@Override
		protected Boolean doInBackground(Context... params) {
			for (int i = 0; i < RUNNING_TIME_SEC; i++) {
				PackageHistory history = getForegroundAppPackage();

				try {
					// 2 seconds time sleep
					Thread.sleep(CHECKING_INTERVAL_SEC * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

                if (history == null)
                    continue;

				publishProgress(history);
			}

			return true;
		}

        @Override
        protected void onProgressUpdate(PackageHistory... history) {
            if (history == null)
                return;

            super.onProgressUpdate(history[0]);
            insertPackageHistory(history[0].getDate(), history[0].getPackageName(), history[0].getAppName());
        }

        @Override
		protected void onPostExecute(Boolean result) {
			try {
				if (!result) {
					Toast.makeText(mContext, R.string.err_unexpected_error, Toast.LENGTH_LONG).show();
					Log.e(TAG, "Execution Error");
				} else {
					Toast.makeText(mContext, R.string.finished, Toast.LENGTH_LONG).show();
					Log.i(TAG, "Execution Success");
				}

				startMenuItem.setEnabled(true);
				clearMenuItem.setEnabled(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

        private PackageHistory getForegroundAppPackage() {
            ComponentName cn = runningAppsHandler.getRunningApplication();

            if (cn == null)
                return null;

            Log.i(TAG, cn.getPackageName());

            String currentTime = getCurrentTime();
            String packageName = cn.getPackageName();
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

		private void insertPackageHistory(String date, String packageName, String appName) {
			realm.beginTransaction();
			PackageHistory history = realm.createObject(PackageHistory.class);
			history.setDate(date);
			history.setPackageName(packageName);
            history.setAppName(appName);
			realm.commitTransaction();
		}

		private String getCurrentTime() {
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
			Date date = new Date();

			return dateFormat.format(date);
		}
	}
}