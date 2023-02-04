package ru.yandex.qe.dispenser.domain.dao;

import org.jetbrains.annotations.NotNull;

import ru.yandex.qe.dispenser.api.v1.DiQuotingMode;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.domain.Entity;
import ru.yandex.qe.dispenser.domain.EntitySpec;
import ru.yandex.qe.dispenser.domain.Resource;
import ru.yandex.qe.dispenser.domain.Service;

public enum Mocks {
    ;

    @NotNull
    public static Entity entity() {
        return Entity.builder("").spec(entitySpec()).build();
    }

    @NotNull
    public static EntitySpec entitySpec() {
        return EntitySpec.builder().withKey("").withDescription("").overResource(resource()).build();
    }

    @NotNull
    public static Resource resource() {
        return new Resource.Builder("", service())
                .name("")
                .type(DiResourceType.ENUMERABLE)
                .mode(DiQuotingMode.ENTITIES_ONLY)
                .build();
    }

    @NotNull
    public static Service service() {
        return Service.withKey("").withName("").build();
    }
}
