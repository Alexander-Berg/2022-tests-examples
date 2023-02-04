#include "common.h"
#include "tvm.h"

#include <maps/renderer/staticapi/yacare/tests/test_utils/mock_servers.h>
#include <maps/renderer/staticapi/yacare/tests/test_utils/test_utils.h>

#include <maps/infra/yacare/include/test_utils.h>
#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/http/include/urlencode.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/string/subst.h>

using namespace testing;

namespace maps::staticapi::tests {

namespace {

const std::string& getHeader(
    const http::MockResponse& response,
    const std::string& name)
{
    auto it = response.headers.find(name);
    if (it != response.headers.end()) {
        return it->second;
    }
    static const std::string EMPTY;
    return EMPTY;
}

http::MockResponse callApi(const std::string& path)
{
    http::MockRequest req(http::GET, "http://localhost" + path);
    return yacare::performTestRequest(req);
}

std::string escapeXml(std::string s)
{
    SubstGlobal(s, "&", "&amp;");
    SubstGlobal(s, "'", "&apos;");
    return s;
}

} // namespace

TEST(service_should, handle_valid_requests)
{
    auto srv = makeAllMockServers();

    constexpr std::array DATA{
        // Moscow region - drawing 'map' layer
        "/1.x/?l=map&ll=37.6,55.6&spn=0.002,0.002",
        "/maps-api-static/1.x/?l=map&ll=37.6,55.6&spn=0.002,0.002",

        // Moscow, 'pmap' and 'pskl' are replaced with 'map' and 'skl' correspondingly
        "/1.x/?l=pmap,pskl&ll=37.6,55.6&spn=0.002,0.002",

        // Max allowed layers number is 4
        "/1.x/?l=map,skl,trf,trfe&ll=30.3,60&z=9",

        // Upscaled 'map'
        "/1.x/?l=map&ll=37.6,55.6&spn=0.002,0.002&scale=2.0",
        "/1.x/?l=map,skl,trf,trfe&ll=30.3,60&z=9&scale=3",

        // Upscaled 'map' with 'trf' placed above
        "/1.x/?l=map,trf&ll=37.6,55.6&spn=0.002,0.002&scale=1.5",

        // Valid signature
        "/1.x/?ll=30.3,60&z=9&l=map&api_key=client&signature=YwSB7_kT9mPfmTeLr3FEdEy3eMGNz_LGWIAKbYpfZ0s=",
        "/1.x/?ll=30.3,60&z=9&l=map&api_key=client&signature=YwSB7_kT9mPfmTeLr3FEdEy3eMGNz_LGWIAKbYpfZ0s%3D",
        "/1.x/?ll=30.3%2C60&z=9&l=map&api_key=client&signature=91xm9B6tGsv3qppBH4YWDuSKRaWeKwwkcQCDmJgTspU=",
        "/1.x/?ll=30.3,60&z=9&l=map&api_key=test&signature=brda4iD7Etjb6UAqGdfe0fxU1eL3MUZnfqyig-56DOE=",
        "/1.x/?ll=30.3,60&z=9&l=map&api_key=test", // unsigned usage is allowed for api_key dev

        // Dark|light theme with valid signature
        "/1.x/?ll=30.3,60&z=9&l=map&theme=dark&api_key=test&signature=NnmFV4mf4oJVCaNGuDgVS6h3pUl83KQCZm7bYry0bU0==",
        "/maps-api-static/1.x/?ll=30.3,60&z=9&l=map&theme=light&api_key=test&signature=ZIHGZPWZ8qSpo2bkj3FyGKDBg4BYb_N2M-fJjM59AYo=",

        // Style with valid signature
        "/1.x/?ll=30.3,60&z=9&l=map&style=elements:label%7Cstylers.visibility:off&api_key=test&signature=UmQi2VcyeHz-M04Tl4GbUAfdVzd4JwiG3TtDnTwNzoE=",

        // Temporary permit using style parameter for several clients with legacy keys
        "/1.x/?ll=30.3,60&z=9&l=map&style=elements:label%7Cstylers.visibility:off"
            "&key=AD8aZFwBAAAAifxyUwMAHsUYtvli_FZzqc4ICuNNo30qaz8AAAAAAAAAAAD4wqOH_fCmtrVRqXMbXOvS-_7k-Q%3D%3D", // local.yandex.ru (closed)
        "/1.x/?ll=30.3,60&z=9&l=map&style=elements:label%7Cstylers.visibility:off"
            "&key=APleUl8BAAAA7APlSAMAlavYTclJZmUrc8aYYnECoawplLYAAAAAAAAAAABkYTVtcjX5wisZWB7tZrsX7r_jiA==", // TURBO-3097

        // A legacy key
        "/1.x/?ll=30.3,60&z=9&l=map&key=some_legacy_key",
    };

    for (std::string path : DATA) {
        auto result = callApi(path);
        EXPECT_EQ(result.status, 200) << path << "\n" << result.body;
    }
}

TEST(service_should, return_content_type)
{
    auto srv = makeAllMockServers();

    const std::vector<std::pair<std::string, std::string>> DATA{
        {"/1.x/?l=map&ll=37.6,55.6&spn=0.002,0.002", "image/png"},
        {"/1.x/?l=map,trf&ll=37.6,55.6&spn=0.002,0.002", "image/png"},
        {"/1.x/?l=sat&ll=37.6,55.6&spn=0.002,0.002", "image/jpeg"},
        {"/1.x/?l=map,sat&ll=37.6,55.6&spn=0.002,0.002", "image/jpeg"},
        {"/1.x/?l=sat,skl&ll=37.6,55.6&spn=0.002,0.002", "image/jpeg"},
    };

    for (const auto& [path, contentType] : DATA) {
        auto result = callApi(path);
        EXPECT_EQ(result.status, 200) << path << "\n" << result.body;
        EXPECT_EQ(getHeader(result, "Content-Type"), contentType) << path;
    }
}

TEST(service_should, resolve_url_templates)
{
    const std::string KEYSERV_URL_EMPTY_KEY = KEYSERV_URL + "&keys=&uri=";
    const std::string META_LL_3060 = "&ll=30.299999999999997158,59.999999999990976107";
    const std::string META_LL_3755 = "&ll=37.600000000000001421,55.599999999993904964";

    std::vector<std::pair<std::string, std::vector<std::string>>> DATA{
        {"/1.x/?ll=30.3,60&z=9&size=200,200&l=map",
            {
                KEYSERV_URL_EMPTY_KEY,
                META_URL + "&lang=ru_RU&l=map",
                META_URL + "&l=map" + META_LL_3060 + "&z=9&lang=ru_RU",
                MAP_TILES_URL + "&x=298&y=148&z=9&lang=ru_RU&scale=1",
                MAP_TILES_URL + "&x=298&y=149&z=9&lang=ru_RU&scale=1",
                MAP_TILES_URL + "&x=299&y=148&z=9&lang=ru_RU&scale=1",
                MAP_TILES_URL + "&x=299&y=149&z=9&lang=ru_RU&scale=1",
            }},
        {"/1.x/?ll=37.6,55.6&spn=0.002,0.002&l=map",
            {
                KEYSERV_URL_EMPTY_KEY,
                META_URL + "&lang=ru_RU&l=map",
                META_URL + "&l=map" + META_LL_3755 + "&lang=ru_RU",
                META_URL + "&l=map" + META_LL_3755 + "&z=17&lang=ru_RU",
                MAP_TILES_URL + "&x=79224&y=41188&z=17&lang=ru_RU&scale=1",
                MAP_TILES_URL + "&x=79224&y=41189&z=17&lang=ru_RU&scale=1",
                MAP_TILES_URL + "&x=79224&y=41190&z=17&lang=ru_RU&scale=1",
                MAP_TILES_URL + "&x=79225&y=41188&z=17&lang=ru_RU&scale=1",
                MAP_TILES_URL + "&x=79225&y=41189&z=17&lang=ru_RU&scale=1",
                MAP_TILES_URL + "&x=79225&y=41190&z=17&lang=ru_RU&scale=1",
                MAP_TILES_URL + "&x=79226&y=41188&z=17&lang=ru_RU&scale=1",
                MAP_TILES_URL + "&x=79226&y=41189&z=17&lang=ru_RU&scale=1",
                MAP_TILES_URL + "&x=79226&y=41190&z=17&lang=ru_RU&scale=1",
            }},
        {"/1.x/?ll=30.3,60&z=9&size=200,200&l=map&lang=en_US&scale=2",
            {
                KEYSERV_URL_EMPTY_KEY,
                META_URL + "&lang=en_US&l=map",
                META_URL + "&l=map" + META_LL_3060 + "&z=9&lang=en_US",
                MAP_TILES_URL + "&x=298&y=148&z=9&lang=en_US&scale=2",
                MAP_TILES_URL + "&x=298&y=149&z=9&lang=en_US&scale=2",
                MAP_TILES_URL + "&x=299&y=148&z=9&lang=en_US&scale=2",
                MAP_TILES_URL + "&x=299&y=149&z=9&lang=en_US&scale=2",
            }},
        {"/1.x/?ll=30.3,60&z=11&size=100,100&l=sat,trf,trfe,skl",
            {
                KEYSERV_URL_EMPTY_KEY,
                META_URL + "&lang=ru_RU&l=sat,skl",
                META_URL + "&l=sat" + META_LL_3060 + "&z=11&lang=ru_RU",
                TRF_VERSION_URL,
                TRFE_VERSION_URL,
                SAT_TILES_URL + "&x=1196&y=596&z=11&lang=ru_RU",
                TRF_TILES_URL + "&x=1196&y=596&z=11&lang=ru_RU&scale=1",
                TRFE_TILES_URL + "&x=1196&y=596&z=11&lang=ru_RU&scale=1",
                SKL_TILES_URL + "&x=1196&y=596&z=11&lang=ru_RU&scale=1",
            }},
        {"/1.x/?ll=30.3,60&z=9&size=200,200&l=skl,map,trf,map&style=,tags:t1,tags:t2,tags:t3&api_key=test",
            {
                META_URL + "&lang=ru_RU&l=skl,map,map",
                META_URL + "&l=skl" + META_LL_3060 + "&z=9&lang=ru_RU",
                TRF_VERSION_URL,
                SKL_TILES_URL + "&x=298&y=148&z=9&lang=ru_RU&scale=1",
                SKL_TILES_URL + "&x=298&y=149&z=9&lang=ru_RU&scale=1",
                SKL_TILES_URL + "&x=299&y=148&z=9&lang=ru_RU&scale=1",
                SKL_TILES_URL + "&x=299&y=149&z=9&lang=ru_RU&scale=1",
                MAP_TILES_URL + "&x=298&y=148&z=9&lang=ru_RU&scale=1&style=tags%3At1",
                MAP_TILES_URL + "&x=298&y=149&z=9&lang=ru_RU&scale=1&style=tags%3At1",
                MAP_TILES_URL + "&x=299&y=148&z=9&lang=ru_RU&scale=1&style=tags%3At1",
                MAP_TILES_URL + "&x=299&y=149&z=9&lang=ru_RU&scale=1&style=tags%3At1",
                TRF_TILES_URL + "&x=298&y=148&z=9&lang=ru_RU&scale=1",
                TRF_TILES_URL + "&x=298&y=149&z=9&lang=ru_RU&scale=1",
                TRF_TILES_URL + "&x=299&y=148&z=9&lang=ru_RU&scale=1",
                TRF_TILES_URL + "&x=299&y=149&z=9&lang=ru_RU&scale=1",
                MAP_TILES_URL + "&x=298&y=148&z=9&lang=ru_RU&scale=1&style=tags%3At3",
                MAP_TILES_URL + "&x=298&y=149&z=9&lang=ru_RU&scale=1&style=tags%3At3",
                MAP_TILES_URL + "&x=299&y=148&z=9&lang=ru_RU&scale=1&style=tags%3At3",
                MAP_TILES_URL + "&x=299&y=149&z=9&lang=ru_RU&scale=1&style=tags%3At3",
            }},
        {"/1.x/?ll=30.3,60&z=9&size=50,50&l=map&style=tags.all%253Atag1%252Ctag2&api_key=test",
            {
                META_URL + "&lang=ru_RU&l=map",
                META_URL + "&l=map" + META_LL_3060 + "&z=9&lang=ru_RU",
                MAP_TILES_URL + "&x=298&y=149&z=9&lang=ru_RU&scale=1&style=tags.all%3Atag1%2Ctag2",
                MAP_TILES_URL + "&x=299&y=149&z=9&lang=ru_RU&scale=1&style=tags.all%3Atag1%2Ctag2",
            }},
        {"/1.x/?ll=30.3,60&z=9&size=50,50&l=map," + http::urlEncode(http::urlEncode(
            CUSTOM_TILES_URL + "?z=%z&x=%x&y=%y&lang=%lang&l=%l&v=%v&style=%style&data=1,2,3")),
            {
                KEYSERV_URL_EMPTY_KEY,
                META_URL + "&lang=ru_RU&l=map",
                META_URL + "&l=map" + META_LL_3060 + "&z=9&lang=ru_RU",
                MAP_TILES_URL + "&x=298&y=149&z=9&lang=ru_RU&scale=1",
                MAP_TILES_URL + "&x=299&y=149&z=9&lang=ru_RU&scale=1",
                CUSTOM_TILES_URL + "?z=9&x=298&y=149&lang=%25lang&l=%25l&v=%25v&style=%25style&data=1,2,3",
                CUSTOM_TILES_URL + "?z=9&x=299&y=149&lang=%25lang&l=%25l&v=%25v&style=%25style&data=1,2,3",
            }},
        {"/1.x/?ll=30.3,60&z=9&size=100,100&l=map," +
            http::urlEncode(http::urlEncode(CUSTOM_TILES_URL + "/scale2?x=%x&y=%y&style=%style&scale=%scale")) +
            ",map&scale=2&style=tags:tag1,,tags:tag2&api_key=test",
            {
                META_URL + "&lang=ru_RU&l=map,map",
                META_URL + "&l=map" + META_LL_3060 + "&z=9&lang=ru_RU",
                MAP_TILES_URL + "&x=298&y=149&z=9&lang=ru_RU&scale=2&style=tags%3Atag1",
                MAP_TILES_URL + "&x=299&y=149&z=9&lang=ru_RU&scale=2&style=tags%3Atag1",
                CUSTOM_TILES_URL + "/scale2?x=298&y=149&style=%25style&scale=%25scale",
                CUSTOM_TILES_URL + "/scale2?x=299&y=149&style=%25style&scale=%25scale",
                MAP_TILES_URL + "&x=298&y=149&z=9&lang=ru_RU&scale=2&style=tags%3Atag2",
                MAP_TILES_URL + "&x=299&y=149&z=9&lang=ru_RU&scale=2&style=tags%3Atag2",
            }},
        {"/1.x/?ll=30.3,60&z=9&size=50,50&l=" + CUSTOM_TILES_URL + "?%2525z/%2525x/%2525y",
            {
                KEYSERV_URL_EMPTY_KEY,
                CUSTOM_TILES_URL + "?9/298/149",
                CUSTOM_TILES_URL + "?9/299/149",
            }},
        {"/1.x/?ll=30.3,60&z=9&size=200,200&l=map&theme=dark&api_key=test",
            {
                META_URL + "&lang=ru_RU&l=map",
                META_URL + "&l=map" + META_LL_3060 + "&z=9&lang=ru_RU",
                MAP_TILES_URL + "&x=298&y=148&z=9&lang=ru_RU&scale=1&theme=dark",
                MAP_TILES_URL + "&x=298&y=149&z=9&lang=ru_RU&scale=1&theme=dark",
                MAP_TILES_URL + "&x=299&y=148&z=9&lang=ru_RU&scale=1&theme=dark",
                MAP_TILES_URL + "&x=299&y=149&z=9&lang=ru_RU&scale=1&theme=dark",
            }},
        {"/1.x/?ll=30.3,60&z=9&size=200,200&l=map&theme=light&api_key=test",
            {
                META_URL + "&lang=ru_RU&l=map",
                META_URL + "&l=map" + META_LL_3060 + "&z=9&lang=ru_RU",
                MAP_TILES_URL + "&x=298&y=148&z=9&lang=ru_RU&scale=1&theme=light",
                MAP_TILES_URL + "&x=298&y=149&z=9&lang=ru_RU&scale=1&theme=light",
                MAP_TILES_URL + "&x=299&y=148&z=9&lang=ru_RU&scale=1&theme=light",
                MAP_TILES_URL + "&x=299&y=149&z=9&lang=ru_RU&scale=1&theme=light",
            }},
        {"/1.x/?ll=30.3,60&z=9&size=200,200&l=map&data_provider=osm"
         "&api_key=66e592f8-5b03-11eb-ae93-0242ac130002&signature=Xa2VWjnzs1mw7QR5AgvNqCemMgJ6thHLZpD3uY2DJYs=",
            {
                MAP_OSM_VERSION_URL,
                MAP_OSM_TILES_URL + "&x=298&y=148&z=9&lang=ru_RU&scale=1",
                MAP_OSM_TILES_URL + "&x=298&y=149&z=9&lang=ru_RU&scale=1",
                MAP_OSM_TILES_URL + "&x=299&y=148&z=9&lang=ru_RU&scale=1",
                MAP_OSM_TILES_URL + "&x=299&y=149&z=9&lang=ru_RU&scale=1",
            }},
    };

    std::vector<std::string> urls;
    auto srv = makeAllMockServers(&urls);

    auto customLayer = http::addMock(
        CUSTOM_TILES_URL,
        [&urls](const http::MockRequest& request) {
            urls.push_back(request.url.toString());
            return http::MockResponse(renderWaterTile(1.0));
        });

    auto customLayerScale2 = http::addMock(
        CUSTOM_TILES_URL + "/scale2",
        [&urls](const http::MockRequest& request) {
            urls.push_back(request.url.toString());
            return http::MockResponse(renderWaterTile(2.0));
        });

    for (const auto& [path, expectedUrls] : DATA) {
        urls.clear();
        auto result = callApi(path);
        EXPECT_EQ(result.status, 200) << path << "\n" << result.body;
        EXPECT_THAT(urls, UnorderedElementsAreArray(expectedUrls)) << path;
    }
}

TEST(service_should, return_404_if_all_tiles_not_found)
{
    auto srv = makeAllMockServers();

    // The max available zoom is 23, but we don't have tiles for that zoom
    EXPECT_EQ(callApi("/1.x/?ll=37.6,55.6&z=23&l=map").status, 404);
    EXPECT_EQ(callApi("/1.x/?ll=37.6,55.6&z=23&l=skl").status, 404);

    // If some tiles Not Found and the other tiles is OK, the result is OK
    EXPECT_EQ(callApi("/1.x/?ll=37.6,55.6&z=20&l=sat,skl").status, 200);
    EXPECT_EQ(callApi("/1.x/?ll=37.6,55.6&z=20&l=sat").status, 404);

    // TODO
    // International Date Line isn't covered by meta - expecting answer to be 404 if zoom WAS NOT specified
    // 404 /1.x/?ll=180,65&l=map
}

TEST(service_should, validate_loaded_tiles_status)
{
    auto meta = makeMetaMockServer();
    auto keyserv = makeKeyservMockServer();

    enum class Status { All200, One400, One401, All401, One404, All404, One429, One500, All500};
    Status mapStatus = Status::All200;
    Status sklStatus = Status::All200;

    auto map = http::addMock(
        MAP_TILES_URL,
        [&](const http::MockRequest& request) {
            auto& status = (request.url.params().find("l=skl") != std::string::npos)
                ? sklStatus : mapStatus;
            if (status == Status::One400) {
                status = Status::All200;
                return http::MockResponse::withStatus(400);
            }
            if (status == Status::One401) {
                status = Status::All200;
                return http::MockResponse::withStatus(401);
            }
            if (status == Status::All401) {
                return http::MockResponse::withStatus(401);
            }
            if (status == Status::One404) {
                status = Status::All200;
                return http::MockResponse::withStatus(404);
            }
            if (status == Status::One429) {
                status = Status::All200;
                return http::MockResponse::withStatus(429);
            }
            if (status == Status::All404) {
                return http::MockResponse::withStatus(404);
            }
            if (status == Status::One500) {
                status = Status::All200;
                return http::MockResponse::withStatus(500);
            }
            if (status == Status::All500) {
                return http::MockResponse::withStatus(500);
            }
            return http::MockResponse(renderWaterTile(1.0));
        });

    const std::string MAP = "/1.x/?ll=30.3,60&z=9&l=map";
    const std::string MAP_SKL = MAP + ",skl";

    mapStatus = Status::All200;
    EXPECT_EQ(callApi(MAP).status, 200);

    mapStatus = Status::One400;
    EXPECT_EQ(callApi(MAP).status, 500);

    mapStatus = Status::One401;
    EXPECT_EQ(callApi(MAP).status, 401);

    mapStatus = Status::All401;
    EXPECT_EQ(callApi(MAP).status, 401);

    mapStatus = Status::One404;
    EXPECT_EQ(callApi(MAP).status, 200);

    mapStatus = Status::All404;
    EXPECT_EQ(callApi(MAP).status, 404);

    mapStatus = Status::One500; // will be retried
    EXPECT_EQ(callApi(MAP).status, 200);

    mapStatus = Status::All500;
    EXPECT_EQ(callApi(MAP).status, 500);

    mapStatus = Status::All200;
    sklStatus = Status::All200;
    EXPECT_EQ(callApi(MAP_SKL).status, 200);

    mapStatus = Status::All401;
    sklStatus = Status::All200;
    EXPECT_EQ(callApi(MAP_SKL).status, 401);

    mapStatus = Status::All401;
    sklStatus = Status::One400;
    EXPECT_EQ(callApi(MAP_SKL).status, 500);

    mapStatus = Status::One401;
    sklStatus = Status::One500; // will be retried
    EXPECT_EQ(callApi(MAP_SKL).status, 401);

    mapStatus = Status::One401;
    sklStatus = Status::All500;
    EXPECT_EQ(callApi(MAP_SKL).status, 500);

    mapStatus = Status::One429;
    sklStatus = Status::All200;
    EXPECT_EQ(callApi(MAP_SKL).status, 429);
}

TEST(service_should, validate_loaded_tiles_content_length)
{
    const std::string BIG_DATA =
        renderWaterTile(1.0)
        .append(2 * MAX_CUSTOM_LAYER_CONTENT_LENGTH, '\0');

    auto meta = makeMetaMockServer();
    auto keyserv = makeKeyservMockServer();

    auto map = http::addMock(
        MAP_TILES_URL,
        [&](const http::MockRequest&) {
            return http::MockResponse(BIG_DATA);
        });

    auto sat = http::addMock(
        SAT_TILES_URL,
        [&](const http::MockRequest&) {
            return makeChunkedResponse(BIG_DATA, 2000);
        });

    auto customLayer = http::addMock(
        CUSTOM_TILES_URL,
        [&](const http::MockRequest& request) {
            http::MockResponse response(BIG_DATA);
            const std::string SIZE_PARAM = "test_size=";
            size_t pos = request.url.params().find(SIZE_PARAM);
            if (pos != std::string::npos) {
                pos += SIZE_PARAM.size();
                size_t end = request.url.params().find("&", pos);
                std::string size = request.url.params().substr(pos, end);
                response.body.resize(std::stoull(size));
            }
            if (request.url.params().find("test_chunked") != std::string::npos) {
                return makeChunkedResponse(response.body, 2000);
            }
            return response;
        });

    EXPECT_EQ(callApi("/1.x/?ll=30.3,60&z=9&l=map").status, 200);
    EXPECT_EQ(callApi("/1.x/?ll=30.3,60&z=9&l=sat").status, 200);

    auto callCustomLayerApi = [](const std::string& url) {
        std::string encodedUrl = http::urlEncode(http::urlEncode(url));
        return callApi("/1.x/?ll=30.3,60&z=9&l=map," + encodedUrl);
    };

    std::string url = CUSTOM_TILES_URL + "?x=%x&y=%y&z=%z";
    EXPECT_EQ(callCustomLayerApi(url + "&test_size=3000").status, 200);
    EXPECT_EQ(callCustomLayerApi(url + "&test_size=" + std::to_string(MAX_CUSTOM_LAYER_CONTENT_LENGTH)).status, 200);
    EXPECT_EQ(callCustomLayerApi(url + "&test_size=" + std::to_string(MAX_CUSTOM_LAYER_CONTENT_LENGTH + 1)).status, 400);
    EXPECT_EQ(callCustomLayerApi(url + "&test_size=" + std::to_string(MAX_CUSTOM_LAYER_CONTENT_LENGTH * 2)).status, 400);

    EXPECT_EQ(callCustomLayerApi(url + "&test_chunked=1&test_size=3000").status, 200);
    EXPECT_EQ(callCustomLayerApi(url + "&test_chunked=1&test_size=" + std::to_string(MAX_CUSTOM_LAYER_CONTENT_LENGTH)).status, 200);
    EXPECT_EQ(callCustomLayerApi(url + "&test_chunked=1&test_size=" + std::to_string(MAX_CUSTOM_LAYER_CONTENT_LENGTH + 1)).status, 400);
    EXPECT_EQ(callCustomLayerApi(url + "&test_chunked=1&test_size=" + std::to_string(MAX_CUSTOM_LAYER_CONTENT_LENGTH * 2)).status, 400);

    std::string error =
        "Invalid custom layer url template: "
        "The response to '" + CUSTOM_TILES_URL + "?x=297&y=148&z=9&test_size=" +
        std::to_string(MAX_CUSTOM_LAYER_CONTENT_LENGTH + 1) +
        "' is too large";
    EXPECT_THAT(
        callCustomLayerApi(
            url + "&test_size=" + std::to_string(MAX_CUSTOM_LAYER_CONTENT_LENGTH + 1)).body,
        HasSubstr(escapeXml(error)));
}

TEST(service_should, send_tvm_header)
{
    auto meta = makeMetaMockServer();
    auto keyserv = makeKeyservMockServer();

    auto mapTvmTicket = tvmTicketFor("maps_core_renderer_cache");
    auto trfTvmTicket = tvmTicketFor("maps_core_jams_rdr_cache");
    auto customLayerTvmTicket = tvmTicketFor("weather");
    EXPECT_FALSE(mapTvmTicket.empty());
    EXPECT_FALSE(trfTvmTicket.empty());
    EXPECT_FALSE(customLayerTvmTicket.empty());
    EXPECT_NE(mapTvmTicket, trfTvmTicket);
    EXPECT_NE(mapTvmTicket, customLayerTvmTicket);

    {
        auto map = http::addMock(
            MAP_TILES_URL,
            [&](const http::MockRequest& request) {
                EXPECT_TRUE(request.headers.contains(auth::SERVICE_TICKET_HEADER));
                EXPECT_EQ(request.header(auth::SERVICE_TICKET_HEADER), mapTvmTicket);
                return renderWaterTile(1.0);
            });
        EXPECT_EQ(callApi("/1.x/?ll=30.3,60&z=9&l=map").status, 200);
    }
    {
        auto trfTiles = http::addMock(
            TRF_TILES_URL,
            [&](const http::MockRequest& request) {
                EXPECT_TRUE(request.headers.contains(auth::SERVICE_TICKET_HEADER));
                EXPECT_EQ(request.header(auth::SERVICE_TICKET_HEADER), trfTvmTicket);
                return renderWaterTile(1.0);
            });
        auto trfVersion = http::addMock(
            TRF_VERSION_URL,
            [&](const http::MockRequest& request) {
                EXPECT_TRUE(request.headers.contains(auth::SERVICE_TICKET_HEADER));
                EXPECT_EQ(request.header(auth::SERVICE_TICKET_HEADER), trfTvmTicket);
                return http::MockResponse::fromFile(testDataPath() + "trf_version.xml");
            });
        EXPECT_EQ(callApi("/1.x/?ll=30.3,60&z=9&l=trf").status, 200);
    }
    {
        auto customTiles = http::addMock(
            CUSTOM_TILES_URL,
            [&](const http::MockRequest& request) {
                EXPECT_TRUE(request.headers.contains(auth::SERVICE_TICKET_HEADER));
                EXPECT_EQ(request.header(auth::SERVICE_TICKET_HEADER), customLayerTvmTicket);
                return renderWaterTile(1.0);
            });
        EXPECT_EQ(callApi("/1.x/?ll=30.3,60&z=9&l=" + CUSTOM_TILES_URL).status, 200);
    }
}

} // namespace maps::staticapi::tests
