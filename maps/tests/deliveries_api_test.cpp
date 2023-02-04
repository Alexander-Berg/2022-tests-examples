#include <maps/factory/services/backend/tests/test_utils.h>

#include <maps/factory/libs/db/delivery_gateway.h>
#include <maps/factory/libs/sproto_helpers/delivery.h>
#include <maps/factory/libs/sproto_helpers/etag_utils.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::factory::backend::tests {

namespace {

using namespace sproto_helpers;

const std::string URL_CREATE = "http://localhost/v1/deliveries/create";
const std::string URL_UPDATE = "http://localhost/v1/deliveries/update";
const std::string URL_GET = "http://localhost/v1/deliveries/get";
const std::string URL_BULK_GET = "http://localhost/v1/deliveries/bulk_get";
const std::string URL_DELETE = "http://localhost/v1/deliveries/delete";

class DeliveryFixture : public BackendFixture {
public:
    DeliveryFixture()
        : testDelivery_(createTestDelivery())
        , deliveryEtag_(calculateEtag(testDelivery_))
    {
        auto txn = txnHandle();
        db::DeliveryGateway(*txn).insert(testDelivery_);
        txn->commit();
    }

    const db::Delivery& testDelivery() { return testDelivery_; }

    const std::string& deliveryEtag() { return deliveryEtag_; }

private:
    db::Delivery testDelivery_;
    std::string deliveryEtag_;
};

} // namespace

TEST_F(BackendFixture, test_deliveries_api_create)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(http::POST, http::URL(URL_CREATE));
    request.body = boost::lexical_cast<std::string>(
        convertToSproto<sdelivery::DeliveryData>(createTestDelivery()));
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto respDelivery = boost::lexical_cast<sdelivery::Delivery>(response.body);
    ASSERT_NE(respDelivery.id(), "0");

    auto dbDelivery = createTestDelivery();
    ASSERT_NO_THROW(
        dbDelivery = db::DeliveryGateway(*txnHandle())
            .loadById(db::parseId(respDelivery.id()));
    );

    EXPECT_EQ(respDelivery.name(), DELIVERY_NAME);
    EXPECT_EQ(respDelivery.year(), YEAR);
    EXPECT_EQ(respDelivery.copyrights().at(0), COPYRIGHTS.at(0));
    EXPECT_EQ(*respDelivery.downloadUrl(), DOWNLOAD_URL);
    EXPECT_EQ(*respDelivery.downloadEnabled(), ENABLED);

    EXPECT_EQ(dbDelivery.year(), YEAR);
    EXPECT_EQ(*dbDelivery.downloadUrl(), DOWNLOAD_URL);
    EXPECT_EQ(dbDelivery.name(), DELIVERY_NAME);
    EXPECT_EQ(dbDelivery.enabled(), ENABLED);
    EXPECT_EQ(dbDelivery.copyrights().at(0), COPYRIGHTS.at(0));
}

TEST_F(DeliveryFixture, test_deliveries_api_update)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto uDelivery = testDelivery();
    updateTestDelivery(uDelivery);
    auto sprotoUDelivery = convertToSproto(uDelivery);
    sprotoUDelivery.etag() = deliveryEtag();

    http::MockRequest request(http::POST, http::URL(URL_UPDATE));
    request.body = boost::lexical_cast<std::string>(sprotoUDelivery);
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto respDelivery = boost::lexical_cast<sdelivery::Delivery>(response.body);

    auto dbDelivery = createTestDelivery();
    ASSERT_NO_THROW(
        dbDelivery = db::DeliveryGateway(*txnHandle())
            .loadById(db::parseId(respDelivery.id()));
    );

    EXPECT_EQ(respDelivery.name(), DELIVERY_NAME_U);
    EXPECT_EQ(respDelivery.year(), YEAR_U);
    EXPECT_EQ(respDelivery.copyrights().at(0), COPYRIGHTS_U.at(0));
    EXPECT_EQ(*respDelivery.downloadUrl(), DOWNLOAD_URL_U);
    EXPECT_EQ(*respDelivery.downloadEnabled(), ENABLED_U);

    EXPECT_EQ(dbDelivery.year(), YEAR_U);
    EXPECT_EQ(*dbDelivery.downloadUrl(), DOWNLOAD_URL_U);
    EXPECT_EQ(dbDelivery.name(), DELIVERY_NAME_U);
    EXPECT_EQ(dbDelivery.enabled(), ENABLED_U);
    EXPECT_EQ(dbDelivery.copyrights().at(0), COPYRIGHTS_U.at(0));
}

