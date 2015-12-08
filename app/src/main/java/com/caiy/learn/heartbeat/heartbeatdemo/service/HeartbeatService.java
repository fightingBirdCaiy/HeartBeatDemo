package com.caiy.learn.heartbeat.heartbeatdemo.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpConnectionParams;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Created by caiyong on 15/12/4.
 */
public class HeartbeatService extends Service{

    private static final String TAG = HeartbeatService.class.getSimpleName();
    /**
     * 必传
     * TYPE_START 开启心跳
     * TYPE_STOP 关闭心跳
     */
    public static final String EXTRA_KEY_TYPE = "type";
    /**
     * 必传
     */
    public static final String EXTRA_KEY_URL = "url";
    /**
     * 心跳间隔，单位：毫秒
     * 如果是TYPE_START类型，必传
     */
    public static final String EXTRA_KEY_INTERVAL_TIME = "intervalTime";
    /**
     * 网络请求超时时间,单位：毫秒
     * 非必传
     * 默认值 DEFAULT_CONNECT_TIMEOUT
     */
    public static final String EXTRA_KEY_CONNECTION_TIMEOUT = "connectTimeout";
    /**
     * 默认网络超时时间
     */
    private static final int DEFAULT_CONNECT_TIMEOUT = 10*1000;
    /**
     * 开启心跳
     */
    public static final int TYPE_START = 1;
    /**
     * 关闭心跳
     */
    public static final int TYPE_STOP = 2;

    public static final String ACTION_BROADCAST_HEARTBEAT = "http://www.baidu.com";
    public static final String EXTRA_BROADCAST_KEY_URL = "url";
    public static final String EXTRA_BROADCAST_KEY_SUCCESS = "isSuccess";
    public static final String EXTRA_BROADCAST_KEY_CONTENT = "content";

    private Map<String,FutureTask<String>> mMap = Collections.synchronizedMap(new HashMap<String, FutureTask<String>>());

    @Override
    public void onCreate() {
        Log.i(TAG, "HeartbeatService onCreate");
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //强制杀死app后，再启动应用，系统会调用service的start方法。
        //这种情况，目前直接返回
        if(intent == null){
            int result =  super.onStartCommand(intent, flags, startId);

            maybeStopSelf();

            return result;
        }

        int type = intent.getIntExtra(EXTRA_KEY_TYPE, -1);
        String url = intent.getStringExtra(EXTRA_KEY_URL);
        long intervalTime = intent.getLongExtra(EXTRA_KEY_INTERVAL_TIME, -1);
        int timeout = intent.getIntExtra(EXTRA_KEY_CONNECTION_TIMEOUT,DEFAULT_CONNECT_TIMEOUT);

        Log.i(TAG,"HeartbeatService onStartCommand,intent=" + intent + ",flags=" + flags + ",startId=" + startId + ",type=" + type + ",url=" + url + ",intervalTime=" + intervalTime);

        //参数错误，直接返回
        if(url == null || url.trim().equals("") || (type != TYPE_START && type != TYPE_STOP) || (intervalTime <= 0 && type == TYPE_START)){
            Log.e(TAG,"参数错误:url=" + url + ",intervalTime=" + intervalTime + ",type=" + type);
            return super.onStartCommand(intent, flags, startId);
        }
        url = url.trim();

        if(type == TYPE_START){

            if(mMap.containsKey(url)){
                //重新启动
                removeHeartbeatTask(url);
                addHeartbeatTask(url, intervalTime, timeout);
            }else{
                //第一次启动
                addHeartbeatTask(url, intervalTime, timeout);
            }

        }else if(type == TYPE_STOP){
            if(mMap.containsKey(url)){
                //如果已经启动
                removeHeartbeatTask(url);
                maybeStopSelf();
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void maybeStopSelf() {
        if(mMap.isEmpty()){
            stopSelf();
        }
    }

    private void addHeartbeatTask(String url, long intervalTime,int timeout) {
        Callable callable = new HeartbeatCallable(url,intervalTime,timeout);
        FutureTask<String> futureTask = new FutureTask<String>(callable);
        Thread thread = new Thread(futureTask);
        thread.start();
        mMap.put(url,futureTask);
    }

    private void removeHeartbeatTask(String url) {
        FutureTask<String> oldFutureTask = mMap.get(url);
        oldFutureTask.cancel(true);
        mMap.remove(url);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "HeartbeatService onDestroy");

        Set<String> urls = mMap.keySet();
        for(String url : urls){
            FutureTask<String> task = mMap.get(url);
            task.cancel(true);
        }
        mMap.clear();


        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class HeartbeatCallable implements Callable<String> {

        private String url;
        private long milliSecond = 60*1000;
        private int timeout = 10;

        HeartbeatCallable(String url,long milliSecond, int timeout){
            this.url = url;
            this.milliSecond = milliSecond;
            this.timeout = timeout;
        }

        @Override
        public String call() throws Exception {
            while(true){
                Thread.currentThread().sleep(milliSecond);
                Log.i(TAG, "Thread:" + Thread.currentThread().toString() + ",ulr=" + url + ",millsSecond=" + milliSecond);


                BufferedReader in = null;

                String content = null;
                try {
                    HttpClient httpClient = new DefaultHttpClient();
                    httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeout);
                    httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, timeout);
                    HttpRequestBase httpGet = new HttpGet(url);
                    HttpResponse response = httpClient.execute(httpGet);
                    int statusCode = response.getStatusLine().getStatusCode();
                    in = new BufferedReader(new InputStreamReader(response.getEntity()
                            .getContent()));
                    StringBuffer sb = new StringBuffer("");
                    String line = "";
                    String NL = System.getProperty("line.separator");
                    while ((line = in.readLine()) != null) {
                        sb.append(line + NL);
                    }
                    in.close();
                    content = sb.toString();
                    Intent successIntent = new Intent();
                    successIntent.setAction(ACTION_BROADCAST_HEARTBEAT);
                    successIntent.putExtra(EXTRA_BROADCAST_KEY_URL, url);
                    successIntent.putExtra(EXTRA_BROADCAST_KEY_SUCCESS, false);
                    successIntent.putExtra(EXTRA_BROADCAST_KEY_CONTENT,content);
                    sendBroadcast(successIntent);
                    Log.i(TAG, "Thread:" + Thread.currentThread().toString() + ",url=" + url + ",millsSecond=" + milliSecond + ",statusCode=" + statusCode + ",content=" + content);
                }catch(Exception e){
                    Intent errorIntent = new Intent();
                    errorIntent.setAction(ACTION_BROADCAST_HEARTBEAT);
                    errorIntent.putExtra(EXTRA_BROADCAST_KEY_URL, url);
                    errorIntent.putExtra(EXTRA_BROADCAST_KEY_SUCCESS,false);
                    sendBroadcast(errorIntent);
                    Log.e(TAG, "Thread:" + Thread.currentThread().toString() + ",url=" + url + ",millsSecond=" + milliSecond + ",exception occur",e);
                }finally {
                    if (in != null) {
                        try {
                            in.close();// 最后要关闭BufferedReader
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

}
