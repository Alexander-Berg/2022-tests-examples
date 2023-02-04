# coding: utf-8


import mock

from review.core import const as const
from review.core import models
from review.lib import datetimes
from review.core.logic import bulk

from tests import helpers


def test_edit_no_access_not_fails(person, person_review):
    review = person_review.review
    helpers.update_model(
        person_review,
        mark='bad',
    )
    helpers.update_model(review, mark_mode=const.REVIEW_MODE.MODE_MANUAL)

    with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
        bulk_result = bulk.bulk_same_action_set(
            subject=person,
            ids=[person_review.id],
            params={
                const.PERSON_REVIEW_ACTIONS.MARK: 'good',
            }
        )
    action_result = bulk_result[person_review]
    assert action_result['failed']

    model_in_db = helpers.fetch_model(person_review)
    assert model_in_db.mark == 'bad', bulk_result


def test_auto_set_goodies_nothing_changes(
    case_actual_reviewer_and_others,
    review_goodie_builder,
    finance_builder,
):
    subject = case_actual_reviewer_and_others.reviewer
    person_review = case_actual_reviewer_and_others.person_review
    review = person_review.review
    helpers.update_model(person_review, mark='extraordinary')
    helpers.update_model(
        review,
        mark_mode=const.REVIEW_MODE.MODE_MANUAL,
        salary_change_mode=const.REVIEW_MODE.MODE_AUTO,
        bonus_mode=const.REVIEW_MODE.MODE_DISABLED,
    )
    _prepare_auto_set_goodies(
        review=review,
        person_review=person_review,
        review_goodie_builder=review_goodie_builder,
        finance_builder=finance_builder,
    )

    with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
        bulk.bulk_same_action_set(
            subject=subject,
            ids=[person_review.id],
            params={
                const.PERSON_REVIEW_ACTIONS.MARK: 'extraordinary',
            }
        )

    assert not models.PersonReviewChange.objects.filter(
        person_review_id=person_review.id
    ).exists()


def test_auto_set_goodies_only_goldstar_changes(
    case_actual_reviewer_and_others,
    review_goodie_builder,
    finance_builder,
):
    subject = case_actual_reviewer_and_others.reviewer
    person_review = case_actual_reviewer_and_others.person_review
    review = person_review.review
    helpers.update_model(
        person_review,
        mark='extraordinary',
        goldstar=const.GOLDSTAR.BONUS_ONLY,
    )
    helpers.update_model(
        review,
        mark_mode=const.REVIEW_MODE.MODE_MANUAL,
        goldstar_mode=const.REVIEW_MODE.MODE_MANUAL,
        salary_change_mode=const.REVIEW_MODE.MODE_AUTO,
        options_rsu_mode=const.REVIEW_MODE.MODE_AUTO,
    )
    goodies = _prepare_auto_set_goodies(
        review=review,
        person_review=person_review,
        review_goodie_builder=review_goodie_builder,
        finance_builder=finance_builder,
    )
    e_option_goodie = goodies['e_option_goodie']

    with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
        bulk.bulk_same_action_set(
            subject=subject,
            ids=[person_review.id],
            params={
                const.PERSON_REVIEW_ACTIONS.MARK: 'extraordinary',
                const.PERSON_REVIEW_ACTIONS.GOLDSTAR: const.GOLDSTAR.OPTION_ONLY,
            }
        )

    _check_last_change(person_review, expected_diff={
        'goldstar': {
            'new': const.GOLDSTAR.OPTION_ONLY,
        },
        'options_rsu': {
            'old': 0,
            'new': e_option_goodie.options_rsu,
        },
    })

    model_in_db = helpers.fetch_model(person_review)
    assert model_in_db.mark == 'extraordinary'
    assert model_in_db.goldstar == const.GOLDSTAR.OPTION_ONLY
    assert model_in_db.options_rsu == e_option_goodie.options_rsu


def test_in_evaluation_after_mark_from_import(
    review_role_superreviewer,
    person_review_builder,
):
    # CIA-822: Не появляется кнопка Согласовать
    # если оценка была импортирована из файла
    review = review_role_superreviewer.review
    person_review = person_review_builder(
        review=review,
        status=const.PERSON_REVIEW_STATUS.WAIT_EVALUATION,
    )
    helpers.update_model(review, mark_mode=const.REVIEW_MODE.MODE_MANUAL)

    bulk.bulk_different_action_set(
        subject=review_role_superreviewer.person,
        data={
            person_review.id: {
                const.PERSON_REVIEW_ACTIONS.MARK: 'extraordinary',
            }
        }
    )

    person_review = helpers.fetch_model(person_review)
    assert person_review.status == const.PERSON_REVIEW_STATUS.EVALUATION


def _check_last_change(person_review, expected_fields=None, expected_diff=None):
    change = models.PersonReviewChange.objects.filter(
        person_review_id=person_review.id
    ).order_by('-created_at').first()
    if expected_fields:
        helpers.check_db_data(change, expected_fields)
    if expected_diff:
        helpers.assert_is_substructure(expected_diff, change.diff)


def _prepare_auto_set_goodies(
    review,
    person_review,
    review_goodie_builder,
    finance_builder
):
    PERSON_LEVEL = 10
    e_no_goodie = review_goodie_builder(
        review=review,
        level=PERSON_LEVEL,
        mark='extraordinary',
        goldstar=const.GOLDSTAR.NO,
        salary_change=50,
    )
    e_option_goodie = review_goodie_builder(
        review=review,
        level=PERSON_LEVEL,
        mark='extraordinary',
        goldstar=const.GOLDSTAR.OPTION_ONLY,
        salary_change=0,
        options_rsu=500,
    )
    finance_builder(
        person=person_review.person,
        grade_history=[
            {
                'gradeName': 'Other.%s.3' % PERSON_LEVEL,
                'dateFrom': datetimes.shifted(
                    review.start_date,
                    months=-5
                ).isoformat(),
                'dateTo': datetimes.shifted(
                    review.start_date,
                    months=+5
                ).isoformat(),
            }
        ]
    )
    return {
        'e_no_goodie': e_no_goodie,
        'e_option_goodie': e_option_goodie,
    }
