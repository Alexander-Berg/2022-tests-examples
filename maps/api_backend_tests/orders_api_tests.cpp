#include <maps/factory/services/sputnica_back/tests/common/common.h>
#include <maps/factory/services/sputnica_back/tests/api_backend_tests/fixture.h>
#include <maps/factory/services/sputnica_back/tests/common/test_data.h>
#include <maps/factory/services/sputnica_back/lib/yacare_helpers.h>

#include <maps/factory/libs/db/order.h>
#include <maps/factory/libs/db/order_gateway.h>
#include <maps/factory/libs/sproto_helpers/order.h>

#include <maps/infra/yacare/include/test_utils.h>
#include <maps/libs/common/include/exception.h>
#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <boost/lexical_cast.hpp>

namespace maps::factory::sputnica::tests {

namespace {

sproto_helpers::sfactory::Orders searchOrders()
{
    static const http::URL
        ordersSearchUrl("http://localhost/orders/search?year=" + std::to_string(ORDER_YEAR));

    http::MockRequest rq(http::GET, ordersSearchUrl);
    setAuthHeaderFor(db::Role::Viewer, rq);
    auto resp = yacare::performTestRequest(rq);
    ASSERT(resp.status == 200);
    auto sprotoOrders = boost::lexical_cast<sproto_helpers::sfactory::Orders>(resp.body);
    return sprotoOrders;
}

} //anonymous namespace

/*
 * WARN: Handlers definition can be found in yacare/lib/orders_api.cpp
 */
Y_UNIT_TEST_SUITE_F(orders_api_tests, Fixture) {

Y_UNIT_TEST(test_order_workflow)
{
    std::string orderId;

    //orders list should be empty
    {
        auto sprotoOrders = searchOrders();
        ASSERT_TRUE(sprotoOrders.orders().empty());
    }

    //creating new order
    {
        sproto_helpers::sfactory::NewOrder sprotoNewOrder;
        sprotoNewOrder.year() = ORDER_YEAR;
        sprotoNewOrder.type() = SPROTO_ORDER_TYPE;
        sprotoNewOrder.name() = ORDER_NAME;

        http::MockRequest rq(http::POST, http::URL("http://localhost/orders/add"));
        setAuthHeaderFor(db::Role::Customer, rq);
        rq.body = boost::lexical_cast<std::string>(sprotoNewOrder);
        auto resp = yacare::performTestRequest(rq);

        ASSERT_EQ(resp.status, 200);
        auto sprotoOrder = boost::lexical_cast<sproto_helpers::sfactory::Order>(resp.body);
        ASSERT_EQ(sprotoOrder.year(), ORDER_YEAR);
        ASSERT_EQ(*sprotoOrder.type(), SPROTO_ORDER_TYPE);
        ASSERT_EQ(*sprotoOrder.status(), sproto_helpers::sfactory::Order::Status::DRAFT);
        ASSERT_FALSE(sprotoOrder.acceptedAt());
        ASSERT_FALSE(sprotoOrder.rejectionReason());
        ASSERT_TRUE(sprotoOrder.name().defined());
        ASSERT_EQ(sprotoOrder.name().get(), ORDER_NAME);
        orderId = sprotoOrder.id();
    }

    {
        // import some aois with validation errors
        auto taskingImportUrl = http::URL("http://localhost/orders/import-aois")
            .addParam("orderId", orderId);
        http::MockRequest importRequest(http::POST, taskingImportUrl);
        importRequest.body = common::readFileToString(SRC_("../data/order_aois_validation_errors.json"));
        setAuthHeaderFor(db::Role::Customer, importRequest);
        auto importResponse = yacare::performTestRequest(importRequest);
        ASSERT_EQ(importResponse.status, 200);
    }

    //orders list should contain exactly one order
    {
        auto sprotoOrders = searchOrders();
        ASSERT_EQ(sprotoOrders.orders().size(), 1u);
        ASSERT_EQ(sprotoOrders.orders()[0].id(), orderId);
        ASSERT_EQ(*sprotoOrders.orders()[0].status(), sproto_helpers::sfactory::Order::Status::DRAFT);
    }

    {
        // test search by name
        const http::URL ordersSearchUrl("http://localhost/orders/search?name=" + ORDER_NAME);
        http::MockRequest rq(http::GET, ordersSearchUrl);
        setAuthHeaderFor(db::Role::Viewer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);
        auto sprotoOrders = boost::lexical_cast<sproto_helpers::sfactory::Orders>(resp.body);
        ASSERT_EQ(sprotoOrders.orders().size(), 1u);
        ASSERT_EQ(sprotoOrders.orders()[0].id(), orderId);
    }

    {
        // test search by status
        http::MockRequest rq(http::GET, http::URL("http://localhost/orders/search?status=tmp"));
        setAuthHeaderFor(db::Role::Viewer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT(resp.status == 400);

        http::MockRequest rq2(http::GET, http::URL("http://localhost/orders/search?status=draft"));
        setAuthHeaderFor(db::Role::Viewer, rq2);
        resp = yacare::performTestRequest(rq2);
        ASSERT(resp.status == 200);
        auto sprotoOrders = boost::lexical_cast<sproto_helpers::sfactory::Orders>(resp.body);
        ASSERT_EQ(sprotoOrders.orders().size(), 1u);
        ASSERT_EQ(sprotoOrders.orders()[0].id(), orderId);

        http::MockRequest rq3(http::GET, http::URL("http://localhost/orders/search?status=ready"));
        setAuthHeaderFor(db::Role::Viewer, rq3);
        resp = yacare::performTestRequest(rq3);
        ASSERT(resp.status == 200);
        sprotoOrders = boost::lexical_cast<sproto_helpers::sfactory::Orders>(resp.body);
        ASSERT_EQ(sprotoOrders.orders().size(), 0u);
    }

    const http::URL setOrderStatusUrl("http://localhost/orders/set-status?orderId=" + orderId);

    //trying to change order status to accepted, expecting given request to be refused
    sproto_helpers::sfactory::OrderStatusMessage sprotoOrderStatus;
    sprotoOrderStatus.status() = sproto_helpers::sfactory::Order::Status::ACCEPTED;
    {
        http::MockRequest rq(http::PATCH, setOrderStatusUrl);
        setAuthHeaderFor(db::Role::Supplier, rq);
        rq.body = boost::lexical_cast<std::string>(sprotoOrderStatus);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 403);
    }

    //changing order status to ready
    sprotoOrderStatus.status() = sproto_helpers::sfactory::Order::Status::READY;
    {
        http::MockRequest rq(http::PATCH, setOrderStatusUrl);
        setAuthHeaderFor(db::Role::Customer, rq);
        rq.body = boost::lexical_cast<std::string>(sprotoOrderStatus);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);
    }

    //trying to change order status to rejected, expecting given request to be refused due to the lack of RejectionReason
    sprotoOrderStatus.status() = sproto_helpers::sfactory::Order::Status::REJECTED;
    {
        http::MockRequest rq(http::PATCH, setOrderStatusUrl);
        setAuthHeaderFor(db::Role::Supplier, rq);
        rq.body = boost::lexical_cast<std::string>(sprotoOrderStatus);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 400);
    }

