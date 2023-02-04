# coding: utf-8


import mock
import pytest

from review.core import const as const
from review.core.logic import bulk
from review.core import models as core_models
from review.core.logic import assemble

from tests import helpers


STATUSES = const.PERSON_REVIEW_STATUS
ACTIONS = const.PERSON_REVIEW_ACTIONS
FIELDS = const.FIELDS
MARKS = const.MARK


@pytest.fixture
def test_data(
    person_review_role_builder,
    review_role_builder,
):
    reviewer_one = person_review_role_builder(
        position=0,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
    )
    person_review = reviewer_one.person_review
    review = person_review.review

    reviewer_two = person_review_role_builder(
        person_review=person_review,
        position=1,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
    )
    reviewer_top = person_review_role_builder(
        person_review=person_review,
        position=2,
        type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
    )
    superreviewer = review_role_builder(
        review=review,
        type=const.ROLE.REVIEW.SUPERREVIEWER,
    )

    return {
        'person_review': person_review,
        'reviewer_one': reviewer_one,
        'reviewer_two': reviewer_two,
        'reviewer_top': reviewer_top,
        'superreviewer': superreviewer,
    }


@pytest.fixture
def test_data_marks_enabled(test_data):
    review = test_data['person_review'].review
    helpers.update_model(
        review,
        mark_mode=const.REVIEW_MODE.MODE_MANUAL,
        status=const.REVIEW_STATUS.IN_PROGRESS,
    )
    return test_data


@pytest.fixture
def test_data_marks_disabled(test_data):
    review = test_data['person_review'].review
    helpers.update_model(
        review,
        mark_mode=const.REVIEW_MODE.MODE_DISABLED,
        bonus_mode=const.REVIEW_MODE.MODE_MANUAL,
        status=const.REVIEW_STATUS.IN_PROGRESS,
    )
    return test_data


def _do_action(person_review, role, params):
    with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
        bulk_result = bulk.bulk_same_action_set(
            subject=role.person,
            ids=[person_review.id],
            params=params,
        )
    only_change = list(bulk_result.values())[0]
    assert not only_change['failed']
    return bulk_result


def _check_last_changes_model(person_review, expected):
    last_change = core_models.PersonReviewChange.objects.filter(
        person_review_id=person_review.id,
    ).order_by('created_at_auto').last()
    assert last_change
    diff = last_change.diff
    assert diff == expected


def test_wait_evaluation_set_mark_marks_enabled(test_data_marks_enabled):
    person_review = test_data_marks_enabled['person_review']

    helpers.update_model(
        person_review,
        mark=MARKS.NOT_SET,
        approve_level=0,
        status=STATUSES.WAIT_EVALUATION,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_enabled['reviewer_one'],
        params={
            ACTIONS.MARK: 'extraordinary',
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.MARK: 'extraordinary',
            FIELDS.STATUS: STATUSES.EVALUATION,
            FIELDS.APPROVE_LEVEL: 0,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.MARK: {'old': MARKS.NOT_SET, 'new': 'extraordinary'},
            FIELDS.STATUS: {
                'old': STATUSES.WAIT_EVALUATION,
                'new': STATUSES.EVALUATION,
            },
        }
    )


def test_evaluation_approve_marks_enabled(test_data_marks_enabled):
    person_review = test_data_marks_enabled['person_review']

    helpers.update_model(
        person_review,
        mark='extraordinary',
        approve_level=0,
        status=STATUSES.EVALUATION,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_enabled['reviewer_one'],
        params={
            ACTIONS.APPROVE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.MARK: 'extraordinary',
            FIELDS.STATUS: STATUSES.APPROVAL,
            FIELDS.APPROVE_LEVEL: 1,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.APPROVE_LEVEL: {
                'old': 0,
                'new': 1,
            },
            FIELDS.STATUS: {
                'old': STATUSES.EVALUATION,
                'new': STATUSES.APPROVAL,
            },
        }
    )


