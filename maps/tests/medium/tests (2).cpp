#include <maps/wikimap/mapspro/libs/social/include/yandex/maps/wiki/social/feedback/gateway_rw.h>
#include <maps/wikimap/mapspro/libs/assessment/include/gateway.h>
#include <maps/wikimap/mapspro/libs/unittest/include/yandex/maps/wiki/unittest/query_helpers_arcadia.h>
#include <maps/wikimap/mapspro/libs/unittest/include/yandex/maps/wiki/unittest/unittest.h>

#include "helpers.h"

#include <library/cpp/testing/unittest/registar.h>

namespace feedback = maps::wiki::social::feedback;
using maps::wiki::unittest::txnNow;


#define CHECK_GRADE(grade_, gradedBy_, gradedAt_, value_, comment_, qualification_)\
    do {\
        UNIT_ASSERT_EQUAL(grade_.gradedBy, gradedBy_);\
        UNIT_ASSERT_EQUAL(grade_.gradedAt, gradedAt_);\
        UNIT_ASSERT_EQUAL(grade_.value, value_);\
        UNIT_ASSERT_EQUAL(grade_.comment, std::optional<std::string>(comment_));\
        UNIT_ASSERT_EQUAL(grade_.qualification, qualification_);\
    } while (false)

#define NO_COMMENT std::optional<std::string>()
#define NO_VALUE std::optional<Grade::Value>()

#define CHECK_ENTITY(entity_, id_, domain_)\
    do {\
        UNIT_ASSERT_EQUAL(entity_.id, id_);\
        UNIT_ASSERT_EQUAL(entity_.domain, domain_);\
    } while(false)

#define CHECK_ACTION(action_, by_, at_, name_)\
    do {\
        UNIT_ASSERT_EQUAL(action_.by, by_);\
        UNIT_ASSERT_EQUAL(action_.at, at_);\
        UNIT_ASSERT_EQUAL(action_.name, name_);\
    } while(false)

#define CHECK_UNIT(unit_, entity_, action_)\
    do {\
        CHECK_ENTITY(unit_.entity, entity_.id, entity_.domain);\
        CHECK_ACTION(unit_.action, action_.by, action_.at, action_.name);\
    } while(false)

#define CHECK_GRADED_UNIT(gradedUnit_, entity_, action_, gradesCount_)\
    do {\
        CHECK_UNIT(gradedUnit_, entity_, action_);\
        UNIT_ASSERT_EQUAL(gradedUnit_.grades.size(), gradesCount_);\
    } while (false)

#define CHECK_UNIT_EXISTS_AND_HAS_ID(optionalUnit_, unitId_)\
    do {\
        UNIT_ASSERT(optionalUnit_);\
        UNIT_ASSERT_EQUAL(optionalUnit_->id, unitId_);\
    } while (false)

#define CHECK_UNITS_EXIST_AND_EQUAL(optionalUnit1_, optionalUnit2_)\
    do {\
        UNIT_ASSERT(optionalUnit1_);\
        UNIT_ASSERT(optionalUnit2_);\
        UNIT_ASSERT_EQUAL(optionalUnit1_->id, optionalUnit2_->id);\
    } while(false);

#define CHECK_UNITS_EXIST_AND_UNEQUAL(optionalUnit1_, optionalUnit2_)\
    do {\
        UNIT_ASSERT(optionalUnit1_);\
        UNIT_ASSERT(optionalUnit2_);\
        UNIT_ASSERT_UNEQUAL(optionalUnit1_->id, optionalUnit2_->id);\
    } while(false);

