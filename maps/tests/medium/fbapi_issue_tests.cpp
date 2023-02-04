#include <yandex/maps/wiki/social/feedback/description.h>
#include <yandex/maps/wiki/social/feedback/gateway_rw.h>
#include <yandex/maps/wiki/social/feedback/task.h>
#include <yandex/maps/wiki/social/fbapi_issue.h>

#include <library/cpp/testing/unittest/registar.h>
#include "helpers.h"

namespace maps::wiki::social::tests {

namespace {

const std::string SRC = "source";
const std::string DESCR = "description";

const std::string ISSUE_ID   = "7adadc6da15e42a0a7cbf3264418773e";
const std::string ISSUE_ID_1 = "e8d92d75f86942ccb63bdd1015b6d2bc";
const std::string ISSUE_ID_2 = "7adada15e42a0a7cbf3233453dgdfgs4h";

const TUid SOME_USER_ID = 1001;

} // namespace anonymous

Y_UNIT_TEST_SUITE(fbapi_issues) {

Y_UNIT_TEST_F(fbapi_issues_filter, DbFixture)
{
    pqxx::work txn(conn);
    feedback::GatewayRW gatewayRw(txn);

    addFbapiIssue(
        txn,
        ISSUE_ID,
        0,
        json::Value::fromString(R"({"id":")" + DESCR + R"(","arr": [0, 1, 2]})")
    );
    auto task1 = gatewayRw.addTask(
        SOME_USER_ID,
        feedback::TaskNew(
            geolib3::Point2(0., 0.),
            feedback::Type::Barrier,
            SRC,
            feedback::Description(DESCR)
        )
    );
    auto task2 = gatewayRw.addTask(
        SOME_USER_ID,
        feedback::TaskNew(
            geolib3::Point2(0., 0.),
            feedback::Type::Barrier,
            SRC,
            feedback::Description(DESCR)
        )
    );

    auto fbapiIssue1 = addFbapiIssue(
        txn,
        ISSUE_ID_1,
        task1.id(),
        json::Value::fromString("{}")
    );
    fbapiIssue1 = updateFbapiIssueHashValue(
        txn,
        fbapiIssue1.id(),
        task1.hashValue()
    );

    auto fbapiIssue2 = addFbapiIssue(
        txn,
        ISSUE_ID_2,
        task2.id(),
        json::Value::fromString("{}")
    );
    fbapiIssue2 = updateFbapiIssueHashValue(
        txn,
        fbapiIssue2.id(),
        task2.hashValue()
    );

    {
        auto issues = fbapiIssuesUnmatchedHash(txn);
        UNIT_ASSERT_EQUAL(issues.size(), 0);
    }

    fbapiIssue2 = updateFbapiIssueHashValue(
            txn,
            fbapiIssue2.id(),
            "smth different"
    );

    {
        auto issues = fbapiIssuesUnmatchedHash(txn);
        UNIT_ASSERT(issues.size() == 1);
        UNIT_ASSERT_EQUAL(issues[0].id(), fbapiIssue2.id());
    }

    {
        FbapiIssueFilter filter;
        filter.feedbackTaskId(task2.id());
        auto issues = fbapiIssuesByFilter(txn, filter);

        UNIT_ASSERT_EQUAL(issues.size(), 1);
        UNIT_ASSERT_EQUAL(issues[0].issueId(), ISSUE_ID_2);

        UNIT_ASSERT(issues[0].feedbackTaskId());
        UNIT_ASSERT_EQUAL(issues[0].feedbackTaskId(), task2.id());
    }
    {
        FbapiIssueFilter filter;
        filter.feedbackTaskId(task1.id());
        auto issues = fbapiIssuesByFilter(txn, filter);

        UNIT_ASSERT_EQUAL(issues.size(), 1);
        UNIT_ASSERT_EQUAL(issues[0].issueId(), ISSUE_ID_1);

        UNIT_ASSERT(issues[0].feedbackTaskId());
        UNIT_ASSERT_EQUAL(issues[0].feedbackTaskId(), task1.id());
    }
}

Y_UNIT_TEST_F(fbapi_issues_filter_by_issue_ids, DbFixture)
{
    constexpr TId FB_TASK_ID_1 = 1;
    constexpr TId FB_TASK_ID_2 = 2;

    pqxx::work txn(conn);

    addFbapiIssue(txn, ISSUE_ID_1, FB_TASK_ID_1, json::Value("hop"));
    addFbapiIssue(txn, ISSUE_ID_2, FB_TASK_ID_2, json::Value("hey"));

    {
        FbapiIssueFilter filter;
        auto issues = fbapiIssuesByFilter(txn, filter);
        UNIT_ASSERT_EQUAL(issues.size(), 2);
    }
    {
        FbapiIssueFilter filter;
        filter.issueIds({ISSUE_ID_1});

        auto issues = fbapiIssuesByFilter(txn, filter);
        UNIT_ASSERT_EQUAL(issues.size(), 1);
        UNIT_ASSERT_STRINGS_EQUAL(issues.front().issueId(), ISSUE_ID_1);
        UNIT_ASSERT_EQUAL(issues.front().feedbackTaskId(), FB_TASK_ID_1);
    }
    {
        FbapiIssueFilter filter;
        filter.issueIds({});
        auto issues = fbapiIssuesByFilter(txn, filter);
        UNIT_ASSERT(issues.empty());
    }
}

} // fbapi issues suite

} // namespace maps::wiki::social::tests
