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

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

/**
 * Our data observer just notifies an update for all widgets when it detects a change.
 */
class PostitListDataProviderObserver extends ContentObserver {
	private AppWidgetManager mAppWidgetManager;
	private ComponentName mComponentName;
	
	PostitListDataProviderObserver(AppWidgetManager mgr, ComponentName cn, Handler h) {
		super(h);
		mAppWidgetManager = mgr;
		mComponentName = cn;
	}

	@Override
	public void onChange(boolean selfChange) {
		// The data has changed, so notify the widget that the collection view needs to be updated.
		// In response, the factory's onDataSetChanged() will be called which will requery the
		// cursor for the new data.
		mAppWidgetManager.notifyAppWidgetViewDataChanged(
				mAppWidgetManager.getAppWidgetIds(mComponentName),
				R.id.stack_view);
	}
}

public class PostitListWidgetProvider extends AppWidgetProvider {

    public static final String TAG = "PostitListWidget";

	public static String CLICK_ACTION = "com.gbbtbb.popuplistwidget.CLICK";
	public static String CLEANLIST_ACTION = "com.gbbtbb.popuplistwidget.CLEANLIST";
	public static String ADDITEM_ACTION = "com.gbbtbb.popuplistwidget.ADDITEM";
	public static String RELOAD_ACTION = "com.gbbtbb.popuplistwidget.RELOAD_LIST";
	public static String RELOAD_ACTION_DONE = "com.gbbtbb.popuplistwidget.RELOAD_LIST_DONE";
	public static String DELETEITEM_ACTION = "com.gbbtbb.popuplistwidget.DELETEITEM";
	public static String EXTRA_ITEM_ID = "com.gbbtbb.popuplistwidget.item";
	
	private static PostitListDataProviderObserver sDataObserver;
	private static boolean progressBarEnabled = false;
	
	private static HandlerThread sWorkerThread;
	private static Handler sWorkerQueue;
	
