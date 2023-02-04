package ru.yandex.webmaster3.internal.user.domain;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.datastax.driver.core.utils.UUIDs;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.data.WebmasterUser;
import ru.yandex.webmaster3.core.host.verification.UserHostVerificationInfo;
import ru.yandex.webmaster3.core.host.verification.VerificationCausedBy;
import ru.yandex.webmaster3.core.host.verification.VerificationStatus;
import ru.yandex.webmaster3.core.host.verification.VerificationType;
import ru.yandex.webmaster3.core.host.verification.fail.NotApplicableFailInfo;
import ru.yandex.webmaster3.core.user.UserVerifiedHost;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.core.util.W3Collectors;
import ru.yandex.webmaster3.storage.host.service.MirrorService2;
import ru.yandex.webmaster3.storage.user.service.UserHostsService;

/**
 * @author avhaliullin
 */
public class DomainVerificationHelperServiceTest {
    private static final String DOMAIN = "example.com";

    private static final WebmasterHostId HTTP_HOST = IdUtils.urlToHostId("http://example.com");
    private static final WebmasterHostId HTTPS_HOST = IdUtils.urlToHostId("https://example.com");
    private static final WebmasterHostId HTTP_WWW_HOST = IdUtils.urlToHostId("http://www.example.com");
    private static final WebmasterHostId HTTPS_WWW_HOST = IdUtils.urlToHostId("https://www.example.com");

    private static final Set<WebmasterHostId> ALL_NOT_WWW = new HashSet<>(Arrays.asList(HTTP_HOST, HTTPS_HOST));
    private static final Set<WebmasterHostId> ALL_WWW = new HashSet<>(Arrays.asList(HTTP_WWW_HOST, HTTPS_WWW_HOST));
    private static final Set<WebmasterHostId> ALL_HTTPS = new HashSet<>(Arrays.asList(HTTPS_HOST, HTTPS_WWW_HOST));
    private static final Set<WebmasterHostId> ALL_HTTP = new HashSet<>(Arrays.asList(HTTP_HOST, HTTP_WWW_HOST));
    private static final Set<WebmasterHostId> ALL_HOSTS = new HashSet<>(Arrays.asList(HTTP_HOST, HTTP_WWW_HOST,
            HTTPS_HOST, HTTPS_WWW_HOST));

    @Test
    public void testSingleVerifiedHostShouldBeUsed() {
        for (WebmasterHostId hostId : ALL_HOSTS) {
            DomainVerificationHelperService service = prepare(1, map(hostId, true), map());
            DomainVerificationState verificationState = service.getDomainVerificationState(1, DOMAIN);
            Assert.assertEquals(verificationState.getPreferredHost(), hostId);
            Assert.assertTrue(verificationState.isVerified(verificationState.getPreferredHost()));
        }
    }

    @Test
    public void testVerifiedHostShouldBePreffered() {
        for (WebmasterHostId hostId : ALL_HOSTS) {
            Map<WebmasterHostId, Boolean> verifiedMap = ALL_HOSTS.stream().map(h -> {
                boolean verified = h.equals(hostId);
                return Pair.of(h, verified);
            }).collect(W3Collectors.toHashMap());
            DomainVerificationHelperService service = prepare(1, verifiedMap, map());
            DomainVerificationState verificationState = service.getDomainVerificationState(1, DOMAIN);
            Assert.assertEquals(verificationState.getPreferredHost(), hostId);
            Assert.assertTrue(verificationState.isVerified(verificationState.getPreferredHost()));
        }
    }

    /*
     * Доя будущих поколений - "mainest mirror" - это такое главное зеркало, которое большее количество сайтов из
     * множества считают своим главным
     * Информация о том, что example.com является себе главным зеркалом - довольно мало говорит о вероятности
     * существования example.com как такогого,
     * в то время как знание о том, что www.example.com считает своим главным example.com - уже дает значительную
     * вероятность того, что example.com существует
     */
    @Test
    public void testMainestMirrorShouldBePreffered() {
        for (int verifiedFlag = 0; verifiedFlag < 2; verifiedFlag++) {
            boolean verified = verifiedFlag == 1;
            for (WebmasterHostId hostId : ALL_HOSTS) {
                Set<WebmasterHostId> slaveMirrors = new HashSet<>(ALL_HOSTS);
                slaveMirrors.remove(hostId);
                slaveMirrors.remove(slaveMirrors.iterator().next()); // запилим еще один сайт, который будет считать
                // себя главным
                Map<WebmasterHostId, WebmasterHostId> mirrors =
                        slaveMirrors.stream().map(h -> Pair.of(h, hostId)).collect(W3Collectors.toHashMap());
                DomainVerificationHelperService service = prepare(1, allAdded(ALL_HOSTS, verified), mirrors);
                DomainVerificationState verificationState = service.getDomainVerificationState(1, DOMAIN);
                Assert.assertEquals(verificationState.getPreferredHost(), hostId);
                Assert.assertEquals(verified, verificationState.isVerified(verificationState.getPreferredHost()));
            }
        }
    }

