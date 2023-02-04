# coding: utf-8
from review.core.logic.assemble import fetched_obj, fetch_person_reviews
from tests import helpers


def test_related_persons_fetched(
    person,
    person_review_builder
):
    person_review_one = person_review_builder()
    person_review_two = person_review_builder()

    fetched = fetched_obj.Fetched()
    fetch_person_reviews.fetch_persons(
        person_reviews=[
            person_review_one,
            person_review_two,
        ],
        fetched=fetched,
        subject=person,
    )

    helpers.assert_ids_equal(
        first=list(fetched.persons.keys()),
        second=[
            person_review_one.person_id,
            person_review_two.person_id,
        ]
    )


def test_no_unrelated_persons(person, person_review_builder):
    fetched = fetched_obj.Fetched()
    fetch_person_reviews.fetch_persons(
        person_reviews=[
            person_review_builder(),
        ],
        fetched=fetched,
        subject=person,
    )

    assert person.id not in fetched.persons
