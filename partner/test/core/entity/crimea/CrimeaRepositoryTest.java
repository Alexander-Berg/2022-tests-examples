package ru.yandex.partner.core.entity.crimea;

import java.util.List;
import java.util.Map;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreConstants;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.dbschema.partner.Tables;


@CoreTest
@ExtendWith(MySqlRefresher.class)
class CrimeaRepositoryTest {

    @Autowired
    CrimeaRepository crimeaRepository;

    @Autowired
    DSLContext dslContext;

    @Test
    void isCrimeaRussianByUserIdFromRussia() {
        Long pageId = 41443L;
        dslContext.update(Tables.USERS)
                .set(Tables.USERS.COUNTRY_ID, CoreConstants.RUSSIA_ID)
                .where(Tables.USERS.ID.eq(1009L))
                .execute();

        Assertions.assertTrue(crimeaRepository.getPageIdsToIsCrimeaRussian(List.of(pageId)).get(pageId));
    }

    @Test
    void isCrimeaRussianByUserIdFromBelarus() {
        Long pageId = 41443L;
        dslContext.update(Tables.USERS)
                .set(Tables.USERS.COUNTRY_ID, CoreConstants.BELARUS_ID)
                .where(Tables.USERS.ID.eq(1009L))
                .execute();

        Assertions.assertTrue(crimeaRepository.getPageIdsToIsCrimeaRussian(List.of(pageId)).get(pageId));
    }

    @Test
    void isCrimeaRussianByUserIdFromKazakhstan() {
        Long pageId = 41443L;
        dslContext.update(Tables.USERS)
                .set(Tables.USERS.COUNTRY_ID, CoreConstants.KAZAKHSTAN_ID)
                .where(Tables.USERS.ID.eq(1009L))
                .execute();

        Assertions.assertTrue(crimeaRepository.getPageIdsToIsCrimeaRussian(List.of(pageId)).get(pageId));
    }

    @Test
    void isCrimeaRussianByUserIdFromUkraine() {
        Long pageId = 41443L;
        dslContext.update(Tables.USERS)
                .set(Tables.USERS.COUNTRY_ID, CoreConstants.UKRAINE_ID)
                .where(Tables.USERS.ID.eq(1009L))
                .execute();

        Assertions.assertFalse(crimeaRepository.getPageIdsToIsCrimeaRussian(List.of(pageId)).get(pageId));
    }

    @Test
    void isCrimeaRussianByUserIdFromGermany() {
        Long pageId = 41443L;
        dslContext.update(Tables.USERS)
                .set(Tables.USERS.COUNTRY_ID, 102L)
                .where(Tables.USERS.ID.eq(1009L))
                .execute();

        Assertions.assertFalse(crimeaRepository.getPageIdsToIsCrimeaRussian(List.of(pageId)).get(pageId));
    }

    @Test
    void allGoodWithNullCountryId() {
        Long pageId = 123456L;
        // у пейджа owner_id = 1011 и country_id = NULL
        Assertions.assertEquals(Map.of(), crimeaRepository.getPageIdsToIsCrimeaRussian(List.of(pageId)));
    }
}
