package ru.yandex.qe.dispenser.ws.logic;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.api.v1.request.DiResourceAmount;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.client.v1.builder.BatchQuotaChangeRequestBuilder;
import ru.yandex.qe.dispenser.client.v1.impl.ProjectFilter;
import ru.yandex.qe.dispenser.client.v1.impl.ResourceFilter;
import ru.yandex.qe.dispenser.domain.request.RequestManager;
import ru.yandex.qe.dispenser.ws.intercept.TransactionWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IdempotentTest extends BusinessLogicTestBase {
    private static final String REQ_ID = "reqidfortest";

    @Autowired
    private RequestManager requestManager;

    @Disabled
    @Test
    public void sameTimeTest() throws InterruptedException {
        final AtomicBoolean hasT1Result = new AtomicBoolean();
        final AtomicBoolean hasT2Result = new AtomicBoolean();
        final AtomicReference<String> t2result = new AtomicReference<>();

        final Thread t1 = new Thread(() -> {
            TransactionWrapper.INSTANCE.execute(() -> {
                final Optional<String> result = requestManager.lockOrResult(REQ_ID);
                hasT1Result.set(result.isPresent());
                Thread.sleep(200);
                requestManager.createRequest(REQ_ID, REQ_ID);
            });
        });

        final Thread t2 = new Thread(() -> {
            TransactionWrapper.INSTANCE.execute(() -> {
                Thread.sleep(100);
                //reqId soudl be locked yet
                final Optional<String> result = requestManager.lockOrResult(REQ_ID);
                hasT2Result.set(result.isPresent());
                if (result.isPresent()) {
                    t2result.set(result.get());
                }
            });
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        assertFalse(hasT1Result.get());
        assertTrue(hasT2Result.get());
        assertEquals(REQ_ID, t2result.get());

    }

    @Test
    public void sameReqIdMustNotChageState() {
        final String reqId = "fake-" + System.currentTimeMillis();
        final BatchQuotaChangeRequestBuilder reqBuilder = dispenser().quotas()
                .changeInService(SCRAPER)
                .acquireResource(DiResourceAmount.ofResource(DOWNLOADS).withAmount(999, DiUnit.COUNT).build(), LYADZHIN.chooses(INFRA));

        reqBuilder.withReqId(reqId).perform();

        //noexception (DISPENSER-89)
        reqBuilder.perform();
        reqBuilder.perform();

        final boolean canAcquire = dispenser().quotas().get()
                .ofProject(INFRA)
                .inService(NIRVANA)
                .byEntitySpecification(YT_FILE)
                .perform()
                .canAcquireAllResources();

        assertTrue(canAcquire);
    }

    @Test
    public void exceptionMustNotHoldLock() {
        final String reqId = "fake-" + System.currentTimeMillis();
        final DiEntity pool1 = DiEntity.withKey("pool1").bySpecification(YT_FILE).occupies(STORAGE, DiAmount.of(99, DiUnit.BYTE)).build();
        //HTTP-403 produces different exceptions while using Remote- and SpyClient
        try {
            dispenser().quotas()
                    .changeInService(NIRVANA)
                    .createEntity(pool1, DiPerson.login("i-am-not-any-valid-user").chooses(INFRA))
                    .withReqId(reqId)
                    .perform();
        } catch (Exception ignore) {
        }

        final DiQuotaGetResponse canAcquire100 = dispenser().quotas().get()
                .inService(NIRVANA)
                .byEntitySpecification(YT_FILE)
                .ofProject(INFRA)
                .perform();

        assertEquals(1, canAcquire100.size());
        assertTrue(canAcquire100.canAcquire(STORAGE, DiAmount.of(100, DiUnit.BYTE)));

        final DiEntity pool2 = DiEntity.withKey("pool2").bySpecification(YT_FILE).occupies(STORAGE, DiAmount.of(99, DiUnit.BYTE)).build();
        dispenser().quotas()
                .changeInService(NIRVANA)
                .createEntity(pool2, LYADZHIN.chooses(INFRA))
                .withReqId(reqId)
                .perform();

        updateHierarchy();

        final DiQuotaGetResponse cantAcquire = dispenser().quotas().get()
                .filterBy(ProjectFilter.single(INFRA))
                .filterBy(ResourceFilter.singleResource(NIRVANA, STORAGE))
                .perform();

        assertEquals(1, cantAcquire.size());
        assertFalse(cantAcquire.canAcquire(STORAGE, DiAmount.of(100, DiUnit.BYTE)));
    }
}
