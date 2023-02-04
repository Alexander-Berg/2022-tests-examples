#include <library/cpp/testing/common/env.h>
#include <maps/automotive/libs/test_helpers/helpers.h>
#include <maps/automotive/libs/test_helpers/serialization.h>
#include <maps/automotive/updater/lib/names.h>
#include <maps/automotive/updater/proto/updater.pb.h>
#include <maps/automotive/updater/test_helpers/introspection.h>
#include <maps/automotive/updater/test_helpers/mocks.h>
#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/locale/include/convert.h>

#include <maps/libs/common/include/file_utils.h>

namespace maps::automotive::updater {

using namespace ::testing;

namespace {

struct TestParams
{
    std::vector<TProtoStringType> cidrs;
    std::string clientIP;
    bool pass;
};

struct IpFixture : public Fixture, public WithParamInterface<TestParams>
{
    IpFixture() : Fixture()
    {
        auto config = std::make_shared<proto::Config>();
        parseFromFile(SRC_("data/DriveApi__ip_ranges.prototxt"), *config);
        config->mutable_drive()->set_use_ip_filter(true);
        config->mutable_drive()->clear_accepted_ip_range();
        setConfig(config);

        masi::Scope scope;
        scope.set_branch("release");
        scope.mutable_headunit()->set_type("carsharing");
        scope.mutable_headunit()->set_vendor("toyota");
        scope.mutable_headunit()->set_model("camry");
        scope.mutable_headunit()->set_mcu("caska");

        masi::Package package;
        parseFromFile(
            SRC_("data/UpdaterApi__PostWithNonce_package.prototxt"),
            package);

        ON_CALL(drive(), getSessionTypeFor(headId))
            .WillByDefault(Return(drive::SessionType::User));

        ON_CALL(uCache(), latest(scope, headId))
            .WillByDefault(Return(Rollout{{{package, std::nullopt}}, {}}));

        taggedApps[GROUPABLE_TAG].insert(LAUNCHER_PACKAGE);
        taggedApps[CORE_TAG].insert(LAUNCHER_PACKAGE);

        ON_CALL(uCache(), taggedApps())
            .WillByDefault(Return(taggedApps));
    }

    TagMap taggedApps;

    const std::string headId = "2516d3cff3ff5de96c2f69d4497063d6";
    const std::string postHandle =
        "/updater/2.x/updates"
        "?" "type=carsharing"
        "&" "vendor=toyota"
        "&" "model=camry"
        "&" "mcu=caska"
        "&" "lang=ru_RU"
        "&" "uuid=2516d3cff3ff5de96c2f69d4497063d6";
};

const TestParams PARAMS[] = {
    {{"178.176.39.88/29"}, "178.176.39.89", true},
    {{"178.176.39.88/29"}, "178.176.39.87", false},
    {{"178.176.39.88/29"}, "::ffff:178.176.39.89", true},
    {{"178.176.39.88/29"}, "::ffff:178.176.39.87", false},
    {{"178.176.39.88/29"}, "::1", false},
    {{"178.176.39.88/29"}, "not an ip", false},
    {{"::/120"}, "::1", true},
    {{"::/120"}, "::0100", false},
    {{"::/120"}, "178.176.39.89", false},
    {{"::/120"}, "not an ip", false},
    {{"::/120", "178.176.39.88/29"}, "178.176.39.89", true},
    {{"::/120", "178.176.39.88/29"}, "::1", true},
    {{"::/120", "178.176.39.88/29"}, "not an ip", false},
    {{"::/0"}, "178.176.39.89", false},
    {{"::/0"}, "::ffff:178.176.39.89", false},
    {{"::/0"}, "::1", true},
    {{"0.0.0.0/0"}, "178.176.39.89", true},
    {{"0.0.0.0/0"}, "::ffff:178.176.39.89", true},
    {{"0.0.0.0/0"}, "::1", false},
};

} // namespace

TEST_P(IpFixture, PostIPFilter)
{
    const auto requestText = common::readFileToString(
        SRC_("data/UpdaterApi__PostWithNonce_request.prototxt"));
    const auto responseText = common::readFileToString(
        SRC_("data/UpdaterApi__PostWithNonce_response.prototxt"));

    const auto& param = GetParam();
    for (auto& cidr: param.cidrs) {
        config()->mutable_drive()->add_accepted_ip_range(cidr);
    }

    if (param.pass) {
        EXPECT_CALL(drive(), scheduleServiceSession(_, _, _));
    }
    auto res = mockPost(
        postHandle, requestText, {{"X-Real-IP", param.clientIP}});

    ASSERT_EQ(res.status, 200);
    EXPECT_EQ(res.body, responseText);
}

INSTANTIATE_TEST_SUITE_P(IpTests, IpFixture, ValuesIn(PARAMS));

} // namespace maps::automotive::updater
