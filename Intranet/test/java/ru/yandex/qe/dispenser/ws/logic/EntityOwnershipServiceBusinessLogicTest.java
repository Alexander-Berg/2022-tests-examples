package ru.yandex.qe.dispenser.ws.logic;

import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.api.v1.request.DiEntityOwnership;
import ru.yandex.qe.dispenser.api.v1.request.DiEntityUsage;
import ru.yandex.qe.dispenser.api.v1.response.DiEntityOwnershipResponse;
import ru.yandex.qe.dispenser.ws.EntityOwnershipService;

public final class EntityOwnershipServiceBusinessLogicTest extends BusinessLogicTestBase {
    @Test
    public void gettingEntityOwnershipsMustIgnoreEntitiesIfAbsent() {
        final Set<DiEntity> entities = dispenser().getEntityOwnerships()
                .inService(NIRVANA)
                .bySpecification(YT_FILE)
                .withEntityKey("pool")
                .perform()
                .getEntities();
        Assertions.assertTrue(entities.isEmpty());
    }

    @Test
    public void gettingEntityOwnershipsTest() throws Exception {
        final DiEntity e1 = randomUnitEntity();
        final DiEntity e2 = randomUnitEntity();
        dispenser().quotas().changeInService(NIRVANA)
                .createEntity(e1, LYADZHIN.chooses(INFRA))
                .createEntity(e2, WHISTLER.chooses(INFRA))
                .shareEntity(DiEntityUsage.singleOf(e1), LYADZHIN.chooses(INFRA))
                .shareEntity(DiEntityUsage.singleOf(e1), WHISTLER.chooses(INFRA))
                .shareEntity(DiEntityUsage.singleOf(e2), LYADZHIN.chooses(VERTICALI))
                .perform();

        final DiEntityOwnershipResponse ownerships = dispenser().getEntityOwnerships()
                .inService(NIRVANA)
                .bySpecification(YT_FILE)
                .withEntityKeys(Arrays.asList(e1.getKey(), e2.getKey()))
                .ofProject(INFRA)
                .perform();


        final int u1 = ownerships.stream().filter(o -> o.getEntity().equals(e1)).map(DiEntityOwnership::getUsagesCount).findFirst().orElse(0);
        final int u2 = ownerships.stream().filter(o -> o.getEntity().equals(e2)).map(DiEntityOwnership::getUsagesCount).findFirst().orElse(0);

        Assertions.assertEquals(3, u1);
        Assertions.assertEquals(1, u2);
    }

    /**
     * {@link EntityOwnershipService#filter}
     * <p>
     * DISPENSER-549: Добавить фильтр .inLeafs() к вызову /entity-ownerships
     */
    @Test
    public void inLeafsMustReturnOnlyLeafProjectOwnerships() {
        dispenser().quotas().changeInService(NIRVANA).createEntity(UNIT_ENTITY, LYADZHIN.chooses(INFRA)).perform();

        final DiEntityOwnershipResponse ownerships = dispenser().getEntityOwnerships()
                .inService(NIRVANA)
                .bySpecification(YT_FILE)
                .withEntityKey(UNIT_ENTITY.getKey())
                .inLeafs()
                .perform();

        Assertions.assertTrue(ownerships.getProjects().stream().anyMatch(p -> p.getKey().equals(INFRA)));
        Assertions.assertTrue(ownerships.getProjects().stream().noneMatch(p -> p.getKey().equals(YANDEX)));
    }

    @Test
    public void entityOwnershipsMethodShouldAcceptEmprtyRequests() {
        final DiEntityOwnershipResponse ownerships = dispenser().getEntityOwnerships().perform();
        Assertions.assertTrue(ownerships.isEmpty());
    }
}
