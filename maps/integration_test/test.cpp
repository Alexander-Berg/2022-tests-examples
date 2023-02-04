#include "fixture.h"
#include "maps/b2bgeo/libs/signature/lib/http_signature.h"

#include <ctime>

#include <maps/b2bgeo/libs/postgres_test/postgres.h>
#include <maps/b2bgeo/libs/logger/logger.h>
#include <maps/b2bgeo/libs/progress_monitor/progress_monitor.h>
#include <maps/b2bgeo/libs/traffic_info/matrices_preprocess.h>
#include <maps/b2bgeo/libs/traffic_info/router_config.h>
#include <maps/b2bgeo/libs/traffic_info/traffic_info.h>
#include <maps/b2bgeo/libs/traffic_info/vehicle_class.h>
#include <maps/b2bgeo/libs/traffic_info/common_mrapi.h>
#include <maps/b2bgeo/libs/traffic_info/job_info.h>
#include <maps/b2bgeo/libs/traffic_info/points_projection.h>
#include <maps/b2bgeo/libs/tvm/tvm_client.h>
#include <maps/b2bgeo/libs/traffic_info/matrix_download_info.h>

#include <maps/b2bgeo/routing_public_api/lib/request_utils.h>
#include <maps/doc/proto/yandex/maps/proto/driving_matrix/matrix_stream.pb.h>
#include <maps/libs/auth/include/test_utils.h>
#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/json/include/value.h>
#include <maps/libs/pbstream/include/yandex/maps/pb_stream2/reader.h>
#include <maps/libs/pbstream/include/yandex/maps/pb_stream2/writer.h>

#include <library/cpp/resource/resource.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/b2bgeo/libs/traffic_info/traffic_info_p.h>

namespace maps::b2bgeo::traffic_info {

using namespace maps::b2bgeo::common;
using namespace maps::b2bgeo::cost_matrices;
using namespace maps::b2bgeo::time;

namespace {

const auto MRAPI_REQUEST_URL = "http://core-driving-matrix-router.maps.yandex.net";
const auto MASSTRANSIT_ROUTER_URL = "http://core-masstransit-matrix.common.testing.maps.yandex.net";
const auto DEFAULT_MASSTRANSIT_MATRIX_ROUTER_URL = "http://core-masstransit-matrix.common.testing.maps.yandex.net";
const auto ROUTER_PROXY_URL = "http://b2bgeo-routerproxy.maps.yandex.net";
const auto AWS_MRAPI_REQUEST_URL = "http://some-aws-adress.yandex.net";
const auto STORAGE_URL = "http://storage.mds.yandex.net:80/get-matrix-router-result";
const int MIDNIGHT_MSK = 1524711600;
const int HOUR_S = 60 * 60;
const int TZ_SHIFT_MSK = 3;

const auto DEFAULT_CONFIG = VehicleClassesConfig::fromJson(
    maps::json::Value::fromString(NResource::Find("truck_classes.json")));

struct CostMatricesWithStats {
    cost_matrices::VehicleCostMatrices matrices;
    CostMatricesRoutingStatistics stats;
};

RoutingConfig getRoutingConfig()
{
    auto config = traffic_info::getRoutingConfig(RuntimeEnv::local);
    config.masstransitRouterUrl = MASSTRANSIT_ROUTER_URL;
    config.mtMatrixRouterUrl = DEFAULT_MASSTRANSIT_MATRIX_ROUTER_URL;
    return config;
}

CostMatricesWithStats getMatrices(
    const std::vector<Location>& locations,
    const std::vector<VehicleClass>& vehicleClasses,
    MatrixRouter router)
{
    const TimeRangeWithTimezone timeRangeWithTz(
        MIDNIGHT_MSK, MIDNIGHT_MSK + DAY_S, TZ_SHIFT_MSK);
    auto [vehicleTypeToSlicesWithStats, locationIdToIndexMap] = getMatrices(
        getRoutingConfig(),
        router,
        vehicleClasses,
        locations,
        timeRangeWithTz,
        alwaysContinue,
        false,
        false);

    cost_matrices::RawMatrices rawMatrices = {
        std::move(vehicleTypeToSlicesWithStats.costMatrices),
        std::move(locationIdToIndexMap)};
    auto [costMatrices, _, stats] = preprocessMatrices(
        std::move(rawMatrices),
        locations,
        downloadConfigFromRoutingConfig(getRoutingConfig()),
        router);
    updateMatrixDownloadedStatistics(vehicleTypeToSlicesWithStats.stats, stats);
    return {std::move(costMatrices), vehicleTypeToSlicesWithStats.stats};
}

std::vector<Location> getLocations(const size_t count = 2, const bool random = false)
{
    std::vector<Location> locations(count);
    if (random) {
        srand(std::time(0));
    }

    for (size_t i = 0; i < count; ++i) {
        locations[i].id = i;
        locations[i].point.lat = random ? rand() % 90 : 55;
        locations[i].point.lon = random ? rand() % 90 : 37;
    }

    return locations;
}

std::vector<VehicleClass> mrapiClasses()
{
    const auto config = DEFAULT_CONFIG;
    auto res = config.truckClasses;
    res.push_back(VehicleClass::DRIVING);
    res.push_back(VehicleClass::TRUCK);
    return res;
}

void getMatricesWithMocks()
{
    const auto locations = getLocations();
    const TimeRangeWithTimezone timeRangeWithTz(
        MIDNIGHT_MSK, MIDNIGHT_MSK + HOUR_S, TZ_SHIFT_MSK);

    bool fallback = false;
    bool accessStatus = false;
    bool accessStorage = false;

    auto mockProxy = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(ROUTER_PROXY_URL)),
        [&fallback, &accessStatus, &accessStorage](const maps::http::MockRequest& request) {
            auto xForwardUrl = request.headers.at("X-Forward-To");
            if (xForwardUrl.find(static_cast<std::string>(MRAPI_REQUEST_URL) + 
                                 "/v2/matrix_stream/request") != std::string::npos) {
                maps::http::MockResponse response = maps::http::MockResponse::withStatus(201);
                response.headers["Location"] = "/v2/matrix_stream/status";
                fallback = true;
                return response;
            } else if (xForwardUrl.find(static_cast<std::string>(MRAPI_REQUEST_URL) +
                                        "/v2/matrix_stream/status") != std::string::npos) {
                maps::http::MockResponse response = maps::http::MockResponse::withStatus(308);
                response.headers["Location"] = static_cast<std::string>(STORAGE_URL);
                accessStatus = true;
                return response;
            } else if (xForwardUrl.find(static_cast<std::string>(STORAGE_URL)) != std::string::npos) {
                accessStorage = true;
                return maps::http::MockResponse();
            } else {
                return maps::http::MockResponse::withStatus(500);
            }
        });

