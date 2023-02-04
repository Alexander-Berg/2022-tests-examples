#include <library/cpp/testing/gtest/gtest.h>
#include <maps/automotive/store_internal/lib/helpers.h>
#include <maps/automotive/store_internal/tests/mocks.h>

namespace maps::automotive::store_internal {

TEST_F(AppContextFixture, firmwareHelpers)
{
    Firmware::Id fw;
    fw.set_name("name");
    fw.set_version("version");

    EXPECT_EQ("name/version", firmwareId(fw));
    EXPECT_EQ("firmware/images/name/version", firmwarePath(fw));
    EXPECT_EQ("https://external-bucket.external-host/firmware/images/name/version", firmwareUrl(fw));
}

TEST_F(AppContextFixture, parseFwBuildMetadata)
{
    struct {
        std::string buildMetadata;
        std::string expectedName;
        std::string expectedVersion;
    } tests[] = {
        {"", "-unknown--", "0"},
        {"irrelevant=\n=values", "-unknown--", "0"},
        {"some_line\n"
         "ro.yap.auto.vendor=kia\n"
         "other=line\n"
         "ro.yap.auto.model=rio\n"
         "ro.yap.auto.mcu=caska_t3\n"
         "ro.yap.auto.type=carsharing\n"
         "ro.build.date.utc=1234",
         "carsharing-kia-rio-caska_t3",
         "1234"},
        {"some_line\n"
         "ro.yap.auto.vendor=kia\n"
         "other=line\n"
         "ro.yap.auto.model=rio\n"
         "ro.yap.auto.mcu=caska_t3\n"
         "ro.yap.auto.type=carsharing\n",
         "carsharing-kia-rio-caska_t3",
         "0"},
        {"some_line\n"
         "other=line\n"
         "ro.yap.auto.model=rio\n"
         "ro.yap.auto.mcu=caska_t3\n"
         "ro.yap.auto.type=carsharing\n"
         "ro.build.date.utc=1234\n",
         "carsharing-unknown-rio-caska_t3",
         "1234"},
        {"some_line\n"
         "other=line\n"
         "ro.yap.auto.mcu=caska_t3\n"
         "ro.yap.auto.type=carsharing\n"
         "ro.build.date.utc=1234",
         "carsharing-unknown--caska_t3",
         "1234"},
    };

    for (const auto& test: tests) {
        Firmware fw;
        auto buildMetadataStream = std::stringstream(test.buildMetadata);
        parseFwBuildMetadata(buildMetadataStream, /*out*/ fw);
        EXPECT_EQ(fw.id().name(), test.expectedName);
        EXPECT_EQ(fw.id().version(), test.expectedVersion);
    }
}
}
