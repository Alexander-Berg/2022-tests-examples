#include <maps/infra/roquefort/lib/operation.h>
#include <maps/libs/json/include/value.h>
#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/string/join.h>

#include <list>
#include <set>
#include <string>
#include <iostream>

using namespace maps::roquefort;

std::string getMatchedConfigKeys(
    const std::string& tskvLogLine,
    const maps::json::Value& config
) {
    auto parsed = Fields::parseTskv(tskvLogLine);
    std::set<std::string> matchedConfigKeys;
    for (const auto& key : config.fields()) {
        auto op = Operation::createFromJson(config[key]);
        if (op->matches(parsed)) {
            matchedConfigKeys.emplace(key);
        }
    }
    return JoinSeq(", ", matchedConfigKeys);
}

Y_UNIT_TEST_SUITE(RoquefortConfigTestSuite)
{
    Y_UNIT_TEST(RoquefortConfigTest)
    {
        auto config = maps::json::Value::fromFile(SRC_("../../roquefort/roquefort.conf"));
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("request=/unknown_endpoint", config),
            "all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=GET\trequest=/build/", config),
            "/build/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=GET\trequest=/build/ymapsdf:1234", config),
            "/build/_, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=GET\trequest=/build_hierarchy/?module=masstransit_deployment&build_id=2233", config),
            "/build_hierarchy/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=GET\trequest=/builds/?contour=stable&module=renderer_deployment", config),
            "/builds/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=GET\trequest=/contours/", config),
            "/contours/@get, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=POST\trequest=/contours/", config),
            "/contours/@post, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=GET\trequest=/contours/test/modules/test/activate_version/?module_version=11", config),
            "/contours/_/modules/_/activate_version/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=DELETE\trequest=/contours/exp", config),
            "/contours/_@delete, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=GET\trequest=/contours/exp", config),
            "/contours/_@get, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=GET\trequest=/dir/d1231a109d520c11462aceef40baa524", config),
            "/dir/_, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=GET\trequest=/idm/add-role/", config),
            "/idm/add-role/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=GET\trequest=/idm/get-all-roles/", config),
            "/idm/get-all-roles/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=GET\trequest=/idm/info/", config),
            "/idm/info/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=GET\trequest=/idm/remove-role/", config),
            "/idm/remove-role/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=GET\trequest=/module_statistics/?module=road_graph_deployment&from=2021-03-22T15", config),
            "/module_statistics/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=DELETE\trequest=/modules/ymapsdf/", config),
            "/modules/_/@delete, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=GET\trequest=/modules/road_graph_deployment/?contour=stable", config),
            "/modules/_/@get, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=GET\trequest=/modules/altay/builds/?contour=datatesting&limit=1", config),
            "/modules/_/builds/@get, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=POST\trequest=/modules/road_graph_deployment/builds/?contour=stable", config),
            "/modules/_/builds/@post, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=DELETE\trequest=/modules/ymapsdf/builds/9781/", config),
            "/modules/_/builds/_/@delete, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=GET\trequest=/modules/ymapsdf/builds/9781/", config),
            "/modules/_/builds/_/@get, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=PUT\trequest=/modules/ymapsdf/builds/123/", config),
            "/modules/_/builds/_/@put, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=GET\trequest=/modules/ymapsdf/builds/123/full_info/", config),
            "/modules/_/builds/_/full_info/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("request=/modules/ymapsdf/disable-autostart/?contour=testing", config),
            "/modules/_/disable-autostart/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("request=/modules/altay/enable-autostart/?contour=testing", config),
            "/modules/_/enable-autostart/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("request=/modules/renderer_publication_validator/release_info/", config),
            "/modules/_/release_info/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("request=/modules/altay/scan_resources/?contour=stable", config),
            "/modules/_/scan_resources/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("request=/modules/ymapsdf/traits/", config),
            "/modules/_/traits/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=GET\trequest=/modules/ymapsdf/versions/", config),
            "/modules/_/versions/@get, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=POST\trequest=/modules/ymapsdf/versions/", config),
            "/modules/_/versions/@post, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=DELETE\trequest=/modules/ymapsdf/versions/1234/", config),
            "/modules/_/versions/_/@delete, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=PUT\trequest=/modules/ymapsdf/versions/1234/", config),
            "/modules/_/versions/_/@put, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("request=/pages/?contour=datatesting", config),
            "/pages/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("request=/pages/renderer_denormalization/?contour=test", config),
            "/pages/_/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("request=/storage/?build=altay%3A1028", config),
            "/storage/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("request=/storage/195d1ca065d29d627a3cce3f94ea2cd6/", config),
            "/storage/_/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("request=/modules/altay/events/?contour=stable", config),
            "/modules/_/events/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("request=/module_event_types/", config),
            "/module_event_types/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("request=/modules/altay/builds/1/errors/", config),
            "/modules/_/builds/_/errors/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("request=/modules/altay/logs/", config),
            "/modules/_/logs/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("request=/module_log_types/", config),
            "/module_log_types/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=POST\trequest=/modules/ymapsdf/cancel_autostart/", config),
            "/modules/_/cancel_autostart/, all");
        UNIT_ASSERT_VALUES_EQUAL(
            getMatchedConfigKeys("method=POST\trequest=/modules/ymapsdf/trigger_autostart/", config),
            "/modules/_/trigger_autostart/, all");
        }
}
