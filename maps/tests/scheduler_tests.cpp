#include <maps/wikimap/infopoints_hypgen/fastcgi/lib/scheduler.h>
#include <maps/wikimap/infopoints_hypgen/libs/unittest/include/db_fixture.h>
#include <maps/wikimap/infopoints_hypgen/libs/db/include/event.h>
#include <maps/wikimap/infopoints_hypgen/libs/db/include/hypothesis.h>

#include <library/cpp/testing/unittest/registar.h>

#include <string>

namespace hypgen = maps::wiki::infopoints_hypgen;
namespace db = hypgen::db;
namespace fc = maps::feedback_client;

const int MOSCOW_REGION_ID = 213;
const int TURKEY_REGION_ID = 983;
const std::string UID = "1";
const std::string INFOPOINT_ID_PREFIX = "infopoint id ";
const bool NEW_FLAG = true;

namespace {

struct EventData
{
    std::string comment;
    double x;
    double y;
    std::chrono::hours timeShift;
    int regionId;
};

struct HypothesisData
{
    maps::geolib3::Point2 position;
};

struct TestData
{
    std::vector<EventData> newEvents;
    std::vector<HypothesisData> expectedHypotheses;
};

void uploadNewEvents(
    maps::pgpool3::Pool& pool,
    const std::vector<EventData>& eventData)
{
    auto txn = pool.masterWriteableTransaction();
    db::EventGateway gateway(*txn);
    static int id = 1;
    for (const auto& ed: eventData) {
        db::Event event(
            NEW_FLAG,
            INFOPOINT_ID_PREFIX + std::to_string(id),
            db::EventType::Feedback,
            maps::geolib3::Point2(ed.x, ed.y),
            ed.comment,
            maps::chrono::TimePoint::clock::now() - ed.timeShift,
            UID,
            {ed.regionId}
        );
        gateway.insert(event);
        ++id;

    }
    txn->commit();
}

void checkHypotheses(
    maps::pgpool3::Pool& pool,
    const std::vector<HypothesisData>& expectedHypotheses)
{
    auto txn = pool.slaveTransaction();
    auto hypotheses = db::HypothesisGateway(*txn).load();

    UNIT_ASSERT_VALUES_EQUAL(expectedHypotheses.size(), hypotheses.size());
    if (hypotheses.empty()) {
        return;
    }

    std::vector<maps::geolib3::Point2> points;
    std::transform(
        hypotheses.begin(),
        hypotheses.end(),
        std::back_inserter(points),
        [](const auto& hyp) { return hyp.position(); });

    std::vector<maps::geolib3::Point2> expectedPoints;
    std::transform(
        expectedHypotheses.begin(),
        expectedHypotheses.end(),
        std::back_inserter(expectedPoints),
        [](const auto& hyp) { return hyp.position; });

    std::sort(points.begin(), points.end());
    std::sort(expectedPoints.begin(), expectedPoints.end());

    UNIT_ASSERT(points == expectedPoints);
}

void runWorkCycle(
    const TestData& testData,
    hypgen::unittest::DbFixture& dbFixture,
    std::shared_ptr<fc::IClient> clientPtr)
{
    uploadNewEvents(dbFixture.pool(), testData.newEvents);

    hypgen::Scheduler scheduler(dbFixture.pool(), *clientPtr);
    scheduler.doWorkCycle();

    checkHypotheses(dbFixture.pool(), testData.expectedHypotheses);
}

void runTest(const TestData& testData)
{
    hypgen::unittest::DbFixture dbFixture;
    std::shared_ptr<fc::IClient> clientPtr(new fc::InMemoryClient());

    uploadNewEvents(dbFixture.pool(), testData.newEvents);

    hypgen::Scheduler scheduler(dbFixture.pool(), *clientPtr);
    scheduler.doWorkCycle();

    checkHypotheses(dbFixture.pool(), testData.expectedHypotheses);

}

}

