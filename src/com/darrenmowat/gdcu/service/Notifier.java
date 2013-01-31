package com.darrenmowat.gdcu.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;

import com.darrenmowat.gdcu.R;
import com.darrenmowat.gdcu.activity.AuthActivity;
import com.darrenmowat.gdcu.activity.SettingsActivity;

public class Notifier {

	public static void notifyAuthError(Context context, String email, Intent intent) {
		final Intent authIntent = new Intent(context, AuthActivity.class);

		authIntent.putExtra(AuthActivity.KEY_EMAIL, email);
		authIntent.putExtra(AuthActivity.KEY_INTENT, intent);

		String title = context.getString(R.string.notif_account_error_title);
		String text = String.format(context.getString(R.string.notif_account_error_text), email);

		notify(context, 4003, title, text, authIntent);
	}

	private static void notify(Context context, int id, String title, String text, Intent intent) {
		final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
				PendingIntent.FLAG_CANCEL_CURRENT);
		final Notification notification = new NotificationCompat.Builder(context)
				.setSmallIcon(R.drawable.ic_notif_icon)
				.setLargeIcon(
						BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher))
				.setAutoCancel(true).setContentTitle(title).setContentText(text)
				.setContentIntent(pendingIntent).build();
		final NotificationManager manager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(id, notification);
	}

}
