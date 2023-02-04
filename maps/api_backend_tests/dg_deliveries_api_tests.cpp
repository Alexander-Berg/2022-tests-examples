#include <maps/factory/services/sputnica_back/lib/yacare_helpers.h>
#include <maps/factory/services/sputnica_back/tests/api_backend_tests/fixture.h>
#include <maps/factory/services/sputnica_back/tests/common/common.h>
#include <maps/factory/services/sputnica_back/tests/common/test_data.h>

#include <maps/factory/libs/db/aoi_gateway.h>
#include <maps/factory/libs/db/dg_delivery_gateway.h>
#include <maps/factory/libs/db/order_gateway.h>
#include <maps/factory/libs/sproto_helpers/dg_delivery.h>
#include <maps/factory/libs/tasks/impl/tasks_gateway.h>

#include <maps/infra/yacare/include/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace maps::factory::sputnica::tests {

Y_UNIT_TEST_SUITE_F(dg_deliveries_api_tests, Fixture) {

constexpr auto TEST_ORDER_NO = "058800151040_01";
constexpr auto TEST_AOI = "Kahramanmaras_AC";
constexpr auto TEST_ORDER_ID = 42;
constexpr auto TEST_OTHER_AOI = "Moscow";
constexpr auto TEST_OTHER_ORDER_ID = 1;
constexpr auto TEST_NOT_FOUND_ORDER_NO = "058800151055_01";

std::string testDeliveryPath()
{
    return ArcadiaSourceRoot() + "/maps/factory/test_data/dg_deliveries/058800151040_01";
}

void addTestDeliveries(const std::string& connStr)
{
    pqxx::connection conn(connStr);
    pqxx::work txn(conn);
    {
        auto dbDl = db::DgDelivery{TEST_ORDER_NO, testDeliveryPath()};
        dbDl.setAreaDescription(TEST_AOI);
        dbDl.setOrderId(TEST_ORDER_ID);
        dbDl.addErrorMessage("Some error");
        db::DgDeliveryGateway{txn}.insert(dbDl);
    }
    {
        auto dbDl = db::DgDelivery{"058800151041_01", "./"};
        dbDl.setAreaDescription(TEST_AOI);
        dbDl.setOrderId(TEST_ORDER_ID);
        db::DgDeliveryGateway{txn}.insert(dbDl);
    }
    txn.commit();
}

void addTestOtherOrderAndAoi(const std::string& connStr)
{
    const auto poly = geolib3::Polygon2{{{0, 0}, {0, 1}, {1, 0}}};
    pqxx::connection conn(connStr);
    pqxx::work txn(conn);
    auto order = db::Order{2077, db::OrderType::Tasking};
    db::OrderGateway{txn}.insert(order);
    auto aoi = db::Aoi{order.id(), TEST_OTHER_AOI, poly};
    db::AoiGateway{txn}.insert(aoi);
    txn.commit();
}

sproto_helpers::sfactory::DgDeliveryData testDeliveryData()
{
    auto sdld = sproto_helpers::sfactory::DgDeliveryData{};
    sdld.areaDescription() = TEST_OTHER_AOI;
    sdld.orderId() = TEST_OTHER_ORDER_ID;
    return sdld;
}

void checkTestDelivery(const sproto_helpers::sfactory::DgDelivery& sdl)
{
    EXPECT_EQ(sdl.orderNumber(), TEST_ORDER_NO);
    EXPECT_EQ(sdl.areaDescription(), TEST_AOI);
    EXPECT_EQ(sdl.orderId(), TEST_ORDER_ID);
    ASSERT_TRUE(sdl.errorMessage());
    EXPECT_EQ(*sdl.errorMessage(), "Some error;");
}

Y_UNIT_TEST(test_get_invalid)
{
    addTestDeliveries(postgres().connectionString());
    auto rq = http::MockRequest{http::GET,
        http::URL{"http://localhost/dg-deliveries/list-invalid"}};
    setAuthHeaderFor(db::Role::Viewer, rq);
    auto resp = yacare::performTestRequest(rq);
    ASSERT_EQ(resp.status, 200);
    const auto sdls = boost::lexical_cast<sproto_helpers::sfactory::DgDeliveries>(resp.body);
    ASSERT_EQ(sdls.deliveries().size(), 1u);
    checkTestDelivery(sdls.deliveries()[0]);
}

Y_UNIT_TEST(test_get)
{
    addTestDeliveries(postgres().connectionString());
    auto rq = http::MockRequest{http::GET,
        http::URL{"http://localhost/dg-deliveries/get"}
            .addParam("orderNumber", TEST_ORDER_NO)};
    setAuthHeaderFor(db::Role::Viewer, rq);
    auto resp = yacare::performTestRequest(rq);
    ASSERT_EQ(resp.status, 200);
    const auto sdl = boost::lexical_cast<sproto_helpers::sfactory::DgDelivery>(resp.body);
    checkTestDelivery(sdl);
}

Y_UNIT_TEST(test_get_not_found)
{
    addTestDeliveries(postgres().connectionString());
    auto rq = http::MockRequest{http::GET,
        http::URL{"http://localhost/dg-deliveries/get"}
            .addParam("orderNumber", TEST_NOT_FOUND_ORDER_NO)};
    setAuthHeaderFor(db::Role::Viewer, rq);
    auto resp = yacare::performTestRequest(rq);
    ASSERT_EQ(resp.status, 404);
}

Y_UNIT_TEST(test_update_not_allowed)
{
    addTestDeliveries(postgres().connectionString());
    auto rq = http::MockRequest{http::PATCH,
        http::URL{"http://localhost/dg-deliveries/update"}
            .addParam("orderNumber", TEST_ORDER_NO)};
    setAuthHeaderFor(db::Role::Viewer, rq);
    rq.body = boost::lexical_cast<std::string>(testDeliveryData());

    auto resp = yacare::performTestRequest(rq);
    ASSERT_EQ(resp.status, 403);
}

Y_UNIT_TEST(test_update_not_found)
{
    addTestDeliveries(postgres().connectionString());
    auto rq = http::MockRequest{http::PATCH,
        http::URL{"http://localhost/dg-deliveries/update"}
            .addParam("orderNumber", TEST_NOT_FOUND_ORDER_NO)};
    setAuthHeaderFor(db::Role::Customer, rq);
    rq.body = boost::lexical_cast<std::string>(testDeliveryData());

    auto resp = yacare::performTestRequest(rq);
    ASSERT_EQ(resp.status, 404);
}

Y_UNIT_TEST(test_update_to_invalid)
{
    addTestDeliveries(postgres().connectionString());
    auto rq = http::MockRequest{http::PATCH,
        http::URL{"http://localhost/dg-deliveries/update"}
            .addParam("orderNumber", TEST_ORDER_NO)};
    setAuthHeaderFor(db::Role::Customer, rq);
    rq.body = boost::lexical_cast<std::string>(testDeliveryData());

    auto resp = yacare::performTestRequest(rq);
    ASSERT_EQ(resp.status, 200);

    const auto sdl = boost::lexical_cast<sproto_helpers::sfactory::DgDelivery>(resp.body);
    EXPECT_EQ(sdl.orderNumber(), TEST_ORDER_NO);
    EXPECT_EQ(sdl.areaDescription(), TEST_OTHER_AOI);
    EXPECT_EQ(sdl.orderId(), TEST_OTHER_ORDER_ID);
    ASSERT_TRUE(sdl.errorMessage());
    EXPECT_EQ(*sdl.errorMessage(), "Cannot find AOI with name 'Moscow' and order id 1;");

    pqxx::connection conn(postgres().connectionString());
    pqxx::work txn(conn);
    const auto tasks = tasks::TasksGateway{txn}.load();
    EXPECT_EQ(tasks.size(), 0u);
    const auto dbDl = db::DgDeliveryGateway{txn}.loadById(TEST_ORDER_NO);
    EXPECT_EQ(dbDl.areaDescription(), TEST_OTHER_AOI);
    EXPECT_EQ(dbDl.orderId(), TEST_OTHER_ORDER_ID);
    ASSERT_TRUE(dbDl.hasError());
    EXPECT_EQ(dbDl.errorMessage(), "Cannot find AOI with name 'Moscow' and order id 1;");
}

Y_UNIT_TEST(test_update_and_process)
{
    addTestDeliveries(postgres().connectionString());
    addTestOtherOrderAndAoi(postgres().connectionString());
    auto rq = http::MockRequest{http::PATCH,
        http::URL{"http://localhost/dg-deliveries/update"}
            .addParam("orderNumber", TEST_ORDER_NO)};
    setAuthHeaderFor(db::Role::Customer, rq);
    rq.body = boost::lexical_cast<std::string>(testDeliveryData());

    auto resp = yacare::performTestRequest(rq);
    ASSERT_EQ(resp.status, 200);

    const auto sdl = boost::lexical_cast<sproto_helpers::sfactory::DgDelivery>(resp.body);
    EXPECT_EQ(sdl.orderNumber(), TEST_ORDER_NO);
    EXPECT_EQ(sdl.areaDescription(), TEST_OTHER_AOI);
    EXPECT_EQ(sdl.orderId(), TEST_OTHER_ORDER_ID);
    EXPECT_FALSE(sdl.errorMessage());

    pqxx::connection conn(postgres().connectionString());
    pqxx::work txn(conn);
    const auto tasks = tasks::TasksGateway{txn}.load();
    EXPECT_EQ(tasks.size(), 1u);
    const auto dbDl = db::DgDeliveryGateway{txn}.loadById(TEST_ORDER_NO);
    EXPECT_EQ(dbDl.areaDescription(), TEST_OTHER_AOI);
    EXPECT_EQ(dbDl.orderId(), TEST_OTHER_ORDER_ID);
    EXPECT_FALSE(dbDl.hasError());
}

} // suite
} // namespace maps::factory::sputnica::tests
