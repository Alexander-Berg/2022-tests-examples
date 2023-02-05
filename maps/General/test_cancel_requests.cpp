#include <maps/sprav/callcenter/libs/dao/task.h>

#include <maps/sprav/callcenter/libs/test_helpers/db_fixture.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gtest_protobuf/matcher.h>


namespace maps::sprav::callcenter::tests {

namespace {

auto makeTx() {
    static DbFixture db;
    return db.pool().masterWriteableTransaction();
}

} // namespace

TEST(RequestsForCancel, FetchTasksForCancel) {
    auto tx = makeTx();
    tx->exec(
        "INSERT INTO tasker.task_active "
            "(id, yang_pool_id, yang_task_id, status) "
        "VALUES "
            "('1', 'yangPool1', 'yangTask1', 'on_hold'), " // issued to yang
            "('2', 'yangPool1', 'yangTask2', 'on_hold'), " // issued to yang
            "('5', null,         null,       'in_progress') " // not issued
    );
    tx->exec(
        "INSERT INTO tasker.task_request_active "
            "(task_id, request_id) "
        "VALUES "
            "('1', '1'),"
            "('1', '2'),"
            "('1', '3')," // not in list of ids
            "('2', '4'),"
            "('3', '5'),"
            "('4', '6'),"
            "('5', '7')"
    );

    std::set<uint64_t> requestIds{1, 2, 4, 5, 6, 7};
    const auto& result = dao::TaskActive::fetchTasksForCancel(requestIds, *tx);
    EXPECT_THAT(result.size(), 2);
    {
        dao::TaskIdWithYangInfo task1{
            .taskId = 1,
            .yangPoolId = "yangPool1",
            .yangTaskId = "yangTask1"
        };
        const auto task1It = result.find(task1);
        EXPECT_TRUE(task1It != result.end());
        const auto& requests = task1It->second;
        EXPECT_THAT(requests, ::testing::UnorderedElementsAreArray({1, 2}));
    }
    {
        dao::TaskIdWithYangInfo task{
            .taskId = 2,
            .yangPoolId = "yangPool1",
            .yangTaskId = "yangTask2"
        };
        const auto taskIt = result.find(task);
        EXPECT_TRUE(taskIt != result.end());
        const auto& requests = taskIt->second;
        EXPECT_THAT(requests, ::testing::UnorderedElementsAreArray({4}));
    }
}

TEST(RequestsForCancel, ResetYangPool) {
    auto tx = makeTx();
    tx->exec(
        "INSERT INTO tasker.task_active "
            "(id,  yang_pool_id, yang_task_id, yang_assessment_id) "
        "VALUES "
            "('1', 'yangPool1', 'yangTask1',   null), "
            "('2', 'yangPool1', 'yangTask2',   null), "
            "('3', 'yangPool2', 'yangTask3',  'yang_assessment1'), "
            "('4', 'yangPool3',  null,        'yang_assessment2'), "
            "('5',  null,        null,         null) "
    );

    EXPECT_TRUE(dao::TaskActive::resetYangPool({1, "yangPool1", "yangTask1"}, *tx));
    // do not update row for task 3: yang_assessment_id is not null
    EXPECT_FALSE(dao::TaskActive::resetYangPool({3, "yangPool2", "yangTask3"}, *tx));
    const auto& rows = tx->exec("SELECT * from tasker.task_active ORDER BY id");
    EXPECT_THAT(rows.size(), 5);
    const auto& row1 = rows[0];
    EXPECT_THAT(row1["id"].as<std::string>(), "1");
    EXPECT_TRUE(row1["yang_pool_id"].is_null());
    EXPECT_TRUE(row1["yang_task_id"].is_null());

    const auto& row2 = rows[1];
    EXPECT_THAT(row2["id"].as<std::string>(), "2");
    EXPECT_THAT(row2["yang_pool_id"].as<std::string>(), "yangPool1");
    EXPECT_THAT(row2["yang_task_id"].as<std::string>(), "yangTask2");

    const auto& row3 = rows[2];
    EXPECT_THAT(row3["id"].as<std::string>(), "3");
    EXPECT_THAT(row3["yang_pool_id"].as<std::string>(), "yangPool2");
    EXPECT_THAT(row3["yang_task_id"].as<std::string>(), "yangTask3");

    const auto& row4 = rows[3];
    EXPECT_THAT(row4["id"].as<std::string>(), "4");
    EXPECT_THAT(row4["yang_pool_id"].as<std::string>(), "yangPool3");
}

} // maps::sprav::callcenter::tests
