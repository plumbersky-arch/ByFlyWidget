package com.byfly.widget;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class FetchBalanceService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
