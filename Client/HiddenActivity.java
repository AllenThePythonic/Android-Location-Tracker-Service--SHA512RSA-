package com.example.lancelot.gps_auto_sender;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class HiddenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(null);
        startIntentService();
        this.moveTaskToBack(true);
    }

    public void startIntentService() {
        Intent intent = new Intent(HiddenActivity.this, HiddenService.class);
        startService(intent);
    }

    @Override
    public void finish() {
        moveTaskToBack(true);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