    //providing rejected reason and trying to repeat the request
    sprotoOrderStatus.status() = sproto_helpers::sfactory::Order::Status::REJECTED;
    sprotoOrderStatus.rejectionReason() = "This order will lead to disorder";
    {
        http::MockRequest rq(http::PATCH, setOrderStatusUrl);
        setAuthHeaderFor(db::Role::Supplier, rq);
        rq.body = boost::lexical_cast<std::string>(sprotoOrderStatus);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);

        auto sprotoOrder = boost::lexical_cast<sproto_helpers::sfactory::Order>(resp.body);
        ASSERT_EQ(*sprotoOrder.rejectionReason(), *sprotoOrderStatus.rejectionReason());
    }

    //finally, changing order status back to Draft, so it can be deleted
    sprotoOrderStatus.status() = sproto_helpers::sfactory::Order::Status::DRAFT;
    {
        http::MockRequest rq(http::PATCH, setOrderStatusUrl);
        setAuthHeaderFor(db::Role::Customer, rq);
        rq.body = boost::lexical_cast<std::string>(sprotoOrderStatus);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);
    }

    //deleting the order
    {
        http::MockRequest rq(http::DELETE, "http://localhost/orders/delete?orderId=" + orderId);
        setAuthHeaderFor(db::Role::Customer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);
    }

    //orders list should be empty
    {
        auto sprotoOrders = searchOrders();
        ASSERT_TRUE(sprotoOrders.orders().empty());
    }
}

