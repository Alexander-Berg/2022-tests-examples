from functools import partial
from django.db.models import Q
import pytest
from mock import patch

from review.core import const
from review.core.logic import bulk
from review.core.models import (
    PersonReview,
    Review,
)
from review.lib import datetimes

from tests import helpers


@patch('review.core.logic.review_actions._publish_review_results')
def test_review_edit_status_as_creator(prr_mock, client, review_role_admin):
    review = review_role_admin.review
    helpers.post_json(
        client=client,
        path='/frontend/reviews/{}/workflow/'.format(review.id),
        request={
            'workflow': const.REVIEW_ACTIONS.STATUS_ARCHIVE,
        },
        login=review_role_admin.person.login
    )
    prr_mock.assert_called_once_with(review.id)
    assert Review.objects.filter(id=review.id).first().status == const.REVIEW_STATUS.ARCHIVE


def test_reopen_to_finish_review(client, review_role_admin):
    review = review_role_admin.review
    review.status = const.REVIEW_STATUS.ARCHIVE
    now = datetimes.now()
    review.start_date = datetimes.shifted(now, months=-10)
    review.finish_date = datetimes.shifted(now, months=-8)
    review.save()
    helpers.post_json(
        client=client,
        path='/frontend/reviews/{}/workflow/'.format(review.id),
        request={
            'workflow': const.REVIEW_ACTIONS.STATUS_UNARCHIVE,
        },
        login=review_role_admin.person.login
    )
    assert Review.objects.filter(id=review.id).first().status == const.REVIEW_STATUS.FINISHED


def test_reopen_to_progress_review(client, review_role_admin):
    review = review_role_admin.review
    review.status = const.REVIEW_STATUS.ARCHIVE
    review.save()
    helpers.post_json(
        client=client,
        path='/frontend/reviews/{}/workflow/'.format(review.id),
        request={
            'workflow': const.REVIEW_ACTIONS.STATUS_UNARCHIVE,
        },
        login=review_role_admin.person.login
    )
    assert Review.objects.filter(id=review.id).first().status == const.REVIEW_STATUS.IN_PROGRESS


def test_review_not_admin_change(client, review, person):
    review.status = const.REVIEW_STATUS.ARCHIVE
    review.save()
    helpers.post_json(
        client=client,
        path='/frontend/reviews/{}/workflow/'.format(review.id),
        request={
            'workflow': const.REVIEW_ACTIONS.STATUS_PUBLISH,
        },
        login=person.login,
        expect_status=403,
        json_response=False,
    )
    assert Review.objects.filter(id=review.id).first().status == const.REVIEW_STATUS.ARCHIVE
    helpers.post_json(
        client=client,
        path='/frontend/reviews/{}/workflow/'.format(review.id),
        request={
            'workflow': const.REVIEW_ACTIONS.STATUS_UNARCHIVE,
        },
        login=person.login,
        expect_status=403,
        json_response=False,
    )
    assert Review.objects.filter(id=review.id).first().status == const.REVIEW_STATUS.ARCHIVE


def test_review_edit_status_as_bad_guy(client, review, person):
    old_status = review.status
    helpers.post_json(
        client=client,
        path='/frontend/reviews/{}/workflow/'.format(review.id),
        request={
            'workflow': const.REVIEW_ACTIONS.STATUS_ARCHIVE,
        },
        login=person.login,
        expect_status=403,
        json_response=False,
    )
    assert Review.objects.filter(id=review.id).first().status == old_status


def test_review_archive_comments_as_admin(
    client,
    person_review_builder,
    person_review_role_builder,
    review_role_admin,
    robot,
):
    review = review_role_admin.review
    person_review = person_review_builder(review=review)
    review.status = const.REVIEW_STATUS.ARCHIVE
    bulk.publish_person_reviews(robot, review.id)

    reviewer = person_review_role_builder(
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
    ).person
    response = helpers.get_json(
        client,
        path='/frontend/persons/{}/log/'.format(person_review.person.login),
        login=reviewer.login,
    )
    change_types = const.PERSON_REVIEW_CHANGE_TYPE
    assert response['log'][0]['subject_type'] == change_types.VERBOSE[change_types.ROBOT]


@pytest.mark.parametrize(
    'review_mark_type', [
        const.REVIEW_MODE.MODE_MANUAL,
        const.REVIEW_MODE.MODE_DISABLED,
    ]
)
def test_publish_review_results(
    person_review_builder,
    person_review_role_builder,
    review_builder,
    review_mark_type,
    robot,
):
    review = review_builder(mark_mode=review_mark_type)
    STATUS = const.PERSON_REVIEW_STATUS

    def create_with_reviewers(status, num=4, level=0):
        person_review = person_review_builder(
            status=status,
            review=review,
            approve_level=level,
        )
        person_review_role_builder(
            type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
            person_review=person_review,
            position=num,
        )
        for i in range(num - 1):
            person_review_role_builder(
                type=const.ROLE.PERSON_REVIEW.REVIEWER,
                person_review=person_review,
                position=i,
            )

    create_with_reviewers(STATUS.WAIT_EVALUATION)

    if review_mark_type == const.REVIEW_MODE.MODE_MANUAL:
        person_review_builder = partial(
            person_review_builder,
            mark='A',
        )
        create_with_reviewers(STATUS.EVALUATION)
    for reviewers_num, approve_level in (
        (4, 2),
        (4, 1),
        (2, 0),
    ):
        create_with_reviewers(STATUS.APPROVAL, reviewers_num, approve_level)

    create_with_reviewers(STATUS.APPROVED, 2, 1)
    create_with_reviewers(STATUS.WAIT_ANNOUNCE, 2, 1)
    create_with_reviewers(STATUS.ANNOUNCED, 2, 1)

    bulk.publish_person_reviews(robot, review.id)

    is_mark_incorrect = (~Q(mark=const.MARK.NOT_SET) &
                         Q(review__mark_mode=const.REVIEW_MODE.MODE_MANUAL))
    not_announced = ~Q(status=STATUS.ANNOUNCED)
    assert not PersonReview.objects.filter(
        is_mark_incorrect & not_announced
    ).exists()


def test_review_start_with_empty_chains(
    client,
    review_role_admin,
    person_review_builder,
):
    review = review_role_admin.review
    helpers.update_model(review, status=const.REVIEW_STATUS.DRAFT)
    person_review = person_review_builder(review=review)

    response_data = helpers.post_json(
        client=client,
        path='/frontend/reviews/{}/workflow/'.format(review.id),
        request={
            'workflow': const.REVIEW_ACTIONS.STATUS_PUBLISH
        },
        login=review_role_admin.person.login,
        expect_status=400,
    )

    helpers.assert_is_substructure(
        {
            'errors': {
                '*': {
                    'params': {
                        'logins': [person_review.person.login],
                    },
                    'code': 'REVIEW_WORKFLOW_EMPTY_CHAINS',
                }
            }
        },
        response_data
    )
