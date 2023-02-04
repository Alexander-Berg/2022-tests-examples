# coding: utf-8
from review.core.logic.assemble import collect_reviews


def test_review_unavailable_for_no_roles(person):
    collected = collect_reviews.get_allowed_reviews_flat(subject=person)

    assert not collected


def test_review_available_for_reviewer(
    person_review_role_reviewer
):
    person = person_review_role_reviewer.person
    review = person_review_role_reviewer.person_review.review

    collected = collect_reviews.get_allowed_reviews_flat(subject=person)

    assert review.id in [
        collected_review.id for collected_review in collected
    ]