    auto routingConfig = traffic_info::getRoutingConfig(maps::b2bgeo::RuntimeEnv::dev);
    routingConfig.awsDrivingMatrixRouterUrl = static_cast<std::string>(AWS_MRAPI_REQUEST_URL);
    routingConfig.requestRouterCloud = common::Cloud::Aws;
    routingConfig.requestRouterMode = common::RequestMode::Direct;
    routingConfig.routerProxyApikeyWithSecret = std::make_pair(
        "c2cc7c5c-f57d-4c58-bc13-39dcf73eb6a6", "aa1cd1107c694fefb4c5b9262e28c8a8");
    routingConfig.routerProxyUrl = ROUTER_PROXY_URL;

    traffic_info::getMatrices(
        routingConfig,
        MatrixRouter::Main,
        {VehicleClass::TRUCK},
        locations,
        timeRangeWithTz,
        alwaysContinue,
        false,
        false);

    UNIT_ASSERT(fallback);
    UNIT_ASSERT(accessStatus);
    UNIT_ASSERT(accessStorage);
}

} // anonymous namespace

Y_UNIT_TEST_SUITE(AWSProxiedRequests) {

Y_UNIT_TEST(prepare_request_no_exception) {
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());

    maps::http::Client client;
    const requests::RequestParams emptyParams;
    const std::string emptyData;

    UNIT_CHECK_GENERATED_NO_EXCEPTION(
            detail::prepareRequest(
                "http://core-driving-router.maps.yandex.net/some/path/",
                common::RequestMode::Proxy,
                common::Cloud::Aws,
                {"c2cc7c5c-f57d-4c58-bc13-39dcf73eb6a6", "aa1cd1107c694fefb4c5b9262e28c8a8"},
                client,
                maps::http::POST,
                DEFAULT_MASSTRANSIT_MATRIX_ROUTER_URL,
                emptyParams, 
                emptyData),
            std::exception);
}

Y_UNIT_TEST(generating_signature) {
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());

    maps::http::Client client;
    const requests::RequestParams emptyParams;
    const std::string emptyData;

    auto request = detail::prepareRequest(
        "http://core-driving-router.maps.yandex.net/some/path/",
        common::RequestMode::Proxy,
        common::Cloud::Aws,
        {"c2cc7c5c-f57d-4c58-bc13-39dcf73eb6a6", "aa1cd1107c694fefb4c5b9262e28c8a8"},
        client,
        maps::http::POST,
        DEFAULT_MASSTRANSIT_MATRIX_ROUTER_URL,
        emptyParams,
        emptyData);

    const auto requestUrl = request.url();
    const auto signature = libs::signature::generateSignature(
        "c2cc7c5c-f57d-4c58-bc13-39dcf73eb6a6",
        "aa1cd1107c694fefb4c5b9262e28c8a8",
        "",
        maps::http::POST.value(),
        requestUrl.path() + "?" + requestUrl.params(),
        emptyData);

    UNIT_ASSERT(signature);

    UNIT_ASSERT_EQUAL(*signature, "358652CB9A29F24897332AA548CEF5CA90741C94C4873FC54139FC0558FDDE39");
}

}// Y_UNIT_TEST_SUITE(ProxiedRequests)

Y_UNIT_TEST_SUITE(MatricesPreprocess) {

namespace {

std::istringstream createMatrixRouterStreamApiResponseStream(
    const std::vector<std::vector<std::pair<uint32_t, uint32_t>>>& values)
{
    std::ostringstream ostream;
    maps::pb_stream2::Writer streamWriter(&ostream);
    for (const auto& row : values) {
        yandex::maps::proto::driving_matrix::Row pbRow;
        pbRow.mutable_duration()->Reserve(row.size());
        pbRow.mutable_distance()->Reserve(row.size());

        for (const auto& weight: row) {
            pbRow.add_distance(weight.first);
            pbRow.add_duration(weight.second);
        }

        streamWriter << pbRow;
    }
    streamWriter.close();
    return std::istringstream(ostream.str());
}

maps::http::MockHandle getMockHandleStatus201() {
    return maps::http::addMock(
        maps::http::URL(static_cast<std::string>(MRAPI_REQUEST_URL) +
                        "/v2/matrix_stream/request"),
        [](const maps::http::MockRequest&) {
            maps::http::MockResponse response =
                maps::http::MockResponse::withStatus(201);
            response.headers["Location"] = "/v2/matrix_stream/status";
            return response;
        });
}

maps::http::MockHandle getMockHandleStatus308() {
    return maps::http::addMock(
        maps::http::URL(static_cast<std::string>(MRAPI_REQUEST_URL) +
                        "/v2/matrix_stream/status"),
        [](const maps::http::MockRequest&) {
            maps::http::MockResponse response =
                maps::http::MockResponse::withStatus(308);
            response.headers["Location"] = static_cast<std::string>(STORAGE_URL);
            return response;
        });
}

} // anonymous namespace

Y_UNIT_TEST(request_to_mrapi_contains_replication_factor_param)
{
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());

    bool replication_factor_is_present = false;
    auto mockHandle = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(MRAPI_REQUEST_URL) + "/v2/matrix_stream/request"),
        [&replication_factor_is_present](const maps::http::MockRequest& request) {
            if (request.url.params().find("replication_factor=") != std::string::npos) {
                replication_factor_is_present = true;
            }
            return maps::http::MockResponse();
        }
    );

    const auto locations = getLocations();
    const TimeRangeWithTimezone timeRangeWithTz(
        MIDNIGHT_MSK, MIDNIGHT_MSK + DAY_S, TZ_SHIFT_MSK);
    getMatrices(
        getRoutingConfig(),
        MatrixRouter::Main,
        {VehicleClass::TRUCK},
        locations,
        timeRangeWithTz,
        alwaysContinue,
        false,
        false);

    UNIT_ASSERT(replication_factor_is_present);
}

Y_UNIT_TEST(retry_on_410)
{
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());

    int attempts = 0;

    auto mockHandleStatus201 = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(MRAPI_REQUEST_URL) +
                        "/v2/matrix_stream/request"),
        [&attempts](const maps::http::MockRequest&) {
            attempts++;

            maps::http::MockResponse response =
                maps::http::MockResponse::withStatus(201);
            response.headers["Location"] = "/v2/matrix_stream/status";
            return response;
        }
    );
    auto mockHandleStatus410 = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(MRAPI_REQUEST_URL) +
                        "/v2/matrix_stream/status"),
        [](const maps::http::MockRequest&) {
            maps::http::MockResponse response =
                maps::http::MockResponse::withStatus(410);
            return response;
        }
    );

    const auto locations = getLocations();
    const TimeRangeWithTimezone timeRangeWithTz(
        MIDNIGHT_MSK, MIDNIGHT_MSK + HOUR_S, TZ_SHIFT_MSK);
    try {
    getMatrices(
        getRoutingConfig(),
        MatrixRouter::Main,
        {VehicleClass::TRUCK},
        locations,
        timeRangeWithTz,
        alwaysContinue,
        false,
        false);
    } catch(const std::exception&) {}
    UNIT_ASSERT_EQUAL(attempts, 5);
}

