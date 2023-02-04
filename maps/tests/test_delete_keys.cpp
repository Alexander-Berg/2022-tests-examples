#include <library/cpp/testing/gtest/gtest.h>
#include <maps/automotive/libs/test_helpers/helpers.h>
#include <maps/automotive/store_internal/lib/dao/idm.h>
#include <maps/automotive/store_internal/tests/helpers.h>
#include <maps/automotive/store_internal/tests/postgres.h>

namespace maps::automotive::store_internal {

using KeysApi = AppContextPostgresFixture;

TEST_F(KeysApi, redirects)
{
    yacare::tests::UserInfoFixture userInfoFixture(makeUserInfo("jeremy"));
    auto rsp = mockPost("/internal/delete_keys?device_id=aabbccddeeff&owner=drive_virtual");
    ASSERT_EQ(308, rsp.status);
    EXPECT_EQ("/headunit/1.x/key?device_id=aabbccddeeff&owner=drive_virtual", rsp.headers["Location"]);
}

TEST_F(KeysApi, forbidden)
{
    // no user info
    EXPECT_EQ(401, mockPost("/headunit/1.x/key?device_id=aabbccddeeff&owner=drive_virtual").status);

    { // unauthorized user
        yacare::tests::UserInfoFixture fixture(makeUserInfo("unknown_login"));
        EXPECT_EQ(401, mockPost("/headunit/1.x/key?device_id=aabbccddeeff&owner=stable").status);
    }

    // inappropriate roles
    for (const std::string& user: {"manager-prod", "manager", "viewer-victor"}) {
        yacare::tests::UserInfoFixture fixture{makeUserInfo(user)};
        EXPECT_EQ(401, mockPost("/headunit/1.x/key?device_id=aabbccddeeff&owner=stable").status);
    }
}

TEST_F(KeysApi, deleteKey)
{
    auto passportMock = http::addMock(
        "http://passport-mock/1/bundle/device_public_key/delete/",
        [](const http::MockRequest& req) {
            EXPECT_EQ("consumer=mapsauto", req.url.params());
            EXPECT_EQ("device_id=aabbccddeeff&owner=stable", req.body);
            return std::string(R"({"status": "ok"})");
        });
    {
        auto txn = dao::makeWriteableTransaction();
        IdmDao(*txn).addIdmUserRole("robot-meklar", role::DEVICE_KEY_MANAGER_PRODUCTION);
        txn->commit();
    }
    yacare::tests::UserInfoFixture userInfoFixture(makeUserInfo("robot-meklar"));
    EXPECT_EQ(200, mockPost("/headunit/1.x/key?device_id=aabbccddeeff&owner=stable").status);
}

}