Y_UNIT_TEST(test_order_update)
{
    pqxx::connection conn(postgres().connectionString());

    int64_t orderId;
    //creating new order
    {
        pqxx::work txn(conn);
        db::Order order(ORDER_YEAR, ORDER_TYPE);
        db::OrderGateway(txn).insert(order);
        txn.commit();
        orderId = order.id();
    }

    {
        sproto_helpers::sfactory::NewOrder sprotoNewOrder;
        sprotoNewOrder.year() = 2020;
        sprotoNewOrder.type() = sproto_helpers::convertToSproto(db::OrderType::Archive);
        sprotoNewOrder.name() = "newName";

        http::MockRequest rq(http::PATCH,
            http::URL("http://localhost/orders/update")
                .addParam("orderId", std::to_string(orderId))
        );
        setAuthHeaderFor(db::Role::Customer, rq);
        rq.body = boost::lexical_cast<std::string>(sprotoNewOrder);
        auto resp = yacare::performTestRequest(rq);

        ASSERT_EQ(resp.status, 200);
        auto sprotoOrder = boost::lexical_cast<sproto_helpers::sfactory::Order>(resp.body);
        ASSERT_EQ(sprotoOrder.year(), 2020u);
        ASSERT_EQ(*sprotoOrder.type(), sproto_helpers::convertToSproto(db::OrderType::Archive));
        ASSERT_EQ(*sprotoOrder.status(), sproto_helpers::sfactory::Order::Status::DRAFT);
        ASSERT_FALSE(sprotoOrder.acceptedAt());
        ASSERT_FALSE(sprotoOrder.rejectionReason());
        ASSERT_TRUE(sprotoOrder.name().defined());
        ASSERT_EQ(sprotoOrder.name().get(), "newName");

        setAuthHeaderFor(db::Role::Supplier, rq);
        rq.body = boost::lexical_cast<std::string>(sprotoNewOrder);
        resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 403);
    }

    {
        pqxx::work txn(conn);
        auto order = db::OrderGateway(txn).loadById(orderId);
        order.setStatusToReady();
        db::OrderGateway(txn).update(order);
        txn.commit();
    }
    // check that update is forbidden
    {
        sproto_helpers::sfactory::NewOrder sprotoNewOrder;
        sprotoNewOrder.year() = 2020;
        sprotoNewOrder.type() = sproto_helpers::convertToSproto(db::OrderType::Archive);
        sprotoNewOrder.name() = "newName";

        http::MockRequest rq(http::PATCH,
            http::URL("http://localhost/orders/update")
                .addParam("orderId", std::to_string(orderId))
        );
        setAuthHeaderFor(db::Role::Customer, rq);
        rq.body = boost::lexical_cast<std::string>(sprotoNewOrder);
        auto resp = yacare::performTestRequest(rq);

        ASSERT_EQ(resp.status, 403);
    }
}