Y_UNIT_TEST(test_get_links)
{
    TUnistat& stat = TUnistat::Instance();
    const auto priority = NUnistat::TPriority(10);
    registerMatrixMrapiUnistat(stat, priority);

    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());

    auto mockHandleStatus201 = getMockHandleStatus201();
    auto mockHandleStatus308 = getMockHandleStatus308();

    int head_request = 0;
    int get_request = 0;
    auto mockHandleStatusResult = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(STORAGE_URL)),
        [&head_request, &get_request](const maps::http::MockRequest& request) {
            if (request.method == maps::http::HEAD) {
                head_request++;
            } else {
                get_request++;
            }
            return maps::http::MockResponse();
        });

    const auto locations = getLocations();
    const TimeRangeWithTimezone timeRangeWithTz(
        MIDNIGHT_MSK, MIDNIGHT_MSK + DAY_S, TZ_SHIFT_MSK);
    auto [vehicleTypeToSlicesWithStats, locationIdToIndexMap] = getMatrices(
        getRoutingConfig(),
        MatrixRouter::Main,
        {VehicleClass::TRUCK},
        locations,
        timeRangeWithTz,
        alwaysContinue,
        false,
        false);

    ASSERT_EQ(vehicleTypeToSlicesWithStats.costMatrices.size(), 1u);
    for (const auto& slice: vehicleTypeToSlicesWithStats.costMatrices["truck"]) {
        ASSERT_EQ(slice.subMatrices.size(), 1u);
        ASSERT_EQ(slice.subMatrices[0].getType(), cost_matrices::SubMatrixType::Link);
    }
    ASSERT_EQ(head_request, 12);
    ASSERT_EQ(get_request, 0);
    ASSERT_GT(TUnistat::Instance().GetSignalValueUnsafe("download_matrix_mrapi_async_attempts")->GetNumber(), 0);
}

Y_UNIT_TEST(test_download_from_links)
{
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());

    auto mockHandleStatus201 = getMockHandleStatus201();
    auto mockHandleStatus308 = getMockHandleStatus308();

    int get_request = 0;
    auto mockHandleStatusResult = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(STORAGE_URL)),
        [&get_request](const maps::http::MockRequest& request) {
            if (request.method == maps::http::HEAD) {
                return maps::http::MockResponse();
            }

            get_request++;
            std::stringstream ss;
            const std::vector<std::vector<std::pair<uint32_t, uint32_t>>>
                values = {{{1, 2}}};
            auto matrixStream =
                createMatrixRouterStreamApiResponseStream(values);
            return maps::http::MockResponse(matrixStream.str());
        });

    try {
        const auto locations = getLocations();
        const auto result = getMatrices(
            locations,
            {VehicleClass::TRUCK},
            MatrixRouter::Main);

        ASSERT_EQ(get_request, 12);
        ASSERT_EQ(result.matrices.size(), 1u);
        const auto& slices = result.matrices.at("truck");
        ASSERT_EQ(slices.size(), 12u);

        for (size_t i = 0; i < slices.size(); ++i) {
            const auto& slice = slices.at(i);
            ASSERT_EQ(slice.matrix->size(), 1u);
        }
    } catch (const maps::RuntimeError& ex) { }
}

Y_UNIT_TEST(handling_temporary_redirect)
{
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());

    const std::string& testRedirectPath = "/v2/matrix_stream/request/test_redirect";

    auto mockHandleStatus307 = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(MRAPI_REQUEST_URL) +
                        "/v2/matrix_stream/request"),
        [&testRedirectPath](const maps::http::MockRequest&) {
            maps::http::MockResponse response =
                maps::http::MockResponse::withStatus(307);
            response.headers["Location"] = testRedirectPath;
            return response;
        }
    );

    auto mockHandleStatus201 = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(MRAPI_REQUEST_URL) + testRedirectPath),
        [](const maps::http::MockRequest&) {
            maps::http::MockResponse response =
                maps::http::MockResponse::withStatus(201);
            response.headers["Location"] = "/v2/matrix_stream/status";
            return response;
        }
    );

    bool handleRedirect = false;
    auto mockHandleMatrixStreamStatus = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(MRAPI_REQUEST_URL) +
                        "/v2/matrix_stream/status"),
        [&handleRedirect](const maps::http::MockRequest&) {
            handleRedirect = true;
            maps::http::MockResponse response = maps::http::MockResponse::withStatus(308);
            response.headers["Location"] = static_cast<std::string>(STORAGE_URL);
            return response;
        }
    );

    bool accessStorage = false;
    auto mockHandleStatusResult = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(STORAGE_URL)),
        [&accessStorage](const maps::http::MockRequest&) {
            accessStorage = true;
            return maps::http::MockResponse();
        });

    const auto locations = getLocations();
    const TimeRangeWithTimezone timeRangeWithTz(
        MIDNIGHT_MSK, MIDNIGHT_MSK + HOUR_S, TZ_SHIFT_MSK);

    getMatrices(
        getRoutingConfig(),
        MatrixRouter::Main,
        {VehicleClass::TRUCK},
        locations,
        timeRangeWithTz,
        alwaysContinue,
        false,
        false);

    UNIT_ASSERT(handleRedirect);
    UNIT_ASSERT(accessStorage);
}

