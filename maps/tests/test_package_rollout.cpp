#include <maps/automotive/libs/test_helpers/helpers.h>
#include <maps/automotive/store_internal/lib/dao/headunit_set.h>
#include <maps/automotive/store_internal/lib/dao/firmware.h>
#include <maps/automotive/store_internal/lib/dao/introspection.h>
#include <maps/automotive/store_internal/lib/dao/package.h>
#include <maps/automotive/store_internal/lib/dao/package_rollout.h>
#include <maps/automotive/store_internal/lib/helpers.h>
#include <maps/automotive/store_internal/lib/serialization.h>
#include <maps/automotive/store_internal/tests/helpers.h>
#include <maps/automotive/store_internal/tests/postgres.h>

namespace maps::automotive::store_internal {

using namespace ::testing;

namespace {

static const std::string API_URL = "/store/1.x/rollout/package/config";

void expectSingleRollout(const PackageRollout& rollout) {
    PackageRollouts rollouts;
    auto response = mockGet(API_URL);
    ASSERT_EQ(200, response.status);
    parseFromString(response.body, rollouts);
    ASSERT_EQ(1, rollouts.rollouts_size());
    auto& actualRollout = rollouts.rollouts(0);
    if (actualRollout.has_experiment()) {
        // clear generated fields
        rollouts.mutable_rollouts(0)->mutable_experiment()->clear_id();
        rollouts.mutable_rollouts(0)->mutable_experiment()->clear_salt();
    }
    PROTO_EQ(rollout, actualRollout);
}

} // namespace

struct PackageRolloutApi: public AppContextPostgresFixture
{
    PackageRolloutApi()
        : managerProd(makeUserInfo("manager-prod"))
    {}

