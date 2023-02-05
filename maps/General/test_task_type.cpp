#include <maps/sprav/callcenter/libs/dao/task_type.h>
#include <library/cpp/protobuf/json/json2proto.h>

#include <maps/sprav/callcenter/libs/test_helpers/db_fixture.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gtest_protobuf/matcher.h>
#include <string>


namespace maps::sprav::callcenter::tests {

namespace {
    auto createTransaction() {
        static DbFixture db;
        return db.pool().masterWriteableTransaction();
    }

    const TString TEST_ID = "test_task_type"; 
} // namespace

TEST(TaskTypeTest, CreateAndGet) {
    auto tx = createTransaction();
    proto::TaskType taskType;
    taskType.set_id(TEST_ID);
    taskType.mutable_config()->set_task_selection_order_mode(proto::TaskTypeConfig::TaskSelectionOrderMode::INVERT);
    taskType.mutable_config()->set_join_requests_by_uid(true);
    dao::TaskType::create(taskType, *tx);

    EXPECT_THAT(dao::TaskType::get(TEST_ID, *tx).value(), NGTest::EqualsProto(taskType));
}

TEST(TaskTypeTest, NotFound) {
    auto tx = createTransaction();
    EXPECT_FALSE(dao::TaskType::get(TEST_ID, *tx).has_value());
}

TEST(TaskTypeTest, CreateAndUpdate) {
    auto tx = createTransaction();
    proto::TaskType taskType;
    taskType.set_id(TEST_ID);
    taskType.mutable_config();
    dao::TaskType::create(taskType, *tx);
    taskType = dao::TaskType::get(TEST_ID, *tx).value();
    taskType.mutable_config()->set_join_requests_by_uid(true);
    dao::TaskType::update(taskType, *tx);
    EXPECT_THAT(dao::TaskType::get(TEST_ID, *tx).value(), NGTest::EqualsProto(taskType));
}

TEST(TaskTypeTest, MultipleCreateConflict) {
    auto tx = createTransaction();
    proto::TaskType taskType;
    taskType.set_id(TEST_ID);
    taskType.mutable_config();
    dao::TaskType::create(taskType, *tx);
    EXPECT_THROW(dao::TaskType::create(taskType, *tx), pqxx::unique_violation);
}

TEST(TaskTypeTest, UpdateNonexistent) {
    auto tx = createTransaction();
    proto::TaskType taskType;
    taskType.set_id(TEST_ID);
    taskType.mutable_config();
    dao::TaskType::update(taskType, *tx);
    EXPECT_FALSE(dao::TaskType::get(TEST_ID, *tx).has_value());
}


} // maps::sprav::callcenter::tests