Y_UNIT_TEST(handling_temporary_redirect_aws)
{
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());

    const std::string testRedirectPath = "/v2/matrix_stream/test_redirect";

    bool handleRedirect = false;
    bool accessStorage = false;

    bool accessExternalIps = false;

    auto mockHandleStatus307 = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(MRAPI_REQUEST_URL) +
                        "/v2/matrix_stream/request"),
        [&accessExternalIps](const maps::http::MockRequest&) {
            accessExternalIps = true;
            return maps::http::MockResponse::withStatus(500);
        }
    );

    auto mockHandleStatus201 = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(MRAPI_REQUEST_URL) + testRedirectPath),
        [&accessExternalIps](const maps::http::MockRequest&) {
            accessExternalIps = true;
            return maps::http::MockResponse::withStatus(500);
        }
    );

    auto mockHandleMatrixStreamStatus = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(MRAPI_REQUEST_URL) +
                        "/v2/matrix_stream/status"),
        [&accessExternalIps](const maps::http::MockRequest&) {
            accessExternalIps = true;
            return maps::http::MockResponse::withStatus(500);
        }
    );

    auto mockHandleStatusResult = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(STORAGE_URL)),
        [&accessExternalIps](const maps::http::MockRequest&) {
            accessExternalIps = true;
            return maps::http::MockResponse::withStatus(500);
        }
    );

    auto mockProxyRedirect = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(ROUTER_PROXY_URL)),
        [&testRedirectPath, &handleRedirect, &accessStorage](const maps::http::MockRequest& request) {
            auto xForwardUrl = request.headers.at("X-Forward-To");
            if (xForwardUrl.find(static_cast<std::string>(MRAPI_REQUEST_URL) + 
                                 "/v2/matrix_stream/request") != std::string::npos) {
                maps::http::MockResponse response = maps::http::MockResponse::withStatus(307);
                response.headers["Location"] = testRedirectPath;
                return response;
            } else if (xForwardUrl.find(static_cast<std::string>(MRAPI_REQUEST_URL) +
                                        testRedirectPath) != std::string::npos) {
                maps::http::MockResponse response = maps::http::MockResponse::withStatus(201);
                response.headers["Location"] = "/v2/matrix_stream/status";
                return response;
            } else if (xForwardUrl.find(static_cast<std::string>(MRAPI_REQUEST_URL) +
                                        "/v2/matrix_stream/status") != std::string::npos) {
                handleRedirect = true;
                maps::http::MockResponse response = maps::http::MockResponse::withStatus(308);
                response.headers["Location"] = static_cast<std::string>(STORAGE_URL);
                return response;
            } else if (xForwardUrl.find(static_cast<std::string>(STORAGE_URL)) != std::string::npos) {
                accessStorage = true;
                return maps::http::MockResponse();
            } else {
                return maps::http::MockResponse::withStatus(500);
            }
        });

    const auto locations = getLocations();
    const TimeRangeWithTimezone timeRangeWithTz(
        MIDNIGHT_MSK, MIDNIGHT_MSK + HOUR_S, TZ_SHIFT_MSK);

    auto routingConfig = getRoutingConfig(maps::b2bgeo::RuntimeEnv::dev);
    routingConfig.requestRouterMode = common::RequestMode::Proxy;
    routingConfig.routerProxyApikeyWithSecret = std::make_pair(
        "c2cc7c5c-f57d-4c58-bc13-39dcf73eb6a6", "aa1cd1107c694fefb4c5b9262e28c8a8");
    routingConfig.routerProxyUrl = ROUTER_PROXY_URL;

    getMatrices(
        routingConfig,
        MatrixRouter::Main,
        {VehicleClass::TRUCK},
        locations,
        timeRangeWithTz,
        alwaysContinue,
        false,
        false);

    UNIT_ASSERT(!accessExternalIps);
    UNIT_ASSERT(handleRedirect);
    UNIT_ASSERT(accessStorage);
}

Y_UNIT_TEST(fallback_to_proxy_on_matrix_request_aws)
{
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());

    bool accessDirect = false;

    auto mockWithError = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(AWS_MRAPI_REQUEST_URL) + "/v2/matrix_stream/request"),
        [&accessDirect](const maps::http::MockRequest&) {
            accessDirect = true;
            throw maps::RuntimeError();
            return maps::http::MockResponse();
        }
    );

    getMatricesWithMocks();

    UNIT_ASSERT(accessDirect);
}

Y_UNIT_TEST(fallback_to_proxy_on_matrix_status_aws)
{
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());

    bool accessDirect = false;

    auto firstMock = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(AWS_MRAPI_REQUEST_URL) + "/v2/matrix_stream/request"),
        [](const maps::http::MockRequest&) {
            maps::http::MockResponse response = maps::http::MockResponse::withStatus(201);
            response.headers["Location"] = "/v2/matrix_stream/status";
            return response;
        }
    );

    auto mockWithError = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(AWS_MRAPI_REQUEST_URL) + "/v2/matrix_stream/status"),
        [&accessDirect](const maps::http::MockRequest&) {
            accessDirect = true;
            throw maps::RuntimeError();
            return maps::http::MockResponse();
        }
    );

    getMatricesWithMocks();

    UNIT_ASSERT(accessDirect);
}

} // Y_UNIT_TEST_SUITE(MatricesPreprocess)

Y_UNIT_TEST_SUITE(MatrixDownloadInfo) {

Y_UNIT_TEST(parses_mrapi_response_correctly)
{
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());

    auto mockHandle = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(MRAPI_REQUEST_URL) + "/v2/schedule"),
        [](const maps::http::MockRequest&) {
            maps::http::MockResponse response = maps::http::MockResponse(
                "{\"redirect\": \
                [{\"location\": \"/v2/matrix_stream/request?dtm=dtm1&vehicle_type=a&vehicle_weight=b&...\", \
                \"dtm\": [{\"from\": 1, \"to\": 2}, {\"from\": 2, \"to\": 3} ]},\
                {\"location\": \"/v2/matrix_stream/request?dtm=dtm2&vehicle_type=a&vehicle_weight=b&...\", \
                \"dtm\": [{\"from\": 3, \"to\": 4}\
                ]}]}"
            );
            return response;
        }
    );

    const TimeRangeWithTimezone timeRangeWithTz(
        MIDNIGHT_MSK, MIDNIGHT_MSK + DAY_S, TZ_SHIFT_MSK);
    const auto points = common::convertLocationsToPoints(getLocations());
    const auto& pointsRanges = detail::splitLocations(points, 10000);

    auto routingParams = getRoutingConfig();
    routingParams.useMrapiSchedule = true;

    auto result = getMatrixDownloadInfo(
        RequestRouter::mrapi,
        timeRangeWithTz,
        routingParams,
        pointsRanges[0],
        pointsRanges[0],
        false,
        {VehicleClass::TRUCK});

    UNIT_ASSERT(result.size() == 2);
    UNIT_ASSERT(result[0].timeRanges[0].begin == 1);
    UNIT_ASSERT(result[0].timeRanges[0].end == 2);

    UNIT_ASSERT(result[0].timeRanges[1].begin == 2);
    UNIT_ASSERT(result[0].timeRanges[1].end == 3);

    UNIT_ASSERT(result[1].timeRanges[0].begin == 3);
    UNIT_ASSERT(result[1].timeRanges[0].end == 4);
}

Y_UNIT_TEST(request_to_mrapi_contains_required_params)
{
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());

    bool parameters_is_present = false;
    auto mockHandle = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(MRAPI_REQUEST_URL) + "/v2/schedule"),
        [&parameters_is_present](const maps::http::MockRequest& request) {
            if (request.url.optParam("dtm_from") != std::nullopt &&
                request.url.optParam("dtm_to") != std::nullopt &&
                request.url.optParam("vehicle_type") != std::nullopt) {
                parameters_is_present = true;
            }
            return maps::http::MockResponse("{\"redirect\": \
                [{\"location\": \"/v2/matrix_stream/request?dtm=dtm1&vehicle_type=a&vehicle_weight=b&...\", \
                \"dtm\": [{\"from\": 1, \"to\": 2}, {\"from\": 2, \"to\": 3} ]},\
                {\"location\": \"/v2/matrix_stream/request?dtm=dtm2&vehicle_type=a&vehicle_weight=b&...\", \
                \"dtm\": [{\"from\": 3, \"to\": 4}\
                ]}]}");
        }
    );

    const TimeRangeWithTimezone timeRangeWithTz(
        MIDNIGHT_MSK, MIDNIGHT_MSK + DAY_S, TZ_SHIFT_MSK);
    const auto points = common::convertLocationsToPoints(getLocations());
    const auto& pointsRanges = detail::splitLocations(points, 10000);

    auto routingParams = getRoutingConfig();
    routingParams.useMrapiSchedule = true;

    getMatrixDownloadInfo(
        RequestRouter::mrapi,
        timeRangeWithTz,
        routingParams,
        pointsRanges[0],
        pointsRanges[0],
        false,
        {VehicleClass::TRUCK});

    UNIT_ASSERT(parameters_is_present);
}

