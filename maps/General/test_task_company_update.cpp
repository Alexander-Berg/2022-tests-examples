#include <maps/sprav/callcenter/libs/dao/task_company_update.h>
#include <maps/sprav/callcenter/libs/dao/request.h>

#include <maps/sprav/callcenter/libs/dao/task.h>
#include <maps/sprav/callcenter/libs/test_helpers/util.h>

#include <maps/sprav/callcenter/libs/test_helpers/db_fixture.h>

#include <maps/sprav/callcenter/proto/request.pb.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gtest_protobuf/matcher.h>

namespace maps::sprav::callcenter::tests {

namespace {

auto createTransaction() {
    static DbFixture db;
    return db.pool().masterWriteableTransaction();
}

proto::TaskCompanyUpdate assembleTaskCompanyUpdate(
        int64_t taskId, uint64_t companyId, uint64_t requestId, proto::TaskCompanyUpdate::Status::Value status) {
    proto::TaskCompanyUpdate result;
    result.set_task_id(taskId);
    result.set_user_id(1);
    result.set_company_id(companyId);
    result.set_request_id(requestId);
    result.set_status(status);
    result.set_create_time(0);
    result.set_updated(0);
    return result;
}

task::TaskDataPtr createTaskFromRequestIds(const std::vector<uint64_t>& ids, pqxx::transaction_base& tx) {
    std::vector<proto::Request> requests;
    std::transform(ids.begin(), ids.end(), std::back_inserter(requests), [&tx](uint64_t id) {  // гавно
        proto::Request result;
        result.set_id(id);
        dao::Request::create(result, tx);
        return result;
    });
    return dao::TaskActive::createFromRequests(requests, "actualization", tx);
}

} // namespace

TEST(TaskCompanyUpdate, CreateAndGet) {
    auto tx = createTransaction();

    auto task = createTaskFromRequestIds(std::vector<uint64_t>{1, 2}, *tx);
    
    auto tcu1 = assembleTaskCompanyUpdate(1, 1, 0, proto::TaskCompanyUpdate::Status::ACT_FULL);
    auto tcu2 = assembleTaskCompanyUpdate(1, 2, 0, proto::TaskCompanyUpdate::Status::ACT_FULL);
    auto tcu3 = assembleTaskCompanyUpdate(1, 0, 1, proto::TaskCompanyUpdate::Status::ACT_FULL);
    auto tcu4 = assembleTaskCompanyUpdate(1, 0, 2, proto::TaskCompanyUpdate::Status::ACT_FULL);

    dao::TaskCompanyUpdate::create(tcu1, *tx);
    dao::TaskCompanyUpdate::create(tcu2, *tx);
    dao::TaskCompanyUpdate::create(tcu3, *tx);
    dao::TaskCompanyUpdate::create(tcu4, *tx);

    EXPECT_THAT(dao::TaskCompanyUpdate::getByTaskIdAndCompanyId(1, 1, *tx).value(), 
                NGTest::EqualsProto(tcu1));
    EXPECT_THAT(dao::TaskCompanyUpdate::getByTaskIdAndCompanyId(1, 2, *tx).value(), 
                NGTest::EqualsProto(tcu2));
    EXPECT_THAT(dao::TaskCompanyUpdate::getByTaskIdAndRequestId(1, 1, *tx).value(), 
                NGTest::EqualsProto(tcu3));
    EXPECT_THAT(dao::TaskCompanyUpdate::getByTaskIdAndRequestId(1, 2, *tx).value(), 
                NGTest::EqualsProto(tcu4));
}

TEST(TaskCompanyUpdate, GetNotFound) {
    auto tx = createTransaction();

    EXPECT_FALSE(dao::TaskCompanyUpdate::getByTaskIdAndCompanyId(1, 1, *tx).has_value());
    EXPECT_FALSE(dao::TaskCompanyUpdate::getByTaskIdAndRequestId(1, 1, *tx).has_value());

    auto task = createTaskFromRequestIds(std::vector<uint64_t>{1}, *tx);

    EXPECT_FALSE(dao::TaskCompanyUpdate::getByTaskIdAndCompanyId(1, 1, *tx).has_value());
    EXPECT_FALSE(dao::TaskCompanyUpdate::getByTaskIdAndRequestId(1, 1, *tx).has_value());
    
    auto tcu1 = assembleTaskCompanyUpdate(1, 1, 0, proto::TaskCompanyUpdate::Status::ACT_FULL);
    auto tcu2 = assembleTaskCompanyUpdate(1, 0, 1, proto::TaskCompanyUpdate::Status::ACT_FULL);
    dao::TaskCompanyUpdate::create(tcu1, *tx);
    dao::TaskCompanyUpdate::create(tcu2, *tx);

    EXPECT_FALSE(dao::TaskCompanyUpdate::getByTaskIdAndCompanyId(1, 2, *tx).has_value());
    EXPECT_FALSE(dao::TaskCompanyUpdate::getByTaskIdAndRequestId(1, 2, *tx).has_value());

    EXPECT_FALSE(dao::TaskCompanyUpdate::getByTaskIdAndCompanyId(2, 1, *tx).has_value());
    EXPECT_FALSE(dao::TaskCompanyUpdate::getByTaskIdAndRequestId(2, 1, *tx).has_value());
}

TEST(TaskCompanyUpdate, CreateAndUpdate) {
    auto tx = createTransaction();

    auto task = createTaskFromRequestIds(std::vector<uint64_t>{1}, *tx);
    
    auto tcu1 = assembleTaskCompanyUpdate(1, 1, 0, proto::TaskCompanyUpdate::Status::ACT_FULL);
    auto tcu2 = assembleTaskCompanyUpdate(1, 0, 1, proto::TaskCompanyUpdate::Status::ACT_FULL);
    dao::TaskCompanyUpdate::create(tcu1, *tx);
    dao::TaskCompanyUpdate::create(tcu2, *tx);

    tcu1.set_updated(1);
    dao::TaskCompanyUpdate::update(tcu1, *tx);
    EXPECT_THAT(dao::TaskCompanyUpdate::getByTaskIdAndCompanyId(1, 1, *tx).value(), 
                NGTest::EqualsProto(tcu1));

    tcu2.set_updated(1);
    dao::TaskCompanyUpdate::update(tcu2, *tx);
    EXPECT_THAT(dao::TaskCompanyUpdate::getByTaskIdAndRequestId(1, 1, *tx).value(), 
                NGTest::EqualsProto(tcu2));
}

} // maps::sprav::callcenter::tests
