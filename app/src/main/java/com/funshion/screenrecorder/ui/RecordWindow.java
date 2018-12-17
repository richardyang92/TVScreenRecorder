package com.funshion.screenrecorder.ui;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import com.funshion.screenrecorder.R;
import com.funshion.screenrecorder.service.RecordService;

public class RecordWindow implements FloatWindow {
    private static final String TAG = "RecordWindow";
    private Context mContext;
    private Callback mCallback;
    private LinearLayout mLinearLayout;
    private WindowManager.LayoutParams mParams;
    private WindowManager mWindowManager;

    private Button mScreenRecordBtn;
    private Button mRecordStopBtn;
    private int statusBarHeight = 0;
    private int mTouchStartX;
    private int mTouchStartY;
    private int mTouchCurrentX;
    private int mTouchCurrentY;

    public interface Callback {
        void onStartRecorder();
        void onStopRecorder();
        void onExitRecorder();
    }

    RecordWindow(Context context) {
        mContext = context;
        mCallback = (RecordService) context;
    }

    @Override
    public void createWindow() {
        Log.i(TAG, "createWindow: ");
        initParams();
        initUi();
    }

    @Override
    public void destroyWindow() {
        mWindowManager.removeView(mLinearLayout);
        mCallback.onExitRecorder();
    }

    private void initParams() {
        int resourceId = mContext.getResources().getIdentifier(
                "status_bar_height","dimen","android");
        if (resourceId > 0) {
            statusBarHeight = mContext.getResources().getDimensionPixelSize(resourceId);
        }
        Log.i(TAG,"status bar height is:" + statusBarHeight);
        mParams = new WindowManager.LayoutParams();
        mWindowManager = (WindowManager)
                mContext.getSystemService(Context.WINDOW_SERVICE);
        mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        mParams.format = PixelFormat.RGBA_8888;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|
                WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR|
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        mParams.gravity = Gravity.START | Gravity.TOP;
        mParams.x = 0;
        mParams.y = statusBarHeight;
        mParams.width = 400;
        mParams.height = 150;
    }

    private void initUi() {
        mLinearLayout = (LinearLayout)
                LayoutInflater.from(mContext)
                        .inflate(R.layout.window_record, null);
        mWindowManager.addView(mLinearLayout, mParams);
        Log.i(TAG,"mLinearLayout-->left:"
                + mLinearLayout.getLeft());
        Log.i(TAG,"mLinearLayout-->right:"
                + mLinearLayout.getRight());
        Log.i(TAG,"mLinearLayout-->top:"
                + mLinearLayout.getTop());
        Log.i(TAG,"mLinearLayout-->bottom:"
                + mLinearLayout.getBottom());
        mLinearLayout.measure(
                View.MeasureSpec.UNSPECIFIED,
                View.MeasureSpec.UNSPECIFIED);
        mScreenRecordBtn = (Button) mLinearLayout.findViewById(R.id.record_btn);
        mScreenRecordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mScreenRecordBtn.getText().equals("start")) {
                    mScreenRecordBtn.setText("stop");
                    mCallback.onStartRecorder();
                } else if (mScreenRecordBtn.getText().equals("stop")) {
                    mScreenRecordBtn.setText("start");
                    mCallback.onStopRecorder();
                }
            }
        });

        mRecordStopBtn = (Button) mLinearLayout.findViewById(R.id.stop_btn);
        mRecordStopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick: should remove this window");
                destroyWindow();
            }
        });

        mScreenRecordBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mTouchStartX = (int)event.getRawX();
                        mTouchStartY = (int)event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        mTouchCurrentX = (int) event.getRawX();
                        mTouchCurrentY = (int) event.getRawY();
                        mParams.x += mTouchCurrentX - mTouchStartX;
                        mParams.y += mTouchCurrentY - mTouchStartY;
                        mWindowManager.updateViewLayout(mLinearLayout, mParams);

                        mTouchStartX = mTouchCurrentX;
                        mTouchStartY = mTouchCurrentY;
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                }
                return false;
            }
        });
    }
}