Y_UNIT_TEST(fallback_on_invalid_result_from_mrapi)
{
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());

    auto mockHandle = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(MRAPI_REQUEST_URL) + "/v2/schedule"),
        [](const maps::http::MockRequest&) {
            maps::http::MockResponse response = maps::http::MockResponse(
                "  "
            );
            return response;
        }
    );

    const TimeRangeWithTimezone timeRangeWithTz(
        MIDNIGHT_MSK, MIDNIGHT_MSK + DAY_S, TZ_SHIFT_MSK);
    const auto points = common::convertLocationsToPoints(getLocations());
    const auto& pointsRanges = detail::splitLocations(points, 10000);

    auto routingParams = getRoutingConfig();
    routingParams.useMrapiSchedule = true;

    auto result = getMatrixDownloadInfo(
        RequestRouter::mrapi,
        timeRangeWithTz,
        routingParams,
        pointsRanges[0],
        pointsRanges[0],
        false,
        {VehicleClass::TRUCK});

    UNIT_ASSERT(result.size() > 0);
}

Y_UNIT_TEST(fallback_on_500_from_mrapi)
{
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());

    auto mockHandle = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(MRAPI_REQUEST_URL) + "/v2/schedule"),
        [](const maps::http::MockRequest&) {
            maps::http::MockResponse response = maps::http::MockResponse::withStatus(500);
            return response;
        }
    );

    const TimeRangeWithTimezone timeRangeWithTz(
        MIDNIGHT_MSK, MIDNIGHT_MSK + DAY_S, TZ_SHIFT_MSK);
    const auto points = common::convertLocationsToPoints(getLocations());
    const auto& pointsRanges = detail::splitLocations(points, 10000);

    auto routingParams = getRoutingConfig();
    routingParams.useMrapiSchedule = true;

    auto result = getMatrixDownloadInfo(
        RequestRouter::mrapi,
        timeRangeWithTz,
        routingParams,
        pointsRanges[0],
        pointsRanges[0],
        false,
        {VehicleClass::TRUCK});

    UNIT_ASSERT(result.size() > 0);
}

Y_UNIT_TEST(fallback_on_big_number_of_urls_from_mrapi)
{
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());

    auto mockHandle = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(MRAPI_REQUEST_URL) + "/v2/schedule"),
        [](const maps::http::MockRequest&) {
            std::string responseString = "{\"redirect\": [";
            for (int i = 0; i < 100; ++i) {
                responseString += "{\"location\": \"some_url\", \"dtm\": [{\"from\": 1, \"to\": 2}]}";
                if (i < 99)
                    responseString += ",";
            }
            responseString += "]}";
            return maps::http::MockResponse(responseString);
        }
    );

    const TimeRangeWithTimezone timeRangeWithTz(
        MIDNIGHT_MSK, MIDNIGHT_MSK + DAY_S, TZ_SHIFT_MSK);
    const auto points = common::convertLocationsToPoints(getLocations());
    const auto& pointsRanges = detail::splitLocations(points, 10000);

    auto routingParams = getRoutingConfig();
    routingParams.useMrapiSchedule = true;

    auto result = getMatrixDownloadInfo(
        RequestRouter::mrapi,
        timeRangeWithTz,
        routingParams,
        pointsRanges[0],
        pointsRanges[0],
        false,
        {VehicleClass::TRUCK});

    UNIT_ASSERT(result.size() == 12);
}

Y_UNIT_TEST(using_correct_urls_aws)
{
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());

    const std::string& awsDrivingMatrixRouterOverOsmUrl = "http://aws.mrapi.over.osm.ru";
    const std::string& downloadInfoResponse = "{\"redirect\": \
        [{\"location\": \"/v2/matrix_stream/request?dtm=dtm1&vehicle_type=a&vehicle_weight=b&...\", \
        \"dtm\": [{\"from\": 1, \"to\": 2}, {\"from\": 2, \"to\": 3} ]},\
        {\"location\": \"/v2/matrix_stream/request?dtm=dtm2&vehicle_type=a&vehicle_weight=b&...\", \
        \"dtm\": [{\"from\": 3, \"to\": 4}\
        ]}]}";

    bool accessMrapi = false;
    bool accessMrapiOverOsm = false;

    auto mockHandleMrapi = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(AWS_MRAPI_REQUEST_URL) + "/v2/schedule"),
        [&accessMrapi, &downloadInfoResponse](const maps::http::MockRequest&) {
            accessMrapi = true;
            return maps::http::MockResponse(downloadInfoResponse);
        }
    );

    auto mockHandleMrapiOverOsm = maps::http::addMock(
        maps::http::URL(awsDrivingMatrixRouterOverOsmUrl + "/v2/schedule"),
        [&accessMrapiOverOsm, &downloadInfoResponse](const maps::http::MockRequest&) {
            accessMrapiOverOsm = true;
            return maps::http::MockResponse(downloadInfoResponse);
        }
    );

    const TimeRangeWithTimezone timeRangeWithTz(
        MIDNIGHT_MSK, MIDNIGHT_MSK + DAY_S, TZ_SHIFT_MSK);
    const auto points = common::convertLocationsToPoints(getLocations());
    const auto& pointsRanges = detail::splitLocations(points, 10000);

    auto routingParams = getRoutingConfig();
    routingParams.useMrapiSchedule = true;
    routingParams.requestRouterCloud = common::Cloud::Aws;
    routingParams.requestRouterMode = common::RequestMode::Direct;
    routingParams.awsDrivingMatrixRouterUrl = static_cast<std::string>(AWS_MRAPI_REQUEST_URL);
    routingParams.awsDrivingMatrixRouterOverOsmUrl = awsDrivingMatrixRouterOverOsmUrl;

    const auto resultMrapi = getMatrixDownloadInfo(
        RequestRouter::mrapi,
        timeRangeWithTz,
        routingParams,
        pointsRanges[0],
        pointsRanges[0],
        false,
        {VehicleClass::TRUCK});

    const auto resultMrapiOverOsm = getMatrixDownloadInfo(
        RequestRouter::mrapiOverOsm,
        timeRangeWithTz,
        routingParams,
        pointsRanges[0],
        pointsRanges[0],
        false,
        {VehicleClass::TRUCK});

    UNIT_ASSERT(resultMrapi.begin()->downloadUrl.find(static_cast<std::string>(AWS_MRAPI_REQUEST_URL)) != std::string::npos);
    UNIT_ASSERT(resultMrapiOverOsm.begin()->downloadUrl.find(awsDrivingMatrixRouterOverOsmUrl) != std::string::npos);
    UNIT_ASSERT(accessMrapi);
    UNIT_ASSERT(accessMrapiOverOsm);
}

} //Y_UNIT_TEST_SUITE(MatrixDownloadInfo)

