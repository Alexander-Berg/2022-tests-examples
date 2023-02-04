package ru.yandex.webmaster3.storage.verification;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.core.host.verification.VerificationType;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.core.user.UserVerifiedHost;

/**
 * @author avhaliullin
 */
public class VerificationInheritanceUtilTest {

    @Test
    public void inheritableHostnamePredicate() throws Exception {
        testPredicate("domain.ru", "sub.domain.ru", true, "from super domain");
        testPredicate("domain.ru", "much.far.sub.domain.ru", true, "from far super domain");
        testPredicate("domain.ru", "www.domain.ru", true, "to www from non-www");
        testPredicate("www.domain.ru", "domain.ru", true, "to non-www from www");
        testPredicate("https://domain.ru", "domain.ru", true, "to http from https");
        testPredicate("domain.ru", "https://domain.ru", true, "to https from http");

        testPredicate("domain.ru", "sub.domain.com", false, "from unrelated domain");
        testPredicate("sub1.domain.ru", "sub2.domain.ru", false, "from non-www neighbour domain");
        testPredicate("sub.domain.ru", "domain.ru", false, "from subdomain to parent");
        testPredicate("domain.ru", "subdomain.ru", false, "from unrelated domain which is a suffix for test domain");
    }

    private void testPredicate(String verifiedHost, String inheritorHost, boolean expectInherited, String comment) {
        UserVerifiedHost userVerifiedHost = toVerifiedHost(verifiedHost);
        Assert.assertEquals(
                (expectInherited ? "Should be inherited " : "Should NOT be inherited ") + comment,
                expectInherited,
                VerificationInheritanceUtil
                        .inheritableHostnamePredicate(IdUtils.urlToHostId(inheritorHost))
                        .test(userVerifiedHost)
        );
    }

    @Test
    public void inheritableHostsComparator() throws Exception {
        testComparator("sub.domain.com", "www.sub.domain.com", "www.domain.com");
        testComparator("sub.domain.com", "https://sub.domain.com", "https://domain.com");
        testComparator("sub.domain.com", "domain.com", "https://domain.com");
        testComparator("sub.domain.com", "domain.com", "www.domain.com");
    }

    private void testComparator(String addingHost, String betterMatch, String worseMatch) {
        Assert.assertEquals(
                "When adding " + addingHost + ", host " + betterMatch + " is better match than " + worseMatch,
                -1,
                VerificationInheritanceUtil.inheritableHostsComparator(IdUtils.urlToHostId(addingHost))
                        .compare(toVerifiedHost(betterMatch), toVerifiedHost(worseMatch))
        );
    }

    private UserVerifiedHost toVerifiedHost(String hostName) {
        return new UserVerifiedHost(IdUtils.urlToHostId(hostName), DateTime.now(),
                DateTime.now(), 123L, VerificationType.META_TAG);
    }

}
