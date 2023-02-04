# coding: utf-8
from review.core.logic.assemble import fetched_obj, fetch_person_reviews
from tests import helpers


def test_related_person_reviews_fetched(person, person_review_builder):
    person_review_one = person_review_builder(person=person)
    person_review_two = person_review_builder(person=person)

    fetched = fetched_obj.Fetched()
    fetch_person_reviews.fetch_person_reviews_history(
        person_reviews=[
            person_review_one,
            person_review_two,
        ],
        fetched=fetched,
    )

    helpers.assert_ids_equal(
        first=list(fetched.person_review_history.keys()),
        second=[person.id]
    )


def test_no_unrelated_reviews(person_builder, person_review_builder):
    person_unrelated = person_builder()
    person_review = person_review_builder()
    fetched = fetched_obj.Fetched()

    fetch_person_reviews.fetch_person_reviews_history(
        person_reviews=[
            person_review,
        ],
        fetched=fetched,
    )

    assert person_unrelated.id not in fetched.person_review_history
