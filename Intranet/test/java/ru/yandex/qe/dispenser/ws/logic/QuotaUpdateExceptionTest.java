package ru.yandex.qe.dispenser.ws.logic;

import java.text.DecimalFormat;

import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiQuotaState;
import ru.yandex.qe.dispenser.client.v1.DiPerson;

public class QuotaUpdateExceptionTest extends BusinessLogicTestBase {

    @Test
    public void errorMessageIsCorrect() {

        assertThrowsWithMessage(() -> {
            dispenser()
                    .service(NIRVANA)
                    .syncState()
                    .quotas()
                    .changeQuota(DiQuotaState.forResource(STORAGE).forProject(SEARCH).withMax(DiAmount.of(532, DiUnit.BYTE)).build())
                    .performBy(AMOSOV_F);
        }, "Quota's max for resource 'Storage' for project 'Yandex' (250 B) is less than subprojects usage (632 B). Diff: 382 B");
    }

    @Test
    public void errorMessageRoundsDiffValueCorrectly() {

        assertThrowsWithMessage(() -> {
            dispenser()
                    .service(NIRVANA)
                    .syncState()
                    .quotas()
                    .changeQuota(DiQuotaState.forResource(STORAGE).forProject(SEARCH).withMax(DiAmount.of(43532, DiUnit.BYTE)).build())
                    .performBy(AMOSOV_F);
        }, "Quota's max for resource 'Storage' for project 'Yandex' (250 B) is less than subprojects usage ("
                + new DecimalFormat("#.##").format(42.61) + " KiB). Diff: "
                + new DecimalFormat("#.##").format(42.37) + " KiB (43382 B)");
    }

    @Test
    public void errorMessageWhenActualIsGreatThanMaxIsCorrect() {
        assertThrowsWithMessage(() -> {
            dispenser()
                    .service(YP)
                    .syncState()
                    .quotas()
                    .changeQuota(DiQuotaState.forResource(SEGMENT_STORAGE)
                            .forProject(VERTICALI)
                            .withActual(DiAmount.of(258L, DiUnit.BYTE))
                            .withMax(DiAmount.of(256L, DiUnit.BYTE))
                            .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                            .build())
                    .performBy(DiPerson.login(ZOMB_MOBSEARCH));
        }, "New quota's max for resource 'Segment Storage' for project 'Verticali' (256 B) is less than actual (258 B). Diff: 2 B");
    }

    @Test
    public void errorMessageWhenOwnActualIsGreatThanOwnMaxIsCorrect() {
        assertThrowsWithMessage(() -> {
            dispenser()
                    .service(YP)
                    .syncState()
                    .quotas()
                    .changeQuota(DiQuotaState.forResource(SEGMENT_STORAGE)
                            .forProject(YANDEX)
                            .withActual(DiAmount.of(200L, DiUnit.BYTE))
                            .withOwnMax(DiAmount.of(128L, DiUnit.BYTE))
                            .withSegments(DC_SEGMENT_1, SEGMENT_SEGMENT_1)
                            .build())
                    .performBy(DiPerson.login(ZOMB_MOBSEARCH));
        }, "New quota's own max for resource 'Segment Storage' for project 'Yandex' (128 B) is less than own actual (200 B). Diff: 72 B");

    }
}
