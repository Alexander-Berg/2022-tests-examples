#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/tests_data.h>
#include <maps/mobile/server/init/lib/access_list.h>
#include <maps/mobile/server/init/lib/environment_experiment.h>
#include <maps/mobile/server/init/lib/experiment_distributor.h>
#include <maps/mobile/server/init/lib/network_metrica_log_experiment.h>
#include <maps/mobile/server/init/lib/tstring_helper.h>
#include <maps/libs/common/include/base64.h>
#include <maps/libs/log8/include/log8.h>
#include <maps/libs/common/include/exception.h>
#include <yandex/maps/proto/mobile_config/experiments.pb.h>

#include <boost/test/unit_test.hpp>

#include <fstream>

namespace proto_experiments = yandex::maps::proto::mobile_config::experiments;

namespace {

class ExperimentCacheMock final : public ExperimentCache {
public:
    explicit ExperimentCacheMock(const std::string& config)
        : cache_(std::make_shared<Config>(
            std::make_unique<const UserSplitCarrier>(config),
            "geoapps"))
    {
    }

    std::shared_ptr<const Config> config(const std::string& /*application*/) const override
    {
        return cache_;
    }

    std::vector<ExperimentCache::LoadError> loadErrors() const override
    {
        std::vector<ExperimentCache::LoadError> result;
        for (ExperimentParsingError& error : cache_->carrier->errors()) {
                result.emplace_back("", std::move(error));
        }
        return result;
    }

    std::vector<ExperimentCache::TestIdError> testIdErrors() const override
    {
        return {};
    }

private:
     std::shared_ptr<const Config> cache_;
};

class GeoHelperMock final : public GeoHelper {
public:
    GeoHelperMock()
    {
        regionSet_ = {1, 3, 225, 10001, 10000};
    }

    uint32_t findRegion(double /*lat*/, double /*lon*/) const override
    {
        return 213;
    }
    const NUserSplit::TRegionSet& regionAncestors(RegionId /*regionId*/) const override
    {
        return regionSet_;
    }
    bool isInside(const std::string& /*ipAddress*/, RegionId /*parentId*/) const override
    {
        return false;
    }

private:
    NUserSplit::TRegionSet regionSet_;
};

std::string experimentConfig(const std::string& name)
{
    std::string fileName = (GetWorkPath() + "/" + name).c_str();
    std::ifstream f(fileName);
    f.seekg(0, std::ios::end);
    size_t length = f.tellg();
    f.seekg(0, std::ios::beg);
    std::string s;
    s.resize(length);
    f.read(s.data(), length);
    return s;
}

const ExperimentCache& experimentCache()
{
       static const ExperimentCacheMock cache(experimentConfig("config.usersplit"));
       return cache;
}

const ExperimentDistributor& experimentDistributor()
{
    static const GeoHelperMock geoHelper;
    static const ExperimentDistributor experimentDistributor(
        &experimentCache(),
        &geoHelper,
        AccessList(SRC_("blacklist"), SRC_("empty")));
    return experimentDistributor;
}

void checkExperimentParsing(
    const std::string& json,
    const std::string& expectedExperiment,
    unsigned expectedTestIdCount,
    unsigned expectedErrorCount)
{
    unsigned testIdCount = 0;
    unsigned errorCount = 0;
    ExperimentInfo(
        maps::base64Encode(json),
        [&](ExperimentInfo::TestId&&) {
            ++testIdCount;
        },
        [&](std::string&& experiment, std::string&& error) {
            BOOST_REQUIRE(experiment == expectedExperiment);
            BOOST_REQUIRE(error.size() > 0);
            INFO() << error;
            ++errorCount;
        }
    );
    BOOST_REQUIRE(testIdCount == expectedTestIdCount);
    BOOST_REQUIRE(errorCount == expectedErrorCount);
}

} // namespace

