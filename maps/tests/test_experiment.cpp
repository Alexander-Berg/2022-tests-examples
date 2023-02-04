#include <google/protobuf/text_format.h>
#include <maps/automotive/libs/test_helpers/helpers.h>
#include <maps/automotive/updater/lib/config.h>
#include <maps/automotive/updater/lib/helpers.h>
#include <maps/automotive/updater/proto/updater.pb.h>
#include <maps/automotive/updater/test_helpers/mocks.h>

#include <cstdlib>

namespace maps::automotive::updater {

using namespace ::testing;

namespace {

const std::string REQ_2X = R"(software {} firmware { properties { key: "ro.build.date.utc" value: "0" }})";
const std::string REQ_FW_1X = R"({"ro.build.date.utc": "0"})";

struct ExperimentFixture : public Fixture
{
    static constexpr PackageVersionCode VERSION_CODE = 5;
    static constexpr int FW_VERSION = 10;

    ExperimentFixture() : Fixture()
    {
        ON_CALL(uCache(), latest(_, _))
            .WillByDefault(Return(Rollout{{}, {}}));
        
        ON_CALL(uCache(), taggedApps())
            .WillByDefault(Return(TagMap{}));

        scope.set_branch("release");
        scope.mutable_headunit()->set_type("taxi");
        scope.mutable_headunit()->set_mcu("astar");
        scope.mutable_headunit()->set_vendor("vendor");
        scope.mutable_headunit()->set_model("exp");

        experiment.set_id(43);

        package.mutable_id()->set_name("pkg");
        package.mutable_id()->set_version_code(VERSION_CODE);
        package.set_url("file://pkg");
        package.mutable_metadata()->set_app_name("pkg.ru");
        package.mutable_metadata()->set_version_name("v1");

        firmware.mutable_id()->set_name("fw");
        firmware.mutable_id()->set_version(ToString(FW_VERSION));
        firmware.set_url("file://fw");
        firmwares[firmware.id()] = firmware;

        masi::FirmwareRollout rollout;
        rollout.set_branch("release");
        *rollout.mutable_headunit() = scope.headunit();
        *rollout.mutable_firmware_id() = firmware.id();
        *rollout.mutable_experiment() = experiment;
        firmwareRollouts.push_back(rollout);

        maps::log8::setLevel(maps::log8::Level::WARNING);
    }

