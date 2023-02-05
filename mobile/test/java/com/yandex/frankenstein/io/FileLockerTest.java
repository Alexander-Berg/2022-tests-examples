package com.yandex.frankenstein.io;

import org.junit.After;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class FileLockerTest {

    private final File mLockerFile = new ResourceReader().getFile("locker");

    final FileLocker mFileLocker = new FileLocker(mLockerFile);
    final FileLocker mFileLocker2 = new FileLocker(mLockerFile);

    @After
    public void unlockLockers() {
        mFileLocker.unlock();
        mFileLocker2.unlock();
    }

    @Test
    public void testLock() {
        assertThat(mFileLocker.lock()).isTrue();
    }

    @Test
    public void testUnlockAfterLock() {
        mFileLocker.lock();

        assertThat(mFileLocker.unlock()).isTrue();
    }

    @Test
    public void testUnlockBeforeLock() {
        assertThat(mFileLocker.unlock()).isFalse();
    }

    @Test
    public void testDoubleLock() {
        assertThat(mFileLocker.lock()).isTrue();
        assertThat(mFileLocker.lock()).isTrue();
    }

    @Test
    public void testTwoLocksLock() {
        assertThat(mFileLocker.lock()).isTrue();
        assertThat(mFileLocker2.lock()).isFalse();
    }

    @Test
    public void testLockOnNewFile() {
        final File tempLockerFile = new File("tmp_locker");
        assertThat(tempLockerFile.exists()).isFalse();
        tempLockerFile.deleteOnExit();
        final FileLocker mFileLocker = new FileLocker(tempLockerFile);

        assertThat(mFileLocker.lock()).isTrue();
        assertThat(mFileLocker.unlock()).isTrue();
    }
}
