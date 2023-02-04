#include <library/cpp/testing/unittest/env.h>
#include <maps/automotive/libs/test_helpers/helpers.h>
#include <maps/automotive/updater/test_helpers/mocks.h>

namespace maps::automotive::updater {

namespace {

struct HeadUnitIdFixture : public Fixture
{
    HeadUnitIdFixture() : Fixture() {}

    size_t postHeadUnit(const char* headId, const char* headUuid)
    {
        const auto postBody = "software {} firmware {}";

        std::stringstream postHandle;
        postHandle << "/updater/2.x/updates?type=oem&"
                      "vendor=kia&model=rio&mcu=caska&"
                      "lang=en_US";

        if (headId != nullptr) {
            postHandle << "&headid=" << headId;
        }

        if (headUuid != nullptr) {
            postHandle << "&uuid=" << headUuid;
        }

        return mockPost(postHandle.str(), postBody).status;
    }
};

} // namespace

TEST_F(HeadUnitIdFixture, NoHeadIdNoUuid)
{
    ASSERT_EQ(400ull, postHeadUnit(nullptr, nullptr));
}

TEST_F(HeadUnitIdFixture, NoHeadIdEmptyUuid)
{
    ASSERT_EQ(400ull, postHeadUnit(nullptr, ""));
}

TEST_F(HeadUnitIdFixture, UuidNoHeadId)
{
    ASSERT_EQ(200ull, postHeadUnit(nullptr, "abc"));
}

TEST_F(HeadUnitIdFixture, EmptyHeadIdNoUuid)
{
    ASSERT_EQ(400ull, postHeadUnit("", nullptr));
}

TEST_F(HeadUnitIdFixture, EmptyHeadIdEmptyUuid)
{
    ASSERT_EQ(400ull, postHeadUnit("", ""));
}

TEST_F(HeadUnitIdFixture, UuidEmptyHeadId)
{
    ASSERT_EQ(200ull, postHeadUnit("", "abc"));
}

TEST_F(HeadUnitIdFixture, HeadIdNoUuid)
{
    ASSERT_EQ(200ull, postHeadUnit("abc", nullptr));
}

TEST_F(HeadUnitIdFixture, HeadIdEmptyUuid)
{
    ASSERT_EQ(200ull, postHeadUnit("abc", ""));
}

TEST_F(HeadUnitIdFixture, HeadIdUuid)
{
    ASSERT_EQ(200ull, postHeadUnit("abc", "abc"));
}

} // namespace maps::automotive::updater