    template <typename TestResponse>
    void test(
        int expectedPercent,
        const std::string& handle,
        const std::string& body,
        TestResponse testResponse)
    {
        const int minRequests = 100;
        const int maxRequests = 1000;
        int inExperiment = 0;
        int reqNum = 1;
        for (; reqNum <= maxRequests; ++reqNum) {
            auto res = mockPost(
                handle +
                    "?" "type=taxi"
                    "&" "mcu=astar"
                    "&" "vendor=vendor"
                    "&" "model=exp"
                    "&" "lang=ru_RU"
                    "&" "headid=headid_" + std::to_string(reqNum),
                body);
            if (testResponse(res)) {
                ++inExperiment;
            }
            // compare inExperiment/numReq with expectedPercent/100 with precision of 0.01
            if (reqNum >= minRequests
                && std::abs(expectedPercent * reqNum - 100 * inExperiment) < reqNum)
            {
                INFO() << "Expected percent " << expectedPercent
                    << ", made " << reqNum << " requests"
                    << ", have " << inExperiment << " hits";
                return;
            }
        }
        throw maps::Exception("Cannot meet expected percent ") << expectedPercent
            << ", made " << reqNum << " requests"
            << ", have " << inExperiment << " hits";
    }

public:
    masi::RolloutExperiment experiment;
    FirmwareMap firmwares;
    std::vector<masi::FirmwareRollout> firmwareRollouts;
    masi::Firmware firmware;
    masi::Package package;
    masi::Scope scope;
};

bool testResponsePackage2x(
    const maps::http::MockResponse& rsp, PackageVersionCode versionCode)
{
    proto::UpdateResponse response;
    TString body(rsp.body);
    ASSERT(NProtoBuf::TextFormat::ParseFromString(body, &response));
    for (const auto& update: response.software().updates()) {
        for (const auto& pkg: update.packages()) {
            if (pkg.name() == "pkg.ru" && pkg.version().code() == versionCode) {
                return true;
            }
        }
    }
    return false;
}

bool testResponsePackage1x(
    const maps::http::MockResponse& rsp, PackageVersionCode versionCode)
{
    if (rsp.status != 200) {
        return false;
    }
    auto json = maps::json::Value::fromString(rsp.body);
    const auto& packages = json["packages"];
    if (!packages.exists()) {
        return false;
    }
    return FindIf(packages, [versionCode](const maps::json::Value& pkg) {
        return pkg["name"].as<std::string>() == "pkg.ru"
            && pkg["version"]["code"].as<PackageVersionCode>() == versionCode;
    }) != packages.end();
}

bool testResponseFirmware2x(
    const maps::http::MockResponse& rsp, int version)
{
    proto::UpdateResponse response;
    TString body(rsp.body);
    ASSERT(NProtoBuf::TextFormat::ParseFromString(body, &response));
    return FindIf(response.firmware().updates(), [version](const auto& update) {
        return update.name() == "fw" && update.version() == ToString(version);
    }) != response.firmware().updates().end();
}

bool testResponseFirmware1x(
    const maps::http::MockResponse& rsp, int version)
{
    if (rsp.status != 200) {
        return false;
    }
    auto firmwares = maps::json::Value::fromString(rsp.body);
    return FindIf(firmwares, [version](const maps::json::Value& fw) {
        return fw["name"].as<std::string>() == "fw" &&
            fw["version"].as<std::string>() == std::to_string(version);
    }) != firmwares.end();
}

} // namespace

TEST_F(ExperimentFixture, PackagePercent0)
{
    experiment.set_percent(0);
    Rollout rollout {
        {{package, experiment.id()}}, 
        {{experiment.id(), experiment}}};
    EXPECT_CALL(uCache(), latest(scope, _)).WillRepeatedly(Return(rollout));

    test(0, "/updater/2.x/updates", REQ_2X,
        [](const auto& rsp) { return testResponsePackage2x(rsp, VERSION_CODE); });
    test(0, "/updater/1.x/updates", "[]",
        [](const auto& rsp) { return testResponsePackage1x(rsp, VERSION_CODE); });
}

TEST_F(ExperimentFixture, PackagePercent42)
{
    auto oldPkg = package;
    oldPkg.mutable_id()->set_version_code(VERSION_CODE - 1);
    experiment.set_percent(42);
    Rollout rollout {
        {{oldPkg, {}}, {package, experiment.id()}}, 
        {{experiment.id(), experiment}}};
    EXPECT_CALL(uCache(), latest(scope, _)).WillRepeatedly(Return(rollout));

    test(42, "/updater/2.x/updates", REQ_2X,
        [](const auto& rsp) { return testResponsePackage2x(rsp, VERSION_CODE); });
    test(42, "/updater/1.x/updates", "[]",
        [](const auto& rsp) { return testResponsePackage1x(rsp, VERSION_CODE); });
}

TEST_F(ExperimentFixture, PackagePercent100)
{
    experiment.set_percent(100);
    Rollout rollout {
        {{package, experiment.id()}}, 
        {{experiment.id(), experiment}}};
    EXPECT_CALL(uCache(), latest(scope, _)).WillRepeatedly(Return(rollout));

    test(100, "/updater/2.x/updates", REQ_2X,
        [](const auto& rsp) { return testResponsePackage2x(rsp, VERSION_CODE); });
    test(100, "/updater/1.x/updates", "[]",
        [](const auto& rsp) { return testResponsePackage1x(rsp, VERSION_CODE); });
}

TEST_F(ExperimentFixture, PackageNewStable)
{
    auto newPkg = package;
    newPkg.mutable_id()->set_version_code(VERSION_CODE + 1);
    experiment.set_percent(50);
    Rollout rollout {
        {{newPkg, {}}, {package, experiment.id()}}, 
        {{experiment.id(), experiment}}};
    EXPECT_CALL(uCache(), latest(scope, _)).WillRepeatedly(Return(rollout));

    test(100, "/updater/2.x/updates", REQ_2X,
        [](const auto& rsp) { return testResponsePackage2x(rsp, VERSION_CODE + 1); });
    test(100, "/updater/1.x/updates", "[]",
        [](const auto& rsp) { return testResponsePackage1x(rsp, VERSION_CODE + 1); });
    test(0, "/updater/2.x/updates", R"(software { packages { name: "pkg.ru" version { code: 999 text: "V999"}}} firmware {})",
        [](const auto& rsp) { return testResponsePackage2x(rsp, VERSION_CODE + 1); });
    test(0, "/updater/1.x/updates", R"([{"package": "pkg.ru", "version": { "code": 999, "text": "V999"}}])",
        [](const auto& rsp) { return testResponsePackage1x(rsp, VERSION_CODE + 1); });
}

TEST_F(ExperimentFixture, FirmwarePercent0)
{
    firmwareRollouts[0].mutable_experiment()->set_percent(0);
    EXPECT_CALL(sCache(), getFirmwareInfo(scope, _))
        .WillRepeatedly(Return(std::make_pair(firmwares, firmwareRollouts)));

    test(0, "/updater/2.x/updates", REQ_2X,
        [](const auto& rsp) { return testResponseFirmware2x(rsp, FW_VERSION); });
    test(0, "/updater/1.x/firmware_updates", REQ_FW_1X,
        [](const auto& rsp) { return testResponseFirmware1x(rsp, FW_VERSION); });
}

TEST_F(ExperimentFixture, FirmwarePercent42)
{
    auto oldFw = firmware;
    oldFw.mutable_id()->set_version(ToString(FW_VERSION - 1));
    firmwareRollouts[0].mutable_experiment()->set_percent(42);
    auto rollout = firmwareRollouts[0];
    *rollout.mutable_firmware_id() = oldFw.id();
    rollout.clear_experiment();
    firmwares[oldFw.id()] = oldFw;
    firmwareRollouts.push_back(rollout);
    EXPECT_CALL(sCache(), getFirmwareInfo(scope, _))
        .WillRepeatedly(Return(std::make_pair(firmwares, firmwareRollouts)));

    test(42, "/updater/2.x/updates", REQ_2X,
        [](const auto& rsp) { return testResponseFirmware2x(rsp, FW_VERSION); });
    test(42, "/updater/1.x/firmware_updates", REQ_FW_1X,
        [](const auto& rsp) { return testResponseFirmware1x(rsp, FW_VERSION); });
}

TEST_F(ExperimentFixture, FirmwarePercent100)
{
    firmwareRollouts[0].mutable_experiment()->set_percent(100);
    EXPECT_CALL(sCache(), getFirmwareInfo(scope, _))
        .WillRepeatedly(Return(std::make_pair(firmwares, firmwareRollouts)));

    test(100, "/updater/2.x/updates", REQ_2X,
        [](const auto& rsp) { return testResponseFirmware2x(rsp, FW_VERSION); });
    test(100, "/updater/1.x/firmware_updates", REQ_FW_1X,
        [](const auto& rsp) { return testResponseFirmware1x(rsp, FW_VERSION); });
}

TEST_F(ExperimentFixture, FirmwareNewStable)
{
    auto newFw = firmware;
    newFw.mutable_id()->set_version(ToString(FW_VERSION + 1));
    firmwareRollouts[0].mutable_experiment()->set_percent(50);
    auto rollout = firmwareRollouts[0];
    *rollout.mutable_firmware_id() = newFw.id();
    rollout.clear_experiment();
    firmwares[newFw.id()] = newFw;
    firmwareRollouts.push_back(rollout);
    EXPECT_CALL(sCache(), getFirmwareInfo(scope, _))
        .WillRepeatedly(Return(std::make_pair(firmwares, firmwareRollouts)));

    test(100, "/updater/2.x/updates", REQ_2X,
        [](const auto& rsp) { return testResponseFirmware2x(rsp, FW_VERSION + 1); });
    test(100, "/updater/1.x/firmware_updates", REQ_FW_1X,
        [](const auto& rsp) { return testResponseFirmware1x(rsp, FW_VERSION + 1); });
    test(0, "/updater/2.x/updates", R"(software {} firmware { properties { key: "ro.build.date.utc" value: "999" }})",
        [](const auto& rsp) { return testResponseFirmware2x(rsp, FW_VERSION + 1); });
    test(0, "/updater/1.x/firmware_updates", R"({"ro.build.date.utc": "999"})",
        [](const auto& rsp) { return testResponseFirmware1x(rsp, FW_VERSION + 1); });
}

} // namespace maps::automotive::updater