def test_approval_unapprove_marks_enabled(test_data_marks_enabled):
    person_review = test_data_marks_enabled['person_review']

    helpers.update_model(
        person_review,
        mark='extraordinary',
        approve_level=1,
        status=STATUSES.APPROVAL,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_enabled['reviewer_one'],
        params={
            ACTIONS.UNAPPROVE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.MARK: 'extraordinary',
            FIELDS.STATUS: STATUSES.EVALUATION,
            FIELDS.APPROVE_LEVEL: 0,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.APPROVE_LEVEL: {
                'old': 1,
                'new': 0,
            },
            FIELDS.STATUS: {
                'old': STATUSES.APPROVAL,
                'new': STATUSES.EVALUATION,
            },
        }
    )


def test_approval_approve_in_middle_marks_enabled(test_data_marks_enabled):
    person_review = test_data_marks_enabled['person_review']

    helpers.update_model(
        person_review,
        mark='extraordinary',
        approve_level=1,
        status=STATUSES.APPROVAL,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_enabled['reviewer_two'],
        params={
            ACTIONS.APPROVE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.MARK: 'extraordinary',
            FIELDS.STATUS: STATUSES.APPROVAL,
            FIELDS.APPROVE_LEVEL: 2,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.APPROVE_LEVEL: {
                'old': 1,
                'new': 2,
            },
        }
    )


def test_approval_approve_by_top_marks_enabled(test_data_marks_enabled):
    person_review = test_data_marks_enabled['person_review']

    helpers.update_model(
        person_review,
        mark='extraordinary',
        approve_level=2,
        status=STATUSES.APPROVAL,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_enabled['reviewer_top'],
        params={
            ACTIONS.APPROVE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.MARK: 'extraordinary',
            FIELDS.STATUS: STATUSES.APPROVED,
            FIELDS.APPROVE_LEVEL: 2,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.STATUS: {
                'old': STATUSES.APPROVAL,
                'new': STATUSES.APPROVED,
            },
        }
    )


def test_approval_allow_announce_marks_enabled(test_data_marks_enabled):
    person_review = test_data_marks_enabled['person_review']

    helpers.update_model(
        person_review,
        mark='extraordinary',
        approve_level=2,
        status=STATUSES.APPROVED,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_enabled['reviewer_top'],
        params={
            ACTIONS.ALLOW_ANNOUNCE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.MARK: 'extraordinary',
            FIELDS.STATUS: STATUSES.WAIT_ANNOUNCE,
            FIELDS.APPROVE_LEVEL: 2,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.STATUS: {
                'old': STATUSES.APPROVED,
                'new': STATUSES.WAIT_ANNOUNCE,
            },
        }
    )


def test_approval_announce_marks_enabled(test_data_marks_enabled):
    person_review = test_data_marks_enabled['person_review']

    helpers.update_model(
        person_review,
        mark='extraordinary',
        approve_level=2,
        status=STATUSES.WAIT_ANNOUNCE,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_enabled['reviewer_top'],
        params={
            ACTIONS.ANNOUNCE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.MARK: 'extraordinary',
            FIELDS.STATUS: STATUSES.ANNOUNCED,
            FIELDS.APPROVE_LEVEL: 2,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.STATUS: {
                'old': STATUSES.WAIT_ANNOUNCE,
                'new': STATUSES.ANNOUNCED,
            },
        }
    )


def test_evaluation_approve_in_middle_with_skip_marks_enabled(test_data_marks_enabled):
    person_review = test_data_marks_enabled['person_review']

    helpers.update_model(
        person_review,
        mark='extraordinary',
        approve_level=0,
        status=STATUSES.EVALUATION,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_enabled['reviewer_two'],
        params={
            ACTIONS.APPROVE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.MARK: 'extraordinary',
            FIELDS.STATUS: STATUSES.APPROVAL,
            FIELDS.APPROVE_LEVEL: 2,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.STATUS: {
                'old': STATUSES.EVALUATION,
                'new': STATUSES.APPROVAL,
            },
            FIELDS.APPROVE_LEVEL: {
                'old': 0,
                'new': 2,
            },
        }
    )


