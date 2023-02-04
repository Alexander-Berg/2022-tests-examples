package ru.yandex.webmaster3.storage.verification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.data.WebmasterUser;
import ru.yandex.webmaster3.core.host.verification.IUserHostVerifier;
import ru.yandex.webmaster3.core.host.verification.VerificationType;
import ru.yandex.webmaster3.core.user.UserVerifiedHost;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.events.service.WMCEventsService;
import ru.yandex.webmaster3.storage.user.dao.UserHostVerificationYDao;
import ru.yandex.webmaster3.storage.user.service.UserHostsService;

/**
 * @author avhaliullin
 */
public class HostVerifierServiceTest {
    private static final long UID = 120160451;

    private Context context;

    @Before
    public void setUp() throws Exception {
        Map<VerificationType, IUserHostVerifier> verifiersMap = new EnumMap<>(VerificationType.class);
        for (VerificationType verificationType : VerificationType.values()) {
            verifiersMap.put(verificationType, EasyMock.createMock(IUserHostVerifier.class));
        }
        context = new Context(
                verifiersMap,
                EasyMock.createMock(WMCEventsService.class),
                EasyMock.createMock(UserHostsService.class),
                EasyMock.createMock(UserHostVerificationYDao.class)
        );
    }

    @Test
    public void verifyHost() throws Exception {
        //TODO:
    }

    @Test
    public void relatedDomainVerificationShouldBeInherited() throws Exception {
        WebmasterUser user = new WebmasterUser(UID);

        WebmasterHostId verifiedHost = IdUtils.urlToHostId("http://domain.com");
        VerificationType verificationType = VerificationType.META_TAG;

        EasyMock.expect(context.userHostsService.getVerifiedHosts(user))
                .andReturn(Collections.singletonList(userVerifiedHost(verifiedHost, 123L, verificationType)));

        context.replayAll();

        Optional<HostVerifierService.InheritedVerificationInfo> resultOpt =
                context.hostVerifierService.inheritVerificationInfo(UID, IdUtils.urlToHostId("http://sub.domain.com"));

        Assert.assertTrue(resultOpt.isPresent());
        HostVerifierService.InheritedVerificationInfo result = resultOpt.get();
        Assert.assertEquals(verifiedHost, result.fromHostId);
        Assert.assertEquals(verificationType, result.verificationType);
        Assert.assertEquals(123L, result.verificationUin);
        context.verifyAll();
    }


    @Test
    public void uninheritableVerificationShouldNotBeInherited() throws Exception {
        WebmasterUser user = new WebmasterUser(UID);

        WebmasterHostId verifiedHost = IdUtils.urlToHostId("http://domain.com");

        EasyMock.expect(context.userHostsService.getVerifiedHosts(user))
                .andReturn(Collections.singletonList(userVerifiedHost(verifiedHost, 123L, VerificationType.SELF)));

        context.replayAll();

        Optional<HostVerifierService.InheritedVerificationInfo> result =
                context.hostVerifierService.inheritVerificationInfo(UID, IdUtils.urlToHostId("http://sub.domain.com"));

        Assert.assertEquals(Optional.empty(), result);
        context.verifyAll();
    }

    @Test
    public void unrelatedDomainVerificationShouldNotBeInherited() throws Exception {
        WebmasterUser user = new WebmasterUser(UID);

        WebmasterHostId verifiedHost = IdUtils.urlToHostId("http://verified.com");

        EasyMock.expect(context.userHostsService.getVerifiedHosts(user))
                .andReturn(Collections.singletonList(userVerifiedHost(verifiedHost, 123L, VerificationType.META_TAG)));

        context.replayAll();

        Optional<HostVerifierService.InheritedVerificationInfo> result =
                context.hostVerifierService.inheritVerificationInfo(UID, IdUtils.urlToHostId("http://not-verified.com"));

        Assert.assertEquals(Optional.empty(), result);
        context.verifyAll();
    }

    private static UserVerifiedHost userVerifiedHost(WebmasterHostId hostId, long uin, VerificationType verificationType) {
        return new UserVerifiedHost(hostId, DateTime.now(), DateTime.now(), uin, verificationType);
    }

    static class Context {
        final Map<VerificationType, IUserHostVerifier> hostVerifiersMap;
        final WMCEventsService wmcEventsService;
        final UserHostsService userHostsService;
        final UserHostVerificationYDao userHostVerificationYDao;

        final HostVerifierService hostVerifierService;

        private final List<Object> mocks;

        public Context(Map<VerificationType, IUserHostVerifier> hostVerifiersMap,
                       WMCEventsService wmcEventsService, UserHostsService userHostsService, UserHostVerificationYDao userHostVerificationYDao) {
            this.hostVerifiersMap = hostVerifiersMap;
            this.wmcEventsService = wmcEventsService;
            this.userHostsService = userHostsService;
            this.userHostVerificationYDao = userHostVerificationYDao;

            this.hostVerifierService = new HostVerifierService(
                    userHostsService,
                    userHostVerificationYDao,
                    wmcEventsService,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            this.hostVerifierService.setHostVerifiersMap(hostVerifiersMap);

            this.mocks = new ArrayList<>();
            mocks.add(userHostsService);
            mocks.add(wmcEventsService);
            mocks.add(userHostVerificationYDao);
            for (IUserHostVerifier verifier : hostVerifiersMap.values()) {
                mocks.add(verifier);
            }
        }

        void replayAll() {
            for (Object mock : mocks) {
                EasyMock.replay(mock);
            }
        }

        void verifyAll() {
            for (Object mock : mocks) {
                EasyMock.verify(mock);
            }
        }
    }
}
