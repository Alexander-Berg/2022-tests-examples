#include <maps/infra/ratelimiter2/common/include/test_helpers.h>
#include <maps/infra/ratelimiter2/core/include/service_api.h>

#include <maps/libs/json-config/include/config.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <google/protobuf/util/message_differencer.h>
#include <google/protobuf/util/json_util.h>


#define EXPECT_PROTO_EQ(expected, actual) { \
    google::protobuf::util::MessageDifferencer comparer; \
    TString diff; \
    comparer.ReportDifferencesToString(&diff); \
    if (!comparer.Compare(expected, actual)) \
        ADD_FAILURE() << "Messages different:\n" << diff; \
}

namespace maps::rate_limiter2::tests {

namespace {

template<typename ProtoMessage>
ProtoMessage jsonToProto(std::string_view jsonText)
{
    ProtoMessage protoMessage;
    google::protobuf::util::JsonStringToMessage(TString(jsonText), &protoMessage);
    return protoMessage;
}

} // anonymous namespace


Y_UNIT_TEST_SUITE(service_api_test) {

Y_UNIT_TEST(limits_on_start)
{
    // Force empty 'peers' param, since its mandatory in server config
    json::config::initConfig({}, R"({"peers": ""})");

    auto protoLimitsX = jsonToProto<proto::rate_limiter2::Limits>(R"({
        "version": 153,
        "resources": [{
            "resource": "resourceA",
            "client_id": "client1",
            "rps": 100, "burst": 500, "unit": 60
        }]
    })");
    auto mockLimitsUpdater = [&](const auto&){
        return ProtoLimitsCache{{"projectX", protoLimitsX}};
    };

    auto api = ServiceApi::setupAsServer(mockLimitsUpdater, {});

    // Check limits version in the cache
    {
        auto limitsResponseProto = api->handleLimitsRequest("projectX");
        EXPECT_PROTO_EQ(limitsResponseProto, protoLimitsX);

        limitsResponseProto = api->handleLimitsRequest("NoSuchProject");
        EXPECT_PROTO_EQ(limitsResponseProto, proto::rate_limiter2::Limits{});
    }
    // Check limits version in the Core
    {
        // Response is 200 when limits_version in the request is the same
        auto [status, protoResponse] = api->handleChildSyncRequest(
            "A", jsonToProto<proto::rate_limiter2::Counters>(R"({"limits_version": 153})"));
        EXPECT_EQ(status, http::Status::OK);
        EXPECT_PROTO_EQ(
            protoResponse,
            jsonToProto<proto::rate_limiter2::Counters>(R"({
                "limits_version": 153,
                "lamport": 0
            })")
        );
    }
    {  // 409(Confilct) when limits_version in the request is the different
        auto [status, protoResponse] = api->handleChildSyncRequest(
            "A", jsonToProto<proto::rate_limiter2::Counters>(R"({"limits_version": 1})"));
        EXPECT_EQ(status, http::Status::Conflict);
        EXPECT_PROTO_EQ(
            protoResponse,  // current server version in the response
            jsonToProto<proto::rate_limiter2::Counters>(R"({"limits_version": 153})")
        );
    }
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::rate_limiter2::tests
