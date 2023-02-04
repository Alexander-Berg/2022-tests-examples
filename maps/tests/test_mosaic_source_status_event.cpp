#include <maps/factory/libs/db/mosaic_source_status_event_gateway.h>
#include <maps/factory/libs/db/acl.h>
#include <maps/factory/libs/db/mosaic_source_pipeline.h>
#include <maps/factory/libs/db/tests/test_data.h>

#include <maps/factory/libs/unittest/fixture.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::factory::db::tests {
using namespace table::alias;

Y_UNIT_TEST_SUITE(test_status_event_gateway) {

Y_UNIT_TEST(test_creating_status_event)
{
    MosaicSource ms(TEST_MOSAIC_SOURCE_NAME);
    ms.setMercatorGeom(TEST_MOSAIC_SOURCE_GEOMETRY);
    ms.setMinZoom(10);
    ms.setMaxZoom(18);
    ms.setSatellite(TEST_MOSAIC_SOURCE_SATELLITE);

    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());
    {
        pqxx::work txn(conn);
        MosaicSourcePipeline(txn).insertNew(ms);
        txn.commit();
    }

    {
        pqxx::work txn(conn);
        MosaicSourcePipeline(txn).transition(ms,
            MosaicSourceStatus::Rejected, UserRole{42, Role::Customer}, "Bad one");
        txn.commit();
    }

    {
        pqxx::work txn(conn);
        auto events = MosaicSourceStatusEventGateway(txn).load(
            _MosaicSourceStatusEvent::mosaicSourceId == ms.id()
        );
        ASSERT_EQ(events.size(), 2u);
        EXPECT_EQ(events[0].status(), MosaicSourceStatus::New);
        EXPECT_EQ(events[0].createdBy(), 0);
        EXPECT_EQ(events[0].comment(), "");
        EXPECT_EQ(events[1].status(), MosaicSourceStatus::Rejected);
        EXPECT_EQ(events[1].createdBy(), 42);
        EXPECT_EQ(events[1].comment(), "Bad one");
    }
}

} // suite

} // namespace maps::factory::db::tests