Y_UNIT_TEST_SUITE(MatrixCaching)
{

Y_UNIT_TEST(test_mrapi_cached_link)
{
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());

    auto mockHandle = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(MRAPI_REQUEST_URL) + "/v2/schedule"),
        [](const maps::http::MockRequest&) {
            return maps::http::MockResponse("{\"redirect\": \
                [{\"location\": \"/v2/matrix_stream/request?dtm=dtm1&vehicle_type=a&vehicle_weight=b&...\", \
                \"dtm\": [{\"from\": 1, \"to\": 2}, {\"from\": 2, \"to\": 3} ]},\
                {\"location\": \"/v2/matrix_stream/request?dtm=dtm2&vehicle_type=a&vehicle_weight=b&...\", \
                \"dtm\": [{\"from\": 3, \"to\": 4}\
                ]}]}");
        }
    );

    const TimeRangeWithTimezone timeRangeWithTz(
            MIDNIGHT_MSK, MIDNIGHT_MSK + DAY_S, TZ_SHIFT_MSK);
    const auto points = common::convertLocationsToPoints(getLocations());
    const auto pointsRanges = detail::splitLocations(points, 10000);
    const VehicleClass vehicleClass = VehicleClass::DRIVING;

    auto routingParams = getRoutingConfig();
    routingParams.useMrapiSchedule = true;

    auto matrixDownloadInfos = getMatrixDownloadInfo(
        RequestRouter::mrapi,
        timeRangeWithTz,
        routingParams,
        pointsRanges[0],
        pointsRanges[0],
        false,
        {vehicleClass});

    Postgres postgres;
    auto pool = createPool(postgres);
    MatrixCacheStorage storage(pool);

    const JobInfo& jobInfo = {
        RoutingParams(getRoutingConfig(), storage),
        vehicleClass,
        MatrixRouter::Main,
        RequestRouter::mrapi,
        pointsRanges[0],
        pointsRanges[0],
        0,
        false,
        1.0,
        [](double, const std::string&){return true;},
        matrixDownloadInfos[0]};

    (*jobInfo.routingParams.matrixCacheStorage).get().saveDownloadLink(
        {"downloadLink", maps::b2bgeo::common::RequestMode::Direct},
        *jobInfo.hash, 
        chrono::TimePoint::clock::now() + std::chrono::seconds(5000), 
        EMatrixType::Driving);
    auto link = detail::tryGetCachedMatrixLink(jobInfo, EMatrixType::Driving, 100, false);

    UNIT_ASSERT(link != std::nullopt);
}

Y_UNIT_TEST(test_no_caching_with_different_cgi_params)
{
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());

    auto mockHandle = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(MRAPI_REQUEST_URL) + "/v2/schedule"),
        [](const maps::http::MockRequest&) {
            return maps::http::MockResponse("{\"redirect\": \
                [{\"location\": \"/v2/matrix_stream/request?dtm=dtm1&vehicle_type=a&vehicle_weight=b&...\", \
                \"dtm\": [{\"from\": 1, \"to\": 2}, {\"from\": 2, \"to\": 3} ]},\
                {\"location\": \"/v2/matrix_stream/request?dtm=dtm2&vehicle_type=a&vehicle_weight=b&...\", \
                \"dtm\": [{\"from\": 3, \"to\": 4}\
                ]}]}");
        }
    );

    const TimeRangeWithTimezone timeRangeWithTz(
            MIDNIGHT_MSK, MIDNIGHT_MSK + DAY_S, TZ_SHIFT_MSK);
    const auto points = common::convertLocationsToPoints(getLocations());
    const auto pointsRanges = detail::splitLocations(points, 10000);

    auto routingParams = getRoutingConfig();
    routingParams.useMrapiSchedule = true;

    auto matrixDownloadInfosOne = getMatrixDownloadInfo(
        RequestRouter::mrapi,
        timeRangeWithTz,
        routingParams,
        pointsRanges[0],
        pointsRanges[0],
        false,
        {VehicleClass::DRIVING});

    auto matrixDownloadInfosTwo = getMatrixDownloadInfo(
        RequestRouter::mrapi,
        timeRangeWithTz,
        routingParams,
        pointsRanges[0],
        pointsRanges[0],
        false,
        {VehicleClass::TRUCK});

    Postgres postgres;
    auto pool = createPool(postgres);
    MatrixCacheStorage storage(pool);

    const JobInfo& jobInfoOne = {
        RoutingParams(getRoutingConfig(), storage),
        VehicleClass::DRIVING,
        MatrixRouter::Main,
        RequestRouter::mrapi,
        pointsRanges[0],
        pointsRanges[0],
        0,
        false,
        1.0,
        [](double, const std::string&){return true;},
        matrixDownloadInfosOne[0]};

    const JobInfo& jobInfoTwo = {
        RoutingParams(getRoutingConfig(), storage),
        VehicleClass::TRUCK,
        MatrixRouter::Main,
        RequestRouter::mrapi,
        pointsRanges[0],
        pointsRanges[0],
        0,
        false,
        1.0,
        [](double, const std::string&){return true;},
        matrixDownloadInfosTwo[0]};


    (*jobInfoOne.routingParams.matrixCacheStorage).get().saveDownloadLink(
        {"downloadLink", maps::b2bgeo::common::RequestMode::Direct}, 
        *jobInfoOne.hash, 
        chrono::TimePoint::clock::now() + std::chrono::seconds(5000), 
        EMatrixType::Driving);
    auto link = detail::tryGetCachedMatrixLink(jobInfoTwo, EMatrixType::Driving, 100, false);

    UNIT_ASSERT(link == std::nullopt);
}

Y_UNIT_TEST(test_mrapi_split_cached_link)
{
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());

    auto mockHandle = maps::http::addMock(
        maps::http::URL(static_cast<std::string>(MRAPI_REQUEST_URL) + "/v2/schedule"),
        [](const maps::http::MockRequest&) {
            return maps::http::MockResponse("{\"redirect\": \
                [{\"location\": \"/v2/matrix_stream/request?dtm=dtm1&vehicle_type=a&vehicle_weight=b&...\", \
                \"dtm\": [{\"from\": 1, \"to\": 2}, {\"from\": 2, \"to\": 3} ]},\
                {\"location\": \"/v2/matrix_stream/request?dtm=dtm2&vehicle_type=a&vehicle_weight=b&...\", \
                \"dtm\": [{\"from\": 3, \"to\": 4}\
                ]}]}");
        }
    );

    const TimeRangeWithTimezone timeRangeWithTz(
            MIDNIGHT_MSK, MIDNIGHT_MSK + DAY_S, TZ_SHIFT_MSK);
    auto points = common::convertLocationsToPoints(getLocations(598, true));
    const auto pointsRanges = detail::splitLocations(points, 300);

    Postgres postgres;
    auto pool = createPool(postgres);
    MatrixCacheStorage storage(pool);

    auto routingConfig = getRoutingConfig();
    routingConfig.useMrapiSchedule = true;
    routingConfig.costMatrixRequestDimensionLimit = 300;
    const auto routingParams = RoutingParams(routingConfig, storage);

    auto jobInfos = detail::createJobInfos(
        routingParams,
        MatrixRouter::Main,
        {VehicleClass::DRIVING},
        points,
        timeRangeWithTz,
        false,
        [](double, const std::string&){return true;}
    );

    auto matrixDownloadInfos = getMatrixDownloadInfo(
        RequestRouter::mrapi,
        timeRangeWithTz,
        routingParams,
        pointsRanges[0],
        pointsRanges[0],
        false,
        {VehicleClass::DRIVING});

    std::vector<JobInfo> jobInfosManual;

    std::unordered_set<std::string> hashes;
    for (const auto& di : matrixDownloadInfos) {
        jobInfosManual.emplace_back(routingParams,
            VehicleClass::DRIVING,
            MatrixRouter::Main,
            RequestRouter::mrapi,
            pointsRanges[0],
            pointsRanges[0],
            0,
            false,
            1.0,
            [](double, const std::string&){return true;},
            di);
        hashes.insert(jobInfosManual.back().hash.value());
    }

    UNIT_ASSERT(jobInfosManual.size() != jobInfos.begin()->second.size());
    UNIT_ASSERT(jobInfosManual[0].hash == jobInfos.begin()->second[0].hash);
    UNIT_ASSERT(jobInfosManual[1].hash != jobInfos.begin()->second[1].hash);
    UNIT_ASSERT(jobInfosManual.size() == hashes.size());
}

}

