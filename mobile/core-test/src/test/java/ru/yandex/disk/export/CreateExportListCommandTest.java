package ru.yandex.disk.export;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.DiskItemFactory;
import ru.yandex.disk.Storage;
import ru.yandex.disk.event.DiskEvents;
import ru.yandex.disk.event.EventLogger;
import ru.yandex.disk.mocks.FakeRemoteRepo;
import ru.yandex.disk.provider.DiskDatabase;
import ru.yandex.disk.provider.DiskItemBuilder;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class CreateExportListCommandTest {

    private CreateExportListCommand command;
    private Storage storage;
    private EventLogger eventLogger;
    private FakeRemoteRepo fakeRemoteRepo;

    @Before
    public void setUp() throws Exception {
        storage = mock(Storage.class);
        eventLogger = new EventLogger();
        fakeRemoteRepo = new FakeRemoteRepo();
        command = new CreateExportListCommand(storage, fakeRemoteRepo, eventLogger,
                mock(DiskDatabase.class));
    }

    @Test
    public void shouldCorrectlyCreateDestinationPath() throws Exception {
        File dirToSave = new File("/sdcard");
        List<DiskItem> files = Collections.singletonList(DiskItemFactory.create("/disk/1"));
        command.execute(new CreateExportListCommandRequest(dirToSave, files, false, ConflictPolicy.USER));

        ExportList exportList = eventLogger.findByClass(DiskEvents.CreatingExportListFinished.class)
                .getExportList();

        String exportPath = exportList.getExportedFileInfos().get(0).getDestFile().getPath();
        assertThat(exportPath, equalTo("/sdcard/1"));
    }

    @Test
    public void shouldHoldDirectoryStructure() throws Exception {
        fakeRemoteRepo.addDiskItems(DiskItemFactory.create("/disk/A/a"));
        File dirToSave = new File("/sdcard");
        DiskItem dirA = builder().setPath("/disk/A").setIsDir(true).build();
        List<DiskItem> files = Collections.singletonList(dirA);
        command.execute(new CreateExportListCommandRequest(dirToSave, files, false, ConflictPolicy.USER));

        ExportList exportList = eventLogger.findByClass(DiskEvents.CreatingExportListFinished.class)
                .getExportList();

        String exportPath = exportList.getExportedFileInfos().get(0).getDestFile().getPath();
        assertThat(exportPath, equalTo("/sdcard/A/a"));
    }

    @Test
    public void shouldHoldDirectoryStructure2() throws Exception {
        final DiskItem folderAB = builder().setPath("/disk/A/B").setIsDir(true).build();
        final DiskItem fileABa = builder().setPath("/disk/A/B/a").build();
        fakeRemoteRepo.addDiskItems(folderAB, fileABa);

        File dirToSave = new File("/sdcard");
        DiskItem dirA = builder().setPath("/disk/A").setIsDir(true).build();

        command.execute(new CreateExportListCommandRequest(dirToSave, Collections.singletonList(dirA), false, ConflictPolicy.USER));
        ExportList exportList = eventLogger.findByClass(DiskEvents.CreatingExportListFinished.class)
                .getExportList();

        List<ExportedFileInfo> exportedFileInfos = exportList.getExportedFileInfos();
        String exportPathAB = exportedFileInfos.get(0).getDestFile().getPath();
        assertThat(exportPathAB, equalTo("/sdcard/A/B"));
        String exportPathABa = exportedFileInfos.get(1).getDestFile().getPath();
        assertThat(exportPathABa, equalTo("/sdcard/A/B/a"));
    }

    private static DiskItemBuilder builder() {
        return new DiskItemBuilder();
    }

    @Test
    public void shouldAllowToSaveSmallFile() throws Exception {
        fakeRemoteRepo.addDiskItems(
                builder()
                        .setPath("/disk/A/a")
                        .setSize(512)
                        .build()
        );

        File dirToSave = new File("/sdcard");
        when(storage.getFreeSpaceLimited(dirToSave, 512)).thenReturn(1024L);

        DiskItem dirA = builder()
                .setPath("/disk/A")
                .setIsDir(true)
                .build();
        command.execute(new CreateExportListCommandRequest(dirToSave, Collections.singletonList(dirA), false, ConflictPolicy.USER));
    }

    @Test
    public void shouldThrowStorageLimitReachedExceptionOnDownloadTooBigFiles() throws Exception {
        DiskItemBuilder builder = builder()
                .setSize(512);
        final DiskItem fileA = builder.setPath("/disk/A/a").build();
        final DiskItem fileB = builder.setPath("/disk/A/b").build();
        fakeRemoteRepo.addDiskItems(fileA, fileB);

        File dirToSave = new File("/sdcard");
        when(storage.getFreeSpaceLimited(dirToSave, 1024)).thenReturn(1023L);

        DiskItem dirA = builder()
                .setPath("/disk/A")
                .setIsDir(true)
                .build();

        command.execute(new CreateExportListCommandRequest(dirToSave,
                Collections.singletonList(dirA), false, ConflictPolicy.USER));
        assertThat(eventLogger.findByClass(DiskEvents.StorageLimitReached.class), notNullValue());
    }
}
