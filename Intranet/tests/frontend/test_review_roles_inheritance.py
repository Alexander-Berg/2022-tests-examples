# coding: utf-8
from review.shortcuts import const
from review.shortcuts import models
from tests import helpers


def test_superreviewer_denormalized_when_just_added(
    client,
    person_review,
    person,
    review_role_builder,
):
    review = person_review.review
    admin_role = review_role_builder(
        review=review,
        type=const.ROLE.REVIEW.ADMIN,
    )
    review_params = helpers.get_json(
        client,
        login=admin_role.person.login,
        path='/frontend/reviews/{}/'.format(review.id),
    )
    review_params.update(
        super_reviewers=[person.login],
        admins=[admin_role.person.login]
    )
    for param, value in list(review_params.items()):
        if value is None:
            review_params.pop(param)

    helpers.post_multipart_data(
        client=client,
        login=admin_role.person.login,
        path='/frontend/reviews/{}/'.format(review.id),
        request=review_params,
    )

    superreviewer_role_params = dict(
        person=person,
        review=review,
        type=const.ROLE.REVIEW.SUPERREVIEWER,
    )
    assert models.ReviewRole.objects.filter(**superreviewer_role_params).exists()
    superreviewer_role = models.ReviewRole.objects.get(**superreviewer_role_params)
    assert models.PersonReviewRole.objects.filter(
        person=person,
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.SUPERREVIEWER_DENORMALIZED,
        from_review_role=superreviewer_role,
    )
