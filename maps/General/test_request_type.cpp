#include <maps/sprav/callcenter/libs/dao/request_type.h>
#include <library/cpp/protobuf/json/json2proto.h>

#include <maps/sprav/callcenter/libs/test_helpers/db_fixture.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gtest_protobuf/matcher.h>


namespace maps::sprav::callcenter::tests {

namespace {
    auto createTransaction() {
        static DbFixture db;
        return db.pool().masterWriteableTransaction();
    }

    const TString TEST_ID = "test_request_type";
} // namespace

TEST(RequestTypeTest, CreateAndGet) {
    auto tx = createTransaction();
    proto::RequestType requestType;
    requestType.set_internal_name(TEST_ID);
    requestType.set_priority(0);
    requestType.mutable_config()->set_generate_signal(true);
    dao::RequestType::create(requestType, *tx);

    EXPECT_THAT(dao::RequestType::get(TEST_ID, *tx).value(), NGTest::EqualsProto(requestType));
}

TEST(RequestTypeTest, NotFound) {
    auto tx = createTransaction();
    EXPECT_FALSE(dao::RequestType::get(TEST_ID, *tx).has_value());
}

TEST(RequestTypeTest, CreateAndUpdate) {
    auto tx = createTransaction();
    proto::RequestType requestType;
    requestType.set_internal_name(TEST_ID);
    requestType.set_priority(0);
    requestType.mutable_config();
    dao::RequestType::create(requestType, *tx);
    requestType = dao::RequestType::get(TEST_ID, *tx).value();
    requestType.set_disregard_work_time(true);
    dao::RequestType::update(requestType, *tx);
    EXPECT_THAT(dao::RequestType::get(TEST_ID, *tx).value(), NGTest::EqualsProto(requestType));
}

TEST(RequestTypeTest, MultipleCreateConflict) {
    auto tx = createTransaction();
    proto::RequestType requestType;
    requestType.set_internal_name(TEST_ID);
    requestType.set_priority(0);
    requestType.mutable_config();
    dao::RequestType::create(requestType, *tx);
    EXPECT_THROW(dao::RequestType::create(requestType, *tx), pqxx::unique_violation);
}

TEST(RequestTypeTest, UpdateNonexistent) {
    auto tx = createTransaction();
    proto::RequestType requestType;
    requestType.set_internal_name(TEST_ID);
    requestType.set_priority(0);
    requestType.mutable_config();
    dao::RequestType::update(requestType, *tx);
    EXPECT_FALSE(dao::RequestType::get(TEST_ID, *tx).has_value());
}

} // maps::sprav::callcenter::tests
