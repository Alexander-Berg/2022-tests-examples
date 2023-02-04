#include <maps/factory/libs/db/aoi.h>
#include <maps/factory/libs/db/aoi_gateway.h>
#include <maps/factory/libs/db/aoi_validation_error_gateway.h>
#include <maps/factory/libs/db/order.h>
#include <maps/factory/libs/db/order_gateway.h>

#include <maps/factory/libs/unittest/fixture.h>

#include <maps/libs/introspection/include/comparison.h>
#include <maps/libs/introspection/include/stream_output.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <pqxx/pqxx>

#include <string>

namespace maps::factory::db {

using introspection::operator==;
using introspection::operator!=;
using introspection::operator<<;

} // namespace maps::factory::db

namespace maps::factory::db::tests {

using namespace table::alias;

constexpr int32_t TEST_ORDER_YEAR = 2018;
constexpr OrderType TEST_ORDER_TYPE = OrderType::Tasking;

Y_UNIT_TEST_SUITE(test_aoi_and_order_gateways) {

Y_UNIT_TEST(test_creating_order)
{

    const std::string name = "test order";
    Order testOrder(TEST_ORDER_YEAR, TEST_ORDER_TYPE);
    testOrder.setName(name);

    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());
    {
        pqxx::work txn(conn);
        OrderGateway(txn).insert(testOrder);
        txn.commit();

        EXPECT_EQ(testOrder.id(), 1);
        EXPECT_EQ(testOrder.status(), OrderStatus::Draft);
        EXPECT_EQ(testOrder.name(), name);
    }

    testOrder.setStatusToReady();
    {
        pqxx::work txn(conn);
        OrderGateway(txn).update(testOrder);
        txn.commit();
    }

    {
        pqxx::work txn(conn);
        auto loadedOrder = OrderGateway(txn).loadById(testOrder.id());

        EXPECT_EQ(loadedOrder.status(), OrderStatus::Ready);
        EXPECT_FALSE(loadedOrder.rejectionReason());
    }

    testOrder.setStatusToRejected("This order is bad");
    {
        pqxx::work txn(conn);
        OrderGateway(txn).update(testOrder);
        txn.commit();
    }

    {
        pqxx::work txn(conn);
        auto loadedOrder = OrderGateway(txn).loadById(testOrder.id());

        EXPECT_EQ(loadedOrder.status(), OrderStatus::Rejected);
        ASSERT_TRUE(loadedOrder.rejectionReason());
        EXPECT_EQ(*loadedOrder.rejectionReason(), "This order is bad");
    }
}

Y_UNIT_TEST(test_creating_aoi_for_order)
{
    Order testOrder(TEST_ORDER_YEAR, TEST_ORDER_TYPE);

    const std::string TEST_AOI_NAME = "Москва";
    const geolib3::Polygon2 TEST_AOI_GEOMETRY({
        {0, 0},
        {0, 1},
        {1, 1},
        {1, 0},
        {0, 0}
    });

    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());
    {
        pqxx::work txn(conn);
        OrderGateway(txn).insert(testOrder);
        txn.commit();

        EXPECT_EQ(testOrder.id(), 1);
        EXPECT_EQ(testOrder.status(), OrderStatus::Draft);
    }

    Aoi testAoi(testOrder.id(), TEST_AOI_NAME, TEST_AOI_GEOMETRY);
    {
        pqxx::work txn(conn);
        AoiGateway(txn).insert(testAoi);
        txn.commit();

        EXPECT_EQ(testAoi.id(), 1);
        EXPECT_EQ(testAoi.name(), TEST_AOI_NAME);
    }

    {
        pqxx::work txn(conn);
        auto aois = AoiGateway(txn).load(_Aoi::orderId == testOrder.id());

        ASSERT_EQ(aois.size(), 1u);
        EXPECT_EQ(aois[0].name(), TEST_AOI_NAME);
    }

    {
        const std::vector<std::string> CATALOG_IDS{"123", "456"};

        pqxx::work txn(conn);
        testAoi.setCatalogIds(CATALOG_IDS);
        EXPECT_EQ(testAoi.catalogIds(), CATALOG_IDS);

        AoiGateway(txn).update(testAoi);
    }
}

Y_UNIT_TEST(test_creating_aoi_validation_errors)
{
    Order testOrder(TEST_ORDER_YEAR, TEST_ORDER_TYPE);

    const std::string TEST_AOI_NAME = "Москва";
    const geolib3::Polygon2 TEST_AOI_GEOMETRY({
        {0, 0},
        {0, 1},
        {1, 1},
        {1, 0},
        {0, 0}
    });

    unittest::Fixture fixture;
    pqxx::connection conn(fixture.postgres().connectionString());
    {
        pqxx::work txn(conn);
        OrderGateway(txn).insert(testOrder);
        txn.commit();

        EXPECT_EQ(testOrder.id(), 1);
        EXPECT_EQ(testOrder.status(), OrderStatus::Draft);
    }

    Aoi testAoi(testOrder.id(), TEST_AOI_NAME, TEST_AOI_GEOMETRY);
    {
        pqxx::work txn(conn);
        AoiGateway(txn).insert(testAoi);
        txn.commit();

        EXPECT_EQ(testAoi.id(), 1);
        EXPECT_EQ(testAoi.name(), TEST_AOI_NAME);
    }

    AoiValidationError segmentLengthValidation(testAoi.id(), AoiValidationMetric::SegmentLength, 10);
    segmentLengthValidation.setMinAllowedValue(100);

    AoiValidationError areaValidation(testAoi.id(), AoiValidationMetric::Area, 20);
    areaValidation.setMinAllowedValue(200);

    AoiValidationError verticesNumberValidation(testAoi.id(), AoiValidationMetric::VerticesNumber, 2000);
    verticesNumberValidation.setMinAllowedValue(4);
    verticesNumberValidation.setMaxAllowedValue(1000);

    {
        pqxx::work txn(conn);
        AoiValidationErrorGateway gtw(txn);
        gtw.insert(segmentLengthValidation);
        gtw.insert(areaValidation);
        gtw.insert(verticesNumberValidation);
        txn.commit();

        EXPECT_EQ(segmentLengthValidation.id(), 1);
    }

    {
        pqxx::work txn(conn);
        AoiValidationErrorGateway gtw(txn);
        auto errors = AoiValidationErrorGateway(txn).load();

        ASSERT_EQ(errors.size(), 3u);
        EXPECT_THAT(errors,
            ::testing::ElementsAre(segmentLengthValidation, areaValidation, verticesNumberValidation));
    }
}

} // suite

} // namespace maps::factory::db::tests
