#include <maps/indoor/long-tasks/src/radiomap-metrics/lib/impl.h>

#include <maps/indoor/libs/db/include/task.h>
#include <maps/indoor/libs/db/include/task_gateway.h>
#include <maps/indoor/libs/db/include/assignment.h>
#include <maps/indoor/libs/db/include/assignment_gateway.h>

#include <maps/indoor/libs/unittest/fixture.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::mirc::radiomap_metrics::test {

namespace {

using namespace indoor::positioning_estimator;

const std::string USER_ID = "12321aBc";
const IndoorPlanId INDOOR_PLAN_ID = "2324aBc";
const IndoorLevelId LEVEL_ID_1 = "1_aBc";
const IndoorLevelId LEVEL_ID_2 = "2_aBc";
const IndoorLevelId LEVEL_ID_NULL = "";

template <typename Func>
auto initCompletedTaskAndAssignment(
    const std::string& indoorPlanId,
    const std::string& uid,
    pqxx::transaction_base& txn,
    Func&& customSetup
)
{
    db::ugc::TaskGateway taskGateway(txn);
    db::ugc::AssignmentGateway assignmentGateway(txn);

    db::ugc::Task task;
    task.setIndoorPlanId(indoorPlanId);
    taskGateway.insert(task);
    task.setStatus(db::ugc::TaskStatus::Available);
    auto assignment = task.assignTo(uid);
    assignment.markAsCompleted();
    assignmentGateway.insert(assignment);
    task.markAsDone();
    customSetup(task, assignment);
    taskGateway.update(task);
    assignmentGateway.update(assignment);
    return std::make_pair(std::move(task), std::move(assignment));
}

} // namespace

