package ru.yandex.disk.fetchfilelist;

import org.junit.Test;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.sync.RemoteFileItem;
import ru.yandex.disk.Storage;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class RegularFileStorageCleanerTest extends SyncListenerTest {

    private Storage storage;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        storage = mock(Storage.class);
        final RegularFileStorageCleaner cleaner = new RegularFileStorageCleaner(storage);
        addSycListener(cleaner);
    }

    @Test
    public void testFileShouldBeDeletedWhenDeletedFromSource() throws Exception {
        addToDb(new DbFileItem("/disk/dir/file.txt", false, DiskItem.OfflineMark.MARKED, null, null));
        emulateSync(new DiskItem[] {});
        verify(storage).deleteFileOrFolder(eq("/disk/dir/file.txt"));
    }

    @Test
    public void testRegularFileShouldBeDeletedWhenChanged() throws Exception {
        addToDb(new DbFileItem("/disk/dir/file.txt", false, DiskItem.OfflineMark.NOT_MARKED, null, null));
        emulateSync(new RemoteFileItem("/disk/dir/file.txt", false, "TAG", 0));
        verify(storage).deleteFileOrFolder(eq("/disk/dir/file.txt"));
    }

    @Test
    public void testOfflineFileShouldNotBeDeletedWhenChanged() throws Exception {
        addToDb(new DbFileItem("/disk/dir/file.txt", false, DiskItem.OfflineMark.MARKED, null, null));
        emulateSync(new RemoteFileItem("/disk/dir/file.txt", false, "TAG", 0));
        verify(storage, never()).deleteFileOrFolder(eq("/disk/dir/file.txt"));
    }

    @Test
    public void testFileInOfflineDirShouldNotBeDeletedWhenChanged() throws Exception {
        addToDb(new DbFileItem("/disk/dir/file.txt", false, DiskItem.OfflineMark.IN_OFFLINE_DIRECTORY, null, null));
        emulateSync(new RemoteFileItem("/disk/dir/file.txt", false, "TAG", 0));
        verify(storage, never()).deleteFileOrFolder(eq("/disk/dir/file.txt"));
    }

    @Test
    public void testDirectoryShouldBeDeletedWhenBecameFile() throws Exception {
        addToDb(new DbFileItem("/disk/dir/filedir", true, DiskItem.OfflineMark.MARKED, null, null));
        emulateSync(new RemoteFileItem("/disk/dir/filedir", false, "TAG", 0));
        verify(storage).deleteFileOrFolder(eq("/disk/dir/filedir"));
    }

    @Test
    public void testFileShouldBeDeletedWhenBecameDirectory() throws Exception {
        addToDb(new DbFileItem("/disk/dir/filedir", false, DiskItem.OfflineMark.MARKED, null, null));
        emulateSync(new RemoteFileItem("/disk/dir/filedir", true, "TAG", 0));
        verify(storage).deleteFileOrFolder(eq("/disk/dir/filedir"));
    }
}
