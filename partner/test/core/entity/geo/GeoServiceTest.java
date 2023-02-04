package ru.yandex.partner.core.entity.geo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.partner.core.CoreConstants;
import ru.yandex.partner.core.CoreTest;
import ru.yandex.partner.core.entity.QueryOpts;
import ru.yandex.partner.core.entity.block.model.Geo;
import ru.yandex.partner.core.entity.crimea.CrimeaRepository;
import ru.yandex.partner.core.entity.page.model.ContextPage;
import ru.yandex.partner.core.entity.page.model.PageWithOwner;
import ru.yandex.partner.core.entity.page.service.PageService;
import ru.yandex.partner.core.junit.MySqlRefresher;
import ru.yandex.partner.dbschema.partner.Tables;


@CoreTest
@ExtendWith(MySqlRefresher.class)
class GeoServiceTest {

    @Autowired
    GeoService geoService;

    @Autowired
    PageService pageService;

    @Autowired
    DSLContext dslContext;

    @Autowired
    GeoBaseRepository geoBaseRepository;

    @Autowired
    CrimeaRepository crimeaRepository;

    @Test
    void getSubtreesWithRootsCpmOneRootAndRussianOwner() {
        Long pageId = 41443L;

        Long countryId = pageService.findAll(QueryOpts.forClass(ContextPage.class)
                        .withFilterByIds(List.of(pageId))
                        .withProps(PageWithOwner.OWNER)
                )
                .get(0)
                .getOwner()
                .getCountryId();

        Assertions.assertEquals(CoreConstants.RUSSIA_ID, countryId);

        BigDecimal fiftyFive = BigDecimal.valueOf(55L);

        List<Geo> roots = List.of(
                new Geo().withId(225L).withCpm(fiftyFive)
        );

        List<Geo> expected = List.of(
                new Geo().withId(10645L).withCpm(fiftyFive),
                new Geo().withId(225L).withCpm(fiftyFive),
                new Geo().withId(3L).withCpm(fiftyFive),
                new Geo().withId(213L).withCpm(fiftyFive),
                new Geo().withId(26L).withCpm(fiftyFive),
                new Geo().withId(2L).withCpm(fiftyFive),
                new Geo().withId(17L).withCpm(fiftyFive),
                new Geo().withId(977L).withCpm(fiftyFive),
                new Geo().withId(1L).withCpm(fiftyFive),
                new Geo().withId(10174L).withCpm(fiftyFive),
                new Geo().withId(10650L).withCpm(fiftyFive)
        );

        Map<Long, Boolean> map = crimeaRepository.getPageIdsToIsCrimeaRussian(List.of(pageId));
        List<GeoBaseDto> geoBaseDtos = geoService.getDtosWithFilledCrimea(map.get(pageId),
                geoBaseRepository.getRawGeoBase());

        List<Geo> actual = geoService.getSubtreesWithRootsCpm(roots, geoBaseDtos);


        Assertions.assertTrue(expected.containsAll(actual));
        Assertions.assertTrue(actual.containsAll(expected));
    }

    @Test
    void getSubtreesWithRootsCpmTwoRootsAndRussianOwner() {
        Long pageId = 41443L;

        Long countryId = pageService.findAll(QueryOpts.forClass(ContextPage.class)
                        .withFilterByIds(List.of(pageId))
                        .withProps(PageWithOwner.OWNER)
                )
                .get(0)
                .getOwner()
                .getCountryId();

        Assertions.assertEquals(CoreConstants.RUSSIA_ID, countryId);

        BigDecimal fiftyFive = BigDecimal.valueOf(55L);
        BigDecimal sixty = BigDecimal.valueOf(60L);

        List<Geo> roots = List.of(
                new Geo().withId(225L).withCpm(fiftyFive),
                new Geo().withId(1L).withCpm(sixty)
        );

        List<Geo> expected = List.of(
                new Geo().withId(10645L).withCpm(fiftyFive),
                new Geo().withId(225L).withCpm(fiftyFive),
                new Geo().withId(3L).withCpm(fiftyFive),
                new Geo().withId(213L).withCpm(sixty),
                new Geo().withId(26L).withCpm(fiftyFive),
                new Geo().withId(2L).withCpm(fiftyFive),
                new Geo().withId(17L).withCpm(fiftyFive),
                new Geo().withId(977L).withCpm(fiftyFive),
                new Geo().withId(10174L).withCpm(fiftyFive),
                new Geo().withId(1L).withCpm(sixty),
                new Geo().withId(10650L).withCpm(fiftyFive)
        );

        Map<Long, Boolean> map = crimeaRepository.getPageIdsToIsCrimeaRussian(List.of(pageId));
        List<GeoBaseDto> geoBaseDtos = geoService.getDtosWithFilledCrimea(map.get(pageId),
                geoBaseRepository.getRawGeoBase());

        List<Geo> actual = geoService.getSubtreesWithRootsCpm(roots, geoBaseDtos);

        Assertions.assertTrue(expected.containsAll(actual));
        Assertions.assertTrue(actual.containsAll(expected));
    }

