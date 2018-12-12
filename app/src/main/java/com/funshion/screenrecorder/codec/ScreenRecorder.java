package com.funshion.screenrecorder.codec;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ScreenRecorder implements VideoEncoder.Callback {
    private static final String TAG = "ScreenRecorder";
    private MediaMuxer mMediaMuxer;
    private VideoEncoder mVideoEncoder;
    private int mVideoTrackIndex;
    private boolean mMuxerStarted;

    public ScreenRecorder(Context context,
                          VideoFormat videoFormat,
                          AudioFormat audioFormat,
                          String recordPath) {
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
        deInitVEncoder();
        stop();
    }

    boolean isMuxerStarted() {
        return mMuxerStarted;
    }

    public void start() {
        mVideoEncoder.encode();
    }

    public void stop() {
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
        mVideoEncoder.release();
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
