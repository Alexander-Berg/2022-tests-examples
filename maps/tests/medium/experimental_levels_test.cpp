#include <maps/indoor/long-tasks/src/radiomap-evaluation-cron-job/lib/impl/experimental_levels.h>

#include <library/cpp/testing/unittest/registar.h>
#include <maps/indoor/libs/unittest/fixture.h>

namespace maps::mirc::radiomap_evaluator::tests {

Y_UNIT_TEST_SUITE_F(radiomap_evaluator_utils, unittest::Fixture)
{

Y_UNIT_TEST(add_and_load_experimental_indoor_levels)
{
    auto& pool = pgPool();

    {
        auto txn = pool.masterWriteableTransaction();
        addExperimentalLevel("1", "1", *txn);
        addExperimentalLevel("1", "2", *txn);
        addExperimentalLevel("3", "3", *txn);
        txn->commit();
    }

    auto txn = pool.slaveTransaction();
    const auto levelsToSkip = loadExperimentalLevels(*txn);

    UNIT_ASSERT_VALUES_EQUAL(levelsToSkip.size(), 2u);
    UNIT_ASSERT_VALUES_EQUAL(levelsToSkip.at("1").size(), 2u);
    UNIT_ASSERT_VALUES_EQUAL(levelsToSkip.at("1")[0], "1");
    UNIT_ASSERT_VALUES_EQUAL(levelsToSkip.at("1")[1], "2");

    UNIT_ASSERT_VALUES_EQUAL(levelsToSkip.at("3").size(), 1u);
    UNIT_ASSERT_VALUES_EQUAL(levelsToSkip.at("3")[0], "3");
}

} // Y_UNIT_TEST_SUITE_F

} // namespace maps::mirc::radiomap_evaluator::tests
