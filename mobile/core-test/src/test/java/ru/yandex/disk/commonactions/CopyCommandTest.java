package ru.yandex.disk.commonactions;

import org.junit.Test;
import ru.yandex.disk.ApplicationStorage;
import ru.yandex.disk.CredentialsManager;
import ru.yandex.disk.FileItem;
import ru.yandex.disk.Mocks;
import ru.yandex.disk.download.DownloadQueue;
import ru.yandex.disk.event.EventSender;
import ru.yandex.disk.provider.DiskDatabaseMethodTest;
import ru.yandex.disk.provider.DiskItemBuilder;
import ru.yandex.disk.remote.RemoteRepo;
import ru.yandex.disk.service.CommandLogger;
import ru.yandex.disk.service.CommandStarter;
import ru.yandex.disk.settings.ApplicationSettings;
import ru.yandex.disk.test.SeclusiveContext;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.disk.upload.StorageListProviderStub;
import ru.yandex.disk.util.Diagnostics;
import ru.yandex.disk.util.FileContentAccessor;
import ru.yandex.disk.util.Signal;
import rx.schedulers.Schedulers;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static ru.yandex.disk.mocks.Stubber.stub;

public class CopyCommandTest extends DiskDatabaseMethodTest {
    private CopyCommand command;
    private FileContentAccessor storageTestHelper;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        final SeclusiveContext context = new SeclusiveContext(mContext);
        Mocks.addContentProviders(context);
        final DownloadQueue downloadQueue = TestObjectsFactory.createDownloadQueue(context);
        final ApplicationStorage storage = TestObjectsFactory.createApplicationStorage(context,
                stub(ApplicationSettings.class), stub(CredentialsManager.class),
                mock(CommandStarter.class),
                new StorageListProviderStub(), mock(Diagnostics.class));
        storageTestHelper = new FileContentAccessor(storage.getStoragePath());
        command = new CopyCommand(stub(RemoteRepo.class), stub(EventSender.class),
                new CommandLogger(), Schedulers.trampoline());
        storageTestHelper.clear();
    }

    @Test(expected = FileNotFoundException.class)
    public void shouldNotCopyFileInLocalCache() throws Exception {
        storageTestHelper.write("/disk/A/a", "DATA");

        command.execute(createRequest(asList("/disk/A/a"), "/disk/B", true));

        assertThat(storageTestHelper.read("/disk/A/a"), equalTo("DATA"));

        storageTestHelper.read("/disk/B/a");
    }

    @Test(expected = FileNotFoundException.class)
    public void shouldNotCopyDirectoryInLocalCache() throws Exception {
        storageTestHelper.write("/disk/A/a", "DATA");

        command.execute(createRequest(asList("/disk/A"), "/disk/B", true));

        assertThat(storageTestHelper.read("/disk/A/a"), equalTo("DATA"));

        storageTestHelper.read("/disk/B/A/a");
    }

    private CopyCommandRequest createRequest(final List<String> paths, final String dir,
                                             final boolean createDir) {
        final List<FileItem> items = new ArrayList<>(paths.size());
        for (final String path : paths) {
            items.add(new DiskItemBuilder().setPath(path).build());
        }
        return new CopyCommandRequest(items, dir, createDir, new Signal());
    }
}
