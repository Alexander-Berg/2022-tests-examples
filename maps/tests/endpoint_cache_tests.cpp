#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include <maps/automotive/parking/lib/endpoint_cache/include/endpoint_cache.h>

#include <infra/yp_service_discovery/api/api.pb.h>
#include <util/system/hostname.h>

#include <filesystem>

namespace maps::automotive::parking::endpoint_cache {

namespace {

const EndpointSets ENDPOINT_SETS_REFERENCE = {
    {"id_1",{"id_1",{
        {"cluster1", {"cluster1", 100, {
            { "cluster1.ep1", "1.1.1.1", "::1111", 1 },
            { "cluster1.ep2", "1.2.1.2", "::1212", 1 }
        }}},
        {"cluster2", {"cluster2", 200, {}}}}
    }},
    {"id_2",{"id_2",{
            {"cluster1", { "cluster1", 100, {{ "cluster1.ep1", "1.1.1.1", "::1111", 1 }}}},
            {"cluster2", { "cluster2", 100, {}}}
        }
    }}
};

}

using namespace NYP::NServiceDiscovery::NApi;

TRspResolveEndpoints buildResponse(const Cluster& cluster) {
    TRspResolveEndpoints response;
    for (auto& endpointReference : cluster.endpoints_) {
        TEndpoint* endpoint = response.Mutableendpoint_set()->add_endpoints();
        endpoint->set_fqdn(endpointReference.fqdn_.c_str());
        endpoint->set_ip4_address(endpointReference.ip4_.c_str());
        endpoint->set_ip6_address(endpointReference.ip6_.c_str());
        endpoint->set_port(endpointReference.port_);
    }
    response.set_timestamp(cluster.timestamp_);
    return response;
}

Y_UNIT_TEST_SUITE(test_parking_lib_endpoint_cache) {

    Y_UNIT_TEST(check_that_load_works_fine)
    {
        EndpointCache cache({"id_1", "id_2"}, {}, "endpointSet.json",
            [](const ClusterName&, const EndpointSetId&) {
                return TRspResolveEndpoints{};
            });
        auto endpointSets = *cache.endpointSets();
        UNIT_ASSERT_EQUAL(ENDPOINT_SETS_REFERENCE, endpointSets);
    }

    Y_UNIT_TEST(check_that_update_works_fine)
    {
        std::string jsonFilename = BuildRoot() + "/maps/automotive/parking/lib/endpoint_cache/tests/data/endpointSet.json.out";

        EndpointCache cache({"id_1", "id_2"}, { "cluster1", "cluster2" }, jsonFilename,
            [](const ClusterName& clusterName, const EndpointSetId& endpointSetId) {
                UNIT_ASSERT_C(ENDPOINT_SETS_REFERENCE.count(endpointSetId), "Unexpected EndpointSet id \"" + endpointSetId + "\"");
                auto& clusters = ENDPOINT_SETS_REFERENCE.at(endpointSetId).clusters_;
                UNIT_ASSERT_C(clusters.count(clusterName), "Unexpected cluster id \"" + clusterName + "\"");
                return buildResponse(clusters.at(clusterName));
            });
        cache.update();

        EndpointCache cacheLoad({"id_1", "id_2"}, {}, jsonFilename,
            [](const ClusterName&, const EndpointSetId&) {
                return TRspResolveEndpoints{};
            });
        auto endpointSets = *cacheLoad.endpointSets();
        UNIT_ASSERT_EQUAL(ENDPOINT_SETS_REFERENCE, endpointSets);
        std::filesystem::remove(jsonFilename);
    }

    Y_UNIT_TEST(check_that_only_newer_entries_updated)
    {
        const EndpointSets endpointSetsUpdate[] =
        {
            ENDPOINT_SETS_REFERENCE,
            {{"id_1",{"id_1",
                {
                    {"cluster1",{"cluster1", 80, {{ "cluster1.ep3", "1.3.1.3", "::1313", 1 }}}},
                    {"cluster2",{"cluster2", 250, {
                        { "cluster2.ep1", "2.1.2.1", "::2121", 1 },
                        { "cluster2.ep2", "2.2.2.2", "::2222", 1 }
                    }}}
                }}
            },
            {"id_2",{"id_2",
                {
                    {"cluster1",{ "cluster1", 80, {{ "cluster1.ep1", "1.1.1.1", "::1111", 1 }}}},
                    {"cluster2", { "cluster2", 80, {}}}
                }
            }}}
        };
        const EndpointSets endpointSetsUpdatedReference = {
            {"id_1",{"id_1",{
                {"cluster1", {"cluster1", 100, {
                    { "cluster1.ep1", "1.1.1.1", "::1111", 1 },
                    { "cluster1.ep2", "1.2.1.2", "::1212", 1 }
                }}},
                {"cluster2", {"cluster2", 250, {
                    { "cluster2.ep1", "2.1.2.1", "::2121", 1 },
                    { "cluster2.ep2", "2.2.2.2", "::2222", 1 }
                }}}}
            }},
            {"id_2",{"id_2",{
                {"cluster1", { "cluster1", 100, {{ "cluster1.ep1", "1.1.1.1", "::1111", 1 }}}},
                {"cluster2", { "cluster2", 100, {}}}
            }}}
        };

        int updateIndex = 0;
        EndpointCache cache({"id_1", "id_2"}, { "cluster1", "cluster2" }, "",
            [&endpointSetsUpdate, &updateIndex](const ClusterName& cluster, const EndpointSetId& endpointSetId) {
                UNIT_ASSERT_C(endpointSetsUpdate[updateIndex].count(endpointSetId), "Unexpected EndpointSet id \"" + endpointSetId + "\"");
                auto& clusters = endpointSetsUpdate[updateIndex].at(endpointSetId).clusters_;
                UNIT_ASSERT_C(clusters.count(cluster), "Unexpected cluster id \"" + cluster + "\"");
                return buildResponse(clusters.at(cluster));
            });
        cache.update();
        updateIndex++;
        cache.update();

        auto endpointSets = *cache.endpointSets();
        UNIT_ASSERT_EQUAL(endpointSetsUpdatedReference, endpointSets);
    }

    Y_UNIT_TEST(check_that_non_existing_cluster_loaded_after_update)
    {
        EndpointSets endpointSetsUpdate = ENDPOINT_SETS_REFERENCE;
        Cluster newCluster{"cluster3", 200, {{ "cluster3.ep2", "3.2.3.2", "::3232", 1 }}};
        endpointSetsUpdate.at("id_1").clusters_.emplace("cluster3", newCluster);
        endpointSetsUpdate.at("id_2").clusters_.emplace("cluster3", newCluster);

        EndpointCache cache({"id_1", "id_2"}, { "cluster1", "cluster2", "cluster3" }, "",
            [&endpointSetsUpdate](const ClusterName& cluster, const EndpointSetId& endpointSetId) {
                UNIT_ASSERT_C(endpointSetsUpdate.count(endpointSetId), "Unexpected EndpointSet id \"" + endpointSetId + "\"");
                auto& clusters = endpointSetsUpdate.at(endpointSetId).clusters_;
                UNIT_ASSERT_C(clusters.count(cluster), "Unexpected cluster id \"" + cluster + "\"");
                return buildResponse(clusters.at(cluster));
            });
        cache.update();

        auto endpointSets = *cache.endpointSets();
        UNIT_ASSERT_EQUAL(endpointSetsUpdate, endpointSets);
    }

    Y_UNIT_TEST(check_that_cache_stays_alive_when_resolver_fails)
    {
        EndpointCache cache({"id_1", "id_2"}, { "cluster1", "cluster2", "cluster3" }, "endpointSet.json",
            [](const ClusterName&, const EndpointSetId&) {
                throw std::exception();
                return TRspResolveEndpoints{};
            });
        cache.update();

        auto endpointSets = *cache.endpointSets();
        UNIT_ASSERT_EQUAL(ENDPOINT_SETS_REFERENCE, endpointSets);
    }

    Y_UNIT_TEST(check_that_own_fqdn_not_added_into_cache)
    {
        std::string jsonFilename = BuildRoot() + "/maps/automotive/parking/lib/endpoint_cache/tests/data/endpointSet.json.out";

        EndpointCache cache({"id_1", "id_2"}, { "cluster1", "cluster2" }, jsonFilename,
            [](const ClusterName& clusterName, const EndpointSetId& endpointSetId) {
                UNIT_ASSERT_C(ENDPOINT_SETS_REFERENCE.count(endpointSetId), "Unexpected EndpointSet id \"" + endpointSetId + "\"");
                auto& clusters = ENDPOINT_SETS_REFERENCE.at(endpointSetId).clusters_;
                UNIT_ASSERT_C(clusters.count(clusterName), "Unexpected cluster id \"" + clusterName + "\"");
                auto cluster = clusters.at(clusterName);
                cluster.endpoints_.emplace_back(GetFQDNHostName(), "", "", 1);
                return buildResponse(cluster);
            });
        cache.update();

        EndpointCache cacheLoad({"id_1", "id_2"}, {}, jsonFilename,
            [](const ClusterName&, const EndpointSetId&) {
                return TRspResolveEndpoints{};
            });
        auto endpointSets = *cacheLoad.endpointSets();
        UNIT_ASSERT_EQUAL(ENDPOINT_SETS_REFERENCE, endpointSets);
        std::filesystem::remove(jsonFilename);
    }

}

} // maps::automotive::parking
