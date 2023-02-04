#include "api.h"

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/infra/yacare/include/test_utils.h>
#include <maps/infra/yacare/include/i18n.h>
#include <maps/libs/common/include/file_utils.h>
#include <yandex/maps/proto/traffic/traffic.pb.h>

#include <maps/jams/renderer2/common/yacare/lib/i18n.h>

#include <filesystem>

using namespace maps::jams::jamsinfo;
using namespace maps::jams;

using namespace std::string_literals;

const std::string COVERAGE_XML = "<xml></xml>";
const std::string COVERAGE_JS = "YMaps.Hotspots.Loader.onLoad(\"stat\",{})";

struct Initializer {
    Initializer(time_t jamsTimestamp)
    {
        jamsinfo::standalone::Stats stats;

        {
            jamsinfo::standalone::Stat s;
            s.timestamp = 1559893650;
            s.isotime = "2019-06-07T10:47:30+0300";
            s.time = "10:47";
            s.date = "2019-06-07";
            s.jamsLength = 27271.799965;

            stats.emplace(11481, s);
        }
        {
            jamsinfo::standalone::Stat s;
            s.level = 4;
            s.levelPredictions = {
                {1559894400, "2019-06-07T13:00:00+0500", 5},
                {1559898000, "2019-06-07T14:00:00+0500", 6}
            };
            s.icon = "yellow";
            s.color = 1;
            s.timestamp = 1559893650;
            s.isotime = "2019-06-07T12:47:30+0500";
            s.time = "12:47";
            s.date = "2019-06-07";
            s.jamsLength = 50917.299985;

            stats.emplace(55, s);
        }

        stats.jamsTimestamp = jamsTimestamp;

        {
            std::ofstream f("stats.mms.1");
            mms::write(f, stats);
        }

        {
            std::ofstream f("coverage.xml");
            f << COVERAGE_XML;
        }

        {
            std::ofstream f("coverage.js");
            f << COVERAGE_JS;
        }

        std::error_code ec;
        std::filesystem::create_symlink(
            static_cast<std::string>(SRC_("hist_levels/hist_jams_levels.pb"s)),
            "./hist_jams_levels.pb", ec);

        initialize(".",
            BinaryPath("maps/jams/jamsinfo/yacare/lib/tests/data/geodata6.bin"),
            BinaryPath("maps/jams/jamsinfo/yacare/lib/tests/data/zones_bin"));

        yacare::setDefaultLang("ru");
        renderer::setupi18n();
    }
};


class Fixture: public NUnitTest::TBaseFixture {
public:
    Fixture()
    {
        static Initializer init(std::time(nullptr) - 10 * 60);
    }

    void test(
        const std::string& url,
        const maps::http::HeaderMap& headers,
        const std::string& response
    ) {
        maps::http::MockRequest req(
            maps::http::GET,
            maps::http::URL(url));

        req.headers = headers;

        auto res = yacare::performTestRequest(req);
        UNIT_ASSERT_EQUAL(res.status, 200);

        auto expectedBody = maps::common::readFileToString(SRC_("responses/"s + response));
        expectedBody.pop_back(); // WTF is this \n
        UNIT_ASSERT_STRINGS_EQUAL(res.body, expectedBody);
    }
};

