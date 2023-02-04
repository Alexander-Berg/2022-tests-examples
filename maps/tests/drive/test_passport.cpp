#include <library/cpp/testing/common/env.h>
#include <maps/automotive/libs/test_helpers/serialization.h>
#include <maps/automotive/updater/lib/config.h>
#include <maps/automotive/updater/lib/names.h>
#include <maps/automotive/updater/lib/types.h>
#include <maps/automotive/updater/test_helpers/mocks.h>
#include <maps/automotive/updater/yacare/drive.h>
#include <maps/libs/http/include/test_utils.h>

namespace maps::automotive::updater {

using namespace ::testing;

namespace {

struct PassportFixture : public Fixture {
    PassportFixture() : Fixture()
    {
        parseFromFile(SRC_("data/DrivePassport__ProcessUpdatesOk.prototxt"), updates);

        info.scope.mutable_headunit()->set_type(CARSHARING);
        info.headId = "deadface";

        ON_CALL(drive(), getSessionTypeFor(info.headId))
            .WillByDefault(Return(drive::SessionType::User));

        info.passportInfo = {"nonce", "signature"};
    }

    PackageUpdates updates;
    HeadunitInfo info;
};

} // namespace

TEST_F(PassportFixture, ProcessUpdatesNoPassportIsOk)
{
    ASSERT_EQ(proto::Config::Passport::IF_PRESENT, config()->passport().signature_validation());
    info.passportInfo = {};

    EXPECT_CALL(
        drive(),
        scheduleServiceSession(info.headId, Ge(TInstant::Now()), TDuration::Minutes(15)));

    EXPECT_NO_THROW(drive::processUpdates(updates, info));
}

TEST_F(PassportFixture, ProcessUpdatesPassportOk)
{
    ASSERT_EQ(proto::Config::Passport::IF_PRESENT, config()->passport().signature_validation());
    EXPECT_CALL(
        drive(),
        scheduleServiceSession(info.headId, Ge(TInstant::Now()), TDuration::Minutes(15)));

    EXPECT_CALL(passport(), assertSignatureValid(info.headId, *info.passportInfo));

    EXPECT_NO_THROW(drive::processUpdates(updates, info));
}

TEST_F(PassportFixture, ProcessUpdatesPassportNOk)
{
    ASSERT_EQ(proto::Config::Passport::IF_PRESENT, config()->passport().signature_validation());
    EXPECT_CALL(passport(), assertSignatureValid(info.headId, *info.passportInfo))
        .WillOnce(Throw(maps::Exception("NOk")));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(updates, info));
}

TEST_F(PassportFixture, ProcessUpdatesSignatureValidationNone)
{
    EXPECT_CALL(drive(), scheduleServiceSession(
        info.headId, Ge(TInstant::Now()), TDuration::Minutes(15)))
        .Times(3);

    EXPECT_CALL(passport(), assertSignatureValid(info.headId, _))
        .Times(0);

    auto c = config();
    c->mutable_passport()->set_signature_validation(proto::Config::Passport::NONE);
    setConfig(c);

    EXPECT_NO_THROW(drive::processUpdates(updates, info));

    info.passportInfo = {"", ""};
    EXPECT_NO_THROW(drive::processUpdates(updates, info));

    info.passportInfo = std::nullopt;
    EXPECT_NO_THROW(drive::processUpdates(updates, info));
}

TEST_F(PassportFixture, ProcessUpdatesSignatureValidationIfPresentNoParams)
{
    ASSERT_EQ(proto::Config::Passport::IF_PRESENT, config()->passport().signature_validation());

    EXPECT_CALL(drive(), scheduleServiceSession(
        info.headId, Ge(TInstant::Now()), TDuration::Minutes(15)));
    EXPECT_CALL(passport(), assertSignatureValid(info.headId, _))
        .Times(0);

    info.passportInfo = std::nullopt;
    EXPECT_NO_THROW(drive::processUpdates(updates, info));
}

TEST_F(PassportFixture, ProcessUpdatesSignatureValidationIfPresentFailures)
{
    ASSERT_EQ(proto::Config::Passport::IF_PRESENT, config()->passport().signature_validation());

    EXPECT_CALL(drive(), scheduleServiceSession(info.headId, _, _))
        .Times(0);
    EXPECT_CALL(passport(), assertSignatureValid(info.headId, _))
        .Times(0);

    info.passportInfo = {"", ""};
    EXPECT_NO_THROW(drive::processUpdates(updates, info));

    info.passportInfo = {"nonce", "signature"};
    EXPECT_CALL(passport(), assertSignatureValid(info.headId, *info.passportInfo))
        .WillOnce(Throw(maps::Exception("Smth is bad here")));
    EXPECT_NO_THROW(drive::processUpdates(updates, info));
}

TEST_F(PassportFixture, ProcessUpdatesSignatureValidationRequireFailures)
{
    auto c = config();
    c->mutable_passport()->set_signature_validation(proto::Config::Passport::REQUIRE);
    setConfig(c);

    EXPECT_CALL(drive(), scheduleServiceSession(info.headId, _, _))
        .Times(0);
    EXPECT_CALL(passport(), assertSignatureValid(info.headId, _))
        .Times(0);

    info.passportInfo = std::nullopt;
    EXPECT_NO_THROW(drive::processUpdates(updates, info));

    info.passportInfo = {"", ""};
    EXPECT_NO_THROW(drive::processUpdates(updates, info));

    info.passportInfo = {"nonce", "signature"};
    EXPECT_CALL(passport(), assertSignatureValid(info.headId, *info.passportInfo))
        .WillOnce(Throw(maps::Exception("Oh, sh_t")));
    EXPECT_NO_THROW(drive::processUpdates(updates, info));
}

TEST_F(PassportFixture, ProcessUpdatesSignatureValidationRequireOk)
{
    auto c = config();
    c->mutable_passport()->set_signature_validation(proto::Config::Passport::REQUIRE);
    setConfig(c);

    EXPECT_CALL(drive(), scheduleServiceSession(
        info.headId, Ge(TInstant::Now()), TDuration::Minutes(15)));
    EXPECT_CALL(passport(), assertSignatureValid(info.headId, *info.passportInfo));

    EXPECT_NO_THROW(drive::processUpdates(updates, info));
}
    
} // namespace maps::automotive::updater
