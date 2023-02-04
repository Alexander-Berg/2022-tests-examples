# coding: utf-8
import pytest

from review.shortcuts import const, models

from tests import helpers


def test_api_remove_person_reviews(client, review_builder, person_review_builder, review_role_builder, test_person):
    reviews = [review_builder(status=const.REVIEW_STATUS.IN_PROGRESS) for _ in range(2)]
    person_reviews = [
        person_review_builder(
            person=test_person,
            review=review
        )
        for review in reviews
    ]
    review_role_builder(review=reviews[0], person=test_person, type=const.ROLE.REVIEW.ADMIN)
    result = helpers.post_json(
        client=client,
        path='/v1/reviews/{}/remove-person-reviews/'.format(reviews[0].id),
        login=test_person.login,
        request={'person_reviews': [pr.id for pr in person_reviews]},
        expect_status=200
    )
    assert 'extra_ids' in result
    assert result['extra_ids'] == [person_reviews[1].id]
    assert not models.PersonReview.objects.filter(id=person_reviews[0].id).exists()


@pytest.mark.parametrize('review_role, can_remove', [
    (const.ROLE.REVIEW.SUPERREVIEWER, True),
    (const.ROLE.REVIEW.ADMIN, True),
    (None, False),
])
def test_api_remove_person_reviews_only_expected_review_roles(
        client,
        review_builder,
        person_review_builder,
        review_role_builder,
        test_person,
        review_role,
        can_remove,
):
    review = review_builder(status=const.REVIEW_STATUS.IN_PROGRESS)
    person_review = person_review_builder(person=test_person, review=review)
    if review_role:
        review_role_builder(review=review, person=test_person, type=review_role)

    helpers.post_json(
        client=client,
        path='/v1/reviews/{}/remove-person-reviews/'.format(review.id),
        login=test_person.login,
        expect_status=200 if can_remove else 403,
        request={'person_reviews': [person_review.id]},
    )


@pytest.mark.parametrize('global_role', [
    const.ROLE.GLOBAL.REVIEW_CREATOR,
    const.ROLE.GLOBAL.CALIBRATION_CREATOR,
    const.ROLE.GLOBAL.SUPPORT,
    const.ROLE.GLOBAL.EXPORTER,
    const.ROLE.GLOBAL.ROBOT,
])
def test_api_remove_person_reviews_not_for_global_roles(
        client,
        review_builder,
        person_review_builder,
        global_role_builder,
        test_person,
        global_role,
):
    review = review_builder(status=const.REVIEW_STATUS.IN_PROGRESS)
    person_review = person_review_builder(person=test_person, review=review)
    global_role_builder(person=test_person, type=global_role)

    helpers.post_json(
        client=client,
        path='/v1/reviews/{}/remove-person-reviews/'.format(review.id),
        login=test_person.login,
        expect_status=403,
        request={'person_reviews': [person_review.id]},
    )