Y_UNIT_TEST_SUITE_F(JamsInfoApi, Fixture)
{
    Y_UNIT_TEST(Default)      { test("http://localhost/info", {}, "info.pb"); }

    Y_UNIT_TEST(JsParams)     { test("http://localhost/info?format=js",              {}, "info.js"); }
    Y_UNIT_TEST(JsonpParams)  { test("http://localhost/info?format=js&callback=123", {}, "info.jsonp"); }
    Y_UNIT_TEST(ProtoParams)  { test("http://localhost/info?format=protobuf",        {}, "info.pb"); }

    Y_UNIT_TEST(JsParamsFiltered)     { test("http://localhost/info?format=js&ids=11481,1",              {}, "info-filtered.js"); }
    Y_UNIT_TEST(JsonpParamsFiltered)  { test("http://localhost/info?format=js&callback=123&ids=11481,1", {}, "info-filtered.jsonp"); }

    Y_UNIT_TEST(JsHeader)    { test("http://localhost/info",              {{"Accept", "application/javascript"}}, "info.js"); }
    Y_UNIT_TEST(JsonpHeader) { test("http://localhost/info?callback=123", {{"Accept", "application/javascript"}}, "info.jsonp"); }
    Y_UNIT_TEST(ProtoHeader) { test("http://localhost/info",              {{"Accept", "application/x-protobuf"}}, "info.pb"); }

    Y_UNIT_TEST(JsHeaderFiltered)    { test("http://localhost/info?ids=11481,1",              {{"Accept", "application/javascript"}}, "info-filtered.js"); }
    Y_UNIT_TEST(JsonpHeaderFiltered) { test("http://localhost/info?callback=123&ids=11481,1", {{"Accept", "application/javascript"}}, "info-filtered.jsonp"); }
    Y_UNIT_TEST(ProtoHeaderFiltered) { test("http://localhost/info?ids=55,1",                 {{"Accept", "application/x-protobuf"}}, "info-filtered.pb"); }

    Y_UNIT_TEST(JsHeaderFilteredNoRegions)    { test("http://localhost/info?ids=1",              {{"Accept", "application/javascript"}}, "info-empty.js"); }
    Y_UNIT_TEST(JsonpHeaderFilteredNoRegions) { test("http://localhost/info?callback=123&ids=1", {{"Accept", "application/javascript"}}, "info-empty.jsonp"); }
    Y_UNIT_TEST(ProtoHeaderFilteredNoRegions) { test("http://localhost/info?ids=1",              {{"Accept", "application/x-protobuf"}}, "info-empty.pb"); }

    Y_UNIT_TEST(LevelsPred) { test("http://localhost/levels_prediction", {}, "levels_prediction.json"); }

    Y_UNIT_TEST(TrafficStat) { test("http://localhost/traffic/current/stat.xml", {}, "info-export.xml"); }

    Y_UNIT_TEST(InfoFormatsNotSupportedAnymore)
    {
        maps::http::MockRequest xmlReq(maps::http::GET, "http://localhost/info?format=xml");
        auto xmlRes = yacare::performTestRequest(xmlReq);
        UNIT_ASSERT_EQUAL(xmlRes.status, 400);

        maps::http::MockRequest jsonReq(maps::http::GET, "http://localhost/info?format=json");
        auto jsonRes = yacare::performTestRequest(jsonReq);
        UNIT_ASSERT_EQUAL(jsonRes.status, 400);

        maps::http::MockRequest exportReq(maps::http::GET, "http://localhost/info?format=export");
        auto exportRes = yacare::performTestRequest(exportReq);
        UNIT_ASSERT_EQUAL(exportRes.status, 400);
    }

    Y_UNIT_TEST(JsonpWrongCallback)
    {
        std::string callback(200, 'a');
        maps::http::MockRequest req(maps::http::GET, "http://localhost/info?format=js&callback="s + callback);
        auto res = yacare::performTestRequest(req);
        UNIT_ASSERT_EQUAL(res.status, 400);
    }

    Y_UNIT_TEST(PassCallbackWithXmlIsWrong)
    {
        maps::http::MockRequest req(maps::http::GET, "http://localhost/info?format=xml&callback=123");
        auto res = yacare::performTestRequest(req);
        UNIT_ASSERT_EQUAL(res.status, 400);
    }

    Y_UNIT_TEST(JamsAreOutdatedPing)
    {
        UNIT_ASSERT(service().outdated());
    }

    Y_UNIT_TEST(LevelsHistory)
    {
        test("http://localhost/levels_history?ids=2,1&l=ru_RU&v=862200&callback=jsonp_1595165999147_31024", {}, "levels-found.js");
        test("http://localhost/levels_history?ids=2,1&l=ru_RU&v=0&callback=jsonp_1595165999147_31024", {}, "levels-no-stats.js");
        test("http://localhost/levels_history?ids=1&l=ru_RU&v=862200&callback=jsonp_1595165999147_31024", {}, "levels-no-region.js");
    }

    Y_UNIT_TEST(UpdateHistoricLevels)
    {
        maps::http::MockRequest req(maps::http::POST, "http://localhost/update_historic_levels");
        auto res = yacare::performTestRequest(req);
        UNIT_ASSERT_EQUAL(res.status, 200);
    }
}
