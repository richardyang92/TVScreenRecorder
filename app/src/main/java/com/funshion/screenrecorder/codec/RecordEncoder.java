package com.funshion.screenrecorder.codec;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RecordEncoder {
    private static final String TAG = "RecordEncoder";
    private static final String MIME_TYPE = "video/avc";
    private static final int FRAME_RATE = 25;
    private static final int I_FRAME_INTERVAL = 5;
    private static final int TIMEOUT_US = 10000;
    private static final long RECORD_TOTAL_TIME = 15 * 1000;

    private MediaCodec mMediaCodec;
    private MediaMuxer mMediaMuxer;
    private VirtualDisplay mVirtualDisplay;
    private Surface mSurface;
    private EncodeThread mEncodeThread;

    public RecordEncoder(Context context,
                  int width,
                  int height,
                  int bitrate,
                  String recordPath) {
        try {
            prepareEncoder(width, height, bitrate);
            mMediaMuxer = new MediaMuxer(recordPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            DisplayManager mDisplayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            int flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION |
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
            if (mDisplayManager != null) {
                mVirtualDisplay = mDisplayManager.createVirtualDisplay("-display-",
                        width, height, DisplayMetrics.DENSITY_HIGH, mSurface, flags);
            }
            Log.d(TAG, "created virtual display: " + mVirtualDisplay);
        } catch (IOException e) {
            Log.e(TAG, "RecordEncoder: ", e);
            releaseEncoder();
        }
    }

    public void encode() {
        Log.i(TAG, "encode: start");
        mEncodeThread = new EncodeThread(false);
        new Thread(mEncodeThread).start();
    }

    private void prepareEncoder(int width, int height, int bitrate) throws IOException {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
        Log.d(TAG, "created video format: " + format);
        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mSurface = mMediaCodec.createInputSurface();
        Log.d(TAG, "created input surface: " + mSurface);
    }

    private void releaseEncoder() {
        if (mEncodeThread != null && !mEncodeThread.mQuit.get()) {
            mEncodeThread.mQuit = new AtomicBoolean(true);
        }
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
        }
        if (mMediaMuxer != null) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mMediaMuxer = null;
        }
    }

    private class EncodeThread implements Runnable {
        AtomicBoolean mQuit;
        long startTime;
        MediaCodec.BufferInfo mBufferInfo;
        boolean mMuxerStarted;
        int mVideoTrackIndex;


        EncodeThread(boolean quit) {
            mQuit = new AtomicBoolean(quit);
            startTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            mMediaCodec.start();
            ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            mBufferInfo = new MediaCodec.BufferInfo();
            while (!mQuit.get() && calculateEncodingTime() < RECORD_TOTAL_TIME) {
                int encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    Log.i(TAG, "no output from encoder available");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    Log.d(TAG, "encoder output buffers changed");
                    encoderOutputBuffers = mMediaCodec.getOutputBuffers();
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (mMuxerStarted) {
                        throw new IllegalStateException("output format already changed!");
                    }
                    MediaFormat newFormat = mMediaCodec.getOutputFormat();
                    Log.i(TAG, "output format changed.\n new format: " + newFormat.toString());
                    mVideoTrackIndex = mMediaMuxer.addTrack(newFormat);
                    mMediaMuxer.start();
                    mMuxerStarted = true;
                    Log.i(TAG, "started media muxer, videoIndex=" + mVideoTrackIndex);
                } else if (encoderStatus < 0) {
                    Log.e(TAG, "unexpected result from encoder.dequeueOutputBuffer: "
                            + encoderStatus);
                } else {
                    if (!mMuxerStarted) {
                        throw new IllegalStateException("MediaMuxer dose not call addTrack(format) ");
                    }
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    encodeToVideoTrack(encodedData);
                    mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                }
            }
            Log.i(TAG, "stop EncodeThread");
            releaseEncoder();
        }

        private void encodeToVideoTrack(ByteBuffer buffer) {
            Log.i(TAG, "encodeToVideoTrack: start");
            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                mBufferInfo.size = 0;
            }
            if (mBufferInfo.size == 0) {
                Log.d(TAG, "info.size == 0, drop it.");
                buffer = null;
            } else {
                Log.d(TAG, "got buffer, info: size=" + mBufferInfo.size
                        + ", presentationTimeUs=" + mBufferInfo.presentationTimeUs
                        + ", offset=" + mBufferInfo.offset);
            }
            if (buffer != null) {
                buffer.position(mBufferInfo.offset);
                buffer.limit(mBufferInfo.offset + mBufferInfo.size);
                mMediaMuxer.writeSampleData(mVideoTrackIndex, buffer, mBufferInfo);
                Log.i(TAG, "sent " + mBufferInfo.size + " bytes to muxer...");
            }
        }

        private long calculateEncodingTime() {
            long currentTime = System.currentTimeMillis();
            return currentTime - startTime;
        }
    }
}
