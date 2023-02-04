#include <maps/renderer/staticapi/yacare/tests/test_utils/mock_servers.h>

#include <maps/infra/yacare/include/test_utils.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/string/subst.h>

using namespace testing;

namespace maps::staticapi::tests {

namespace {

struct BadRequestTest {
    std::string path;
    std::string error;
    http::Status status = http::Status::BadRequest;
};

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

TEST(bad_requests, validation)
{
    auto srv = makeAllMockServers();

    const std::vector<BadRequestTest> DATA{
        {"/1.x/?l=map&ll=37.6,55.6&spn=0.002,0.002&scale=0.5", "scale should be within 1 and 4"},
        {"/1.x/?l=map&ll=37.6,55.6&spn=0.002,0.002&scale=5.0", "scale should be within 1 and 4"},

        {"/1.x/?l=map&ll=37.6,55.6&spn=0.002,0.002&lg=0", "Logo disabling isn't allowed"},
        {"/1.x/?l=map&ll=37.6,55.6&spn=0.002,0.002&cr=0", "Copyright disabling isn't allowed"},

        {"/1.x/?l=sat&ll=37.6,55.6&spn=0.002,0.002&scale=2.0", "Layer 'sat' can't be scaled"},

        {"/1.x/?ll=37.6,55.6&z=24&l=map", "zoom should be within 0 and 23"},
        {"/1.x/?ll=37.6,55.6&z=99&l=map", "zoom should be within 0 and 23"},
        {"/1.x/?ll=37.6,55.6&z=-1&l=map", "zoom should be within 0 and 23"},
        {"/1.x/?ll=37.6,55.6&z=&l=map", "z parameter malformed"},
        {"/1.x/?ll=37.6,55.6&z=abc&l=map", "z parameter malformed"},

        {"/1.x/?ll=37.6,55.6&spn=0.002,0.002&l=map,sta", "Layer 'sta' isn't allowed for rendering"},
        {"/1.x/?ll=37.6,55.6&spn=0.002,0.002&l=sta,stv", "Layer 'sta' isn't allowed for rendering"},

        {"/1.x/?ll=37.6,55.6&spn=0.002,0.002&l=map&lang=", "lang parameter malformed"},
        {"/1.x/?l=map&ll=37.6,55.6&spn=0.002,0.002&lang=lang", "lang parameter malformed"},

        // Some requests intersecting International Date Line can't be parsed properly
        // (we are unable to compute BoundingBox properly)
        // Expecting 400 to be returned
        {"/1.x/?ll=180,90&size=337,120&z=14&l=map", "Request area is too large"},

        {"/1.x/?ll=37.6,55.6&z=17&scale=3&size=990,390&pt=30.6,6000&l=map",
            "Wrong geometry: some coordinates are out-bound", http::Status::BadRequest},
        {"/1.x/?ll=37.6,55.6&z=17&size=990,390&pl=37.65,55.74,37.65,6000&l=map",
            "Wrong geometry: some coordinates are out-bound", http::Status::BadRequest},

        {"/1.x/", "l parameter missing or mismatched"},
        {"/1.x/?ll=30.3,60&z=9", "l parameter missing or mismatched"},
        {"/1.x/?ll=30.3,60&z=9&l=map,map,map,map,map",
            "Layer list size (5) should be no greater than 4"},

        {"/1.x/?ll=30.3,60&z=9&l=http://custom", "Layer 'http://custom' is not allowed for rendering"},
        {"/1.x/?ll=30.3,60&z=9&l=%", "Invalid urlencoded string '%'"},
        {"/1.x/?ll=30.3,60&z=9&l=%uuuu", "Invalid urlencoded string '%uuuu'"},

        {"/1.x/?ll=30.3,60&z=9&l=map&style=elements:label%7Cstylers.visibility:off",
            "api_key and signature parameters required to use style", http::Status::Forbidden},
        {"/1.x/?ll=30.3,60&z=9&l=map&style=bad_style&api_key=test",
            "Invalid style argument: Invalid property 'bad_style'. Expected format 'name:value'"},
        {"/1.x/?ll=30.3,60&z=9&l=map&style=tags:t1,tags:t2&api_key=test",
            "Style list size (2) must be equal to layer list size (1)"},
        {"/1.x/?ll=30.3,60&z=9&l=map,trf,map&style=tags:t1&api_key=test",
            "Style list size (1) must be equal to layer list size (3)"},
        {"/1.x/?ll=30.3,60&z=9&l=map&style=tags.all%3Atag1%2Ctag2&api_key=test",
            "Style list size (2) must be equal to layer list size (1)"},
        {"/1.x/?ll=30.3,60&z=9&l=map&style=%style&api_key=test",
            "Invalid urlencoded string '%style'"},
        {"/1.x/?ll=30.3,60&z=9&l=" + CUSTOM_TILES_URL + "&style=tags:t&api_key=test",
            "Custom layer style should be empty"},

        {"/1.x/?ll=30.3,60&z=9&l=map&api_key=unknown",
            "Invalid api_key parameter", http::Status::Forbidden},
        {"/1.x/?ll=30.3,60&z=9&l=map&api_key=client",
            "signature parameter required", http::Status::Forbidden},
        {"/1.x/?ll=30.3,60&z=9&l=map&api_key=client&signature=***",
            "invalid signature", http::Status::Forbidden},
        {"/1.x/?ll=30.3,60&z=9&l=map&api_key=test&signature=XwqW2e5SswwTNw4qgQeCVNTWkg7n9PWdaxSROVlre3o=",
            "invalid signature for URL /1.x/?ll=30.3,60&z=9&l=map&api_key=test", http::Status::Forbidden},
        {"/1.x/?ll=30.3,60&z=9&l=map&signature=brda4iD7Etjb6UAqGdfe0fxU1eL3MUZnfqyig-56DOE=&api_key=test",
            "signature must be the last parameter", http::Status::BadRequest},
        {"/1.x/?ll=30.3,60&z=9&l=map&signature=kI-kKwdDSr2Pgy32lvY8jxM73Hh537XkzAXXK-HiXJ4=",
            "api_key parameter required", http::Status::Forbidden},
        {"/1.x/?ll=30.3,60&z=9&l=map&key=dev&signature=dCdnxivzNoLJWZ63M_ch1AJQEVgmO-4427YdHhFjehc=",
            "api_key parameter required", http::Status::Forbidden},
        {"/1.x/?ll=30.3,60&z=9&l=map&key=dev&api_key=test&signature=-quIw9mlzqTartWmjF0MO-CDZjeRNl3SyAM0pdydLqg=",
            "key parameter is deprecated", http::Status::BadRequest},

        {"/1.x/?ll=30.3,60&z=9&l=map&theme=invalid&api_key=test",
            "Invalid theme parameter value. Expected one of 'light', 'dark'"},
        {"/1.x/?ll=30.3,60&z=9&l=map&theme=light",
            "api_key and signature parameters required to use theme", http::Status::Forbidden},

        // data_provider=osm
        {"/1.x/?ll=30.3,60&z=9&l=map&data_provider=osm",
            "api_key and signature parameters required to use data_provider", http::Status::Forbidden},
        {"/1.x/?ll=30.3,60&z=9&l=map&data_provider=osm&api_key=test",
            "api_key and signature parameters required to use data_provider", http::Status::Forbidden},
        {"/1.x/?ll=30.3,60&z=9&l=map&data_provider=osm&api_key=test&signature=yS4T5ROXzPyYZYNYp1iGjvIINcYyE5rfxWf4mjZKBTs=",
            "data_provider is not allowed", http::Status::Forbidden},
        {"/1.x/?ll=30.3,60&z=9&l=map&data_provider=osm&api_key=test&signature=eskIISz0icS5K6m6UFNbzJ217uQMG6PRdKz90oLXUuU=",
            "invalid signature", http::Status::Forbidden},
        {"/1.x/?ll=30.3,60&z=9&l=map&data_provider=unknown&api_key=66e592f8-5b03-11eb-ae93-0242ac130002&signature=eskIISz0icS5K6m6UFNbzJ217uQMG6PRdKz90oLXUuU=",
            "Data provider 'unknown' is not supported for layer 'map'"},
        {"/1.x/?ll=30.3,60&z=9&l=map,trfe&data_provider=osm&api_key=66e592f8-5b03-11eb-ae93-0242ac130002&signature=jHlMvd3monGRgzRA3Rp_kMhM6Y8uWjryO7ndXqXcKaw=",
            "Data provider 'osm' is not supported for layer 'trfe'"},
    };

    for (const auto& data : DATA) {
        auto result = callApi(data.path);
        EXPECT_EQ(http::Status(result.status), data.status) << data.path;

        EXPECT_THAT(result.body, HasSubstr(escapeXml(data.error))) << data.path;

        std::string statusXml = "<status>" + std::to_string(data.status.code()) + "</status>";
        EXPECT_THAT(result.body, HasSubstr(statusXml)) << data.path;
    }
}

} // namespace maps::staticapi::tests