    /*
     * Если все сайты не подтверждены - мы ничего не знаем о вероятности их существования
     * Вероятность получить ответ по http от случайного сайта - выше, ибо даже у https сайтов обычно есть http,
     * который редиректит
     */
    @Test
    public void testHttpShouldBePrefferedIfNotVerified() {
        boolean verified = false;
        DomainVerificationHelperService service = prepare(1, allAdded(ALL_HOSTS, verified), map());
        DomainVerificationState verificationState = service.getDomainVerificationState(1, DOMAIN);
        Assert.assertTrue(ALL_HTTP.contains(verificationState.getPreferredHost()));
        Assert.assertEquals(verified, verificationState.isVerified(verificationState.getPreferredHost()));
    }

    /*
     * Если все сайты подтверждены - то тот, который https - с большей вероятностью главный
     */
    @Test
    public void testHttpsShouldBePrefferedIfVerified() {
        boolean verified = true;
        DomainVerificationHelperService service = prepare(1, allAdded(ALL_HOSTS, verified), map());
        DomainVerificationState verificationState = service.getDomainVerificationState(1, DOMAIN);
        Assert.assertTrue(ALL_HTTPS.contains(verificationState.getPreferredHost()));
        Assert.assertEquals(verified, verificationState.isVerified(verificationState.getPreferredHost()));
    }

    /*
     * Просто по статистике у нас больше не-www
     */
    @Test
    public void testNoWwwShouldBePreffered() {
        for (int verifiedFlag = 0; verifiedFlag < 2; verifiedFlag++) {
            boolean verified = verifiedFlag == 1;
            DomainVerificationHelperService service = prepare(1, allAdded(ALL_HOSTS, verified), map());
            DomainVerificationState verificationState = service.getDomainVerificationState(1, DOMAIN);
            Assert.assertTrue(ALL_NOT_WWW.contains(verificationState.getPreferredHost()));
            Assert.assertEquals(verified, verificationState.isVerified(verificationState.getPreferredHost()));
        }
    }

    static Map<WebmasterHostId, Boolean> allAdded(Collection<WebmasterHostId> hosts, boolean verified) {
        return hosts.stream().map(h -> Pair.of(h, verified)).collect(W3Collectors.toHashMap());
    }

    static DomainVerificationHelperService prepare(long userId, Map<WebmasterHostId, Boolean> verifiedState,
                                                   Map<WebmasterHostId, WebmasterHostId> mirrors) {
        UserHostsServiceMock userHostsServiceMock = new UserHostsServiceMock(userId, verifiedState);
        MirrorService2Mock mockMirrorService = new MirrorService2Mock(mirrors);
        DomainVerificationHelperService service = new DomainVerificationHelperService();
        service.setMirrorService2(mockMirrorService);
        service.setUserHostsService(userHostsServiceMock);
        return service;
    }

    static <K, V> MapBuilder<K, V> map() {
        return new MapBuilder<>();
    }

    static <K, V> MapBuilder<K, V> map(K k, V v) {
        MapBuilder<K, V> res = new MapBuilder<>();
        return res.with(k, v);
    }

    static class MapBuilder<K, V> extends HashMap<K, V> {
        public MapBuilder<K, V> with(K k, V v) {
            put(k, v);
            return this;
        }
    }

    static class UserHostsServiceMock extends UserHostsService {
        private final long userId;
        private final Map<WebmasterHostId, Boolean> verifiedState;

        public UserHostsServiceMock(long userId, Map<WebmasterHostId, Boolean> verifiedState) {
            super(null, null, null, null, null, null, null,   null, null, null);
            this.userId = userId;
            this.verifiedState = verifiedState;
        }

        @Override
        public List<UserVerifiedHost> getVerifiedHosts(WebmasterUser user, Collection<WebmasterHostId> hostIds) {
            if (user.getUserId() != userId) {
                return Collections.emptyList();
            }
            return verifiedState.entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .filter(e -> hostIds.contains(e.getKey()))
                    .map(e -> new UserVerifiedHost(
                            e.getKey(),
                            DateTime.now(),
                            DateTime.now(),
                            ThreadLocalRandom.current().nextLong(),
                            VerificationType.META_TAG
                    ))
                    .collect(Collectors.toList());
        }

        @Override
        public UserHostVerificationInfo getVerificationInfo(long userId, WebmasterHostId hostId) {
            if (userId != this.userId) {
                return null;
            }
            Boolean verified = verifiedState.get(hostId);
            if (verified == null) {
                return null;
            }
            return new UserHostVerificationInfo(
                    UUIDs.timeBased(),
                    userId,
                    hostId,
                    ThreadLocalRandom.current().nextLong(),
                    VerificationType.META_TAG,
                    verified ? VerificationStatus.VERIFIED : VerificationStatus.VERIFICATION_FAILED,
                    0,
                    verified ? null : new NotApplicableFailInfo(),
                    null,
                    null,
                    true,
                    VerificationCausedBy.INITIAL_VERIFICATION
            );
        }
    }

    static class MirrorService2Mock extends MirrorService2 {
        private final Map<WebmasterHostId, WebmasterHostId> mirrors;


        public MirrorService2Mock(Map<WebmasterHostId, WebmasterHostId> mirrors) {
            super(null, null);
            this.mirrors = mirrors;
        }

        @Override
        public Map<WebmasterHostId, WebmasterHostId> getMainMirrorsFromAllMirrors(Collection<WebmasterHostId> hosts) {
            return hosts.stream()
                    .collect(Collectors.toMap(h -> h, h -> mirrors.getOrDefault(h, h)));
        }
    }
}
