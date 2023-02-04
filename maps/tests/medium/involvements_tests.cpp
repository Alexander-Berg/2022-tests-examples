#include <maps/libs/geolib/include/conversion.h>
#include <maps/libs/geolib/include/test_tools/comparison.h>

#include <yandex/maps/wiki/social/exception.h>
#include <yandex/maps/wiki/social/involvement.h>
#include <yandex/maps/wiki/social/involvement_stat.h>
#include <yandex/maps/wiki/social/involvement_filter.h>
#include <maps/libs/introspection/include/comparison.h>

#include <library/cpp/testing/unittest/registar.h>
#include "helpers.h"

const double MERC_EPSILON = 1;

namespace maps::wiki::social::tests {

Y_UNIT_TEST_SUITE(involvements_suite) {

Y_UNIT_TEST_F(involvements, DbFixture)
{
    pqxx::work txn(conn);

    geolib3::Polygon2 luzhaGeo({
        {37.549617, 55.713764}, {37.549617, 55.717907}, {37.558328, 55.717980},
        {37.558414, 55.713789}, {37.549617, 55.713764}
    });

    geolib3::Polygon2 olympiyskiyGeo({
        {37.624365, 55.779892}, {37.624172, 55.782516}, {37.628785, 55.782552},
        {37.628678, 55.779856}, {37.624365, 55.779892}
    });

    geolib3::MultiPolygon2 stadiumsGeo({luzhaGeo, olympiyskiyGeo});
    auto stadiumsMerc = geolib3::convertGeodeticToMercator(stadiumsGeo);

    //testing basic Involvement operations
    const std::string TITLE_1980 = "olympics:moscow-1980-title";
    Involvement olympics1980(
        0,
        TITLE_1980,
        "http://moscow-1980.yandex.ru",
        {"cat:cow", "cat:moose"},
        Enabled::Yes,
        chrono::parseIsoDateTime("1980-07-19T00:00:00+03:00")
    );

    olympics1980.setFinish(chrono::parseIsoDateTime("1980-08-03T23:59:59+03:00"));
    olympics1980.setPolygons(stadiumsMerc);

    UNIT_ASSERT_EQUAL(olympics1980.id(), 0);
    UNIT_ASSERT_EQUAL(olympics1980.title(), TITLE_1980);

    olympics1980.writeToDatabase(txn);
    UNIT_ASSERT(olympics1980.id() != 0);

    auto loadedOlympics = Involvement::byId(txn, olympics1980.id());
    UNIT_ASSERT_EQUAL(loadedOlympics.id(), olympics1980.id());
    UNIT_ASSERT_EQUAL(loadedOlympics.title(), olympics1980.title());

    UNIT_ASSERT(
        geolib3::test_tools::approximateEqual(
            loadedOlympics.polygons(),
            olympics1980.polygons(),
            MERC_EPSILON
        )
    );

    const std::string TITLE_2014 = "olympics:sochi-2014-title";
    Involvement olympics2014(
        0,
        TITLE_2014,
        "http://sochi-2014.yandex.ru",
        {"cat:cat"},
        Enabled::No,
        chrono::parseIsoDateTime("2014-02-07T00:00:00+03:00")
    );
    olympics2014.writeToDatabase(txn);

    olympics2014.setTypes({"cat:cat", "cat:dog"});
    olympics2014.writeToDatabase(txn);

    //testing filtering by InvolvementFilter
    //
    {
        auto enabledInvolvements = Involvement::byFilter(
            txn,
            InvolvementFilter().enabled(Enabled::Yes)
        );
        UNIT_ASSERT_EQUAL(enabledInvolvements.size(), 1);
        UNIT_ASSERT_EQUAL(enabledInvolvements.front().id(), olympics1980.id());
    }
    {
        auto emptyInvolvements = Involvement::byFilter(
            txn,
            InvolvementFilter().startedBefore(chrono::parseIsoDateTime("1970-01-01T00:00:00"))
        );
        UNIT_ASSERT(emptyInvolvements.empty());
    }
    {
        auto allInvolvements = Involvement::byFilter(
            txn,
            InvolvementFilter().finishedAfter(chrono::parseIsoDateTime("1970-01-01T00:00:00"))
        );
        UNIT_ASSERT_EQUAL(allInvolvements.size(), 2);
    }

    const std::set<std::string> NEW_CATEGORIES{"cat:owl", "cat:squirrel"};
    olympics2014
        .setEnabled(Enabled::Yes)
        .setTitle("olympics:soci-2014-title")
        .setTypes(NEW_CATEGORIES)
        .setStart(chrono::parseIsoDateTime("2014-02-06T00:00:00+03:00"))
    ;
    olympics2014.writeToDatabase(txn);

    {
        auto activeInvolvements = Involvement::byFilter(
            txn,
            InvolvementFilter().active(Active::Yes)
        );
        UNIT_ASSERT_EQUAL(activeInvolvements.size(), 1);
        UNIT_ASSERT(activeInvolvements.front() == olympics2014);
        UNIT_ASSERT(activeInvolvements.front().polygons().polygonsNumber() == 0);

        auto inactiveInvolvements = Involvement::byFilter(
            txn,
            InvolvementFilter().active(Active::No)
        );
        UNIT_ASSERT_EQUAL(inactiveInvolvements.size(), 1);
        UNIT_ASSERT_EQUAL(inactiveInvolvements.front().id(), olympics1980.id());
    }
    {
        geolib3::Polygon2 mamontovBuildingGeo({
            {37.589777, 55.733116}, {37.588581, 55.733736},
            {37.589418, 55.734157}, {37.590378, 55.733503},
            {37.589777, 55.733116}
        });

        auto mamontovBuildingBboxMerc = geolib3::convertGeodeticToMercator(
            mamontovBuildingGeo.boundingBox()
        );

        auto involvementsOutsideStadions = Involvement::byFilter(
            txn,
            InvolvementFilter().boundedBy(mamontovBuildingBboxMerc)
        );

        UNIT_ASSERT_EQUAL(involvementsOutsideStadions.size(), 1);
        UNIT_ASSERT_EQUAL(
            involvementsOutsideStadions.front().id(),
            olympics2014.id()
        );
    }
    {
        geolib3::BoundingBox insideLuzhnikiBboxGeo(
            {37.553504, 55.715597}, {37.553767, 55.715742}
        );

        auto insideLuzhnikiBboxMerc = geolib3::convertGeodeticToMercator(
            insideLuzhnikiBboxGeo
        );

        auto involvementsInsideStadions = Involvement::byFilter(
            txn,
            InvolvementFilter().boundedBy(insideLuzhnikiBboxMerc)
        );

        UNIT_ASSERT_EQUAL(involvementsInsideStadions.size(), 2);
    }

    //testing operations with InvolvementStat objects
    //
    InvolvementStat counter1(olympics1980.id(), "cat:cow");
    counter1 += 500;
    UNIT_ASSERT_EQUAL(counter1.value(), 500);
    counter1.writeToDatabase(txn);
    UNIT_ASSERT_EQUAL(counter1.value(), 500);
    counter1 -= 500;
    UNIT_ASSERT_EQUAL(counter1.value(), 0);
    counter1.writeToDatabase(txn);
    UNIT_ASSERT_EQUAL(counter1.value(), 0);

    InvolvementStat counter2(olympics2014.id(), "cat:cat");
    counter2.writeToDatabase(txn);

    auto counters1980 = InvolvementStat::byInvolvementId(txn, olympics1980.id());
    UNIT_ASSERT_EQUAL(counters1980.size(), 1);
    UNIT_ASSERT_EQUAL(counters1980.front().type(), "cat:cow");
    UNIT_ASSERT_EQUAL(counters1980.front().value(), 0);

    //zero-valued counters aren't written to database
    auto counters2014 = InvolvementStat::byInvolvementId(txn, olympics2014.id());
    UNIT_ASSERT(counters2014.empty());
    //zero-valued counters can be requested by calling loadInvolvementStatMap
    auto involvementMap = loadInvolvementStatMap(txn, {olympics2014});
    const auto& stats = involvementMap.at(olympics2014);
    UNIT_ASSERT_EQUAL(stats.size(), 2);
    UNIT_ASSERT_EQUAL(stats.front().type(), "cat:owl");
    UNIT_ASSERT_EQUAL(stats.back().type(), "cat:squirrel");

    auto allCounters = InvolvementStat::byInvolvements(
        txn,
        {olympics1980, olympics2014}
    );
    UNIT_ASSERT_EQUAL(allCounters.size(), 1);

    const TId WRONG_ID = 1005003;
    UNIT_ASSERT_EXCEPTION(Involvement::byId(txn, WRONG_ID), InvolvementNotFound);

    //writing involvement with non-zero but non-existing id to database
    //will result in InvolvementNotFound exception being thrown
    Involvement wrongInvolvement(
        WRONG_ID,
        "wrong:title",
        "wrong:url",
        {},
        Enabled::Yes,
        chrono::TimePoint(std::chrono::system_clock::now())
    );
    wrongInvolvement.setFinish(chrono::TimePoint(std::chrono::system_clock::now()));

    UNIT_ASSERT_EXCEPTION(wrongInvolvement.writeToDatabase(txn), InvolvementNotFound);

    UNIT_ASSERT_NO_EXCEPTION(txn.commit());
}

}

} // namespace maps::wiki::social::tests
