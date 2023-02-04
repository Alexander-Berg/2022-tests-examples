#include <maps/infra/apiteka/agent/tests/samples.h>

#include <maps/libs/http/include/response.h>
#include <maps/infra/apiteka/agent/lib/include/inventory.h>
#include <maps/infra/apiteka/proto/apiteka.pb.h>

#include <maps/libs/json/include/value.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <contrib/restricted/googletest/googlemock/include/gmock/gmock.h>
#include <contrib/libs/protobuf/src/google/protobuf/util/json_util.h>

#include <iostream>

using namespace testing;
namespace proto = yandex::maps::proto::apiteka;

using std::chrono::steady_clock;

namespace maps::apiteka::tests {

TEST(Inventory, DeserializeEmptyKey)
{
    proto::ProviderInventory inventoryProto;
    inventoryProto.add_keys_by_plan()->add_keys();

    EXPECT_THROW_MESSAGE_HAS_SUBSTR(
        Inventory::createFromProto(inventoryProto, steady_clock::now()),
        RuntimeError, "Invalid inventory entry with empty key"
    );
}

TEST(Inventory, DeserializeMissingSecret)
{
    proto::ProviderInventory inventoryProto;
    auto keySpec = inventoryProto.add_keys_by_plan()->add_keys();
    keySpec->set_key("this-is-the-key");
    keySpec->add_restrictions()->mutable_signature()->set_signing_secret("");

    EXPECT_THROW_MESSAGE_HAS_SUBSTR(
        Inventory::createFromProto(inventoryProto, steady_clock::now()),
        RuntimeError, "Missing secret"
    );
}

TEST(Inventory, Deserialize)
{
    proto::ProviderInventory inventoryProto;
    {
        auto planA = inventoryProto.add_keys_by_plan();
        planA->mutable_plan()->set_id("PlanA");
        planA->mutable_plan()->set_features(
            R"({"allowLogoDisabling", "1"}, {"maxSize", "1000,1000"})"
        );
        {
            auto keySpec = planA->add_keys();
            keySpec->set_key(TString{samples::SOME_APIKEY});
            keySpec->add_restrictions()->mutable_signature()->set_signing_secret(TString{samples::SOME_SECRET});
            keySpec->add_restrictions()->set_app_id("someapp");
            keySpec->add_restrictions()->set_app_id("someapp.x");
        }
        {
            auto keySpec = planA->add_keys();
            keySpec->set_key(TString{samples::ANOTHER_APIKEY});
            keySpec->add_restrictions()->mutable_signature()->set_signing_secret(TString{samples::ANOTHER_SECRET});
        }

        auto planB = inventoryProto.add_keys_by_plan();
        {
            auto keySpec = planB->add_keys();
            keySpec->set_key("extra-key");
            keySpec->add_restrictions()->set_http_referer("yandex.kek");
        }
    }

    const auto timestamp = steady_clock::now();
    auto inventory = Inventory::createFromProto(inventoryProto, timestamp);

    EXPECT_EQ(inventory.updated, timestamp);
    EXPECT_THAT(
        inventory.keys,
        UnorderedElementsAre(
            Pair(samples::SOME_APIKEY, FieldsAre(
                ElementsAre(
                    VariantWith<SignatureValidity>(SignatureValidity{samples::SOME_SECRET}),
                    VariantWith<AppIdValidity>(AppIdValidity{{"someapp", "someapp.x"}})
                ),
                FieldsAre(
                    "PlanA",
                    R"({"allowLogoDisabling", "1"}, {"maxSize", "1000,1000"})"
                )
            )),
            Pair(samples::ANOTHER_APIKEY, FieldsAre(
                ElementsAre(
                    VariantWith<SignatureValidity>(SignatureValidity{samples::ANOTHER_SECRET})
                ),
                FieldsAre(
                    "PlanA",
                    R"({"allowLogoDisabling", "1"}, {"maxSize", "1000,1000"})"
                )
            )),
            Pair("extra-key", FieldsAre(
                ElementsAre(
                    VariantWith<HttpRefererValidity>(HttpRefererValidity{{"yandex.kek"}})
                ),
                Inventory::Plan{}  // empty plan
            ))
        )
    );
}

} // namespace maps::apiteka::tests
