package com.owson.shareimage;

import android.app.Application;

import com.facebook.drawee.backends.pipeline.Fresco;

public class DefaultApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Fresco.initialize(this);
    }
}