def test_evaluation_approve_by_top_with_skip_all_marks_enabled(test_data_marks_enabled):
    person_review = test_data_marks_enabled['person_review']

    helpers.update_model(
        person_review,
        mark='extraordinary',
        approve_level=0,
        status=STATUSES.EVALUATION,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_enabled['reviewer_top'],
        params={
            ACTIONS.APPROVE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.MARK: 'extraordinary',
            FIELDS.STATUS: STATUSES.APPROVED,
            FIELDS.APPROVE_LEVEL: 2,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.STATUS: {
                'old': STATUSES.EVALUATION,
                'new': STATUSES.APPROVED,
            },
            FIELDS.APPROVE_LEVEL: {
                'old': 0,
                'new': 2,
            },
        }
    )


def test_approval_approve_by_top_with_skip_not_all_marks_enabled(test_data_marks_enabled):
    person_review = test_data_marks_enabled['person_review']

    helpers.update_model(
        person_review,
        mark='extraordinary',
        approve_level=1,
        status=STATUSES.APPROVAL,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_enabled['reviewer_top'],
        params={
            ACTIONS.APPROVE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.MARK: 'extraordinary',
            FIELDS.STATUS: STATUSES.APPROVED,
            FIELDS.APPROVE_LEVEL: 2,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.STATUS: {
                'old': STATUSES.APPROVAL,
                'new': STATUSES.APPROVED,
            },
            FIELDS.APPROVE_LEVEL: {
                'old': 1,
                'new': 2,
            },
        }
    )


def test_evaluation_approve_by_super_marks_enabled(test_data_marks_enabled):
    person_review = test_data_marks_enabled['person_review']

    helpers.update_model(
        person_review,
        mark='extraordinary',
        approve_level=0,
        status=STATUSES.EVALUATION,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_enabled['superreviewer'],
        params={
            ACTIONS.APPROVE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.MARK: 'extraordinary',
            FIELDS.STATUS: STATUSES.APPROVAL,
            FIELDS.APPROVE_LEVEL: 1,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.STATUS: {
                'old': STATUSES.EVALUATION,
                'new': STATUSES.APPROVAL,
            },
            FIELDS.APPROVE_LEVEL: {
                'old': 0,
                'new': 1,
            },
        }
    )


def test_approval_approve_by_super_non_final_marks_enabled(test_data_marks_enabled):
    person_review = test_data_marks_enabled['person_review']

    helpers.update_model(
        person_review,
        mark='extraordinary',
        approve_level=1,
        status=STATUSES.APPROVAL,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_enabled['superreviewer'],
        params={
            ACTIONS.APPROVE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.MARK: 'extraordinary',
            FIELDS.STATUS: STATUSES.APPROVAL,
            FIELDS.APPROVE_LEVEL: 2,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.APPROVE_LEVEL: {
                'old': 1,
                'new': 2,
            },
        }
    )


def test_approval_approve_by_super_final_marks_enabled(test_data_marks_enabled):
    person_review = test_data_marks_enabled['person_review']

    helpers.update_model(
        person_review,
        mark='extraordinary',
        approve_level=2,
        status=STATUSES.APPROVAL,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_enabled['superreviewer'],
        params={
            ACTIONS.APPROVE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.MARK: 'extraordinary',
            FIELDS.STATUS: STATUSES.APPROVED,
            FIELDS.APPROVE_LEVEL: 2,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.STATUS: {
                'old': STATUSES.APPROVAL,
                'new': STATUSES.APPROVED,
            },
        }
    )


def test_wait_evaluation_set_bonus_marks_disabled(test_data_marks_disabled):
    person_review = test_data_marks_disabled['person_review']

    helpers.update_model(
        person_review,
        approve_level=0,
        status=STATUSES.WAIT_EVALUATION,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_disabled['reviewer_one'],
        params={
            ACTIONS.BONUS: 100,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.BONUS: 100,
            FIELDS.STATUS: STATUSES.EVALUATION,
            FIELDS.APPROVE_LEVEL: 0,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.BONUS: {
                'old': 0,
                'new': 100,
            },
            FIELDS.BONUS_ABSOLUTE: {
                'old': 0,
                'new': 100,
            },
            FIELDS.STATUS: {
                'old': STATUSES.WAIT_EVALUATION,
                'new': STATUSES.EVALUATION,
            },
        }
    )


