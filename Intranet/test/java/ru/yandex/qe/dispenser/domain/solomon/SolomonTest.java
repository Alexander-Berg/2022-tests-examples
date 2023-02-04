package ru.yandex.qe.dispenser.domain.solomon;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.Quota;
import ru.yandex.qe.dispenser.domain.QuotaSpec;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Service;
import ru.yandex.qe.dispenser.solomon.Solomon;

public final class SolomonTest {

    @Test
    public void newResourcesMustHaveCorrectSensorName() {
        final Project yandex = Project.withKey("yandex").name("Яндекс").build();
        final Service scraper = Service.withKey("nirvana").withName("Нирвана").build();
        final Resource mdsDisk = new Resource.Builder("mds-disk", scraper).name("Диск в MDS").type(DiResourceType.MEMORY).build();
        final QuotaSpec spec = new QuotaSpec.Builder("mds-disk-quota", mdsDisk).description("Описание").build();
        final Quota quota = Quota.noQuota(yandex, spec, Collections.emptySet());

        Assertions.assertEquals("yandex: nirvana/mds-disk/mds-disk-quota", Solomon.getSensorName(quota.getKey()));
    }

    @Test
    public void oldResourcesMustHaveOldstyleSensorName() {
        final Project yandex = Project.withKey("yandex").name("Яндекс").build();
        final Service service = Service.withKey("cluster-api").withName("Cluster API").build();
        final Resource resource = new Resource.Builder("ram", service).name("RAM").type(DiResourceType.MEMORY).build();
        final QuotaSpec spec = new QuotaSpec.Builder("ram-quota", resource).description("Описание").build();
        final Quota quota = Quota.noQuota(yandex, spec, Collections.emptySet());

        Assertions.assertEquals("Cluster API/yandex/Описание (RAM)", Solomon.getSensorName(quota.getKey()));
    }

}
