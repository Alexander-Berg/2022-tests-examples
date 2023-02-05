package ru.yandex.disk.commonactions;

import ru.yandex.disk.Credentials;
import ru.yandex.disk.DeveloperSettings;
import ru.yandex.disk.remote.webdav.WebdavClient;
import ru.yandex.disk.settings.AutoUploadSettings;
import ru.yandex.disk.toggle.SeparatedAutouploadToggle;

import javax.annotation.NonnullByDefault;

import static org.mockito.Mockito.mock;

@NonnullByDefault
public class SingleWebdavClientPool extends WebdavClient.Pool {

    private final WebdavClient client;

    public SingleWebdavClientPool(final WebdavClient client) {
        super(null, null, WebdavClient.WebdavConfig.DEFAULT_HOST,
            mock(AutoUploadSettings.class),
            new SeparatedAutouploadToggle(false),
            mock(DeveloperSettings.class));
        this.client = client;
    }

    @Override
    public WebdavClient getClient(final Credentials account, final WebdavClient.Op op) {
        return client;
    }

}
