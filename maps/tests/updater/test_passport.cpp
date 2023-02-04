#include <library/cpp/testing/common/env.h>
#include <maps/automotive/libs/test_helpers/helpers.h>
#include <maps/automotive/libs/test_helpers/serialization.h>
#include <maps/automotive/updater/proto/updater.pb.h>
#include <maps/automotive/updater/lib/names.h>
#include <maps/automotive/updater/test_helpers/introspection.h>
#include <maps/automotive/updater/test_helpers/mocks.h>
#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/locale/include/convert.h>
#include <maps/libs/common/include/file_utils.h>

namespace maps::automotive::updater {

using namespace ::testing;

namespace {

struct UpdaterFixture : public Fixture
{
    UpdaterFixture() : Fixture()
    {
        scope.set_branch("release");
        scope.mutable_headunit()->set_type(CARSHARING);
        scope.mutable_headunit()->set_vendor("toyota");
        scope.mutable_headunit()->set_model("camry");
        scope.mutable_headunit()->set_mcu("caska");

        parseFromFile(
            SRC_("data/UpdaterApi__PostWithNonce_package.prototxt"),
            package);

        ON_CALL(drive(), getSessionTypeFor(headId))
            .WillByDefault(Return(drive::SessionType::User));

        taggedApps[GROUPABLE_TAG].emplace(LAUNCHER_PACKAGE);
        taggedApps[CORE_TAG].emplace(LAUNCHER_PACKAGE);

        ON_CALL(uCache(), taggedApps())
            .WillByDefault(Return(taggedApps));
    }

    masi::Scope scope;
    masi::Package package;
    TagMap taggedApps;
    std::string headId = "2516d3cff3ff5de96c2f69d4497063d6";
};

} // namespace

TEST_F(UpdaterFixture, PostWithNonce)
{
    ON_CALL(uCache(), latest(scope, headId))
        .WillByDefault(Return(Rollout{{{package, std::nullopt}}, {}}));

    EXPECT_CALL(
        drive(),
        scheduleServiceSession(headId, Gt(TInstant::Now()), TDuration::Minutes(15)));

    EXPECT_CALL(passport(), assertSignatureValid(headId, PassportInfo{"nonce", "signature"}));

    auto res = mockPost(
            "/updater/2.x/updates"
                "?" "type=carsharing"
                "&" "vendor=toyota"
                "&" "model=camry"
                "&" "mcu=caska"
                "&" "lang=ru_RU"
                "&" "uuid=2516d3cff3ff5de96c2f69d4497063d6"
                "&" "deviceid=deviceid"
                "&" "nonce=nonce"
                "&" "signature=signature"
                "&" "session_type=user",
            common::readFileToString(SRC_("data/UpdaterApi__PostWithNonce_request.prototxt")));

    ASSERT_EQ(res.status, 200);
    EXPECT_EQ(
        res.body,
        common::readFileToString(SRC_("data/UpdaterApi__PostWithNonce_response.prototxt")));
}

TEST_F(UpdaterFixture, PostWithNonceNoSignature)
{
    ON_CALL(uCache(), latest(scope, headId))
        .WillByDefault(Return(Rollout{{{package, std::nullopt}}, {}}));

    auto res = mockPost(
            "/updater/2.x/updates"
                "?" "type=carsharing"
                "&" "vendor=toyota"
                "&" "model=camry"
                "&" "mcu=caska"
                "&" "lang=ru_RU"
                "&" "uuid=2516d3cff3ff5de96c2f69d4497063d6"
                "&" "deviceid=deviceid"
                "&" "nonce=nonce"
                "&" "session_type=user",
            common::readFileToString(SRC_("data/UpdaterApi__PostWithNonce_request.prototxt")));

    ASSERT_EQ(res.status, 200);
    EXPECT_EQ(
        res.body,
        common::readFileToString(SRC_("data/UpdaterApi__PostWithNonce_response.prototxt")));
}

} // namespace maps::automotive::updater
