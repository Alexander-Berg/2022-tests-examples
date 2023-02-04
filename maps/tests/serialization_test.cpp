#include <maps/infra/ratelimiter2/common/include/serialization.h>
#include <maps/infra/ratelimiter2/common/include/test_helpers.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <google/protobuf/util/message_differencer.h>
#include <google/protobuf/util/json_util.h>

#include <maps/libs/common/include/exception.h>

namespace maps::rate_limiter2::tests {

/// Package for serialization to counters proto message.
struct SyncData {
    Counters counters;
    int32_t limitsVersion;
    std::optional<int64_t> lamport;
};

namespace {

template<typename ProtoMessage>
inline ProtoMessage jsonToProto(std::string_view jsonText)
{
    ProtoMessage protoMessage;
    google::protobuf::util::JsonStringToMessage(TString(jsonText), &protoMessage);
    return protoMessage;
}

} // anonymous namespace

Y_UNIT_TEST_SUITE(serialization_test) {

Y_UNIT_TEST(resource_counters_to_proto_and_back)
{
    ResourceCounters counters{
        {uuid("e4a3cb5d-afc0-4acc-931a-88c3dce990ce"), 1},
        {uuid("f0b24df8-ceb8-46ea-8a33-3c216a9268f0"), 2}
    };

    maps::proto::rate_limiter2::ResourceCounters resource;
    write(counters, &resource);

    EXPECT_TRUE(parse<ResourceCounters>(resource) == counters);
}

Y_UNIT_TEST(counters_to_proto_and_back)
{
    Counters counters{ {
        "resource 1", {
            {uuid("e4a3cb5d-afc0-4acc-931a-88c3dce990ce"), 1},
            {uuid("f0b24df8-ceb8-46ea-8a33-3c216a9268f0"), 2}
        } }, {
        "resource 2", {
            {uuid("289fc1ed-a2b3-4ded-b4b8-9de79f4a3a17"), 42}
        } }, {
        "resource 3", {} }
    };

    maps::proto::rate_limiter2::Counters proto;
    write(counters, &proto);

    EXPECT_EQ(parse<Counters>(proto), counters);
}

Y_UNIT_TEST(sorted_counters_to_proto_and_back)
{
    SortedCounters counters{ {
        "resource 1", {
            {uuid("e4a3cb5d-afc0-4acc-931a-88c3dce990ce"), 1},
            {uuid("f0b24df8-ceb8-46ea-8a33-3c216a9268f0"), 2}
        } }, {
        "resource 2", {
            {uuid("289fc1ed-a2b3-4ded-b4b8-9de79f4a3a17"), 42}
        } }, {
        "resource 3", {} }
    };

    maps::proto::rate_limiter2::Counters resource;
    write(counters, &resource);

    EXPECT_EQ(parse<SortedCounters>(resource), counters);
}

Y_UNIT_TEST(empty_counters_message_deserialization)
{
    CountersMessage emptyMessage {};
    EXPECT_EQ(serialize(emptyMessage), "");
    auto parsedEmptyMessage = deserialize<CountersMessage>("");
    EXPECT_EQ(parsedEmptyMessage.counters, emptyMessage.counters);
    EXPECT_EQ(parsedEmptyMessage.limitsVersion, emptyMessage.limitsVersion);
}

Y_UNIT_TEST(counters_message_deserialization)
{
    CountersMessage countersMessage {
        .counters = {{"resource 1",
                      {{uuid("e4a3cb5d-afc0-4acc-931a-88c3dce990ce"), 1},
                       {uuid("f0b24df8-ceb8-46ea-8a33-3c216a9268f0"), 2}}},
                     {"resource 2", {{uuid("289fc1ed-a2b3-4ded-b4b8-9de79f4a3a17"), 42}}},
                     {"resource 3", {}}},
        .limitsVersion = 42};
    auto serializedMessage = serialize(countersMessage);
    auto parsedMessage = deserialize<CountersMessage>(serializedMessage);
    EXPECT_EQ(parsedMessage.counters, countersMessage.counters);
    EXPECT_EQ(parsedMessage.limitsVersion, countersMessage.limitsVersion);
}

Y_UNIT_TEST(counters_to_string_and_back)
{
    {   // test empty
        Counters original;
        std::string protoPacket = serialize(SyncData {.counters = original});
        EXPECT_EQ(protoPacket, "");

        auto remake = deserialize<Counters>(protoPacket);
        EXPECT_TRUE(remake == original);
    }
    {   // just resources
        Counters original{ {"resource1", {}}, {"resource2", {}} };
        std::string protoPacket = serialize(SyncData {.counters = original});

        auto remake = deserialize<Counters>(protoPacket);
        EXPECT_TRUE(remake == original);
    }
    // test invalid string
    EXPECT_THROW(
        (deserialize<Counters>("some bullshit")),
        maps::RuntimeError);

    {   // test several
        Counters original = {
            {"resource1", {
                { makeClientHash("client1"), 1}
            }},
            {"resource2", {
                { makeClientHash("client1"), 153},
                { makeClientHash("client2"), 2}
            }},
            {"resource3", {
                { makeClientHash("client1"), 11} ,
                { makeClientHash("client33"), 33},
                { makeClientHash("client153"), 37}
            }},
        };

        std::string protoPacket = serialize(SyncData {.counters = original});
        auto remake = deserialize<Counters>(protoPacket);
        EXPECT_TRUE(original == remake);
    }
}

Y_UNIT_TEST(override_errors)
{
    // first check original exception throwed
    UNIT_ASSERT_EXCEPTION_CONTAINS(
        deserialize<Counters>("bullshit"),
        maps::RuntimeError,
        "Failed to deserialize protobuf string."
    );

    // check overrides
    UNIT_ASSERT_EXCEPTION_CONTAINS(
        overrideExceptions<maps::LogicError>([]() {
            return deserialize<Counters>("bullshit");
        }),
        maps::LogicError,
        "Failed to deserialize protobuf string."
    );
    UNIT_ASSERT_EXCEPTION_CONTAINS(
        overrideExceptions<std::invalid_argument>([]() {
            return deserialize<Counters>("bullshit");
        }),
        std::invalid_argument,
        "Failed to deserialize protobuf string."
    );
}

// limits serialization tests

Y_UNIT_TEST(limits_registry_parse)
{
    maps::proto::rate_limiter2::Limits protoLimits;
    protoLimits.set_version(153);
    {   // client1 is super
        auto res = protoLimits.add_resources();
        res->set_client_id(TString("super.client.1"));
        res->set_rps(50000);
        res->set_burst(1000);
        res->set_unit(1);
    }
    {   // resource limit with no client_id and no resource_id - should be ignored
        auto res = protoLimits.add_resources();
        res->set_rps(333);
        res->set_burst(666);
        res->set_unit(15);
    }
    {   // client2 rph to resource1
        auto res = protoLimits.add_resources();
        res->set_resource(TString("resource.1"));
        res->set_client_id(TString("client.2"));
        res->set_rps(1000);
        res->set_burst(1000);
        res->set_unit(1*60*60);
    }
    {   // client2 rpd to resource2
        auto res = protoLimits.add_resources();
        res->set_resource(TString("resource.2"));
        res->set_client_id(TString("client.2"));
        res->set_rps(300);
        res->set_burst(300);
        res->set_unit(24*60*60);
    }
    {   // client3 rps to resource2
        auto res = protoLimits.add_resources();
        res->set_resource(TString("resource.2"));
        res->set_client_id(TString("client.3"));
        res->set_rps(500);
        res->set_burst(500);
        res->set_unit(1);
    }
    {   // 'anybody' rps to to resource3
        auto res = protoLimits.add_resources();
        res->set_resource(TString("resource.3"));
        res->set_rps(100);
        res->set_burst(500);
        res->set_unit(1);
    }

    auto registry = parse<LimitsRegistry>(protoLimits);
    LimitsRegistry::Storage expected = {
        { makeClientHash("super.client.1"), {{ "", LimitInfo({.rate = 50000, .unit = 1}, 1000)}} },
        { makeClientHash("client.2"), {
            {"resource.1", LimitInfo({.rate = 1000, .unit = 1*60*60}, 1000)},
            {"resource.2", LimitInfo({.rate = 300, .unit = 24*60*60}, 300)}
        }},
        { makeClientHash(""), {{"resource.3", LimitInfo({.rate = 100, .unit = 1}, 500)}}  },
        { makeClientHash("client.3"), {{"resource.2", LimitInfo({.rate = 500, .unit = 1}, 500)}} }
    };

    EXPECT_TRUE(registry.storage() == expected);
    EXPECT_EQ(registry.version(), 153);
}


Y_UNIT_TEST(limits_registry_deserialize)
{
    {   // test empty
        auto registry = deserialize<LimitsRegistry, proto::rate_limiter2::Limits>("");
        EXPECT_TRUE(registry.empty());
    }

    EXPECT_THROW(
        (deserialize<LimitsRegistry, proto::rate_limiter2::Limits>("some bullshit")),
        std::exception
    );

    maps::proto::rate_limiter2::Limits protoLimits;
    protoLimits.set_version(1);
    {
        auto res = protoLimits.add_resources();
        res->set_resource(TString("resource.1"));
        res->set_rps(10);
        res->set_burst(5);
        res->set_unit(1);
    }
    {   // any client to resource.forbidden (explicilty forbidden)
        auto res = protoLimits.add_resources();
        res->set_resource(TString("resource.forbidden"));
        res->set_rps(0);
        res->set_burst(0);
        res->set_unit(1);
    }
    TString payload;
    Y_ENSURE(protoLimits.SerializeToString(&payload));

    auto expected = LimitsRegistry({
        { makeClientHash(""), {
            {"resource.1", {{.rate = 10, .unit  = 1}, 5}} ,
            {"resource.forbidden", {Limit{}, 0}}
        } }
    }, 1);

    EXPECT_TRUE(expected.storage() ==
        (deserialize<LimitsRegistry, proto::rate_limiter2::Limits>(payload)).storage());
}


Y_UNIT_TEST(nova_message_to_proto_and_back)
{
    auto client1 = uuid("11111111-1111-1111-1111-111111111111");
    auto client2 = uuid("22222222-2222-2222-2222-222222222222");
    auto agentMessage = NovaAgentMessage {
        .counters = {
            {"resourceA", {{client1, 1}, {client2, 2}}},
            {"resourceB", {{client1, 42}}},
            {"resourceC", {}}
        },
        .limits = {
            {"resourceA", {
                {client2, {.rate=30, .unit=1, .gen=10}}, // NB: shuffled entries order
                {client1, {.rate=30, .unit=1, .gen=1}}
            }},
            {"resourceB", {{client1, {.rate=300, .unit=60, .gen=5}}}},
            {"resourceC", {}}
        },
        .lamport = 153
    };

    // NovaAgentMessage to proto and back
    maps::proto::rate_limiter2::CountersV2 protoAgentMessage;
    write(agentMessage, &protoAgentMessage);
    {
        auto parsed = parse<NovaAgentMessage>(protoAgentMessage);
        EXPECT_EQ(parsed.counters, agentMessage.counters);
        EXPECT_EQ(parsed.limits, agentMessage.limits);
        EXPECT_EQ(parsed.lamport, agentMessage.lamport);
    }

    // Parse as NovaServiceMessage
    auto serviceMessage = parse<NovaServiceMessage>(protoAgentMessage);
    {
        auto expected = NovaServiceMessage {
            .counters = {
                {"resourceA", {{client1, 1}, {client2, 2}}},
                {"resourceB", {{client1, 42}}},
                {"resourceC", {}}
            },
            .limits = {
                {"resourceA", {
                    {client1, {.rate=30, .unit=1, .gen=1}},
                    {client2, {.rate=30, .unit=1, .gen=10}}
                }},
                {"resourceB", {{client1, {.rate=300, .unit=60, .gen=5}}}},
                {"resourceC", {}}
            },
            .lamport = 153
        };

        EXPECT_EQ(serviceMessage.counters, expected.counters);
        EXPECT_EQ(serviceMessage.limits, expected.limits);
        EXPECT_EQ(serviceMessage.lamport, expected.lamport);
    }

    // NovaServiceMessage to proto and back
    maps::proto::rate_limiter2::CountersV2 protoServiceMessage;
    write(serviceMessage, &protoServiceMessage);
    {
        auto parsed = parse<NovaServiceMessage>(protoServiceMessage);
        EXPECT_EQ(parsed.counters, serviceMessage.counters);
        EXPECT_EQ(parsed.limits, serviceMessage.limits);
        EXPECT_EQ(parsed.lamport, serviceMessage.lamport);
    }

    // And back to NovaAgentMessage
    {  // compare with original
        auto parsed = parse<NovaAgentMessage>(protoServiceMessage);
        EXPECT_EQ(parsed.counters, agentMessage.counters);
        EXPECT_EQ(parsed.limits, agentMessage.limits);
        EXPECT_EQ(parsed.lamport, agentMessage.lamport);
    }
}

Y_UNIT_TEST(nova_message_serialization_validations)
{
    auto client1 = uuid("11111111-1111-1111-1111-111111111111");
    auto client2 = uuid("22222222-2222-2222-2222-222222222222");
    {  // Write with missed limit entry fails
        auto message = NovaServiceMessage {
            .counters = {{"resourceA", {{client1, 1}, {client2, 2}}}},
            .limits = {{"resourceA", {{client1, {.rate=30, .unit=1, .gen=1}}}}}
        };
        maps::proto::rate_limiter2::CountersV2 protoMessage;
        EXPECT_THROW_MESSAGE_HAS_SUBSTR(
            write(message, &protoMessage);,
            maps::LogicError,
            "No matching limit entry for the counter"
        );
    }
    {  // Write with missed resource in limits fails
        auto message = NovaServiceMessage {
            .counters = {
                {"resourceA", {{client1, 1}}},
                {"resourceB", {}},
            },
            .limits = {{"resourceA", {{client1, {.rate=30, .unit=1, .gen=1}}}}}
        };
        maps::proto::rate_limiter2::CountersV2 protoMessage;
        EXPECT_THROW(
            write(message, &protoMessage);,
            std::out_of_range
        );
    }
    {  // Write ignores extra resources and entries in limits collection
        auto message = NovaServiceMessage {
            .counters = {{"resourceA", {{client1, 1}}}},
            .limits = {
                {"resourceA", {
                    {client1, {.rate=30, .unit=1, .gen=1}},
                    {client1, {.rate=300, .unit=61, .gen=1}}
                }},
                {"resourceB", {}}
            }
        };
        maps::proto::rate_limiter2::CountersV2 protoMessage;
        write(message, &protoMessage);

        auto parsed = parse<NovaServiceMessage>(protoMessage);
        EXPECT_EQ(parsed.counters, message.counters);
        EXPECT_EQ(
            parsed.limits,  // NB: unused limits not serialized
            SortedLimits({{"resourceA", {{client1, {.rate=30, .unit=1, .gen=1}}}}})
        );
        EXPECT_EQ(parsed.lamport, message.lamport);
    }
    {  // Write with invalid generation fails
        auto message = NovaServiceMessage {
            .counters = {{"resourceA", {{client1, 1}}}},
            .limits = {{"resourceA", {{client1, {}}}}}
        };
        maps::proto::rate_limiter2::CountersV2 protoMessage;
        EXPECT_THROW_MESSAGE_HAS_SUBSTR(
            write(message, &protoMessage);,
            maps::LogicError,
            "Invalid limit/generation entry"
        );
    }
}

Y_UNIT_TEST(nova_message_deserialization_validations)
{
    // We check NovaServiceMessage and NovaAgentMessage in each case
    {  // parse with missed limit_rate / generation
        auto protoMessage = jsonToProto<maps::proto::rate_limiter2::CountersV2>(R"({
            "resources": [{
                "name": "resourceA", "limit_unit": 60,
                "client_id_low": [1, 2],
                "client_id_high": [3, 4],
                "value": [1, 2],
                "limit_rate": [10, 20],
                "generation": [1]
            }]
        })");
        EXPECT_THROW_MESSAGE_HAS_SUBSTR(
            parse<NovaServiceMessage>(protoMessage),
            maps::RuntimeError,
            "Inconsistent client_id, values, limit_rate, generation fields count"
        );
        EXPECT_THROW_MESSAGE_HAS_SUBSTR(
            parse<NovaAgentMessage>(protoMessage),
            maps::RuntimeError,
            "Inconsistent client_id, values, limit_rate, generation fields count"
        );

        protoMessage = jsonToProto<maps::proto::rate_limiter2::CountersV2>(R"({
            "resources": [{
                "name": "resourceA", "limit_unit": 60,
                "client_id_low": [1, 2],
                "client_id_high": [3, 4],
                "value": [1, 2],
                "limit_rate": [10],
                "generation": [1, 3]
            }]
        })");
        EXPECT_THROW_MESSAGE_HAS_SUBSTR(
            parse<NovaServiceMessage>(protoMessage),
            maps::RuntimeError,
            "Inconsistent client_id, values, limit_rate, generation fields count"
        );
        EXPECT_THROW_MESSAGE_HAS_SUBSTR(
            parse<NovaAgentMessage>(protoMessage),
            maps::RuntimeError,
            "Inconsistent client_id, values, limit_rate, generation fields count"
        );
        // Success case
        protoMessage = jsonToProto<maps::proto::rate_limiter2::CountersV2>(R"({
            "resources": [{
                "name": "resourceA", "limit_unit": 60,
                "client_id_low": [1, 2],
                "client_id_high": [1, 2],
                "value": [1, 5],
                "limit_rate": [10, 300],
                "generation": [1, 3]
            }]
        })");
        auto parsedServiceMessage = parse<NovaServiceMessage>(protoMessage);
        EXPECT_EQ(parsedServiceMessage.limits.at("resourceA").size(), 2ul);
        EXPECT_EQ(parsedServiceMessage.counters.at("resourceA").size(), 2ul);

        auto parsedAgentMessage = parse<NovaAgentMessage>(protoMessage);
        EXPECT_EQ(parsedServiceMessage.limits.at("resourceA").size(), 2ul);
        EXPECT_EQ(parsedServiceMessage.counters.at("resourceA").size(), 2ul);
    }
    {  // parse with invalid limit_unit
        auto protoMessage = jsonToProto<maps::proto::rate_limiter2::CountersV2>(R"({
            "resources": [{
                "name": "resourceA",
                "client_id_low": [1], "client_id_high": [3],
                "value": [1], "limit_rate": [10], "generation": [1]
            }]
        })");
        EXPECT_THROW_MESSAGE_HAS_SUBSTR(
            parse<NovaServiceMessage>(protoMessage),
            maps::RuntimeError,
            "Invalid limit.time_unit value"
        );
        EXPECT_THROW_MESSAGE_HAS_SUBSTR(
            parse<NovaAgentMessage>(protoMessage),
            maps::RuntimeError,
            "Invalid limit.time_unit value"
        );
    }
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::rate_limiter2::tests
