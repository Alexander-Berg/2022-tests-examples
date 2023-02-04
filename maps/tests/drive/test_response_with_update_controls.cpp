#include <library/cpp/testing/common/env.h>
#include <maps/automotive/libs/test_helpers/serialization.h>
#include <maps/automotive/updater/lib/config.h>
#include <maps/automotive/updater/lib/names.h>
#include <maps/automotive/updater/proto/updater.pb.h>
#include <maps/automotive/updater/test_helpers/mocks.h>
#include <maps/automotive/updater/yacare/drive.h>

namespace maps::automotive::updater {

using namespace ::testing;

namespace {

struct WithControlsFixture : public Fixture {
    WithControlsFixture() : Fixture()
    {
        parseFromFile(SRC_("data/DriveResponseWithUpdateControls__Input.prototxt"), response);

        info.scope.mutable_headunit()->set_type(CARSHARING);
        info.headId = "deadface";
    }
    proto::UpdateResponse response;
    HeadunitInfo info;
};

} // namespace

// download_during_user_session = false

TEST_F(WithControlsFixture, ProcessUpdatesSessionUserForceDoDownloadFalse)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(true);
    c->mutable_drive()->set_download_during_user_session(false);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::User));

    EXPECT_CALL(
        drive(),
        scheduleServiceSession(info.headId, Gt(TInstant::Now()), TDuration::Minutes(30)));

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_SUPPORTS_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWithUpdateControls__SessionUserForceDoDownloadFalse.prototxt")),
        printToString(response));
}

TEST_F(WithControlsFixture, ProcessUpdatesSessionUserDoDownloadFalse)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(false);
    c->mutable_drive()->set_download_during_user_session(false);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::User));

    EXPECT_CALL(
        drive(),
        scheduleServiceSession(info.headId, Gt(TInstant::Now()), TDuration::Minutes(45)));

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_SUPPORTS_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWithUpdateControls__SessionUserDoDownloadFalse.prototxt")),
        printToString(response));
}

TEST_F(WithControlsFixture, ProcessUpdatesSessionServiceForceDoDownloadFalse)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(true);
    c->mutable_drive()->set_download_during_user_session(false);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::Service));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_SUPPORTS_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWithUpdateControls__SessionService.prototxt")),
        printToString(response));
}

TEST_F(WithControlsFixture, ProcessUpdatesSessionServiceDoDownloadFalse)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(false);
    c->mutable_drive()->set_download_during_user_session(false);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::Service));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_SUPPORTS_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWithUpdateControls__SessionService.prototxt")),
        printToString(response));
}

TEST_F(WithControlsFixture, ProcessUpdatesSessionUnregisteredForceDoDownloadFalse)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(true);
    c->mutable_drive()->set_download_during_user_session(false);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::Unregistered));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_SUPPORTS_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWithUpdateControls__SessionUnregistered.prototxt")),
        printToString(response));
}

TEST_F(WithControlsFixture, ProcessUpdatesSessionUnregisteredDoDownloadFalse)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(false);
    c->mutable_drive()->set_download_during_user_session(false);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::Unregistered));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_SUPPORTS_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWithUpdateControls__SessionUnregistered.prototxt")),
        printToString(response));
}

TEST_F(WithControlsFixture, ProcessUpdatesSessionUnknownForceDoDownloadFalse)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(true);
    c->mutable_drive()->set_download_during_user_session(false);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::Unknown));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_SUPPORTS_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWithUpdateControls__SessionUnknownForce.prototxt")),
        printToString(response));
}

TEST_F(WithControlsFixture, ProcessUpdatesSessionUnknownDoDownloadFalse)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(false);
    c->mutable_drive()->set_download_during_user_session(false);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::Unknown));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_SUPPORTS_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWithUpdateControls__SessionUnknownDoDownloadFalse.prototxt")),
        printToString(response));
}

// download_during_user_session = true

TEST_F(WithControlsFixture, ProcessUpdatesSessionUserForceDoDownloadTrue)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(true);
    c->mutable_drive()->set_download_during_user_session(true);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::User));

    EXPECT_CALL(
        drive(),
        scheduleServiceSession(info.headId, Gt(TInstant::Now()), TDuration::Minutes(30)));

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_SUPPORTS_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWithUpdateControls__SessionUserForceDoDownloadTrue.prototxt")),
        printToString(response));
}

TEST_F(WithControlsFixture, ProcessUpdatesSessionUserDoDownloadTrue)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(false);
    c->mutable_drive()->set_download_during_user_session(true);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::User));

    EXPECT_CALL(
        drive(),
        scheduleServiceSession(info.headId, Gt(TInstant::Now()), TDuration::Minutes(45)));

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_SUPPORTS_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWithUpdateControls__SessionUserDoDownloadTrue.prototxt")),
        printToString(response));
}

TEST_F(WithControlsFixture, ProcessUpdatesSessionServiceForceDoDownloadTrue)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(true);
    c->mutable_drive()->set_download_during_user_session(true);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::Service));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_SUPPORTS_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWithUpdateControls__SessionService.prototxt")),
        printToString(response));
}

TEST_F(WithControlsFixture, ProcessUpdatesSessionServiceDoDownloadTrue)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(false);
    c->mutable_drive()->set_download_during_user_session(true);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::Service));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_SUPPORTS_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWithUpdateControls__SessionService.prototxt")),
        printToString(response));
}

TEST_F(WithControlsFixture, ProcessUpdatesSessionUnregisteredForceDoDownloadTrue)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(true);
    c->mutable_drive()->set_download_during_user_session(true);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::Unregistered));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_SUPPORTS_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWithUpdateControls__SessionUnregistered.prototxt")),
        printToString(response));
}

TEST_F(WithControlsFixture, ProcessUpdatesSessionUnregisteredDoDownloadTrue)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(false);
    c->mutable_drive()->set_download_during_user_session(true);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::Unregistered));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_SUPPORTS_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWithUpdateControls__SessionUnregistered.prototxt")),
        printToString(response));
}

TEST_F(WithControlsFixture, ProcessUpdatesSessionUnknownForceDoDownloadTrue)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(true);
    c->mutable_drive()->set_download_during_user_session(true);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::Unknown));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_SUPPORTS_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWithUpdateControls__SessionUnknownForce.prototxt")),
        printToString(response));
}

TEST_F(WithControlsFixture, ProcessUpdatesSessionUnknownDoDownloadTrue)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(false);
    c->mutable_drive()->set_download_during_user_session(true);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::Unknown));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_SUPPORTS_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWithUpdateControls__SessionUnknownDoDownloadTrue.prototxt")),
        printToString(response));
}

TEST_F(WithControlsFixture, ProcessUpdatesUnserviceableForce)
{
    ON_CALL(unserviceable(), isServiceable(info.headId))
        .WillByDefault(Return(false));

    auto c = config();
    c->mutable_drive()->set_force_updates(true);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::Unknown));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_SUPPORTS_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWithUpdateControls__Unserviceable.prototxt")),
        printToString(response));
}

TEST_F(WithControlsFixture, ProcessUpdatesUnserviceable)
{
    ON_CALL(unserviceable(), isServiceable(info.headId))
        .WillByDefault(Return(false));

    auto c = config();
    c->mutable_drive()->set_force_updates(false);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::Unknown));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_SUPPORTS_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWithUpdateControls__Unserviceable.prototxt")),
        printToString(response));
}

} // namespace maps::automotive::updater
