#include <maps/infra/apiteka/agent/tests/backend_stub.h>
#include <maps/infra/apiteka/agent/tests/samples.h>
#include <maps/infra/apiteka/proto/apiteka.pb.h>

#include <maps/libs/http/include/response.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gtest_protobuf/matcher.h>
#include <contrib/restricted/googletest/googlemock/include/gmock/gmock.h>

namespace proto = yandex::maps::proto::apiteka;

namespace maps::apiteka::tests {

namespace samples {
const std::string SOME_INPUT{"deadbeef"};
const std::string ANOTHER_INPUT{"helloworld"};
} // namespace samples

TEST(BackendStub, SeparateIOStringBuf)
{
    SeparateIOStringBuf buffer{samples::SOME_INPUT};

    std::string result;
    {
        std::istream stream{&buffer};
        ASSERT_TRUE(std::getline(stream, result));
        EXPECT_EQ(result, samples::SOME_INPUT);
        EXPECT_TRUE(stream.eof());
    }
    {
        std::ostream stream{&buffer};
        stream << samples::ANOTHER_INPUT;
        EXPECT_EQ(buffer.output(), samples::ANOTHER_INPUT);
        stream.flush();
        EXPECT_TRUE(buffer.output().empty());
    }

    std::istream stream{&buffer};
    ASSERT_TRUE(std::getline(stream, result));
    EXPECT_EQ(result, samples::SOME_INPUT);
    EXPECT_TRUE(stream.eof());
}

TEST(BackendStub, InventoryResponse)
{
    using NGTest::EqualsProto;
    using namespace google::protobuf::util;
    using Comparison = MessageDifferencer::RepeatedFieldComparison;

    BackendStub backendStub{{
        {samples::SOME_APIKEY, samples::SOME_SECRET},
        {samples::ANOTHER_APIKEY, samples::ANOTHER_SECRET}
    }};

    http::Response response{&backendStub, http::GET, {}, backendStub.connect({})};

    proto::ProviderInventory inventory;
    EXPECT_NO_THROW(inventory.ParseFromStringOrThrow(response.readBody()));

    proto::ProviderInventory reference;
    {
        auto planEntry = reference.add_keys_by_plan();
        {
            auto keySpec = planEntry->add_keys();
            keySpec->set_key(TString{samples::SOME_APIKEY});
            keySpec->add_restrictions()->mutable_signature()->set_signing_secret(
                TString{samples::SOME_SECRET}
            );
        }
        {
            auto keySpec = planEntry->add_keys();
            keySpec->set_key(TString{samples::ANOTHER_APIKEY});
            keySpec->add_restrictions()->mutable_signature()->set_signing_secret(
                TString{samples::ANOTHER_SECRET}
            );
        }
    }

    ASSERT_THAT(
        inventory,
        EqualsProto(reference, {.RepeatedFieldComparison = Comparison::AS_SET})
    );
}

}  // namespace maps::apiteka::tests
