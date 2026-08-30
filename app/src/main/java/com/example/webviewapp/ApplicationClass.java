package com.qandilalzman.digital;

import android.app.Application;

import com.onesignal.OneSignal;

public class ApplicationClass extends Application {

    private static final String ONESIGNAL_APP_ID =
            "3b3d8c70-06af-4e67-94b1-7e6641939b90";

    @Override
    public void onCreate() {
        super.onCreate();

        // تهيئة OneSignal
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);
    }
}
