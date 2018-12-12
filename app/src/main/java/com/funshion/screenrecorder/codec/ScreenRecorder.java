package com.funshion.screenrecorder.codec;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ScreenRecorder implements VideoEncoder.Callback {
    private static final String TAG = "ScreenRecorder";
    private Context mContext;
    private MediaMuxer mMediaMuxer;
    private VideoEncoder mVideoEncoder;
    private int mVideoTrackIndex;
    private boolean mMuxerStarted;
    private long mRecordStartedTime;
    private Handler mHandler;

    public ScreenRecorder(Context context,
                          VideoFormat videoFormat,
                          AudioFormat audioFormat,
                          String recordPath) {
        mContext = context;
        mHandler = new Handler();
        try {
            initVEncoder(context, videoFormat);
            initMuxer(recordPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onVideoFormatChanged(MediaFormat mediaFormat) {
        mVideoTrackIndex = mMediaMuxer.addTrack(mediaFormat);
        mMediaMuxer.start();
        mMuxerStarted = true;
    }

    @Override
    public void onVideoTrackEncoded(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        Log.i(TAG, "encodeToVideoTrack: start");
        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
            bufferInfo.size = 0;
        }
        if (bufferInfo.size == 0) {
            Log.d(TAG, "info.size == 0, drop it.");
            byteBuffer = null;
        } else {
            Log.d(TAG, "got buffer, info: size=" + bufferInfo.size
                    + ", presentationTimeUs=" + bufferInfo.presentationTimeUs
                    + ", offset=" + bufferInfo.offset);
        }
        if (byteBuffer != null) {
            byteBuffer.position(bufferInfo.offset);
            byteBuffer.limit(bufferInfo.offset + bufferInfo.size);
            mMediaMuxer.writeSampleData(mVideoTrackIndex, byteBuffer, bufferInfo);
            Log.i(TAG, "sent " + bufferInfo.size + " bytes to muxer...");
        }
    }

    @Override
    public void onVideoEncodeStopped() {
        Log.i(TAG, "onVideoEncodeStopped: start");
        deInitVEncoder();
    }

    boolean isMuxerStarted() {
        return mMuxerStarted;
    }

    public long getRecordStartedTime() {
        return mRecordStartedTime;
    }

    public void start() {
        mRecordStartedTime = System.currentTimeMillis();
        mVideoEncoder.encode();
        Toast.makeText(mContext, "录屏开始", Toast.LENGTH_LONG).show();
    }

    public void stop() {
        Toast.makeText(mContext, "录屏结束", Toast.LENGTH_LONG).show();
        deInitMuxer();
    }

    private void initVEncoder(Context context, VideoFormat videoFormat) throws IOException {
        mVideoEncoder = new VideoEncoder(videoFormat);
        mVideoEncoder.setContext(context);
        mVideoEncoder.addCallback(this);
        mVideoEncoder.prepare();
    }

    private void initMuxer(String recordPath) throws IOException {
        mMediaMuxer = new MediaMuxer(recordPath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        mMuxerStarted = false;
    }

    private void deInitVEncoder() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mVideoEncoder.release();
                stop();
                Log.i(TAG, "onVideoEncodeStopped: stop");
            }
        });
    }

    private void deInitMuxer() {
        mMuxerStarted = false;
        if (mMediaMuxer != null) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mMediaMuxer = null;
        }
    }
}