    yacare::tests::UserInfoFixture managerProd;
};

TEST_F(PackageRolloutApi, GetAllProto)
{
    http::MockRequest request(http::GET, http::URL("http://localhost" + API_URL));
    request.headers["Accept"] = "application/x-protobuf";
    auto response = yacare::performTestRequest(request);

    PackageRollouts rollouts;
    Y_PROTOBUF_SUPPRESS_NODISCARD rollouts.ParseFromString(TString(response.body));

    ASSERT_EQ(200, response.status);

    ASSERT_EQ(1, rollouts.rollouts_size());
    ASSERT_EQ(0, rollouts.packages_size());

    PROTO_EQ(defaultPackageRollout(), rollouts.rollouts()[0]);
}

TEST_F(PackageRolloutApi, GetAllProtoDetailed)
{
    auto headUnitSet = simpleHeadUnitSet();

    {
        auto txn = dao::makeWriteableTransaction();
        HeadUnitSetDao huSetDao(*txn);
        huSetDao.createOrUpdate(headUnitSet);
        txn->commit();
    }

    http::MockRequest request(
        http::GET,
        http::URL("http://localhost" + API_URL).addParam("details", "true"));
    request.headers["Accept"] = "application/x-protobuf";
    auto response = yacare::performTestRequest(request);

    PackageRollouts rollouts;
    Y_PROTOBUF_SUPPRESS_NODISCARD rollouts.ParseFromString(TString(response.body));

    ASSERT_EQ(200, response.status);

    ASSERT_EQ(1, rollouts.rollouts_size());
    ASSERT_EQ(1, rollouts.packages_size());

    PROTO_EQ(defaultPackageRollout(), rollouts.rollouts()[0]);
    PROTO_EQ(packageWithRollout(), rollouts.packages()[0]);
    PROTO_EQ(defaultHeadUnits(), rollouts.headunits());

    ASSERT_EQ(1, rollouts.headunit_sets_size());
    PROTO_EQ(headUnitSet, rollouts.headunit_sets(0));
}

TEST_F(PackageRolloutApi, PutNew)
{
    PackageRollout ro;
    ro.set_branch("testing");
    *ro.mutable_package_id() = packageNoRollout().id();
    ro.mutable_headunit()->set_type("taxi");
    ro.mutable_headunit()->set_vendor("lada");
    ro.mutable_headunit()->set_model("niva");
    ro.mutable_headunit()->set_mcu("pentium");
    ro.mutable_experiment()->set_percent(10);
    {
        PackageRollouts rollouts;
        auto response = mockGet(API_URL);
        ASSERT_EQ(200, response.status);
        parseFromString(response.body, rollouts);
        ASSERT_FALSE(FindIfPtr(rollouts.rollouts(), [&ro](const auto& rollout) {
            return ro.package_id() == rollout.package_id();
        }));
    }
    ASSERT_EQ(204, mockPut(API_URL, printToString(ro)).status);
    {
        PackageRollouts rollouts;
        auto response = mockGet(API_URL);
        ASSERT_EQ(200, response.status);
        parseFromString(response.body, rollouts);
        auto* found = FindIfPtr(rollouts.rollouts(), [&ro](const auto& rollout) {
            return ro.package_id() == rollout.package_id();
        });
        ASSERT_TRUE(found != nullptr);
        ASSERT_TRUE(found->experiment().has_id());
        ASSERT_TRUE(found->experiment().has_salt());
        ro.mutable_experiment()->set_id(found->experiment().id());
        ro.mutable_experiment()->set_salt(found->experiment().salt());
        PROTO_EQ(ro, *found);
    }
}

TEST_F(PackageRolloutApi, PutExisting)
{
    auto pkg = packageWithRollout();
    pkg.mutable_id()->clear_flavor();
    pkg.set_md5("ccbbaa");
    {
        auto txn = dao::makeWriteableTransaction();
        PackageDao(*txn).create(pkg);
        txn->commit();
    }
    PackageRollouts rollouts;
    auto response = mockGet(API_URL);
    ASSERT_EQ(200, response.status);
    parseFromString(response.body, rollouts);
    ASSERT_EQ(1, rollouts.rollouts_size());

    auto ro = rollouts.rollouts()[0];
    *ro.mutable_package_id() = pkg.id();
    ASSERT_EQ(204, mockPut(API_URL, printToString(ro)).status);
    expectSingleRollout(ro);
}

TEST_F(PackageRolloutApi, PutExperiment)
{
    auto ro = defaultPackageRollout();
    EXPECT_FALSE(ro.has_experiment());
    expectSingleRollout(ro);

    ro.mutable_experiment()->set_percent(34);
    ASSERT_EQ(204, mockPut(API_URL, printToString(ro)).status);
    expectSingleRollout(ro);

    // increase percent
    ro.mutable_experiment()->set_percent(35);
    ASSERT_EQ(204, mockPut(API_URL, printToString(ro)).status);
    expectSingleRollout(ro);

    // decrease percent
    ro.mutable_experiment()->set_percent(33);
    ASSERT_EQ(204, mockPut(API_URL, printToString(ro)).status);
    expectSingleRollout(ro);

    // remove experiment
    ro.clear_experiment();
    ASSERT_EQ(204, mockPut(API_URL, printToString(ro)).status);
    expectSingleRollout(ro);
}

TEST_F(PackageRolloutApi, PutNoPackage)
{
    auto ro = defaultPackageRollout();
    Package::Id id;
    id.set_name("unknown_paket");
    id.set_version_code(44044);
    *ro.mutable_package_id() = id;
    ASSERT_EQ(404, mockGet("/store/1.x/package" + packageId(id)).status);
    ASSERT_EQ(422, mockPut(API_URL, printToString(ro)).status);
}

TEST_F(PackageRolloutApi, PutWildcardRollout)
{
    {
        auto ro = defaultPackageRollout();
        ro.mutable_headunit()->clear_type();
        ASSERT_EQ(204, mockPut(API_URL, printToString(ro)).status);
    }
    {
        auto ro = defaultPackageRollout();
        ro.mutable_headunit()->clear_vendor();
        ASSERT_EQ(400, mockPut(API_URL, printToString(ro)).status);
    }
    {
        auto ro = defaultPackageRollout();
        ro.mutable_headunit()->clear_model();
        ASSERT_EQ(204, mockPut(API_URL, printToString(ro)).status);
    }
    {
        auto ro = defaultPackageRollout();
        ro.mutable_headunit()->clear_vendor();
        ro.mutable_headunit()->clear_model();
        ASSERT_EQ(204, mockPut(API_URL, printToString(ro)).status);
    }
    {
        auto ro = defaultPackageRollout();
        ro.mutable_headunit()->clear_mcu();
        ASSERT_EQ(204, mockPut(API_URL, printToString(ro)).status);
    }
}

TEST_F(PackageRolloutApi, Delete)
{
    PackageRollouts rollouts;
    {
        auto response = mockGet(API_URL);
        ASSERT_EQ(200, response.status);
        parseFromString(response.body, rollouts);
        ASSERT_EQ(1, rollouts.rollouts_size());
    }
    auto ro = rollouts.rollouts()[0];
    ASSERT_EQ(204, mockDelete(API_URL, printToString(ro)).status);
    {
        auto response = mockGet(API_URL);
        ASSERT_EQ(200, response.status);
        parseFromString(response.body, rollouts);
        ASSERT_EQ(0, rollouts.rollouts_size());
    }
}

TEST_F(PackageRolloutApi, NotFound)
{
    {
        PackageRollout ro = defaultPackageRollout();
        ro.set_branch("missing");
        EXPECT_EQ(404, mockDelete(API_URL, printToString(ro)).status);
    }
    {
        PackageRollout ro = defaultPackageRollout();
        ro.mutable_package_id()->set_name("missing");
        EXPECT_EQ(404, mockDelete(API_URL, printToString(ro)).status);
    }
    {
        PackageRollout ro = defaultPackageRollout();
        ro.mutable_package_id()->set_version_code(76521371);
        EXPECT_EQ(404, mockDelete(API_URL, printToString(ro)).status);
    }
    {
        PackageRollout ro = defaultPackageRollout();
        ro.mutable_headunit()->set_type("missing");
        EXPECT_EQ(404, mockDelete(API_URL, printToString(ro)).status);
    }
    {
        PackageRollout ro = defaultPackageRollout();
        ro.mutable_headunit()->set_vendor("missing");
        EXPECT_EQ(404, mockDelete(API_URL, printToString(ro)).status);
    }
    {
        PackageRollout ro = defaultPackageRollout();
        ro.mutable_headunit()->set_model("missing");
        EXPECT_EQ(404, mockDelete(API_URL, printToString(ro)).status);
    }
    {
        PackageRollout ro = defaultPackageRollout();
        ro.mutable_headunit()->set_mcu("missing");
        EXPECT_EQ(404, mockDelete(API_URL, printToString(ro)).status);
    }
}

TEST_F(PackageRolloutApi, GetReturnsMaxVersion)
{
    auto pkg = packageWithRollout();
    auto oldVersion = pkg.id().version_code();
    auto ro = defaultPackageRollout();
    {
        auto response = mockGet(API_URL);
        ASSERT_EQ(200, response.status);
        PackageRollouts rollouts;
        parseFromString(response.body, rollouts);
        for (const auto& ro: rollouts.rollouts()) {
            if (ro.package_id().name() == pkg.id().name()) {
                ASSERT_EQ(oldVersion, ro.package_id().version_code());
            }
        }
    }
    { // add package with smaller version
        pkg.mutable_id()->set_version_code(oldVersion - 1);
        pkg.set_md5("minus1");
        *ro.mutable_package_id() = pkg.id();
        auto txn = dao::makeWriteableTransaction();
        PackageDao(*txn).create(pkg);
        PackageRolloutDao(*txn).upsert(ro);
        txn->commit();
    }
    { // returned version is still oldVersion
        auto response = mockGet(API_URL);
        ASSERT_EQ(200, response.status);
        PackageRollouts rollouts;
        parseFromString(response.body, rollouts);
        auto found = false;
        for (const auto& ro: rollouts.rollouts()) {
            if (ro.package_id().name() == pkg.id().name()) {
                ASSERT_EQ(oldVersion, ro.package_id().version_code());
                ASSERT_FALSE(found);
                found = true;
            }
        }
    }
    { // add package with larger version
        pkg.mutable_id()->set_version_code(oldVersion + 1);
        pkg.set_md5("plus1");
        *ro.mutable_package_id() = pkg.id();
        auto txn = dao::makeWriteableTransaction();
        PackageDao(*txn).create(pkg);
        PackageRolloutDao(*txn).upsert(ro);
        txn->commit();
    }
    { // returned version is now oldVersion + 1
        auto response = mockGet(API_URL);
        ASSERT_EQ(200, response.status);
        PackageRollouts rollouts;
        parseFromString(response.body, rollouts);
        auto found = false;
        for (const auto& ro: rollouts.rollouts()) {
            if (ro.package_id().name() == pkg.id().name()) {
                ASSERT_EQ(oldVersion + 1, ro.package_id().version_code());
                ASSERT_FALSE(found);
                found = true;
            }
        }
    }
    { // add package with larger version and another flavor
        pkg.mutable_id()->set_version_code(oldVersion + 2);
        pkg.mutable_id()->set_flavor("another_flavor");
        pkg.set_md5("plus2_another_flavor");
        *ro.mutable_package_id() = pkg.id();
        auto txn = dao::makeWriteableTransaction();
        PackageDao(*txn).create(pkg);
        PackageRolloutDao(*txn).upsert(ro);
        txn->commit();
    }
    { // returned version is now oldVersion + 2
        auto response = mockGet(API_URL);
        ASSERT_EQ(200, response.status);
        PackageRollouts rollouts;
        parseFromString(response.body, rollouts);
        auto found = false;
        for (const auto& ro: rollouts.rollouts()) {
            if (ro.package_id().name() == pkg.id().name()) {
                ASSERT_EQ(oldVersion + 2, ro.package_id().version_code());
                ASSERT_EQ(pkg.id().flavor(), ro.package_id().flavor());
                ASSERT_FALSE(found);
                found = true;
            }
        }
    }
    { // add package with smaller version and with some dependency
        auto txn = dao::makeWriteableTransaction();

        Feature::Id feature;
        feature.set_name("some_feature");
        feature.set_version_major(10);
        feature.set_version_minor(20);

        pkg.mutable_id()->set_version_code(oldVersion);
        pkg.mutable_id()->set_flavor("dependent_on_feature");
        pkg.set_md5("dependent_on_feature");
        *pkg.mutable_metadata()->add_depends()->mutable_feature() = feature;

        auto fw = firmwareWithRollout();
        *fw.mutable_metadata()->add_provides() = feature;
        FirmwareDao(*txn).update(fw);

        *ro.mutable_package_id() = pkg.id();
        PackageDao(*txn).create(pkg);
        PackageRolloutDao(*txn).upsert(ro);
        txn->commit();
    }
    { // returns both oldVersion with dependency and maximum version
        auto response = mockGet(API_URL);
        ASSERT_EQ(200, response.status);
        PackageRollouts rollouts;
        parseFromString(response.body, rollouts);
        std::vector<Package::Id> pkgIds;
        for (const auto& ro: rollouts.rollouts()) {
            if (ro.package_id().name() == pkg.id().name()) {
                pkgIds.push_back(ro.package_id());
            }
        }
        Sort(pkgIds);
        ASSERT_EQ(2u, pkgIds.size());
        ASSERT_EQ(pkg.id(), pkgIds.at(0));
        ASSERT_EQ(oldVersion, pkgIds.at(0).version_code());
        ASSERT_EQ(oldVersion + 2, pkgIds.at(1).version_code());
    }
}

TEST_F(PackageRolloutApi, AtMostOneExperimentForPackageName)
{
    PackageRollout ro = defaultPackageRollout();
    auto pkg = packageNoRollout();
    ASSERT_EQ(ro.package_id().name(), pkg.id().name());

    *ro.mutable_package_id() = pkg.id();
    ro.mutable_experiment()->set_percent(10);
    ASSERT_EQ(204, mockPut(API_URL, printToString(ro)).status);

    // have 2 rollouts for package 'rupaket'
    auto response = mockGet(API_URL);
    ASSERT_EQ(200, response.status);
    PackageRollouts rollouts;
    parseFromString(response.body, rollouts);
    auto* rolloutNoExp = FindIfPtr(rollouts.rollouts(), [](const auto& rollout) {
        return rollout.package_id() == packageWithRollout().id();
    });
    ASSERT_TRUE(rolloutNoExp);
    ASSERT_FALSE(rolloutNoExp->has_experiment());
    auto* rolloutWithExp = FindIfPtr(rollouts.rollouts(), [&pkg](const auto& rollout) {
        return rollout.package_id() == pkg.id();
    });
    ASSERT_TRUE(rolloutWithExp);
    ASSERT_TRUE(rolloutWithExp->has_experiment());

    *ro.mutable_package_id() = packageWithRollout().id();
    ro.mutable_experiment()->set_percent(20);
    ASSERT_EQ(422, mockPut(API_URL, printToString(ro)).status);
}

TEST_F(PackageRolloutApi, AtMostOneExperimentByMask)
{
    auto pkg = packageNoRollout();
    PackageRollout roTaxi;
    *roTaxi.mutable_package_id() = pkg.id();
    roTaxi.set_branch("AtMostOneExperimentByMask");
    roTaxi.mutable_headunit()->set_type("taxi");
    roTaxi.mutable_experiment()->set_percent(11);

    ASSERT_EQ(204, mockPut(API_URL, printToString(roTaxi)).status);

    PackageRollout roTaxiCaska = roTaxi;
    roTaxiCaska.mutable_headunit()->set_mcu("caska");
    ASSERT_EQ(422, mockPut(API_URL, printToString(roTaxiCaska)).status);

    PackageRollout roCaska = roTaxiCaska;
    roCaska.mutable_headunit()->clear_type();
    ASSERT_EQ(422, mockPut(API_URL, printToString(roCaska)).status);

    PackageRollout roTaxiCaskaFF = roTaxiCaska;
    roTaxiCaskaFF.mutable_headunit()->set_vendor("ford");
    roTaxiCaskaFF.mutable_headunit()->set_model("focus");
    ASSERT_EQ(422, mockPut(API_URL, printToString(roTaxiCaskaFF)).status);

    PackageRollout roPersonal = roTaxi;
    roPersonal.mutable_headunit()->set_type("personal");
    ASSERT_EQ(204, mockPut(API_URL, printToString(roPersonal)).status);

    // delete taxi-*-*-* and create taxi-ford-focus-caska first
    ASSERT_EQ(204, mockDelete(API_URL, printToString(roTaxi)).status);
    ASSERT_EQ(204, mockDelete(API_URL, printToString(roPersonal)).status);

    ASSERT_EQ(204, mockPut(API_URL, printToString(roTaxiCaskaFF)).status);
    ASSERT_EQ(422, mockPut(API_URL, printToString(roTaxiCaska)).status);
    ASSERT_EQ(422, mockPut(API_URL, printToString(roCaska)).status);
    ASSERT_EQ(422, mockPut(API_URL, printToString(roTaxi)).status);
    ASSERT_EQ(204, mockPut(API_URL, printToString(roPersonal)).status);
}

TEST_F(PackageRolloutApi, FlavorIsOverridenByPut)
{
    auto ro = defaultPackageRollout();
    { // check we have default rollout with some flavor
        auto response = mockGet(API_URL);
        ASSERT_EQ(200, response.status);
        PackageRollouts rollouts;
        parseFromString(response.body, rollouts);
        ASSERT_EQ(1, rollouts.rollouts_size());
        ASSERT_EQ(printToString(ro), printToString(rollouts.rollouts(0)));
        ASSERT_EQ("flavored", ro.package_id().flavor());
    }
    Package pkg = packageWithRollout();
    { // add package with new flavor
        auto txn = dao::makeWriteableTransaction();
        *pkg.mutable_id() = ro.package_id();
        pkg.mutable_id()->set_flavor("new-flavor");
        pkg.set_md5("new_md5");
        PackageDao(*txn).create(pkg);
        txn->commit();
    }
    // add rollout with only changed flavor
    ro.mutable_package_id()->set_flavor(pkg.id().flavor());
    ASSERT_EQ(204, mockPut(API_URL, printToString(ro)).status);
    { // check we have only one rollout with changed flavor
        auto response = mockGet(API_URL);
        ASSERT_EQ(200, response.status);
        PackageRollouts rollouts;
        parseFromString(response.body, rollouts);
        ASSERT_EQ(1, rollouts.rollouts_size());
        ASSERT_EQ(printToString(ro), printToString(rollouts.rollouts(0)));
        ASSERT_EQ("new-flavor", ro.package_id().flavor());
    }
}

TEST_F(PackageRolloutApi, FlavoredToRelease)
{
    auto ro = defaultPackageRollout();
    ro.set_branch("release");
    ASSERT_EQ("flavored", ro.package_id().flavor());
    ASSERT_EQ(204, mockPut(API_URL, printToString(ro)).status);
}

TEST_F(PackageRolloutApi, NullEmptyFlavor)
{
    // delete the only rollout existing in DB
    ASSERT_EQ(204, mockDelete(API_URL, printToString(defaultPackageRollout())).status);
    // 
    { // now we have no rollouts in DB
        auto response = mockGet(API_URL);
        ASSERT_EQ(200, response.status);
        PackageRollouts rollouts;
        parseFromString(response.body, rollouts);
        ASSERT_EQ(0, rollouts.rollouts_size());
    }

    auto pkg = packageNoRollout();
    ASSERT_FALSE(pkg.id().has_flavor());

    PackageRollout ro;
    *ro.mutable_package_id() = pkg.id();
    ro.mutable_package_id()->set_flavor("");
    ro.set_branch("daily");
    ro.mutable_headunit()->set_type("oem");

    ASSERT_EQ(204, mockPut(API_URL, printToString(ro)).status);
    ro.mutable_package_id()->clear_flavor();
    ASSERT_FALSE(ro.package_id().has_flavor());
    { // check rollout was stored with null flavor
        auto response = mockGet(API_URL);
        ASSERT_EQ(200, response.status);
        PackageRollouts rollouts;
        parseFromString(response.body, rollouts);
        ASSERT_EQ(1, rollouts.rollouts_size());
        PROTO_EQ(ro, rollouts.rollouts(0));
    }
    ASSERT_EQ(204, mockPut(API_URL, printToString(ro)).status);
    { // check we update the same rollout
        auto response = mockGet(API_URL);
        ASSERT_EQ(200, response.status);
        PackageRollouts rollouts;
        parseFromString(response.body, rollouts);
        ASSERT_EQ(1, rollouts.rollouts_size());
        PROTO_EQ(ro, rollouts.rollouts(0));
    }

    ro.mutable_package_id()->set_flavor("");
    ASSERT_EQ(204, mockDelete(API_URL, printToString(ro)).status);
    { // check we have no rollouts in DB
        auto response = mockGet(API_URL);
        ASSERT_EQ(200, response.status);
        PackageRollouts rollouts;
        parseFromString(response.body, rollouts);
        ASSERT_EQ(0, rollouts.rollouts_size());
    }
}

TEST_F(PackageRolloutApi, PutNewWithHeadUnitSet)
{
    PackageRollout ro;
    ro.set_branch("testing");
    *ro.mutable_package_id() = packageNoRollout().id();
    ro.mutable_headunit()->set_type("taxi");
    ro.mutable_headunit()->set_vendor("lada");
    ro.mutable_headunit()->set_model("niva");
    ro.mutable_headunit()->set_mcu("pentium");
    ro.mutable_experiment()->set_percent(10);
    ro.set_headunit_set_name("hu_set_1");
    auto roBody = printToString(ro);

    // Should fail since headunit set 'hu_set_1' doesn't exist yet
    ASSERT_EQ(422, mockPut(API_URL, roBody).status);

    {
        HeadUnitSets sets;
        auto set = sets.add_headunit_sets();
        set->set_name("hu_set_1");
        ASSERT_EQ(
            204, mockPut("/store/1.x/headunit_set", printToString(sets)).status);
    }

    ASSERT_EQ(204, mockPut(API_URL, roBody).status);

    {
        PackageRollouts rollouts;
        auto response = mockGet(API_URL);
        ASSERT_EQ(200, response.status);
        parseFromString(response.body, rollouts);
        auto* found = FindIfPtr(rollouts.rollouts(), [&ro](const auto& rollout) {
            return ro.package_id() == rollout.package_id();
        });
        ASSERT_TRUE(found != nullptr);
        ASSERT_TRUE(found->experiment().has_id());
        ASSERT_TRUE(found->experiment().has_salt());
        ro.mutable_experiment()->set_id(found->experiment().id());
        ro.mutable_experiment()->set_salt(found->experiment().salt());
        PROTO_EQ(ro, *found);
    }
}

TEST_F(PackageRolloutApi, forbidden)
{
    {
        auto ro = defaultPackageRollout();
        ro.set_branch("internal");
        for (const std::string& user: {"key-manager-prod", "key-manager", "viewer-victor"}) {
            yacare::tests::UserInfoFixture fixture{makeUserInfo(user)};
            ASSERT_EQ(401, mockPut(API_URL, printToString(ro)).status);
            ASSERT_EQ(401, mockDelete(API_URL, printToString(ro)).status);
        }
    }
    {
        auto ro = defaultPackageRollout();
        ro.set_branch("release");
        for (const std::string& user: {"key-manager-prod", "key-manager", "viewer-victor", "manager"}) {
            yacare::tests::UserInfoFixture fixture{makeUserInfo(user)};
            ASSERT_EQ(401, mockPut(API_URL, printToString(ro)).status);
            ASSERT_EQ(401, mockDelete(API_URL, printToString(ro)).status);
        }
    }
}

} // maps::automotive::store_internal