def test_wait_evaluation_approve_marks_disabled(test_data_marks_disabled):
    person_review = test_data_marks_disabled['person_review']

    helpers.update_model(
        person_review,
        approve_level=0,
        status=STATUSES.WAIT_EVALUATION,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_disabled['reviewer_one'],
        params={
            ACTIONS.APPROVE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.STATUS: STATUSES.APPROVAL,
            FIELDS.APPROVE_LEVEL: 1,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.APPROVE_LEVEL: {
                'old': 0,
                'new': 1,
            },
            FIELDS.STATUS: {
                'old': STATUSES.WAIT_EVALUATION,
                'new': STATUSES.APPROVAL,
            },
        }
    )


def test_approval_unapprove_marks_disabled(test_data_marks_disabled):
    person_review = test_data_marks_disabled['person_review']

    helpers.update_model(
        person_review,
        approve_level=1,
        status=STATUSES.APPROVAL,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_disabled['reviewer_one'],
        params={
            ACTIONS.UNAPPROVE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.STATUS: STATUSES.WAIT_EVALUATION,
            FIELDS.APPROVE_LEVEL: 0,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.APPROVE_LEVEL: {
                'old': 1,
                'new': 0,
            },
            FIELDS.STATUS: {
                'old': STATUSES.APPROVAL,
                'new': STATUSES.WAIT_EVALUATION,
            },
        }
    )


def test_approval_approve_in_middle_marks_disabled(test_data_marks_disabled):
    person_review = test_data_marks_disabled['person_review']

    helpers.update_model(
        person_review,
        approve_level=1,
        status=STATUSES.APPROVAL,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_disabled['reviewer_two'],
        params={
            ACTIONS.APPROVE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.STATUS: STATUSES.APPROVAL,
            FIELDS.APPROVE_LEVEL: 2,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.APPROVE_LEVEL: {
                'old': 1,
                'new': 2,
            },
        }
    )


def test_approval_approve_by_top_marks_disabled(test_data_marks_disabled):
    person_review = test_data_marks_disabled['person_review']

    helpers.update_model(
        person_review,
        approve_level=2,
        status=STATUSES.APPROVAL,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_disabled['reviewer_top'],
        params={
            ACTIONS.APPROVE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.STATUS: STATUSES.APPROVED,
            FIELDS.APPROVE_LEVEL: 2,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.STATUS: {
                'old': STATUSES.APPROVAL,
                'new': STATUSES.APPROVED,
            },
        }
    )


def test_approval_allow_announce_marks_disabled(test_data_marks_disabled):
    person_review = test_data_marks_disabled['person_review']

    helpers.update_model(
        person_review,
        approve_level=2,
        status=STATUSES.APPROVED,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_disabled['reviewer_top'],
        params={
            ACTIONS.ALLOW_ANNOUNCE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.STATUS: STATUSES.WAIT_ANNOUNCE,
            FIELDS.APPROVE_LEVEL: 2,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.STATUS: {
                'old': STATUSES.APPROVED,
                'new': STATUSES.WAIT_ANNOUNCE,
            },
        }
    )


def test_approval_announce_marks_disabled(test_data_marks_disabled):
    person_review = test_data_marks_disabled['person_review']

    helpers.update_model(
        person_review,
        approve_level=2,
        status=STATUSES.WAIT_ANNOUNCE,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_disabled['reviewer_top'],
        params={
            ACTIONS.ANNOUNCE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.STATUS: STATUSES.ANNOUNCED,
            FIELDS.APPROVE_LEVEL: 2,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.STATUS: {
                'old': STATUSES.WAIT_ANNOUNCE,
                'new': STATUSES.ANNOUNCED,
            },
        }
    )


def test_wait_evaluation_approve_in_middle_with_skip_marks_disabled(test_data_marks_disabled):
    person_review = test_data_marks_disabled['person_review']

    helpers.update_model(
        person_review,
        approve_level=0,
        status=STATUSES.WAIT_EVALUATION,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_disabled['reviewer_two'],
        params={
            ACTIONS.APPROVE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.STATUS: STATUSES.APPROVAL,
            FIELDS.APPROVE_LEVEL: 2,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.STATUS: {
                'old': STATUSES.WAIT_EVALUATION,
                'new': STATUSES.APPROVAL,
            },
            FIELDS.APPROVE_LEVEL: {
                'old': 0,
                'new': 2,
            },
        }
    )


