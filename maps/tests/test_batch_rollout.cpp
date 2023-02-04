#include <maps/automotive/libs/test_helpers/helpers.h>
#include <maps/automotive/store_internal/lib/serialization.h>
#include <maps/automotive/store_internal/tests/helpers.h>
#include <maps/automotive/store_internal/tests/postgres.h>

namespace maps::automotive::store_internal {

static const std::string API_URL = "/store/1.x/rollout/config";

namespace {

void checkNoRollouts()
{
    { // check all package rollouts are deleted
        PackageRollouts rollouts;
        auto response = mockGet("/store/1.x/rollout/package/config");
        ASSERT_EQ(200, response.status);
        parseFromString(response.body, rollouts);
        ASSERT_EQ(0, rollouts.rollouts_size());
    }
    { // check all firmware rollouts are deleted
        FirmwareRollouts rollouts;
        auto response = mockGet("/store/1.x/rollout/firmware/config");
        ASSERT_EQ(200, response.status);
        parseFromString(response.body, rollouts);
        ASSERT_EQ(0, rollouts.rollouts_size());
    }
}

void clearDefaultRollouts()
{
    Rollouts toDelete;
    *toDelete.add_package_rollouts() = defaultPackageRollout();
    *toDelete.add_firmware_rollouts() = defaultFirmwareRollout();
    EXPECT_EQ(204, mockDelete(API_URL, printToString(toDelete)).status);
    checkNoRollouts();
}

HeadUnitSelector defaultHeadunit()
{
    HeadUnitSelector hu;
    hu.set_type("oem");
    hu.set_mcu("astar");
    hu.set_vendor("seat");
    hu.set_model("cordoba");
    return hu;
}

} // namespace

struct BatchRolloutApi: public AppContextPostgresFixture
{
    BatchRolloutApi()
        : managerProd(makeUserInfo("manager-prod"))
    {}

