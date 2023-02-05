#include <maps/sprav/callcenter/libs/dao/request.h>
#include <maps/libs/chrono/include/time_point.h>

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

proto::Request createRequest() {
    proto::Request request;
    request.set_id(1);
    request.mutable_actualization()->set_permalink(1);
    request.set_oid_space(NSprav::OriginalIdSpace::CC_OIS_NONE);
    request.set_original_id("10");
    request.set_priority(1);
    request.set_type_priority(1);
    request.set_receive_time(1640995200123);
    request.add_related_phones("8800");
    request.add_related_phones("8801");
    request.set_status(proto::Request::Status::NEW);
    request.set_workflow_status(proto::Request::WorkflowStatus::QUEUED);
    return request;
}

std::vector<uint64_t> toIds(const std::vector<proto::Request>& requests) {
    std::vector<std::uint64_t> ids;
    std::transform(
        requests.begin(), requests.end(), std::back_inserter(ids),
        [](const auto& request) { return request.id(); }
    );
    return ids;
}

} // namespace

TEST(RequestTest, CreateAndGet) {
    auto tx = createTransaction();
    auto request = createRequest();
    dao::Request::create(request, *tx);

    EXPECT_THAT(dao::Request::get(1, *tx).value(), NGTest::EqualsProto(request));
}

TEST(Request, NotFound) {
    auto tx = createTransaction();
    EXPECT_FALSE(dao::Request::get(1, *tx).has_value());
}

TEST(Request, CreateAndUpdate) {
    auto tx = createTransaction();
    auto request = createRequest();
    dao::Request::create(request, *tx);
    request.mutable_actualization()->set_comment("test");
    dao::Request::update(request, *tx);
    EXPECT_THAT(dao::Request::get(1, *tx).value(), NGTest::EqualsProto(request));
}

TEST(Request, MultipleCreateConflict) {
    auto tx = createTransaction();
    auto request = createRequest();
    dao::Request::create(request, *tx);
    EXPECT_THROW(dao::Request::create(request, *tx), pqxx::unique_violation);
}

TEST(Request, SetStatus) {
    auto tx = createTransaction();

    EXPECT_FALSE(dao::Request::setStatus(1, proto::Request::Status::IN_PROGRESS, *tx));

    dao::Request::create(createRequest(), *tx);

    auto requestBefore = dao::Request::get(1, *tx);
    EXPECT_EQ(requestBefore->status(), proto::Request::Status::NEW);

    EXPECT_TRUE(dao::Request::setStatus(1, proto::Request::Status::IN_PROGRESS, *tx));
    auto requestAfter = dao::Request::get(1, *tx);
    EXPECT_EQ(requestAfter->status(), proto::Request::Status::IN_PROGRESS);
    requestAfter->set_status(proto::Request::Status::NEW);
    EXPECT_THAT(*requestAfter, NGTest::EqualsProto(*requestBefore));

    EXPECT_TRUE(dao::Request::setStatus(1, proto::Request::Status::NEW, *tx));
    EXPECT_THAT(*dao::Request::get(1, *tx), NGTest::EqualsProto(*requestBefore));

    EXPECT_THROW(dao::Request::setStatus(1, proto::Request::Status::Value(-12345), *tx), maps::LogicError);
}

TEST(Request, DuplicateOriginalIdNoneSpace) {
    auto tx = createTransaction();
    auto request = createRequest();
    dao::Request::create(request, *tx);
    request.set_id(2);
    EXPECT_NO_THROW(dao::Request::create(request, *tx));
}

TEST(Request, DuplicateOriginalIdConflict) {
    auto tx = createTransaction();
    auto request = createRequest();
    request.set_oid_space(NSprav::OriginalIdSpace::CC_OIS_FEEDBACK);
    dao::Request::create(request, *tx);
    request.set_id(2);
    EXPECT_THROW(dao::Request::create(request, *tx), pqxx::unique_violation);
}

TEST(Request, UpdateInNonNoneOidSpace) {
    auto tx = createTransaction();
    auto request = createRequest();
    request.set_oid_space(NSprav::OriginalIdSpace::CC_OIS_FEEDBACK);
    dao::Request::create(request, *tx);
    request.mutable_actualization()->set_comment("test");
    dao::Request::update(request, *tx);
    request.set_workflow_status(proto::Request::WorkflowStatus::COMPLETED);
    dao::Request::update(request, *tx);
    EXPECT_THAT(dao::Request::get(1, *tx).value(), NGTest::EqualsProto(request));
}

