package ru.yandex.disk.remote.webdav;

import org.junit.Test;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.DiskItemFactory;
import ru.yandex.disk.util.Signal;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class MoveConflictCheckingTest {

    private Signal cancellationSignal = new Signal();

    @Test
    public void testNoConflicts(){

        List<DiskItem> filesToMove = new ArrayList<DiskItem>() {{
            add(DiskItemFactory.create("/disk/B"));
        }};

        ConflictChecker checker = new ConflictChecker(filesToMove, cancellationSignal);

        checker.handleFile(DiskItemFactory.create("/disk/A/b"));
        assertTrue(checker.shouldContinueParsing());
        checker.handleFile(DiskItemFactory.create("/disk/A/c"));
        assertTrue(checker.shouldContinueParsing());

        assertFalse(checker.thereAreConflicts());
    }

    @Test
    public void testExceptionOnConflict(){

        List<DiskItem> filesToMove = new ArrayList<DiskItem>() {{
            add(DiskItemFactory.create("/disk/B"));
        }};

        ConflictChecker checker = new ConflictChecker(filesToMove, cancellationSignal);

        checker.handleFile(DiskItemFactory.create("/disk/A/b"));
        assertTrue(checker.shouldContinueParsing());

        checker.handleFile(DiskItemFactory.create("/disk/A/B"));
        assertFalse(checker.shouldContinueParsing());

        assertTrue(checker.thereAreConflicts());
    }

}