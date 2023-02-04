#include "helpers.h"

#include <maps/libs/http/include/urlencode.h>
#include <maps/libs/json/include/prettify.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::automotive::tests {

void formRequest(yacare::RequestBuilder& builder, const std::optional<std::string>& params)
{
    builder.putenv("REQUEST_METHOD", "GET");
    builder.putenv("PATH_INFO", "/some/path");
    builder.putenv("QUERY_STRING", params ? params.value() : "");
    builder.putenv("REMOTE_ADDR", "127.0.0.1");

    // force builder to read params from 'QUERY_STRING'
    builder.readBody([&](char*, size_t) { return false; } );
}

void formRequest(
    yacare::RequestBuilder& builder,
    const std::string& login,
    const std::string& groupSlug,
    const std::string& roleId)
{
    const auto params = boost::format("login=%s&role=%s")
        % login
        % maps::http::urlEncode(json::prettifyJson(json::Builder() << [&](json::ObjectBuilder b){
            b[groupSlug] = roleId;
        }));
    formRequest(builder, params.str());
}

void checkResponse(
    yacare::Response& response,
    int code,
    const std::string& kind,
    const std::optional<std::string>& msg)
{
    EXPECT_TRUE(true);
    UNIT_ASSERT_VALUES_EQUAL(response.status() == http::Status(http::Status::OK), true);

    const auto jsonResponse = maps::json::Value::fromString(response.bodyString());
    UNIT_ASSERT_VALUES_EQUAL(jsonResponse["code"].as<int>(), code);
    if (msg) {
        UNIT_ASSERT_VALUES_EQUAL(jsonResponse[kind].toString(), msg.value());
    } else {
        UNIT_ASSERT_VALUES_EQUAL(jsonResponse[kind].toString().empty(), false);
    }
}

} // namespace maps::automotive::tests
