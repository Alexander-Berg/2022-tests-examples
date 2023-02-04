# coding: utf-8


import mock
import pytest

from review.core import const as const
from review.core import models as core_models
from review.core.logic import bulk
from review.core.logic.bulk import save as bulk_save


def test_reviewer_chain_create(person_builder, person_review):
    persons = [person_builder() for _ in range(10)]
    diff = {
        'old': [],
        'new': [
            [persons[0].login, persons[1].login],
            persons[2].login,
            [persons[3].login, persons[4].login, persons[5].login],
            [persons[1].login, persons[6].login]
        ]
    }
    update, create, delete = bulk_save.build_reviewers_update(person_review, diff)
    assert not update
    assert not delete
    assert len(create) == 8
    assert len([r for r in create if r['position'] == 3]) == 2
    assert len([r for r in create if r['type'] == const.ROLE.PERSON_REVIEW.TOP_REVIEWER]) == 2


def test_reviewer_chain_delete(person_review_role_builder, person_review):
    reviewers, old = create_dummy_reviewers_chain(person_review_role_builder, person_review)
    new = [
        [reviewers[0][0].person.login, reviewers[0][1].person.login, ],
        reviewers[1].person.login,
    ]
    diff = {'old': old, 'new': new}
    update, create, delete = bulk_save.build_reviewers_update(person_review, diff)

    assert not create
    assert all([
        len(update) == 1,
        update[0]['filter']['position'] == 1,
        update[0]['update']['type'] == const.ROLE.PERSON_REVIEW.TOP_REVIEWER
    ])
    assert len(delete) == 2


def test_reviewer_chain_update(person_review_role_builder, person_review):
    reviewers, old = create_dummy_reviewers_chain(person_review_role_builder, person_review)
    super_logins = ['super_login', 'another_super_login']
    new = [
        [reviewers[0][0].person.login, reviewers[0][1].person.login, ],
        reviewers[1].person.login,
        [reviewers[2][0].person.login, reviewers[2][1].person.login, ],
        super_logins
    ]
    diff = {'old': old, 'new': new}
    update, create, delete = bulk_save.build_reviewers_update(person_review, diff)

    assert not delete
    assert all([
        len(update) == 2,
        update[0]['filter']['person__login'] == reviewers[2][0].person.login,
        update[0]['update']['type'] == const.ROLE.PERSON_REVIEW.REVIEWER
    ])


def test_action_add_reviewer_after(person_review, person_review_role_builder, person_builder,
                                   test_person, review_role_builder):
    reviewers = [
        person_review_role_builder(
            position=i,
            type=const.ROLE.PERSON_REVIEW.REVIEWER,
            person_review=person_review,
        )
        for i in range(3)
    ]
    review_admin = review_role_builder(
        person=test_person,
        review=person_review.review,
        type=const.ROLE.REVIEW.ADMIN,
    )
    new_reviewer = person_builder()
    with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
        bulk.bulk_same_action_set(test_person, [person_review.id], {
            'reviewers': {
                'type': const.PERSON_REVIEW_ACTIONS.REVIEW_CHAIN_ADD,
                'position': const.PERSON_REVIEW_ACTIONS.ADD_POSITION_AFTER,
                'person': new_reviewer,
                'position_person': reviewers[1].person
            }
        })

    assert core_models.PersonReviewRole.objects.filter(person_review=person_review).count() == 4
    assert core_models.PersonReviewRole.objects.get(
        person_review=person_review,
        person=reviewers[2].person
    ).position == 3


def test_action_add_reviewer_same(person_review, person_review_role_builder, person_builder,
                                  test_person, review_role_builder):
    reviewers, _ = create_dummy_reviewers_chain(person_review_role_builder, person_review)
    review_admin = review_role_builder(
        person=test_person,
        review=person_review.review,
        type=const.ROLE.REVIEW.ADMIN,
    )
    new_reviewer = person_builder()
    with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
        bulk.bulk_same_action_set(test_person, [person_review.id], {
            'reviewers': {
                'type': const.PERSON_REVIEW_ACTIONS.REVIEW_CHAIN_ADD,
                'position': const.PERSON_REVIEW_ACTIONS.ADD_POSITION_SAME,
                'person': new_reviewer,
                'position_person': reviewers[1].person
            }
        })

    assert core_models.PersonReviewRole.objects.filter(person_review=person_review).count() == 6
    assert core_models.PersonReviewRole.objects.filter(
        person_review=person_review,
        position=reviewers[1].position
    ).count() == 2