Y_UNIT_TEST_SUITE_F(radiomap_metrics_worker_impl_should, unittest::Fixture)
{

Y_UNIT_TEST(load_single_task_data)
{
    auto txn = pgPool().masterWriteableTransaction();
    auto [task, assignment] = initCompletedTaskAndAssignment(
        INDOOR_PLAN_ID, USER_ID, *txn,
        [](auto&& task, auto&&)
        {
            task.setIsTest(true);
            task.setSkipEvaluation(false);
        }
    );

    const auto loadedData = impl::loadTaskData(*txn);
    UNIT_ASSERT_EQUAL(loadedData.size(), 1);
    const auto& taskData = loadedData.front();
    UNIT_ASSERT_EQUAL(taskData.taskId, task.id());
    UNIT_ASSERT_EQUAL(taskData.assignmentId, assignment.id());
    UNIT_ASSERT_EQUAL(taskData.indoorPlanId, INDOOR_PLAN_ID);
    UNIT_ASSERT_EQUAL(taskData.doneAt, task.doneAt());
}

Y_UNIT_TEST(not_load_processed_tasks)
{
    auto txn = pgPool().masterWriteableTransaction();
    auto [task, assignment] = initCompletedTaskAndAssignment(
        INDOOR_PLAN_ID, USER_ID, *txn,
        [](auto&& task, auto&&)
        {
            task.setIsTest(true);
            task.setMetricsCalculatedAt(chrono::TimePoint::clock::now());
        }
    );

    const auto loadedData = impl::loadTaskData(*txn);
    UNIT_ASSERT(loadedData.empty());
}

Y_UNIT_TEST(not_load_non_test_tasks)
{
    auto txn = pgPool().masterWriteableTransaction();
    auto [task, assignment] = initCompletedTaskAndAssignment(
        INDOOR_PLAN_ID, USER_ID, *txn,
        [](auto&& task, auto&&)
        {
            task.setIsTest(false);
        }
    );

    const auto loadedData = impl::loadTaskData(*txn);
    UNIT_ASSERT(loadedData.empty());
}

Y_UNIT_TEST(mark_task_as_processed)
{
    auto txn = pgPool().masterWriteableTransaction();
    auto [task, assignment] = initCompletedTaskAndAssignment(
        INDOOR_PLAN_ID, USER_ID, *txn,
        [](auto&& task, auto&&)
        {
            task.setIsTest(true);
            task.setSkipEvaluation(true);
        }
    );

    impl::markTaskAsProcessed(task.id(), *txn);

    db::ugc::TaskGateway gw(*txn);
    auto processedTask = gw.loadById(task.id());
    UNIT_ASSERT(processedTask.metricsCalculatedAt().has_value());
    UNIT_ASSERT(!processedTask.skipEvaluation());
}

Y_UNIT_TEST(save_metrics_to_db)
{
    auto txn = pgPool().masterWriteableTransaction();

    auto [task, assignment] = initCompletedTaskAndAssignment(
        INDOOR_PLAN_ID, USER_ID, *txn,
        [](auto&& task, auto&&)
        {
            task.setIsTest(true);
            task.setSkipEvaluation(false);
        }
    );

    using maps::indoor::positioning_estimator::SolutionError;
    LevelMetrics refMetrics {
        .errors = { .1, .2, .3, .4, .5 },
        .noSolutionArea = .1,
        .unCoveredArea = .1,
        .percentileValues = {
            SolutionError{
                .type = SolutionError::ErrorType::NoError,
                .value = .1,
            },
            SolutionError{
                .type = SolutionError::ErrorType::NoError,
                .value = .2,
            },
            SolutionError{
                .type = SolutionError::ErrorType::LevelMismatch,
                .value = .3,
            },
            SolutionError{
            },
        },
        .duration = 10,
        .timeToFirstFix = .1,
        .lvlMissedDuration = 11,
        .lvlSwitchCount = 12,
        .lvlHitDuration = 13,
        .noSolutionDuration = 14,
    };

    const IndoorLevelId LEVEL_ID_1 = "12";
    impl::updateRadioMapMetricsInDB(
        INDOOR_PLAN_ID,
        assignment.id(),
        chrono::TimePoint::clock::now(),
        DeviceType::Android,
        LocationProvider::Indoor,
        LEVEL_ID_1,
        refMetrics,
        *txn
    );

    auto checkQueryResult = txn->exec1("SELECT COUNT(1) FROM ugc.radiomap_metrics");
    UNIT_ASSERT_EQUAL(checkQueryResult[0].as<size_t>(), 1);
}

Y_UNIT_TEST(save_aggregated_metrics_to_db)
{
    auto txn = pgPool().masterWriteableTransaction();

    auto [task, assignment] = initCompletedTaskAndAssignment(
        INDOOR_PLAN_ID, USER_ID, *txn,
        [](auto&& task, auto&&)
        {
            task.setIsTest(true);
            task.setSkipEvaluation(false);
        }
    );

    using maps::indoor::positioning_estimator::SolutionError;
    LevelMetrics refMetrics {
        .errors = { .1, .2, .3, .4, .5 },
        .noSolutionArea = .1,
        .unCoveredArea = .1,
        .percentileValues = {
            SolutionError{
                .type = SolutionError::ErrorType::NoError,
                .value = .1,
            },
            SolutionError{
                .type = SolutionError::ErrorType::NoError,
                .value = .2,
            },
            SolutionError{
                .type = SolutionError::ErrorType::LevelMismatch,
                .value = .3,
            },
            SolutionError{
            },
        },
        .duration = 10,
        .timeToFirstFix = .1,
        .lvlMissedDuration = 11,
        .lvlSwitchCount = 12,
        .lvlHitDuration = 13,
        .noSolutionDuration = 14,
    };

    impl::updateAggregatedRadioMapMetricsInDB(
        INDOOR_PLAN_ID,
        assignment.id(),
        chrono::TimePoint::clock::now(),
        DeviceType::Android,
        refMetrics,
        *txn
    );

    auto checkQueryResult = txn->exec1("SELECT COUNT(1) FROM ugc.aggregated_radiomap_metrics");
    UNIT_ASSERT_EQUAL(checkQueryResult[0].as<size_t>(), 1);
}

namespace {

// Used to generate and test multiple-level_device_provider metrics
using Test50PValueByLvlDeviceProvider =
    std::map<IndoorLevelId, std::map<DeviceType, std::map<LocationProvider, double>>>;
const Test50PValueByLvlDeviceProvider TEST_PERCENTILE_50_ERROR_NON_ZERO{{
    {LEVEL_ID_1,
        {{DeviceType::Android,   {{LocationProvider::Fused01, 0.10}, {LocationProvider::Fused05, 0.50}}},
        {DeviceType::AndroidOld, {{LocationProvider::Fused01, 0.01}, {LocationProvider::Fused05, 0.05}}}}
    },
    {LEVEL_ID_2,
        {{DeviceType::Android,   {{LocationProvider::Fused01, 0.120}, {LocationProvider::Fused05, 0.0520}}},
        {DeviceType::AndroidOld, {{LocationProvider::Fused01, 0.012}, {LocationProvider::Fused05, 0.0052}}}}
    },
    {LEVEL_ID_NULL,
        {{DeviceType::Android,   {{LocationProvider::Fused01, 0.130}, {LocationProvider::Fused05, 0.0530}}},
        {DeviceType::AndroidOld, {{LocationProvider::Fused01, 0.013}, {LocationProvider::Fused05, 0.0053}}}}
    }
}};
const Test50PValueByLvlDeviceProvider TEST_PERCENTILE_50_ERROR_ZERO{{
    {LEVEL_ID_1,
        {{DeviceType::Android,   {{LocationProvider::Fused01, 0.0}, {LocationProvider::Fused05, 0.0}}},
        {DeviceType::AndroidOld, {{LocationProvider::Fused01, 0.0}, {LocationProvider::Fused05, 0.0}}}}
    },
    {LEVEL_ID_2,
        {{DeviceType::Android,   {{LocationProvider::Fused01, 0.0}, {LocationProvider::Fused05, 0.0}}},
        {DeviceType::AndroidOld, {{LocationProvider::Fused01, 0.0}, {LocationProvider::Fused05, 0.0}}}}
    },
    {LEVEL_ID_NULL,
        {{DeviceType::Android,   {{LocationProvider::Fused01, 0.0}, {LocationProvider::Fused05, 0.0}}},
        {DeviceType::AndroidOld, {{LocationProvider::Fused01, 0.0}, {LocationProvider::Fused05, 0.0}}}}
    }
}};

LevelMetrics getSomeLevelMetrics(const double p50Value)
{
    return LevelMetrics{
        .errors = { .1, .2, .3, .4, .5 },
        .noSolutionArea = .1,
        .unCoveredArea = .1,
        .percentileValues = {
            SolutionError{ .type = SolutionError::ErrorType::NoError, .value = p50Value},
            SolutionError{ .type = SolutionError::ErrorType::LevelMismatch, .value = p50Value * 1.1 },
            SolutionError{ .type = SolutionError::ErrorType::NoSolution },
            SolutionError{},
        },
        .duration = 10,
        .timeToFirstFix = .1,
        .lvlMissedDuration = 11,
        .lvlSwitchCount = 12,
        .lvlHitDuration = 13,
        .noSolutionDuration = 14,
    };
}

AssignmentId addTaskWithAssignmentAndMetrics(
    const IndoorPlanId& planId,
    const Test50PValueByLvlDeviceProvider& value50Percentile,
    pqxx::transaction_base& txn) {
    auto [task, assignment] = initCompletedTaskAndAssignment(
        planId, USER_ID, txn,
        [] (auto&& task, auto&&) {
            task.setIsTest(true);
            task.setSkipEvaluation(false);
        }
    );

    for (const auto& [lvlId, byDevice] : value50Percentile) {
        for (const auto& [device, byProvider] : byDevice) {
            for (const auto& [provider, value] : byProvider) {
                if (lvlId != LEVEL_ID_NULL) {
                    impl::updateRadioMapMetricsInDB(
                        planId,
                        assignment.id(),
                        chrono::TimePoint::clock::now(),
                        device,
                        provider,
                        lvlId,
                        getSomeLevelMetrics(value),
                        txn
                    );
                } else {
                    impl::updateAggregatedRadioMapMetricsInDB(
                        planId,
                        assignment.id(),
                        chrono::TimePoint::clock::now(),
                        device,
                        getSomeLevelMetrics(value),
                        txn
                    );
                }
            }
        }
    }
    return assignment.id();
}

} // namespace

Y_UNIT_TEST(load_specific_assignment_percentiles_no_throw)
{
    auto txn = pgPool().masterWriteableTransaction();
    const auto ASSIGNMENT_ID =
        addTaskWithAssignmentAndMetrics(INDOOR_PLAN_ID, TEST_PERCENTILE_50_ERROR_NON_ZERO, *txn);

    UNIT_ASSERT_NO_EXCEPTION(
        impl::loadPercentiles(
            ASSIGNMENT_ID,
            pe::DeviceType::Android,
            *txn)
    );

    const AssignmentId NON_EXISTENT_ID = 12345;
    UNIT_ASSERT_NO_EXCEPTION(
        impl::loadPercentiles(
            NON_EXISTENT_ID,
            pe::DeviceType::Android,
            *txn)
    );
}

Y_UNIT_TEST(load_specific_assignment_percentiles)
{
    auto txn = pgPool().masterWriteableTransaction();

    addTaskWithAssignmentAndMetrics(INDOOR_PLAN_ID, TEST_PERCENTILE_50_ERROR_ZERO, *txn);
    addTaskWithAssignmentAndMetrics("SomeAnotherPlanId", TEST_PERCENTILE_50_ERROR_ZERO, *txn);
    const auto LATEST_ASSIGNMENT_ID = addTaskWithAssignmentAndMetrics(INDOOR_PLAN_ID, TEST_PERCENTILE_50_ERROR_NON_ZERO, *txn);
    addTaskWithAssignmentAndMetrics(INDOOR_PLAN_ID, TEST_PERCENTILE_50_ERROR_ZERO, *txn);

    const auto res = impl::loadPercentiles(
        LATEST_ASSIGNMENT_ID,
        DeviceType::Android,
        *txn
    );

    UNIT_ASSERT_EQUAL(res.size(), 2u);
    UNIT_ASSERT(res.contains(LEVEL_ID_1));
    UNIT_ASSERT(res.contains(LEVEL_ID_2));

    {
        const auto& percentiles = res.at(LEVEL_ID_1).at(LocationProvider::Fused01);

        UNIT_ASSERT(percentiles.at(50).has_value());
        UNIT_ASSERT_DOUBLES_EQUAL(0.1, percentiles.at(50).value(), 1e-3);

        UNIT_ASSERT(percentiles.at(75).has_value());
        UNIT_ASSERT_DOUBLES_EQUAL(0.11, percentiles.at(75).value(), 1e-3);

        UNIT_ASSERT(!percentiles.at(90).has_value());
        UNIT_ASSERT(!percentiles.at(95).has_value());
    }

    {
        const auto& percentiles = res.at(LEVEL_ID_1).at(LocationProvider::Fused05);
        UNIT_ASSERT(percentiles.at(50).has_value());
        UNIT_ASSERT_DOUBLES_EQUAL(0.5, percentiles.at(50).value(), 1e-3);
    }

    {
        const auto& percentiles = res.at(LEVEL_ID_2).at(LocationProvider::Fused01);
        UNIT_ASSERT(percentiles.at(50).has_value());
        UNIT_ASSERT_DOUBLES_EQUAL(0.12, percentiles.at(50).value(), 1e-3);
    }

    {
        const auto& percentiles = res.at(LEVEL_ID_2).at(LocationProvider::Fused05);
        UNIT_ASSERT(percentiles.at(50).has_value());
        UNIT_ASSERT_DOUBLES_EQUAL(0.052, percentiles.at(50).value(), 1e-3);
    }
}

Y_UNIT_TEST(load_n_latest_percentiles_non_existent_plan_empty)
{
    auto txn = pgPool().masterReadOnlyTransaction();
    auto percentiles = impl::loadNLatestIndoorPercentiles(
        "SomePlanID", DeviceType::Android, 42u, 100l, *txn);

    UNIT_ASSERT(percentiles.empty());
}

Y_UNIT_TEST(load_n_latest_percentiles_loads_latest)
{
    // Create multiple assignments for different plans
    const auto [tooSmallAssignmentId, anotherPlanAssignment, ASSIGNMENT_IDS] = [&pool = pgPool()] {
        auto txn = pool.masterWriteableTransaction();
        std::vector<AssignmentId> result;

        addTaskWithAssignmentAndMetrics("SomeAnotherPlan0", TEST_PERCENTILE_50_ERROR_NON_ZERO, *txn);
        const auto smallAssignmentId = addTaskWithAssignmentAndMetrics("SomeAnotherPlan1", TEST_PERCENTILE_50_ERROR_NON_ZERO, *txn);

        result.push_back(addTaskWithAssignmentAndMetrics(INDOOR_PLAN_ID, TEST_PERCENTILE_50_ERROR_NON_ZERO, *txn));

        const auto someAnotherPlanAssignment = addTaskWithAssignmentAndMetrics("SomeAnotherPlan2", TEST_PERCENTILE_50_ERROR_NON_ZERO, *txn);
        result.push_back(addTaskWithAssignmentAndMetrics(INDOOR_PLAN_ID, TEST_PERCENTILE_50_ERROR_ZERO, *txn));

        addTaskWithAssignmentAndMetrics("SomeAnotherPlan2", TEST_PERCENTILE_50_ERROR_NON_ZERO, *txn);
        result.push_back(addTaskWithAssignmentAndMetrics(INDOOR_PLAN_ID, TEST_PERCENTILE_50_ERROR_NON_ZERO, *txn));

        addTaskWithAssignmentAndMetrics("SomeAnotherPlan3", TEST_PERCENTILE_50_ERROR_NON_ZERO, *txn);
        txn->commit();
        return std::make_tuple(smallAssignmentId, someAnotherPlanAssignment, result);
    }();

    auto txn = pgPool().masterReadOnlyTransaction();

    { // No percentiles should be found for assignment_id <= than provided
        auto percentiles = impl::loadNLatestIndoorPercentiles(
            INDOOR_PLAN_ID, DeviceType::Android, 2u, tooSmallAssignmentId, *txn);
        UNIT_ASSERT(percentiles.empty());
    }

    { // Exception is thown if loaded plan_id for provided assignment_id differs from provider planId
        UNIT_ASSERT_EXCEPTION(
            impl::loadNLatestIndoorPercentiles(
                INDOOR_PLAN_ID, DeviceType::Android, 2u, anotherPlanAssignment, *txn),
            maps::RuntimeError
        );
    }

    { // 1 percentiles could be loaded
        const auto loaded =
            impl::loadNLatestIndoorPercentiles(INDOOR_PLAN_ID, DeviceType::Android, 2u, ASSIGNMENT_IDS.at(0), *txn);
        const auto& percentilesByAssignment = loaded.at(LEVEL_ID_1).at(LocationProvider::Fused01);
        UNIT_ASSERT_EQUAL(percentilesByAssignment.size(), 1u);
        UNIT_ASSERT(percentilesByAssignment.contains(ASSIGNMENT_IDS.at(0)));
    }

    { // 2 percentiles could be loaded when available
        const auto loaded =
            impl::loadNLatestIndoorPercentiles(INDOOR_PLAN_ID, DeviceType::Android, 2u, ASSIGNMENT_IDS.at(1), *txn);
        const auto& percentilesByAssignment = loaded.at(LEVEL_ID_1).at(LocationProvider::Fused01);
        UNIT_ASSERT_EQUAL(percentilesByAssignment.size(), 2u);
        UNIT_ASSERT(percentilesByAssignment.contains(ASSIGNMENT_IDS.at(0)));
        UNIT_ASSERT(percentilesByAssignment.contains(ASSIGNMENT_IDS.at(1)));
    }

        const auto loaded =
            impl::loadNLatestIndoorPercentiles(INDOOR_PLAN_ID, DeviceType::Android, 2u, ASSIGNMENT_IDS.at(2), *txn);
        UNIT_ASSERT_EQUAL(loaded.size(), 2u);

    { // 2 latest assignments are loaded when available
        const auto& percentilesByAssignment = loaded.at(LEVEL_ID_1).at(LocationProvider::Fused01);
        UNIT_ASSERT_EQUAL(percentilesByAssignment.size(), 2u);
        UNIT_ASSERT(percentilesByAssignment.contains(ASSIGNMENT_IDS.at(1)));
        UNIT_ASSERT(percentilesByAssignment.contains(ASSIGNMENT_IDS.at(2)));
    }

    // Correct data is loaded for all levels, all providers and all assignments.
    { // Check 1st level data.
        const auto levelId = LEVEL_ID_1;
        UNIT_ASSERT(loaded.contains(levelId));

        { // Check Fused01 provider.
            const auto provider = LocationProvider::Fused01;
            UNIT_ASSERT(loaded.at(levelId).contains(provider));
            const auto& percentilesByAssignmentId = loaded.at(levelId).at(provider);

            // ASSIGNMENT_IDS.at(0) is too small and not loaded.
            UNIT_ASSERT(!percentilesByAssignmentId.contains(ASSIGNMENT_IDS.at(0)));

            // ASSIGNMENT_IDS.at(1) is loaded.
            UNIT_ASSERT(percentilesByAssignmentId.contains(ASSIGNMENT_IDS.at(1)));
            { // ASSIGNMENT_IDS.at(1) percentiles data is correct.
                const auto& percentiles = percentilesByAssignmentId.at(ASSIGNMENT_IDS.at(1));
                UNIT_ASSERT(percentiles.at(50).has_value());
                UNIT_ASSERT_DOUBLES_EQUAL(0.0, percentiles.at(50).value(), 1e-3);
            }

            // ASSIGNMENT_IDS.at(2) is loaded.
            UNIT_ASSERT(percentilesByAssignmentId.contains(ASSIGNMENT_IDS.at(2)));
            { // ASSIGNMENT_IDS.at(2) percentiles data is correct.
                const auto& percentiles = percentilesByAssignmentId.at(ASSIGNMENT_IDS.at(2));
                UNIT_ASSERT(percentiles.at(50).has_value());
                UNIT_ASSERT_DOUBLES_EQUAL(0.1, percentiles.at(50).value(), 1e-3);
            }
        }

        { // Check Fused05 provider.
            const auto provider = LocationProvider::Fused05;
            UNIT_ASSERT(loaded.at(levelId).contains(provider));
            const auto& percentilesByAssignmentId = loaded.at(levelId).at(provider);

            // ASSIGNMENT_IDS.at(0) is too small and not loaded.
            UNIT_ASSERT(!percentilesByAssignmentId.contains(ASSIGNMENT_IDS.at(0)));

            // ASSIGNMENT_IDS.at(1) is loaded.
            UNIT_ASSERT(percentilesByAssignmentId.contains(ASSIGNMENT_IDS.at(1)));
            { // ASSIGNMENT_IDS.at(1) percentiles data is correct.
                const auto& percentiles = percentilesByAssignmentId.at(ASSIGNMENT_IDS.at(1));
                UNIT_ASSERT(percentiles.at(50).has_value());
                UNIT_ASSERT_DOUBLES_EQUAL(0.0, percentiles.at(50).value(), 1e-3);
            }

            // ASSIGNMENT_IDS.at(2) is loaded.
            UNIT_ASSERT(percentilesByAssignmentId.contains(ASSIGNMENT_IDS.at(2)));
            { // ASSIGNMENT_IDS.at(2) percentiles data is correct.
                const auto& percentiles = percentilesByAssignmentId.at(ASSIGNMENT_IDS.at(2));
                UNIT_ASSERT(percentiles.at(50).has_value());
                UNIT_ASSERT_DOUBLES_EQUAL(0.5, percentiles.at(50).value(), 1e-3);
            }
        }
    }

    { // Check LEVEL_ID_2 level data.
        const auto levelId = LEVEL_ID_2;
        UNIT_ASSERT(loaded.contains(levelId));

        { // Check percentiles for Fused01 provider.
            const auto provider = LocationProvider::Fused01;
            UNIT_ASSERT(loaded.at(levelId).contains(provider));
            const auto& percentilesByAssignmentId = loaded.at(levelId).at(provider);

            // ASSIGNMENT_IDS.at(0) is too small and not loaded.
            UNIT_ASSERT(!percentilesByAssignmentId.contains(ASSIGNMENT_IDS.at(0)));

            // ASSIGNMENT_IDS.at(1) is loaded.
            UNIT_ASSERT(percentilesByAssignmentId.contains(ASSIGNMENT_IDS.at(1)));
            { // ASSIGNMENT_IDS.at(1) percentiles data is correct.
                const auto& percentiles = percentilesByAssignmentId.at(ASSIGNMENT_IDS.at(1));
                UNIT_ASSERT(percentiles.at(50).has_value());
                UNIT_ASSERT_DOUBLES_EQUAL(0.0, percentiles.at(50).value(), 1e-3);
            }

            // ASSIGNMENT_IDS.at(2) is loaded.
            UNIT_ASSERT(percentilesByAssignmentId.contains(ASSIGNMENT_IDS.at(2)));
            { // ASSIGNMENT_IDS.at(2) percentiles data is correct.
                const auto& percentiles = percentilesByAssignmentId.at(ASSIGNMENT_IDS.at(2));
                UNIT_ASSERT(percentiles.at(50).has_value());
                UNIT_ASSERT_DOUBLES_EQUAL(0.12, percentiles.at(50).value(), 1e-3);
            }
        }

        { // Check percentiles for Fused05 provider.
            const auto provider = LocationProvider::Fused05;
            UNIT_ASSERT(loaded.at(levelId).contains(provider));
            const auto& percentilesByAssignmentId = loaded.at(levelId).at(provider);

            // ASSIGNMENT_IDS.at(0) is too small and not loaded.
            UNIT_ASSERT(!percentilesByAssignmentId.contains(ASSIGNMENT_IDS.at(0)));

            // ASSIGNMENT_IDS.at(1) is loaded.
            UNIT_ASSERT(percentilesByAssignmentId.contains(ASSIGNMENT_IDS.at(1)));
            { // ASSIGNMENT_IDS.at(1) percentiles data is correct.
                const auto& percentiles = percentilesByAssignmentId.at(ASSIGNMENT_IDS.at(1));
                UNIT_ASSERT(percentiles.at(50).has_value());
                UNIT_ASSERT_DOUBLES_EQUAL(0.0, percentiles.at(50).value(), 1e-3);
            }

            // ASSIGNMENT_IDS.at(2) is loaded.
            UNIT_ASSERT(percentilesByAssignmentId.contains(ASSIGNMENT_IDS.at(2)));
            { // ASSIGNMENT_IDS.at(1) percentiles data is correct.
                const auto& percentiles = percentilesByAssignmentId.at(ASSIGNMENT_IDS.at(2));
                UNIT_ASSERT(percentiles.at(50).has_value());
                UNIT_ASSERT_DOUBLES_EQUAL(0.052, percentiles.at(50).value(), 1e-3);
            }
        }
    }
}

Y_UNIT_TEST(update_optimal_k_fuse_throw_no_new_tasks)
{
    auto txn = pgPool().masterWriteableTransaction();
    auto kFuseByLevel = impl::updateOptimalKFuse(INDOOR_PLAN_ID, AssignmentId(42), 4, DeviceType::Android, *txn);
    UNIT_ASSERT(kFuseByLevel.empty());
}

Y_UNIT_TEST(update_optimal_k_fuse)
{
    auto txn = pgPool().masterWriteableTransaction();
    const auto assignmentId = addTaskWithAssignmentAndMetrics(INDOOR_PLAN_ID, TEST_PERCENTILE_50_ERROR_NON_ZERO, *txn);

    const auto calculated = impl::updateOptimalKFuse(INDOOR_PLAN_ID, assignmentId, 4, DeviceType::Android, *txn);
    const auto loaded = pe::loadLatestFuseCoefficients(INDOOR_PLAN_ID, *txn);
    UNIT_ASSERT(calculated == loaded);

    {
        UNIT_ASSERT(loaded.contains(LEVEL_ID_1));
        UNIT_ASSERT_DOUBLES_EQUAL(loaded.at(LEVEL_ID_1), 0.1, 1e-3);
        UNIT_ASSERT(loaded.contains(LEVEL_ID_2));
        UNIT_ASSERT_DOUBLES_EQUAL(loaded.at(LEVEL_ID_2), 0.5, 1e-3);
    }
}

} // Y_UNIT_TEST_SUITE_F

} // namespace maps::mirc::radiomap_metrics::test
