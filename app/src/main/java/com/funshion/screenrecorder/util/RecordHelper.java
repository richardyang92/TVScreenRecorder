package com.funshion.screenrecorder.util;

public class RecordHelper {
    public static long calculateEncodingTime(long startTime) {
        long currentTime = System.currentTimeMillis();
        return currentTime - startTime;
    }
}
