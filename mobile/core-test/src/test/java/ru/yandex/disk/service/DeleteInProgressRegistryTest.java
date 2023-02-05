package ru.yandex.disk.service;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.disk.DeleteInProgressRegistry;

import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.disk.test.Assert2.assertThat;

public class DeleteInProgressRegistryTest {

    private static final String FILENAME_1 = "/disk/file.jpg";
    private static final String FILENAME_2 = "/disk/file2.jpg";

    private static DeleteInProgressRegistry deleteInProgressRegistry;

    @Before
    public void setup() {
        deleteInProgressRegistry = new DeleteInProgressRegistry();
    }

    @Test
    public void testBaseFunctionality() {
        checkFileNotInProgress(FILENAME_1);
        checkFileNotInProgress(FILENAME_2);
        deleteInProgressRegistry.add(FILENAME_1);
        checkFileInProgress(FILENAME_1);
        checkFileNotInProgress(FILENAME_2);
        deleteInProgressRegistry.remove(FILENAME_1);
        checkFileNotInProgress(FILENAME_1);
        checkFileNotInProgress(FILENAME_2);
    }

    @Test
    public void shouldCreateIndependentSnapshot() {
        checkFileNotInProgress(FILENAME_1);

        final DeleteInProgressRegistry emptySnaphot = deleteInProgressRegistry.captureSnapshot();
        deleteInProgressRegistry.add(FILENAME_1);
        final DeleteInProgressRegistry filledSnaphot = deleteInProgressRegistry.captureSnapshot();

        checkFileInProgress(FILENAME_1);
        checkFileNotInProgress(emptySnaphot, FILENAME_1);
        checkFileInProgress(filledSnaphot, FILENAME_1);

        deleteInProgressRegistry.remove(FILENAME_1);

        checkFileNotInProgress(FILENAME_1);
        checkFileNotInProgress(emptySnaphot, FILENAME_1);
        checkFileInProgress(filledSnaphot, FILENAME_1);

        checkFileNotInProgress(FILENAME_2);
        emptySnaphot.add(FILENAME_2);
        checkFileInProgress(emptySnaphot, FILENAME_2);
        checkFileNotInProgress(FILENAME_2);
        checkFileNotInProgress(filledSnaphot, FILENAME_2);
    }

    private void checkFileInProgress(final String filename) {
        checkFileInProgress(deleteInProgressRegistry, filename);
    }

    private void checkFileNotInProgress(final String filename) {
        checkFileNotInProgress(deleteInProgressRegistry, filename);
    }

    private void checkFileInProgress(final DeleteInProgressRegistry registry, final String filename) {
        assertThat(registry.shouldSkipFileSync(filename), equalTo(true));
    }

    private void checkFileNotInProgress(final DeleteInProgressRegistry registry, final String filename) {
        assertThat(registry.shouldSkipFileSync(filename), equalTo(false));
    }

}
