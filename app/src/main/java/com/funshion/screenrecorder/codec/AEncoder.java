package com.funshion.screenrecorder.codec;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import com.funshion.screenrecorder.util.RecordConst;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AEncoder implements Encoder {
    private static final String TAG = "AEncoder";

    public interface Callback {
        void onAudioFormatChanged(MediaFormat mediaFormat);
        void onAudioTrackEncoded(ByteBuffer byteBuffer,
                                 MediaCodec.BufferInfo bufferInfo);
    }

    private MediaCodec mACodec;
    private MediaFormat mAFormat;
    private AudioRecord mAudioRecord;
    private byte[] mABuffer;
    private MediaCodec.BufferInfo mAACBufferInfo;
    private Callback mCallback;
    private long prevOutputPTSUs = 0;

    AEncoder(AFormat aFormat) {
        mAFormat = aFormat.toMediaFormat();
    }

    void addCallback(Callback callback) {
        mCallback = callback;
    }

    @Override
    public void prepare() throws IOException {
        int flags = MediaCodec.CONFIGURE_FLAG_ENCODE;
        mABuffer = new byte[RecordConst.AUDIO_FRAME_SIZE];
        int minBufferSize = AudioRecord.getMinBufferSize(
                mAFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                AudioFormat.CHANNEL_IN_DEFAULT,
                AudioFormat.ENCODING_PCM_16BIT);
        mAFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBufferSize * 2);
        mACodec = MediaCodec.createEncoderByType(RecordConst.AUDIO_MIME_TYPE);
        mACodec.configure(mAFormat, null, null, flags);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.REMOTE_SUBMIX,
                mAFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize * 2);
        Log.d(TAG, "create AudioRecord: " + mAudioRecord);
        mACodec.start();
        mAudioRecord.startRecording();
    }

    @Override
    public void encode() {
        Log.i(TAG, "encode: 1");
        int audioByteNums =
                mAudioRecord.read(
                        mABuffer,
                        0,
                        RecordConst.AUDIO_FRAME_SIZE);
        Log.i(TAG, "encode: 2");
        encodePCM2AAC(mABuffer, audioByteNums);
        Log.i(TAG, "encode: 3");
    }

    @Override
    public void release() {
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null;
        }
        if (mACodec != null) {
            mACodec.stop();
            mACodec.release();
            mACodec = null;
        }
    }

    private void encodePCM2AAC(byte[] audioBuff, int readBytes) {
        ByteBuffer[] inputBuffers = mACodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mACodec.getOutputBuffers();
        int inputBufferIndex = mACodec.dequeueInputBuffer(RecordConst.AUDIO_TIMEOUT_US);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer;
            inputBuffer = inputBuffers[inputBufferIndex];
            if (audioBuff == null || readBytes <= 0) {
                mACodec.queueInputBuffer(inputBufferIndex, 0, readBytes,
                        System.nanoTime(), 0);
            } else {
                inputBuffer.clear();
                inputBuffer.put(audioBuff);
                mACodec.queueInputBuffer(inputBufferIndex, 0,
                        readBytes, System.nanoTime(), 0);
            }
        }
        mAACBufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex;
        do {
            Log.i(TAG, "encodePCM2AAC......");
            outputBufferIndex = mACodec.dequeueOutputBuffer(mAACBufferInfo, RecordConst.AUDIO_TIMEOUT_US);
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                Log.i(TAG, "no output from encoder available");
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                Log.d(TAG, "encoder output buffers changed");
                outputBuffers = mACodec.getOutputBuffers();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = mACodec.getOutputFormat();
                Log.i(TAG, "output format changed.\n new format: "
                        + newFormat.toString());
                if (((Recorder) mCallback).isMuxerStarted()) {
                    throw new IllegalStateException("output format already changed!");
                }
                mCallback.onAudioFormatChanged(newFormat);
            } else {
                if ((mAACBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mAACBufferInfo.size = 0;
                }
                if ((mAACBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;
                }
                if (!((Recorder)
                        mCallback).isMuxerStarted()) {
                    continue;
                }
                ByteBuffer encodedData = outputBuffers[outputBufferIndex];
                mAACBufferInfo.presentationTimeUs = getPTSUs();
                mCallback.onAudioTrackEncoded(encodedData, mAACBufferInfo);
                prevOutputPTSUs = mAACBufferInfo.presentationTimeUs;
                mACodec.releaseOutputBuffer(outputBufferIndex, false);
            }
        } while (outputBufferIndex >= 0);
    }

    private long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        if (result < prevOutputPTSUs)
            result = (prevOutputPTSUs - result) + result;
        return result;

    }
}
