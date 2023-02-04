#pragma once

#include <google/protobuf/message.h>
#include <maps/infra/yacare/include/test_utils.h>
#include <maps/libs/json/include/value.h>

#include <ostream>
#include <string>

#define PROTO_EQ(lhs, rhs) { \
    auto [equals, msg] = maps::automotive::compareProtobuf(lhs, rhs); \
    if (!equals) { ADD_FAILURE() << "Objects are different\n" << msg; } \
}

namespace maps::automotive {

std::pair<bool, std::string> compareProtobuf(
    const google::protobuf::Message& expected, const google::protobuf::Message& actual);

inline http::MockResponse mockRequest(
    http::Method method,
    const std::string& request,
    const std::string& body,
    const http::HeaderMap& headers)
{
    http::MockRequest req(method, http::URL("http://localhost" + request));
    req.body = body;
    req.headers = headers;
    return yacare::performTestRequest(req);
}

inline http::MockResponse mockGet(
    const std::string& request,
    const http::HeaderMap& headers = {})
{
    return mockRequest(http::GET, request, "", headers);
}

inline http::MockResponse mockPut(
    const std::string& request,
    const std::string& body = "",
    const http::HeaderMap& headers = {})
{
    return mockRequest(http::PUT, request, body, headers);
}

inline http::MockResponse mockPost(
    const std::string& request,
    const std::string& body = "",
    const http::HeaderMap& headers = {})
{
    return mockRequest(http::POST, request, body, headers);
}

inline http::MockResponse mockPatch(
    const std::string& request,
    const std::string& body = "",
    const http::HeaderMap& headers = {})
{
    return mockRequest(http::PATCH, request, body, headers);
}

inline http::MockResponse mockDelete(
    const std::string& request,
    const std::string& body = "",
    const http::HeaderMap& headers = {})
{
    return mockRequest(http::DELETE, request, body, headers);
}

} // namespace maps::automotive

namespace maps::json {

inline void PrintTo(const maps::json::Value& v, std::ostream * s)
{
    *s << v;
}

} // namespace maps::json
