package com.funshion.screenrecorder.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.funshion.screenrecorder.util.RecordConst;

public class RecordUiHandler extends Handler {
    private static final String TAG = "RecordUiHandler";
    private Context mContext;
    private RecordWindow mWindow;

    public RecordUiHandler(Context context) {
        mContext = context;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            default:
                break;
            case RecordConst.RECORDER_COMMAND_UI_INIT:
                handleUiInit();
                break;
            case RecordConst.RECORDER_COMMAND_UI_DE_INIT:
                break;
        }
    }

    public void postInitRecordUi() {
        Message message = Message.obtain();
        message.what = RecordConst.RECORDER_COMMAND_UI_INIT;
        sendMessage(message);
    }

    public void postDeInitRecordUi() {
        Message message = Message.obtain();
        message.what = RecordConst.RECORDER_COMMAND_UI_DE_INIT;
        sendMessage(message);
    }

    private void handleUiInit() {
        mWindow = new RecordWindow(mContext);
        mWindow.createWindow();
    }
}
