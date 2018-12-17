package com.funshion.screenrecorder.codec;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import com.funshion.screenrecorder.util.RecordConst;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VEncoder implements Encoder {
    private static final String TAG = "VEncoder";

    public interface Callback {
        void onVideoFormatChanged(MediaFormat mediaFormat);
        void onVideoTrackEncoded(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo);
    }

    private MediaCodec mVCodec;
    private Surface mSurface;
    private MediaFormat mVFormat;
    private Callback mCallback;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaCodec.BufferInfo mAVCBufferInfo;
    private ByteBuffer[] mEncoderOutputBuffers;

    VEncoder(VFormat vFormat) {
        mVFormat = vFormat.toMediaFormat();
    }

    void setMediaProjection(MediaProjection mediaProjection) {
        mMediaProjection = mediaProjection;
    }

    void addCallback(Callback callback) {
        mCallback = callback;
    }

    @Override
    public void prepare() throws IOException {
        mVCodec = MediaCodec.createEncoderByType(RecordConst.VIDEO_MIME_TYPE);
        mVCodec.configure(mVFormat, null, null,
                MediaCodec.CONFIGURE_FLAG_ENCODE);
        mSurface = mVCodec.createInputSurface();
        Log.d(TAG, "created input surface: " + mSurface);
        if (mMediaProjection == null) {
            throw new NullPointerException("mMediaProjection == null");
        }
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG + "-display",
                mVFormat.getInteger(MediaFormat.KEY_WIDTH),
                mVFormat.getInteger(MediaFormat.KEY_HEIGHT),
                DisplayMetrics.DENSITY_DEFAULT,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mSurface, null, null);
        Log.d(TAG, "created virtual display: " + mVirtualDisplay);
        mVCodec.start();

        mEncoderOutputBuffers = mVCodec.getOutputBuffers();
        mAVCBufferInfo = new MediaCodec.BufferInfo();
    }

    @Override
    public void encode() {
            int encoderStatus =
                    mVCodec.dequeueOutputBuffer(
                            mAVCBufferInfo,
                            RecordConst.VIDEO_TIMEOUT_US);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                Log.i(TAG, "no output from encoder available");
            } else if (encoderStatus ==
                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                Log.d(TAG, "encoder output buffers changed");
                mEncoderOutputBuffers = mVCodec.getOutputBuffers();
            } else if (encoderStatus ==
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = mVCodec.getOutputFormat();
                Log.i(TAG, "output format changed.\n new format: "
                        + newFormat.toString());
                if (((Recorder) mCallback).isMuxerStarted()) {
                    throw new IllegalStateException("output format already changed!");
                }
                mCallback.onVideoFormatChanged(newFormat);
            } else if (encoderStatus < 0) {
                Log.e(TAG, "unexpected result from encoder.dequeueOutputBuffer: "
                        + encoderStatus);
            } else {
                if (((Recorder)
                        mCallback).isMuxerStarted()) {
                    ByteBuffer encodedData = mEncoderOutputBuffers[encoderStatus];
                    mCallback.onVideoTrackEncoded(encodedData, mAVCBufferInfo);
                    mVCodec.releaseOutputBuffer(encoderStatus, false);
                }
            }
    }

    @Override
    public void release() {
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
}
