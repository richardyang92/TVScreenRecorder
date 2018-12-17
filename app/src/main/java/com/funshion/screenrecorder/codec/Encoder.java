package com.funshion.screenrecorder.codec;

import java.io.IOException;

public interface Encoder {
    void prepare() throws IOException;
    void encode();
    void release();
}
