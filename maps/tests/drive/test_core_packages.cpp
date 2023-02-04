#include <library/cpp/testing/unittest/env.h>
#include <maps/automotive/libs/test_helpers/helpers.h>
#include <maps/automotive/updater/lib/names.h>
#include <maps/automotive/updater/lib/store_internal.h>
#include <maps/automotive/updater/test_helpers/mocks.h>
#include <maps/automotive/updater/yacare/drive.h>

namespace maps::automotive::updater {

using namespace ::testing;

namespace {

struct CorePackages : public Fixture
{
    CorePackages() : Fixture()
    {
        ON_CALL(drive(), getSessionTypeFor(_))
            .WillByDefault(Return(drive::SessionType::User));

        taggedApps[CORE_TAG].emplace(LAUNCHER_PACKAGE);
        taggedApps[CORE_TAG].emplace("ru.yandex.yap.hal.services");
        taggedApps[CORE_TAG].emplace("ru.yandex.yandexnavi");
    }

    HeadunitInfo info;
    proto::UpdateResponse emptyResponse;
    TagMap taggedApps;
};

} // namespace

TEST_F(CorePackages, OnlyLauncher)
{
    EXPECT_CALL(
        drive(),
        scheduleServiceSession(_, Gt(TInstant::Now()), TDuration::Minutes(15)));

    proto::UpdateResponse response;
    auto* update = response.mutable_software()->add_updates();
    auto* pkg = update->add_packages();
    pkg->set_name(LAUNCHER_PACKAGE);
    pkg->mutable_version()->set_code(10000);
    drive::processUpdates(
        response, info, drive::HEAD_DOES_NOT_SUPPORT_UPDATE_CONTROLS);
    PROTO_EQ(emptyResponse, response);
}

TEST_F(CorePackages, AllCorePackages)
{
    EXPECT_CALL(
        drive(),
        scheduleServiceSession(_, Gt(TInstant::Now()), TDuration::Minutes(15)));

    proto::UpdateResponse response;
    auto* update = response.mutable_software()->add_updates();
    for (const auto& package: taggedApps[CORE_TAG]) {
        auto* pkg = update->add_packages();
        pkg->set_name(package);
        pkg->mutable_version()->set_code(20000);
    }
    drive::processUpdates(
        response, info, drive::HEAD_DOES_NOT_SUPPORT_UPDATE_CONTROLS);
    PROTO_EQ(emptyResponse, response);
}

TEST_F(CorePackages, AllCorePackagesAndSomePackage)
{
    EXPECT_CALL(
        drive(),
        scheduleServiceSession(_, Gt(TInstant::Now()), TDuration::Minutes(15)));

    proto::UpdateResponse response;
    auto* update = response.mutable_software()->add_updates();
    for (const auto& package: taggedApps[CORE_TAG]) {
        auto* pkg = update->add_packages();
        pkg->set_name(package);
        pkg->mutable_version()->set_code(30000);
    }
    auto* otherUpdate = response.mutable_software()->add_updates();
    auto* pkg = otherUpdate->add_packages();
    pkg->set_name("pkg.some.am.i");
    pkg->mutable_version()->set_code(30000);

    drive::processUpdates(
        response, info, drive::HEAD_DOES_NOT_SUPPORT_UPDATE_CONTROLS);

    proto::UpdateResponse expected;
    auto* expectedUpdate = expected.mutable_software()->add_updates();
    *expectedUpdate = *otherUpdate;

    PROTO_EQ(expected, response);
}

TEST_F(CorePackages, AllCorePackagesWithoutLauncher)
{
    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    proto::UpdateResponse response;
    auto* update = response.mutable_software()->add_updates();
    for (const auto& package: taggedApps[CORE_TAG]) {
        if (package == LAUNCHER_PACKAGE) {
            continue;
        }
        auto* pkg = update->add_packages();
        pkg->set_name(package);
        pkg->mutable_version()->set_code(20000);
    }
    auto expected = response;
    drive::processUpdates(
        response, info, drive::HEAD_DOES_NOT_SUPPORT_UPDATE_CONTROLS);
    PROTO_EQ(expected, response);
}

} // namespace maps::automotive::update
