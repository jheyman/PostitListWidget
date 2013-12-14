package com.gbbtbb.postitlistwidget;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.Button;

public class CleanListMenuActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.cleanlist_popup_menu);
		
		// Attach a pending broadcast intent to the Confirm button, that will get sent to PostitListWidgetProvider
		Button buttonConfirm = (Button) findViewById(R.id.button_confirm);
		buttonConfirm.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				final Intent cleanListIntent = new Intent(CleanListMenuActivity.this, PostitListWidgetProvider.class);
				cleanListIntent.setAction(PostitListWidgetProvider.CLEANLIST_ACTION);
				final PendingIntent cleanListPendingIntent = PendingIntent.getBroadcast(CleanListMenuActivity.this, 0, cleanListIntent, PendingIntent.FLAG_UPDATE_CURRENT);

				try {
					Log.i(PostitListWidgetProvider.TAG, "CleanListMenuActivity: launching cleanlist pending Intent");    		
					cleanListPendingIntent.send();
				} 
				catch (CanceledException ce) {
					Log.i(PostitListWidgetProvider.TAG, "CleanListMenuActivity: cleanListPendingIntent.send Exception: "+ce.toString());
				}

				// close this GUI activity
				finish();
			}
		});
		
		Button buttonCancel = (Button) findViewById(R.id.button_cancel);
		buttonCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Log.i(PostitListWidgetProvider.TAG, "CleanListMenuActivity: canceled delete list");    		
				// Close this GUI activity
				finish();
			}
		});			
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.popup_menu, menu);
		return true;
	}
}
