#include <maps/infra/quotateka/agent/include/inventory.h>

#include <maps/infra/quotateka/agent/tests/proto_utils.h>
#include <maps/infra/quotateka/agent/tests/test_helpers.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::quotateka::tests {

Y_UNIT_TEST_SUITE(deserialization_test) {

Y_UNIT_TEST(empty_update)
{
    auto inventory = Inventory::deserializeFromProto(proto::ProviderInventory());
    EXPECT_EQ(inventory.accounts(), Inventory::AccountsInventory{});
    EXPECT_EQ(inventory.resourceQuotas(), Inventory::ResourceQuotasInventory{});
}

Y_UNIT_TEST(only_resources_update)
{
    auto inventoryProto = jsonToProto<proto::ProviderInventory>(R"({
        "quotas_version": 1,
        "provider_id": "driving-router",
        "resources": [{
            "resource_id": "resA",
            "type": "PerSecondLimit",
            "default_limit": 42,
            "anonym_limit": 84,
            "endpoints": [
                {"path": "/one", "cost": 11},
                {"path": "/two", "cost": 22},
            ],
        }, {
            "resource_id": "resB",
            "type": "PerHourLimit",
            "default_limit": 123,
            "anonym_limit": 456
        }],
        "accounts": [],
        "quotas": []
    })");

    auto inventory = Inventory::deserializeFromProto(inventoryProto);
    EXPECT_STREQ(inventory.providerId().c_str(), "driving-router");
    EXPECT_EQ(inventory.accounts(), Inventory::AccountsInventory{});
    EXPECT_EQ(
        inventory.resourceQuotas(),
        Inventory::ResourceQuotasInventory({
            {"resA", {
                .resourceId = "resA",
                .unit = 1,
                .defaultLimit = {{.rate=42, .unit=1, .gen=1}, 5*42},
                .anonymLimit = {{.rate=84, .unit=1, .gen=1}, 5*84},
                .endpointCosts = {{"/one", 11}, {"/two", 22}},
            }},
            {"resB", {
                .resourceId = "resB",
                .unit = 3600,
                .defaultLimit = {{.rate=123, .unit=3600, .gen=1}, 1*123},
                .anonymLimit = {{.rate=456, .unit=3600, .gen=1}, 1*456},
            }}
        })
    );
    EXPECT_EQ(inventory.version(), 1);
}

Y_UNIT_TEST(only_accounts_update)
{
    auto inventoryProto = jsonToProto<proto::ProviderInventory>(R"({
        "quotas_version": 42,
        "resources": [],
        "accounts": [{
            "account_id": "projA",
            "quota_id": "quotA",
            "identities": []
        }, {
            "account_id": "projB",
            "quota_id": "projB",
            "identities": [{"tvm_id": 42}, {"tvm_id": 56}]
        }, {
            "account_id": "projC",
            "quota_id": "projC",
            "identities": [{"tvm_id": 123}]
        }],
        "quotas": []
    })");

    auto inventory = Inventory::deserializeFromProto(inventoryProto);
    EXPECT_EQ(
        inventory.accounts(),
        Inventory::AccountsInventory({
            {{56}, "projB"},
            {{42}, "projB"},
            {{123}, "projC"}
        })
    );
    EXPECT_EQ(inventory.resourceQuotas(), Inventory::ResourceQuotasInventory{});
    EXPECT_EQ(inventory.version(), 42);
}

Y_UNIT_TEST(empty_quotas_update)
{
    auto inventoryProto = jsonToProto<proto::ProviderInventory>(R"({
        "quotas_version": 24,
        "resources": [{
            "resource_id": "resA",
            "type": "PerSecondLimit",
            "default_limit": 0,
            "anonym_limit": 0
        }, {
            "resource_id": "resB",
            "type": "PerHourLimit",
            "default_limit": 0,
            "anonym_limit": 0
        }],
        "accounts": [{
            "account_id": "projA",
            "quota_id": "projA",
            "identities": [{"tvm_id": 42}]
        }],
        "quotas": [{
            "quota_id": "projA",
            "quotas": []
        }]
    })");

    auto inventory = Inventory::deserializeFromProto(inventoryProto);
    EXPECT_EQ(
        inventory.accounts(),
        Inventory::AccountsInventory({{{42}, "projA"}})
    );
    EXPECT_EQ(
        inventory.resourceQuotas(),
        Inventory::ResourceQuotasInventory({
            {"resA", Inventory::ResourceQuotas{
                .resourceId = "resA",
                .unit = 1,
                .defaultLimit = {{.unit=1, .gen=24}, 0},
                .anonymLimit = {{.unit=1, .gen=24}, 0},
            }},
            {"resB", Inventory::ResourceQuotas{
                .resourceId = "resB",
                .unit = 3600,
                .defaultLimit = {{.unit=3600, .gen=24}, 0},
                .anonymLimit = {{.unit=3600, .gen=24}, 0},
            }}
        })
    );
    EXPECT_EQ(inventory.version(), 24);
}

Y_UNIT_TEST(non_empty_quotas_update)
{
    auto inventoryProto = jsonToProto<proto::ProviderInventory>(R"({
        "quotas_version": 24,
        "resources": [{
            "resource_id": "resA",
            "type": "PerSecondLimit",
            "default_limit": 0,
            "anonym_limit": 0
        }, {
            "resource_id": "resB",
            "type": "PerHourLimit",
            "default_limit": 0,
            "anonym_limit": 0
        }],
        "accounts": [{
            "account_id": "projA",
            "quota_id": "projA",
            "identities": [{"tvm_id": 42}]
        }],
        "quotas": [{
            "quota_id": "projA",
            "quotas": [
                {"resource_id": "resA", "limit": 10},
                {"resource_id": "resB", "limit": 1000}
            ]
        },{
            "quota_id": "projA",
            "quotas": [
                {"resource_id": "resA", "limit": 153},
            ]
        }]
    })");

    auto inventory = Inventory::deserializeFromProto(inventoryProto);
    EXPECT_EQ(
        inventory.accounts(),
        Inventory::AccountsInventory({{{42}, "projA"}})
    );
    // Expect 'quotX' ignored 'cause there's no matching account
    EXPECT_EQ(
        inventory.resourceQuotas(),
        Inventory::ResourceQuotasInventory({
            {"resA", Inventory::ResourceQuotas{
                .resourceId = "resA",
                .unit = 1,
                .defaultLimit = {{.unit=1, .gen=24}, 0},
                .anonymLimit = {{.unit=1, .gen=24}, 0},
                .accountLimits = {{"projA", {{.rate=10, .unit=1, .gen=24}, 5*10}}}
            }},
            {"resB", Inventory::ResourceQuotas{
                .resourceId = "resB",
                .unit = 3600,
                .defaultLimit = {{.unit=3600, .gen=24}, 0},
                .anonymLimit = {{.unit=3600, .gen=24}, 0},
                .accountLimits = {{"projA", {{.rate=1000, .unit=3600, .gen=24}, 1*1000}}}
            }}
        })
    );
    EXPECT_EQ(inventory.version(), 24);
}

} // Y_UNIT_TEST_SUITE(deserialization_test)

} // namespace maps::quotateka::tests
