# coding: utf-8
from review.core.logic.assemble import fetched_obj, fetch_person_reviews
from tests import helpers


def test_related_reviews_fetched(person_review_builder):
    person_review_one = person_review_builder()
    person_review_two = person_review_builder()

    fetched = fetched_obj.Fetched()
    fetch_person_reviews.fetch_reviews(
        person_reviews=[
            person_review_one,
            person_review_two,
        ],
        fetched=fetched,
    )

    helpers.assert_ids_equal(
        first=list(fetched.reviews.keys()),
        second=[
            person_review_one.review_id,
            person_review_two.review_id,
        ]
    )


def test_no_unrelated_reviews(review, person_review_builder):
    fetched = fetched_obj.Fetched()
    fetch_person_reviews.fetch_reviews(
        person_reviews=[
            person_review_builder(),
        ],
        fetched=fetched,
    )

    assert review.id not in fetched.reviews
