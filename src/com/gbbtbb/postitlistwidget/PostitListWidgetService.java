/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gbbtbb.postitlistwidget;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.format.Time;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class PostitListWidgetService extends RemoteViewsService {
	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new PostitListRemoteViewsFactory(this.getApplicationContext(), intent);
	}
}

class PostitListRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
	private Context mContext;
	private Cursor mCursor;
	private String ipAddress;
	
	public PostitListRemoteViewsFactory(Context context, Intent intent) {
		mContext = context;
	}

	public void onCreate() {
		
		SharedPreferences sharedPreferences = mContext.getSharedPreferences(PostitListWidgetConfig.POSTIT_PREFS, Context.MODE_PRIVATE);
        String ip = sharedPreferences.getString(PostitListWidgetConfig.POSTIT_PREF_IPADDRESS , null);
        if (ip != null) {
            ipAddress = ip;
        } else {
        	ipAddress = "127.0.0.1";
        }
        
        Log.i(PostitListWidgetProvider.TAG, "PostitListRemoteViewsFactory: IP address read =" + ipAddress);
	}

	public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
	}

	public int getCount() {
		Log.i(PostitListWidgetProvider.TAG, "getCount returned " + Integer.toString(mCursor.getCount()));
		return mCursor.getCount();
		
	}

	public Bitmap drawTextOnPostIt(String textToDisplay, String creationDate, int position, int total) 
	{
	    int color  = position % 4;
	    int resource;
	    switch(color) {
	    case 0:
	    	resource = R.drawable.postit_yellow;
	    	break;
	    case 1:
	    	resource = R.drawable.postit_pink;
	    	break;
	    case 2:
	    	resource = R.drawable.postit_green;
	    	break;
	    case 3:
	    	resource = R.drawable.postit_violet;
	    	break;
	    default:
	    	resource = R.drawable.postit_yellow;
	    	break;
	    }
		Bitmap tmpBitmap = BitmapFactory.decodeResource(mContext.getResources(), resource);
	    Bitmap mutableBitmap = tmpBitmap.copy(tmpBitmap.getConfig(), true);
	    
	    Canvas myCanvas = new Canvas(mutableBitmap);
	    Typeface myfont = Typeface.createFromAsset(mContext.getAssets(),"fonts/passing_notes.ttf");
	    
	    TextPaint textPaint = new TextPaint();
	    textPaint.setTypeface(myfont);
	    textPaint.setStyle(Paint.Style.FILL);
	    textPaint.setTextSize(50);
	    textPaint.setColor(Color.BLACK);
	    textPaint.setTextAlign(Align.LEFT);
	    textPaint.setAntiAlias(true);
	    textPaint.setSubpixelText(true);

	    StaticLayout sl = new StaticLayout(textToDisplay, textPaint, ((int)90*myCanvas.getWidth())/100, Layout.Alignment.ALIGN_CENTER, 1, 1, false);

	    myCanvas.save();
	    myCanvas.translate(20, 125);
	    sl.draw(myCanvas);
	    myCanvas.restore();
	    
	    textPaint.setTextSize(30);
	    String index = String.format("%d/%d", position+1, total);
	    StaticLayout sl2 = new StaticLayout(index, textPaint, ((int)90*myCanvas.getWidth())/100, Layout.Alignment.ALIGN_NORMAL, 1, 1, false);

	    myCanvas.save();
	    myCanvas.translate(10, 45);
	    sl2.draw(myCanvas);
	    myCanvas.restore();	  
	    
	    textPaint.setTextSize(20);
	    StaticLayout sl3 = new StaticLayout(creationDate, textPaint, ((int)90*myCanvas.getWidth())/100, Layout.Alignment.ALIGN_OPPOSITE, 1, 1, false);

	    myCanvas.save();
	    myCanvas.translate(30, 55);
	    sl3.draw(myCanvas);
	    myCanvas.restore();	  	    
	     
	    return mutableBitmap;
	}
	
	public RemoteViews getViewAt(int position) {
		// position will always range from 0 to getCount() - 1.

		Log.i(PostitListWidgetProvider.TAG, "getViewAt " + Integer.toString(position));
		String item = "Unknown Item";
		String date = "Unknown date";
		if (mCursor.moveToPosition(mCursor.getCount()-1-position)) {
			final int itemColIndex = mCursor.getColumnIndex(PostitListDataProvider.Columns.ITEM);
			item = mCursor.getString(itemColIndex);
			final int dateColIndex = mCursor.getColumnIndex(PostitListDataProvider.Columns.CREATION_DATE);
			date = mCursor.getString(dateColIndex);
		}

		final String formatStr = mContext.getResources().getString(R.string.item_format_string);
		final int itemId = R.layout.widget_item;
		RemoteViews rv = new RemoteViews(mContext.getPackageName(), itemId);
		rv.setImageViewBitmap(R.id.widget_item, drawTextOnPostIt(String.format(formatStr, item), date, position, mCursor.getCount()));

		// Set the click intent so that we can handle it
        // Do not set a click handler on dummy items that are there only to make the list look good when empty
		if (item.compareTo("")!=0) {
			final Intent fillInIntent = new Intent();
			final Bundle extras = new Bundle();
			extras.putString(PostitListWidgetProvider.EXTRA_ITEM_ID, item);
			fillInIntent.putExtras(extras);
			rv.setOnClickFillInIntent(R.id.widget_item, fillInIntent);
		}

		return rv;
	}

	public RemoteViews getLoadingView() {
		// You can create a custom loading view (for instance when getViewAt() is slow.) If you
		// return null here, you will get the default loading view.
		return null;
	}

	public int getViewTypeCount() {
		return 1;
	}

	public long getItemId(int position) {
		return position;
	}

	public boolean hasStableIds() {
		return true;
	}

	public void onDataSetChanged() {
		Log.i(PostitListWidgetProvider.TAG, "onDataSetChanged called");
		// Refresh the cursor
		if (mCursor != null) {
			mCursor.close();
		}
		mCursor = mContext.getContentResolver().query(PostitListDataProvider.CONTENT_URI, null, ipAddress,null, null);

		final Intent doneIntent = new Intent(mContext, PostitListWidgetProvider.class);
		doneIntent.setAction(PostitListWidgetProvider.RELOAD_ACTION_DONE);
		final PendingIntent donePendingIntent = PendingIntent.getBroadcast(mContext, 0, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		try {
			Log.i(PostitListWidgetProvider.TAG, "onDataSetChanged: launching pending Intent for loading done");    		
			donePendingIntent.send();
		} 
		catch (CanceledException ce) {
			Log.i(PostitListWidgetProvider.TAG, "onDataSetChanged: Exception: "+ce.toString());
		}        
	}
}