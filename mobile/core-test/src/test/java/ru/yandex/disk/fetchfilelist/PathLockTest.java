package ru.yandex.disk.fetchfilelist;

import junit.framework.TestCase;
import ru.yandex.util.Path;

public class PathLockTest extends TestCase {

    private static final Path DIR = new Path("/disk/dir");
    private static final Path SUBDIR = new Path("/disk/dir/subdir");
    private static final Path NOTSUBDIR = new Path("/disk/dirsubdir");

    private static final boolean LOCKED = true;
    private static final boolean UNLOCKED = false;

    private PathLock lock;
    private PathLock.Lock plainLock;
    private PathLock.Lock recursiveLock;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        lock = new PathLock();
        System.out.println();
        plainLock = lock.getPlainLock();
        recursiveLock = lock.getRecursiveLock();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        lock = null;
        plainLock = null;
        recursiveLock = null;
    }

    public void testDirWriteShouldNotBeForbiddenWithoutLocking() {
        assertFalse(plainLock.isLocked(DIR));
    }

    public void testDirRecursiveWriteShouldNotBeForbiddenWithoutLocking() {
        assertFalse(recursiveLock.isLocked(DIR));
    }

    public void testLockPlainDir() {
        plainLock.lock(DIR);

        assertEquals(LOCKED, plainLock.isLocked(DIR));
        assertEquals(UNLOCKED, plainLock.isLocked(SUBDIR));

        assertEquals(LOCKED, recursiveLock.isLocked(DIR));
        assertEquals(UNLOCKED, recursiveLock.isLocked(SUBDIR));

        assertEquals(UNLOCKED, plainLock.isLocked(NOTSUBDIR));
        assertEquals(UNLOCKED, recursiveLock.isLocked(NOTSUBDIR));

    }

    public void testLockPlainSubdir() {
        plainLock.lock(SUBDIR);

        assertEquals(UNLOCKED, plainLock.isLocked(DIR));
        assertEquals(LOCKED, plainLock.isLocked(SUBDIR));

        assertEquals(LOCKED, recursiveLock.isLocked(DIR));
        assertEquals(LOCKED, recursiveLock.isLocked(SUBDIR));

        assertEquals(UNLOCKED, plainLock.isLocked(NOTSUBDIR));
        assertEquals(UNLOCKED, recursiveLock.isLocked(NOTSUBDIR));
    }

    public void testLockRecursiveDir() {
        recursiveLock.lock(DIR);

        assertEquals(LOCKED, plainLock.isLocked(DIR));
        assertEquals(LOCKED, plainLock.isLocked(SUBDIR));

        assertEquals(LOCKED, recursiveLock.isLocked(DIR));
        assertEquals(LOCKED, recursiveLock.isLocked(SUBDIR));

        assertEquals(UNLOCKED, plainLock.isLocked(NOTSUBDIR));
        assertEquals(UNLOCKED, recursiveLock.isLocked(NOTSUBDIR));
    }

    public void testLockRecursiveSubdir() {
        recursiveLock.lock(SUBDIR);

        assertEquals(UNLOCKED, plainLock.isLocked(DIR));
        assertEquals(LOCKED, plainLock.isLocked(SUBDIR));

        assertEquals(LOCKED, recursiveLock.isLocked(DIR));
        assertEquals(LOCKED, recursiveLock.isLocked(SUBDIR));

        assertEquals(UNLOCKED, plainLock.isLocked(NOTSUBDIR));
        assertEquals(UNLOCKED, recursiveLock.isLocked(NOTSUBDIR));
    }

}
