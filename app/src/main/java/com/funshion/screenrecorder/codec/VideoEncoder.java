package com.funshion.screenrecorder.codec;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import com.funshion.screenrecorder.util.Const;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public final class VideoEncoder implements IEncoder {
    private static final String TAG = "VideoEncoder";

    public interface Callback {
        void onVideoFormatChanged(MediaFormat mediaFormat);
        void onVideoTrackEncoded(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo);
        void onVideoEncodeStopped();
    }

    private MediaCodec mVCodec;
    private Surface mSurface;
    private MediaFormat mVFormat;
    private Context mContext;
    private Callback mCallback;
    private VirtualDisplay mVirtualDisplay;
    private EncodeThread mEncodeThread;

    VideoEncoder(VideoFormat videoFormat) {
        mVFormat = videoFormat.toMediaFormat();
    }

    public void setContext(Context context) {
        mContext = context;
    }

    void addCallback(Callback callback) {
        mCallback = callback;
    }

    @Override
    public void prepare() throws IOException {
        mVCodec = MediaCodec.createEncoderByType(Const.MIME_TYPE);
        mVCodec.configure(mVFormat, null, null,
                MediaCodec.CONFIGURE_FLAG_ENCODE);
        mSurface = mVCodec.createInputSurface();
        Log.d(TAG, "created input surface: " + mSurface);
        DisplayManager displayManager = (DisplayManager)
                mContext.getSystemService(Context.DISPLAY_SERVICE);
        int flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION |
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
        if (displayManager != null) {
            mVirtualDisplay = displayManager.createVirtualDisplay("-display-",
                    mVFormat.getInteger(MediaFormat.KEY_WIDTH),
                    mVFormat.getInteger(MediaFormat.KEY_HEIGHT),
                    DisplayMetrics.DENSITY_HIGH, mSurface, flags);
        }
        Log.d(TAG, "created virtual display: " + mVirtualDisplay);
    }

    @Override
    public void encode() {
        Log.i(TAG, "encode: start");
        mEncodeThread = new EncodeThread(false);
        new Thread(mEncodeThread).start();
    }

    @Override
    public void release() {
        if (mEncodeThread != null && !mEncodeThread.mQuit.get()) {
            mEncodeThread.mQuit = new AtomicBoolean(true);
        }
        if (mVCodec != null) {
            mVCodec.stop();
            mVCodec.release();
            mVCodec = null;
        }
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }
    }

    private class EncodeThread implements Runnable {
        private AtomicBoolean mQuit;
        long startTime;
        MediaCodec.BufferInfo mBufferInfo;

        EncodeThread(boolean quit) {
            mQuit = new AtomicBoolean(quit);
            startTime = System.currentTimeMillis();
        }
        @Override
        public void run() {
            mVCodec.start();
            ByteBuffer[] encoderOutputBuffers = mVCodec.getOutputBuffers();
            mBufferInfo = new MediaCodec.BufferInfo();
            while (!mQuit.get() && calculateEncodingTime() < Const.RECORD_TOTAL_TIME) {
                int encoderStatus = mVCodec.dequeueOutputBuffer(mBufferInfo, Const.TIMEOUT_US);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    Log.i(TAG, "no output from encoder available");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    Log.d(TAG, "encoder output buffers changed");
                    encoderOutputBuffers = mVCodec.getOutputBuffers();
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = mVCodec.getOutputFormat();
                    Log.i(TAG, "output format changed.\n new format: "
                            + newFormat.toString());
                    if (((ScreenRecorder) mCallback).isMuxerStarted()) {
                        throw new IllegalStateException("output format already changed!");
                    }
                    mCallback.onVideoFormatChanged(newFormat);
                } else if (encoderStatus < 0) {
                    Log.e(TAG, "unexpected result from encoder.dequeueOutputBuffer: "
                            + encoderStatus);
                } else {
                    if (!((ScreenRecorder) mCallback).isMuxerStarted()) {
                        throw new IllegalStateException("MediaMuxer dose not call addTrack(format) ");
                    }
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    mCallback.onVideoTrackEncoded(encodedData, mBufferInfo);
                    mVCodec.releaseOutputBuffer(encoderStatus, false);
                }
            }
            Log.i(TAG, "stop EncodeThread");
            mCallback.onVideoEncodeStopped();
        }

        private long calculateEncodingTime() {
            long currentTime = System.currentTimeMillis();
            return currentTime - startTime;
        }
    }
}