Y_UNIT_TEST_SUITE(Tests)
{

Y_UNIT_TEST(RegularWorkflow)
{
    runTest({
        {
            {"???????????? ????????????", 0., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"???????????? ????????????", 10., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"?????? ?????????????? ", -10., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
        },
        {
            {maps::geolib3::Point2(0., 0.)}
        }
    });
}

Y_UNIT_TEST(TooFarEvents)
{
    runTest({
        {
            {"???????????? ????????????", 0., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"???????????? ????????????", 10., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"?????? ?????????????? ", -1000., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
        },
        {}
    });
}

Y_UNIT_TEST(TooFewEvents)
{
    runTest({
        {
            {"???????????? ????????????", 0., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"???????????? ????????????", 10., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
        },
        {}
    });
}

Y_UNIT_TEST(SeveralHypotheses)
{
    runTest({
        {
            {"???????????? ????????????", 0., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"???????????? ????????????", 10., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"?????? ?????????????? ", -10., 0., std::chrono::hours(0), MOSCOW_REGION_ID},

            {"???????????? ????????????", 1000., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"???????????? ????????????", 1005., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"?????? ?????????????? ", 995., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
        },
        {
            {maps::geolib3::Point2(0., 0.)},
            {maps::geolib3::Point2(1000., 0.)}
        }
    });
}

Y_UNIT_TEST(AdditionalEvents)
{
    runTest({
        {
            {"???????????? ????????????", 0., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"???????????? ????????????", 10., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"?????? ?????????????? ", -10., 0., std::chrono::hours(0), MOSCOW_REGION_ID},

            {"???????????? ????????????", 0., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"???????????? ????????????", 5., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"?????? ?????????????? ", -5., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
        },
        {
            {maps::geolib3::Point2(0., 0.)}
        }
    });
}

Y_UNIT_TEST(AdditionalEventsLater)
{
    hypgen::unittest::DbFixture dbFixture;
    std::shared_ptr<fc::IClient> clientPtr(new fc::InMemoryClient());

    runWorkCycle({
        {
            {"???????????? ????????????", 0., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"???????????? ????????????", 10., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"?????? ?????????????? ", -10., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
        },
        {
            {maps::geolib3::Point2(0., 0.)}
        }
    }, dbFixture, clientPtr);

    runWorkCycle({
        {
            {"???????????? ????????????", 0., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"???????????? ????????????", 5., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"?????? ?????????????? ", 7.5, 0., std::chrono::hours(0), MOSCOW_REGION_ID},
        },
        {
            {maps::geolib3::Point2(0., 0.)}
        }
    }, dbFixture, clientPtr);
}

Y_UNIT_TEST(UnappropriateEvents)
{
    runTest({
        {
            {"???????????? ????????????", 0., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"???????????? ????????????", 10., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"?????? ??????????????", -10., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
        },
        {}
    });
}

Y_UNIT_TEST(TooLateEvents)
{
    runTest({
        {
            {"???????????? ????????????", 0., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"???????????? ????????????", 10., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"?????? ?????????????? ", -10., 0., std::chrono::hours(1000), MOSCOW_REGION_ID},
        },
        {}
    });
}

Y_UNIT_TEST(TurkeyEvents)
{
    runTest({
        {
            {"kapal?? yol", 10., 0., std::chrono::hours(0), TURKEY_REGION_ID},
            {"no entry", -10., 0., std::chrono::hours(26), TURKEY_REGION_ID},
        },
        {
            {maps::geolib3::Point2(0., 0.)}
        }
    });
}

Y_UNIT_TEST(Barrier)
{
    runTest({
        {
            {"????????????????", 0., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"????????", 10., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"??????????", -10., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
        },
        {
            {maps::geolib3::Point2(0., 0.)}
        }
    });
}

Y_UNIT_TEST(Mixed)
{
    runTest({
        {
            {"????????????????", 0., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"????????", 10., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
            {"?????? ?????????????? ", -10., 0., std::chrono::hours(0), MOSCOW_REGION_ID},
        },
        {}
    });
}


};


