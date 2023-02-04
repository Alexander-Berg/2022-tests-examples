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
#include <iostream>

using namespace maps::automotive::parking::endpoint_cache;
using namespace std::chrono;
using namespace std::literals::chrono_literals;

namespace maps::automotive::parking::tests {

namespace  {
const char* API_PATH_INTERNAL_GET_REGISTERED_DEVICES = "/parking/1.x/internal/get_registered_devices";
const Endpoints endpoints1{
    { "test1", "1.1.1.1", "", 1 },
    { "test2", "2.2.2.2", "", 2 } };
const Endpoints endpoints2{
    { "test3", "", "::3333", 3 },
    { "test4", "", "::4444", 4 } };
const Endpoints endpoints3{
    { "test5", "", "::5555", 5 },
    { "test6", "6.6.6.6", "", 6 } };
const DeviceRegistryEntries entries1{
    { 1, "abc", 100s },
    { 2, "def", 110s }
};
const DeviceRegistryEntries entries2{
    { 2, "def", 110s },
    { 3, "ghi", 100s }
};
const DeviceRegistryEntries entries3{
    { 1, "abc", 110s },
    { 2, "def", 110s }
};
const DeviceRegistryEntries resultEntries{
    { 1, "abc", 100s },
    { 2, "def", 110s },
    { 3, "ghi", 100s },
    { 1, "abc", 110s }
};
const std::string testTicket = "fWw8aN5K3XO0dChBzR6K+9SzkYGNsSyh";
const std::string apiHost = "auto-parking-api.maps.yandex.net";
}

Y_UNIT_TEST_SUITE(test_request_get_devices) {

    Y_UNIT_TEST(check_get_registered_devices)
    {
        std::vector<std::pair<Endpoints, DeviceRegistryEntries>> endpointsToDevices = { {endpoints1, entries1}, {endpoints2, entries2}, {endpoints3, entries3} };
        std::set<std::string> visitedEndpoints;
        std::vector<maps::http::MockHandle> mockHandles;
        for (const auto& it : endpointsToDevices) {
            for (const auto& endpoint : it.first) {
                auto& deviceEntries = it.second;
                auto& host = endpoint.ip4_.empty() ? endpoint.ip6_ : endpoint.ip4_;
                maps::http::URL url;
                url.setScheme("http");
                url.setHost(host);
                if (endpoint.port_)
                    url.setPort(endpoint.port_);
                url.setPath(API_PATH_INTERNAL_GET_REGISTERED_DEVICES);
                mockHandles.push_back(maps::http::addMock(
                    url,
                    [endpoint, &visitedEndpoints, deviceEntries]
                    (const maps::http::MockRequest& request) mutable {
                        UNIT_ASSERT_EQUAL(request.header("Host"), apiHost);
                        UNIT_ASSERT_EQUAL(request.header("X-Ya-Service-Ticket"), testTicket);
                        visitedEndpoints.emplace(endpoint.fqdn_);
                        auto entriesProto = encode(deviceEntries);
                        TString serializedEntries;
                        Y_PROTOBUF_SUPPRESS_NODISCARD entriesProto.SerializeToString(&serializedEntries);
                        return maps::http::MockResponse(serializedEntries);
                    }));
            }
        }

        Clusters clusters1{{ "cluster1", { "cluster1", 0, endpoints1 } }};
        EndpointSet endpointSet1{ "endpoint_set_id_1", clusters1 };
        Clusters clusters2{{ "cluster2", { "cluster2", 0, endpoints2 } }};
        EndpointSet endpointSet2{ "endpoint_set_id_2", clusters2 };
        Clusters clusters3{{ "cluster3", { "cluster3", 0, endpoints3 } }};
        EndpointSet endpointSet3{ "endpoint_set_id_3", clusters3 };
        EndpointSets endpointSets{{ endpointSet1.id_, endpointSet1 }, { endpointSet2.id_, endpointSet2 }, { endpointSet3.id_, endpointSet3 }};
        auto entries = requestRegisteredDevices(testTicket, endpointSets);
        for (const auto& it : endpointsToDevices)
            for (const auto& endpoint : it.first)
                UNIT_ASSERT(visitedEndpoints.count(endpoint.fqdn_));
        for (const auto& entry : entries) {
            auto it = std::find(resultEntries.begin(), resultEntries.end(), entry);
            UNIT_ASSERT(it != resultEntries.end());
        }
    }

} //Y_UNIT_TEST_SUITE

} //maps::automotive::parking::tests
