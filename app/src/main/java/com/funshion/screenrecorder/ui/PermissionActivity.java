package com.funshion.screenrecorder.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.funshion.screenrecorder.R;
import com.funshion.screenrecorder.service.RecordService;
import com.funshion.screenrecorder.util.PermissionUtil;
import com.funshion.screenrecorder.util.RecordConst;

public class PermissionActivity extends Activity {
    private static final String TAG = "PermissionActivity";

    MediaProjectionManager mMediaProjectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission);
        requestAudioRecordAndSDCardPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode == RecordConst.REQUEST_CODE_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    requireScreenOverlayPermission();
                } else {
                    Log.e(TAG, "requestAudioRecordAndSDCardPermission: " +
                            RecordConst.MESSAGE_PERMISSION_DENI);
                    finish();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RecordConst.REQUEST_CODE_RECORDER) {
            if (resultCode == RESULT_OK) {
                Log.i(TAG, "onActivityResult: "
                        + RecordConst.MESSAGE_COULD_RECORD);
                startRecordService(resultCode, data);
            } else {
                Log.e(TAG, "onActivityResult: "
                        + RecordConst.MESSAGE_CAN_NOT_RECORD);
            }
            finish();
        } else if (requestCode == RecordConst.REQUEST_CODE_OVERLAY_SCREEN) {
            if (!Settings.canDrawOverlays(PermissionActivity.this)) {
                Toast.makeText(PermissionActivity.this,
                        RecordConst.MESSAGE_CAN_NOT_OVERLAY_SCREEN,
                        Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(PermissionActivity.this,
                        RecordConst.MESSAGE_COULD_OVERLAY_SCREEN,
                        Toast.LENGTH_SHORT).show();
                requireScreenRecordPermission();
            }
        }
    }

    private void requestAudioRecordAndSDCardPermission() {
        if (Build.VERSION.SDK_INT
                < Build.VERSION_CODES.LOLLIPOP) {
            Toast.makeText(PermissionActivity.this,
                    RecordConst.MESSAGE_LOW_VERSION,
                    Toast.LENGTH_LONG).show();
            Log.e(TAG, "requestAudioRecordAndSDCardPermission: "
                    + RecordConst.MESSAGE_LOW_VERSION);
            finish();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean allowWriteSDCard = PermissionUtil.checkPermission(
                    PermissionActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
            boolean allowRecordAudio = PermissionUtil.checkPermission(
                    PermissionActivity.this,
                    Manifest.permission.RECORD_AUDIO);
            if (allowWriteSDCard
                    && allowRecordAudio) {
                requireScreenOverlayPermission();
            } else {
                Log.e(TAG, "checkPermission: " +
                        RecordConst.MESSAGE_PERMISSION_DENI);
                PermissionUtil.requirePermissions(
                        PermissionActivity.this,
                        new String[] {
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.RECORD_AUDIO
                        }, RecordConst.REQUEST_CODE_PERMISSION);
            }
        }
    }

    private void requireScreenRecordPermission() {
        mMediaProjectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mMediaProjectionManager != null) {
            Intent intent = mMediaProjectionManager.createScreenCaptureIntent();
            startActivityForResult(intent, RecordConst.REQUEST_CODE_RECORDER);
        }
    }

    private void requireScreenOverlayPermission() {
        if (Settings.canDrawOverlays(PermissionActivity.this)) {
            requireScreenRecordPermission();
            return;
        }
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, RecordConst.REQUEST_CODE_OVERLAY_SCREEN);
    }

    private void startRecordService(int resultCode, Intent data) {
        Intent intent = new Intent(
                PermissionActivity.this,
                RecordService.class);
        intent.putExtra(
                RecordConst.SERVICE_COMMAND_ID,
                RecordConst.SERVICE_COMMAND_START);
        intent.putExtra(RecordConst.SERVICE_RESULT_CODE, resultCode);
        intent.putExtra(RecordConst.SERVICE_RESULT_DATA, data);
        startService(intent);
    }
}
