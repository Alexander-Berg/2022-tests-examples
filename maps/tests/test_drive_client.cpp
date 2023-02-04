#include <library/cpp/testing/unittest/env.h>
#include <maps/automotive/updater/lib/drive/client_impl.h>
#include <maps/automotive/updater/lib/drive/session.h>
#include <maps/automotive/updater/test_helpers/mocks.h>
#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/common/include/file_utils.h>

namespace maps::automotive::updater {

using namespace maps::http;

using DriveClientTest = Fixture;

TEST_F(DriveClientTest, TagsSessionMalformed)
{
    auto handleTags = addMock(
        config()->drive().url() + "/api/yaauto/car/tag/list",
        [] (const auto& req) {
            EXPECT_EQ(req.url.params(), "head_id=id");

            MockResponse resp;
            EXPECT_NO_THROW(resp = MockResponse::fromFile(
                SRC_("data/DriveClient__TagsSessionMalformed.json")));
            resp.headers.emplace("content", "application/json");
            return resp;
        });

    drive::ClientImpl client(*config());
    EXPECT_EQ(
        client.getSessionTypeFor("id"),
        drive::SessionType::Unknown);
}

TEST_F(DriveClientTest, TagsSessionUserEmpty)
{
    auto handleTags = addMock(
        config()->drive().url() + "/api/yaauto/car/tag/list",
        [] (const auto& req) {
            EXPECT_EQ(req.url.params(), "head_id=id");

            MockResponse resp;
            EXPECT_NO_THROW(resp = MockResponse::fromFile(
                SRC_("data/DriveClient__TagsSessionUserEmpty.json")));
            resp.headers.emplace("content", "application/json");
            return resp;
        });

    drive::ClientImpl client(*config());
    EXPECT_EQ(
        client.getSessionTypeFor("id"),
        drive::SessionType::User);
}

TEST_F(DriveClientTest, TagsSessionUserNoPerformer)
{
    auto handleTags = addMock(
        config()->drive().url() + "/api/yaauto/car/tag/list",
        [] (const auto& req) {
            EXPECT_EQ(req.url.params(), "head_id=id");

            MockResponse resp;
            EXPECT_NO_THROW(resp = MockResponse::fromFile(
                SRC_("data/DriveClient__TagsSessionUserNoPerformer.json")));
            resp.headers.emplace("content", "application/json");
            return resp;
        });

    drive::ClientImpl client(*config());
    EXPECT_EQ(
        client.getSessionTypeFor("id"),
        drive::SessionType::User);
}

TEST_F(DriveClientTest, TagsSessionService)
{
    auto handleTags = addMock(
        config()->drive().url() + "/api/yaauto/car/tag/list",
        [] (const auto& req) {
            EXPECT_EQ(req.url.params(), "head_id=id");

            MockResponse resp;
            EXPECT_NO_THROW(resp = MockResponse::fromFile(
                SRC_("data/DriveClient__TagsSessionService.json")));
            resp.headers.emplace("content", "application/json");
            return resp;
        });

    drive::ClientImpl client(*config());
    EXPECT_EQ(
        client.getSessionTypeFor("id"),
        drive::SessionType::Service);
}

TEST_F(DriveClientTest, TagsSessionUnregistered)
{
    auto handleTags = addMock(
        config()->drive().url() + "/api/yaauto/car/tag/list",
        [] (const auto& req) {
            EXPECT_EQ(req.url.params(), "head_id=id");

            return MockResponse::withStatus(401);
        });

    drive::ClientImpl client(*config());
    EXPECT_EQ(
        client.getSessionTypeFor("id"),
        drive::SessionType::Unregistered);
}

TEST_F(DriveClientTest, TagsSessionUnknown)
{
    auto handleTags = addMock(
        config()->drive().url() + "/api/yaauto/car/tag/list",
        [] (const auto& req) {
            EXPECT_EQ(req.url.params(), "head_id=id");

            return MockResponse::withStatus(500);
        });

    drive::ClientImpl client(*config());
    EXPECT_EQ(
        client.getSessionTypeFor("id"),
        drive::SessionType::Unknown);
}

TEST_F(DriveClientTest, scheduleZeroDuration)
{
    auto handleTags = addMock(
        config()->drive().url() + "/api/yaauto/car/tag/add",
        [] (const auto&) {
            EXPECT_TRUE(false) << "Should not be called";
            return MockResponse::withStatus(500);
        });

    drive::ClientImpl client(*config());
    client.scheduleServiceSession("head_id", TInstant::Now(), TDuration::Zero());
}

TEST_F(DriveClientTest, scheduleBadStatus)
{
    auto handleTags = addMock(
        config()->drive().url() + "/api/yaauto/car/tag/add",
        [] (const auto&) {
            return MockResponse::withStatus(500);
        });

    drive::ClientImpl client(*config());
    EXPECT_THROW(
        client.scheduleServiceSession("head_id", TInstant::Now(), TDuration::Seconds(1)),
        maps::Exception);
}

TEST_F(DriveClientTest, cancelServiceSession)
{
    auto handleTags = addMock(
        config()->drive().url() + "/api/yaauto/car/tag/remove",
        [] (const auto& req) {
            EXPECT_EQ("/api/yaauto/car/tag/remove", req.url.path());
            EXPECT_EQ("head_id", req.headers.at("DeviceId"));
            EXPECT_EQ(
                common::readFileToString(SRC_("data/DriveClient__TagRemove.json")),
                req.body);
            return MockResponse::withStatus(200);
        });

    drive::ClientImpl client(*config());
    client.cancelServiceSession("head_id");
}

} // namespace maps::automotive::updater