BOOST_AUTO_TEST_CASE(test_android)
{
    BOOST_REQUIRE(experimentCache().loadErrors().size() == 1);

    ExperimentDistributor::UserInformation ui;
    ui.id  = "c84ee6a356a3de84b04de954de5f0413";
    ui.library = "";
    ui.applicationId = "";
    ui.applicationVersion = "5.0";
    ui.platform ="android";
    ui.deviceId = "345345";
    ui.longitude = 37.620652;
    ui.latitude = 55.754196;
    std::optional<Settings> settings = experimentDistributor().userExperimentSettings(ui);
    BOOST_REQUIRE(settings);

    int maxExperiments = (hasNetworkMetricaLogExperiment(*settings) ? 85 : 84);
    const proto_experiments::Config& config = *settings->MutableExtension(proto_experiments::config);
    BOOST_REQUIRE(config.experiments_size() == (!hasDataprestableExperiment(*settings) ? maxExperiments - 1 : maxExperiments));

    std::unordered_map<unsigned, int> experiments;
    for (int i = 0; i < config.experiments_size(); ++i) {
        const proto_experiments::Experiment& e = config.experiments(i);
        experiments.emplace(e.id(), i);
    }
    BOOST_REQUIRE(config.experiments_size() == static_cast<int>(experiments.size()));

    {
        auto iter = experiments.find(216653);
        BOOST_REQUIRE(iter != experiments.cend());

        const proto_experiments::Experiment& e = config.experiments(iter->second);
        BOOST_REQUIRE(e.user_group() == 76);
        BOOST_REQUIRE(e.user_bucket() == 27);
        BOOST_REQUIRE(e.request_parameters_size() == 1);

        const proto_experiments::RequestParameter& p = e.request_parameters(0);
        BOOST_REQUIRE(p.service() == "MAPS_UI");
        BOOST_REQUIRE(p.name() == "navi_feature_map_remote_config");
        BOOST_REQUIRE(p.value() == "enabled");
    }
    {
        auto iter = experiments.find(210758);
        BOOST_REQUIRE(iter != experiments.cend());

        const proto_experiments::Experiment& e = config.experiments(iter->second);
        BOOST_REQUIRE(e.user_group() == 55);
        BOOST_REQUIRE(e.user_bucket() == 27);
        BOOST_REQUIRE(e.request_parameters_size() == 1);

        const proto_experiments::RequestParameter& p = e.request_parameters(0);
        BOOST_REQUIRE(p.service() == "MAPS_UI");
        BOOST_REQUIRE(p.name() == "navi_zero_ads_use_direct_ad");
        BOOST_REQUIRE(p.value() == "enabled");
    }
    {
        auto iter = experiments.find(204078);
        BOOST_REQUIRE(iter != experiments.cend());

        const proto_experiments::Experiment& e = config.experiments(iter->second);
        BOOST_REQUIRE(e.user_group() == 41);
        BOOST_REQUIRE(e.user_bucket() == 27);
        BOOST_REQUIRE(e.request_parameters_size() == 7);

        const proto_experiments::RequestParameter& p0 = e.request_parameters(0);
        BOOST_REQUIRE(p0.service() == "MAPS_UI");
        BOOST_REQUIRE(p0.name() == "navi_feature_gas_stations_show_card_raw_position_distance_from_matched_meters");
        BOOST_REQUIRE(p0.value() == "6.0");

        const proto_experiments::RequestParameter& p1 = e.request_parameters(1);
        BOOST_REQUIRE(p1.service() == "MAPS_UI");
        BOOST_REQUIRE(p1.name() == "navi_feature_gas_stations_show_card_raw_position_enabled");
        BOOST_REQUIRE(p1.value() == "enabled");

        const proto_experiments::RequestParameter& p2 = e.request_parameters(2);
        BOOST_REQUIRE(p2.service() == "MAPS_UI");
        BOOST_REQUIRE(p2.name() == "navi_feature_gas_stations_show_card_raw_position_max_accuracy_meters");
        BOOST_REQUIRE(p2.value() == "30.0");

        const proto_experiments::RequestParameter& p3 = e.request_parameters(3);
        BOOST_REQUIRE(p3.service() == "MAPS_UI");
        BOOST_REQUIRE(p3.name() == "navi_feature_gas_stations_show_card_raw_position_min_accuracy_meters");
        BOOST_REQUIRE(p3.value() == "4.0");

        const proto_experiments::RequestParameter& p4 = e.request_parameters(4);
        BOOST_REQUIRE(p4.service() == "MAPS_UI");
        BOOST_REQUIRE(p4.name() == "navi_feature_gas_stations_show_card_raw_position_seconds_in_polygon");
        BOOST_REQUIRE(p4.value() == "10.0");

        const proto_experiments::RequestParameter& p5 = e.request_parameters(5);
        BOOST_REQUIRE(p5.service() == "MAPS_UI");
        BOOST_REQUIRE(p5.name() == "navi_feature_gas_stations_show_card_raw_position_speed_bellow_kph");
        BOOST_REQUIRE(p5.value() == "10.0");

        const proto_experiments::RequestParameter& p6 = e.request_parameters(6);
        BOOST_REQUIRE(p6.service() == "MAPS_UI");
        BOOST_REQUIRE(p6.name() == "navi_feature_gas_stations_show_card_raw_position_use_accuracy");
        BOOST_REQUIRE(p6.value() == "enabled");
    }
}

