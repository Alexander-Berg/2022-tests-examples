#include <maps/wikimap/mapspro/libs/social/include/yandex/maps/wiki/social/feedback/gateway_rw.h>
#include <maps/wikimap/mapspro/libs/assessment/include/gateway.h>
#include <maps/wikimap/mapspro/libs/assessment/tests/helpers/unit_creator.h>
#include <maps/wikimap/mapspro/libs/unittest/include/yandex/maps/wiki/unittest/query_helpers_arcadia.h>
#include <maps/wikimap/mapspro/libs/unittest/include/yandex/maps/wiki/unittest/unittest.h>
#include <fmt/format.h>

#include "helpers.h"

#include <library/cpp/testing/unittest/registar.h>

namespace feedback = maps::wiki::social::feedback;
using namespace fmt::literals;

#define NO_COMMENT std::nullopt

namespace maps::wiki::assessment::tests {

namespace {

void setLastGradedAt(pqxx::transaction_base& txn, TId unitId, const std::string& lastGradedAt)
{
    const auto result = txn.exec(fmt::format(
        "UPDATE assessment.unit "
        "SET last_graded_at = {last_graded_at} "
        "WHERE unit_id = {unit_id}",

        "last_graded_at"_a = lastGradedAt,
        "unit_id"_a = unitId));

    UNIT_ASSERT_EQUAL(result.affected_rows(), 1);
}

void setGradedAt(pqxx::transaction_base& txn, TId gradeId, const std::string& gradedAt)
{
    const auto result = txn.exec(fmt::format(
        "UPDATE assessment.grade "
        "SET graded_at = {graded_at} "
        "WHERE grade_id = {grade_id}",

        "graded_at"_a = gradedAt,
        "grade_id"_a = gradeId));

    UNIT_ASSERT_EQUAL(result.affected_rows(), 1);
}

GradedUnit expectUnit(Console& console, TId unitId, const UnitFilter& filter)
{
    const auto unitFeed = console.unitFeed(
        UnitFeedParams(0, 0, UnitFeedParams::perPageDefault),
        filter,
        GradesVisibility::All);

    UNIT_ASSERT_EQUAL(unitFeed.units().size(), 1);
    UNIT_ASSERT_EQUAL(unitFeed.units()[0].id, unitId);
    return unitFeed.units()[0];
}

} // namespace

Y_UNIT_TEST_SUITE_F(assessment_unit_status_tests, DbFixture) {
    Y_UNIT_TEST(unit_with_no_grades_should_not_be_fixable)
    {
        constexpr TUid resolverUid = 101;
        pqxx::work txn(conn);

        const auto unitId = createUnit(txn, *UnitCreator().actionBy(resolverUid));
        UNIT_ASSERT(!Gateway(txn).console(resolverUid).tryFixUnit(unitId));
    }

    Y_UNIT_TEST(unit_should_be_fixable_after_single_basic_incorrect_with_no_expert_grades_if_no_longer_expecting_grades_from_basic_sample) {
        constexpr TUid resolverUid = 101;

        pqxx::work txn(conn);
        auto resolverConsole = createConsoles(txn, {resolverUid})[0];
        auto assessorConsole = createConsoles(txn, {201})[0];

        const auto unitId = createUnit(txn, *UnitCreator().actionBy(resolverUid));
        assessorConsole.gradeUnit(unitId, Grade::Value::Incorrect, NO_COMMENT, Qualification::Basic);
        setLastGradedAt(txn, unitId, "NOW() - '3h'::interval - '1s'::interval");

        auto gradedUnit = expectUnit(resolverConsole, unitId, UnitFilter().status(UnitStatus::Incorrect));
        UNIT_ASSERT(gradedUnit.permissions[UnitPermission::Fixable]);
        UNIT_ASSERT(resolverConsole.tryFixUnit(unitId));

        gradedUnit = expectUnit(resolverConsole, unitId, UnitFilter().status(UnitStatus::Fixed));
        UNIT_ASSERT(!gradedUnit.permissions[UnitPermission::Fixable]);
    }

    Y_UNIT_TEST(unit_should_not_be_fixable_after_single_basic_incorrect_with_no_expert_grades_if_still_expecting_grades_from_basic_sample) {
        constexpr TUid resolverUid = 101;

        pqxx::work txn(conn);
        auto resolverConsole = createConsoles(txn, {resolverUid})[0];
        auto assessorConsole = createConsoles(txn, {201})[0];

        const auto unitId = createUnit(txn, *UnitCreator().actionBy(resolverUid));
        assessorConsole.gradeUnit(unitId, Grade::Value::Incorrect, NO_COMMENT, Qualification::Basic);
        setLastGradedAt(txn, unitId, "NOW() - '3h'::interval + '1s'::interval");

        const auto gradedUnit = expectUnit(resolverConsole, unitId, UnitFilter());
        UNIT_ASSERT(!gradedUnit.permissions[UnitPermission::Fixable]);
    }

    Y_UNIT_TEST(unit_should_not_be_fixable_after_single_basic_correct_with_no_expert_grades) {
        constexpr TUid resolverUid = 101;

        pqxx::work txn(conn);
        auto resolverConsole = createConsoles(txn, {resolverUid})[0];
        auto assessorConsole = createConsoles(txn, {201})[0];

        const auto unitId = createUnit(txn, *UnitCreator().actionBy(resolverUid));
        assessorConsole.gradeUnit(unitId, Grade::Value::Correct, NO_COMMENT, Qualification::Basic);
        setLastGradedAt(txn, unitId, "NOW() - '3h'::interval - '1s'::interval");

        const auto gradedUnit = expectUnit(resolverConsole, unitId, UnitFilter().status(UnitStatus::Correct));
        UNIT_ASSERT(!gradedUnit.permissions[UnitPermission::Fixable]);
    }

    Y_UNIT_TEST(unit_should_not_be_fixable_after_prevailing_corrects_among_basic_grades_with_no_expert_grades) {
        constexpr TUid resolverUid = 101;

        pqxx::work txn(conn);
        auto resolverConsole = createConsoles(txn, {resolverUid})[0];
        auto assessorConsoles = createConsoles(txn, {201, 202, 203});

        const auto unitId = createUnit(txn, *UnitCreator().actionBy(resolverUid));
        assessorConsoles[0].gradeUnit(unitId, Grade::Value::Correct,   NO_COMMENT, Qualification::Basic);
        assessorConsoles[1].gradeUnit(unitId, Grade::Value::Incorrect, NO_COMMENT, Qualification::Basic);
        assessorConsoles[2].gradeUnit(unitId, Grade::Value::Correct,   NO_COMMENT, Qualification::Basic);

        const auto gradedUnit = expectUnit(resolverConsole, unitId, UnitFilter().status(UnitStatus::Correct));
        UNIT_ASSERT(!gradedUnit.permissions[UnitPermission::Fixable]);
    }

    Y_UNIT_TEST(unit_should_be_fixable_after_expert_incorrect_regardless_of_basic_corrects)
    {
        constexpr TUid resolverUid = 101;

        pqxx::work txn(conn);
        auto resolverConsole = createConsoles(txn, {resolverUid})[0];
        auto assessorConsoles = createConsoles(txn, {201, 202, 203});

        const auto unitId = createUnit(txn, *UnitCreator().actionBy(resolverUid));
        assessorConsoles[0].gradeUnit(unitId, Grade::Value::Incorrect, NO_COMMENT, Qualification::Expert);

        auto gradedUnit = expectUnit(resolverConsole, unitId, UnitFilter().status(UnitStatus::Incorrect));
        UNIT_ASSERT(gradedUnit.permissions[UnitPermission::Fixable]);

        assessorConsoles[1].gradeUnit(unitId, Grade::Value::Correct, NO_COMMENT, Qualification::Basic);
        assessorConsoles[2].gradeUnit(unitId, Grade::Value::Correct, NO_COMMENT, Qualification::Basic);

        gradedUnit = expectUnit(resolverConsole, unitId, UnitFilter().status(UnitStatus::Incorrect));
        UNIT_ASSERT(gradedUnit.permissions[UnitPermission::Fixable]);
        UNIT_ASSERT(resolverConsole.tryFixUnit(unitId));

        gradedUnit = expectUnit(resolverConsole, unitId, UnitFilter().status(UnitStatus::Fixed));
        UNIT_ASSERT(!gradedUnit.permissions[UnitPermission::Fixable]);
    }

    Y_UNIT_TEST(unit_should_not_be_fixable_after_expert_correct_regardless_of_basic_incorrects)
    {
        constexpr TUid resolverUid = 101;

        pqxx::work txn(conn);
        auto resolverConsole = createConsoles(txn, {resolverUid})[0];
        auto assessorConsoles = createConsoles(txn, {201, 202, 203});

        const auto unitId = createUnit(txn, *UnitCreator().actionBy(resolverUid));
        assessorConsoles[0].gradeUnit(unitId, Grade::Value::Correct, NO_COMMENT, Qualification::Expert);

        auto gradedUnit = expectUnit(resolverConsole, unitId, UnitFilter().status(UnitStatus::Correct));
        UNIT_ASSERT(!gradedUnit.permissions[UnitPermission::Fixable]);

        assessorConsoles[1].gradeUnit(unitId, Grade::Value::Incorrect, NO_COMMENT, Qualification::Basic);
        assessorConsoles[2].gradeUnit(unitId, Grade::Value::Incorrect, NO_COMMENT, Qualification::Basic);

        gradedUnit = expectUnit(resolverConsole, unitId, UnitFilter().status(UnitStatus::Correct));
        UNIT_ASSERT(!gradedUnit.permissions[UnitPermission::Fixable]);
    }

    Y_UNIT_TEST(unit_should_be_fixable_after_last_expert_incorrect_despite_prior_expert_correct)
    {
        constexpr TUid resolverUid = 101;

        pqxx::work txn(conn);
        auto resolverConsole = createConsoles(txn, {resolverUid})[0];
        auto assessorConsoles = createConsoles(txn, {201, 202});

        const auto unitId = createUnit(txn, *UnitCreator().actionBy(resolverUid));
        assessorConsoles[0].gradeUnit(unitId, Grade::Value::Correct,   NO_COMMENT, Qualification::Expert);
        assessorConsoles[1].gradeUnit(unitId, Grade::Value::Incorrect, NO_COMMENT, Qualification::Expert);

        auto gradedUnit = expectUnit(resolverConsole, unitId, UnitFilter().status(UnitStatus::Incorrect));
        UNIT_ASSERT(gradedUnit.permissions[UnitPermission::Fixable]);
        UNIT_ASSERT(resolverConsole.tryFixUnit(unitId));

        gradedUnit = expectUnit(resolverConsole, unitId, UnitFilter().status(UnitStatus::Fixed));
        UNIT_ASSERT(!gradedUnit.permissions[UnitPermission::Fixable]);
    }

    Y_UNIT_TEST(unit_should_not_be_fixable_after_last_expert_correct_despite_prior_expert_incorrect)
    {
        constexpr TUid resolverUid = 101;

        pqxx::work txn(conn);
        auto resolverConsole = createConsoles(txn, {resolverUid})[0];
        auto assessorConsoles = createConsoles(txn, {201, 202});

        const auto unitId = createUnit(txn, *UnitCreator().actionBy(resolverUid));
        assessorConsoles[0].gradeUnit(unitId, Grade::Value::Incorrect, NO_COMMENT, Qualification::Expert);
        assessorConsoles[1].gradeUnit(unitId, Grade::Value::Correct,   NO_COMMENT, Qualification::Expert);

        auto gradedUnit = expectUnit(resolverConsole, unitId, UnitFilter().status(UnitStatus::Correct));
        UNIT_ASSERT(!gradedUnit.permissions[UnitPermission::Fixable]);
    }

    Y_UNIT_TEST(unit_should_not_be_fixable_if_already_fixed)
    {
        constexpr TUid resolverUid = 101;

        pqxx::work txn(conn);
        auto resolverConsole = createConsoles(txn, {resolverUid})[0];
        auto assessorConsoles = createConsoles(txn, {201, 202, 203});

        const auto unitId = createUnit(txn, *UnitCreator().actionBy(resolverUid));
        assessorConsoles[0].gradeUnit(unitId, Grade::Value::Incorrect, NO_COMMENT, Qualification::Expert);

        auto gradedUnit = expectUnit(resolverConsole, unitId, UnitFilter().status(UnitStatus::Incorrect));
        UNIT_ASSERT(gradedUnit.permissions[UnitPermission::Fixable]);
        UNIT_ASSERT(resolverConsole.tryFixUnit(unitId));

        assessorConsoles[1].gradeUnit(unitId, Grade::Value::Correct,   NO_COMMENT, Qualification::Expert);
        assessorConsoles[2].gradeUnit(unitId, Grade::Value::Incorrect, NO_COMMENT, Qualification::Expert);

        gradedUnit = expectUnit(resolverConsole, unitId, UnitFilter().status(UnitStatus::Fixed));
        UNIT_ASSERT(!gradedUnit.permissions[UnitPermission::Fixable]);
        UNIT_ASSERT(!resolverConsole.tryFixUnit(unitId));
    }

    Y_UNIT_TEST(unit_should_not_be_fixable_by_other_users)
    {
        const std::vector<TUid> resolverUids {101, 102};

        pqxx::work txn(conn);
        auto resolverConsoles = createConsoles(txn, resolverUids);
        auto assessorConsole = createConsoles(txn, {201})[0];

        const auto unitId = createUnit(txn, *UnitCreator().actionBy(resolverUids[0]));
        assessorConsole.gradeUnit(unitId, Grade::Value::Incorrect, NO_COMMENT, Qualification::Expert);

        const auto gradedUnit = expectUnit(resolverConsoles[1], unitId, UnitFilter().status(UnitStatus::Incorrect));
        UNIT_ASSERT(!gradedUnit.permissions[UnitPermission::Fixable]);
        UNIT_ASSERT(!resolverConsoles[1].tryFixUnit(unitId));
    }
}

Y_UNIT_TEST_SUITE_F(assessment_grade_status_tests, DbFixture) {
    Y_UNIT_TEST(grade_refutation_should_not_be_possible_without_expert_grade)
    {
        pqxx::work txn(conn);
        auto assessorConsoles = createConsoles(txn, {201, 202});

        const auto unitId = createUnit(txn, *UnitCreator());
        assessorConsoles[0].gradeUnit(unitId, Grade::Value::Incorrect, NO_COMMENT, Qualification::Basic);

        const auto gradedUnit = expectUnit(assessorConsoles[0], unitId, UnitFilter());
        UNIT_ASSERT(!gradedUnit.permissions[UnitPermission::RefutationAcceptable]);
        UNIT_ASSERT(!assessorConsoles[0].tryAcceptRefutation(unitId));
    }

    Y_UNIT_TEST(unit_with_refuted_grade_should_be_acceptable)
    {
        pqxx::work txn(conn);
        auto assessorConsoles = createConsoles(txn, {201, 202});

        const auto unitId = createUnit(txn, *UnitCreator());
        assessorConsoles[0].gradeUnit(unitId, Grade::Value::Incorrect, NO_COMMENT, Qualification::Basic);
        assessorConsoles[1].gradeUnit(unitId, Grade::Value::Correct, NO_COMMENT, Qualification::Expert);

        auto gradedUnit = expectUnit(assessorConsoles[0], unitId, UnitFilter().gradeStatus(GradeStatus::Refuted));
        UNIT_ASSERT(gradedUnit.permissions[UnitPermission::RefutationAcceptable]);
        UNIT_ASSERT(assessorConsoles[0].tryAcceptRefutation(unitId));
    }

    Y_UNIT_TEST(unit_with_confirmed_grade_should_not_be_acceptable)
    {
        pqxx::work txn(conn);
        auto assessorConsoles = createConsoles(txn, {201, 202});

        const auto unitId = createUnit(txn, *UnitCreator());
        assessorConsoles[0].gradeUnit(unitId, Grade::Value::Correct, NO_COMMENT, Qualification::Basic);
        assessorConsoles[1].gradeUnit(unitId, Grade::Value::Correct, NO_COMMENT, Qualification::Expert);

        const auto gradedUnit = expectUnit(assessorConsoles[0], unitId, UnitFilter().gradeStatus(GradeStatus::Confirmed));
        UNIT_ASSERT(!gradedUnit.permissions[UnitPermission::RefutationAcceptable]);
        UNIT_ASSERT(!assessorConsoles[0].tryAcceptRefutation(unitId));
    }

    Y_UNIT_TEST(unit_with_refuted_other_user_grade_should_not_be_acceptabe)
    {
        pqxx::work txn(conn);
        auto assessorConsoles = createConsoles(txn, {201, 202, 203});

        const auto unitId = createUnit(txn, *UnitCreator());
        assessorConsoles[0].gradeUnit(unitId, Grade::Value::Incorrect, NO_COMMENT, Qualification::Basic);
        assessorConsoles[1].gradeUnit(unitId, Grade::Value::Correct, NO_COMMENT, Qualification::Expert);

        auto gradedUnit = expectUnit(assessorConsoles[1], unitId, UnitFilter().gradeStatus(GradeStatus::Refuted));
        UNIT_ASSERT(!gradedUnit.permissions[UnitPermission::RefutationAcceptable]);
        UNIT_ASSERT(!assessorConsoles[1].tryAcceptRefutation(unitId));

        gradedUnit = expectUnit(assessorConsoles[2], unitId, UnitFilter().gradeStatus(GradeStatus::Refuted));
        UNIT_ASSERT(!gradedUnit.permissions[UnitPermission::RefutationAcceptable]);
        UNIT_ASSERT(!assessorConsoles[2].tryAcceptRefutation(unitId));
    }

    Y_UNIT_TEST(refutation_of_same_grade_should_change_grade_status_and_not_be_acceptable_multiple_times) {
        pqxx::work txn(conn);
        auto assessorConsoles = createConsoles(txn, {201, 202, 203, 204});

        const auto unitId = createUnit(txn, *UnitCreator());
        assessorConsoles[0].gradeUnit(unitId, Grade::Value::Incorrect, NO_COMMENT, Qualification::Basic);
        assessorConsoles[1].gradeUnit(unitId, Grade::Value::Correct, NO_COMMENT, Qualification::Expert);

        auto gradedUnit = expectUnit(assessorConsoles[0], unitId, UnitFilter().gradeStatus(GradeStatus::Refuted));
        UNIT_ASSERT(gradedUnit.permissions[UnitPermission::RefutationAcceptable]);
        UNIT_ASSERT(assessorConsoles[0].tryAcceptRefutation(unitId));

        assessorConsoles[2].gradeUnit(unitId, Grade::Value::Incorrect, NO_COMMENT, Qualification::Expert);
        assessorConsoles[3].gradeUnit(unitId, Grade::Value::Correct, NO_COMMENT, Qualification::Expert);

        gradedUnit = expectUnit(assessorConsoles[0], unitId, UnitFilter().gradeStatus(GradeStatus::RefutationAccepted));
        UNIT_ASSERT(!gradedUnit.permissions[UnitPermission::RefutationAcceptable]);
        UNIT_ASSERT(!assessorConsoles[0].tryAcceptRefutation(unitId));
    }

    Y_UNIT_TEST(refutation_of_new_grade_should_be_acceptable_even_if_refutations_of_prior_grades_are_already_accepted) {
        pqxx::work txn(conn);
        auto assessorConsoles = createConsoles(txn, {201, 202});

        // grade & refute
        const auto unitId = createUnit(txn, *UnitCreator());
        const auto gradeId = assessorConsoles[0].gradeUnit(unitId, Grade::Value::Incorrect, NO_COMMENT, Qualification::Basic).id;
        assessorConsoles[1].gradeUnit(unitId, Grade::Value::Correct, NO_COMMENT, Qualification::Expert);
        setGradedAt(txn, gradeId, "NOW() - '1s'::interval");

        // accept refutation
        auto gradedUnit = expectUnit(assessorConsoles[0], unitId, UnitFilter().gradeStatus(GradeStatus::Refuted));
        UNIT_ASSERT(gradedUnit.permissions[UnitPermission::RefutationAcceptable]);
        UNIT_ASSERT(assessorConsoles[0].tryAcceptRefutation(unitId));

        gradedUnit = expectUnit(assessorConsoles[0], unitId, UnitFilter().gradeStatus(GradeStatus::RefutationAccepted));
        UNIT_ASSERT(!gradedUnit.permissions[UnitPermission::RefutationAcceptable]);
        UNIT_ASSERT(!assessorConsoles[0].tryAcceptRefutation(unitId));

        // new grade => refuted, refutation-acceptable
        assessorConsoles[0].gradeUnit(unitId, Grade::Value::Incorrect, NO_COMMENT, Qualification::Basic);

        gradedUnit = expectUnit(assessorConsoles[0], unitId, UnitFilter().gradeStatus(GradeStatus::Refuted));
        UNIT_ASSERT(gradedUnit.permissions[UnitPermission::RefutationAcceptable]);
        UNIT_ASSERT(assessorConsoles[0].tryAcceptRefutation(unitId));
    }

    Y_UNIT_TEST(prior_refutations_by_same_user_should_no_block_grade_from_being_confirmed)
    {
        pqxx::work txn(conn);
        auto assessorConsoles = createConsoles(txn, {201, 202});
        const auto unitId = createUnit(txn, *UnitCreator());

        // grade & refute
        const auto gradeId = assessorConsoles[1].gradeUnit(unitId, Grade::Value::Correct, NO_COMMENT, Qualification::Expert).id;
        setGradedAt(txn, gradeId, "NOW() - '1s'::interval");

        assessorConsoles[0].gradeUnit(unitId, Grade::Value::Incorrect, NO_COMMENT, Qualification::Basic);

        // accept refutation
        auto gradedUnit = expectUnit(assessorConsoles[0], unitId, UnitFilter().gradeStatus(GradeStatus::Refuted));
        UNIT_ASSERT(gradedUnit.permissions[UnitPermission::RefutationAcceptable]);
        UNIT_ASSERT(assessorConsoles[0].tryAcceptRefutation(unitId));

        // confirm grade
        assessorConsoles[1].gradeUnit(unitId, Grade::Value::Incorrect, NO_COMMENT, Qualification::Expert);

        gradedUnit = expectUnit(assessorConsoles[0], unitId, UnitFilter().gradeStatus(GradeStatus::Confirmed));
        UNIT_ASSERT(!gradedUnit.permissions[UnitPermission::RefutationAcceptable]);
        UNIT_ASSERT(!assessorConsoles[0].tryAcceptRefutation(unitId));
    }

    Y_UNIT_TEST(should_accept_all_refuted_grades_by_the_user)
    {
        pqxx::work txn(conn);
        auto assessorConsoles = createConsoles(txn, {201, 202});
        const auto unitId = createUnit(txn, *UnitCreator());

        // same grades by same assessor
        const auto gradeId = assessorConsoles[0].gradeUnit(unitId, Grade::Value::Correct, NO_COMMENT, Qualification::Basic).id;
        setGradedAt(txn, gradeId, "NOW() - '1s'::interval");
        assessorConsoles[0].gradeUnit(unitId, Grade::Value::Correct, NO_COMMENT, Qualification::Basic);

        // refute
        assessorConsoles[1].gradeUnit(unitId, Grade::Value::Incorrect, NO_COMMENT, Qualification::Expert);

        const auto gradedUnit = expectUnit(assessorConsoles[0], unitId, UnitFilter().gradeStatus(GradeStatus::Refuted));
        UNIT_ASSERT(gradedUnit.permissions[UnitPermission::RefutationAcceptable]);
        UNIT_ASSERT(assessorConsoles[0].tryAcceptRefutation(unitId));
    }
}

} // namespace maps::wiki::assessment::tests
