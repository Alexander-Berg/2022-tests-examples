package ru.yandex.payments.tvm;

import javax.inject.Inject;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.payments.tvm.client.CheckResult;
import ru.yandex.payments.tvm.client.TvmClient;
import ru.yandex.payments.tvm.client.TvmTicket;
import ru.yandex.payments.tvm.client.TvmTicket.UserTvmTicket;
import ru.yandex.payments.tvm.exceptions.TvmException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest
class TvmToolClientTest {
    private static final long HE = 100500;
    private static final long SHE = 100501;
    private static final long UNKNOWN = 200602;
    static final long USER_MAIN_UID = 500100;
    static final String USER_TICKET = """
            3:user:CA0Q__________9_GhgKBAiEwx4KBAjowx4QhMMeINKF2MwEKAE:\
            KzR14bm2OBb6JQypTckDIHdI8Q50lV09ZGKGJMafXOrL8Vf4gWXS-_DAX77a2OkejV-hV-avxRxUWBn9A3pDbC9MG1_ERnV\
            u4NcixGhxyNN5j7_g9OTSZQwFRB2spSJqCryQ_MdnWoEVKWSAncd7tg8wEoAcaTZVXdfniuIsTTg\
            """;

    private static final long CLIENT_SERVICE_TVM_ID = 11;
    private static final String SERVICE_TICKET = """
            3:serv:CBAQ__________9_IgQICxAq:SaYQ-GLhg5m7mWBCMML4fq0ZI\
            QhDHqRMR8LDMvPaq8zBmKX_jK3LfYIO4O_eiNQ52EJI90kYhmAfG4yOYUHSR55e_qwpT4hNPW9vhmRdT7fIVb_wR9u53H4Re\
            tJ8suqBk5jdRu2Rv4e5Wv2F_ico4h2OQKmPrBon1QGCXY4BcKg\
            """;
    private static final String UNAPPROVED_SERVICE_TICKET = """
            3:serv:CBAQ__________9_IgUICxCnEg:HKc0TIFFIy0qA\
            E8rN7q8gAEuMEh57Q9e90UQzfJWnFB70bG4AsSc892FL4gPa_pmvHQEUndnZCFMkuKR1KxyDCoQZA2EbZFSse_UfKAWD95oFK\
            B16BnVWPw0Kiz-0ZYCBNV9_kudxKay-cA3oK_1CmiDs-l1OSeyR79vJp_gGw\
            """;

    @Inject
    TvmClient client;

    @Test
    @DisplayName("Verify that getTicketFor returns correct ticket")
    void getTicketTest() {
        val ticket = client.getTicketFor(HE).block();

        assertThat(ticket)
                .isNotNull();
        assertThat(ticket.tvmId())
                .isEqualTo(HE);
    }

    @Test
    @DisplayName("Verify that getTicketFor returns exception when unknown destination is passed")
    void getUnknownTicketTest() {
        assertThrows(TvmException.class, () -> client.getTicketFor(UNKNOWN).block());
    }

    @Test
    @DisplayName("Verify that getTicketsFor returns exception when one of the passed destinations is unknown")
    void getUnknownTicketsTest() {
        assertThrows(TvmException.class, () -> client.getTicketsFor(HE, UNKNOWN).block());
    }

    @Test
    @DisplayName("Verify that getTicketsFor returns correct tickets")
    void getTicketsTest() {
        val tickets = client.getTicketsFor(HE, SHE).block();
        assertThat(tickets)
                .isNotNull()
                .containsKeys(HE, SHE);
    }

    @Test
    @DisplayName("Verify that `checkService` accept valid service ticket and return client tvm id")
    void checkServiceTest() {
        val result = client.checkServiceTicket(SERVICE_TICKET).block();
        assertThat(result)
                .isNotNull()
                .isInstanceOf(CheckResult.Success.class);
        assertThat(result.isSuccess())
                .isTrue();
        assertThat(result.isForbidden())
                .isFalse();

        @SuppressWarnings("rawtypes")
        val successResult = (CheckResult.Success) result;
        assertThat(successResult.ticket()).isInstanceOf(TvmTicket.ServiceTvmTicket.class);

        val serviceTicket = (TvmTicket.ServiceTvmTicket) successResult.ticket();
        assertThat(serviceTicket.tvmId()).isEqualTo(CLIENT_SERVICE_TVM_ID);
    }

    @Test
    @DisplayName("Verify that checkService deny invalid service ticket")
    void checkServiceWithInvalidTicketTest() {
        val result = client.checkServiceTicket("fqoaqg[iaqewrg").block();
        assertThat(result)
                .isNotNull()
                .isEqualTo(CheckResult.FORBIDDEN);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isForbidden()).isTrue();
    }

    @Test
    @DisplayName("Verify that checkService deny unapproved service ticket")
    void checkServiceWithUnapprovedTicketTest() {
        val result = client.checkServiceTicket(UNAPPROVED_SERVICE_TICKET).block();
        assertThat(result)
                .isNotNull()
                .isEqualTo(CheckResult.FORBIDDEN);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isForbidden()).isTrue();
    }

    @Test
    @DisplayName("Verify that checkUser deny invalid user ticket")
    void checkUserWithInvalidTicketTest() {
        val result = client.checkUserTicket("aqeworij[0erg").block();
        assertThat(result)
                .isNotNull()
                .isEqualTo(CheckResult.FORBIDDEN);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isForbidden()).isTrue();
    }

    @Test
    @DisplayName("Verify that checkService accept valid user ticket and return user uid")
    void checkUserTest() {
        val result = client.checkUserTicket(USER_TICKET).block();
        assertThat(result)
                .isNotNull()
                .isInstanceOf(CheckResult.Success.class);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isForbidden()).isFalse();

        @SuppressWarnings("rawtypes")
        val successResult = (CheckResult.Success) result;
        assertThat(successResult.ticket()).isInstanceOf(UserTvmTicket.class);

        val userTicket = (UserTvmTicket) successResult.ticket();
        assertThat(userTicket.defaultUid()).isEqualTo(USER_MAIN_UID);
    }
}
