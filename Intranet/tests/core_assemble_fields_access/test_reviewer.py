# coding: utf-8
from review.core.logic import assemble
from review.shortcuts import const
from tests import helpers


def test_reviewer_salary_no_access(person_review_role_reviewer):
    person_review = person_review_role_reviewer.person_review
    reviewer = person_review_role_reviewer.person
    person_review_fetched = assemble.get_person_review(
        subject=reviewer,
        id=person_review.id,
        fields_requested=(
            const.FIELDS.SALARY_VALUE,
        )
    )

    assert person_review_fetched.salary_value == const.NO_ACCESS


def test_reviewer_inherited_salary_no_access(
    case_two_reviews_for_person,
    person_review_role_builder,
):
    reviewer_role = person_review_role_builder(
        person_review=case_two_reviews_for_person.person_review_latest,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
    )
    person_review_earliest_fetched = assemble.get_person_review(
        subject=reviewer_role.person,
        id=case_two_reviews_for_person.person_review_earliest.id,
        fields_requested=(
            const.FIELDS.SALARY_VALUE,
        )
    )

    assert person_review_earliest_fetched.salary_value == const.NO_ACCESS


def test_reviewer_inherited_other_money_no_access(
    case_two_reviews_for_person,
    person_review_role_builder,
):
    reviewer_role = person_review_role_builder(
        person_review=case_two_reviews_for_person.person_review_latest,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
    )
    helpers.update_model(
        case_two_reviews_for_person.review_earliest,
        options_rsu_mode=const.REVIEW_MODE.MODE_MANUAL,
        bonus_mode=const.REVIEW_MODE.MODE_MANUAL,
        salary_change_mode=const.REVIEW_MODE.MODE_MANUAL,
    )
    helpers.update_model(
        case_two_reviews_for_person.person_review_earliest,
        options_rsu=300,
        bonus=50,
        salary_change=10,
    )
    person_review_earliest_fetched = assemble.get_person_review(
        subject=reviewer_role.person,
        id=case_two_reviews_for_person.person_review_earliest.id,
        fields_requested=(
            const.FIELDS.OPTIONS_RSU,
            const.FIELDS.SALARY_CHANGE,
            const.FIELDS.BONUS,
        )
    )

    assert person_review_earliest_fetched.salary_change == const.NO_ACCESS
    assert person_review_earliest_fetched.bonus == const.NO_ACCESS
    assert person_review_earliest_fetched.options_rsu == const.NO_ACCESS


def test_top_reviewer_salary_has_access(person_review_role_top_reviewer):
    person_review = person_review_role_top_reviewer.person_review
    top_reviewer = person_review_role_top_reviewer.person
    person_review_fetched = assemble.get_person_review(
        subject=top_reviewer,
        id=person_review.id,
        fields_requested=(
            const.FIELDS.SALARY_VALUE,
        )
    )

    assert person_review_fetched.salary_value != const.NO_ACCESS


def test_top_reviewer_inherited_salary_has_access(
    case_two_reviews_for_person,
    person_review_role_builder,
):
    top_reviewer_role = person_review_role_builder(
        person_review=case_two_reviews_for_person.person_review_latest,
        type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
    )
    person_review_fetched = assemble.get_person_review(
        subject=top_reviewer_role.person,
        id=case_two_reviews_for_person.person_review_earliest.id,
        fields_requested=(
            const.FIELDS.SALARY_VALUE,
        )
    )
    assert person_review_fetched.salary_value != const.NO_ACCESS


def test_top_reviewer_inherited_other_money_has_access(
    case_two_reviews_for_person,
    person_review_role_builder,
):
    top_reviewer_role = person_review_role_builder(
        person_review=case_two_reviews_for_person.person_review_latest,
        type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
    )
    helpers.update_model(
        case_two_reviews_for_person.review_earliest,
        options_rsu_mode=const.REVIEW_MODE.MODE_MANUAL,
        bonus_mode=const.REVIEW_MODE.MODE_MANUAL,
        salary_change_mode=const.REVIEW_MODE.MODE_MANUAL,
    )
    helpers.update_model(
        case_two_reviews_for_person.person_review_earliest,
        options_rsu=300,
        bonus=50,
        salary_change=10,
    )
    person_review_fetched = assemble.get_person_review(
        subject=top_reviewer_role.person,
        id=case_two_reviews_for_person.person_review_earliest.id,
        fields_requested=(
            const.FIELDS.SALARY_VALUE,
        )
    )
    assert person_review_fetched.salary_value != const.NO_ACCESS