TEST_F(DeliveryFixture, test_deliveries_api_get)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::GET,
        http::URL(URL_GET)
            .addParam("id", testDelivery().id())
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto respDelivery = boost::lexical_cast<sdelivery::Delivery>(response.body);

    EXPECT_EQ(respDelivery.name(), DELIVERY_NAME);
    EXPECT_EQ(respDelivery.year(), YEAR);
    EXPECT_EQ(respDelivery.copyrights().at(0), COPYRIGHTS.at(0));
    EXPECT_EQ(*respDelivery.downloadUrl(), DOWNLOAD_URL);
    EXPECT_EQ(*respDelivery.downloadEnabled(), ENABLED);
}

TEST_F(DeliveryFixture, test_deliveries_api_bulk_get)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::GET,
        http::URL(URL_BULK_GET)
            .addParam("ids", testDelivery().id())
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    auto respDelivery = boost::lexical_cast<sdelivery::Deliveries>(response.body)
        .deliveries().at(0);

    EXPECT_EQ(respDelivery.name(), DELIVERY_NAME);
    EXPECT_EQ(respDelivery.year(), YEAR);
    EXPECT_EQ(respDelivery.copyrights().at(0), COPYRIGHTS.at(0));
    EXPECT_EQ(*respDelivery.downloadUrl(), DOWNLOAD_URL);
    EXPECT_EQ(*respDelivery.downloadEnabled(), ENABLED);
}

TEST_F(DeliveryFixture, test_deliveries_api_bulk_get_special_cases)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    {
        http::MockRequest request(
            http::GET,
            http::URL(URL_BULK_GET)
                .addParam("ids", "a,b,c")
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 400);
    }
    {
        http::MockRequest request(
            http::GET,
            http::URL(URL_BULK_GET)
                .addParam("ids", std::to_string(testDelivery().id()) + ",1111")
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 200);
    }
}

TEST_F(DeliveryFixture, test_deliveries_api_delete)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    http::MockRequest request(
        http::DELETE,
        http::URL(URL_DELETE)
            .addParam("id", testDelivery().id())
    );
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 200);

    EXPECT_THROW(
        db::DeliveryGateway(*txnHandle()).loadById(testDelivery().id()),
        maps::sql_chemistry::ObjectNotFound
    );
}

TEST_F(DeliveryFixture, test_deliveries_api_not_found)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    {
        http::MockRequest request(http::POST, http::URL(URL_UPDATE));
        auto uDelivery = convertToSproto(testDelivery());
        uDelivery.id() = "1111";
        request.body = boost::lexical_cast<std::string>(uDelivery);
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 404);
    }
    {
        http::MockRequest request(
            http::GET,
            http::URL(URL_GET)
                .addParam("id", "1111")
        );
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 404);
    }
}

TEST_F(DeliveryFixture, test_deliveries_api_etag_error)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto delivery = testDelivery();
    const auto sprotoDelivery = convertToSproto(delivery);
    updateTestDelivery(delivery);
    auto txn = txnHandle();
    db::DeliveryGateway(*txn).update(delivery);
    txn->commit();

    http::MockRequest request(http::POST, http::URL(URL_UPDATE));
    request.body = boost::lexical_cast<std::string>(sprotoDelivery);
    auto response = yacare::performTestRequest(request);
    ASSERT_EQ(response.status, 409);
}

/*
TEST_F(DeliveryFixture, test_deliveries_api_no_authorization)
{
    const auto login = "john";
    auth::UserInfo userInfo{};
    userInfo.setLogin(login);
    yacare::tests::UserInfoFixture userInfoFixture(userInfo);

    http::MockRequest request(http::POST, http::URL(URL_CREATE));
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(http::POST, http::URL(URL_UPDATE));
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::GET,
        http::URL(URL_GET).addParam("id", "1")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::GET,
        http::URL(URL_BULK_GET).addParam("ids", "1")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::DELETE,
        http::URL(URL_DELETE).addParam("id", "1")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);
}

TEST_F(DeliveryFixture, test_deliveries_api_no_edit_access)
{
    std::string login = "john";
    auto txn = txnHandle();
    idm::IdmService(*txn, login)
        .addRole(idm::parseSlugPath("project/mapsfactory/role/viewer"));
    txn->commit();

    auth::UserInfo userInfo{};
    userInfo.setLogin(login);
    yacare::tests::UserInfoFixture userInfoFixture(userInfo);

    http::MockRequest request(http::POST, http::URL(URL_CREATE));
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(http::POST, http::URL(URL_UPDATE));
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::DELETE,
        http::URL(URL_DELETE).addParam("id", "1")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);
}
*/

} // namespace maps::factory::backend::tests
