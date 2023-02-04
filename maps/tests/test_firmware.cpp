#include <library/cpp/testing/gtest/gtest.h>
#include <maps/automotive/store_internal/lib/dao/firmware.h>
#include <maps/automotive/store_internal/lib/helpers.h>
#include <maps/automotive/store_internal/lib/serialization.h>
#include <maps/automotive/store_internal/tests/helpers.h>
#include <maps/automotive/store_internal/tests/postgres.h>

#include <pqxx/pqxx>

namespace maps::automotive::store_internal {

using FirmwareDaoTests = AppContextPostgresFixture;

TEST_F(FirmwareDaoTests, remove)
{
    Firmware::Id id = firmwareNoRollout().id();
    {
        auto txn = dao::makeWriteableTransaction();
        FirmwareDao(*txn).remove(id);
        txn->commit();
    }
    {
        auto txn = dao::makeWriteableTransaction();
        EXPECT_THROW(FirmwareDao(*txn).select(id), yacare::errors::NotFound);
        EXPECT_THROW(FirmwareDao(*txn).remove(id), yacare::errors::NotFound);
    }
    {
        Firmware::Id fwId = firmwareWithRollout().id();
        auto txn = dao::makeWriteableTransaction();
        EXPECT_THROW(FirmwareDao(*txn).remove(fwId), yacare::Error);
    }
    {
        Firmware::Id fwId;
        fwId.set_name("missing");
        fwId.set_version(SOME_FW_VERSION);
        auto txn = dao::makeWriteableTransaction();
        EXPECT_THROW(FirmwareDao(*txn).remove(fwId), yacare::errors::NotFound);
    }
    {
        Firmware::Id fwId;
        fwId.set_name(FW_NO_ROLLOUT);
        fwId.set_version("missing");
        auto txn = dao::makeWriteableTransaction();
        EXPECT_THROW(FirmwareDao(*txn).remove(fwId), yacare::errors::NotFound);
    }
}

TEST_F(FirmwareDaoTests, create)
{
    Firmware firmware;
    buildFirmware(firmware, "new", "18");
    {
        auto txn = dao::makeWriteableTransaction();
        ASSERT_THROW(FirmwareDao(*txn).creationStatus(firmware.id()), yacare::errors::NotFound);
        ASSERT_THROW(FirmwareDao(*txn).select(firmware.id()), yacare::errors::NotFound);
        FirmwareDao(*txn).addIncoming(firmware.id());
        txn->commit();
    }
    {
        auto txn = dao::makeWriteableTransaction();
        EXPECT_THROW(FirmwareDao(*txn).addIncoming(firmware.id()), yacare::errors::UnprocessableEntity);
    }
    {
        auto txn = dao::makeWriteableTransaction();
        auto status = FirmwareDao(*txn).creationStatus(firmware.id());
        ASSERT_EQ("in_progress", status.status());
        EXPECT_TRUE(status.description().empty());
    }
    {
        auto txn = dao::makeWriteableTransaction();
        FirmwareDao(*txn).create(firmware);
        txn->commit();
    }
    {
        auto txn = dao::makeWriteableTransaction();
        auto status = FirmwareDao(*txn).creationStatus(firmware.id());
        ASSERT_EQ("created", status.status());
        EXPECT_TRUE(status.description().empty());
        auto savedFw = FirmwareDao(*txn).select(firmware.id());
        EXPECT_EQ(printToString(firmware), printToString(savedFw));
    }
}

TEST_F(FirmwareDaoTests, abortCreation)
{
    Firmware::Id id = firmwareNoRollout().id();
    Status expectedStatus;
    expectedStatus.set_status("error");
    expectedStatus.set_description("smth bad happened");
    {
        auto txn = dao::makeWriteableTransaction();
        EXPECT_THROW(FirmwareDao(*txn).abortCreation(id, "failed"), yacare::errors::NotFound);
    }
    id.set_name("new");
    {
        auto txn = dao::makeWriteableTransaction();
        FirmwareDao(*txn).addIncoming(id);
        txn->commit();
    }
    {
        auto txn = dao::makeWriteableTransaction();
        auto status = FirmwareDao(*txn).creationStatus(id);
        ASSERT_EQ("in_progress", status.status());
        EXPECT_TRUE(status.description().empty());
        FirmwareDao(*txn).abortCreation(id, expectedStatus.description());
        txn->commit();
    }
    {
        auto txn = dao::makeWriteableTransaction();
        auto status = FirmwareDao(*txn).creationStatus(id);
        EXPECT_EQ(printToString(expectedStatus), printToString(status));
    }
}

TEST_F(FirmwareDaoTests, update)
{
    Firmware firmware = firmwareWithRollout();
    firmware.mutable_metadata()->mutable_title()->insert({"af", "Afrikaans title"});
    firmware.mutable_metadata()->mutable_release_notes()->insert({"af", "Afrikaans release notes"});
    {
        auto txn = dao::makeWriteableTransaction();
        FirmwareDao(*txn).update(firmware);
        txn->commit();
    }
    {
        auto txn = dao::makeWriteableTransaction();
        auto fw = FirmwareDao(*txn).select(firmware.id());
        EXPECT_EQ(printToString(firmware), printToString(fw));
    }
    firmware.mutable_id()->set_name("missing");
    {
        Firmware::Id id;
        auto txn = dao::makeWriteableTransaction();
        EXPECT_THROW(FirmwareDao(*txn).update(firmware), yacare::errors::NotFound);
    }
}

TEST_F(FirmwareDaoTests, CreationStatus)
{
    auto checkEnum = [](Dao::CreationStatus value) {
        auto txn = dao::makeReadOnlyTransaction();
        txn->exec("select '" + ToString(value) + "'::creation_status_enum");
    };
    checkEnum(Dao::CreationStatus::InProgress);
    checkEnum(Dao::CreationStatus::Created);
    checkEnum(Dao::CreationStatus::Error);
}

} // namespace maps::automotive::store_internal
