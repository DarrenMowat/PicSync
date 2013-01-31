package com.darrenmowat.gdcu.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.darrenmowat.gdcu.GDCU;
import com.darrenmowat.gdcu.Keys;
import com.darrenmowat.gdcu.otto.DataBus;
import com.flurry.android.FlurryAgent;

public class WebviewActivity extends SherlockActivity {

	private WebView webview;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String title = getIntent().getStringExtra("title");
		String url = getIntent().getStringExtra("url");

		setTitle(title);

		webview = new WebView(this);
		setContentView(webview);
		getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);

		webview.getSettings().setJavaScriptEnabled(false);

		webview.setWebViewClient(new WebViewClient() {

			@Override
			public void onReceivedError(WebView view, int errorCode, String description,
					String failingUrl) {
				final Context context = WebviewActivity.this;
				Toast.makeText(context, "Couldn't load page! " + description, Toast.LENGTH_SHORT)
						.show();
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				final Context context = WebviewActivity.this;
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				context.startActivity(browserIntent);
				return true;
			}
		});

		webview.loadUrl(url);

	}

	@Override
	public void onStart() {
		super.onStart();
		DataBus.getInstance().register(this);
		if (!GDCU.DEVEL_BUILD) {
			FlurryAgent.onStartSession(this, Keys.FLURRY_KEY);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		DataBus.getInstance().unregister(this);
		if (!GDCU.DEVEL_BUILD) {
			FlurryAgent.onEndSession(this);
		}
	}

}
