package ru.yandex.webmaster3.storage.verification;

import junit.framework.TestCase;
import org.apache.commons.lang3.tuple.Pair;
import org.xbill.DNS.Type;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.host.verification.UinUtil;
import ru.yandex.webmaster3.core.host.verification.VerificationCausedBy;
import ru.yandex.webmaster3.core.host.verification.VerificationFailInfo;
import ru.yandex.webmaster3.core.host.verification.fail.DNSRecord;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.verification.dns.DnsLookupService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.webmaster3.storage.verification.dns.DnsLookupService.LookupStatus.HOST_NOT_FOUND;
import static ru.yandex.webmaster3.storage.verification.dns.DnsLookupService.LookupStatus.SUCCESSFUL;
import static ru.yandex.webmaster3.storage.verification.dns.DnsLookupService.LookupStatus.TYPE_NOT_FOUND;

/**
 * @author akhazhoyan 06/2018
 */
public class DNSVerifierTest extends TestCase {

    private DnsLookupService mockDnsLookupService;
    private DNSVerifier dnsVerifier;

    private static final String HOST = "yandex.ru";
    private static final WebmasterHostId HOST_ID = IdUtils.urlToHostId("http://" + HOST);
    private static final List<String> NAME_SERVERS = Collections.emptyList();
    private static final long VERIFICATION_UIN = 0xcafe_babe_dead_beefL;
    private static final String VERIFICATION_UIN_STRING = UinUtil.getUinString(VERIFICATION_UIN);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockDnsLookupService = mock(DnsLookupService.class);
        dnsVerifier = new DNSVerifier(mockDnsLookupService, NAME_SERVERS);
    }

    private DNSRecord dnsRecord(String data) {
        return new DNSRecord("abc", "NS", "IN", 123, data);
    }

    public void testVerifySuccess() {
        {
            when(mockDnsLookupService.lookup(NAME_SERVERS, HOST, Type.TXT)).thenReturn(Pair.of(SUCCESSFUL, Arrays.asList(
                    dnsRecord("somethingelse"),
                    dnsRecord(DNSVerifier.YANDEX_VERIFICATION_STRING + ": " + VERIFICATION_UIN_STRING)
            )));
            Optional<VerificationFailInfo> failInfo = dnsVerifier.verify(1, HOST_ID, null, VERIFICATION_UIN, VerificationCausedBy.INITIAL_VERIFICATION);
            assertFalse(failInfo.isPresent());
        }
        {
            when(mockDnsLookupService.lookup(NAME_SERVERS, HOST, Type.TXT)).thenReturn(Pair.of(SUCCESSFUL, Arrays.asList(
                    dnsRecord(DNSVerifier.YANDEX_VERIFICATION_STRING + ": " + VERIFICATION_UIN_STRING),
                    dnsRecord("somethingelse")
            )));
            Optional<VerificationFailInfo> failInfo = dnsVerifier.verify(1, HOST_ID, null, VERIFICATION_UIN, VerificationCausedBy.INITIAL_VERIFICATION);
            assertFalse(failInfo.isPresent());
        }
        {
            when(mockDnsLookupService.lookup(NAME_SERVERS, HOST, Type.TXT)).thenReturn(Pair.of(SUCCESSFUL, Arrays.asList(
                    dnsRecord(DNSVerifier.YANDEX_VERIFICATION_STRING + ": " + VERIFICATION_UIN_STRING)
            )));
            Optional<VerificationFailInfo> failInfo = dnsVerifier.verify(1, HOST_ID, null, VERIFICATION_UIN, VerificationCausedBy.INITIAL_VERIFICATION);
            assertFalse(failInfo.isPresent());
        }
        { // специальный случай: должно работать даже если uin не равен, а лишь встречается в записи
            when(mockDnsLookupService.lookup(NAME_SERVERS, HOST, Type.TXT)).thenReturn(Pair.of(SUCCESSFUL, Arrays.asList(
                    dnsRecord(DNSVerifier.YANDEX_VERIFICATION_STRING + ": " + VERIFICATION_UIN_STRING + "suffix")
            )));
            Optional<VerificationFailInfo> failInfo = dnsVerifier.verify(1, HOST_ID, null, VERIFICATION_UIN, VerificationCausedBy.INITIAL_VERIFICATION);
            assertFalse(failInfo.isPresent());
        }
    }

    public void testVerifyFailWrongUin() {
        when(mockDnsLookupService.lookup(NAME_SERVERS, HOST, Type.TXT)).thenReturn(Pair.of(SUCCESSFUL, Arrays.asList(
                dnsRecord(DNSVerifier.YANDEX_VERIFICATION_STRING + ": wrong uin"),
                dnsRecord("something else")
        )));
        Optional<VerificationFailInfo> failInfo = dnsVerifier.verify(1, HOST_ID, null, VERIFICATION_UIN, VerificationCausedBy.INITIAL_VERIFICATION);
        assertTrue(failInfo.isPresent());
    }

    public void testVerifyFailNoVerificationRecord() {
        when(mockDnsLookupService.lookup(NAME_SERVERS, HOST, Type.TXT)).thenReturn(Pair.of(SUCCESSFUL, Arrays.asList(
                dnsRecord("not verification"),
                dnsRecord("also not verification")
        )));
        Optional<VerificationFailInfo> failInfo = dnsVerifier.verify(1, HOST_ID, null, VERIFICATION_UIN, VerificationCausedBy.INITIAL_VERIFICATION);
        assertTrue(failInfo.isPresent());
    }

    public void testVerifyFailHostNotFound() {
        when(mockDnsLookupService.lookup(NAME_SERVERS, HOST, Type.TXT)).thenReturn(Pair.of(HOST_NOT_FOUND, Collections.emptyList()));
        Optional<VerificationFailInfo> failInfo = dnsVerifier.verify(1, HOST_ID, null, VERIFICATION_UIN, VerificationCausedBy.INITIAL_VERIFICATION);
        assertTrue(failInfo.isPresent());
    }

    public void testVerifyFailTypeNotFound() {
        when(mockDnsLookupService.lookup(NAME_SERVERS, HOST, Type.TXT)).thenReturn(Pair.of(TYPE_NOT_FOUND, Collections.emptyList()));
        Optional<VerificationFailInfo> failInfo = dnsVerifier.verify(1, HOST_ID, null, VERIFICATION_UIN, VerificationCausedBy.INITIAL_VERIFICATION);
        assertTrue(failInfo.isPresent());
    }
}