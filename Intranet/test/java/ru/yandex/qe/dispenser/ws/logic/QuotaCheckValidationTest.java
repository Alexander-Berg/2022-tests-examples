package ru.yandex.qe.dispenser.ws.logic;

import javax.ws.rs.core.Response;

import com.google.common.net.HttpHeaders;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiCheckCompareType;
import ru.yandex.qe.dispenser.api.v1.DiCheckValueType;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiQuotaState;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.ws.QuotaCheckService;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QuotaCheckValidationTest extends BusinessLogicTestBase {

    @Test
    public void checkCanRelativeCompareGreaterAndLess() {

        final Response response = createLocalClient()
                .path("/v1/check-quotas")
                .query("projectKey", INFRA)
                .query("quotaSpecKey", "/" + NIRVANA + "/" + YT_CPU + "/" + YT_CPU)
                .query("valueType", DiCheckValueType.PERCENT.name())
                .query("compareType", DiCheckCompareType.LESS.name())
                .query("value", 50)
                .replaceHeader(HttpHeaders.AUTHORIZATION, "OAuth " + STERLIGOVAK)
                .get();

        assertEquals(QuotaCheckService.STATUS_FAIL, response.getStatus());

        final Response response2 = createLocalClient()
                .path("/v1/check-quotas")
                .query("projectKey", INFRA)
                .query("quotaSpecKey", "/" + NIRVANA + "/" + YT_CPU + "/" + YT_CPU)
                .query("valueType", DiCheckValueType.PERCENT.name())
                .query("compareType", DiCheckCompareType.GREATER.name())
                .query("value", 50)
                .replaceHeader(HttpHeaders.AUTHORIZATION, "OAuth " + STERLIGOVAK)
                .get();

        assertEquals(QuotaCheckService.STATUS_OK, response2.getStatus());
    }

    @Test
    public void checkCanAbsoluteCompareGreaterAndLess() {

        final Response state = createLocalClient()
                .path("/v1/check-quotas")
                .query("projectKey", INFRA)
                .query("quotaSpecKey", "/" + NIRVANA + "/" + YT_CPU + "/" + YT_CPU)
                .query("valueType", DiCheckValueType.ABSOLUTE.name())
                .query("compareType", DiCheckCompareType.LESS.name())
                .query("value", 100)
                .query("valueUnit", DiUnit.PERMILLE)
                .replaceHeader(HttpHeaders.AUTHORIZATION, "OAuth " + STERLIGOVAK)
                .get();

        assertEquals(QuotaCheckService.STATUS_FAIL, state.getStatus());

        final Response state2 = createLocalClient()
                .path("/v1/check-quotas")
                .query("projectKey", INFRA)
                .query("quotaSpecKey", "/" + NIRVANA + "/" + YT_CPU + "/" + YT_CPU)
                .query("valueType", DiCheckValueType.ABSOLUTE.name())
                .query("compareType", DiCheckCompareType.GREATER.name())
                .query("value", 100)
                .query("valueUnit", DiUnit.PERMILLE)
                .replaceHeader(HttpHeaders.AUTHORIZATION, "OAuth " + STERLIGOVAK)
                .get();

        assertEquals(QuotaCheckService.STATUS_OK, state2.getStatus());
    }

    @Test
    public void checkShouldReactOnActualChange() {
        final Response state = createLocalClient()
                .path("/v1/check-quotas")
                .query("projectKey", INFRA)
                .query("quotaSpecKey", "/" + NIRVANA + "/" + YT_CPU + "/" + YT_CPU)
                .query("valueType", DiCheckValueType.ABSOLUTE.name())
                .query("compareType", DiCheckCompareType.GREATER.name())
                .query("value", 100)
                .query("valueUnit", DiUnit.PERMILLE)
                .replaceHeader(HttpHeaders.AUTHORIZATION, "OAuth " + STERLIGOVAK)
                .get();

        assertEquals(QuotaCheckService.STATUS_OK, state.getStatus());

        dispenser().service(NIRVANA)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState.forResource(YT_CPU)
                        .forProject(INFRA)
                        .withActual(DiAmount.of(110, DiUnit.PERMILLE))
                        .build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));

        updateHierarchy();

        final Response state2 = createLocalClient()
                .path("/v1/check-quotas")
                .query("projectKey", INFRA)
                .query("quotaSpecKey", "/" + NIRVANA + "/" + YT_CPU + "/" + YT_CPU)
                .query("valueType", DiCheckValueType.ABSOLUTE.name())
                .query("compareType", DiCheckCompareType.GREATER.name())
                .query("value", 100)
                .query("valueUnit", DiUnit.PERMILLE)
                .replaceHeader(HttpHeaders.AUTHORIZATION, "OAuth " + STERLIGOVAK)
                .get();

        assertEquals(QuotaCheckService.STATUS_FAIL, state2.getStatus());

        final Response state3 = createLocalClient()
                .path("/v1/check-quotas")
                .query("projectKey", INFRA)
                .query("quotaSpecKey", "/" + NIRVANA + "/" + YT_CPU + "/" + YT_CPU)
                .query("valueType", DiCheckValueType.ABSOLUTE.name())
                .query("compareType", DiCheckCompareType.GREATER.name())
                .query("value", 110)
                .query("valueUnit", DiUnit.PERMILLE)
                .replaceHeader(HttpHeaders.AUTHORIZATION, "OAuth " + STERLIGOVAK)
                .get();

        assertEquals(QuotaCheckService.STATUS_OK, state3.getStatus());
    }

    @Test
    public void checkShouldCorrectCompareAmounts() {
        final Response state = createLocalClient()
                .path("/v1/check-quotas")
                .query("projectKey", INFRA)
                .query("quotaSpecKey", "/" + NIRVANA + "/" + YT_CPU + "/" + YT_CPU)
                .query("valueType", DiCheckValueType.ABSOLUTE.name())
                .query("compareType", DiCheckCompareType.GREATER.name())
                .query("value", 10)
                .query("valueUnit", DiUnit.PERCENT)
                .replaceHeader(HttpHeaders.AUTHORIZATION, "OAuth " + STERLIGOVAK)
                .get();

        assertEquals(QuotaCheckService.STATUS_OK, state.getStatus());

        dispenser().service(NIRVANA)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState.forResource(YT_CPU)
                        .forProject(INFRA)
                        .withActual(DiAmount.of(11, DiUnit.PERCENT))
                        .build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));

        updateHierarchy();

        final Response state2 = createLocalClient()
                .path("/v1/check-quotas")
                .query("projectKey", INFRA)
                .query("quotaSpecKey", "/" + NIRVANA + "/" + YT_CPU + "/" + YT_CPU)
                .query("valueType", DiCheckValueType.ABSOLUTE.name())
                .query("compareType", DiCheckCompareType.GREATER.name())
                .query("value", 10)
                .query("valueUnit", DiUnit.PERCENT)
                .replaceHeader(HttpHeaders.AUTHORIZATION, "OAuth " + STERLIGOVAK)
                .get();

        assertEquals(QuotaCheckService.STATUS_FAIL, state2.getStatus());

        final Response state3 = createLocalClient()
                .path("/v1/check-quotas")
                .query("projectKey", INFRA)
                .query("quotaSpecKey", "/" + NIRVANA + "/" + YT_CPU + "/" + YT_CPU)
                .query("valueType", DiCheckValueType.ABSOLUTE.name())
                .query("compareType", DiCheckCompareType.GREATER.name())
                .query("value", 1)
                .query("valueUnit", DiUnit.COUNT)
                .replaceHeader(HttpHeaders.AUTHORIZATION, "OAuth " + STERLIGOVAK)
                .get();

        assertEquals(QuotaCheckService.STATUS_OK, state3.getStatus());
    }

    @Test
    public void checkCanHaveSegments() {
        final Response state = createLocalClient()
                .path("/v1/check-quotas")
                .query("projectKey", VERTICALI)
                .query("quotaSpecKey", "/" + YP + "/" + SEGMENT_STORAGE + "/" + SEGMENT_STORAGE)
                .query("valueType", DiCheckValueType.PERCENT.name())
                .query("compareType", DiCheckCompareType.GREATER.name())
                .query("value", 50)
                .query("segments", DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                .replaceHeader(HttpHeaders.AUTHORIZATION, "OAuth " + STERLIGOVAK)
                .get();

        assertEquals(QuotaCheckService.STATUS_OK, state.getStatus());

        dispenser().service(YP)
                .syncState()
                .quotas()
                .changeQuota(DiQuotaState.forResource(SEGMENT_STORAGE)
                        .forProject(VERTICALI)
                        .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                        .withActual(DiAmount.of(200, DiUnit.BYTE))
                        .build())
                .performBy(DiPerson.login(ZOMB_MOBSEARCH));

        updateHierarchy();

        final Response state2 = createLocalClient()
                .path("/v1/check-quotas")
                .query("projectKey", VERTICALI)
                .query("quotaSpecKey", "/" + YP + "/" + SEGMENT_STORAGE + "/" + SEGMENT_STORAGE)
                .query("valueType", DiCheckValueType.PERCENT.name())
                .query("compareType", DiCheckCompareType.GREATER.name())
                .query("value", 50)
                .query("segments", DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                .replaceHeader(HttpHeaders.AUTHORIZATION, "OAuth " + STERLIGOVAK)
                .get();

        assertEquals(QuotaCheckService.STATUS_FAIL, state2.getStatus());
    }
}