def test_wait_evaluation_approve_by_top_with_skip_all_marks_disabled(test_data_marks_disabled):
    person_review = test_data_marks_disabled['person_review']

    helpers.update_model(
        person_review,
        approve_level=0,
        status=STATUSES.WAIT_EVALUATION,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_disabled['reviewer_top'],
        params={
            ACTIONS.APPROVE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.STATUS: STATUSES.APPROVED,
            FIELDS.APPROVE_LEVEL: 2,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.STATUS: {
                'old': STATUSES.WAIT_EVALUATION,
                'new': STATUSES.APPROVED,
            },
            FIELDS.APPROVE_LEVEL: {
                'old': 0,
                'new': 2,
            },
        }
    )


def test_approval_approve_by_top_with_skip_not_all_marks_disabled(test_data_marks_disabled):
    person_review = test_data_marks_disabled['person_review']

    helpers.update_model(
        person_review,
        approve_level=1,
        status=STATUSES.APPROVAL,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_disabled['reviewer_top'],
        params={
            ACTIONS.APPROVE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.STATUS: STATUSES.APPROVED,
            FIELDS.APPROVE_LEVEL: 2,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.STATUS: {
                'old': STATUSES.APPROVAL,
                'new': STATUSES.APPROVED,
            },
            FIELDS.APPROVE_LEVEL: {
                'old': 1,
                'new': 2,
            },
        }
    )


def test_wait_evaluation_approve_by_super_marks_disabled(test_data_marks_disabled):
    person_review = test_data_marks_disabled['person_review']

    helpers.update_model(
        person_review,
        approve_level=0,
        status=STATUSES.WAIT_EVALUATION,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_disabled['superreviewer'],
        params={
            ACTIONS.APPROVE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.STATUS: STATUSES.APPROVAL,
            FIELDS.APPROVE_LEVEL: 1,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.STATUS: {
                'old': STATUSES.WAIT_EVALUATION,
                'new': STATUSES.APPROVAL,
            },
            FIELDS.APPROVE_LEVEL: {
                'old': 0,
                'new': 1,
            },
        }
    )


def test_approval_approve_by_super_non_final_marks_disabled(test_data_marks_disabled):
    person_review = test_data_marks_disabled['person_review']

    helpers.update_model(
        person_review,
        approve_level=1,
        status=STATUSES.APPROVAL,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_disabled['superreviewer'],
        params={
            ACTIONS.APPROVE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.STATUS: STATUSES.APPROVAL,
            FIELDS.APPROVE_LEVEL: 2,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.APPROVE_LEVEL: {
                'old': 1,
                'new': 2,
            },
        }
    )


def test_approval_approve_by_super_final_marks_disabled(test_data_marks_disabled):
    person_review = test_data_marks_disabled['person_review']

    helpers.update_model(
        person_review,
        mark='extraordinary',
        approve_level=2,
        status=STATUSES.APPROVAL,
    )

    _do_action(
        person_review=person_review,
        role=test_data_marks_disabled['superreviewer'],
        params={
            ACTIONS.APPROVE: True,
        }
    )
    helpers.check_db_data(
        something=person_review,
        expected={
            FIELDS.MARK: 'extraordinary',
            FIELDS.STATUS: STATUSES.APPROVED,
            FIELDS.APPROVE_LEVEL: 2,
        }
    )
    _check_last_changes_model(
        person_review=person_review,
        expected={
            FIELDS.STATUS: {
                'old': STATUSES.APPROVAL,
                'new': STATUSES.APPROVED,
            },
        }
    )


@pytest.fixture(params=['flagged', 'flagged_positive'])
def flagged_person_review(request, test_data_marks_enabled):
    person_review = test_data_marks_enabled['person_review']
    helpers.update_model(person_review, **{request.param: True})
    return person_review


