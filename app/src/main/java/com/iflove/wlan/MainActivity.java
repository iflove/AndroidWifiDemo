package com.iflove.wlan;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.iflove.wlan.wifi.WifiSettingsFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getFragmentManager().beginTransaction()
                .add(R.id.contentView, new WifiSettingsFragment())
                .commitAllowingStateLoss();

    }
}
