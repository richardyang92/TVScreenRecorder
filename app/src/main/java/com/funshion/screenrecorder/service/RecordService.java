package com.funshion.screenrecorder.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.funshion.screenrecorder.codec.AudioFormat;
import com.funshion.screenrecorder.codec.ScreenRecorder;
import com.funshion.screenrecorder.codec.VideoFormat;
import com.funshion.screenrecorder.util.RecordConst;

public class RecordService extends Service {
    private static final String TAG = "RecordService";
    private static final int RECORDER_INIT_CMD = 0x01;
    private static final int RECORDER_RECORD_CMD = 0x03;
    private static final int RECORD_DEFAULT_WIDTH = 1280;
    private static final int RECORD_DEFAULT_HEIGHT = 720;
    private static final int RECORD_DEFAULT_BITRATE = 2500;

    private Handler mHandler;

    private String mDstPath;
    private int mWidth;
    private int mHeight;
    private int mBitRate;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                switch (message.what) {
                    default:
                        break;
                    case RECORDER_INIT_CMD:
                        init((Intent) message.obj);
                        break;
                    case RECORDER_RECORD_CMD:
                        record();
                        break;
                }
                return true;
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: record video");
        postInit(intent);
        postRecord();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void postInit(Intent intent) {
        Message message = Message.obtain();
        message.what = RECORDER_INIT_CMD;
        message.obj = intent;
        mHandler.sendMessage(message);
    }

    private void postRecord() {
        Message message = Message.obtain();
        message.what = RECORDER_RECORD_CMD;
        mHandler.sendMessage(message);
    }

    private void init(Intent intent) {
        if (intent == null) {
            throw new IllegalStateException("intent is null");
        }
        mDstPath = intent.getStringExtra(RecordConst.RECORD_PATH);
        mWidth = intent.getIntExtra(RecordConst.VIDEO_WIDTH, RECORD_DEFAULT_WIDTH);
        mHeight = intent.getIntExtra(RecordConst.VIDEO_HEIGHT, RECORD_DEFAULT_HEIGHT);
        mBitRate = intent.getIntExtra(RecordConst.VIDEO_BITRATE, RECORD_DEFAULT_BITRATE);
    }

    public void record() {
        VideoFormat videoFormat = new VideoFormat();
        videoFormat.setVideoWidth(mWidth);
        videoFormat.setVideoHeight(mHeight);
        videoFormat.setVideoBitrate(mBitRate);
        ScreenRecorder screenRecorder = new ScreenRecorder(this,
                videoFormat, new AudioFormat(), mDstPath);
        screenRecorder.start();
    }
}
