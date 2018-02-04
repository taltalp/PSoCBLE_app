package com.example.taltalp.psocble_app;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class TestBeaconService extends Service {
    public TestBeaconService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