BOOST_AUTO_TEST_CASE(test_iphone)
{
    BOOST_REQUIRE(experimentCache().loadErrors().size() == 1);

    ExperimentDistributor::UserInformation ui;
    ui.id  = "c84ee6a356a3de84b04de954de5f0413";
    ui.library = "";
    ui.applicationId = "";
    ui.applicationVersion = "500.0";
    ui.platform ="iphoneos";
    ui.deviceId = "345345";
    ui.longitude = 37.620652;
    ui.latitude = 55.754196;

    std::optional<Settings> settings = experimentDistributor().userExperimentSettings(ui);
    BOOST_REQUIRE(settings);

    int maxExperiments = (hasNetworkMetricaLogExperiment(*settings) ? 85 : 84);
    const proto_experiments::Config& config = *settings->MutableExtension(proto_experiments::config);
    BOOST_REQUIRE(config.experiments_size() == (!hasDataprestableExperiment(*settings) ? maxExperiments - 1 : maxExperiments));

    std::unordered_map<unsigned, int> experiments;
    for (int i = 0; i < config.experiments_size(); ++i) {
        const proto_experiments::Experiment& e = config.experiments(i);
        experiments.emplace(e.id(), i);
    }
    BOOST_REQUIRE(config.experiments_size() == static_cast<int>(experiments.size()));

    BOOST_REQUIRE(experiments.count(216653) == 1);
    BOOST_REQUIRE(experiments.count(204078) == 1);

    {
        auto iter = experiments.find(215094);
        BOOST_REQUIRE(iter != experiments.cend());

        const proto_experiments::Experiment& e = config.experiments(iter->second);
        BOOST_REQUIRE(e.user_group() == 7);
        BOOST_REQUIRE(e.user_bucket() == 27);
        BOOST_REQUIRE(e.request_parameters_size() == 1);

        const proto_experiments::RequestParameter& p = e.request_parameters(0);
        BOOST_REQUIRE(p.service() == "MAPS_UI");
        BOOST_REQUIRE(p.name() == "navi_zero_ads_use_direct_ad");
        BOOST_REQUIRE(p.value() == "enabled");
    }
}