@pytest.mark.parametrize(
    'action,approve_level,status,reviewer_type', [
        (
            ACTIONS.APPROVE,
            0,
            STATUSES.EVALUATION,
            'reviewer_one'
        ),
        (
            ACTIONS.UNAPPROVE,
            1,
            STATUSES.APPROVAL,
            'reviewer_one'
        ),
        (
            ACTIONS.ALLOW_ANNOUNCE,
            2,
            STATUSES.APPROVED,
            'reviewer_top'
        ),
        (
            ACTIONS.ANNOUNCE,
            2,
            STATUSES.WAIT_ANNOUNCE,
            'reviewer_top'
        ),
    ]
)
def test_workflow_fails_if_flagged(
    test_data_marks_enabled,
    flagged_person_review,
    action,
    approve_level,
    status,
    reviewer_type
):
    helpers.update_model(
        flagged_person_review,
        mark='extraordinary',
        approve_level=approve_level,
        status=status,
    )
    with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
        bulk_result = bulk.bulk_same_action_set(
            subject=test_data_marks_enabled[reviewer_type].person,
            ids=[flagged_person_review.id],
            params={action: True},
        )
    helpers.check_db_data(
        something=flagged_person_review,
        expected={
            FIELDS.MARK: 'extraordinary',
            FIELDS.STATUS: status,
            FIELDS.APPROVE_LEVEL: approve_level,
        }
    )
    assert bulk_result[flagged_person_review]['failed'][action] == const.NO_ACCESS