	public PostitListWidgetProvider() {
		sWorkerThread = new HandlerThread("PostitListWidgetProvider-worker");
		sWorkerThread.start();
		sWorkerQueue = new Handler(sWorkerThread.getLooper());
	}
	
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    @Override
    public void onEnabled(Context context) {
    	final ContentResolver r = context.getContentResolver();
		if (sDataObserver == null) {
			final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
			final ComponentName cn = new ComponentName(context, PostitListWidgetProvider.class);
			sDataObserver = new PostitListDataProviderObserver(mgr, cn, sWorkerQueue);
			r.registerContentObserver(PostitListDataProvider.CONTENT_URI, true, sDataObserver);
			Log.i(PostitListWidgetProvider.TAG, "onEnabled: Registered data observer");
		}    	
	   
    	super.onEnabled(context);
			
        Log.i("PostitList", "onEnabled");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        
    	final String action = intent.getAction();
		final Context ctx = context;
		final Intent i = intent;
		
        if (action.equals(ADDITEM_ACTION)) {

			sWorkerQueue.removeMessages(0);
			sWorkerQueue.post(new Runnable() {
				@Override
				public void run() {
					final ContentResolver r = ctx.getContentResolver();

					// We disable the data changed observer temporarily since each of the updates
					// will trigger an onChange() in our data observer.
					r.unregisterContentObserver(sDataObserver);

					final ContentValues values = new ContentValues();
									
					String newItemName = i.getExtras().getString(ADDITEM_ACTION);
					
				    //Time today = new Time(Time.getCurrentTimezone());
				    //String creationDate = String.format("%d/%d/%d %d:%d", today.monthDay, today.month+1, today.year, today.hour, today.minute);
				    
				    Calendar c = Calendar.getInstance();
			        SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy à HH:mm");
				    String creationDate = df.format(c.getTime());
				    
					setLoadingInProgress(ctx, true);
					
					if (newItemName != null)
					{
						values.put(PostitListDataProvider.Columns.ITEM, newItemName);
						values.put(PostitListDataProvider.Columns.CREATION_DATE, creationDate);
						r.insert(PostitListDataProvider.CONTENT_URI, values);
					}
					else
						Log.i(PostitListWidgetProvider.TAG, "onReceive/addItem got null newItemName");
					
					r.registerContentObserver(PostitListDataProvider.CONTENT_URI, true, sDataObserver);

					final AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
					final ComponentName cn = new ComponentName(ctx, PostitListWidgetProvider.class);
					mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.stack_view);
				}
			});        	
        }
    	else if (action.equals(CLEANLIST_ACTION)) {

    		sWorkerQueue.removeMessages(0);
			sWorkerQueue.post(new Runnable() {
				@Override
				public void run() {
					final ContentResolver r = ctx.getContentResolver();
					String whereClause = "*";
					String[] args = new String[] { "" };
					
					setLoadingInProgress(ctx,true);
					
					// We disable the data changed observer temporarily since each of the updates
					// will trigger an onChange() in our data observer.
					r.unregisterContentObserver(sDataObserver);
					r.delete(PostitListDataProvider.CONTENT_URI, whereClause, args);
					r.registerContentObserver(PostitListDataProvider.CONTENT_URI, true, sDataObserver);

					final AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
					final ComponentName cn = new ComponentName(ctx, PostitListWidgetProvider.class);
					mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.stack_view);
				}
			});
		} else if (action.equals(CLICK_ACTION)) {
			
			final String item = intent.getStringExtra(EXTRA_ITEM_ID);

			Bundle b = new Bundle();
			b.putString(EXTRA_ITEM_ID, item); 
			intent.putExtras(b); 
			intent.setClass(ctx, DeleteItemMenuActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
			ctx.startActivity(intent);
			
		} else if (action.equals(DELETEITEM_ACTION)) {
			
			Bundle b = intent.getExtras();
			final String itemName = b.getString(PostitListWidgetProvider.EXTRA_ITEM_ID);
			
			sWorkerQueue.removeMessages(0);
			sWorkerQueue.post(new Runnable() {
				@Override
				public void run() {
					final ContentResolver r = ctx.getContentResolver();
					String whereClause = itemName;
					String[] args = new String[] { "" };
					
					setLoadingInProgress(ctx, true);
					
					// We disable the data changed observer temporarily since each of the updates
					// will trigger an onChange() in our data observer.
					r.unregisterContentObserver(sDataObserver);
					r.delete(PostitListDataProvider.CONTENT_URI, whereClause, args);
					r.registerContentObserver(PostitListDataProvider.CONTENT_URI, true, sDataObserver);

					final AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
					final ComponentName cn = new ComponentName(ctx, PostitListWidgetProvider.class);
					mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.stack_view);
				}
			});			
		} 
		else if (action.equals(RELOAD_ACTION) || action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
			

			sWorkerQueue.removeMessages(0);
			sWorkerQueue.post(new Runnable() {
				@Override
				public void run() {

					setLoadingInProgress(ctx, true);

					// Just force a reload by notifying that data has changed
					final AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
					final ComponentName cn = new ComponentName(ctx, PostitListWidgetProvider.class);
					mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.stack_view);
					Log.i(PostitListWidgetProvider.TAG, "onReceive: notified appwidget to refresh");
				}
			});				
	    }
		else if (action.equals(RELOAD_ACTION_DONE)) {
			
			Log.i(PostitListWidgetProvider.TAG, "processing RELOAD_ACTION_DONE...");
			setLoadingInProgress(context, false);
		}        
        
        super.onReceive(context, intent);
    }

	private void setLoadingInProgress(Context context, boolean state) {
		
		AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
		ComponentName widgetComponent = new ComponentName(context, PostitListWidgetProvider.class);
		int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);

		progressBarEnabled = state;
		onUpdate(context, widgetManager, widgetIds);
	}    
    
    @SuppressWarnings("deprecation")
	@Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        
    	
    	final ContentResolver r = context.getContentResolver();
		if (sDataObserver == null) {
			final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
			final ComponentName cn = new ComponentName(context, PostitListWidgetProvider.class);
			sDataObserver = new PostitListDataProviderObserver(mgr, cn, sWorkerQueue);
			r.registerContentObserver(PostitListDataProvider.CONTENT_URI, true, sDataObserver);
			Log.i(PostitListWidgetProvider.TAG, "onEnabled: Registered data observer");
		} 
		//update each of the widgets with the remote adapter
        for (int i = 0; i < appWidgetIds.length; ++i) {
        	 updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
    
    
    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
    		 int appWidgetId){

        // Here we setup the intent which points to the StackViewService which will
        // provide the views for this collection.
        Intent intent = new Intent(context, PostitListWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        // When intents are compared, the extras are ignored, so we need to embed the extras
        // into the data so that the extras will not be ignored.
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        rv.setRemoteAdapter(appWidgetId, R.id.stack_view, intent);

        // The empty view is displayed when the collection has no items. It should be a sibling
        // of the collection view.
        rv.setEmptyView(R.id.stack_view, R.id.empty_view);

        // Here we setup the a pending intent template. Individuals items of a collection
        // cannot setup their own pending intents, instead, the collection as a whole can
        // setup a pending intent template, and the individual items can set a fillInIntent
        // to create unique before on an item to item basis.
        Intent toastIntent = new Intent(context, PostitListWidgetProvider.class);
        toastIntent.setAction(PostitListWidgetProvider.CLICK_ACTION);
        toastIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        PendingIntent toastPendingIntent = PendingIntent.getBroadcast(context, 0, toastIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setPendingIntentTemplate(R.id.stack_view, toastPendingIntent);

    	// Bind the click intent for the clean list button on the widget
		final Intent cleanListIntent = new Intent(context, CleanListMenuActivity.class);
		cleanListIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		final PendingIntent cleanListPendingIntent = PendingIntent.getActivity(context, 0, cleanListIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		rv.setOnClickPendingIntent(R.id.cleanList, cleanListPendingIntent);

		// Bind the click intent for the addItem list button on the widget
		final Intent addItemIntent = new Intent(context, AddItemMenuActivity.class);
		addItemIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		final PendingIntent addItemPendingIntent = PendingIntent.getActivity(context, 0, addItemIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		rv.setOnClickPendingIntent(R.id.addItem, addItemPendingIntent);
		
        // Bind the click intent for the refresh button on the widget
        final Intent reloadIntent = new Intent(context, PostitListWidgetProvider.class);
        reloadIntent.setAction(PostitListWidgetProvider.RELOAD_ACTION);
        final PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0, reloadIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.reloadList, refreshPendingIntent);
        
        // Show either the reload button of the spinning progress icon, depending of the current state 
        rv.setViewVisibility(R.id.reloadList, progressBarEnabled ? View.GONE : View.VISIBLE);
        rv.setViewVisibility(R.id.loadingProgress, progressBarEnabled ? View.VISIBLE : View.GONE); 
        
        appWidgetManager.updateAppWidget(appWidgetId, rv);    	
    }
    
}