#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/analyzer/modules/matcher/dispatcher/lib/shard_list.h>

Y_UNIT_TEST_SUITE(shard_list)
{
    Y_UNIT_TEST(read_json)
    {
        const std::string text = R"(
        {
            "shards":
                [
                    {
                        "id":"2",
                        "nodes":
                            [
                                {"url": "21/matcher/collect_gpssignal"},
                                {"url": "22/matcher/collect_gpssignal"},
                                {"url": "23/matcher/collect_gpssignal"}
                            ]
                    },
                    {
                        "id":"1",
                        "nodes":
                            [
                                {"url": "11/matcher/collect_gpssignal"},
                                {"url": "12/matcher/collect_gpssignal"},
                                {"url": "13/matcher/collect_gpssignal"}
                            ]
                    },
                    {
                        "id":"3",
                        "nodes":
                            [
                                {"url": "31/matcher/collect_gpssignal"},
                                {"url": "32/matcher/collect_gpssignal"},
                                {"url": "33/matcher/collect_gpssignal"}
                            ]
                    }
                ]
        })";
        maps::analyzer::matcher::ShardList shardes(
            maps::json::Value::fromString(text));
        {
            const std::string uuid = "11";
            UNIT_ASSERT_EQUAL(std::hash<std::string>{}(uuid) % 3, 0);
            const auto& shard = shardes.shard(uuid);
            UNIT_ASSERT_EQUAL(shard.id(), "1");
            UNIT_ASSERT_EQUAL(shard.nodes().size(), 3);
            UNIT_ASSERT_EQUAL(
                shard.nodes()[0].url(),
                "11/matcher/collect_gpssignal");
        }
        {
            const std::string uuid = "22";
            UNIT_ASSERT_EQUAL(std::hash<std::string>{}(uuid) % 3, 1);
            const auto& shard = shardes.shard(uuid);
            UNIT_ASSERT_EQUAL(shard.id(), "2");
            UNIT_ASSERT_EQUAL(shard.nodes().size(), 3);
            UNIT_ASSERT_EQUAL(
                shard.nodes()[0].url(),
                "21/matcher/collect_gpssignal");
        }
        {
            const std::string uuid = "3";
            UNIT_ASSERT_EQUAL(std::hash<std::string>{}(uuid) % 3, 2);
            const auto& shard = shardes.shard(uuid);
            UNIT_ASSERT_EQUAL(shard.id(), "3");
            UNIT_ASSERT_EQUAL(shard.nodes().size(), 3);
            UNIT_ASSERT_EQUAL(
                shard.nodes()[0].url(),
                "31/matcher/collect_gpssignal");
        }
    }
}
