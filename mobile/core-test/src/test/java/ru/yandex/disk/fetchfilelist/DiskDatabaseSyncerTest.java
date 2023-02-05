package ru.yandex.disk.fetchfilelist;

import com.google.common.collect.Lists;
import org.junit.Test;
import ru.yandex.disk.provider.DiskItemBuilder;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.sync.RemoteFileItem;
import ru.yandex.disk.sync.SyncListener;

import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;

public class DiskDatabaseSyncerTest extends SyncListenerTest {

    private List<String> createdFiles;
    private List<String> changedFiles;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createdFiles = Lists.newArrayList();
        changedFiles = Lists.newArrayList();
        addSycListener(new SyncListener.SimpleSyncListener<DbFileItem>() {
            @Override
            public void onFileCreated(RemoteFileItem remoteFileItem) {
                createdFiles.add(remoteFileItem.getPath());
            }

            @Override
            public void onFileChanged(DbFileItem dbFileItem, RemoteFileItem remoteFileItem) {
                changedFiles.add(dbFileItem.getPath());
            }
        });
    }

    @Test
    public void testShouldProcessNewFileItemsInAlphabeticalOrder() throws Exception {
        DiskItem fileA = new DiskItemBuilder().setPath("/disk/a").build();
        DiskItem fileB = new DiskItemBuilder().setPath("/disk/b").build();
        DiskItem fileC = new DiskItemBuilder().setPath("/disk/c").build();
        DiskItem fileD = new DiskItemBuilder().setPath("/disk/d").build();

        emulateSync(fileA, fileC, fileB, fileD);

        assertThat(createdFiles, equalTo(asList("/disk/a", "/disk/b", "/disk/c", "/disk/d")));
    }

    @Test
    public void testShouldProcessNewFilesLevelByLevel() throws Exception {
        DiskItem file0 = new DiskItemBuilder().setPath("/disk/a").build();
        DiskItem file1 = new DiskItemBuilder().setPath("/disk/A/a").build();
        DiskItem file2 = new DiskItemBuilder().setPath("/disk/A/A/a").build();
        DiskItem file3 = new DiskItemBuilder().setPath("/disk/A/A/A/a").build();

        emulateSync(file0, file2, file3, file1);

        assertThat(createdFiles, equalTo(asList("/disk/a", "/disk/A/a", "/disk/A/A/a", "/disk/A/A/A/a")));
    }

    @Test
    public void testShouldProcessChangedFileItemsInAlphabeticalOrder() throws Exception {
        addToDb(new DiskItemBuilder().setPath("/disk/a").setEtag("OLD").build());
        addToDb(new DiskItemBuilder().setPath("/disk/b").setEtag("OLD").build());
        addToDb(new DiskItemBuilder().setPath("/disk/c").setEtag("OLD").build());
        addToDb(new DiskItemBuilder().setPath("/disk/d").setEtag("OLD").build());

        DiskItem fileA = new DiskItemBuilder().setPath("/disk/a").setEtag("NEW").build();
        DiskItem fileB = new DiskItemBuilder().setPath("/disk/b").setEtag("NEW").build();
        DiskItem fileC = new DiskItemBuilder().setPath("/disk/c").setEtag("NEW").build();
        DiskItem fileD = new DiskItemBuilder().setPath("/disk/d").setEtag("NEW").build();

        emulateSync(fileA, fileC, fileB, fileD);

        assertThat(changedFiles, equalTo(asList("/disk/a", "/disk/b", "/disk/c", "/disk/d")));
    }

    @Test
    public void testShouldReleaseDatabaseIfCommitDoesNotCalled() throws Exception {
        addToDb(new DiskItemBuilder().setPath("/disk/a").setEtag("OLD").build());

        DiskItem fileA = new DiskItemBuilder().setPath("/disk/a").setEtag("NEW").build();
        emulateSyncInterrupted(fileA);
    }

}
