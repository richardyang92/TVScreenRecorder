package com.funshion.screenrecorder.codec;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Recorder
        implements VEncoder.Callback, AEncoder.Callback {
    private static final String TAG = "Recorder";
    private MediaMuxer mMediaMuxer;
    private MediaProjection mMediaProjection;
    private VEncoder mVEncoder;
    private AEncoder mAEncoder;
    private int mVideoTrackIndex;
    private int mAudioTrackIndex;
    private boolean mVideoTrackSetted;
    private boolean mAudioTrackSetted;
    private boolean mMuxerStarted;
    private Handler mHandler;
    private RecordThread mThread;

    public Recorder(VFormat vFormat,
                    AFormat aFormat,
                    MediaProjection mediaProjection,
                    String recordPath) {
        mMediaProjection = mediaProjection;
        mHandler = new Handler();
        initVEncoder(vFormat);
        initAEncoder(aFormat);
        initMuxer(recordPath);
    }

    private void initVEncoder(VFormat vFormat) {
        mVEncoder = new VEncoder(vFormat);
        mVEncoder.setMediaProjection(mMediaProjection);
        mVEncoder.addCallback(this);

        try {
            mVEncoder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initAEncoder(AFormat aFormat) {
        mAEncoder = new AEncoder(aFormat);
        mAEncoder.addCallback(this);
        try {
            mAEncoder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initMuxer(String recordPath) {
        try {
            mMediaMuxer = new MediaMuxer(recordPath,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mMuxerStarted = false;
            mVideoTrackSetted = false;
            mAudioTrackSetted = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deInitVEncoder() {
        if (mVEncoder != null) {
            mVEncoder.release();
            mVEncoder = null;
        }
    }

    private void deInitAEncoder() {
        if (mAEncoder != null) {
            mAEncoder.release();
            mAEncoder = null;
        }
    }

    private void deInitMuxer() {
        if (mMediaMuxer != null) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mMediaMuxer = null;
            mMuxerStarted = false;
        }
    }

    public void start() {
        mThread = new RecordThread(false);
        new Thread(mThread).start();
    }

    public void stop() {
        mThread.mQuit = true;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                deInitVEncoder();
                deInitAEncoder();
                deInitMuxer();
            }
        }, 20);
    }

    @Override
    public void onVideoFormatChanged(MediaFormat mediaFormat) {
        mVideoTrackIndex = mMediaMuxer.addTrack(mediaFormat);
        mVideoTrackSetted = true;
    }

    @Override
    public void onVideoTrackEncoded(
            ByteBuffer byteBuffer,
            MediaCodec.BufferInfo bufferInfo) {
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
            Log.i(TAG, "sent video " + bufferInfo.size + " bytes to muxer...");
        }
    }

    @Override
    public void onAudioFormatChanged(MediaFormat mediaFormat) {
        mAudioTrackIndex = mMediaMuxer.addTrack(mediaFormat);
        mAudioTrackSetted = true;
    }

    @Override
    public void onAudioTrackEncoded(
            ByteBuffer byteBuffer,
            MediaCodec.BufferInfo bufferInfo) {
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
            mMediaMuxer.writeSampleData(mAudioTrackIndex, byteBuffer, bufferInfo);
            Log.i(TAG, "sent audio " + bufferInfo.size + " bytes to muxer...");
        }
    }

    boolean isMuxerStarted() {
        return mMuxerStarted;
    }

    private class RecordThread implements Runnable {
        boolean mQuit;

        RecordThread(boolean quit) {
            mQuit = quit;
        }

        @Override
        public void run() {
            while (!mQuit) {
                if (mAudioTrackSetted && mVideoTrackSetted) {
                    if (!isMuxerStarted()) {
                        mMediaMuxer.start();
                        mMuxerStarted = true;
                    }
                }
                Log.i(TAG, "encoding......");
                mVEncoder.encode();
                mAEncoder.encode();
            }
        }
    }
}
