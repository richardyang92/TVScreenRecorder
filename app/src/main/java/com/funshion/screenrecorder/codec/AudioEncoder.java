package com.funshion.screenrecorder.codec;

import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import com.funshion.screenrecorder.util.RecordConst;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioEncoder implements IEncoder {
    private static final String TAG = "AudioEncoder";
    private MediaCodec mACodec;
    private MediaFormat mAFormat;
    private AudioRecord mAudioRecord;
    private byte[] mABuffer;
    private AACEncodeThread mAACEncodeThread;

    public AudioEncoder(AudioFormat audioFormat) {
        mAFormat = audioFormat.toMediaFormat();
    }

    @Override
    public void prepare() throws IOException {
        mACodec = MediaCodec.createEncoderByType(RecordConst.AUDIO_MIME_TYPE);
        int flags = MediaCodec.CONFIGURE_FLAG_ENCODE;
        mACodec.configure(mAFormat, null, null, flags);
        mABuffer = new byte[RecordConst.AUDIO_FRAME_SIZE];
        int minBufferSize = AudioRecord.getMinBufferSize(
                mAFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                android.media.AudioFormat.CHANNEL_IN_STEREO,
                android.media.AudioFormat.ENCODING_PCM_16BIT);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.REMOTE_SUBMIX,
                mAFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                android.media.AudioFormat.CHANNEL_IN_STEREO,
                android.media.AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize * 2);
        Log.d(TAG, "create AudioRecord: " + mAudioRecord);
    }

    @Override
    public void encode() {
        mAACEncodeThread = new AACEncodeThread(false);
        new Thread(mAACEncodeThread).start();
    }

    @Override
    public void release() {
        if (mAACEncodeThread != null && !mAACEncodeThread.mAACQuit.get()) {
            mAACEncodeThread.mAACQuit = new AtomicBoolean(true);
        }
    }

    private class AACEncodeThread implements Runnable {
        private AtomicBoolean mAACQuit;
        long mAACStartTime;
        MediaCodec.BufferInfo mAACBufferInfo;

        AACEncodeThread(boolean quit) {
            mAACQuit = new AtomicBoolean(quit);
            mAACStartTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            mACodec.start();
            mAudioRecord.startRecording();
            while (!mAACQuit.get() && calculateEncodingTime() < RecordConst.RECORD_TOTAL_TIME) {
                int audioByteNums = mAudioRecord.read(mABuffer, 0, RecordConst.AUDIO_FRAME_SIZE);
                encodePCM2AAC(mABuffer, audioByteNums);
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
                            readBytes, System.nanoTime(),0);
                }
            }
            mAACBufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex;
            do {
                outputBufferIndex = mACodec.dequeueOutputBuffer(mAACBufferInfo,RecordConst.AUDIO_TIMEOUT_US);
                if (outputBufferIndex == MediaCodec. INFO_TRY_AGAIN_LATER) {
                    Log.i(TAG, "encodePCM2AAC: ");
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {

                } else if(outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                } else {
                    if ((mAACBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        mAACBufferInfo.size = 0;
                    }
                    if((mAACBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                        break;
                    }
                }
            } while (outputBufferIndex >= 0);
        }

        private long calculateEncodingTime() {
            long currentTime = System.currentTimeMillis();
            return currentTime - mAACStartTime;
        }
    }
}
