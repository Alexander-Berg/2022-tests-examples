#include <maps/wikimap/infopoints_hypgen/fastcgi/lib/fetcher.h>
#include <maps/wikimap/infopoints_hypgen/libs/unittest/include/db_fixture.h>
#include <maps/wikimap/infopoints_hypgen/libs/db/include/event.h>
#include <maps/wikimap/infopoints_hypgen/libs/db/include/hypothesis.h>
#include <maps/wikimap/infopoints_hypgen/libs/db/include/timestamp.h>

#include <maps/libs/infopoints_client/include/client.h>
#include <maps/libs/infopoints_client/include/models.h>

#include <library/cpp/testing/unittest/registar.h>

#include <string>

namespace hypgen = maps::wiki::infopoints_hypgen;
namespace db = hypgen::db;
namespace ic = maps::infopoints_client;

const bool NEW_FLAG = true;
const std::string UID_1 = "1";
const std::string UID_2 = "2";
const std::string UID_3 = "3";
const std::string UID_4 = "4";
const maps::geolib3::Point2 geoPoint = { 45.0, 45.0 };

namespace {

struct InfopointData {
    std::string id;
    ic::Type type;
    maps::geolib3::Point2 position;
};

struct EventData {
    std::string id;
    db::EventType type;
    maps::geolib3::Point2 position;
};

struct TestData {
    std::vector<InfopointData> newInfopoints;
    std::vector<EventData> expectedEvents;
};

class FakeInfopointsClient : public ic::IClient {
public:
    FakeInfopointsClient(const std::vector<InfopointData>& data)
    {
        for (const auto& ip : data) {
            buffer_.push_back(ic::Infopoint{
                "u" + ip.id,
                {{"", ""}},
                std::chrono::time_point_cast<ic::TimePoint::duration>(
                    maps::chrono::TimePoint::clock::now()),
                std::chrono::time_point_cast<ic::TimePoint::duration>(
                    maps::chrono::TimePoint::clock::now()),
                ip.type,
                "",
                "",
                ip.position,
                std::nullopt,
                std::nullopt
            });
        }
    }

    std::optional<ic::Infopoint> get(const std::string& /*id*/) override
    {
        throw maps::RuntimeError() << "Invalid infopoints client call";
    }

    ic::Infopoints get(
        const ic::InfopointsFilter& /*filter*/,
        ic::DescriptionDetalization /*detalization*/) override
    {
        ic::Infopoints ret;
        ret.swap(buffer_);
        return ret;
    }

    ic::Infopoint create(const ic::NewInfopoint& /*newInfopoint*/) override
    {
        throw maps::RuntimeError() << "Invalid infopoints client call";
    }

    ic::Infopoint createForced(const ic::NewInfopoint& /*newInfopoint*/) override
    {
        throw maps::RuntimeError() << "Invalid infopoints client call";
    }

    ic::Infopoint put(
        const std::string& /*id*/,
        const ic::NewInfopoint& /*newInfopoint*/
    ) override
    {
        throw maps::RuntimeError() << "Invalid infopoints client call";
    }

    bool drop(const std::string& /*id*/) override {
        throw maps::RuntimeError() << "Invalid infopoints client call";
    }

    ~FakeInfopointsClient() override = default;

private:
    ic::Infopoints buffer_;
};

class FakeGeobase : public hypgen::IGeobase {
public:
    std::vector<int> getRegionsIds(maps::geolib3::Point2 /*geoPosition*/) const override
    {
        return { 0 };
    }
};

void checkEvents(
    maps::pgpool3::Pool& pool,
    std::vector<EventData> expectedEvents
) {
    auto txn = pool.slaveTransaction();
    auto events = db::EventGateway(*txn).load();

    UNIT_ASSERT_VALUES_EQUAL(expectedEvents.size(), events.size());
    if (events.empty()) {
        return;
    }

    std::sort(
        expectedEvents.begin(),
        expectedEvents.end(),
        [](const EventData& lhs, const EventData& rhs) {
            return lhs.id < rhs.id;
        }
    );
    std::sort(
        events.begin(),
        events.end(),
        [](const db::Event& lhs, const db::Event& rhs) {
            return lhs.uid() < rhs.uid();
        }
    );

    for (size_t i = 0; i < events.size(); ++i) {
        UNIT_ASSERT_VALUES_EQUAL(events[i].isNew(), NEW_FLAG);
        UNIT_ASSERT_VALUES_EQUAL(events[i].infopointId(), expectedEvents[i].id);
        UNIT_ASSERT_VALUES_EQUAL(events[i].type(), expectedEvents[i].type);

        UNIT_ASSERT_VALUES_EQUAL(
            maps::geolib3::compare(
                events[i].position(),
                expectedEvents[i].position
            ),
            0
        );
    }
}

void prepareDb(maps::pgpool3::Pool& pool) {
    auto txn = pool.masterWriteableTransaction();
    auto timestampsGateway = db::TimestampsGateway(*txn);

    hypgen::db::Timestamps fetcherTimestamp(
        db::timestamps_key::FETCHER,
        maps::chrono::TimePoint::clock::now()
    );
    timestampsGateway.upsert(fetcherTimestamp);

    txn->commit();
}

void runTest(TestData testData)
{
    auto client = std::make_unique<FakeInfopointsClient>(testData.newInfopoints);

    hypgen::unittest::DbFixture dbFixture;
    prepareDb(dbFixture.pool());

    hypgen::Fetcher fetcher(
        dbFixture.pool(),
        std::move(client),
        std::make_shared<FakeGeobase>()
    );

    fetcher.doWorkCycle();

    checkEvents(dbFixture.pool(), testData.expectedEvents);
}

} // namespace

Y_UNIT_TEST_SUITE(FetcherTests)
{
    Y_UNIT_TEST(NoOp)
    {
        runTest({{}, {}});
    }

    Y_UNIT_TEST(RegularWorkflow)
    {
        auto mercPoint = maps::geolib3::convertGeodeticToMercator(geoPoint);
        runTest({
            {
                {UID_1, ic::Type::Feedback, geoPoint},
                {UID_2, ic::Type::Chat, geoPoint},
                {UID_3, ic::Type::Closed, geoPoint},
                {UID_4, ic::Type::Other, geoPoint}
            },
            {
                {UID_1, db::EventType::Feedback, mercPoint},
                {UID_2, db::EventType::Chat, mercPoint},
                {UID_3, db::EventType::Other, mercPoint}, // NB
                {UID_4, db::EventType::Other, mercPoint}
            }
        });
    }

    Y_UNIT_TEST(UniqueInfopointId)
    {
        auto mercPoint = maps::geolib3::convertGeodeticToMercator(geoPoint);
        runTest({
            {
                {UID_1, ic::Type::Chat, geoPoint},
                {UID_2, ic::Type::Feedback, geoPoint},
                {UID_1, ic::Type::Chat, geoPoint}
            },
            {
                {UID_1, db::EventType::Chat, mercPoint},
                {UID_2, db::EventType::Feedback, mercPoint}
            }
        });
    }
}

