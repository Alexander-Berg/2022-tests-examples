# coding: utf-8


import mock
import pytest

from review.core import const as const
from review.core.logic import bulk

from tests import helpers


def test_edit_mark(person_review_role_reviewer):
    subject = person_review_role_reviewer.person
    person_review = person_review_role_reviewer.person_review
    review = person_review.review
    helpers.update_model(
        person_review,
        mark='bad',
        approve_level=person_review_role_reviewer.position,
    )
    helpers.update_model(review, mark_mode=const.REVIEW_MODE.MODE_MANUAL)

    with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
        bulk_result = bulk.bulk_same_action_set(
            subject=subject,
            ids=[person_review.id],
            params={
                const.PERSON_REVIEW_ACTIONS.MARK: 'good',
            }
        )
    model_in_db = helpers.fetch_model(person_review)
    assert model_in_db.mark == 'good', bulk_result


def test_salary_change_ok(case_actual_reviewer_and_others):
    subject = case_actual_reviewer_and_others.reviewer
    person_review = case_actual_reviewer_and_others.person_review
    review = person_review.review
    helpers.update_model(
        person_review,
        mark='bad',
    )
    helpers.update_model(
        review,
        salary_change_mode=const.REVIEW_MODE.MODE_MANUAL
    )

    with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
        bulk_result = bulk.bulk_same_action_set(
            subject=subject,
            ids=[person_review.id],
            params={
                const.PERSON_REVIEW_ACTIONS.SALARY_CHANGE: 50,
            }
        )

    model_in_db = helpers.fetch_model(person_review)
    assert model_in_db.salary_change == 50, bulk_result


def test_salary_change_auto_failed(case_actual_reviewer_and_others):
    subject = case_actual_reviewer_and_others.reviewer
    person_review = case_actual_reviewer_and_others.person_review
    review = person_review.review
    helpers.update_model(
        person_review,
        mark='bad',
    )
    helpers.update_model(
        review,
        salary_change_mode=const.REVIEW_MODE.MODE_AUTO
    )

    with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
        bulk_result = bulk.bulk_same_action_set(
            subject=subject,
            ids=[person_review.id],
            params={
                const.PERSON_REVIEW_ACTIONS.SALARY_CHANGE: 50,
            }
        )
    only_result = list(bulk_result.values())[0]
    assert only_result['failed'][const.FIELDS.SALARY_CHANGE] == const.NO_ACCESS


@pytest.mark.skip(reason="return after completing CIA-1783")  # TODO
def test_salary_change_failed_if_no_mark_set(case_actual_reviewer_and_others):
    subject = case_actual_reviewer_and_others.reviewer
    person_review = case_actual_reviewer_and_others.person_review
    review = person_review.review
    helpers.update_model(
        person_review,
        mark=const.MARK.NOT_SET,
    )
    helpers.update_model(
        review,
        mark_mode=const.REVIEW_MODE.MODE_MANUAL,
        salary_change_mode=const.REVIEW_MODE.MODE_MANUAL
    )

    with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
        bulk_result = bulk.bulk_same_action_set(
            subject=subject,
            ids=[person_review.id],
            params={
                const.PERSON_REVIEW_ACTIONS.SALARY_CHANGE: 50,
            }
        )

    only_result = list(bulk_result.values())[0]
    assert only_result['failed'][const.FIELDS.SALARY_CHANGE] == const.ERROR_CODES.SHOULD_NOT_BEFORE_MARK