BOOST_AUTO_TEST_CASE(test_parsing_no_error)
{
    constexpr const char* json =
        R"d([{"TESTID":["1"],"HANDLER":"MAPS","CONTEXT":{"MAIN":{"source":{"MAPS_ROUTER":{"text":["1"]}}}},
            "CONDITION":""}])d";
    checkExperimentParsing(json, "1", 1, 0);
}
BOOST_AUTO_TEST_CASE(test_parsing_handler_error)
{
    constexpr const char* json =
        R"d([{"TESTID":["1"],"HNDLR":"MAPS","CONTEXT":{"MAIN":{"source":{"MAPS_ROUTER":{"text":["1"]}}}},
            "CONDITION":""}])d";
    checkExperimentParsing(json, "1", 1, 1);
}
BOOST_AUTO_TEST_CASE(test_parsing_context_main_source_error)
{
    constexpr const char* json =
        R"d([{"TESTID":["1"],"HNDLR":"MAPS","CONTEXT":{"source":{"MAPS_ROUTER":{"text":["1"]}}},
            "CONDITION":""}])d";
    checkExperimentParsing(json, "1", 1, 1);
}
BOOST_AUTO_TEST_CASE(test_parsing_value_as_array_error)
{
    constexpr const char* json =
        R"d([{"TESTID":["1"],"HANDLER":"MAPS","CONTEXT":{"MAIN":{"source":{"MAPS_ROUTER":{"text":["1","2"]}}}},
            "CONDITION":""}])d";
    checkExperimentParsing(json, "1", 1, 1);
}
BOOST_AUTO_TEST_CASE(test_parsing_value_as_number)
{
    constexpr const char* json =
        R"d([{"TESTID":["1"],"HANDLER":"MAPS","CONTEXT":{"MAIN":{"source":{"MAPS_ROUTER":{"text":[1]}}}},
            "CONDITION":""}])d";
    checkExperimentParsing(json, "1", 1, 0);
}
BOOST_AUTO_TEST_CASE(test_parsing_value_not_number_error)
{
    constexpr const char* json =
        R"d([{"TESTID":["1"],"HANDLER":"MAPS","CONTEXT":{"MAIN":{"source":{"MAPS_ROUTER":{"text":[{"1":"2"}]}}}},
            "CONDITION":""}])d";
    checkExperimentParsing(json, "1", 1, 1);
}
BOOST_AUTO_TEST_CASE(test_parsing_condition_json_error)
{
    constexpr const char* json =
        R"d([{"TESTID":["1"],"HANDLER":"MAPS","CONTEXT":{"MAIN":{"source":{"MAPS_ROUTER":{"text":[1]}}}},
            "CONDITION":"1\"2"}])d";
    checkExperimentParsing(json, "1", 1, 1);
}
BOOST_AUTO_TEST_CASE(test_parsing_condition)
{
    constexpr const char* json =
        R"d([{"TESTID":["1"],"HANDLER":"MAPS","CONTEXT":{"MAIN":{"source":{"MAPS_ROUTER":{"text":[1]}}}},
            "CONDITION":"[{\"appVersion\": {\"minAppVersionIphone\": \"441.00\", \"minAppVersionAndroid\": \"4.41.00\"}}]"}])d";
    checkExperimentParsing(json, "1", 1, 0);
}
BOOST_AUTO_TEST_CASE(test_parsing_condition_min_max_android_error)
{
    constexpr const char* json =
        R"d([{"TESTID":["1"],"HANDLER":"MAPS","CONTEXT":{"MAIN":{"source":{"MAPS_ROUTER":{"text":[1]}}}},
            "CONDITION":"[{\"appVersion\": {\"minAppVersionAndroid\": \"7.1.00\",\"maxAppVersionAndroid\": \"6.1.00\"}}]"}])d";
    checkExperimentParsing(json, "1", 1, 1);
}
BOOST_AUTO_TEST_CASE(test_parsing_condition_min_max_iphone_error)
{
    constexpr const char* json =
        R"d([{"TESTID":["1"],"HANDLER":"MAPS","CONTEXT":{"MAIN":{"source":{"MAPS_ROUTER":{"text":[1]}}}},
            "CONDITION":"[{\"appVersion\": {\"minAppVersionIphone\": \"710.00\",\"maxAppVersionIphone\": \"610.00\"}}]"}])d";
    checkExperimentParsing(json, "1", 1, 1);
}
BOOST_AUTO_TEST_CASE(test_parsing_condition_with_empty_device_segments)
{
    constexpr const char* json =
        R"d([{"TESTID":["1"],"HANDLER":"MAPS","CONTEXT":{"MAIN":{"source":{"MAPS_ROUTER":{"text":[1]}}}},
            "CONDITION":"[{\"appVersion\": {\"minAppVersionIphone\": \"10.0\",\"maxAppVersionIphone\": \"20.0\"},
                          \"anyDeviceSegments\":[]}]"}])d";
    checkExperimentParsing(json, "1", 1, 1);
}
BOOST_AUTO_TEST_CASE(test_parsing_condition_with_one_device_segment)
{
    constexpr const char* json =
        R"d([{"TESTID":["1"],"HANDLER":"MAPS","CONTEXT":{"MAIN":{"source":{"MAPS_ROUTER":{"text":[1]}}}},
            "CONDITION":"[{\"anyDeviceSegments\":[\"with_2_gis\"]}]"}])d";
    checkExperimentParsing(json, "1", 1, 0);
}
BOOST_AUTO_TEST_CASE(test_parsing_condition_with_device_segments)
{
    constexpr const char* json =
        R"d([{"TESTID":["1"],"HANDLER":"MAPS","CONTEXT":{"MAIN":{"source":{"MAPS_ROUTER":{"text":[1]}}}},
            "CONDITION":"[{\"anyDeviceSegments\":[\"with_2_gis\", \"driver\"]}]"}])d";
    checkExperimentParsing(json, "1", 1, 0);
}
BOOST_AUTO_TEST_CASE(test_parsing_error_condition)
{
    constexpr const char* json =
        R"d([{"TESTID":["1"],"HANDLER":"MAPS","CONTEXT":{"MAIN":{"source":{"MAPS_ROUTER":{"text":[1]}}}},
            "CONDITION":"[{\"appVersion\": {\"minAppVersionIphone\": \"10.0\",\"maxAppVersionIphone\": \"20.0\"}},
                         {\"anyDeviceSegments\":[\"with_2_gis\", \"driver\"]}]"}])d";
    checkExperimentParsing(json, "1", 1, 1);
}
BOOST_AUTO_TEST_CASE(test_parsing_unknown_condition)
{
    constexpr const char* json =
        R"d([{"TESTID":["1"],"HANDLER":"MAPS","CONTEXT":{"MAIN":{"source":{"MAPS_ROUTER":{"text":[1]}}}},
            "CONDITION":"[{\"appVersion\": {\"minAppVersionIphone\": \"10.0\",\"maxAppVersionIphone\": \"20.0\"},
                         \"deviceSegments\":[\"driver\"]}]"}])d";
    checkExperimentParsing(json, "1", 1, 1);
}
BOOST_AUTO_TEST_CASE(test_parsing_json_error)
{
    constexpr const char* json = R"d(["})d";
    try {
        checkExperimentParsing(json, "", 0, 0);
    } catch(const maps::RuntimeError& e) {
        INFO() << e.what();
        BOOST_REQUIRE(true);
        return;
    }
    BOOST_REQUIRE(false);
}
BOOST_AUTO_TEST_CASE(test_parsing_condition_no_test_id_with_handler_error)
{
    constexpr const char* json = R"d([{"HNDLR":"MAPS"}])d";
    try {
        checkExperimentParsing(json, "0", 0, 0);
    } catch(const maps::RuntimeError& e) {
        INFO() << e.what();
        BOOST_REQUIRE(true);
        return;
    }
    BOOST_REQUIRE(false);
}
BOOST_AUTO_TEST_CASE(test_parsing_test_id_as_number_error)
{
    constexpr const char* json =
        R"d([{"TESTID":[1],"HANDLER":"MAPS","CONTEXT":{"MAIN":{"source":{"MAPS_ROUTER":{"text":[1]}}}},
            "CONDITION":""}])d";
    try {
        checkExperimentParsing(json, "0", 0, 0);
    } catch(const maps::RuntimeError& e) {
        INFO() << e.what();
        BOOST_REQUIRE(true);
        return;
    }
    BOOST_REQUIRE(false);
}
BOOST_AUTO_TEST_CASE(test_parsing_test_id_as_string_non_number_error)
{
    constexpr const char* json =
        R"d([{"TESTID":["abc"],"HANDLER":"MAPS","CONTEXT":{"MAIN":{"source":{"MAPS_ROUTER":{"text":[1]}}}},
            "CONDITION":""}])d";
    try {
        checkExperimentParsing(json, "0", 0, 0);
    } catch(const maps::RuntimeError& e) {
        INFO() << e.what();
        BOOST_REQUIRE(true);
        return;
    }
    BOOST_REQUIRE(false);
}

