package com.funshion.screenrecorder.util;

public class RecordConst {
    public static final String MESSAGE_LOW_VERSION =
            "current device do not support screen record !!!";
    public static final String MESSAGE_PERMISSION_DENI =
            "current device do not have scrren record permission";
    public static final String MESSAGE_COULD_RECORD =
            "current device is ready for recording";
    public static final String MESSAGE_CAN_NOT_RECORD =
            "current device isn't ready for recording";
    public static final String MESSAGE_CAN_NOT_OVERLAY_SCREEN =
            "current device don't have overlay screen permission";
    public static final String MESSAGE_COULD_OVERLAY_SCREEN =
            "current device have overlay screen permission";

    public static final String SERVICE_COMMAND_ID = "commandId";
    public static final String SERVICE_RESULT_CODE = "code";
    public static final String SERVICE_RESULT_DATA = "data";

    public static final String VIDEO_MIME_TYPE = "video/avc";
    public static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";

    private static final int REQUEST_CODE_BASE = 100;
    public static final int REQUEST_CODE_PERMISSION = REQUEST_CODE_BASE;
    public static final int REQUEST_CODE_RECORDER = REQUEST_CODE_BASE + 1;
    public static final int REQUEST_CODE_OVERLAY_SCREEN = REQUEST_CODE_BASE + 2;

    private static final int RECORDER_COMMAND_BASE = 200;
    public static final int RECORDER_COMMAND_UI_INIT = RECORDER_COMMAND_BASE;
    public static final int RECORDER_COMMAND_UI_DE_INIT = RECORDER_COMMAND_BASE + 1;

    private static final int SERVICE_COMMAND_BASE = 300;
    public static final int SERVICE_COMMAND_NULL = SERVICE_COMMAND_BASE;
    public static final int SERVICE_COMMAND_START = SERVICE_COMMAND_BASE + 1;
    public static final int SERVICE_COMMAND_STOP = SERVICE_COMMAND_BASE + 2;

    public static final int VIDEO_FRAME_RATE = 25;
    public static final int VIDEO_I_FRAME_INTERVAL = 5;
    public static final int VIDEO_TIMEOUT_US = 10000;

    public static final int AUDIO_FRAME_SIZE = 2048;
    public static final int AUDIO_TIMEOUT_US = 10000;
}
