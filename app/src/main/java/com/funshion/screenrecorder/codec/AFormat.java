package com.funshion.screenrecorder.codec;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import com.funshion.screenrecorder.util.RecordConst;

public class AFormat implements Format {
    private int audioChannel;
    private int audioSampleRate;
    private int audioBitrate;

    public void setAudioChannel(int audioChannel) {
        this.audioChannel = audioChannel;
    }

    public void setAudioSampleRate(int audioSampleRate) {
        this.audioSampleRate = audioSampleRate;
    }

    public void setAudioBitrate(int audioBitrate) {
        this.audioBitrate = audioBitrate;
    }

    @Override
    public MediaFormat toMediaFormat() {
        MediaFormat format = MediaFormat.createAudioFormat(
                RecordConst.AUDIO_MIME_TYPE,
                audioSampleRate, audioChannel);
        format.setInteger(MediaFormat.KEY_BIT_RATE,
                audioBitrate);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        return format;
    }
}
