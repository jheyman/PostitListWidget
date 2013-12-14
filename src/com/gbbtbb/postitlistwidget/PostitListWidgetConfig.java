package com.gbbtbb.postitlistwidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class PostitListWidgetConfig extends Activity {

	Button configOkButton;
	EditText ipAddressField;
	int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    public static final String POSTIT_PREFS  = "com.gbbtbb.postitlistwidget.PostitListWidgetProvider";
    public static final String POSTIT_PREF_IPADDRESS = "postitlist_";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		setResult(RESULT_CANCELED);

		setContentView(R.layout.config);

		// Find the EditText
        ipAddressField = (EditText)findViewById(R.id.editText);
        
		configOkButton = (Button)findViewById(R.id.okconfig);
		configOkButton.setOnClickListener(configOkButtonOnClickListener);

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
			mAppWidgetId = extras.getInt(
					AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}

		// If they gave us an intent without the widget id, just bail.
		if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
		}
		
		ipAddressField.setText(loadTitlePref(PostitListWidgetConfig.this));
	}

	private Button.OnClickListener configOkButtonOnClickListener = new Button.OnClickListener(){

		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub

			final Context context = PostitListWidgetConfig.this;
			
			 // Save the url in shared prefs and return OK.
            String ipAddress = ipAddressField.getText().toString();
            saveTitlePref(context, mAppWidgetId, ipAddress);
            
			Toast.makeText(context, "PostitListWidgetConfig: saved IP=" + ipAddress , Toast.LENGTH_LONG).show();
			
			Log.i(PostitListWidgetProvider.TAG,"PostitListWidgetConfig: saved IP=" + ipAddress);
			
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

			PostitListWidgetProvider.updateAppWidget(context, appWidgetManager, mAppWidgetId);



			Intent resultValue = new Intent();
			resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
			setResult(RESULT_OK, resultValue);
			finish();
		}
	};
	
    // Write the prefix to the SharedPreferences object for this widget
    private void saveTitlePref(Context context, int appWidgetId, String text) {
        
      SharedPreferences sharedPreferences = context.getSharedPreferences(POSTIT_PREFS, Context.MODE_PRIVATE);
    		  //getPreferences(MODE_PRIVATE);
      SharedPreferences.Editor editor = sharedPreferences.edit();
      editor.putString(POSTIT_PREF_IPADDRESS, text);
      boolean result = editor.commit();
      
      if(result)
    	  Log.i(PostitListWidgetProvider.TAG, "successfully commited prefs ("+ text+")");
    }

    // Read the prefix from the SharedPreferences object for this widget.
    private String loadTitlePref(Context context) {
        
		SharedPreferences sharedPreferences = context.getSharedPreferences(POSTIT_PREFS, Context.MODE_PRIVATE);
        String ip = sharedPreferences.getString(POSTIT_PREF_IPADDRESS , null);
        if (ip != null) {
            return ip;
        } else {
        	return "127.0.0.1";
        }
        
    }

}