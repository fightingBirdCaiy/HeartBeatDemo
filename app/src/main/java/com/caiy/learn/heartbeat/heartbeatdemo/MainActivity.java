package com.caiy.learn.heartbeat.heartbeatdemo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.caiy.learn.heartbeat.heartbeatdemo.service.HeartbeatService;

import java.util.Random;

public class MainActivity extends Activity {

    private BroadcastReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.button_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent heartbeatIntent = new Intent(MainActivity.this, HeartbeatService.class);
                heartbeatIntent.putExtra(HeartbeatService.EXTRA_KEY_URL, "http://m.baidu.com/news");
                heartbeatIntent.putExtra(HeartbeatService.EXTRA_KEY_TYPE, HeartbeatService.TYPE_START);
                Random random = new Random();
                int time = random.nextInt(5) + 1;
                heartbeatIntent.putExtra(HeartbeatService.EXTRA_KEY_INTERVAL_TIME, time * 1000L);
                heartbeatIntent.putExtra(HeartbeatService.EXTRA_KEY_CONNECTION_TIMEOUT, time * 1000L);
                startService(heartbeatIntent);

            }
        });
        findViewById(R.id.button_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent heartbeatIntent = new Intent(MainActivity.this, HeartbeatService.class);
                heartbeatIntent.putExtra(HeartbeatService.EXTRA_KEY_URL, "http://m.baidu.com/news");
                heartbeatIntent.putExtra(HeartbeatService.EXTRA_KEY_TYPE, HeartbeatService.TYPE_STOP);
                startService(heartbeatIntent);

            }
        });
        findViewById(R.id.button_3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent heartbeatIntent = new Intent(MainActivity.this, HeartbeatService.class);
                heartbeatIntent.putExtra(HeartbeatService.EXTRA_KEY_URL, "http://www.baidu404404.com/");
                heartbeatIntent.putExtra(HeartbeatService.EXTRA_KEY_TYPE, HeartbeatService.TYPE_START);
                Random random = new Random();
                int time = random.nextInt(5) + 1;
                heartbeatIntent.putExtra(HeartbeatService.EXTRA_KEY_INTERVAL_TIME, time * 1000L);
                heartbeatIntent.putExtra(HeartbeatService.EXTRA_KEY_CONNECTION_TIMEOUT, time * 1000L);
                startService(heartbeatIntent);

            }
        });
        findViewById(R.id.button_4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent heartbeatIntent = new Intent(MainActivity.this, HeartbeatService.class);
                heartbeatIntent.putExtra(HeartbeatService.EXTRA_KEY_URL, "http://www.baidu404404.com/");
                heartbeatIntent.putExtra(HeartbeatService.EXTRA_KEY_TYPE, HeartbeatService.TYPE_STOP);
                startService(heartbeatIntent);

            }
        });
        findViewById(R.id.button_5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent heartbeatIntent = new Intent(MainActivity.this, HeartbeatService.class);
                stopService(heartbeatIntent);

            }
        });

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("HeartbeatService","接收到广播：url=" + intent.getStringExtra(HeartbeatService.EXTRA_BROADCAST_KEY_URL) + ",isSuccess=" + intent.getBooleanExtra(HeartbeatService.EXTRA_BROADCAST_KEY_SUCCESS,false) + ",content=" + intent.getStringExtra(HeartbeatService.EXTRA_BROADCAST_KEY_CONTENT));
            }
        };
        // 动态注册广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(HeartbeatService.ACTION_BROADCAST_HEARTBEAT);
        registerReceiver(mReceiver, filter);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        //停止service
        Intent heartbeatIntent = new Intent(MainActivity.this, HeartbeatService.class);
        stopService(heartbeatIntent);

        unregisterReceiver(mReceiver);
    }
}
