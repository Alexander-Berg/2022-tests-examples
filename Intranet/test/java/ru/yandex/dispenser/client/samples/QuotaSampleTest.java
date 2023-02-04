package ru.yandex.dispenser.client.samples;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiPerformer;
import ru.yandex.qe.dispenser.api.v1.DiQuota;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.api.v1.request.DiEntityUsage;
import ru.yandex.qe.dispenser.api.v1.request.DiResourceAmount;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaChangeResponse;
import ru.yandex.qe.dispenser.api.v1.response.DiQuotaGetResponse;
import ru.yandex.qe.dispenser.client.v1.Dispenser;
import ru.yandex.qe.dispenser.client.v1.impl.DispenserConfig;
import ru.yandex.qe.dispenser.client.v1.impl.RemoteDispenserFactory;
import ru.yandex.qe.dispenser.client.v1.impl.ResourceFilter;

public class QuotaSampleTest {
    private final Dispenser dispenser = new RemoteDispenserFactory(new DispenserConfig()).get();

    public void checkQuotaForMultipleProjects() {
        final DiQuota response = dispenser.quotas().get()
                .inService("nirvana")
                .forResource("storage")
                .ofProject("search-infra")
                .perform()
                .getFirst();
        if (response.canAcquire(DiAmount.of(100, DiUnit.BYTE))) {
            // do something
        }
    }

    public void canAcquireAnyAmountOfResource() {
        dispenser.quotas().get()
                .inService("nirvana")
                .forResource("storage")
                .ofProject("search-infra")
                .perform();
        dispenser.quotas().get()
                .inService("nirvana")
                .forResource("storage")
                .ofProject("search-infra")
                .perform()
                .getFirst();
    }

    public void getQuotas() {
        final DiQuotaGetResponse quotaGetResponse = dispenser.quotas().get()
                .filterBy(ResourceFilter.resource("nirvana", "storage").or("cluster-api", "cpu").build())
                .perform();
        for (final DiQuota quota : quotaGetResponse) {
            final String projectKey = quota.getProject().getKey();
            final long maxValue = quota.getMax().getValue();
            // do something
        }
    }


    public void changeQuotas() {
        final DiEntity entity = DiEntity.withKey("324234")
                .bySpecification("yt-file")
                .occupies(DiResourceAmount.ofResource("yt-disk").withAmount(100, DiUnit.GIBIBYTE).build())
                .build();
        final DiPerformer performer = DiPerformer.login("starlight").chooses("yandex");

        final DiQuotaChangeResponse response = dispenser.quotas().changeInService("nirvana")
                .acquireResource(DiResourceAmount.ofResource("yt-disk").withAmount(100, DiUnit.GIBIBYTE).build(), performer)
                .createEntity(entity, performer)
                .shareEntity(DiEntityUsage.of(entity, 10), performer)
                .perform();
    }


    public void getEntities() {
        dispenser.getEntities().inService("nirvana").bySpecification("yt-file").trashOnly().page(1).limit(10).perform();
    }
}
