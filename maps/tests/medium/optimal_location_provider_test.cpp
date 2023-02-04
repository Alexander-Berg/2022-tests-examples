#include <maps/indoor/libs/radiomap_metrics/optimal_location_provider.h>

#include <library/cpp/testing/unittest/gtest.h>

#include <maps/indoor/libs/unittest/fixture.h>

namespace maps::indoor::positioning_estimator::tests {

namespace {

const std::string OPTIMAL_COEFFICIENT_GARDEN_TABLE = "ugc.lbs_fuse_coefficient";
const std::string OPTIMAL_COEFFICIENT_HISTORY_TABLE = "ugc.optimal_lbs_fuse_coefficient";

size_t countRows(const std::string& tableName, pgpool3::Pool& pool)
{
    auto txn = pool.masterReadOnlyTransaction();
    const auto row = txn->exec1(fmt::format(
        "SELECT COUNT(*) FROM {tableName}", fmt::arg("tableName", tableName)
    ));
    return row[0].as<size_t>();
}

FuseCoefficient getGardenFuseCoefficient(
    const IndoorPlanId planId,
    const IndoorLevelId levelId,
    pgpool3::Pool& pool)
{
    static const std::string query = R"end(
        SELECT value
        FROM {table}
        WHERE
            indoor_plan_id = {plan_id}
            AND
            indoor_level_universal_id = {level_id}
    )end";

    auto txn = pool.masterReadOnlyTransaction();
    return txn->exec1(fmt::format(
        query,
        fmt::arg("table", OPTIMAL_COEFFICIENT_GARDEN_TABLE),
        fmt::arg("plan_id", txn->quote(planId)),
        fmt::arg("level_id", txn->quote(levelId))
    ))[0].as<double>();
}

struct TestKFuseOnEmptyDB : mirc::unittest::Fixture, ::testing::Test {};

} // namespace

TEST_F(TestKFuseOnEmptyDB, NoCoefficientsToSave)
{
    EXPECT_EQ(countRows(OPTIMAL_COEFFICIENT_HISTORY_TABLE, pgPool()), 0u);
    {
        auto txn = pgPool().masterWriteableTransaction();
        ASSERT_NO_THROW(saveFuseCoefficients({}, *txn));
        txn->commit();
    }
    EXPECT_EQ(countRows(OPTIMAL_COEFFICIENT_HISTORY_TABLE, pgPool()), 0u);
}

TEST_F(TestKFuseOnEmptyDB, AddSingleValueToHistory)
{
    const IndoorPlanId PLAN_ID = "IndoorPlanId";
    const IndoorLevelId LEVEL_ID = "IndoorLevelId";
    const mirc::db::TId ASSIGNMENT_ID = 42;
    const double FUSE_COEFFICIENT = 0.5;
    EXPECT_EQ(countRows(OPTIMAL_COEFFICIENT_HISTORY_TABLE, pgPool()), 0u);
    {
        auto txn = pgPool().masterWriteableTransaction();
        ASSERT_NO_THROW(
            saveFuseCoefficients({{PLAN_ID, LEVEL_ID, ASSIGNMENT_ID, FUSE_COEFFICIENT}}, *txn)
        );
        txn->commit();
    }
    EXPECT_EQ(countRows(OPTIMAL_COEFFICIENT_HISTORY_TABLE, pgPool()), 1u);
}

TEST_F(TestKFuseOnEmptyDB, AddManyValuesToHistory)
{
    ASSERT_EQ(countRows(OPTIMAL_COEFFICIENT_HISTORY_TABLE, pgPool()), 0u);

    const IndoorPlanId PLAN_ID = "IndoorPlanId";
    const IndoorLevelId LEVEL_ID = "IndoorLevelId";
    const mirc::db::TId ASSIGNMENT_ID = 42;
    const double FUSE_COEFFICIENT = 0.5;
    std::vector<OptimalKFuse> kFuses;
    const size_t ASSIGNMENTS_COUNT = 1000;
    for (size_t i = 0; i < ASSIGNMENTS_COUNT; ++i) {
        kFuses.emplace_back(OptimalKFuse{
            .planId = PLAN_ID,
            .levelId = LEVEL_ID,
            .assignmentId = static_cast<AssignmentId>(ASSIGNMENT_ID + i),
            .kFuse = FUSE_COEFFICIENT}
        );
    }
    {
        auto txn = pgPool().masterWriteableTransaction();
        ASSERT_NO_THROW(
            saveFuseCoefficients(kFuses, *txn)
        );
        txn->commit();
    }
    EXPECT_EQ(countRows(OPTIMAL_COEFFICIENT_HISTORY_TABLE, pgPool()), ASSIGNMENTS_COUNT);
}

