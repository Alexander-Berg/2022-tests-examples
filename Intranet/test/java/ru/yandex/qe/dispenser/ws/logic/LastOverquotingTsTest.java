package ru.yandex.qe.dispenser.ws.logic;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.HttpHeaders;

import com.google.common.collect.ImmutableMap;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiPerformer;
import ru.yandex.qe.dispenser.api.v1.DiQuota;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.domain.aspect.HierarchyRequiredAspect;
import ru.yandex.qe.dispenser.domain.lots.LotsManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LastOverquotingTsTest extends BusinessLogicTestBase {
    @Autowired(required = false)
    private LotsManager lotsManager;

    @Test
    public void lastOverquotingMustBeLastFirstTimeThenActualIsGreaterThanMax() throws InterruptedException {
        skipIfNeeded();
        final DiPerformer performer = LYADZHIN.chooses(INFRA);

        final DiEntity pool1 = DiEntity.withKey("pool1")
                .bySpecification(YT_FILE)
                .occupies(STORAGE, DiAmount.of(50, DiUnit.BYTE))
                .build();
        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(pool1, performer)
                .perform();

        //wait for manager
        HierarchyRequiredAspect.runWithDisabledCheck(() -> lotsManager.update());

        final Long noLastOverquotingTs = dispenser().quotas().get()
                .inService(NIRVANA)
                .forResource(STORAGE)
                .ofProject(INFRA)
                .perform()
                .getFirst()
                .getLastOverquotingTs();
        assertNull(noLastOverquotingTs);

        final DiEntity pool2 = DiEntity.withKey("pool2")
                .bySpecification(YT_FILE)
                .occupies(STORAGE, DiAmount.of(51, DiUnit.BYTE))
                .build();
        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(pool2, performer)
                .perform();
        HierarchyRequiredAspect.runWithDisabledCheck(() -> lotsManager.update());

        updateHierarchy();

        final Long appearedLastOverquotingTs = dispenser().quotas().get()
                .inService(NIRVANA)
                .forResource(STORAGE)
                .ofProject(INFRA)
                .perform()
                .getFirst()
                .getLastOverquotingTs();
        assertNotNull(appearedLastOverquotingTs);
        assertBetween(appearedLastOverquotingTs, System.currentTimeMillis(), appearedLastOverquotingTs + TimeUnit.MINUTES.toMillis(1));

        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(UNIT_ENTITY, performer)
                .perform();
        HierarchyRequiredAspect.runWithDisabledCheck(() -> lotsManager.update());
        final Long remainedLastOverquotingTs = dispenser().quotas().get()
                .inService(NIRVANA)
                .forResource(STORAGE)
                .ofProject(INFRA)
                .perform()
                .getFirst()
                .getLastOverquotingTs();
        assertEquals(remainedLastOverquotingTs, appearedLastOverquotingTs);
    }

    @Disabled
    @Test
    public void setMaxShouldUpdateLastOverquotingTsIfNeeded() throws InterruptedException {
        skipIfNeeded();
        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(DiEntity.withKey("pool").bySpecification(YT_FILE).occupies(STORAGE, DiAmount.of(100, DiUnit.BYTE)).build(), LYADZHIN.chooses(INFRA))
                .perform();
        // TODO: use dispenser() after DISPENSER-105
        WebClient client = createLocalClient()
                .path("/v1/quotas/" + INFRA + "/" + NIRVANA + "/" + STORAGE + "/storage")
                .header("Content-Type", "application/json")
                .header(HttpHeaders.AUTHORIZATION, "OAuth " + WHISTLER);
        client.post(ImmutableMap.of("maxValue", 99, "unit", DiUnit.BYTE), DiQuota.class);
        lotsManager.update();
        final Long lastOverquotingTs = dispenser().quotas().get()
                .inService(NIRVANA)
                .forResource(STORAGE)
                .ofProject(INFRA)
                .perform()
                .getFirst()
                .getLastOverquotingTs();
        assertNotNull(lastOverquotingTs);
        assertBetween(lastOverquotingTs, System.currentTimeMillis(), lastOverquotingTs + TimeUnit.MINUTES.toMillis(1));

        client = createLocalClient()
                .path("/v1/quotas/" + INFRA + "/" + NIRVANA + "/" + STORAGE + "/storage")
                .header("Content-Type", "application/json")
                .header(HttpHeaders.AUTHORIZATION, "OAuth " + WHISTLER);
        assertEquals(client.post(ImmutableMap.of("maxValue", 100, "unit", DiUnit.BYTE), DiQuota.class).getLastOverquotingTs(), lastOverquotingTs);
    }

    private void skipIfNeeded() {
        Assumptions.assumeFalse(lotsManager == null, "No lots manager found");
    }
}