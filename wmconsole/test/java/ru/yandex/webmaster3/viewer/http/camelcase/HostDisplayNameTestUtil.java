package ru.yandex.webmaster3.viewer.http.camelcase;

import lombok.AllArgsConstructor;
import org.easymock.EasyMock;

import ru.yandex.webmaster3.storage.events.service.WMCEventsService;
import ru.yandex.webmaster3.storage.host.moderation.camelcase.dao.HostDisplayNameModerationRequestsYDao;
import ru.yandex.webmaster3.storage.host.moderation.camelcase.dao.HostDisplayNameModerationYtRequestsYDao;
import ru.yandex.webmaster3.storage.host.moderation.camelcase.dao.HostModeratedDisplayNameYDao;
import ru.yandex.webmaster3.storage.host.moderation.camelcase.service.DisplayNameService2;
import ru.yandex.webmaster3.storage.host.moderation.camelcase.service.HostDisplayNameService;
import ru.yandex.webmaster3.storage.host.service.HostService;

/**
 * @author avhaliullin
 */
public class HostDisplayNameTestUtil {
    public static Context createMockContext() {

        HostDisplayNameModerationRequestsYDao hostDisplayNameModerationRequestsYDao = EasyMock.createMock(HostDisplayNameModerationRequestsYDao.class);

        HostModeratedDisplayNameYDao hostModeratedDisplayNameYDao = EasyMock.createMock(HostModeratedDisplayNameYDao.class);

        HostService hostService = EasyMock.createMock(HostService.class);
        HostDisplayNameModerationYtRequestsYDao hhYDao = EasyMock.createMock(HostDisplayNameModerationYtRequestsYDao.class);
        DisplayNameService2 displayNameService2 = EasyMock.createMock(DisplayNameService2.class);
        WMCEventsService wmcEventsService = EasyMock.createMock(WMCEventsService.class);

        HostDisplayNameService hostDisplayNameService = new HostDisplayNameService(
                displayNameService2,
                hostDisplayNameModerationRequestsYDao,
                hhYDao,
                hostModeratedDisplayNameYDao,
                wmcEventsService
        );
        return new Context(hostDisplayNameService, hostDisplayNameModerationRequestsYDao, hostModeratedDisplayNameYDao, hostService, displayNameService2);
    }

    @AllArgsConstructor
    public static class Context {
        public final HostDisplayNameService hostDisplayNameService;

        public final HostDisplayNameModerationRequestsYDao mockHostDisplayNameModerationRequestsYDao;
        public final HostModeratedDisplayNameYDao mockHostModeratedDisplayNameYDao;
        public final HostService mockHostService;
        public final DisplayNameService2 displayNameService2;
    }
}