@pytest.mark.parametrize(
    'role_name,status,approve_level,expected', [
        (
            'reviewer_one',
            STATUSES.WAIT_EVALUATION,
            0,
            [FIELDS.ACTION_MARK],
        ),
        (
            'reviewer_one',
            STATUSES.EVALUATION,
            0,
            [
                FIELDS.ACTION_MARK,
                FIELDS.ACTION_APPROVE,
            ],
        ),
        (
            'reviewer_one',
            STATUSES.APPROVAL,
            1,
            [
                FIELDS.ACTION_UNAPPROVE,
            ],
        ),
        (
            'reviewer_one',
            STATUSES.APPROVAL,
            2,
            [],
        ),
        (
            'reviewer_one',
            STATUSES.APPROVED,
            2,
            [],
        ),
        (
            'reviewer_one',
            STATUSES.WAIT_ANNOUNCE,
            2,
            [
                FIELDS.ACTION_ANNOUNCE
            ],
        ),
        (
            'reviewer_one',
            STATUSES.ANNOUNCED,
            2,
            [],
        ),
        (
            'reviewer_two',
            STATUSES.WAIT_EVALUATION,
            0,
            [FIELDS.ACTION_MARK],
        ),
        (
            'reviewer_two',
            STATUSES.EVALUATION,
            0,
            [
                FIELDS.ACTION_MARK,
                FIELDS.ACTION_APPROVE,
            ],
        ),
        (
            'reviewer_two',
            STATUSES.APPROVAL,
            1,
            [
                FIELDS.ACTION_MARK,
                FIELDS.ACTION_APPROVE,
            ],
        ),
        (
            'reviewer_two',
            STATUSES.APPROVAL,
            2,
            [
                FIELDS.ACTION_UNAPPROVE,
            ],
        ),
        (
            'reviewer_two',
            STATUSES.APPROVED,
            2,
            [],
        ),
        (
            'reviewer_two',
            STATUSES.WAIT_ANNOUNCE,
            2,
            [
                FIELDS.ACTION_ANNOUNCE,
            ],
        ),
        (
            'reviewer_two',
            STATUSES.ANNOUNCED,
            2,
            [],
        ),
        (
            'reviewer_top',
            STATUSES.WAIT_EVALUATION,
            0,
            [FIELDS.ACTION_MARK],
        ),
        (
            'reviewer_top',
            STATUSES.EVALUATION,
            0,
            [
                FIELDS.ACTION_MARK,
                FIELDS.ACTION_APPROVE,
            ],
        ),
        (
            'reviewer_top',
            STATUSES.APPROVAL,
            1,
            [
                FIELDS.ACTION_MARK,
                FIELDS.ACTION_APPROVE,
            ],
        ),
        (
            'reviewer_top',
            STATUSES.APPROVAL,
            2,
            [
                FIELDS.ACTION_MARK,
                FIELDS.ACTION_APPROVE,
            ],
        ),
        (
            'reviewer_top',
            STATUSES.APPROVED,
            2,
            [
                FIELDS.ACTION_MARK,
                FIELDS.ACTION_UNAPPROVE,
                FIELDS.ACTION_ALLOW_ANNOUNCE,
            ],
        ),
        (
            'reviewer_top',
            STATUSES.WAIT_ANNOUNCE,
            2,
            [
                FIELDS.ACTION_ANNOUNCE,
            ],
        ),
        (
            'reviewer_top',
            STATUSES.ANNOUNCED,
            2,
            [],
        ),
        (
            'superreviewer',
            STATUSES.WAIT_EVALUATION,
            0,
            [FIELDS.ACTION_MARK],
        ),
        (
            'superreviewer',
            STATUSES.EVALUATION,
            0,
            [
                FIELDS.ACTION_MARK,
                FIELDS.ACTION_APPROVE,
            ],
        ),
        (
            'superreviewer',
            STATUSES.APPROVAL,
            1,
            [
                FIELDS.ACTION_MARK,
                FIELDS.ACTION_UNAPPROVE,
                FIELDS.ACTION_APPROVE,
            ],
        ),
        (
            'superreviewer',
            STATUSES.APPROVAL,
            2,
            [
                FIELDS.ACTION_MARK,
                FIELDS.ACTION_UNAPPROVE,
                FIELDS.ACTION_APPROVE,
            ],
        ),
        (
            'superreviewer',
            STATUSES.APPROVED,
            2,
            [
                FIELDS.ACTION_MARK,
                FIELDS.ACTION_UNAPPROVE,
                FIELDS.ACTION_ALLOW_ANNOUNCE,
            ],
        ),
        (
            'superreviewer',
            STATUSES.WAIT_ANNOUNCE,
            2,
            [
                FIELDS.ACTION_MARK,
                FIELDS.ACTION_ANNOUNCE,
            ],
        ),
        (
            'superreviewer',
            STATUSES.ANNOUNCED,
            2,
            [
                FIELDS.ACTION_MARK,
            ],
        ),
    ]
)
def test_available_transitions_marks_enabled(
        role_name, status, approve_level, expected, test_data_marks_enabled):
    person_review = test_data_marks_enabled['person_review']
    role = test_data_marks_enabled[role_name]

    helpers.update_model(
        person_review,
        status=status,
        approve_level=approve_level,
    )
    if status == STATUSES.WAIT_EVALUATION:
        helpers.update_model(person_review, mark=MARKS.NOT_SET)
    else:
        helpers.update_model(person_review, mark='extraordinary')

    person_review_extended = assemble.get_person_review(
        subject=role.person,
        fields_requested=FIELDS.ALL_ACTION_FIELDS,
        id=person_review.id,
    )

    relevant_action_fields = FIELDS.WORKFLOW_ACTION_FIELDS | {FIELDS.ACTION_MARK}
    available_action_fields = {
        field for field in relevant_action_fields
        if getattr(person_review_extended, field) == const.OK
    }
    assert available_action_fields == set(expected)