    yacare::tests::UserInfoFixture managerProd;
};

TEST_F(BatchRolloutApi, PutGetDelete)
{
    clearDefaultRollouts();

    Rollouts insertPkg;
    auto* roPkg1 = insertPkg.add_package_rollouts();
    *roPkg1->mutable_package_id() = packageNoRollout().id();
    roPkg1->set_branch("release");
    roPkg1->mutable_headunit()->set_type("oem");
    roPkg1->mutable_headunit()->set_mcu("caska");
    EXPECT_EQ(204, mockPut(API_URL, printToString(insertPkg)).status);

    Rollouts insertFw;
    auto* roFw1 = insertFw.add_firmware_rollouts();
    *roFw1->mutable_firmware_id() = firmwareNoRollout().id();
    roFw1->set_branch("daily");
    roFw1->mutable_headunit()->set_type("aftermarket-mts");
    roFw1->mutable_headunit()->set_vendor("no");
    roFw1->mutable_headunit()->set_model("rollout");
    roFw1->mutable_headunit()->set_mcu("caska_t3");
    EXPECT_EQ(204, mockPut(API_URL, printToString(insertFw)).status);

    Rollouts insertExtra;
    *insertExtra.add_package_rollouts() = insertPkg.package_rollouts(0);
    *insertExtra.add_firmware_rollouts() = insertFw.firmware_rollouts(0);

    auto* roPkg2 = insertExtra.add_package_rollouts();
    *roPkg2->mutable_package_id() = packageWithRollout().id();
    roPkg2->set_branch("testing");
    roPkg2->mutable_headunit()->set_type("personal");
    roPkg2->mutable_headunit()->set_vendor("ford");

    auto* roFw2 = insertExtra.add_firmware_rollouts();
    *roFw2->mutable_firmware_id() = firmwareWithRollout().id();
    roFw2->set_branch("weekly");
    roFw2->mutable_headunit()->set_type("aftermarket-mts");
    roFw2->mutable_headunit()->set_vendor("with");
    roFw2->mutable_headunit()->set_model("rollout");
    roFw2->mutable_headunit()->set_mcu("caska_t3");

    { // insert the same package and firmware with new package and firmware
        http::MockRequest request(http::PUT, http::URL("http://localhost" + API_URL));
        request.headers["Content-Type"] = "application/x-protobuf";
        TString body;
        ASSERT_TRUE(insertExtra.SerializeToString(&body));
        request.body = body;
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(204, response.status);
    }

    { // check package rollouts
        PackageRollouts rollouts;
        auto response = mockGet("/store/1.x/rollout/package/config");
        ASSERT_EQ(200, response.status);
        parseFromString(response.body, rollouts);
        ASSERT_EQ(2, rollouts.rollouts_size());
        PROTO_EQ(*roPkg1, *FindIfPtr(rollouts.rollouts(), [](const auto& ro) {
            return ro.branch() == "release";
        }));
        PROTO_EQ(*roPkg2, *FindIfPtr(rollouts.rollouts(), [](const auto& ro) {
            return ro.branch() == "testing";
        }));
    }
    { // check firmware rollouts
        FirmwareRollouts rollouts;
        auto response = mockGet("/store/1.x/rollout/firmware/config");
        ASSERT_EQ(200, response.status);
        parseFromString(response.body, rollouts);
        ASSERT_EQ(2, rollouts.rollouts_size());
        PROTO_EQ(*roFw1, *FindIfPtr(rollouts.rollouts(), [](const auto& ro) {
            return ro.branch() == "daily";
        }));
        PROTO_EQ(*roFw2, *FindIfPtr(rollouts.rollouts(), [](const auto& ro) {
            return ro.branch() == "weekly";
        }));
    }

    // delete all rollouts
    EXPECT_EQ(204, mockDelete(API_URL, printToString(insertExtra)).status);
    checkNoRollouts();
}

TEST_F(BatchRolloutApi, EmptyPutDelete)
{
    Rollouts empty;
    EXPECT_EQ(400, mockPut(API_URL, printToString(empty)).status);
    EXPECT_EQ(400, mockDelete(API_URL, printToString(empty)).status);
}

TEST_F(BatchRolloutApi, PackageNotFound)
{
    clearDefaultRollouts();

    Rollouts toInsert;

    Package::Id unknown;
    unknown.set_name("unknown");
    unknown.set_version_code(345);
    auto* roPkg = toInsert.add_package_rollouts();
    *roPkg->mutable_package_id() = unknown;
    roPkg->set_branch("release");
    roPkg->mutable_headunit()->set_type("oem");
    roPkg->mutable_headunit()->set_mcu("caska");

    auto* roFw = toInsert.add_firmware_rollouts();
    *roFw->mutable_firmware_id() = firmwareNoRollout().id();
    roFw->set_branch("daily");
    *roFw->mutable_headunit() = defaultHeadunit();

    EXPECT_EQ(422, mockPut(API_URL, printToString(toInsert)).status);
    checkNoRollouts();
}

TEST_F(BatchRolloutApi, FirmwareNotFound)
{
    clearDefaultRollouts();

    Rollouts toInsert;

    auto* roPkg = toInsert.add_package_rollouts();
    *roPkg->mutable_package_id() = packageNoRollout().id();
    roPkg->set_branch("release");
    roPkg->mutable_headunit()->set_type("oem");
    roPkg->mutable_headunit()->set_mcu("caska");

    Firmware::Id unknown;
    unknown.set_name(FW_NO_ROLLOUT);
    unknown.set_version("unknown");
    auto* roFw = toInsert.add_firmware_rollouts();
    *roFw->mutable_firmware_id() = unknown;
    roFw->set_branch("daily");
    roFw->mutable_headunit()->set_type("aftermarket-mts");
    roFw->mutable_headunit()->set_vendor("no");
    roFw->mutable_headunit()->set_model("rollout");
    roFw->mutable_headunit()->set_mcu("caska_t3");

    EXPECT_EQ(422, mockPut(API_URL, printToString(toInsert)).status);
    checkNoRollouts();
}

TEST_F(BatchRolloutApi, forbidden)
{
    Rollouts ro;
    *ro.add_package_rollouts() = defaultPackageRollout();

    for (const std::string& user: {"key-manager-prod", "key-manager", "viewer-victor"}) {
        yacare::tests::UserInfoFixture fixture{makeUserInfo(user)};
        ASSERT_EQ(401, mockPut(API_URL, printToString(ro)).status);
        ASSERT_EQ(401, mockDelete(API_URL, printToString(ro)).status);
    }

    ro.mutable_package_rollouts(0)->set_branch("release");
    yacare::tests::UserInfoFixture fixture{makeUserInfo("manager")};
    ASSERT_EQ(401, mockPut(API_URL, printToString(ro)).status);
    ASSERT_EQ(401, mockDelete(API_URL, printToString(ro)).status);
}

} // namespace maps::automotive::store_internal
