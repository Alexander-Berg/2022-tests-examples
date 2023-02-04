#include <maps/automotive/parking/fastcgi/parking_api/lib/request_sender.h>

#include <maps/automotive/parking/lib/endpoint_cache/include/endpoint_cache.h>

#include <maps/automotive/proto/parking_internal.pb.h>
#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/testing/unittest/registar.h>

#include <string>
#include <vector>
#include <set>
#include <algorithm>
#include <cstdint>

using namespace maps::automotive::parking::endpoint_cache;
using namespace std::chrono;
using namespace std::literals::chrono_literals;

namespace maps::automotive::parking::tests {

namespace  {
const char* API_PATH_INTERNAL_REGISTER_DEVICE = "/parking/1.x/internal/register_devices";
const Endpoints endpoints1{
    { "test1", "1.1.1.1", "", 1 },
    { "test2", "2.2.2.2", "", 2 },
    { "test3", "", "::3333", 3 } };
const Endpoints endpoints2{
    { "test4", "", "::4444", 4 },
    { "test5", "", "::5555", 5 },
    { "test6", "6.6.6.6", "", 6 } };
const DeviceRegistryEntries entries{
    { 123, "abc", 100s },
    { 456, "def", 110s }
};
const std::string testTicket = "fWw8aN5K3XO0dChBzR6K+9SzkYGNsSyh";
const std::string apiHost = "auto-parking-api.maps.yandex.net";
}

Y_UNIT_TEST_SUITE(test_request_forwarding) {

    Y_UNIT_TEST(check_device_registration_forwarding)
    {
        Endpoints endpoints;
        endpoints.insert(endpoints.end(), endpoints1.begin(), endpoints1.end());
        endpoints.insert(endpoints.end(), endpoints2.begin(), endpoints2.end());
        std::set<std::string> visitedEndpoints;
        std::vector<maps::http::MockHandle> mockHandles;
        for (const auto& endpoint : endpoints) {
            auto& host = endpoint.ip4_.empty() ? endpoint.ip6_ : endpoint.ip4_;
            maps::http::URL url;
            url.setScheme("http");
            url.setHost(host);
            if (endpoint.port_)
                url.setPort(endpoint.port_);
            url.setPath(API_PATH_INTERNAL_REGISTER_DEVICE);
            mockHandles.push_back(maps::http::addMock(
                url,
                [endpoint, &visitedEndpoints]
                (const maps::http::MockRequest& request) mutable {
                    UNIT_ASSERT_EQUAL(request.header("Host"), apiHost);
                    UNIT_ASSERT_EQUAL(request.header("X-Ya-Service-Ticket"), testTicket);
                    visitedEndpoints.emplace(endpoint.fqdn_);
                    yandex::automotive::proto::parking_internal::DeviceRegistryEntries entriesProto;
                    UNIT_ASSERT(entriesProto.ParseFromArray(request.body.data(), request.body.size()));
                    for (auto& entryProto : entriesProto.entries()) {
                        auto ttlSeconds = DeviceRegistryEntry::Duration(entryProto.ttlseconds());
                        DeviceRegistryEntry entry{ entryProto.uid(), entryProto.device_id(), ttlSeconds };
                        auto it = std::find(entries.begin(), entries.end(), entry);
                        UNIT_ASSERT(it != entries.end());
                    }

                    return maps::http::MockResponse();
                }));
        }

        Clusters clusters1{{ "cluster1", { "cluster1", 0, endpoints1 } }};
        EndpointSet endpointSet1{ "endpoint_set_id_1", clusters1 };
        Clusters clusters2{{ "cluster2", { "cluster2", 0, endpoints2 } }};
        EndpointSet endpointSet2{ "endpoint_set_id_2", clusters2 };
        EndpointSets endpointSets{{ endpointSet1.id_, endpointSet1 }, { endpointSet2.id_, endpointSet2 }};
        forwardDeviceRegistration(entries, entries.size(), testTicket, endpointSets);
        for (auto &endpoint : endpoints)
            UNIT_ASSERT(visitedEndpoints.count(endpoint.fqdn_));
    }

} //Y_UNIT_TEST_SUITE

} //maps::automotive::parking::tests
