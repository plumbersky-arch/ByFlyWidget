package com.byfly.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;

public class ByFlyWidgetSmallProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            ByFlyWidgetProvider.updateWidget(context, appWidgetManager, appWidgetId, R.layout.widget_byfly_small);
        }
    }

    @Override
    public void onReceive(Context context, android.content.Intent intent) {
        super.onReceive(context, intent);
    }
}
