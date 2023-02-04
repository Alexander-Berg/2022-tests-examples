from review.core import const

from tests import helpers


def test_review_person_reviews_for_admin(
    client,
    review,
    person_review_builder,
    review_role_builder,
    test_person
):
    [person_review_builder(review=review) for _ in range(3)]
    review_admin_role = review_role_builder(
        review=review,
        type=const.ROLE.REVIEW.ADMIN,
        person=test_person,
    )

    response = helpers.get_json(
        client,
        login=review_admin_role.person.login,
        path='/frontend/reviews/{}/person-reviews/'.format(review.id),
    )
    result = response["person_reviews"]
    assert len(result) == 3


def test_review_person_reviews_filter(
    client,
    review,
    person_review_builder,
    review_role_builder,
    test_person
):
    prs = [person_review_builder(review=review) for _ in range(3)]
    person_to_search = prs[0].person.login
    review_admin_role = review_role_builder(
        review=review,
        type=const.ROLE.REVIEW.ADMIN,
        person=test_person,
    )

    response = helpers.get_json(
        client,
        login=review_admin_role.person.login,
        path='/frontend/reviews/{}/person-reviews/?persons={}'.format(
            review.id,
            person_to_search,
        ),
    )
    result = response["person_reviews"]
    assert len(result) == 1
    assert result[0]['person']['login'] == person_to_search


def test_review_person_reviews_not_for_admin(
        client,
        review,
        test_person
):
    response = helpers.get_json(
        client,
        login=test_person.login,
        path='/frontend/reviews/{}/person-reviews/'.format(review.id)
    )
    result = response["person_reviews"]
    assert len(result) == 0
