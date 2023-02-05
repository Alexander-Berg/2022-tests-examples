package com.yandex.mail.generators;

import androidx.annotation.NonNull;

@SuppressWarnings("checkstyle:magicnumber")
public class ContainersGenerator {
    /*
        Big numbers to avoid confusing these with database IDs
     */
    private long currentFid = 1000;
    private long currentLid = 2000;
    private long currentTid = 3000;

    @NonNull
    public String nextFid() {
        return Long.toString(currentFid++);
    }

    @NonNull
    public String nextLid() {
        return Long.toString(currentLid++);
    }

    @NonNull
    public String nextTid() {
        return Long.toString(currentTid++);
    }

}