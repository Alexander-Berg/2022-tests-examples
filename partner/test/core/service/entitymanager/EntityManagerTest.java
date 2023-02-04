package ru.yandex.partner.core.service.entitymanager;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.block.model.BaseBlock;
import ru.yandex.partner.core.entity.block.model.ContentBlock;
import ru.yandex.partner.core.entity.block.model.InternalMobileRtbBlock;
import ru.yandex.partner.core.entity.block.model.InternalRtbBlock;
import ru.yandex.partner.core.entity.block.model.MobileRtbBlock;
import ru.yandex.partner.core.entity.block.model.RtbBlock;

import static org.junit.jupiter.api.Assertions.assertTrue;


@CoreTest
class EntityManagerTest {

    @Autowired
    EntityManager entityManager;

    @Test
    void missedAnyBlockClassShouldReturnMissed() {
        Set<Class<? extends BaseBlock>> expected = Set.of(
                InternalRtbBlock.class,
                MobileRtbBlock.class,
                InternalMobileRtbBlock.class,
                ContentBlock.class);
        Set<Class<? extends BaseBlock>> actual = entityManager.getMissedBlocks(Set.of(RtbBlock.class));

        assertTrue(expected.containsAll(actual));
        assertTrue(actual.containsAll(expected));
    }

    @Test
    void allMissedShouldReturnAll() {
        Set<Class<? extends BaseBlock>> expected = Set.of(
                RtbBlock.class,
                InternalRtbBlock.class,
                MobileRtbBlock.class,
                InternalMobileRtbBlock.class,
                ContentBlock.class);
        Set<Class<? extends BaseBlock>> actual = entityManager.getMissedBlocks(Set.of());

        assertTrue(expected.containsAll(actual));
        assertTrue(actual.containsAll(expected));
    }

    @Test
    void noMissedShouldReturnEmpty() {
        Set<Class<? extends BaseBlock>> expected = Set.of();
        Set<Class<? extends BaseBlock>> actual = entityManager
                .getMissedBlocks(Set.of(
                        RtbBlock.class,
                        InternalRtbBlock.class,
                        MobileRtbBlock.class,
                        InternalMobileRtbBlock.class,
                        ContentBlock.class));

        assertTrue(expected.containsAll(actual));
        assertTrue(actual.containsAll(expected));
    }

}
