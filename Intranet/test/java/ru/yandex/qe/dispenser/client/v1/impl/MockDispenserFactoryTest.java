package ru.yandex.qe.dispenser.client.v1.impl;

import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiPerformer;
import ru.yandex.qe.dispenser.api.v1.DiProject;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.api.v1.request.DiEntityReference;
import ru.yandex.qe.dispenser.api.v1.request.DiEntityUsage;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaChangeResponse;
import ru.yandex.qe.dispenser.client.v1.Dispenser;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class MockDispenserFactoryTest {
    @Test
    public void throwExceptionIfTimeout() throws Throwable {
        assertThrows(SocketTimeoutException.class, () -> {
            final Dispenser dispenser = MockDispenserFactory.builder()
                    .timeout(0, () -> {
                    })
                    .build()
                    .get();
            try {
                dispenser.projects().get().perform();
            } catch (ProcessingException e) {
                throw e.getCause();
            }
        });
    }

    @Test
    public void throwExceptionIfError() {
        assertThrows(WebApplicationException.class, () -> {
            final Dispenser dispenser = MockDispenserFactory.builder()
                    .error(() -> {
                    })
                    .build()
                    .get();

            dispenser.projects().get().perform();
        });
    }

    @Test
    public void canCheckRequestBody() {
        final DiEntity entity = DiEntity.withKey("pool").occupies("yt-disk", DiAmount.anyOf(DiUnit.BYTE)).bySpecification("yt-file").build();
        final DiPerformer performer = DiPerformer.login("amosov-f").chooses("default");

        final Dispenser dispenser = MockDispenserFactory.builder()
                .success(DiMocks.changeQuota("nirvana").createEntity(entity, performer), () -> {
                })
                .build()
                .get();

        final DiQuotaChangeResponse actualResponse = dispenser.quotas()
                .changeInService("nirvana")
                .createEntity(entity, performer)
                .perform();

        Assertions.assertEquals(entity, actualResponse.getFirst().getOperation().getAction());
        Assertions.assertSame(actualResponse.getFirst().getOperation().getPerformer(), performer);
    }

    @Test
    public void canMockResponseByRequestIdAndByRequestBody() {
        final DiEntity entity = DiEntity.withKey("pool").occupies("yt-disk", DiAmount.anyOf(DiUnit.BYTE)).bySpecification("yt-file").build();
        final DiPerformer performer = DiPerformer.login("amosov-f").chooses("default");

        final Dispenser dispenser = MockDispenserFactory.builder()
                .timeout("1", 0, () -> {
                })
                .success("3", () -> {
                })
                .error(DiMocks.changeQuota("nirvana").createEntity(entity, performer), () -> {
                })
                .build()
                .get();

        try {
            dispenser.quotas().changeInService("nirvana").releaseEntity(entity).withReqId("1").perform();
            Assertions.fail("ReqId '1' must be with timeout!");
        } catch (RuntimeException ignored) {
        }

        try {
            dispenser.quotas().changeInService("nirvana").createEntity(entity, performer).withReqId("2").perform();
            Assertions.fail("ReqId '2' must go to error case!");
        } catch (RuntimeException ignored) {
        }

        dispenser.quotas().changeInService("nirvana").releaseEntitySharing(DiEntityUsage.singleOf(entity), performer).withReqId("3").perform();
        // success case
    }

    @Test
    public void assertionErrorIfNoCaseConfiguredInMockDispenserFactory() {
        assertThrows(AssertionError.class, () -> {
            final Dispenser dispenser = MockDispenserFactory.builder().build().get();

            final DiEntityReference ref = DiEntityReference.withKey("pool").bySpecification("yt-file").build();
            dispenser.quotas().changeInService("nirvana").releaseEntity(ref).withReqId("3").perform();
        });
    }

    @Test
    public void canMockByPathWithQuery() {
        final DiProject deFault = DiProject.withKey("default")
                .withName("Default")
                .build();
        final Dispenser dispenser = MockDispenserFactory.builder()
                .mock("/v1/projects?leaf=true&member=bendyna", new DiListResponse<>(Collections.singleton(deFault)), () -> {
                })
                .build()
                .get();
        final DiListResponse<DiProject> bendynaProjects = dispenser.projects().get().avaliableFor("bendyna").perform();
        Assertions.assertEquals(1, bendynaProjects.size());
        Assertions.assertEquals(deFault, bendynaProjects.getFirst());
    }

    @Test
    public void canCheckPerformExecutionUsingSuccessMockCallback() {
        final CountDownLatch latch = new CountDownLatch(1);

        final DiEntityReference ref = DiEntityReference.withKey("pool").bySpecification("yt-file").build();
        final Dispenser dispenser = MockDispenserFactory.builder()
                .success(DiMocks.changeQuota("nirvana").releaseEntity(ref), latch::countDown)
                .build()
                .get();

        dispenser.quotas().changeInService("nirvana").releaseEntity(ref);
        Assertions.assertEquals(1, latch.getCount());
        dispenser.quotas().changeInService("nirvana").releaseEntity(ref).perform();
        Assertions.assertEquals(0, latch.getCount());
    }

    @Test
    public void callbackAppliesOnlyIfMockMatchesRequest() {
        final CountDownLatch latch = new CountDownLatch(1);

        final DiEntityReference ref = DiEntityReference.withKey("pool").bySpecification("yt-file").build();
        final Dispenser dispenser = MockDispenserFactory.builder()
                .success(DiMocks.changeQuota("nirvana").releaseEntity(ref), latch::countDown)
                .success(() -> {
                })
                .build()
                .get();

        dispenser.quotas().changeInService("nirvana1").releaseEntity(ref).perform();
        Assertions.assertEquals(1, latch.getCount());
        dispenser.quotas().changeInService("nirvana").releaseEntity(ref).perform();
        Assertions.assertEquals(0, latch.getCount());
    }

    @Test
    public void callbackAppliesInErrorMockToo() {
        final CountDownLatch latch = new CountDownLatch(1);

        final Dispenser dispenser = MockDispenserFactory.builder()
                .error(latch::countDown)
                .build()
                .get();

        try {
            dispenser.getEntities().perform();
            Assertions.fail("All performs must go to error!");
        } catch (WebApplicationException ignored) {
            Assertions.assertEquals(0, latch.getCount());
        }
    }
}