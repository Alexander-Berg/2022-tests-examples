#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <maps/fastcgi/meta/lib/version_composer.h>

#include <string>

namespace maps::meta::tests {

namespace {

std::string getTestDataPath(const std::string& filename)
{
    return ArcadiaSourceRoot()
        + "/maps/fastcgi/meta/tests/data/"
        + filename;
}

} // namespace

Y_UNIT_TEST_SUITE(version_composer) {

Y_UNIT_TEST(from_dir)
{
    auto versionComposer = VersionComposer::fromDirs({getTestDataPath("version_patches"), "unknown_dir"});
    EXPECT_EQ(versionComposer.patch("map", "1-2-3"), "1-2-3-b123");
    EXPECT_EQ(versionComposer.patch("skl", "1-2-3"), "1-2-3-b456");
    EXPECT_EQ(versionComposer.patch("trf", "1-2-3"), "1-2-3");
}

} // Y_UNIT_TEST_SUITE(version_composer)

} // namespace maps::meta::tests
