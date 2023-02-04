from review.core.logic.assemble import get_reviews
from review.core import const
from tests import helpers


def test_review_list_get(test_person, review_builder, review_role_builder, person_review_builder):
    reviews = [review_builder(status=const.REVIEW_STATUS.IN_PROGRESS) for _ in range(3)]
    review_role_builder(
        review=reviews[0],
        person=test_person,
        type=const.ROLE.REVIEW.ADMIN
    )
    review_role_builder(
        review=reviews[1],
        person=test_person,
        type=const.ROLE.REVIEW.SUPERREVIEWER
    )
    for review in reviews[:2]:
        person_review_builder(
            review=review
        )
    person_review_builder(
        person=test_person,
        review=reviews[2],
        status=const.PERSON_REVIEW_STATUS.ANNOUNCED,
    )
    review_list = get_reviews(test_person, {'status': const.REVIEW_STATUS.IN_PROGRESS})
    assert len(review_list) == 3


def test_review_list_queries(
        test_person,
        review_builder,
        review_role_builder,
        person_review_builder,
        global_role_builder,
        person_review_role_builder,
):
    reviews = [review_builder(status=const.REVIEW_STATUS.IN_PROGRESS) for _ in range(5)]
    global_role_builder(person=test_person, type=const.ROLE.GLOBAL.REVIEW_CREATOR)
    review_roles = [
        review_role_builder(
            review=review,
            person=test_person,
            type=role
        )
        for role in const.ROLE.REVIEW.ALL for review in reviews
    ]
    person_reviews = [
        person_review_builder(
            person=test_person,
            review=review
        )
        for review in reviews
    ]
    person_review_roles = [
        person_review_role_builder(
            person_review=person_review,
            person=test_person,
            type=role,
        )
        for role in const.ROLE.PERSON_REVIEW.ALL for person_review in person_reviews
    ]
    with helpers.assert_num_queries(8):
        get_reviews(test_person, {'status': const.REVIEW_STATUS.IN_PROGRESS})


def test_review_list_permissions(review_builder, person_review_builder, test_person, person_review_role_builder):
    reviews = [review_builder(status=const.REVIEW_STATUS.IN_PROGRESS) for _ in range(5)]
    person_reviews = [
        person_review_builder(
            person=test_person,
            review=review
        )
        for review in reviews
    ]
    person_review_roles = [
        person_review_role_builder(
            person_review=person_review,
            person=test_person,
            type=role,
        )
        for role in const.ROLE.PERSON_REVIEW.ALL for person_review in person_reviews
    ]
    result = get_reviews(test_person, {'status': const.REVIEW_STATUS.IN_PROGRESS})

    def check_no_permissions(review):
        assert all([v != const.OK for v in list(review.actions.values())]), review

    for review in result:
        check_no_permissions(review)
