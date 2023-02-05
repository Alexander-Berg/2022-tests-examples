#include <maps/sprav/callcenter/libs/dao/feedback_complete_request.h>

#include <maps/sprav/callcenter/libs/test_helpers/db_fixture.h>
#include <maps/sprav/callcenter/libs/test_helpers/util.h>

#include <maps/sprav/callcenter/proto/feedback_complete_request.pb.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gtest_protobuf/matcher.h>


namespace maps::sprav::callcenter::dao::tests {

namespace {

auto newTx() {
    static callcenter::tests::DbFixture db;
    return db.pool().masterWriteableTransaction();
}

} // namespace

TEST(FeedbackCompleteRequest, ReadAndWrite) {
    auto request = test_helpers::protoFromTextFormat<proto::FeedbackCompleteRequest>(R"(
        feedback_id: 1234,
        payload: {
            cancelled: {
                user_id: 4321,
                cancel_time: 1000
            }
        }
    )");

    auto tx = newTx();

    auto emptyBatch = dao::FeedbackCompleteRequest::load(100, *tx);
    ASSERT_EQ(emptyBatch.size(), 0u);

    dao::FeedbackCompleteRequest::create(request, *tx);
    auto batchOfOne = dao::FeedbackCompleteRequest::load(100, *tx);
    ASSERT_EQ(batchOfOne.size(), 1u);
    ASSERT_THAT(batchOfOne[0], NGTest::EqualsProto(request));

    dao::FeedbackCompleteRequest::clear({}, *tx);
    auto notAClear = dao::FeedbackCompleteRequest::load(100, *tx);
    ASSERT_EQ(notAClear.size(), 1u);

    dao::FeedbackCompleteRequest::clear({request.feedback_id()}, *tx);
    auto batchOfNone = dao::FeedbackCompleteRequest::load(100, *tx);
    ASSERT_EQ(batchOfNone.size(), 0u);
}

} // namespace maps::sprav::callcenter::tests