BOOST_AUTO_TEST_CASE(test_parsing_config_with_empty_json_android)
{
    ExperimentCacheMock cache(experimentConfig("config_with_empty_json.usersplit"));
    BOOST_REQUIRE(cache.loadErrors().empty());

    const GeoHelperMock geoHelper;
    const ExperimentDistributor distributor(
        &cache,
        &geoHelper,
        AccessList(SRC_("blacklist"), SRC_("empty")));

    ExperimentDistributor::UserInformation ui;
    ui.id  = "c84ee6a356a3de84b04de954de5f0413";
    ui.library = "";
    ui.applicationId = "";
    ui.applicationVersion = "5.0";
    ui.platform ="android";
    ui.deviceId = "345345";
    ui.longitude = 37.620652;
    ui.latitude = 55.754196;
    std::optional<Settings> settings = distributor.userExperimentSettings(ui);
    BOOST_REQUIRE(settings);

    int maxExperiments = (hasNetworkMetricaLogExperiment(*settings) ? 9 : 8);
    const proto_experiments::Config& config = *settings->MutableExtension(proto_experiments::config);
    BOOST_REQUIRE(config.experiments_size() == (!hasDataprestableExperiment(*settings) ? maxExperiments - 1 : maxExperiments));

    std::unordered_map<unsigned, int> experiments;
    for (int i = 0; i < config.experiments_size(); ++i) {
        const proto_experiments::Experiment& e = config.experiments(i);
        experiments.emplace(e.id(), i);
    }
    BOOST_REQUIRE(config.experiments_size() == static_cast<int>(experiments.size()));

    auto iter = experiments.find(313363);
    BOOST_REQUIRE(iter != experiments.cend());

    const proto_experiments::Experiment& e = config.experiments(iter->second);
    BOOST_REQUIRE(e.user_group() == 76);
    BOOST_REQUIRE(e.user_bucket() == 27);
    BOOST_REQUIRE(e.request_parameters_size() == 0);
}

