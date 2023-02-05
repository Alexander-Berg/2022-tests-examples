package ru.yandex.disk.fetchfilelist;

import ru.yandex.util.Path;

import java.util.ArrayList;
import java.util.List;

public class PathLockSpy extends PathLock {
    private final class LockWrapper extends Lock {
        private final List<Path> log;
        private final Lock wrapee;

        public LockWrapper(Lock wrapee, List<Path> log) {
            this.wrapee = wrapee;
            this.log = log;
        }

        @Override
        boolean isLocked(Path path) {
            return wrapee.isLocked(path);
        }

        @Override
        public void lock(Path path) {
            log.add(path);
            wrapee.lock(path);
        }

        @Override
        public void unlock(Path dir) {
            wrapee.unlock(dir);
        }
    }

    private final List<Path> recursiveLockingLog = new ArrayList<>();
    private final List<Path> plainLockingLog = new ArrayList<>();

    @Override
    public Lock getPlainLock() {
        return new LockWrapper(super.getPlainLock(), plainLockingLog);
    }

    @Override
    public Lock getRecursiveLock() {
        return new LockWrapper(super.getRecursiveLock(), recursiveLockingLog);
    }

    @Override
    public void writePlain(Path path, Runnable r) {
        plainLockingLog.add(path);
        r.run();
    }

    @Override
    public void writeRecursive(Path path, Runnable r) {
        recursiveLockingLog.add(path);
        r.run();
    }

    public List<Path> getRecursiveLockingLog() {
        return recursiveLockingLog;
    }

    public List<Path> getPlainLockingLog() {
        return plainLockingLog;
    }

    public boolean isLocked(String path) {
        return isLocked(new Path(path));
    }

    public boolean isLocked(Path path) {
        return getPlainLock().isLocked(path) || getRecursiveLock().isLocked(path);
    }
}
