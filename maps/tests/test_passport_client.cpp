#include <library/cpp/testing/unittest/env.h>
#include <maps/automotive/updater/lib/passport/client_impl.h>
#include <maps/automotive/updater/test_helpers/mocks.h>
#include <maps/libs/http/include/test_utils.h>

#include <optional>

namespace maps::automotive::updater {

using namespace maps::http;

TEST_F(Fixture, PassportCheckOk)
{
    auto handle = addMock(
        config()->passport().url() + "/blackbox",
        [] (const auto& req) {
            using namespace maps::automotive::store_internal;

            EXPECT_EQ(req.url.params(),
                "method=check_device_signature&"
                "format=json&"
                "nonce=nonce&"
                "nonce_sign_space=auto_head_unit&"
                "signature=signature&"
                "device_id=id");
            MockResponse resp;
            EXPECT_NO_THROW(resp = MockResponse::fromFile(
                SRC_("data/PassportCheckOk.json")));
            resp.headers.emplace("Content-Type", "application/json");
            return resp;
        });

    passport::ClientImpl client(*config());

    EXPECT_NO_THROW(client.assertSignatureValid("id", {"nonce", "signature"}));
}

TEST_F(Fixture, PassportCheckHttpNOk)
{
    auto handle = addMock(
        config()->passport().url() + "/blackbox",
        [] (const auto& req) {
            using namespace maps::automotive::store_internal;

            EXPECT_EQ(req.url.params(),
                "method=check_device_signature&"
                "format=json&"
                "nonce=nonce&"
                "nonce_sign_space=auto_head_unit&"
                "signature=signature&"
                "device_id=id");
            return MockResponse::withStatus(500);
        });

    passport::ClientImpl client(*config());

    EXPECT_THROW(
        client.assertSignatureValid("id", {"nonce", "signature"}),
        maps::Exception);
}

TEST_F(Fixture, PassportCheckJsonInvalidSignature)
{
    auto handle = addMock(
        config()->passport().url() + "/blackbox",
        [] (const auto& req) {
            using namespace maps::automotive::store_internal;

            EXPECT_EQ(req.url.params(),
                "method=check_device_signature&"
                "format=json&"
                "nonce=nonce&"
                "nonce_sign_space=auto_head_unit&"
                "signature=signature&"
                "device_id=id");
            MockResponse resp;
            EXPECT_NO_THROW(resp = MockResponse::fromFile(
                SRC_("data/PassportCheckJsonInvalidSignature.json")));
            resp.headers.emplace("Content-Type", "application/json");
            return resp;
        });

    passport::ClientImpl client(*config());

    EXPECT_THROW(
        client.assertSignatureValid("id", {"nonce", "signature"}),
        maps::Exception);
}

TEST_F(Fixture, PassportCheckJsonCorrupted)
{
    auto handle = addMock(
        config()->passport().url() + "/blackbox",
        [] (const auto& req) {
            using namespace maps::automotive::store_internal;

            EXPECT_EQ(req.url.params(),
                "method=check_device_signature&"
                "format=json&"
                "nonce=nonce&"
                "nonce_sign_space=auto_head_unit&"
                "signature=signature&"
                "device_id=id");
            MockResponse resp;
            EXPECT_NO_THROW(resp = MockResponse::fromFile(
                SRC_("data/PassportCheckJsonCorrupted.json")));
            resp.headers.emplace("Content-Type", "application/json");
            return resp;
        });

    passport::ClientImpl client(*config());

    EXPECT_THROW(
        client.assertSignatureValid("id", {"nonce", "signature"}),
        maps::Exception);
}

TEST_F(Fixture, PassportRetry)
{
    size_t requestNb = 0;
    auto handle = addMock(
        config()->passport().url() + "/blackbox",
        [&] (const auto&) {
            ++requestNb;
            MockResponse rsp;
            if (requestNb == 1) {
                rsp.status = 500;
            } else if (requestNb == 2) {
                rsp.status = 400;
            } else {
                rsp.body = R"({"status": "OK"})";
            }
            return rsp;
        });

    passport::ClientImpl client(*config());
    client.assertSignatureValid("id", {"nonce", "signature"});
}

} // namespace maps::automotive::updater
