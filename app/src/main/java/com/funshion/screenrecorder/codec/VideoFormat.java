package com.funshion.screenrecorder.codec;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.funshion.screenrecorder.util.Const;

public class VideoFormat implements IFormat {
    private static final String TAG = "VideoFormat";
    private int videoWidth;
    private int videoHeight;
    private int videoBitrate;

    public int getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(int videoWidth) {
        this.videoWidth = videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(int videoHeight) {
        this.videoHeight = videoHeight;
    }

    public int getVideoBitrate() {
        return videoBitrate;
    }

    public void setVideoBitrate(int videoBitrate) {
        this.videoBitrate = videoBitrate;
    }

    @Override
    public MediaFormat toMediaFormat() {
        MediaFormat format = MediaFormat.createVideoFormat(Const.MIME_TYPE, videoWidth, videoHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, videoBitrate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, Const.FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, Const.I_FRAME_INTERVAL);
        Log.d(TAG, "created video format: " + format);
        return format;
    }
}