TEST_F(TestKFuseOnEmptyDB, AddSingleValueToGarden)
{
    const IndoorPlanId PLAN_ID = "IndoorPlanId";
    const IndoorLevelId LEVEL_ID = "IndoorLevelId";
    const double FUSE_COEFFICIENT = 0.5;
    EXPECT_EQ(countRows(OPTIMAL_COEFFICIENT_GARDEN_TABLE, pgPool()), 0u);
    {
        auto txn = pgPool().masterWriteableTransaction();
        ASSERT_NO_THROW(
            saveFuseCoefficientsToGardenTable(PLAN_ID, KFuseByLevel{{LEVEL_ID, FUSE_COEFFICIENT}}, *txn)
        );
        txn->commit();
    }
    EXPECT_EQ(countRows(OPTIMAL_COEFFICIENT_GARDEN_TABLE, pgPool()), 1u);

    EXPECT_DOUBLE_EQ(
        getGardenFuseCoefficient(PLAN_ID, LEVEL_ID, pgPool()),
        FUSE_COEFFICIENT
    );
}

TEST_F(TestKFuseOnEmptyDB, AddSameValue)
{
    const IndoorPlanId PLAN_ID = "IndoorPlanId";
    const IndoorLevelId LEVEL_ID = "IndoorLevelId";
    auto addSameCoefficient = [&]{
        const mirc::db::TId ASSIGNMENT_ID = 42;
        const double FUSE_COEFFICIENT = 0.5;
        auto txn = pgPool().masterWriteableTransaction();
        saveFuseCoefficients({{PLAN_ID, LEVEL_ID, ASSIGNMENT_ID, FUSE_COEFFICIENT}}, *txn);
        saveFuseCoefficientsToGardenTable(PLAN_ID, {{LEVEL_ID, FUSE_COEFFICIENT}}, *txn);
        txn->commit();
    };

    EXPECT_EQ(countRows(OPTIMAL_COEFFICIENT_HISTORY_TABLE, pgPool()), 0u);
    EXPECT_EQ(countRows(OPTIMAL_COEFFICIENT_GARDEN_TABLE, pgPool()), 0u);
    addSameCoefficient();
    EXPECT_EQ(countRows(OPTIMAL_COEFFICIENT_HISTORY_TABLE, pgPool()), 1u);
    EXPECT_EQ(countRows(OPTIMAL_COEFFICIENT_GARDEN_TABLE, pgPool()), 1u);

    ASSERT_THROW(addSameCoefficient(), pqxx::unique_violation);
    EXPECT_EQ(countRows(OPTIMAL_COEFFICIENT_HISTORY_TABLE, pgPool()), 1u);
    EXPECT_EQ(countRows(OPTIMAL_COEFFICIENT_GARDEN_TABLE, pgPool()), 1u);
}

TEST_F(TestKFuseOnEmptyDB, GardenValueIsUpdated)
{
    const IndoorPlanId PLAN_ID = "IndoorPlanId";
    const IndoorLevelId LEVEL_ID = "IndoorLevelId";
    auto addCoefficient = [&] (const double k) {
        auto txn = pgPool().masterWriteableTransaction();
        saveFuseCoefficientsToGardenTable(PLAN_ID, KFuseByLevel{{LEVEL_ID, k}}, *txn);
        txn->commit();
    };

    EXPECT_EQ(countRows(OPTIMAL_COEFFICIENT_GARDEN_TABLE, pgPool()), 0u);
    addCoefficient(0.1);
    EXPECT_EQ(countRows(OPTIMAL_COEFFICIENT_GARDEN_TABLE, pgPool()), 1u);
    EXPECT_DOUBLE_EQ(
        getGardenFuseCoefficient(PLAN_ID, LEVEL_ID, pgPool()),
        0.1
    );

    addCoefficient(0.5);
    EXPECT_EQ(countRows(OPTIMAL_COEFFICIENT_GARDEN_TABLE, pgPool()), 1u);
    EXPECT_DOUBLE_EQ(
        getGardenFuseCoefficient(PLAN_ID, LEVEL_ID, pgPool()),
        0.5
    );
}

