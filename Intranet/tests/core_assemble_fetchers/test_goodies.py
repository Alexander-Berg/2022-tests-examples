# coding: utf-8
from review.core.logic.assemble import fetched_obj, fetch_person_reviews


def test_related_goodies_fetched(review_goodie, person_review_builder):
    review = review_goodie.review
    person_review_one = person_review_builder(review=review)
    person_review_two = person_review_builder(review=review)

    fetched = fetched_obj.Fetched()
    fetch_person_reviews.fetch_goodies(
        person_reviews=[
            person_review_one,
            person_review_two,
        ],
        fetched=fetched,
    )

    assert list(fetched.goodies.values())[0] == {
        'options_rsu': review_goodie.options_rsu,
        'bonus': review_goodie.bonus,
        'salary_change': review_goodie.salary_change,
    }


def test_no_unrelated_goodies(review_goodie, person_review_builder):
    fetched = fetched_obj.Fetched()

    fetch_person_reviews.fetch_goodies(
        person_reviews=[
            person_review_builder(),
        ],
        fetched=fetched,
    )

    assert not fetched.goodies