@pytest.mark.parametrize('is_multi', (True, False))
def test_action_delete_reviewer(person_review, person_review_role_builder, person_builder,
                                test_person, review_role_builder, is_multi):
    reviewers, old_reviewers = create_dummy_reviewers_chain(person_review_role_builder, person_review)
    for person_review_role in reviewers[-1]:
        person_review_role.type = const.ROLE.PERSON_REVIEW.REVIEWER
        person_review_role.save()

    reviewers.append(
        person_review_role_builder(
            position=3,
            type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
            person_review=person_review
        )
    )
    old_reviewers.append(reviewers[-1].person.login)

    review_admin = review_role_builder(
        person=test_person,
        review=person_review.review,
        type=const.ROLE.REVIEW.ADMIN,
    )
    person_review.approve_level = len(old_reviewers) - 1
    person_review.save()

    assert core_models.PersonReviewRole.objects.filter(person_review=person_review).count() == 6
    assert person_review.status == const.PERSON_REVIEW_STATUS.WAIT_EVALUATION

    persons = [reviewers[-1].person]
    expected_count = 5

    if is_multi:
        persons.append(reviewers[-2][-1].person)
        expected_count = 4

    with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
        bulk.bulk_same_action_set(test_person, [person_review.id], {
            'reviewers': {
                'type': const.PERSON_REVIEW_ACTIONS.REVIEW_CHAIN_DELETE,
                'position': const.PERSON_REVIEW_ACTIONS.ADD_POSITION_SAME,
                'persons': persons,
            }
        })
    person_review.refresh_from_db()

    assert core_models.PersonReviewRole.objects.filter(person_review=person_review).count() == expected_count
    assert person_review.status == const.PERSON_REVIEW_STATUS.APPROVED
    assert person_review.approve_level == 2
    assert not core_models.PersonReviewRole.objects.filter(
        person_review=person_review,
        person__in=persons,
    ).exists()


def test_action_replace_all(person_review, person_review_role_builder, person_builder,
                            test_person, review_role_builder):
    reviewers, old_reviewers = create_dummy_reviewers_chain(person_review_role_builder, person_review)

    review_role_builder(
        person=test_person,
        review=person_review.review,
        type=const.ROLE.REVIEW.ADMIN,
    )

    person_review.approve_level = len(old_reviewers) - 1
    person_review.save()

    person_to = person_builder()

    with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay'):
        bulk.bulk_same_action_set(test_person, [person_review.id], {
            'reviewers': {
                'ids': [person_review.id],
                'type': const.PERSON_REVIEW_ACTIONS.REVIEW_CHAIN_REPLACE_ALL,
                'person_to': person_to,
            }
        })
    person_review.refresh_from_db()

    assert core_models.PersonReviewRole.objects.filter(person_review=person_review).count() == 1
    assert person_review.status == const.PERSON_REVIEW_STATUS.WAIT_EVALUATION
    assert person_review.approve_level == 0
    assert core_models.PersonReviewRole.objects.filter(
        person_review=person_review,
        person=person_to,
    ).exists()


def create_dummy_reviewers_chain(person_review_role_builder, person_review):
    reviewers = [
        [
            person_review_role_builder(
                position=0,
                type=const.ROLE.PERSON_REVIEW.REVIEWER,
                person_review=person_review
            ),
            person_review_role_builder(
                position=0,
                type=const.ROLE.PERSON_REVIEW.REVIEWER,
                person_review=person_review
            )
        ],
        person_review_role_builder(
            position=1,
            type=const.ROLE.PERSON_REVIEW.REVIEWER,
            person_review=person_review
        ),
        [
            person_review_role_builder(
                position=2,
                type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
                person_review=person_review
            ),
            person_review_role_builder(
                position=2,
                type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
                person_review=person_review
            )
        ],
    ]
    structure = [
        [reviewers[0][0].person.login, reviewers[0][1].person.login, ],
        reviewers[1].person.login,
        [reviewers[2][0].person.login, reviewers[2][1].person.login, ],

    ]

    return reviewers, structure