Y_UNIT_TEST_SUITE(DistanceMatrixClient)
{
Y_UNIT_TEST(test_maps_2x2_1_unique)
{
    TUnistat& stat = TUnistat::Instance();
    const auto priority = NUnistat::TPriority(10);
    registerMatrixMrapiUnistat(stat, priority);

    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());
    std::vector<Location> locations(2);

    locations[0].id = 0;
    locations[0].point.lat = 55;
    locations[0].point.lon = 37;

    locations[1].id = 1;
    locations[1].point.lat = 55;
    locations[1].point.lon = 37;

    const auto taskId = CreateGuidAsString();
    auto t = taskIdSetter(taskId);

    for (const auto& vehicleClass: mrapiClasses()) {
        for (const auto& router : {
            MatrixRouter::Main,
            MatrixRouter::Alternative,
            MatrixRouter::Global
        }) {
            const auto result = getMatrices(locations, {vehicleClass}, router);
            const auto& matrices = result.matrices.at(vehicleClass.id);
            const auto& stats = result.stats.at(vehicleClass.id);

            ASSERT_GT(stats.totalDistances, uint64_t(0));
            ASSERT_EQ(stats.geodesicDistances, 0U);
            ASSERT_EQ(stats.requestedRouter, router);
            ASSERT_EQ(stats.usedRouter, router);

            ASSERT_GT(matrices.size(), size_t(1));
            ASSERT_EQ(matrices[0].matrix->size(), 1u);

            // Start of the second interval is end of the first
            ASSERT_EQ(matrices[1].timeInterval.begin, matrices[0].timeInterval.end);
            // Intervals are not empty
            ASSERT_LT(matrices[0].timeInterval.begin, matrices[0].timeInterval.end);
            ASSERT_LT(matrices[1].timeInterval.begin, matrices[1].timeInterval.end);
        }
    }
    ASSERT_GT(TUnistat::Instance().GetSignalValueUnsafe("download_matrix_mrapi_attempts")->GetNumber(), 0);
}

Y_UNIT_TEST(test_vehicle_class_parameters_are_taken_into_account_for_truck_n3)
{
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());
    std::vector<Location> locations(2);
    const VehicleClass truckN2{
        "truck_N2",
        cost_matrices::VehicleSpecs::create<cost_matrices::RoutingMode::TRUCK>(
            cost_matrices::VehicleSpecs::Truck{3.2, 2.4, 9.0, 12.0})};
    const VehicleClass truckN3{
        "truck_N3",
        cost_matrices::VehicleSpecs::create<cost_matrices::RoutingMode::TRUCK>(
            cost_matrices::VehicleSpecs::Truck{3.4, 2.5, 20.0, 35.0})};

    // the bridge in Shimsk has max weigth restriction - 25t
    // truck_n2 can cross it, truck_n3 cannot
    locations[0].id = 0;
    locations[0].point.lat = 58.2039;
    locations[0].point.lon = 30.72;

    locations[1].id = 1;
    locations[1].point.lat = 58.209;
    locations[1].point.lon = 30.719;

    const auto taskId = CreateGuidAsString();
    const auto t = taskIdSetter(taskId);
    const auto result = getMatrices(
        locations, {truckN2, truckN3}, MatrixRouter::Main);

    const auto& matricesN2 = result.matrices.at(truckN2.id);
    const auto& matricesN3 = result.matrices.at(truckN3.id);

    ASSERT_LT(matricesN2.averageAt(MatrixId(0), MatrixId(1)).distance, 5'000);
    ASSERT_LT(matricesN2.averageAt(MatrixId(1), MatrixId(0)).distance, 5'000);
    ASSERT_GT(matricesN3.averageAt(MatrixId(0), MatrixId(1)).distance, 300'000);
    ASSERT_GT(matricesN3.averageAt(MatrixId(1), MatrixId(0)).distance, 300'000);
}

Y_UNIT_TEST(test_vehicle_class_parameters_are_taken_into_account_for_truck_n2)
{
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());
    std::vector<Location> locations(2);
    const VehicleClass truckN1{
        "truck_N1",
        cost_matrices::VehicleSpecs::create<cost_matrices::RoutingMode::TRUCK>(
            cost_matrices::VehicleSpecs::Truck{2.5, 2.2, 6.5, 3.5})};
    const VehicleClass truckN2{
        "truck_N2",
        cost_matrices::VehicleSpecs::create<cost_matrices::RoutingMode::TRUCK>(
            cost_matrices::VehicleSpecs::Truck{3.2, 2.4, 9.0, 12.0})};

    // height restriction for road under the Voronezh's stone bridge is 2.7m
    locations[0].id = 0;
    locations[0].point.lat = 51.661466;
    locations[0].point.lon = 39.209279;

    locations[1].id = 1;
    locations[1].point.lat = 51.661122;
    locations[1].point.lon = 39.207446;

    const auto taskId = CreateGuidAsString();
    const auto t = taskIdSetter(taskId);
    const auto result = getMatrices(
        locations, {truckN2, truckN1}, MatrixRouter::Main);

    const auto& matricesN1 = result.matrices.at(truckN1.id);
    const auto& matricesN2 = result.matrices.at(truckN2.id);

    ASSERT_LT(matricesN1.averageAt(MatrixId(0), MatrixId(1)).distance, 200);
    ASSERT_GT(matricesN2.averageAt(MatrixId(0), MatrixId(1)).distance, 600);
}

namespace {
std::vector<Location> getLocations(size_t size)
{
    std::vector<Location> locations;
    for (size_t i = 0; i < size; ++i) {
        Location loc;
        loc.point.lat = 55 + 0.001 * i;
        loc.point.lon = 37;
        locations.push_back(loc);
    }
    return locations;
}
} // anon namespace

Y_UNIT_TEST(test_maps_10x10)
{
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());
    const std::vector<Location> locations = getLocations(10);

    const auto taskId = CreateGuidAsString();
    auto t = taskIdSetter(taskId);

    for (const auto& vehicleClass: mrapiClasses()) {
        for (const auto& router : {MatrixRouter::Main}) {
            const auto result = getMatrices(locations, {vehicleClass}, router);

            const auto& matrices = result.matrices.at(vehicleClass.id);
            const auto& stats = result.stats.at(vehicleClass.id);

            ASSERT_GT(stats.totalDistances, uint64_t(0));
            ASSERT_EQ(stats.geodesicDistances, 0U);
            ASSERT_EQ(stats.requestedRouter, router);
            ASSERT_EQ(stats.usedRouter, router);

            ASSERT_GT(matrices.size(), size_t(1));
            ASSERT_EQ(matrices[0].matrix->size(), locations.size() * locations.size());

            // Start of the second interval is end of the first
            ASSERT_EQ(matrices[1].timeInterval.begin, matrices[0].timeInterval.end);
            // Intervals are not empty
            ASSERT_LT(matrices[0].timeInterval.begin, matrices[0].timeInterval.end);
            ASSERT_LT(matrices[1].timeInterval.begin, matrices[1].timeInterval.end);
        }
    }
}

Y_UNIT_TEST(test_transport_transit_2x2)
{
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());
    const std::vector<Location> locations = getLocations(2);

    const auto vehicleClass = VehicleClass::TRANSIT;
    const auto router = MatrixRouter::Main;

    const auto result = getMatrices(locations, {vehicleClass}, router);

    const auto& matrices = result.matrices.at(vehicleClass.id);
    const auto& stats = result.stats.at(vehicleClass.id);

    ASSERT_EQ(stats.requestedRouter, router);
    ASSERT_EQ(stats.usedRouter, router);

    ASSERT_EQ(matrices.size(), size_t(1));
    ASSERT_EQ(matrices[0].matrix->size(), locations.size() * locations.size());
    ASSERT_EQ(stats.totalDistances, locations.size() * locations.size());

    // Interval is not empty
    ASSERT_LT(matrices[0].timeInterval.begin, matrices[0].timeInterval.end);

    for (size_t i = 0; i < locations.size(); ++i) {
        for (size_t j = 0; j < locations.size(); ++j) {
            if (i == j) {
                ASSERT_EQ(matrices[0].at(MatrixId(i), MatrixId(j)).duration, 0.0);
                ASSERT_EQ(matrices[0].at(MatrixId(i), MatrixId(j)).distance, 0.0);
            } else {
                ASSERT_GT(matrices[0].at(MatrixId(i), MatrixId(j)).duration, 0.0);
                ASSERT_GT(matrices[0].at(MatrixId(i), MatrixId(j)).distance, 0.0);
            }
        }
    }
}

