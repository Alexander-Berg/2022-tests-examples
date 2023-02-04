#include <maps/automotive/libs/test_helpers/helpers.h>
#include <maps/automotive/updater/lib/store_internal.h>
#include <maps/automotive/updater/test_helpers/mocks.h>

namespace maps::automotive::updater {

using namespace ::testing;

namespace {

struct UnprocessableFixture : public Fixture
{
    UnprocessableFixture() : Fixture()
    {
        scope.set_branch("release");
        scope.mutable_headunit()->set_type("oem");
        scope.mutable_headunit()->set_vendor("kia");
        scope.mutable_headunit()->set_model("rio");
        scope.mutable_headunit()->set_mcu("caska");
    }

    std::string url(const std::string& handle)
    {
        return handle + 
            "?" "type=oem"
            "&" "vendor=kia"
            "&" "model=rio"
            "&" "mcu=caska"
            "&" "lang=en_US"
            "&" "uuid=ffeeddccbbaa";
    }

    void isSupported(int expectedStatusCode, bool hasSw, bool hasFw)
    {
        EXPECT_CALL(uCache(), isSupported(scope))
            .WillRepeatedly(Return(hasSw));
        EXPECT_CALL(sCache(), isSupported(scope))
            .WillRepeatedly(Return(hasFw));

        ASSERT_EQ(
            (expectedStatusCode == 204) ? 200 : expectedStatusCode,
            mockGet(url("/updater/2.x/updates")).status);
        ASSERT_EQ(
            (expectedStatusCode == 204) ? 200 : expectedStatusCode,
            mockPost(url("/updater/2.x/updates"), "software {} firmware {}").status);
        ASSERT_EQ(
            expectedStatusCode,
            mockPost(url("/updater/1.x/updates"), "[]").status);
        ASSERT_EQ(
            expectedStatusCode,
            mockPost(url("/updater/1.x/firmware_updates"), R"({"ro.build.date.utc": "0"})").status);
    }

    masi::Scope scope;
};

} // namespace

TEST_F(UnprocessableFixture, Test)
{
    isSupported(204, true, true);
    isSupported(204, true, false);
    isSupported(204, false, true);
    isSupported(422, false, false);
}

} // namespace maps::automotive::updater
