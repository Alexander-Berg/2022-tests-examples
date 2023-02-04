package ru.yandex.partner.core.entity.simplemodels.kvstorefrontend.service;

import java.util.List;

import one.util.streamex.StreamEx;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.kvstorefrontend.model.KvStoreFrontend;
import ru.yandex.partner.core.entity.simplemodels.kvstorefrontend.filter.KvStoreFrontendFilters;
import ru.yandex.partner.core.entity.simplemodels.kvstorefrontend.repository.KvStoreFrontendRepository;
import ru.yandex.partner.core.entity.utils.DSLUtils;
import ru.yandex.partner.core.filter.CoreFilterNode;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.defaultconfiguration.PartnerLocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.partner.dbschema.partner.Tables.KV_STORE_FRONTEND;

@ExtendWith(MySqlRefresher.class)
@CoreTest
class KvStoreFrontendRepositoryTest {

    @Autowired
    KvStoreFrontendRepository repository;

    @Autowired
    DSLContext dslContext;

    @Test
    void testCreateOrUpdate() {

        var newKvStore = new KvStoreFrontend()
                .withUserId(1009L)
                .withKey("new_key")
                .withValue("my_value");

        var existingKvStore1 = new KvStoreFrontend()
                .withUserId(1009L)
                .withKey("test_key_1")
                .withValue("newValue");
        var existingKvStore2 = new KvStoreFrontend()
                .withUserId(1009L)
                .withKey("test_key_2")
                .withValue("newValue2");

        var result = repository.createOrUpdate(List.of(newKvStore, existingKvStore1, existingKvStore2));
        assertThat(result.size()).isEqualTo(3);
        var data = StreamEx.of(repository.getAll(CoreFilterNode.in(KvStoreFrontendFilters.USER_ID,
                List.of(1009L)), null, null, false)).toMap(KvStoreFrontend::getKey, KvStoreFrontend::getValue);

        assertThat(data.get("new_key")).contains("my_value");
        assertThat(data.get("test_key_1")).contains("newValue");
        assertThat(data.get("test_key_2")).contains("newValue2");

    }

    @Test
    void insertOnDuplicateKeyTest() {
        var toAdd = List.of(new KvStoreFrontend()
                        .withUserId(1009L)
                        .withKey("new_key")
                        .withValue("my_value"),
                new KvStoreFrontend()
                        .withUserId(1009L)
                        .withKey("new_key_2")
                        .withValue("newValue"));

        var now = PartnerLocalDateTime.now();
        var query = dslContext.insertInto(
                KV_STORE_FRONTEND,
                KV_STORE_FRONTEND.USER_ID,
                KV_STORE_FRONTEND.KEY,
                KV_STORE_FRONTEND.VALUE,
                KV_STORE_FRONTEND.CREATE_TIME,
                KV_STORE_FRONTEND.UPDATE_TIME
        );

        for (var entity : toAdd) {
            query.values(
                    entity.getUserId(),
                    entity.getKey(),
                    entity.getValue(),
                    now,
                    now
            );
        }
        query.onDuplicateKeyUpdate()
                .set(KV_STORE_FRONTEND.VALUE, DSLUtils.getValuesStatement(KV_STORE_FRONTEND.VALUE))
                .set(KV_STORE_FRONTEND.UPDATE_TIME, DSLUtils.getValuesStatement(KV_STORE_FRONTEND.UPDATE_TIME));
        var result = query.returning().fetch();
        assertThat(result.size()).isEqualTo(2);
    }
}