TEST(Request, getByStatus) {
    auto tx = createTransaction();
    auto request = createRequest();
    request.clear_id();

    std::vector<uint64_t> expectedInProgress;
    std::vector<uint64_t> expectedQueued;
    std::vector<uint64_t> expectedCompleted;

    request.set_workflow_status(proto::Request::WorkflowStatus::IN_PROGRESS);
    for ([[maybe_unused]] auto _ : xrange(3)) {
        expectedInProgress.push_back(dao::Request::create(request, *tx));
    }

    request.set_workflow_status(proto::Request::WorkflowStatus::QUEUED);
    for ([[maybe_unused]] auto _ : xrange(5)) {
        expectedQueued.push_back(dao::Request::create(request, *tx));
    }

    for ([[maybe_unused]] auto _ : xrange(5)) {
        request.clear_id();
        request.set_workflow_status(proto::Request::WorkflowStatus::QUEUED);
        expectedCompleted.push_back(dao::Request::create(request, *tx));
        request.set_id(expectedCompleted.back());
        request.set_workflow_status(proto::Request::WorkflowStatus::COMPLETED);
        dao::Request::update(request, *tx);
    }

    EXPECT_THAT(
        expectedQueued,
        ::testing::UnorderedElementsAreArray(
            toIds(dao::Request::getByWorkflowStatus(proto::Request::WorkflowStatus::QUEUED, 10, *tx))
        )
    );

    EXPECT_THAT(
        expectedInProgress,
        ::testing::UnorderedElementsAreArray(
            toIds(dao::Request::getByWorkflowStatus(proto::Request::WorkflowStatus::IN_PROGRESS, 10, *tx))
        )
    );

    EXPECT_THAT(
        expectedCompleted,
        ::testing::UnorderedElementsAreArray(
            toIds(dao::Request::getByWorkflowStatus(proto::Request::WorkflowStatus::COMPLETED, 10, *tx))
        )
    );

    auto smallBatchCompleted = toIds(
        dao::Request::getByWorkflowStatus(proto::Request::WorkflowStatus::COMPLETED, 2, *tx)
    );

    EXPECT_EQ(smallBatchCompleted.size(), 2u);
    EXPECT_THAT(
        expectedCompleted,
        ::testing::IsSupersetOf(smallBatchCompleted)
    );

}

TEST(Request, ScheduleRequestsCancellation) {
    auto now = maps::chrono::sinceEpoch<std::chrono::milliseconds>( maps::chrono::TimePoint::clock::now());
    auto tx = createTransaction();
    tx->exec(
        "INSERT INTO tasker.request "
            "(id, workflow_status) "
        "VALUES "
            "('1', 'in_progress'), "
            "('2', 'queued'), "
            "('3', 'completed'), "
            "('4', 'cancelled'), "
            "('5', 'in_progress'), "
            "('6', 'cancelled')"
    );

    const auto result = dao::Request::scheduleRequestsCancellation({1, 2, 3, 4, 7}, 1, *tx);
    EXPECT_THAT(result.size(), 2);
    EXPECT_THAT(result, ::testing::UnorderedElementsAreArray({1u, 2u}));

    const auto& rows = tx->exec(
        "SELECT id, workflow_status, cancel_user_id, cancel_ts "
        "FROM tasker.request WHERE id in ('1', '2') ORDER BY id"
    );
    EXPECT_THAT(rows.size(), 2);
    const auto& row1 = rows[0];
    EXPECT_THAT(row1["id"].as<uint64_t>(), 1);
    EXPECT_THAT(row1["workflow_status"].as<std::string>(), "cancel_queued");
    EXPECT_THAT(row1["cancel_user_id"].as<std::string>(), "1");
    EXPECT_TRUE(row1["cancel_ts"].as<uint64_t>() >= now);

    const auto& row2 = rows[1];
    EXPECT_THAT(row2["id"].as<uint64_t>(), 2);
    EXPECT_THAT(row2["workflow_status"].as<std::string>(), "cancel_queued");
    EXPECT_THAT(row2["cancel_user_id"].as<std::string>(), "1");
    EXPECT_TRUE(row2["cancel_ts"].as<uint64_t>() >= now);
}

TEST(Request, GetIdsByOriginal) {

    auto tx = createTransaction();
    tx->exec(
        "INSERT INTO tasker.request "
            "(id, oid_space, original_id) "
        "VALUES "
            "('1', '1', 'id1'), "
            "('2', '1', 'id2'), "
            "('3', '2', 'id2'), "
            "('4', '1', 'id2')"
    );
    {
        const std::set<uint64_t> byOriginal = dao::Request::getIdsByOriginal(
            NSprav::OriginalIdSpace::CC_OIS_FEEDBACK, "id2", *tx
        );
        EXPECT_THAT(byOriginal, (std::set<uint64_t>{2, 4}));
    }
    {
        const std::set<uint64_t> byOriginal = dao::Request::getIdsByOriginal(
            NSprav::OriginalIdSpace::CC_OIS_MODERATION, "id2", *tx
        );
        EXPECT_THAT(byOriginal, (std::set<uint64_t>{3}));
    }
    {
        const std::set<uint64_t> byOriginal = dao::Request::getIdsByOriginal(
            NSprav::OriginalIdSpace::CC_OIS_MODERATION, "id3", *tx
        );
        EXPECT_THAT(byOriginal, (std::set<uint64_t>{}));
    }
}

} // maps::sprav::callcenter::tests
