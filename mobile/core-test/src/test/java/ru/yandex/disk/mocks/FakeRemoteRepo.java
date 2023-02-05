package ru.yandex.disk.mocks;

import ru.yandex.disk.AppStartSessionProvider;
import ru.yandex.disk.Credentials;
import ru.yandex.disk.DeveloperSettings;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.SortOrder;
import ru.yandex.disk.remote.FileParsingHandler;
import ru.yandex.disk.remote.RemoteRepo;
import ru.yandex.disk.remote.RestApiClient;
import ru.yandex.disk.remote.exceptions.RemoteExecutionException;
import ru.yandex.disk.remote.webdav.WebdavClient;
import ru.yandex.disk.toggle.SeparatedAutouploadToggle;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static ru.yandex.util.Path.asPath;

public class FakeRemoteRepo extends RemoteRepo {
    private final List<DiskItemInfo> diskItemInfos = new ArrayList<>();

    public FakeRemoteRepo() {
        super(mock(Credentials.class), mock(WebdavClient.Pool.class), mock(RestApiClient.class), mock(DeveloperSettings.class), new SeparatedAutouploadToggle(false),
            mock(AppStartSessionProvider.class));
    }

    public FakeRemoteRepo(WebdavClient.Pool webdavClientPool) {
        super(mock(Credentials.class), webdavClientPool, mock(RestApiClient.class), mock(DeveloperSettings.class), new SeparatedAutouploadToggle(false),
            mock(AppStartSessionProvider.class));
    }

    @Override
    public void getFileList(final String dir, final int itemsOnPage, final SortOrder sortOrder,
                            final FileParsingHandler fileParsingHandler)
            throws RemoteExecutionException {
        fileParsingHandler.parsingStarted();
        for (DiskItemInfo info : diskItemInfos) {
            if (info.exception != null) {
                throw info.exception;
            }
            if (asPath(info.item.getPath()).getParent().getPath().equals(dir)) {
                fileParsingHandler.handleFile(info.item);
            }
        }
        fileParsingHandler.parsingFinished();
    }

    public void addDiskItems(final DiskItem... items) {
        for (DiskItem item : items) {
            final DiskItemInfo info = new DiskItemInfo();
            info.item = item;
            diskItemInfos.add(info);
        }
    }

    public void throwException(final RemoteExecutionException e) {
        final DiskItemInfo info = new DiskItemInfo();
        info.exception = e;
        diskItemInfos.add(info);
    }

    private static class DiskItemInfo {
        DiskItem item;
        RemoteExecutionException exception;
    }
}