namespace maps::wiki::assessment::tests {

const size_t ONE_TASK_PER_UNIT = 1;

Y_UNIT_TEST_SUITE_F(assessment_gateway_tests, DbFixture) {
    Y_UNIT_TEST(should_find_existing_units_and_create_new_units) {
        pqxx::work txn(conn);
        assessment::Gateway gateway(txn);

        const Entity entity = {"10", Entity::Domain::Feedback};
        constexpr TUid resolverUid = 2;
        const std::vector<chrono::TimePoint> timePoints = {
            chrono::parseSqlDateTime("2014-06-16 15:25:21.662649+03"),
            chrono::parseSqlDateTime("2017-12-03 12:32:10.662649+03")};

        const std::vector<TId> unitIds = {
            gateway.getOrCreateUnit(entity, {"accept", resolverUid, timePoints[0]}),
            gateway.getOrCreateUnit(entity, {"accept", resolverUid, timePoints[1]}),
            gateway.getOrCreateUnit(entity, {"accept", resolverUid, timePoints[0]})};

        UNIT_ASSERT_UNEQUAL(unitIds[0], unitIds[1]);
        UNIT_ASSERT_EQUAL(unitIds[0], unitIds[2]);
    }

    Y_UNIT_TEST(feedback_unit_check_requires_existing_and_eligible_task_operation) {
        pqxx::work txn(conn);
        assessment::Gateway gateway(txn);
        feedback::GatewayRW feedbackGateway(txn);

        TId taskId = 1;
        constexpr TUid resolverUid = 100;
        constexpr TUid assessorUid = 200;
        const auto now = txnNow(txn);

        UNIT_ASSERT_EXCEPTION_CONTAINS(
            gateway.checkEntityAction(
                {std::to_string(taskId), Entity::Domain::Feedback},
                {"accept", resolverUid, now}),
            RuntimeError,
            "No feedback task with id = 1");

        UNIT_ASSERT_EXCEPTION_CONTAINS(
            gateway.checkEntityAction(
                {std::to_string(taskId), Entity::Domain::Feedback},
                {"acquire", resolverUid, now}),
            RuntimeError,
            "Unsupported action 'acquire' for domain 'feedback'");

        taskId = feedbackGateway.addTask(
            assessorUid,
            {
                geolib3::Point2{.0, .0},
                feedback::Type::Road,
                "src",
                feedback::Description()
            }).id();

        feedbackGateway.updateTaskById(
            taskId,
            feedback::TaskPatch(resolverUid).setBucket(feedback::Bucket::Outgoing));

        UNIT_ASSERT_EXCEPTION_CONTAINS(
            gateway.checkEntityAction(
                {std::to_string(taskId), Entity::Domain::Feedback},
                {"accept", resolverUid, now}),
            RuntimeError,
            "No TaskOperation accept by "
            "uid = " + std::to_string(resolverUid) + " "
            "at " + chrono::formatSqlDateTime(now) + " "
            "for feedback task with id = " + std::to_string(taskId));

        feedbackGateway.updateTaskById(
            taskId,
            feedback::TaskPatch(resolverUid).setResolution(feedback::Resolution::createAccepted()));

        gateway.checkEntityAction(
            {std::to_string(taskId), Entity::Domain::Feedback},
            {"accept", resolverUid, now});

        feedbackGateway.updateTaskById(
            taskId,
            feedback::TaskPatch(resolverUid).setResolution({}));

        gateway.checkEntityAction(
            {std::to_string(taskId), Entity::Domain::Feedback},
            {"accept", resolverUid, now});
    }

    Y_UNIT_TEST(should_get_or_create_competence_by_role_name)
    {
        pqxx::work txn(conn);
        assessment::Gateway gateway(txn);

        const std::vector<TId> competenceIds {
            gateway.getOrCreateCompetence("role-1"),
            gateway.getOrCreateCompetence("role-2")};

        UNIT_ASSERT_EQUAL(competenceIds[0], gateway.getOrCreateCompetence("role-1"));
        UNIT_ASSERT_EQUAL(competenceIds[1], gateway.getOrCreateCompetence("role-2"));
    }

    Y_UNIT_TEST(should_load_competences_by_role_names)
    {
        pqxx::work txn(conn);
        assessment::Gateway gateway(txn);

        const std::vector<TId> competenceIds {
            gateway.getOrCreateCompetence("role-1"),
            gateway.getOrCreateCompetence("role-2")};

        const auto competences = gateway.loadCompetences({"role-1", "role-2"});
        UNIT_ASSERT_EQUAL(competences.size(), 2);
        UNIT_ASSERT_UNEQUAL(competences[0].id, competences[1].id);
        UNIT_ASSERT(competences[0].id == competenceIds[0] || competences[0].id == competenceIds[1]);
        UNIT_ASSERT(competences[1].id == competenceIds[0] || competences[1].id == competenceIds[1]);

        UNIT_ASSERT(gateway.loadCompetences({"role-3"}).empty());
    }

    Y_UNIT_TEST(should_check_if_sample_exists_by_name) {
        pqxx::work txn(conn);
        assessment::Gateway gateway(txn);

        const TId unitId = 1;
        for (const auto& name : {"sample", "duplicated-sample", "duplicated-sample"}) {
            gateway.createSample(
                Entity::Domain::Moderation,
                Qualification::Basic,
                name,
                {unitId},
                ONE_TASK_PER_UNIT);
        }

        UNIT_ASSERT(gateway.sampleExistsByName("sample"));
        UNIT_ASSERT(gateway.sampleExistsByName("duplicated-sample"));
        UNIT_ASSERT(!gateway.sampleExistsByName("non-existent-sample"));
    }

    Y_UNIT_TEST(should_count_sample_task_grades) {
        constexpr TId moderationTaskId = 10;
        constexpr TUid resolverUid = 101;
        const std::vector<TUid> assessorUids = {201, 202};

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        std::vector<Console> consoles = {
            gateway.console(assessorUids[0]),
            gateway.console(assessorUids[1])};

        const auto unitId = gateway.getOrCreateUnit(
            {std::to_string(moderationTaskId), Entity::Domain::Moderation},
            {"resolve", resolverUid, now});

        const auto sampleId = gateway.createSample(
            Entity::Domain::Moderation,
            Qualification::Basic,
            "sample",
            {unitId},
            3 * ONE_TASK_PER_UNIT);

        auto acquiredUnit = consoles[0].acquireUnit(sampleId, SkipAcquired::No);
        UNIT_ASSERT_EQUAL(acquiredUnit->id, unitId);
        consoles[0].gradeUnit(acquiredUnit->id, Grade::Value::Correct, NO_COMMENT, Qualification::Basic);

        acquiredUnit = consoles[1].acquireUnit(sampleId, SkipAcquired::No);
        UNIT_ASSERT_EQUAL(acquiredUnit->id, unitId);
        consoles[1].gradeUnit(acquiredUnit->id, Grade::Value::Incorrect, NO_COMMENT, Qualification::Expert);

        const auto stats = gateway.sampleGradeStats(sampleId);
        UNIT_ASSERT_EQUAL(stats.size(), 1);
        UNIT_ASSERT_EQUAL(stats[0].unitId, unitId);
        UNIT_ASSERT_EQUAL(stats[0].correct, 1);
        UNIT_ASSERT_EQUAL(stats[0].incorrect, 1);
    }

    Y_UNIT_TEST(units_with_no_grades_should_be_in_sample_grade_stats) {
        constexpr TId moderationTaskId = 10;
        constexpr TUid resolverUid = 101;

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        const auto unitId = gateway.getOrCreateUnit(
            {std::to_string(moderationTaskId), Entity::Domain::Moderation},
            {"resolve", resolverUid, now});

        const auto sampleId = gateway.createSample(
            Entity::Domain::Moderation,
            Qualification::Basic,
            "sample",
            {unitId},
            ONE_TASK_PER_UNIT);

        const auto stats = gateway.sampleGradeStats(sampleId);
        UNIT_ASSERT_EQUAL(stats.size(), 1);
        UNIT_ASSERT_EQUAL(stats[0].unitId, unitId);
        UNIT_ASSERT_EQUAL(stats[0].correct, 0);
        UNIT_ASSERT_EQUAL(stats[0].incorrect, 0);
    }

    Y_UNIT_TEST(should_not_count_grades_outside_of_sample) {
        constexpr TId moderationTaskId = 10;
        constexpr TUid resolverUid = 101;
        constexpr TUid assessorUid = 201;

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        auto console = gateway.console(assessorUid);

        const auto unitId = gateway.getOrCreateUnit(
            {std::to_string(moderationTaskId), Entity::Domain::Moderation},
            {"resolve", resolverUid, now});

        const auto sampleId = gateway.createSample(
            Entity::Domain::Moderation,
            Qualification::Basic,
            "sample",
            {unitId},
            ONE_TASK_PER_UNIT);

        console.gradeUnit(unitId, Grade::Value::Correct, NO_COMMENT, Qualification::Basic);
        const auto stats = gateway.sampleGradeStats(sampleId);

        UNIT_ASSERT_EQUAL(stats.size(), 1);
        UNIT_ASSERT_EQUAL(stats[0].unitId, unitId);
        UNIT_ASSERT_EQUAL(stats[0].correct, 0);
        UNIT_ASSERT_EQUAL(stats[0].incorrect, 0);
    }

    Y_UNIT_TEST(should_delete_unit_skips_older_than)
    {
        pqxx::work txn(conn);
        const auto now = txnNow(txn);
        assessment::Gateway gateway(txn);

        txn.exec(
            "INSERT INTO assessment.unit_skip "
                "(unit_id, skipped_by, skipped_at) "
            "VALUES "
                "(1, 201, NOW() - '1 second'::interval), "
                "(1, 202, NOW()), "
                "(1, 200, NOW() + '1 second'::interval)");

        const auto deletedSkips = gateway.deleteOldUnitSkips(now);
        UNIT_ASSERT_EQUAL(deletedSkips, 1);

        UNIT_ASSERT_QUERIES_RESULTS_EQUAL(
            txn,
            "SELECT unit_id, skipped_by, skipped_at FROM assessment.unit_skip ORDER BY skipped_at",
            "VALUES "
                "(1, 202, NOW()), "
                "(1, 200, NOW() + '1 second'::interval)");
    }
}


Y_UNIT_TEST_SUITE_F(assessment_console_tests, DbFixture) {
    Y_UNIT_TEST(should_add_and_get_grades) {
        pqxx::work txn(conn);
        assessment::Gateway gateway(txn);

        const std::vector<TUid> assessorUids = {1, 2, 3};
        std::vector<assessment::Console> consoles = {
            gateway.console(assessorUids[0]),
            gateway.console(assessorUids[1]),
            gateway.console(assessorUids[2])};

        constexpr TId feedbackTaskId = 11;
        constexpr TId moderationTaskId = 12;
        constexpr TId nonExistentEntityId = 13;

        const Entity feedbackEntity   = {std::to_string(feedbackTaskId),   Entity::Domain::Feedback};
        const Entity moderationEntity = {std::to_string(moderationTaskId), Entity::Domain::Moderation};

        constexpr TUid resolverUid = 21;
        const auto now = txnNow(txn);
        const std::vector<chrono::TimePoint> timePoints = {
            chrono::parseSqlDateTime("2015-06-16 15:25:21.662649+03"),
            chrono::parseSqlDateTime("2017-12-03 12:32:10.662649+03")};
        const std::vector<Action> actions = {
            {"accept", resolverUid, timePoints[0]},
            {"accept", resolverUid, timePoints[1]}};

        const std::vector<TId> unitIds = {
            gateway.getOrCreateUnit(feedbackEntity, actions[0]),
            gateway.getOrCreateUnit(feedbackEntity, actions[1])};
        gateway.getOrCreateUnit(moderationEntity, actions[1]);

        const GradeVec addedGrades = {
            consoles[0].gradeUnit(unitIds[0], Grade::Value::Correct, "comment 10", Qualification::Basic),
            consoles[1].gradeUnit(unitIds[0], Grade::Value::Correct, "comment 11", Qualification::Expert),
            consoles[0].gradeUnit(unitIds[1], Grade::Value::Incorrect, "comment 20", Qualification::Basic)};

        CHECK_GRADE(addedGrades[0], assessorUids[0], now, Grade::Value::Correct, "comment 10", Qualification::Basic);
        CHECK_GRADE(addedGrades[1], assessorUids[1], now, Grade::Value::Correct, "comment 11", Qualification::Expert);
        CHECK_GRADE(addedGrades[2], assessorUids[0], now, Grade::Value::Incorrect, "comment 20", Qualification::Basic);

        const auto feed = consoles[2].unitFeed(
            UnitFeedParams(0, 0, 2),
            UnitFilter()
                .entityIds(StringSet({feedbackEntity.id, std::to_string(nonExistentEntityId)}))
                .entityDomain(feedbackEntity.domain),
            GradesVisibility::All);

        UNIT_ASSERT_EQUAL(feed.units().size(), 2);
        UNIT_ASSERT(!feed.hasMore());

        CHECK_GRADED_UNIT(feed.units()[0], feedbackEntity, actions[1], 1);
        CHECK_GRADE(feed.units()[0].grades[0], assessorUids[0], now, Grade::Value::Incorrect, "comment 20", Qualification::Basic);

        CHECK_GRADED_UNIT(feed.units()[1], feedbackEntity, actions[0], 2);
        CHECK_GRADE(feed.units()[1].grades[0], assessorUids[1], now, Grade::Value::Correct, "comment 11", Qualification::Expert);
        CHECK_GRADE(feed.units()[1].grades[1], assessorUids[0], now, Grade::Value::Correct, "comment 10", Qualification::Basic);
    }

    Y_UNIT_TEST(grades_should_update_unit_stats) {
        pqxx::work txn(conn);
        assessment::Gateway gateway(txn);

        constexpr TUid assessorUid = 1;
        auto console = gateway.console(assessorUid);

        constexpr TId feedbackTaskId = 11;
        constexpr TUid resolverUid = 21;
        const auto actionAt = chrono::parseSqlDateTime("2015-06-16 15:25:21.662649+03");
        const auto unitId = gateway.getOrCreateUnit(
            {std::to_string(feedbackTaskId), Entity::Domain::Feedback},
            {"accept", resolverUid, actionAt});

        gateway.createSample(Entity::Domain::Feedback, Qualification::Basic, "sample", {unitId}, 3 * ONE_TASK_PER_UNIT);

        UNIT_ASSERT_QUERIES_RESULTS_EQUAL(
            txn,
            "SELECT last_expert_value FROM assessment.unit WHERE unit_id = " + std::to_string(unitId),
            "SELECT NULL");

        console.gradeUnit(unitId, Grade::Value::Correct, NO_COMMENT, Qualification::Expert);

        UNIT_ASSERT_QUERIES_RESULTS_EQUAL(
            txn,
            "SELECT last_expert_value FROM assessment.unit WHERE unit_id = " + std::to_string(unitId),
            "SELECT 'correct'");
    }

    Y_UNIT_TEST(unit_feed_should_return_units_with_respective_grades) {
        constexpr TId moderationTaskId = 10;
        const std::vector<TUid> assessorUids = {100, 101};
        constexpr TUid resolverUid = 200;

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        std::vector<Console> consoles = {
            gateway.console(assessorUids[0]),
            gateway.console(assessorUids[1])};

        const Entity entity = {
            std::to_string(moderationTaskId),
            Entity::Domain::Moderation};

        const std::vector<Action> actions = {
            {"resolve", resolverUid, now},
            {"close", resolverUid, now}};

        const std::vector<TId> unitIds = {
            gateway.getOrCreateUnit(entity, actions[0]),
            gateway.getOrCreateUnit(entity, actions[1])};

        consoles[0].gradeUnit(unitIds[0], Grade::Value::Correct, NO_COMMENT, Qualification::Basic);
        consoles[1].gradeUnit(unitIds[0], Grade::Value::Incorrect, NO_COMMENT, Qualification::Expert);
        consoles[0].gradeUnit(unitIds[1], Grade::Value::Correct, NO_COMMENT, Qualification::Basic);

        const auto feed = consoles[0].unitFeed(
            UnitFeedParams(0, 0, UnitFeedParams::perPageDefault),
            UnitFilter(),
            GradesVisibility::All);

        UNIT_ASSERT_EQUAL(feed.hasMore(), false);
        UNIT_ASSERT_EQUAL(feed.units().size(), 2);
        CHECK_GRADED_UNIT(feed.units()[0], entity, actions[1], 1);
        CHECK_GRADE(feed.units()[0].grades[0], assessorUids[0], now, Grade::Value::Correct, NO_COMMENT, Qualification::Basic);
        CHECK_GRADED_UNIT(feed.units()[1], entity, actions[0], 2);
        CHECK_GRADE(feed.units()[1].grades[0], assessorUids[1], now, Grade::Value::Incorrect, NO_COMMENT, Qualification::Expert);
        CHECK_GRADE(feed.units()[1].grades[1], assessorUids[0], now, Grade::Value::Correct, NO_COMMENT, Qualification::Basic);
    }

    Y_UNIT_TEST(unit_feed_should_not_return_units_without_visible_grades) {
        constexpr TId moderationTaskId = 10;
        const std::vector<TUid> assessorUids = {100, 101};
        constexpr TUid resolverUid = 200;

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        std::vector<Console> consoles = {
            gateway.console(assessorUids[0]),
            gateway.console(assessorUids[1])};

        const Entity entity = {
            std::to_string(moderationTaskId),
            Entity::Domain::Moderation};

        const std::vector<Action> actions = {
            {"resolve", resolverUid, now},
            {"close", resolverUid, now}};

        const std::vector<TId> unitIds = {
            gateway.getOrCreateUnit(entity, actions[0]),
            gateway.getOrCreateUnit(entity, actions[1])};

        consoles[0].gradeUnit(unitIds[0], Grade::Value::Correct, NO_COMMENT, Qualification::Basic);

        const auto feed = consoles[1].unitFeed(
            UnitFeedParams(0, 0, UnitFeedParams::perPageDefault),
            UnitFilter(),
            GradesVisibility::MyOrReceivedByMe);

        UNIT_ASSERT_EQUAL(feed.hasMore(), false);
        UNIT_ASSERT_EQUAL(feed.units().size(), 0);
    }

    Y_UNIT_TEST(unit_feed_should_show_my_grades_by_permisson)
    {
        constexpr TId moderationTaskId = 10;
        const std::vector<TUid> assessorUids {100, 101};
        const std::vector<TUid> resolverUids {200, 201};

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        std::vector<Console> consoles {
            gateway.console(assessorUids[0]),
            gateway.console(assessorUids[1])};

        const Entity entity {std::to_string(moderationTaskId), Entity::Domain::Moderation};

        const std::vector<TId> unitIds = {
            gateway.getOrCreateUnit(entity, {"accept", resolverUids[0], now}),
            gateway.getOrCreateUnit(entity, {"accept", resolverUids[1], now})};

        consoles[0].gradeUnit(unitIds[0], Grade::Value::Correct, NO_COMMENT, Qualification::Basic); // my
        consoles[1].gradeUnit(unitIds[1], Grade::Value::Correct, NO_COMMENT, Qualification::Basic);

        const auto feed = consoles[0].unitFeed(
            UnitFeedParams(0, 0, UnitFeedParams::perPageDefault),
            UnitFilter(),
            GradesVisibility::My);

        UNIT_ASSERT_EQUAL(feed.hasMore(), false);
        UNIT_ASSERT_EQUAL(feed.units().size(), 1);

        CHECK_GRADED_UNIT(feed.units()[0], entity, (Action{"accept", resolverUids[0], now}), 1);
        CHECK_GRADE(feed.units()[0].grades[0], assessorUids[0], now, Grade::Value::Correct, NO_COMMENT, Qualification::Basic);
    }

    Y_UNIT_TEST(unit_feed_should_show_my_or_received_by_me_grades_by_permission)
    {
        constexpr TId moderationTaskId = 10;
        const std::vector<TUid> assessorUids {100, 101};
        const std::vector<TUid> resolverUids {200, 201};

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        std::vector<Console> consoles {
            gateway.console(assessorUids[0]),
            gateway.console(assessorUids[1])};

        const Entity entity {std::to_string(moderationTaskId), Entity::Domain::Moderation};

        const std::vector<TId> unitIds = {
            gateway.getOrCreateUnit(entity, {"accept", resolverUids[0], now}),
            gateway.getOrCreateUnit(entity, {"accept", resolverUids[1], now}),
            gateway.getOrCreateUnit(entity, {"accept", assessorUids[0], now})};

        consoles[0].gradeUnit(unitIds[0], Grade::Value::Correct, NO_COMMENT, Qualification::Basic); // my
        consoles[1].gradeUnit(unitIds[1], Grade::Value::Correct, NO_COMMENT, Qualification::Basic);
        consoles[1].gradeUnit(unitIds[2], Grade::Value::Correct, NO_COMMENT, Qualification::Basic); // received by me

        const auto feed = consoles[0].unitFeed(
            UnitFeedParams(0, 0, UnitFeedParams::perPageDefault),
            UnitFilter(),
            GradesVisibility::MyOrReceivedByMe);

        UNIT_ASSERT_EQUAL(feed.hasMore(), false);
        UNIT_ASSERT_EQUAL(feed.units().size(), 2);

        CHECK_GRADED_UNIT(feed.units()[0], entity, (Action{"accept", assessorUids[0], now}), 1);
        CHECK_GRADE(feed.units()[0].grades[0], assessorUids[1], now, Grade::Value::Correct, NO_COMMENT, Qualification::Basic);

        CHECK_GRADED_UNIT(feed.units()[1], entity, (Action{"accept", resolverUids[0], now}), 1);
        CHECK_GRADE(feed.units()[1].grades[0], assessorUids[0], now, Grade::Value::Correct, NO_COMMENT, Qualification::Basic);
    }

    Y_UNIT_TEST(unit_feed_should_show_all_grades_by_permission)
    {
        constexpr TId moderationTaskId = 10;
        const std::vector<TUid> assessorUids {100, 101};
        const std::vector<TUid> resolverUids {200, 201};

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        std::vector<Console> consoles {
            gateway.console(assessorUids[0]),
            gateway.console(assessorUids[1])};

        const Entity entity {std::to_string(moderationTaskId), Entity::Domain::Moderation};

        const std::vector<TId> unitIds = {
            gateway.getOrCreateUnit(entity, {"accept", resolverUids[0], now}),
            gateway.getOrCreateUnit(entity, {"accept", resolverUids[1], now}),
            gateway.getOrCreateUnit(entity, {"accept", assessorUids[0], now})};

        consoles[0].gradeUnit(unitIds[0], Grade::Value::Correct, NO_COMMENT, Qualification::Basic); // my
        consoles[1].gradeUnit(unitIds[1], Grade::Value::Correct, NO_COMMENT, Qualification::Basic); // other
        consoles[1].gradeUnit(unitIds[2], Grade::Value::Correct, NO_COMMENT, Qualification::Basic); // receivedByMe

        const auto feed = consoles[0].unitFeed(
            UnitFeedParams(0, 0, UnitFeedParams::perPageDefault),
            UnitFilter(),
            GradesVisibility::All);

        UNIT_ASSERT_EQUAL(feed.hasMore(), false);
        UNIT_ASSERT_EQUAL(feed.units().size(), 3);
 
        CHECK_GRADED_UNIT(feed.units()[0], entity, (Action{"accept", assessorUids[0], now}), 1);
        CHECK_GRADE(feed.units()[0].grades[0], assessorUids[1], now, Grade::Value::Correct, NO_COMMENT, Qualification::Basic);

        CHECK_GRADED_UNIT(feed.units()[1], entity, (Action{"accept", resolverUids[1], now}), 1);
        CHECK_GRADE(feed.units()[1].grades[0], assessorUids[1], now, Grade::Value::Correct, NO_COMMENT, Qualification::Basic);

        CHECK_GRADED_UNIT(feed.units()[2], entity, (Action{"accept", resolverUids[0], now}), 1);
        CHECK_GRADE(feed.units()[2].grades[0], assessorUids[0], now, Grade::Value::Correct, NO_COMMENT, Qualification::Basic);
    }

    Y_UNIT_TEST(unit_feed_should_extend_grade_visibility_from_one_to_all_per_unit)
    {
        constexpr TId moderationTaskId = 10;
        const std::vector<TUid> assessorUids {100, 101, 102};
        constexpr TUid resolverUid = 200;

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        std::vector<Console> consoles {
            gateway.console(assessorUids[0]),
            gateway.console(assessorUids[1]),
            gateway.console(assessorUids[2])};

        const Entity entity {std::to_string(moderationTaskId), Entity::Domain::Moderation};
        const auto unitId = gateway.getOrCreateUnit(entity, {"accept", resolverUid, now});

        consoles[0].gradeUnit(unitId, Grade::Value::Correct, NO_COMMENT, Qualification::Basic); // my
        consoles[1].gradeUnit(unitId, Grade::Value::Correct, NO_COMMENT, Qualification::Basic);
        consoles[2].gradeUnit(unitId, Grade::Value::Correct, NO_COMMENT, Qualification::Basic);

        const auto feed = consoles[0].unitFeed(
            UnitFeedParams(0, 0, UnitFeedParams::perPageDefault),
            UnitFilter(),
            GradesVisibility::MyOrReceivedByMe);

        UNIT_ASSERT_EQUAL(feed.hasMore(), false);
        UNIT_ASSERT_EQUAL(feed.units().size(), 1);

        CHECK_GRADED_UNIT(feed.units()[0], entity, (Action{"accept", resolverUid, now}), 3);
        CHECK_GRADE(feed.units()[0].grades[0], assessorUids[2], now, Grade::Value::Correct, NO_COMMENT, Qualification::Basic);
        CHECK_GRADE(feed.units()[0].grades[1], assessorUids[1], now, Grade::Value::Correct, NO_COMMENT, Qualification::Basic);
        CHECK_GRADE(feed.units()[0].grades[2], assessorUids[0], now, Grade::Value::Correct, NO_COMMENT, Qualification::Basic);
    }

    Y_UNIT_TEST(before_param_should_navigate_unit_feed) {
        constexpr TId moderationTaskId = 10;
        constexpr TUid assessorUid = 100;
        constexpr TUid resolverUid = 200;

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        auto console = gateway.console(assessorUid);

        const Entity entity = {
            std::to_string(moderationTaskId),
            Entity::Domain::Moderation};

        const std::vector<Action> actions = {
            {"accept", resolverUid, now},
            {"reject", resolverUid, now}};

        const std::vector<TId> unitIds = {
            gateway.getOrCreateUnit(entity, actions[0]),
            gateway.getOrCreateUnit(entity, actions[1])};

        console.gradeUnit(unitIds[0], Grade::Value::Correct, NO_COMMENT, Qualification::Basic);
        console.gradeUnit(unitIds[1], Grade::Value::Incorrect, NO_COMMENT, Qualification::Basic);

        const auto feed = console.unitFeed(
            UnitFeedParams(unitIds[0], 0, UnitFeedParams::perPageDefault),
            UnitFilter(),
            GradesVisibility::All);

        UNIT_ASSERT_EQUAL(feed.hasMore(), false);
        UNIT_ASSERT_EQUAL(feed.units().size(), 1);
        CHECK_GRADED_UNIT(feed.units()[0], entity, actions[1], 1);
        CHECK_GRADE(feed.units()[0].grades[0], assessorUid, now, Grade::Value::Incorrect, NO_COMMENT, Qualification::Basic);
    }

    Y_UNIT_TEST(after_param_should_navigate_unit_feed) {
        constexpr TId moderationTaskId = 10;
        constexpr TUid assessorUid = 100;
        constexpr TUid resolverUid = 200;

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        auto console = gateway.console(assessorUid);

        const Entity entity = {
            std::to_string(moderationTaskId),
            Entity::Domain::Moderation};

        const std::vector<Action> actions = {
            {"accept", resolverUid, now},
            {"reject", resolverUid, now}};

        const std::vector<TId> unitIds = {
            gateway.getOrCreateUnit(entity, actions[0]),
            gateway.getOrCreateUnit(entity, actions[1])};

        console.gradeUnit(unitIds[0], Grade::Value::Correct, NO_COMMENT, Qualification::Basic);
        console.gradeUnit(unitIds[1], Grade::Value::Incorrect, NO_COMMENT, Qualification::Basic);

        const auto feed = console.unitFeed(
            UnitFeedParams(0, unitIds[1], UnitFeedParams::perPageDefault),
            UnitFilter(),
            GradesVisibility::All);

        UNIT_ASSERT_EQUAL(feed.hasMore(), false);
        UNIT_ASSERT_EQUAL(feed.units().size(), 1);
        CHECK_GRADED_UNIT(feed.units()[0], entity, actions[0], 1);
        CHECK_GRADE(feed.units()[0].grades[0], assessorUid, now, Grade::Value::Correct, NO_COMMENT, Qualification::Basic);
    }

    Y_UNIT_TEST(per_page_param_should_define_unit_feed_page_size) {
        constexpr TId moderationTaskId = 10;
        constexpr TUid assessorUid = 100;
        constexpr TUid resolverUid = 200;

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        auto console = gateway.console(assessorUid);

        const Entity entity = {std::to_string(moderationTaskId), Entity::Domain::Moderation};
        const std::vector<Action> actions = {
            {"accept", resolverUid, now},
            {"reject", resolverUid, now}};

        const std::vector<TId> unitIds = {
            gateway.getOrCreateUnit(entity, actions[0]),
            gateway.getOrCreateUnit(entity, actions[1])};

        console.gradeUnit(unitIds[0], Grade::Value::Correct, NO_COMMENT, Qualification::Basic);
        console.gradeUnit(unitIds[1], Grade::Value::Incorrect, NO_COMMENT, Qualification::Basic);

        const auto feed = console.unitFeed(
            UnitFeedParams(0, 0, 1),
            UnitFilter(),
            GradesVisibility::All);

        UNIT_ASSERT_EQUAL(feed.hasMore(), true);
        UNIT_ASSERT_EQUAL(feed.units().size(), 1);
        CHECK_GRADED_UNIT(feed.units()[0], entity, actions[1], 1);
        CHECK_GRADE(feed.units()[0].grades[0], assessorUid, now, Grade::Value::Incorrect, NO_COMMENT, Qualification::Basic);
    }

    Y_UNIT_TEST(should_return_currently_acquired_unit_if_not_skip)
    {
        constexpr TId feedbackTaskId = 20;
        constexpr TUid resolverUid = 100;
        constexpr TUid assessorUid = 200;

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        auto console = gateway.console(assessorUid);

        const TIds unitIds = {
            gateway.getOrCreateUnit(
                {std::to_string(feedbackTaskId), Entity::Domain::Feedback},
                {"need-info", resolverUid, now}),
            gateway.getOrCreateUnit(
                {std::to_string(feedbackTaskId), Entity::Domain::Feedback},
                {"accept", resolverUid, now})};

        const TId sampleId = gateway.createSample(
            Entity::Domain::Feedback,
            Qualification::Basic,
            "sample",
            unitIds,
            2 * ONE_TASK_PER_UNIT);

        const std::vector<std::optional<Unit>> acquiredUnits = {
            console.acquireUnit(sampleId, SkipAcquired::No),
            console.acquireUnit(sampleId, SkipAcquired::No)};
        CHECK_UNITS_EXIST_AND_EQUAL(acquiredUnits[0], acquiredUnits[1]);
    }

    Y_UNIT_TEST(should_not_acquire_units_acquired_by_others_if_no_overlap)
    {
        constexpr TId feedbackTaskId = 20;
        constexpr TUid resolverUid = 100;
        const std::vector<TUid> assessorUids = {200, 300};

        pqxx::work txn(conn);
        auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        std::vector<Console> consoles = {
            gateway.console(assessorUids[0]),
            gateway.console(assessorUids[1])};

        const TIds unitIds = {
            gateway.getOrCreateUnit(
                {std::to_string(feedbackTaskId), Entity::Domain::Feedback},
                {"need-info", resolverUid, now}),
            gateway.getOrCreateUnit(
                {std::to_string(feedbackTaskId), Entity::Domain::Feedback},
                {"reject", resolverUid, now})};

        const TId sampleId = gateway.createSample(
            Entity::Domain::Feedback,
            Qualification::Basic,
            "sample",
            unitIds,
            ONE_TASK_PER_UNIT);

        const std::vector<std::optional<Unit>> acquiredUnits = {
            consoles[0].acquireUnit(sampleId, SkipAcquired::No),
            consoles[1].acquireUnit(sampleId, SkipAcquired::No)};
        CHECK_UNITS_EXIST_AND_UNEQUAL(acquiredUnits[0], acquiredUnits[1]);
    }

    Y_UNIT_TEST(should_acquire_unit_acquired_by_others_if_overlap)
    {
        constexpr TId feedbackTaskId = 20;
        constexpr TUid resolverUid = 100;
        const std::vector<TUid> assessorUids = {200, 300};

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        std::vector<Console> consoles = {
            gateway.console(assessorUids[0]),
            gateway.console(assessorUids[1])};

        const TIds unitIds = {
            gateway.getOrCreateUnit(
                {std::to_string(feedbackTaskId), Entity::Domain::Feedback},
                {"need-info", resolverUid, now}),
            gateway.getOrCreateUnit(
                {std::to_string(feedbackTaskId), Entity::Domain::Feedback},
                {"reject", resolverUid, now})};

        const TId sampleId = gateway.createSample(
            Entity::Domain::Feedback,
            Qualification::Basic,
            "sample",
            unitIds,
            2 * ONE_TASK_PER_UNIT);

        const std::vector<std::optional<Unit>> acquiredUnits = {
            consoles[0].acquireUnit(sampleId, SkipAcquired::No),
            consoles[1].acquireUnit(sampleId, SkipAcquired::No)};
        CHECK_UNITS_EXIST_AND_EQUAL(acquiredUnits[0], acquiredUnits[1]);
    }

    Y_UNIT_TEST(should_skip_units)
    {
        constexpr TId feedbackTaskId = 20;
        constexpr TUid resolverUid = 100;
        constexpr TUid assessorUid = 200;

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        auto console = gateway.console(assessorUid);

        const TId unitId = gateway.getOrCreateUnit(
            {std::to_string(feedbackTaskId), Entity::Domain::Feedback},
            {"reject", resolverUid, now});

        const TId sampleId = gateway.createSample(
            Entity::Domain::Feedback,
            Qualification::Basic,
            "sample",
            {unitId},
            2 * ONE_TASK_PER_UNIT);

        auto acquiredUnit = console.acquireUnit(sampleId, SkipAcquired::No);
        CHECK_UNIT_EXISTS_AND_HAS_ID(acquiredUnit, unitId);

        acquiredUnit = console.acquireUnit(sampleId, SkipAcquired::Yes);
        UNIT_ASSERT(!acquiredUnit);
    }

    Y_UNIT_TEST(should_acquire_units_skipped_by_others)
    {
        constexpr TId feedbackTaskId = 20;
        constexpr TUid resolverUid = 100;
        const std::vector<TUid> assessorUids = {200, 300};

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        std::vector<Console> consoles = {
            gateway.console(assessorUids[0]),
            gateway.console(assessorUids[1])};

        const auto unitId = gateway.getOrCreateUnit(
            {std::to_string(feedbackTaskId), Entity::Domain::Feedback},
            {"need-info", resolverUid, now});

        const TId sampleId = gateway.createSample(
            Entity::Domain::Feedback,
            Qualification::Basic,
            "sample",
            {unitId},
            2 * ONE_TASK_PER_UNIT);

        auto acquiredUnit = consoles[0].acquireUnit(sampleId, SkipAcquired::No);
        CHECK_UNIT_EXISTS_AND_HAS_ID(acquiredUnit, unitId);

        acquiredUnit = consoles[0].acquireUnit(sampleId, SkipAcquired::Yes);
        UNIT_ASSERT(!acquiredUnit);

        acquiredUnit = consoles[1].acquireUnit(sampleId, SkipAcquired::No);
        CHECK_UNIT_EXISTS_AND_HAS_ID(acquiredUnit, unitId);
    }

    Y_UNIT_TEST(should_acquire_free_unit)
    {
        constexpr TId feedbackTaskId = 20;
        constexpr TUid resolverUid = 100;
        constexpr TUid assessorUid = 200;

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        auto console = gateway.console(assessorUid);

        const auto unitId = gateway.getOrCreateUnit(
            {std::to_string(feedbackTaskId), Entity::Domain::Feedback},
            {"need-info", resolverUid, now});

        const TId sampleId = gateway.createSample(
            Entity::Domain::Feedback,
            Qualification::Basic,
            "sample",
            {unitId},
            ONE_TASK_PER_UNIT);

        const auto acquiredUnit = console.acquireUnit(sampleId, SkipAcquired::No);
        CHECK_UNIT_EXISTS_AND_HAS_ID(acquiredUnit, unitId);
    }

    Y_UNIT_TEST(should_acquire_previously_released_unit)
    {
        constexpr TId feedbackTaskId = 20;
        constexpr TUid resolverUid = 100;
        constexpr TUid assessorUid = 200;

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        auto console = gateway.console(assessorUid);

        const auto unitId = gateway.getOrCreateUnit(
            {std::to_string(feedbackTaskId), Entity::Domain::Feedback},
            {"need-info", resolverUid, now});

        const TId sampleId = gateway.createSample(
            Entity::Domain::Feedback,
            Qualification::Basic,
            "sample",
            {unitId},
            ONE_TASK_PER_UNIT);

        auto acquiredUnit = console.acquireUnit(sampleId, SkipAcquired::No);
        CHECK_UNIT_EXISTS_AND_HAS_ID(acquiredUnit, unitId);
        console.releaseUnit(sampleId);

        acquiredUnit = console.acquireUnit(sampleId, SkipAcquired::No);
        CHECK_UNIT_EXISTS_AND_HAS_ID(acquiredUnit, unitId);
    }

    Y_UNIT_TEST(should_acquire_units_released_by_others)
    {
        constexpr TId feedbackTaskId = 20;
        constexpr TUid resolverUid = 100;
        const std::vector<TUid> assessorUids = {200, 300};

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        std::vector<Console> consoles = {
            gateway.console(assessorUids[0]),
            gateway.console(assessorUids[1])};

        const auto unitId = gateway.getOrCreateUnit(
            {std::to_string(feedbackTaskId), Entity::Domain::Feedback},
            {"need-info", resolverUid, now});

        const TId sampleId = gateway.createSample(
            Entity::Domain::Feedback,
            Qualification::Basic,
            "sample",
            {unitId},
            ONE_TASK_PER_UNIT);

        auto acquiredUnit = consoles[0].acquireUnit(sampleId, SkipAcquired::No);
        CHECK_UNIT_EXISTS_AND_HAS_ID(acquiredUnit, unitId);
        consoles[0].releaseUnit(sampleId);

        acquiredUnit = consoles[1].acquireUnit(sampleId, SkipAcquired::No);
        CHECK_UNIT_EXISTS_AND_HAS_ID(acquiredUnit, unitId);
    }

    Y_UNIT_TEST(release_should_not_raise_error_if_no_units_currently_acquired)
    {
        constexpr TId feedbackTaskId = 20;
        constexpr TUid resolverUid = 100;
        const std::vector<TUid> assessorUids = {200, 300};

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        std::vector<Console> consoles = {
            gateway.console(assessorUids[0]),
            gateway.console(assessorUids[1])};

        const auto unitId = gateway.getOrCreateUnit(
            {std::to_string(feedbackTaskId), Entity::Domain::Feedback},
            {"need-info", resolverUid, now});

        const TId sampleId = gateway.createSample(
            Entity::Domain::Feedback,
            Qualification::Basic,
            "sample",
            {unitId},
            ONE_TASK_PER_UNIT);

        consoles[1].releaseUnit(sampleId);
    }

    Y_UNIT_TEST(should_acquire_overdue_unit_from_overdue_sample_task)
    {
        constexpr TId feedbackTaskId = 20;
        constexpr TUid resolverUid = 100;
        const std::vector<TUid> assessorUids = {200, 300};

        pqxx::work txn(conn);
        const auto now = txnNow(txn);


        assessment::Gateway gateway(txn);
        std::vector<Console> consoles = {
            gateway.console(assessorUids[0]),
            gateway.console(assessorUids[1])};

        const auto unitId = gateway.getOrCreateUnit(
            {std::to_string(feedbackTaskId), Entity::Domain::Feedback},
            {"need-info", resolverUid, now});

        const TId sampleId = gateway.createSample(
            Entity::Domain::Feedback,
            Qualification::Basic,
            "sample",
            {unitId},
            ONE_TASK_PER_UNIT);

        auto acquiredUnit = consoles[0].acquireUnit(sampleId, SkipAcquired::No);
        CHECK_UNIT_EXISTS_AND_HAS_ID(acquiredUnit, unitId);

        txn.exec(
            "UPDATE assessment.sample_task "
            "SET acquired_at = NOW() - interval '1 hour 1 minute'");

        acquiredUnit = consoles[1].acquireUnit(sampleId, SkipAcquired::No);
        CHECK_UNIT_EXISTS_AND_HAS_ID(acquiredUnit, unitId);
    }

    Y_UNIT_TEST(arbitrary_grade_should_not_block_unit_from_acquisition)
    {
        constexpr TId feedbackTaskId = 20;
        constexpr TUid resolverUid = 100;
        constexpr TUid assessorUid = 200;

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        auto console = gateway.console(assessorUid);

        const auto unitId = gateway.getOrCreateUnit(
            {std::to_string(feedbackTaskId), Entity::Domain::Feedback},
            {"need-info", resolverUid, now});

        const TId sampleId = gateway.createSample(
            Entity::Domain::Feedback,
            Qualification::Basic,
            "sample",
            {unitId},
            ONE_TASK_PER_UNIT);

        const auto grade = console.gradeUnit(unitId, Grade::Value::Incorrect, NO_COMMENT, Qualification::Expert);
        UNIT_ASSERT_EQUAL(grade.qualification, Qualification::Expert);

        const auto acquiredUnit = console.acquireUnit(sampleId, SkipAcquired::No);
        CHECK_UNIT_EXISTS_AND_HAS_ID(acquiredUnit, unitId);
    }

    Y_UNIT_TEST(arbitrary_grade_should_not_complete_task)
    {
        constexpr TId feedbackTaskId = 20;
        constexpr TUid resolverUid = 100;
        const std::vector<TUid> assessorUids = {200, 300};

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        std::vector<Console> consoles = {
            gateway.console(assessorUids[0]),
            gateway.console(assessorUids[1])};

        const auto unitId = gateway.getOrCreateUnit(
            {std::to_string(feedbackTaskId), Entity::Domain::Feedback},
            {"need-info", resolverUid, now});

        const TId sampleId = gateway.createSample(
            Entity::Domain::Feedback,
            Qualification::Basic,
            "sample",
            {unitId},
            ONE_TASK_PER_UNIT);

        auto acquiredUnit = consoles[0].acquireUnit(sampleId, SkipAcquired::No);
        CHECK_UNIT_EXISTS_AND_HAS_ID(acquiredUnit, unitId);

        const auto grade = consoles[1].gradeUnit(unitId, Grade::Value::Correct, NO_COMMENT, Qualification::Basic);
        UNIT_ASSERT_EQUAL(grade.qualification, Qualification::Basic);

        acquiredUnit = consoles[0].acquireUnit(sampleId, SkipAcquired::No);
        CHECK_UNIT_EXISTS_AND_HAS_ID(acquiredUnit, unitId);
    }

    Y_UNIT_TEST(grade_should_complete_sample_task)
    {
        constexpr TId feedbackTaskId = 20;
        constexpr TUid resolverUid = 100;
        constexpr TUid assessorUid = 200;

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        auto console = gateway.console(assessorUid);

        const auto unitId = gateway.getOrCreateUnit(
            {std::to_string(feedbackTaskId), Entity::Domain::Feedback},
            {"need-info", resolverUid, now});

        const TId sampleId = gateway.createSample(
            Entity::Domain::Feedback,
            Qualification::Basic,
            "sample",
            {unitId},
            ONE_TASK_PER_UNIT);

        auto acquiredUnit = console.acquireUnit(sampleId, SkipAcquired::No);
        CHECK_UNIT_EXISTS_AND_HAS_ID(acquiredUnit, unitId);

        const auto grade = console.gradeUnit(unitId, Grade::Value::Incorrect, "comment", Qualification::Basic);
        UNIT_ASSERT_EQUAL(grade.qualification, Qualification::Basic);

        acquiredUnit = console.acquireUnit(sampleId, SkipAcquired::No);
        UNIT_ASSERT(!acquiredUnit);
    }

    Y_UNIT_TEST(grade_should_not_complete_overdue_sample_task)
    {
        // We could complete overdue tasks that haven't been reacquired by other users.
        // But it would make handling overlap (tasksPerUnit > 1) more complex.

        constexpr TId feedbackTaskId = 20;
        constexpr TUid resolverUid = 100;
        constexpr TUid assessorUid = 200;

        pqxx::work txn(conn);
        const auto now = txnNow(txn);

        assessment::Gateway gateway(txn);
        auto console = gateway.console(assessorUid);

        const auto unitId = gateway.getOrCreateUnit(
            {std::to_string(feedbackTaskId), Entity::Domain::Feedback},
            {"need-info", resolverUid, now});

        const TId sampleId = gateway.createSample(
            Entity::Domain::Feedback,
            Qualification::Basic,
            "sample",
            {unitId},
            ONE_TASK_PER_UNIT);

        auto acquiredUnit = console.acquireUnit(sampleId, SkipAcquired::No);
        CHECK_UNIT_EXISTS_AND_HAS_ID(acquiredUnit, unitId);

        txn.exec(
            "UPDATE assessment.sample_task "
            "SET acquired_at = NOW() - interval '1 hour 1 minute'");

        const auto grade = console.gradeUnit(unitId, Grade::Value::Incorrect, "comment", Qualification::Basic);
        UNIT_ASSERT_EQUAL(grade.qualification, Qualification::Basic);

        acquiredUnit = console.acquireUnit(sampleId, SkipAcquired::No);
        UNIT_ASSERT_EQUAL(acquiredUnit->id, unitId);
    }
}

} // namespace maps::wiki::assessment::tests