Y_UNIT_TEST(test_transport_sync_async)
{
    /* Download matrices 2x2 (sync) and 20x20 (async)
       and compare distance at (0, 1) - they should be equal.
     */
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());

    const auto vehicleClass = VehicleClass::TRANSIT;
    const auto router = MatrixRouter::Main;

    const std::vector<size_t> sizes = {2, 20};
    std::vector<DistanceDuration> samples;

    for (const auto size : sizes) {
        const std::vector<Location> locations = getLocations(size);

        const auto result = getMatrices(locations, {vehicleClass}, router);
        const auto& matrices = result.matrices.at(vehicleClass.id);
        const auto& stats = result.stats.at(vehicleClass.id);

        ASSERT_EQ(stats.requestedRouter, router);
        ASSERT_EQ(stats.usedRouter, router);

        ASSERT_EQ(matrices.size(), size_t(1));
        ASSERT_EQ(matrices[0].matrix->size(), locations.size() * locations.size());
        ASSERT_EQ(stats.totalDistances, locations.size() * locations.size());

        samples.push_back(matrices[0].at(MatrixId(0), MatrixId(1)));
    }

    ASSERT_EQ(samples[0].distance, samples[1].distance);
    ASSERT_EQ(samples[0].duration, samples[1].duration);
}

Y_UNIT_TEST(test_haversine_fallback_durations)
{
    Fixture fix;
    maps::b2bgeo::TvmClient::configure(fix.tvmtoolSettings());
    std::vector<Location> locations(4);

    locations[0].point.lat = 55;
    locations[0].point.lon = 37;

    locations[1].point.lat = 55.01;
    locations[1].point.lon = 37.01;

    locations[2].point.lat = 55.02;
    locations[2].point.lon = 37.02;

    locations[3].point.lat = 0;
    locations[3].point.lon = 0;

    auto computeFallbackDuration = [&](const VehicleClass& vehicleClass) {
        const auto result = getMatrices(
            locations,
            {vehicleClass},
            MatrixRouter::Geodesic);
        const auto& intervalCostMatrix = result.matrices.at(vehicleClass.id)[0];
        return intervalCostMatrix.at(MatrixId(3), MatrixId(0)).duration;
    };

    const auto fallbackDurationDriving = computeFallbackDuration(VehicleClass::DRIVING);
    const auto fallbackDurationTruck = computeFallbackDuration(VehicleClass::TRUCK);
    const auto fallbackDurationTransit = computeFallbackDuration(VehicleClass::TRANSIT);
    const auto fallbackDurationWalking = computeFallbackDuration(VehicleClass::WALKING);
    const auto fallbackDurationBicycle = computeFallbackDuration(VehicleClass::BICYCLE);

    const auto eps = 1e-7;
    UNIT_ASSERT_DOUBLES_EQUAL(fallbackDurationTruck / fallbackDurationDriving, 18. / 14., eps);
    UNIT_ASSERT_DOUBLES_EQUAL(fallbackDurationTransit / fallbackDurationDriving, 18. / 5., eps);
    UNIT_ASSERT_DOUBLES_EQUAL(fallbackDurationWalking / fallbackDurationDriving, 18. / 2., eps);
    UNIT_ASSERT_DOUBLES_EQUAL(fallbackDurationBicycle / fallbackDurationDriving, 18. / 3.5, eps);

    for (const auto& truck : DEFAULT_CONFIG.truckClasses) {
        UNIT_ASSERT_DOUBLES_EQUAL(computeFallbackDuration(truck), fallbackDurationTruck, eps);
    }
}

}

Y_UNIT_TEST_SUITE(PointsProjections)
{

Y_UNIT_TEST(points_projection_same_size)
{   
    //todo mock
    const auto points = common::convertLocationsToPoints(getLocations());
    const auto& pointsRanges = detail::splitLocations(points, 10000);

    const auto result = getProjections(pointsRanges[0]);

    ASSERT_EQ(result.size(), pointsRanges[0].size());
}

}

} // namespace maps::b2bgeo::traffic_info