Y_UNIT_TEST(name_year_uniqueness_check)
{
    std::string orderId1;
    std::string orderId2;

    //creating new order
    {
        sproto_helpers::sfactory::NewOrder sprotoNewOrder;
        sprotoNewOrder.year() = ORDER_YEAR;
        sprotoNewOrder.type() = SPROTO_ORDER_TYPE;
        sprotoNewOrder.name() = ORDER_NAME;

        http::MockRequest rq(http::POST, http::URL("http://localhost/orders/add"));
        setAuthHeaderFor(db::Role::Customer, rq);
        rq.body = boost::lexical_cast<std::string>(sprotoNewOrder);
        auto resp = yacare::performTestRequest(rq);

        ASSERT_EQ(resp.status, 200);
        auto sprotoOrder = boost::lexical_cast<sproto_helpers::sfactory::Order>(resp.body);
        orderId1 = sprotoOrder.id();
    }

    //creating another order
    {
        sproto_helpers::sfactory::NewOrder sprotoNewOrder;
        sprotoNewOrder.year() = ORDER_YEAR + 1;
        sprotoNewOrder.type() = SPROTO_ORDER_TYPE;
        sprotoNewOrder.name() = ORDER_NAME;

        http::MockRequest rq(http::POST, http::URL("http://localhost/orders/add"));
        setAuthHeaderFor(db::Role::Customer, rq);
        rq.body = boost::lexical_cast<std::string>(sprotoNewOrder);
        auto resp = yacare::performTestRequest(rq);

        ASSERT_EQ(resp.status, 200);
        auto sprotoOrder = boost::lexical_cast<sproto_helpers::sfactory::Order>(resp.body);
        orderId2 = sprotoOrder.id();
    }

    // try to create duplicate order
    {
        sproto_helpers::sfactory::NewOrder sprotoNewOrder;
        sprotoNewOrder.year() = ORDER_YEAR;
        sprotoNewOrder.type() = SPROTO_ORDER_TYPE;
        sprotoNewOrder.name() = ORDER_NAME;

        http::MockRequest rq(http::POST, http::URL("http://localhost/orders/add"));
        setAuthHeaderFor(db::Role::Customer, rq);
        rq.body = boost::lexical_cast<std::string>(sprotoNewOrder);
        auto resp = yacare::performTestRequest(rq);

        ASSERT_EQ(resp.status, 422);
        auto invalidOrderDatum = boost::lexical_cast<sproto_helpers::sfactory::InvalidOrderDatum>(resp.body);
        ASSERT_TRUE(invalidOrderDatum.invalidName().defined());
        EXPECT_EQ(invalidOrderDatum.invalidName().get().reason().get(),
            sproto_helpers::sfactory::InvalidString::DUPLICATE);
        EXPECT_EQ(invalidOrderDatum.invalidName().get().value().text(), ORDER_NAME);
    }

    // try to update order
    {
        sproto_helpers::sfactory::NewOrder sprotoNewOrder;
        sprotoNewOrder.year() = ORDER_YEAR;
        sprotoNewOrder.type() = sproto_helpers::convertToSproto(db::OrderType::Archive);
        sprotoNewOrder.name() = ORDER_NAME;

        http::MockRequest rq(http::PATCH,
            http::URL("http://localhost/orders/update")
                .addParam("orderId", orderId2)
        );
        setAuthHeaderFor(db::Role::Customer, rq);
        rq.body = boost::lexical_cast<std::string>(sprotoNewOrder);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 422);
        auto invalidOrderDatum = boost::lexical_cast<sproto_helpers::sfactory::InvalidOrderDatum>(resp.body);
        ASSERT_TRUE(invalidOrderDatum.invalidName().defined());
        EXPECT_EQ(invalidOrderDatum.invalidName().get().reason().get(),
            sproto_helpers::sfactory::InvalidString::DUPLICATE);
        EXPECT_EQ(invalidOrderDatum.invalidName().get().value().text(), ORDER_NAME);
    }

    // try to create order without name
    {
        sproto_helpers::sfactory::NewOrder sprotoNewOrder;
        sprotoNewOrder.year() = ORDER_YEAR;
        sprotoNewOrder.type() = sproto_helpers::convertToSproto(db::OrderType::Archive);

        http::MockRequest rq(http::POST,
            http::URL("http://localhost/orders/add"));
        setAuthHeaderFor(db::Role::Customer, rq);
        rq.body = boost::lexical_cast<std::string>(sprotoNewOrder);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 422);
        auto invalidOrderDatum = boost::lexical_cast<sproto_helpers::sfactory::InvalidOrderDatum>(resp.body);
        EXPECT_EQ(invalidOrderDatum.invalidName().get().reason().get(),
            sproto_helpers::sfactory::InvalidString::EMPTY);
    }

    // try to create order with defined by empty name
    {
        sproto_helpers::sfactory::NewOrder sprotoNewOrder;
        sprotoNewOrder.year() = ORDER_YEAR;
        sprotoNewOrder.type() = sproto_helpers::convertToSproto(db::OrderType::Archive);
        sprotoNewOrder.name() = "";

        http::MockRequest rq(http::POST,
            http::URL("http://localhost/orders/add"));
        setAuthHeaderFor(db::Role::Customer, rq);
        rq.body = boost::lexical_cast<std::string>(sprotoNewOrder);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 422);
        auto invalidOrderDatum = boost::lexical_cast<sproto_helpers::sfactory::InvalidOrderDatum>(resp.body);
        EXPECT_EQ(invalidOrderDatum.invalidName().get().reason().get(),
            sproto_helpers::sfactory::InvalidString::EMPTY);
    }
}

Y_UNIT_TEST(test_order_search_by_filter)
{
    unittest::Fixture fixture;

    pqxx::connection conn(postgres().connectionString());
    {
        pqxx::work txn(conn);
        makeOrder(txn);
        makeNewDigitalGlobeMosaicSource(txn);
        txn.commit();
    }

    {
        auto sprotoOrders = searchOrders();
        ASSERT_EQ(sprotoOrders.orders().size(), 2u);
    }

    {
        // test search by name
        const http::URL ordersSearchUrl("http://localhost/orders/search?filter=status:new");
        http::MockRequest rq(http::GET, ordersSearchUrl);
        setAuthHeaderFor(db::Role::Viewer, rq);
        auto resp = yacare::performTestRequest(rq);
        ASSERT_EQ(resp.status, 200);
        auto sprotoOrders = boost::lexical_cast<sproto_helpers::sfactory::Orders>(resp.body);
        ASSERT_EQ(sprotoOrders.orders().size(), 1u);
    }

}

} //Y_UNIT_TEST_SUITE

} //namespace maps::factory::sputnica::tests
