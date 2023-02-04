package ru.yandex.qe.dispenser.ws.dao;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.ResourceUnit;
import ru.yandex.qe.dispenser.domain.dao.resource.ResourceDao;
import ru.yandex.qe.dispenser.domain.dao.resource.unit.ResourceUnitDao;
import ru.yandex.qe.dispenser.domain.dao.service.ServiceDao;
import ru.yandex.qe.dispenser.ws.AcceptanceTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourceUnitDaoTest extends AcceptanceTestBase {

    @Autowired
    private ResourceUnitDao resourceUnitDao;
    @Autowired
    private ResourceDao resourceDao;
    @Autowired
    private ServiceDao serviceDao;

    @Test
    public void resourceUnitCanBeUpdated() {
        final Resource ytCpu = resourceDao.read(new Resource.Key(YT_CPU, serviceDao.read(NIRVANA)));

        resourceUnitDao.clear();
        resourceUnitDao.create(new ResourceUnit(ytCpu.getId(), DiUnit.KILO.name(),
                new ResourceUnit.UnitsSettings(ImmutableMap.of(
                        DiUnit.COUNT.name(), new ResourceUnit.UnitProperties("ht core", "unit.ht_core")
                ))));

        resourceUnitDao.update(new ResourceUnit(ytCpu.getId(), DiUnit.KILO.name(),
                new ResourceUnit.UnitsSettings(ImmutableMap.of(
                        DiUnit.COUNT.name(), new ResourceUnit.UnitProperties("single core", "unit.single_core")
                ))));

        final ResourceUnit.UnitProperties updatedUnitProperties = resourceUnitDao.read(ytCpu.getId()).getUnitsSettings().getPropertiesByUnitKey().get(DiUnit.COUNT.name());
        assertEquals("single core", updatedUnitProperties.getName());
        assertEquals("unit.single_core", updatedUnitProperties.getLocalizationKey());
    }

    @Test
    public void resourceUnitCanBeRemoved() {
        final Resource ytCpu = resourceDao.read(new Resource.Key(YT_CPU, serviceDao.read(NIRVANA)));
        resourceUnitDao.clear();

        resourceUnitDao.create(new ResourceUnit(ytCpu.getId(), DiUnit.KILO.name(),
                new ResourceUnit.UnitsSettings(ImmutableMap.of(
                        DiUnit.COUNT.name(), new ResourceUnit.UnitProperties("ht core", "unit.ht_core")
                ))));

        resourceUnitDao.delete(resourceUnitDao.read(ytCpu.getId()));

        assertTrue(resourceUnitDao.getAll().isEmpty());
    }

}
