# coding: utf-8


import mock
from itertools import chain

from review.shortcuts import const
from review.lib import datetimes
from review.core.logic import bulk


FIELDS_BEFORE = {
    const.FIELDS.MARK: 'bad',
    const.FIELDS.GOLDSTAR: const.GOLDSTAR.BONUS_ONLY,
    const.FIELDS.LEVEL_CHANGE: 0,
    const.FIELDS.BONUS: 0,
    const.FIELDS.BONUS_ABSOLUTE: 0,
    const.FIELDS.SALARY_CHANGE: 0,
    const.FIELDS.SALARY_CHANGE_ABSOLUTE: 0,
    const.FIELDS.OPTIONS_RSU: 0,
    const.FIELDS.BONUS_RSU: 0,
}


FIELDS_AFTER = {
    const.FIELDS.MARK: 'extraordinary',
    const.FIELDS.GOLDSTAR: const.GOLDSTAR.OPTION_AND_BONUS,
    const.FIELDS.LEVEL_CHANGE: 1,
    const.FIELDS.BONUS: 50,
    const.FIELDS.BONUS_ABSOLUTE: 50,
    const.FIELDS.SALARY_CHANGE: 70,
    const.FIELDS.SALARY_CHANGE_ABSOLUTE: 70,
    const.FIELDS.OPTIONS_RSU: 200,
    const.FIELDS.BONUS_RSU: 400,
}

BONUS_FIELDS = {
    const.FIELDS.BONUS,
    const.FIELDS.BONUS_ABSOLUTE,
}

SALARY_CHANGE_FIELDS = {
    const.FIELDS.SALARY_CHANGE,
    const.FIELDS.SALARY_CHANGE_ABSOLUTE,
}

PERSON_LEVEL = 10

ALL_EDIT_FIELDS = frozenset(FIELDS_BEFORE)
INPUT_FIELDS = const.FIELDS.INPUT_FIELDS
GOODIE_FIELDS = const.FIELDS.GOODIE_FIELDS


TESTED_ROLES = [
    const.ROLE.PERSON_REVIEW.REVIEWER,
    const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
    const.ROLE.REVIEW.SUPERREVIEWER,
]

TESTED_EDIT_MODES = ['actions', 'import']
YES_NO = [True, False]


def do_update(edit_mode, role, case, update_fields):
    subject = {
        const.ROLE.PERSON_REVIEW.REVIEWER: case.reviewer,
        const.ROLE.PERSON_REVIEW.TOP_REVIEWER: case.top_reviewer,
        const.ROLE.REVIEW.SUPERREVIEWER: case.superreviewer,
    }[role]
    params = {
        field: FIELDS_AFTER[field]
        for field in update_fields
    }
    if edit_mode == 'actions':
        with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
            return bulk.bulk_same_action_set(
                subject=subject,
                ids=[case.person_review.id],
                params=params,
            )
    else:
        return bulk.bulk_different_action_set(
            subject=subject,
            data={
                case.person_review.id: params,
            }
        )


def assert_only_affected(case, updated_fields):
    person_review = case.person_review
    person_review.refresh_from_db()
    is_updating_bonus = False
    bonus_type = None
    is_updating_salary = False
    salary_change_type = None
    for field in updated_fields:
        if field == const.FIELDS.BONUS:
            is_updating_bonus = True
            bonus_type = const.SALARY_DEPENDENCY_TYPE.PERCENTAGE
        elif field == const.FIELDS.BONUS_ABSOLUTE:
            is_updating_bonus = True
            bonus_type = bonus_type or const.SALARY_DEPENDENCY_TYPE.ABSOLUTE
        elif field == const.FIELDS.SALARY_CHANGE:
            is_updating_salary = True
            salary_change_type = const.SALARY_DEPENDENCY_TYPE.PERCENTAGE
        elif field == const.FIELDS.SALARY_CHANGE_ABSOLUTE:
            is_updating_salary = True
            salary_change_type = salary_change_type or const.SALARY_DEPENDENCY_TYPE.ABSOLUTE

    for field in ALL_EDIT_FIELDS:
        field_value = getattr(person_review, field)
        have_to_be_changed = (
            (field in updated_fields) or
            (is_updating_bonus and field in BONUS_FIELDS) or
            (is_updating_salary and field in SALARY_CHANGE_FIELDS)
        )
        if have_to_be_changed:
            assert field_value == FIELDS_AFTER[field], (field, field_value, FIELDS_AFTER[field])
        else:
            assert field_value == FIELDS_BEFORE[field], (field, field_value, FIELDS_BEFORE[field])

    changes = person_review.changes.all()
    if not updated_fields:
        assert len(changes) == 0
    else:
        assert len(changes) == 1

        only_change = changes[0]
        diff_fields = updated_fields
        if is_updating_bonus:
            diff_fields = chain(diff_fields, BONUS_FIELDS)
        if is_updating_salary:
            diff_fields = chain(diff_fields, SALARY_CHANGE_FIELDS)
        expecting_diff = {
            field: {
                'old': FIELDS_BEFORE[field],
                'new': FIELDS_AFTER[field],
            } for field in diff_fields
        }
        if bonus_type is not None and bonus_type != const.SALARY_DEPENDENCY_TYPE.DEFAULT:
            expecting_diff[const.FIELDS.BONUS_TYPE] = {
                'old': const.SALARY_DEPENDENCY_TYPE.DEFAULT,
                'new': bonus_type,
            }
        if salary_change_type is not None and salary_change_type != const.SALARY_DEPENDENCY_TYPE.DEFAULT:
            expecting_diff[const.FIELDS.SALARY_CHANGE_TYPE] = {
                'old': const.SALARY_DEPENDENCY_TYPE.DEFAULT,
                'new': salary_change_type,
            }

        assert only_change.diff == expecting_diff, (only_change.diff, expecting_diff)


def build_person_matched_goodie(
    case,
    review_goodie_builder,
    finance_builder,
):
    review_goodie_builder(
        review=case.review,
        level=PERSON_LEVEL,
        mark=FIELDS_AFTER[const.FIELDS.MARK],
        goldstar=FIELDS_AFTER[const.FIELDS.GOLDSTAR],
        level_change=FIELDS_AFTER[const.FIELDS.LEVEL_CHANGE],
        salary_change=FIELDS_AFTER[const.FIELDS.SALARY_CHANGE],
        bonus=FIELDS_AFTER[const.FIELDS.BONUS],
        options_rsu=FIELDS_AFTER[const.FIELDS.OPTIONS_RSU],
    )
    finance_builder(
        person=case.person_review.person,
        grade_history=[
            {
                'gradeName': 'Other.%s.3' % PERSON_LEVEL,
                'dateFrom': datetimes.shifted(
                    case.review.start_date,
                    months=-5
                ).isoformat(),
                'dateTo': datetimes.shifted(
                    case.review.start_date,
                    months=+5
                ).isoformat(),
            }
        ]
    )
