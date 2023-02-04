#include <library/cpp/testing/gtest/gtest.h>
#include <maps/automotive/libs/test_helpers/helpers.h>
#include <maps/automotive/store_internal/lib/serialization.h>
#include <maps/automotive/store_internal/proto/store_internal.pb.h>
#include <maps/automotive/store_internal/tests/helpers.h>
#include <maps/automotive/store_internal/tests/postgres.h>

namespace maps::automotive::store_internal {

static const std::string API_URL = "/store/1.x/headunit";

struct HeadunitApi: public AppContextPostgresFixture
{
    HeadunitApi()
        : adminArnold(makeUserInfo("admin-arnold"))
    {}

    yacare::tests::UserInfoFixture adminArnold;
};

TEST_F(HeadunitApi, addRemove)
{
    HeadUnits added;
    {
        *added.add_type() = "type-1";
        *added.add_mcu() = "mcu-1";
        *added.add_mcu() = "mcu-2";
        auto* model = added.add_model();
        model->set_vendor("vendor-1");
        model->set_model("model-1");
    }
    ASSERT_EQ(204, mockDelete(API_URL, printToString(defaultHeadUnits())).status);
    { // check there are no units
        auto response = mockGet(API_URL);
        ASSERT_EQ(200, response.status);
        HeadUnits selected;
        parseFromString(response.body, selected);
        HeadUnits empty;
        EXPECT_EQ(printToString(empty), printToString(selected));
    }
    ASSERT_EQ(204, mockPut(API_URL, printToString(added)).status);
    // PUT is idempotent operation
    ASSERT_EQ(204, mockPut(API_URL, printToString(added)).status);
    {
        auto response = mockGet(API_URL);
        ASSERT_EQ(200, response.status);
        HeadUnits selected;
        parseFromString(response.body, selected);
        EXPECT_EQ(printToString(added), printToString(selected));
    }
    ASSERT_EQ(204, mockDelete(API_URL, printToString(added)).status);
    { // check there are no units
        auto response = mockGet(API_URL);
        ASSERT_EQ(200, response.status);
        HeadUnits selected;
        parseFromString(response.body, selected);
        HeadUnits empty;
        EXPECT_EQ(printToString(empty), printToString(selected));
    }
    // DELETE is idempotent operation
    ASSERT_EQ(204, mockDelete(API_URL, printToString(added)).status);
    { // check there are no units
        auto response = mockGet(API_URL);
        ASSERT_EQ(200, response.status);
        HeadUnits selected;
        parseFromString(response.body, selected);
        HeadUnits empty;
        EXPECT_EQ(printToString(empty), printToString(selected));
    }
}

TEST_F(HeadunitApi, validate)
{
    {
        HeadUnits hu;
        *hu.add_type() = "type-1_.";
        *hu.add_mcu() = "mcu-1._";
        auto* model = hu.add_model();
        model->set_vendor("vendor_-1.");
        model->set_model("model-_1.");
        ASSERT_EQ(204, mockPut(API_URL, printToString(hu)).status);
    }
    {
        HeadUnits hu;
        *hu.add_type() = "";
        ASSERT_EQ(400, mockPut(API_URL, printToString(hu)).status);
    }
    {
        HeadUnits hu;
        *hu.add_mcu() = ".";
        ASSERT_EQ(400, mockPut(API_URL, printToString(hu)).status);
    }
    {
        HeadUnits hu;
        auto* model = hu.add_model();
        model->set_vendor("vendor*");
        model->set_model("model");
        ASSERT_EQ(400, mockPut(API_URL, printToString(hu)).status);
    }
    {
        HeadUnits hu;
        auto* model = hu.add_model();
        model->set_vendor("vendor");
        model->set_model("*model");
        ASSERT_EQ(400, mockPut(API_URL, printToString(hu)).status);
    }
}

TEST_F(HeadunitApi, forbidden)
{
    HeadUnits hu;
    *hu.add_type() = "typ42";
    *hu.add_mcu() = "mcu43";
    auto* model = hu.add_model();
    model->set_vendor("vendor44");
    model->set_model("mod45");
    for (const std::string& user: {"manager", "key-manager-prod", "key-manager", "viewer-victor"}) {
        yacare::tests::UserInfoFixture fixture{makeUserInfo(user)};
        ASSERT_EQ(401, mockPut(API_URL, printToString(hu)).status);
        ASSERT_EQ(401, mockDelete(API_URL, printToString(hu)).status);
    }
    yacare::tests::UserInfoFixture fixture{makeUserInfo("manager-prod")};
    ASSERT_EQ(401, mockDelete(API_URL, printToString(hu)).status);
}

}