namespace {

struct TestSavingOptimalKFuse : mirc::unittest::Fixture, ::testing::Test {
    using mirc::unittest::Fixture::SetUp;
    void SetUp() override {
        auto txn = pgPool().masterWriteableTransaction();
        txn->exec(R"end(
            INSERT INTO
                ugc.optimal_lbs_fuse_coefficient (
                    indoor_plan_id,
                    indoor_level_universal_id,
                    assignment_id,
                    value
                )
            VALUES
                ('Plan1', 'DeletedLevel', 1,  0.1),
                ('Plan1', 'Lvl1', 1,   0.9),

                ('Plan2', 'Lvl1', 2,  0.2),
                ('Plan2', 'Lvl2', 2,  0.2),

                ('Plan1', 'Lvl1', 11,  0.11),

                ('Plan2', 'Lvl1', 22, 0.21),
                ('Plan2', 'Lvl2', 22, 0.22)
        )end");

        txn->exec(R"end(
            INSERT INTO
                ugc.lbs_fuse_coefficient (
                    indoor_plan_id,
                    indoor_level_universal_id,
                    value
                )
            VALUES
                ('Plan1', 'Lvl1',  0.11),
                ('Plan2', 'Lvl1',  0.21),
                ('Plan2', 'Lvl2',  0.22)
        )end");
        txn->commit();
    }
};

KFuseByPlanId loadFuseCoefficientsFromGardenTable(
    pqxx::transaction_base& txn)
{
    using indoor::positioning_estimator::IndoorPlanId;
    using indoor::positioning_estimator::IndoorLevelId;
    static const std::string query = R"end(
        SELECT indoor_plan_id, indoor_level_universal_id, value
        FROM ugc.lbs_fuse_coefficient
    )end";
    const auto rows = txn.exec(query);
    KFuseByPlanId result;
    for (const auto& row : rows) {
        auto planId = row.at("indoor_plan_id").as<IndoorPlanId>();
        auto lvlId = row.at("indoor_level_universal_id").as<IndoorLevelId>();
        auto value = row.at("value").as<FuseCoefficient>();
        const auto [_, ok] = result[std::move(planId)].emplace(
            std::make_pair(std::move(lvlId), value)
        );
        REQUIRE(ok, "Duplicate level id for a selected plan id");
    }
    return result;
}

} // namespace

TEST_F(TestSavingOptimalKFuse, loadLatestFuseCoefficientsNoThrow){
    auto txn = pgPool().masterReadOnlyTransaction();

    EXPECT_NO_THROW(
        loadLatestFuseCoefficients({}, *txn)
    );
    EXPECT_NO_THROW(
        loadLatestFuseCoefficients({"NonExistentPlanId"}, *txn)
    );
    EXPECT_NO_THROW(
        loadLatestFuseCoefficients({"Plan1"}, *txn)
    );
    EXPECT_NO_THROW(
        loadLatestFuseCoefficients({"Plan1", "Plan2"}, *txn)
    );
}

TEST_F(TestSavingOptimalKFuse, loadLatestFuseCoefficientsEmpty){
    auto txn = pgPool().masterReadOnlyTransaction();

    const auto coefs = loadLatestFuseCoefficients({"NonExistentPlan"}, *txn);
    ASSERT_TRUE(coefs.empty());
}

