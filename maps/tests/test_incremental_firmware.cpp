#include <google/protobuf/text_format.h>
#include <maps/automotive/libs/test_helpers/helpers.h>
#include <maps/automotive/libs/test_helpers/serialization.h>
#include <maps/automotive/updater/config/maintenance.pb.h>
#include <maps/automotive/updater/test_helpers/mocks.h>

namespace maps::automotive::updater {

using namespace ::testing;

using Incremental = Fixture;

TEST_F(Incremental, EnableDisable)
{
    masi::Firmware incFw;
    incFw.mutable_id()->set_name("incremental");
    incFw.mutable_id()->set_version("202");
    incFw.mutable_id()->set_from_version("101");

    FirmwareMap firmwares;
    firmwares[incFw.id()] = incFw;

    masi::Scope scope;
    scope.set_branch("release");
    scope.mutable_headunit()->set_type("taxi");
    scope.mutable_headunit()->set_mcu("astar");
    scope.mutable_headunit()->set_vendor("vendor");
    scope.mutable_headunit()->set_model("m2");

    std::vector<masi::FirmwareRollout> rollouts;
    masi::FirmwareRollout rollout;
    rollout.set_branch("release");
    *rollout.mutable_headunit() = scope.headunit();
    *rollout.mutable_firmware_id() = incFw.id();
    rollouts.push_back(rollout);

    EXPECT_CALL(sCache(), getFirmwareInfo(_, _))
        .WillRepeatedly(Return(std::make_pair(firmwares, rollouts)));

    auto req = "/updater/2.x/updates"
                "?" "type=taxi"
                "&" "mcu=astar"
                "&" "vendor=vendor"
                "&" "model=m2"
                "&" "lang=ru_RU"
                "&" "headid=ddeeffaabbcc";
    auto installed101 = R"(software {} firmware { properties { key: "ro.build.date.utc" value: "101" }})";
    { // enable incremental firmware updates
        auto rsp = mockPatch("/config", "updates { enable_incremental_firmware: true }");
        ASSERT_EQ(200, rsp.status);
    }
    {
        auto rsp = mockPost(req, installed101);
        ASSERT_EQ(200, rsp.status);
        proto::UpdateResponse response;
        TString body(rsp.body);
        ASSERT_TRUE(NProtoBuf::TextFormat::ParseFromString(body, &response));
        ASSERT_EQ(1, response.firmware().updates_size());
        EXPECT_EQ("incremental", response.firmware().updates(0).name());
        EXPECT_EQ("202", response.firmware().updates(0).version());
    }
    { // disable incremental firmware updates
        auto rsp = mockPatch("/config", "updates { enable_incremental_firmware: false }");
        ASSERT_EQ(200, rsp.status);
    }
    {
        auto rsp = mockPost(req, installed101);
        ASSERT_EQ(200, rsp.status);
        proto::UpdateResponse response;
        TString body(rsp.body);
        ASSERT_TRUE(NProtoBuf::TextFormat::ParseFromString(body, &response));
        ASSERT_EQ(0, response.firmware().updates_size());
    }
}

}
