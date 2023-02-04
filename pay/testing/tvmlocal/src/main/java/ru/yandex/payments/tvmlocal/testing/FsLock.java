package ru.yandex.payments.tvmlocal.testing;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
class FsLock implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;

    @SneakyThrows
    static FsLock open(Path file) {
        val channel = new RandomAccessFile(file.toFile(), "rw").getChannel();
        try {
            val lock = channel.lock();
            return new FsLock(channel, lock);
        } catch (Throwable e) {
            channel.close();
            throw e;
        }
    }

    @SneakyThrows
    @Override
    public void close() {
        try {
            lock.close();
        } finally {
            channel.close();
        }
    }
}