    @Test
    void getSubtreesWithRootsCpmOneRootAndUkrainianOwner() {
        Long pageId = 41443L;

        dslContext.update(Tables.USERS)
                .set(Tables.USERS.COUNTRY_ID, CoreConstants.UKRAINE_ID)
                .where(Tables.USERS.ID.eq(1009L))
                .execute();

        Long countryId = pageService.findAll(QueryOpts.forClass(ContextPage.class)
                        .withFilterByIds(List.of(pageId))
                        .withProps(PageWithOwner.OWNER)
                )
                .get(0)
                .getOwner()
                .getCountryId();

        Assertions.assertEquals(CoreConstants.UKRAINE_ID, countryId);

        BigDecimal fortyFive = BigDecimal.valueOf(45L);

        List<Geo> roots = List.of(
                new Geo().withId(166L).withCpm(fortyFive)
        );

        List<Geo> expected = List.of(
                new Geo().withId(20529L).withCpm(fortyFive),
                new Geo().withId(149L).withCpm(fortyFive),
                new Geo().withId(20524L).withCpm(fortyFive),
                new Geo().withId(29630L).withCpm(fortyFive),
                new Geo().withId(20526L).withCpm(fortyFive),
                new Geo().withId(977L).withCpm(fortyFive),
                new Geo().withId(20530L).withCpm(fortyFive),
                new Geo().withId(166L).withCpm(fortyFive),
                new Geo().withId(29629L).withCpm(fortyFive),
                new Geo().withId(187L).withCpm(fortyFive)
        );

        Map<Long, Boolean> map = crimeaRepository.getPageIdsToIsCrimeaRussian(List.of(pageId));
        List<GeoBaseDto> geoBaseDtos = geoService.getDtosWithFilledCrimea(map.get(pageId),
                geoBaseRepository.getRawGeoBase());

        List<Geo> actual = geoService.getSubtreesWithRootsCpm(roots, geoBaseDtos);


        Assertions.assertTrue(expected.containsAll(actual));
        Assertions.assertTrue(actual.containsAll(expected));
    }

    @Test
    void getSubtreesWithRootsCpmTwoRootsAndUkrainianOwner() {
        Long pageId = 41443L;

        dslContext.update(Tables.USERS)
                .set(Tables.USERS.COUNTRY_ID, CoreConstants.UKRAINE_ID)
                .where(Tables.USERS.ID.eq(1009L))
                .execute();

        Long countryId = pageService.findAll(QueryOpts.forClass(ContextPage.class)
                        .withFilterByIds(List.of(pageId))
                        .withProps(PageWithOwner.OWNER)
                )
                .get(0)
                .getOwner()
                .getCountryId();

        Assertions.assertEquals(CoreConstants.UKRAINE_ID, countryId);

        BigDecimal fortyFive = BigDecimal.valueOf(45L);
        BigDecimal twenty = BigDecimal.valueOf(20L);

        List<Geo> roots = List.of(
                new Geo().withId(166L).withCpm(fortyFive),
                new Geo().withId(20524L).withCpm(twenty)

        );

        List<Geo> expected = List.of(
                new Geo().withId(20529L).withCpm(twenty),
                new Geo().withId(149L).withCpm(fortyFive),
                new Geo().withId(20524L).withCpm(twenty),
                new Geo().withId(29630L).withCpm(fortyFive),
                new Geo().withId(20526L).withCpm(fortyFive),
                new Geo().withId(977L).withCpm(fortyFive),
                new Geo().withId(20530L).withCpm(twenty),
                new Geo().withId(166L).withCpm(fortyFive),
                new Geo().withId(29629L).withCpm(fortyFive),
                new Geo().withId(187L).withCpm(fortyFive)
        );

        Map<Long, Boolean> map = crimeaRepository.getPageIdsToIsCrimeaRussian(List.of(pageId));
        List<GeoBaseDto> geoBaseDtos = geoService.getDtosWithFilledCrimea(map.get(pageId),
                geoBaseRepository.getRawGeoBase());

        List<Geo> actual = geoService.getSubtreesWithRootsCpm(roots, geoBaseDtos);

        Assertions.assertTrue(expected.containsAll(actual));
        Assertions.assertTrue(actual.containsAll(expected));
    }
}