TEST_F(TestSavingOptimalKFuse, loadLatestFuseCoefficientsLatestValues)
{
    auto txn = pgPool().masterReadOnlyTransaction();

    const auto plan1Coefs = loadLatestFuseCoefficients("Plan1", *txn);
    ASSERT_TRUE(!plan1Coefs.empty());
    const auto plan2Coefs = loadLatestFuseCoefficients("Plan2", *txn);
    ASSERT_TRUE(!plan2Coefs.empty());
    const auto nonExistentPlanCoefs = loadLatestFuseCoefficients("NonExistentPlan", *txn);
    ASSERT_TRUE(nonExistentPlanCoefs.empty());

    ASSERT_EQ(plan1Coefs.size(), 1u);
    EXPECT_DOUBLE_EQ(0.11, plan1Coefs.at("Lvl1"));

    ASSERT_EQ(plan2Coefs.size(), 2u);
    EXPECT_DOUBLE_EQ(0.21, plan2Coefs.at("Lvl1"));
    EXPECT_DOUBLE_EQ(0.22, plan2Coefs.at("Lvl2"));
}

TEST_F(TestSavingOptimalKFuse, SaveNewData)
{
    auto txn = pgPool().masterWriteableTransaction();
    ASSERT_NO_THROW(
        saveFuseCoefficientsToGardenTable(
            "NewPlan",
            KFuseByLevel{
                {"Lvl1", 0.1},
                {"Lvl2", 0.2}
            },
            *txn
        )
    );
    const auto loaded = tests::loadFuseCoefficientsFromGardenTable(*txn);
    ASSERT_EQ(loaded.size(), 3u);
    ASSERT_TRUE(loaded.contains("NewPlan"));
    ASSERT_EQ(loaded.at("NewPlan").size(), 2u);
    EXPECT_DOUBLE_EQ(0.1, loaded.at("NewPlan").at("Lvl1"));
    EXPECT_DOUBLE_EQ(0.2, loaded.at("NewPlan").at("Lvl2"));
}

TEST_F(TestSavingOptimalKFuse, UpdateData)
{
    auto txn = pgPool().masterWriteableTransaction();

    {
        const auto loaded = tests::loadFuseCoefficientsFromGardenTable(*txn);
        ASSERT_EQ(loaded.size(), 2u);
        ASSERT_EQ(loaded.at("Plan2").size(), 2u);
        EXPECT_DOUBLE_EQ(0.21, loaded.at("Plan2").at("Lvl1"));
        EXPECT_DOUBLE_EQ(0.22, loaded.at("Plan2").at("Lvl2"));
    }

    ASSERT_NO_THROW(
        saveFuseCoefficientsToGardenTable(
            "Plan2",
            KFuseByLevel{
                {"Lvl1", 0.91},
                {"Lvl2", 0.92}
            },
            *txn));
    {
        const auto loaded = tests::loadFuseCoefficientsFromGardenTable(*txn);
        ASSERT_EQ(loaded.size(), 2u);
        ASSERT_EQ(loaded.at("Plan2").size(), 2u);
        EXPECT_DOUBLE_EQ(0.91, loaded.at("Plan2").at("Lvl1"));
        EXPECT_DOUBLE_EQ(0.92, loaded.at("Plan2").at("Lvl2"));
    }
}

TEST_F(TestSavingOptimalKFuse, RemoveSomeLevel)
{
    auto txn = pgPool().masterWriteableTransaction();

    {
        const auto loaded = tests::loadFuseCoefficientsFromGardenTable(*txn);
        ASSERT_EQ(loaded.size(), 2u);
        ASSERT_EQ(loaded.at("Plan2").size(), 2u);
        EXPECT_DOUBLE_EQ(0.21, loaded.at("Plan2").at("Lvl1"));
        EXPECT_DOUBLE_EQ(0.22, loaded.at("Plan2").at("Lvl2"));
    }

    ASSERT_NO_THROW(
        saveFuseCoefficientsToGardenTable(
            "Plan2",
            KFuseByLevel{
                {"Lvl1", 0.17},
            },
            *txn));
    {
        const auto loaded = tests::loadFuseCoefficientsFromGardenTable(*txn);
        ASSERT_EQ(loaded.size(), 2u);
        ASSERT_EQ(loaded.at("Plan2").size(), 1u);
        EXPECT_DOUBLE_EQ(0.17, loaded.at("Plan2").at("Lvl1"));
    }
}

} // namespace maps::indoor::positioning_estimator::tests
