#include <library/cpp/testing/gtest/gtest.h>
#include <maps/automotive/store_internal/lib/dao/firmware_rollout.h>
#include <maps/automotive/store_internal/lib/helpers.h>
#include <maps/automotive/store_internal/lib/serialization.h>
#include <maps/automotive/store_internal/tests/helpers.h>
#include <maps/automotive/store_internal/tests/postgres.h>

#include <pqxx/pqxx>

namespace maps::automotive::store_internal {

using FirmwareRolloutDaoTests = AppContextPostgresFixture;

TEST_F(FirmwareRolloutDaoTests, removeUpsert)
{
    FirmwareRollout ro = defaultFirmwareRollout();
    {
        auto txn = dao::makeWriteableTransaction();
        auto rollouts = FirmwareRolloutDao(*txn).selectAll();
        ASSERT_EQ(1, rollouts.rollouts_size());
        FirmwareRolloutDao(*txn).remove(ro);
        txn->commit();
    }
    {
        auto txn = dao::makeWriteableTransaction();
        auto rollouts = FirmwareRolloutDao(*txn).selectAll();
        ASSERT_EQ(0, rollouts.rollouts_size());
    }
    buildFirmwareRollout(ro, "prestable", firmwareNoRollout().id());
    ro.mutable_experiment()->set_percent(10);
    {
        auto txn = dao::makeWriteableTransaction();
        FirmwareRolloutDao(*txn).upsert(ro);
        txn->commit();
    }
    {
        auto txn = dao::makeWriteableTransaction();
        auto rollouts = FirmwareRolloutDao(*txn).selectAll();
        ASSERT_EQ(1, rollouts.rollouts_size());
        enrich(rollouts.rollouts(0), ro);
        EXPECT_EQ(printToString(ro), printToString(rollouts.rollouts(0)));
    }
    ro.mutable_experiment()->set_percent(11);
    {
        auto txn = dao::makeWriteableTransaction();
        FirmwareRolloutDao(*txn).upsert(ro);
        txn->commit();
    }
    {
        auto txn = dao::makeWriteableTransaction();
        auto rollouts = FirmwareRolloutDao(*txn).selectAll();
        ASSERT_EQ(1, rollouts.rollouts_size());
        enrich(rollouts.rollouts(0), ro);
        EXPECT_EQ(printToString(ro), printToString(rollouts.rollouts(0)));
    }
    ro.set_branch("testing");
    {
        auto txn = dao::makeWriteableTransaction();
        FirmwareRolloutDao(*txn).upsert(ro);
        txn->commit();
    }
    {
        auto txn = dao::makeWriteableTransaction();
        auto rollouts = FirmwareRolloutDao(*txn).selectAll();
        ASSERT_EQ(2, rollouts.rollouts_size());
    }
}

TEST_F(FirmwareRolloutDaoTests, removeByFirmware)
{
    Firmware::Id id = firmwareNoRollout().id();
    {
        auto txn = dao::makeWriteableTransaction();
        auto rollouts = FirmwareRolloutDao(*txn).selectAll();
        ASSERT_EQ(1, rollouts.rollouts_size());
        FirmwareRolloutDao(*txn).remove(id);
        txn->commit();
    }
    id.set_name(FW_WITH_ROLLOUT);
    {

        auto txn = dao::makeWriteableTransaction();
        auto rollouts = FirmwareRolloutDao(*txn).selectAll();
        ASSERT_EQ(1, rollouts.rollouts_size());
        FirmwareRolloutDao(*txn).remove(id);
        txn->commit();
    }
    {
        auto txn = dao::makeWriteableTransaction();
        auto rollouts = FirmwareRolloutDao(*txn).selectAll();
        ASSERT_EQ(0, rollouts.rollouts_size());
    }
}

TEST_F(FirmwareRolloutDaoTests, removeMissing)
{
    {
        auto txn = dao::makeWriteableTransaction();
        FirmwareRollout ro = defaultFirmwareRollout();
        ro.mutable_firmware_id()->set_name("missing");
        EXPECT_THROW(FirmwareRolloutDao(*txn).remove(ro), yacare::errors::NotFound);
    }
    {
        auto txn = dao::makeWriteableTransaction();
        FirmwareRollout ro = defaultFirmwareRollout();
        ro.mutable_firmware_id()->set_version("missing");
        EXPECT_THROW(FirmwareRolloutDao(*txn).remove(ro), yacare::errors::NotFound);
    }
    {
        auto txn = dao::makeWriteableTransaction();
        FirmwareRollout ro = defaultFirmwareRollout();
        ro.set_branch("missing");
        EXPECT_THROW(FirmwareRolloutDao(*txn).remove(ro), yacare::errors::NotFound);
    }
    {
        auto txn = dao::makeWriteableTransaction();
        FirmwareRollout ro = defaultFirmwareRollout();
        ro.mutable_headunit()->set_type("missing");
        EXPECT_THROW(FirmwareRolloutDao(*txn).remove(ro), yacare::errors::NotFound);
    }
    {
        auto txn = dao::makeWriteableTransaction();
        FirmwareRollout ro = defaultFirmwareRollout();
        ro.mutable_headunit()->set_vendor("missing");
        EXPECT_THROW(FirmwareRolloutDao(*txn).remove(ro), yacare::errors::NotFound);
    }
    {
        auto txn = dao::makeWriteableTransaction();
        FirmwareRollout ro = defaultFirmwareRollout();
        ro.mutable_headunit()->set_model("missing");
        EXPECT_THROW(FirmwareRolloutDao(*txn).remove(ro), yacare::errors::NotFound);
    }
    {
        auto txn = dao::makeWriteableTransaction();
        FirmwareRollout ro = defaultFirmwareRollout();
        ro.mutable_headunit()->set_mcu("missing");
        EXPECT_THROW(FirmwareRolloutDao(*txn).remove(ro), yacare::errors::NotFound);
    }
}

} // namespace maps::automotive::store_internal
