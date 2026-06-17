package com.byfly.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.widget.RemoteViews;

public class ByFlyWidgetProvider extends AppWidgetProvider {

    static final String ACTION_REFRESH = "com.byfly.widget.ACTION_REFRESH";
    static final String PREFS_NAME = "ByFlyPrefs";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, R.layout.widget_byfly);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            ComponentName cn1 = new ComponentName(context, ByFlyWidgetProvider.class);
            for (int id : mgr.getAppWidgetIds(cn1)) {
                fetchBalance(context, mgr, id, R.layout.widget_byfly);
            }
            ComponentName cn2 = new ComponentName(context, ByFlyWidgetSmallProvider.class);
            for (int id : mgr.getAppWidgetIds(cn2)) {
                fetchBalance(context, mgr, id, R.layout.widget_byfly_small);
            }
        }
    }

    private static boolean isSmall(int layoutId) {
        return layoutId == R.layout.widget_byfly_small;
    }

    private static String formatBalance(int layoutId, String balance) {
        return balance;
    }

    static void setupClicks(Context context, RemoteViews views, int widgetId, int layoutId) {
        Intent appIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (appIntent != null) {
            appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent appPI = PendingIntent.getActivity(context, widgetId * 10 + 1, appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.widget_icon, appPI);
        }

        Intent refreshIntent = new Intent(context, ByFlyWidgetProvider.class);
        refreshIntent.setAction(ACTION_REFRESH);
        PendingIntent refreshPI = PendingIntent.getBroadcast(context, widgetId * 10 + 2, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_balance, refreshPI);
    }

    static void updateWidget(Context context, AppWidgetManager mgr, int widgetId, int layoutId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);
        setupClicks(context, views, widgetId, layoutId);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String login = prefs.getString("login", "");
        String savedBalance = prefs.getString("balance_" + login, "");

        if (!savedBalance.isEmpty()) {
            views.setTextViewText(R.id.widget_balance, formatBalance(layoutId, savedBalance));
        } else {
            views.setTextViewText(R.id.widget_balance, formatBalance(layoutId, "---"));
            if (!login.isEmpty()) {
                fetchBalance(context, mgr, widgetId, layoutId);
            }
        }

        mgr.updateAppWidget(widgetId, views);
    }

    static void fetchBalance(final Context context, final AppWidgetManager mgr, final int widgetId, final int layoutId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String login = prefs.getString("login", "");
        String password = prefs.getString("password", "");

        if (login.isEmpty() || password.isEmpty()) {
            RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);
            views.setTextViewText(R.id.widget_balance, formatBalance(layoutId, "---"));
            mgr.updateAppWidget(widgetId, views);
            return;
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);
        views.setTextViewText(R.id.widget_balance, formatBalance(layoutId, "..."));
        mgr.updateAppWidget(widgetId, views);

        final String fLogin = login;
        final String fPassword = password;

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    return ByFlyFetcher.fetchBalance(fLogin, fPassword);
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String balance) {
                RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);
                setupClicks(context, views, widgetId, layoutId);

                if (balance != null) {
                    views.setTextViewText(R.id.widget_balance, formatBalance(layoutId, balance));

                    SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                    editor.putString("balance_" + fLogin, balance);
                    editor.apply();
                } else {
                    views.setTextViewText(R.id.widget_balance, formatBalance(layoutId, "error"));
                }

                mgr.updateAppWidget(widgetId, views);
            }
        }.execute();
    }
}
