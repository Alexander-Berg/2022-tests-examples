package ru.yandex.qe.dispenser.domain.dao.entity;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.DiMetaValueSet;
import ru.yandex.qe.dispenser.api.v1.request.DiProcessingMode;
import ru.yandex.qe.dispenser.domain.Entity;
import ru.yandex.qe.dispenser.domain.Person;
import ru.yandex.qe.dispenser.domain.PersonAffiliation;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.domain.dao.Mocks;
import ru.yandex.qe.dispenser.domain.support.EntityUsageDiff;

public final class EntityDaoUtilsTest {
    @Test
    public void changeUsagesMustDecrementFatProjectUsagesFirst() {
        final String key = "key";
        final Project project = Project.withKey(key).name("").build();
        final Project lyadzhin = Project.withKey(Project.Key.of(key, new Person("lyadzhin", 1120000000014351L, false, false, false, PersonAffiliation.YANDEX))).name("").parent(project).build();
        final Project inikifor = Project.withKey(Project.Key.of(key, new Person("inikifor", 1120000000018258L, false, false, false, PersonAffiliation.YANDEX))).name("").parent(project).build();
        final Project welvet = Project.withKey(Project.Key.of(key, new Person("welvet", 1120000000025633L, false, false, false, PersonAffiliation.YANDEX))).name("").parent(project).build();

        final DiProcessingMode mode = DiProcessingMode.ROLLBACK_ON_ERROR;
        final Entity e = Mocks.entity();

        {
            final Map<Project, Integer> allUsages = new HashMap<Project, Integer>() {{
                put(lyadzhin, 1);
                put(inikifor, 2);
                put(welvet, 3);
            }};

            new EntityUsageDiff(mode, e, lyadzhin, DiMetaValueSet.EMPTY, -5, null).processOverUsages(allUsages);

            Assertions.assertNull(allUsages.get(lyadzhin));
            Assertions.assertEquals(1, (int) allUsages.get(inikifor));
            Assertions.assertNull(allUsages.get(welvet));
        }

        {
            final Map<Project, Integer> allUsages = new HashMap<Project, Integer>() {{
                put(lyadzhin, 2);
                put(inikifor, 2);
                put(welvet, 3);
            }};

            new EntityUsageDiff(mode, e, inikifor, DiMetaValueSet.EMPTY, -3, null).processOverUsages(allUsages);

            Assertions.assertEquals(new Integer(2), allUsages.get(lyadzhin));
            Assertions.assertNull(allUsages.get(inikifor));
            Assertions.assertEquals(new Integer(2), allUsages.get(welvet));
        }

        {
            final Map<Project, Integer> allUsages = new HashMap<Project, Integer>() {{
                put(lyadzhin, 1);
                put(inikifor, 2);
                put(welvet, 3);
            }};

            new EntityUsageDiff(mode, e, inikifor, DiMetaValueSet.EMPTY, -6, null).processOverUsages(allUsages);

            Assertions.assertTrue(allUsages.isEmpty());
        }
    }

    @Test
    public void changeUsagesShouldThrowExceptionIfThereIsNotEnoughUsages() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            testChangeUsagesIfThereIsNotEnough(DiProcessingMode.ROLLBACK_ON_ERROR);
        });
    }

    @Test
    public void chageUsagesShouldNotThrowExceptionIfThereIsNotEnoughUsagesInSpecificMode() {
        testChangeUsagesIfThereIsNotEnough(DiProcessingMode.IGNORE_UNKNOWN_ENTITIES_AND_USAGES);
    }

    private void testChangeUsagesIfThereIsNotEnough(@NotNull final DiProcessingMode mode) {
        final String key = "key";
        final Project project = Project.withKey(key).name("").build();
        final Project lyadzhin = Project.withKey(Project.Key.of(key, new Person("lyadzhin", 1120000000014351L, false, false, false, PersonAffiliation.YANDEX))).name("").parent(project).build();
        final Project inikifor = Project.withKey(Project.Key.of(key, new Person("inikifor", 1120000000018258L, false, false, false, PersonAffiliation.YANDEX))).name("").parent(project).build();
        final Project welvet = Project.withKey(Project.Key.of(key, new Person("welvet", 1120000000025633L, false, false, false, PersonAffiliation.YANDEX))).name("").parent(project).build();

        final Entity e = Mocks.entity();

        final Map<Project, Integer> allUsages = new HashMap<Project, Integer>() {{
            put(lyadzhin, 1);
            put(inikifor, 2);
            put(welvet, 3);
        }};

        new EntityUsageDiff(mode, e, lyadzhin, DiMetaValueSet.EMPTY, -7, null).processOverUsages(allUsages);
    }
}