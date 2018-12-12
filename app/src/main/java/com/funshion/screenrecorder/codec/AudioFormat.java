package com.funshion.screenrecorder.codec;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import com.funshion.screenrecorder.util.RecordConst;

public class AudioFormat implements IFormat {
    private int audioChannel;
    private int audioSampleRate;
    private int audioBitrate;

    public int getAudioChannel() {
        return audioChannel;
    }

    public void setAudioChannel(int audioChannel) {
        this.audioChannel = audioChannel;
    }

    public int getAudioSampleRate() {
        return audioSampleRate;
    }

    public void setAudioSampleRate(int audioSampleRate) {
        this.audioSampleRate = audioSampleRate;
    }

    public int getAudioBitrate() {
        return audioBitrate;
    }

    public void setAudioBitrate(int audioBitrate) {
        this.audioBitrate = audioBitrate;
    }

    @Override
    public MediaFormat toMediaFormat() {
        MediaFormat format = MediaFormat.createAudioFormat(RecordConst.AUDIO_MIME_TYPE,
                audioSampleRate, audioChannel);
        format.setInteger(MediaFormat.KEY_BIT_RATE, audioBitrate);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        return format;
    }
}
