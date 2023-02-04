#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>
#include <maps/automotive/libs/test_helpers/serialization.h>
#include <maps/automotive/store_internal/proto/store_internal.pb.h>
#include <maps/automotive/updater/lib/names.h>
#include <maps/automotive/updater/lib/updates.h>
#include <maps/automotive/updater/test_helpers/mocks.h>

namespace maps::automotive::updater {

TEST_F(Fixture, Kinopoisk)
{
    HeadunitInfo info;
    TagMap taggedApps;

    masi::Package latestKinopoisk;
    latestKinopoisk.mutable_metadata()->set_app_name("ru.kinopoisk.yandex.auto");

    proto::PackageVersion installedServicesVersion;
    installedServicesVersion.set_code(0);

    proto::PackageVersion installedLauncherVersion;
    installedLauncherVersion.set_code(0);
    auto software = findUpdates(
        info,
        {std::nullopt, {
            {LAUNCHER_PACKAGE, {installedLauncherVersion}},
            {"ru.yandex.yap.hal.services", {installedServicesVersion}},
        }, {}, {}},
        {{{latestKinopoisk, std::nullopt}}, {}}, taggedApps);
    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/Kinopoisk_empty.prototxt")),
        printToString(software));

    installedServicesVersion.set_code(0);
    installedLauncherVersion.set_code(9815);
    software = findUpdates(
        info,
        {std::nullopt, {
            {LAUNCHER_PACKAGE, {installedLauncherVersion}},
            {"ru.yandex.yap.hal.services", {installedServicesVersion}},
        }, {}, {}},
        {{{latestKinopoisk, std::nullopt}}, {}}, taggedApps);
    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/Kinopoisk_empty.prototxt")),
        printToString(software));

    installedServicesVersion.set_code(436);
    installedLauncherVersion.set_code(0);
    software = findUpdates(
        info,
        {std::nullopt, {
            {LAUNCHER_PACKAGE, {installedLauncherVersion}},
            {"ru.yandex.yap.hal.services", {installedServicesVersion}},
        }, {}, {}},
        {{{latestKinopoisk, std::nullopt}}, {}}, taggedApps);
    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/Kinopoisk_empty.prototxt")),
        printToString(software));

    installedServicesVersion.set_code(436);
    installedLauncherVersion.set_code(9815);
    software = findUpdates(
        info,
        {std::nullopt, {
            {LAUNCHER_PACKAGE, {installedLauncherVersion}},
            {"ru.yandex.yap.hal.services", {installedServicesVersion}},
        }, {}, {}},
        {{{latestKinopoisk, std::nullopt}}, {}}, taggedApps);
    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/Kinopoisk_nonempty.prototxt")),
        printToString(software));
}

TEST_F(Fixture, Split)
{
    HeadunitInfo info;
    TagMap taggedApps;

    masi::Package latestLauncher;
    latestLauncher.mutable_metadata()->set_app_name(LAUNCHER_PACKAGE);
    latestLauncher.mutable_id()->set_version_code(1);
    latestLauncher.set_size(2);

    masi::Package latestCoagent;
    latestCoagent.mutable_metadata()->set_app_name("com.coagent.service");
    latestCoagent.set_size(4);

    masi::Package other;
    other.mutable_metadata()->set_app_name("other");
    other.set_size(1);

    proto::PackageVersion installedServicesVersion;
    installedServicesVersion.set_code(0);

    proto::PackageVersion installedLauncherVersion;
    installedLauncherVersion.set_code(0);

    taggedApps[GROUPABLE_TAG].insert(LAUNCHER_PACKAGE);
    taggedApps[GROUPABLE_TAG].insert("com.coagent.service");
    taggedApps[CORE_TAG].insert(LAUNCHER_PACKAGE);

    info.scope.mutable_headunit()->set_type(AFTERMARKET);
    auto software = findUpdates(
        info,
        {std::nullopt, {
            {LAUNCHER_PACKAGE, {installedLauncherVersion}},
            {"ru.yandex.yap.hal.services", {installedServicesVersion}},
        }, {}, {}},
        {{{latestCoagent, std::nullopt}, {latestLauncher, std::nullopt}, {other, std::nullopt}}, {}}, taggedApps);
    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/Split_aftermarket.prototxt")),
        printToString(software));

    info.scope.mutable_headunit()->set_type(CARSHARING);
    software = findUpdates(
        info,
        {std::nullopt, {
            {LAUNCHER_PACKAGE, {installedLauncherVersion}},
            {"ru.yandex.yap.hal.services", {installedServicesVersion}},
        }, {}, {}},
        {{{latestCoagent, std::nullopt}, {latestLauncher, std::nullopt}, {other, std::nullopt}}, {}}, taggedApps);
    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/Split_carsharing.prototxt")),
        printToString(software));
}

} // namespace maps::automotive::updater
