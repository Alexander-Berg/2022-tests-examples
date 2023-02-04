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

struct WithoutControls : public Fixture {
    WithoutControls() : Fixture()
    {
        parseFromFile(SRC_("data/DriveResponseWoUpdateControls__Input.prototxt"), response);

        info.scope.mutable_headunit()->set_type(CARSHARING);
        info.headId = "deadface";
    }
    proto::UpdateResponse response;
    HeadunitInfo info;
};

} // namespace

TEST_F(WithoutControls, ProcessUpdatesSessionUserForce)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(true);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::User));

    EXPECT_CALL(
        drive(),
        scheduleServiceSession(info.headId, Gt(TInstant::Now()), TDuration::Minutes(30)));

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_DOES_NOT_SUPPORT_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWoUpdateControls__SessionUserForce.prototxt")),
        printToString(response));
}

TEST_F(WithoutControls, ProcessUpdatesSessionUser)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(false);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::User));

    EXPECT_CALL(
        drive(),
        scheduleServiceSession(info.headId, Gt(TInstant::Now()), TDuration::Minutes(45)));

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_DOES_NOT_SUPPORT_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWoUpdateControls__SessionUser.prototxt")),
        printToString(response));
}

TEST_F(WithoutControls, ProcessUpdatesSessionServiceForce)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(true);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::Service));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_DOES_NOT_SUPPORT_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWoUpdateControls__SessionService.prototxt")),
        printToString(response));
}

TEST_F(WithoutControls, ProcessUpdatesSessionService)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(false);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::Service));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_DOES_NOT_SUPPORT_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWoUpdateControls__SessionService.prototxt")),
        printToString(response));
}

TEST_F(WithoutControls, ProcessUpdatesSessionUnregisteredForce)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(true);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::Unregistered));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_DOES_NOT_SUPPORT_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWoUpdateControls__SessionUnregistered.prototxt")),
        printToString(response));
}

TEST_F(WithoutControls, ProcessUpdatesSessionUnregistered)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(false);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::Unregistered));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_DOES_NOT_SUPPORT_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWoUpdateControls__SessionUnregistered.prototxt")),
        printToString(response));
}

TEST_F(WithoutControls, ProcessUpdatesSessionUnknownForce)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(true);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::Unknown));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_DOES_NOT_SUPPORT_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWoUpdateControls__SessionUnknownForce.prototxt")),
        printToString(response));
}

TEST_F(WithoutControls, ProcessUpdatesSessionUnknown)
{
    auto c = config();
    c->mutable_drive()->set_force_updates(false);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::Unknown));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_DOES_NOT_SUPPORT_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWoUpdateControls__SessionUnknown.prototxt")),
        printToString(response));
}

TEST_F(WithoutControls, ProcessUpdatesUnserviceableForce)
{
    ON_CALL(unserviceable(), isServiceable(info.headId))
        .WillByDefault(Return(false));

    auto c = config();
    c->mutable_drive()->set_force_updates(true);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::Unknown));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_DOES_NOT_SUPPORT_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWoUpdateControls__Unserviceable.prototxt")),
        printToString(response));
}

TEST_F(WithoutControls, ProcessUpdatesUnserviceable)
{
    ON_CALL(unserviceable(), isServiceable(info.headId))
        .WillByDefault(Return(false));

    auto c = config();
    c->mutable_drive()->set_force_updates(false);
    setConfig(c);

    EXPECT_CALL(drive(), getSessionTypeFor(info.headId))
        .WillOnce(Return(drive::SessionType::Unknown));

    EXPECT_CALL(drive(), scheduleServiceSession(_, _, _)).Times(0);

    EXPECT_NO_THROW(drive::processUpdates(response, info, drive::HEAD_DOES_NOT_SUPPORT_UPDATE_CONTROLS));

    EXPECT_EQ(
        maps::common::readFileToString(SRC_("data/DriveResponseWoUpdateControls__Unserviceable.prototxt")),
        printToString(response));
}

} // namespace maps::automotive::updater
