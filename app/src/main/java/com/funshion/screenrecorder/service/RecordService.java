package com.funshion.screenrecorder.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.funshion.screenrecorder.codec.AFormat;
import com.funshion.screenrecorder.codec.Recorder;
import com.funshion.screenrecorder.codec.VFormat;
import com.funshion.screenrecorder.ui.RecordUiHandler;
import com.funshion.screenrecorder.ui.RecordWindow;
import com.funshion.screenrecorder.util.RecordConst;

public class RecordService extends Service
        implements RecordWindow.Callback {
    private static final String TAG = "RecordService";
    private RecordUiHandler mHandler;
    private int mWidth;
    private int mHeight;
    private int mBitrate;
    private int mCode;
    private Intent mResult;
    private String mDstPath;
    Recorder mRecorder;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new RecordUiHandler(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int commandId = intent.getIntExtra(
                RecordConst.SERVICE_COMMAND_ID,
                RecordConst.SERVICE_COMMAND_NULL);
        Log.i(TAG, "onStartCommand: commandId is " + commandId);
        switch (commandId) {
            default:
                break;
            case RecordConst.SERVICE_COMMAND_START:
                mCode = intent.getIntExtra(
                        RecordConst.SERVICE_RESULT_CODE,
                        -1);
                mResult = intent.getParcelableExtra(
                        RecordConst.SERVICE_RESULT_DATA);
                mHandler.postInitRecordUi();
                break;
            case RecordConst.SERVICE_COMMAND_STOP:
                break;
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStartRecorder() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager)
                getSystemService(WINDOW_SERVICE);
        if (windowManager != null) {
            Display display = windowManager.getDefaultDisplay();
            display.getMetrics(metrics);
            mWidth = metrics.widthPixels;
            mHeight = metrics.heightPixels;
            mBitrate = 4000;
            mDstPath = Environment
                    .getExternalStorageDirectory()
                    .getPath()
                    + "/demo.mp4";
        }
        VFormat vFormat = new VFormat();
        vFormat.setVideoWidth(mWidth);
        vFormat.setVideoHeight(mHeight);
        vFormat.setVideoBitrate(mBitrate);

        AFormat aFormat = new AFormat();
        aFormat.setAudioChannel(
                AudioFormat.CHANNEL_IN_DEFAULT);
        aFormat.setAudioSampleRate(32000);
        aFormat.setAudioBitrate(128000);

        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        MediaProjection mediaProjection;
        if (mediaProjectionManager != null) {
            mediaProjection = mediaProjectionManager.getMediaProjection(mCode, mResult);
            if (mediaProjection == null) {
                throw new NullPointerException("mMediaProjection == null");
            }
            mRecorder = new Recorder(
                    vFormat,
                    aFormat,
                    mediaProjection,
                    mDstPath);
            mRecorder.start();
        }

    }

    @Override
    public void onStopRecorder() {
        mRecorder.stop();
    }

    @Override
    public void onExitRecorder() {
        stopSelf();
    }
}
