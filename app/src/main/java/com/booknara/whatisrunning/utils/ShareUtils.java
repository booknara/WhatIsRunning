package com.booknara.whatisrunning.utils;

import android.content.Context;
import android.content.Intent;

import com.booknara.whatisrunning.R;


public class ShareUtils {

	public static void share(Context context, String body){
		Intent i = new Intent(Intent.ACTION_SEND);

		i.setType("text/plain");
		i.putExtra(Intent.EXTRA_EMAIL, new String[]{""});
		i.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_email_title));
		i.putExtra(Intent.EXTRA_TEXT, body);

		context.startActivity(Intent.createChooser(i, context.getString(R.string.send_email_title)));
	}

}
