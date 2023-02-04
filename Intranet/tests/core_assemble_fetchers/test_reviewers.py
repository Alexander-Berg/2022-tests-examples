# coding: utf-8
from review.core import const
from review.core.logic.assemble import fetched_obj, fetch_person_reviews

from tests import helpers


def test_fetch_one_reviewer(
    person,
    person_review,
    person_review_role_builder,
):
    fetched = fetched_obj.Fetched()
    reviewer_role_one = person_review_role_builder(
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
    )

    fetch_person_reviews.fetch_reviewers(
        subject=person,
        person_reviews=[person_review],
        fetched=fetched,
    )

    expected_fuzzy = [
        {'login': reviewer_role_one.person.login},
    ]
    helpers.assert_is_substructure(
        expected_fuzzy,
        fetched.reviewers[person_review.id]
    )


def test_fetch_two_reviewers_in_order(
    person,
    person_review,
    person_review_role_builder,

):
    fetched = fetched_obj.Fetched()
    reviewer_role_one = person_review_role_builder(
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
        position=0,
    )
    reviewer_role_two = person_review_role_builder(
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
        position=1,
    )

    fetch_person_reviews.fetch_reviewers(
        subject=person,
        person_reviews=[person_review],
        fetched=fetched,
    )

    expected_fuzzy = [
        {'login': reviewer_role_one.person.login},
        {'login': reviewer_role_two.person.login},
    ]
    helpers.assert_is_substructure(
        expected_fuzzy,
        fetched.reviewers[person_review.id]
    )


def test_fetch_two_reviewers_in_same_place(
    person,
    person_review,
    person_review_role_builder,
):
    fetched = fetched_obj.Fetched()
    reviewer_role_one_first = person_review_role_builder(
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
        position=0,
    )
    reviewer_role_two_second = person_review_role_builder(
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
        position=0,
    )

    fetch_person_reviews.fetch_reviewers(
        subject=person,
        person_reviews=[person_review],
        fetched=fetched,
    )

    expected_fuzzy = [
        [
            {'login': reviewer_role_one_first.person.login},
            {'login': reviewer_role_two_second.person.login},
        ]
    ]
    helpers.assert_is_substructure(
        expected_fuzzy,
        fetched.reviewers[person_review.id]
    )


def test_fetch_two_reviewers_in_same_place_and_one_before_and_after(
    person,
    person_review,
    person_review_role_builder,
):
    fetched = fetched_obj.Fetched()
    reviewer_role_one = person_review_role_builder(
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
        position=0,
    )
    reviewer_role_two_first = person_review_role_builder(
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
        position=1
    )
    reviewer_role_two_second = person_review_role_builder(
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
        position=1,
    )
    reviewer_role_three = person_review_role_builder(
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
        position=2,
    )

    fetch_person_reviews.fetch_reviewers(
        subject=person,
        person_reviews=[person_review],
        fetched=fetched,
    )

    expected_fuzzy = [
        {'login': reviewer_role_one.person.login},
        [
            {'login': reviewer_role_two_first.person.login},
            {'login': reviewer_role_two_second.person.login},
        ],
        {'login': reviewer_role_three.person.login},
    ]
    helpers.assert_is_substructure(
        expected_fuzzy,
        fetched.reviewers[person_review.id]
    )


def test_fetch_two_reviewers_at_beginning_and_by_one_after(
    person,
    person_review,
    person_review_role_builder,
):
    fetched = fetched_obj.Fetched()
    reviewer_role_one_first = person_review_role_builder(
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
        position=0,
    )
    reviewer_role_one_second = person_review_role_builder(
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
        position=0
    )
    reviewer_role_two = person_review_role_builder(
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
        position=1,
    )
    reviewer_role_three = person_review_role_builder(
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
        position=2,
    )
    reviewer_role_four = person_review_role_builder(
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
        position=3,
    )
    reviewer_role_five = person_review_role_builder(
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
        position=4,
    )

    fetch_person_reviews.fetch_reviewers(
        subject=person,
        person_reviews=[person_review],
        fetched=fetched,
    )

    expected_fuzzy = [
        [
            {'login': reviewer_role_one_first.person.login},
            {'login': reviewer_role_one_second.person.login},
        ],
        {'login': reviewer_role_two.person.login},
        {'login': reviewer_role_three.person.login},
        {'login': reviewer_role_four.person.login},
        {'login': reviewer_role_five.person.login},
    ]
    helpers.assert_is_substructure(
        expected_fuzzy,
        fetched.reviewers[person_review.id]
    )