BOOST_AUTO_TEST_CASE(test_parsing_config_with_empty_json_iphone)
{
    ExperimentCacheMock cache(experimentConfig("config_with_empty_json.usersplit"));
    BOOST_REQUIRE(cache.loadErrors().empty());

    const GeoHelperMock geoHelper;
    const ExperimentDistributor distributor(
        &cache,
        &geoHelper,
        AccessList(SRC_("blacklist"), SRC_("empty")));

    ExperimentDistributor::UserInformation ui;
    ui.id  = "c84ee6a356a3de84b04de954de5f0413";
    ui.library = "";
    ui.applicationId = "";
    ui.applicationVersion = "500.0";
    ui.platform ="iphoneos";
    ui.deviceId = "345345";
    ui.longitude = 37.620652;
    ui.latitude = 55.754196;
    std::optional<Settings> settings = distributor.userExperimentSettings(ui);
    BOOST_REQUIRE(settings);

    int maxExperiments = (hasNetworkMetricaLogExperiment(*settings) ? 10 : 9);
    const proto_experiments::Config& config = *settings->MutableExtension(proto_experiments::config);
    BOOST_REQUIRE(config.experiments_size() == (!hasDataprestableExperiment(*settings) ? maxExperiments - 1 : maxExperiments));

    std::unordered_map<unsigned, int> experiments;
    for (int i = 0; i < config.experiments_size(); ++i) {
        const proto_experiments::Experiment& e = config.experiments(i);
        experiments.emplace(e.id(), i);
    }
    BOOST_REQUIRE(config.experiments_size() == static_cast<int>(experiments.size()));

    auto iter = experiments.find(313363);
    BOOST_REQUIRE(iter != experiments.cend());

    const proto_experiments::Experiment& e = config.experiments(iter->second);
    BOOST_REQUIRE(e.user_group() == 76);
    BOOST_REQUIRE(e.user_bucket() == 27);
    BOOST_REQUIRE(e.request_parameters_size() == 0);
}

BOOST_AUTO_TEST_CASE(test_parsing_config_with_device_segments)
{
    ExperimentCacheMock cache(experimentConfig("config_with_device_segments.usersplit"));
    BOOST_REQUIRE(cache.loadErrors().empty());

    const GeoHelperMock geoHelper;
    const ExperimentDistributor distributor(
        &cache,
        &geoHelper,
        AccessList(SRC_("blacklist"), SRC_("empty")));

    ExperimentDistributor::UserInformation ui;
    ui.id  = "c84ee6a356a3de84b04de954de5f0413";
    ui.library = "";
    ui.applicationId = "";
    ui.applicationVersion = "500.0";
    ui.platform ="iphoneos";
    ui.deviceId = "345345";
    ui.longitude = 37.620652;
    ui.latitude = 55.754196;
    std::optional<Settings> settings = distributor.userExperimentSettings(ui);
    BOOST_REQUIRE(settings);

    const proto_experiments::Config& config = *settings->MutableExtension(proto_experiments::config);

    std::unordered_map<unsigned, int> experiments;
    for (int i = 0; i < config.experiments_size(); ++i) {
        const proto_experiments::Experiment& e = config.experiments(i);
        experiments.emplace(e.id(), i);
    }
    auto iter = experiments.find(471011);
    BOOST_REQUIRE(iter == experiments.cend());
}

BOOST_AUTO_TEST_CASE(test_environment_experiment_settings)
{
    const Settings& settings = environmentExperimentSettings(ExperimentalEnvironment::DATATESTING);
    const proto_experiments::Config& config = settings.GetExtension(proto_experiments::config);

    BOOST_REQUIRE(config.experiments_size() > 0);
    const proto_experiments::Experiment& e = config.experiments(0);
    BOOST_REQUIRE(e.request_parameters_size() > 0);
    for (const auto& p : e.request_parameters()) {
        INFO() << p.service();
    }
}