@pytest.mark.parametrize(
    'role_name,status,approve_level,expected', [
        (
            'reviewer_one',
            STATUSES.WAIT_EVALUATION,
            0,
            [
                FIELDS.ACTION_BONUS,
                FIELDS.ACTION_APPROVE,
            ],
        ),
        (
            'reviewer_one',
            STATUSES.APPROVAL,
            1,
            [
                FIELDS.ACTION_UNAPPROVE,
            ],
        ),
        (
            'reviewer_one',
            STATUSES.APPROVAL,
            2,
            [],
        ),
        (
            'reviewer_one',
            STATUSES.APPROVED,
            2,
            [],
        ),
        (
            'reviewer_one',
            STATUSES.WAIT_ANNOUNCE,
            2,
            [
                FIELDS.ACTION_ANNOUNCE,
            ],
        ),
        (
            'reviewer_one',
            STATUSES.ANNOUNCED,
            2,
            [],
        ),
        (
            'reviewer_two',
            STATUSES.WAIT_EVALUATION,
            0,
            [
                FIELDS.ACTION_BONUS,
                FIELDS.ACTION_APPROVE,
            ],
        ),
        (
            'reviewer_two',
            STATUSES.APPROVAL,
            1,
            [
                FIELDS.ACTION_BONUS,
                FIELDS.ACTION_APPROVE,
            ],
        ),
        (
            'reviewer_two',
            STATUSES.APPROVAL,
            2,
            [
                FIELDS.ACTION_UNAPPROVE,
            ],
        ),
        (
            'reviewer_two',
            STATUSES.APPROVED,
            2,
            [],
        ),
        (
            'reviewer_two',
            STATUSES.WAIT_ANNOUNCE,
            2,
            [
                FIELDS.ACTION_ANNOUNCE,
            ],
        ),
        (
            'reviewer_two',
            STATUSES.ANNOUNCED,
            2,
            [],
        ),
        (
            'reviewer_top',
            STATUSES.WAIT_EVALUATION,
            0,
            [
                FIELDS.ACTION_BONUS,
                FIELDS.ACTION_APPROVE,
            ],
        ),
        (
            'reviewer_top',
            STATUSES.APPROVAL,
            1,
            [
                FIELDS.ACTION_BONUS,
                FIELDS.ACTION_APPROVE,
            ],
        ),
        (
            'reviewer_top',
            STATUSES.APPROVAL,
            2,
            [
                FIELDS.ACTION_BONUS,
                FIELDS.ACTION_APPROVE,
            ],
        ),
        (
            'reviewer_top',
            STATUSES.APPROVED,
            2,
            [
                FIELDS.ACTION_BONUS,
                FIELDS.ACTION_UNAPPROVE,
                FIELDS.ACTION_ALLOW_ANNOUNCE,
            ],
        ),
        (
            'reviewer_top',
            STATUSES.WAIT_ANNOUNCE,
            2,
            [
                FIELDS.ACTION_BONUS,
                FIELDS.ACTION_ANNOUNCE,
            ],
        ),
        (
            'reviewer_top',
            STATUSES.ANNOUNCED,
            2,
            [
                FIELDS.ACTION_BONUS,
            ],
        ),
        (
            'superreviewer',
            STATUSES.WAIT_EVALUATION,
            0,
            [
                FIELDS.ACTION_BONUS,
                FIELDS.ACTION_APPROVE,
            ],
        ),
        (
            'superreviewer',
            STATUSES.APPROVAL,
            1,
            [
                FIELDS.ACTION_BONUS,
                FIELDS.ACTION_UNAPPROVE,
                FIELDS.ACTION_APPROVE,
            ],
        ),
        (
            'superreviewer',
            STATUSES.APPROVAL,
            2,
            [
                FIELDS.ACTION_BONUS,
                FIELDS.ACTION_UNAPPROVE,
                FIELDS.ACTION_APPROVE,
            ],
        ),
        (
            'superreviewer',
            STATUSES.APPROVED,
            2,
            [
                FIELDS.ACTION_BONUS,
                FIELDS.ACTION_UNAPPROVE,
                FIELDS.ACTION_ALLOW_ANNOUNCE,
            ],
        ),
        (
            'superreviewer',
            STATUSES.WAIT_ANNOUNCE,
            2,
            [
                FIELDS.ACTION_BONUS,
                FIELDS.ACTION_ANNOUNCE,
            ],
        ),
        (
            'superreviewer',
            STATUSES.ANNOUNCED,
            2,
            [
                FIELDS.ACTION_BONUS,
            ],
        ),
    ]
)
def test_available_transitions_marks_disabled(
        role_name, status, approve_level, expected, test_data_marks_disabled):
    person_review = test_data_marks_disabled['person_review']
    role = test_data_marks_disabled[role_name]

    helpers.update_model(
        person_review,
        status=status,
        approve_level=approve_level,
    )

    person_review_extended = assemble.get_person_review(
        subject=role.person,
        fields_requested=FIELDS.ALL_ACTION_FIELDS,
        id=person_review.id,
    )

    relevant_action_fields = FIELDS.WORKFLOW_ACTION_FIELDS | {FIELDS.ACTION_BONUS}
    available_action_fields = {
        field for field in relevant_action_fields
        if getattr(person_review_extended, field) == const.OK
    }
    assert available_action_fields == set(expected)
