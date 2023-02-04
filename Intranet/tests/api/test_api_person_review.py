# coding: utf-8
import pytest

from review.shortcuts import const

from tests import helpers


def test_api_person_review_response(client, person_review):
    goldstar = const.GOLDSTAR.OPTION_AND_BONUS
    helpers.update_model(
        person_review.review,
        goldstar_mode=const.REVIEW_MODE.MODE_MANUAL,
    )
    helpers.update_model(
        person_review,
        status=const.PERSON_REVIEW_STATUS.ANNOUNCED,
        mark='B',
        goldstar=goldstar,
        tag_average_mark='fusrodah',
    )

    response = helpers.get_json(
        client=client,
        path='/v1/person-reviews/',
        login=person_review.person.login,
    )

    helpers.assert_is_substructure(
        {
            'person_reviews': [
                {
                    'id': person_review.id,
                    'mark': 'B',
                    'goldstar': const.GOLDSTAR.VERBOSE[goldstar],
                    'review': {
                        'id': person_review.review_id,
                        'start_date': person_review.review.start_date.isoformat(),
                        'finish_date': person_review.review.finish_date.isoformat(),
                        'type': const.REVIEW_TYPE.VERBOSE[person_review.review.type],
                    },
                    'status': const.PERSON_REVIEW_STATUS.VERBOSE[const.PERSON_REVIEW_STATUS.ANNOUNCED],
                    'person': {
                        'login': person_review.person.login,
                    },
                    'tag_average_mark': 'fusrodah',
                }
            ]
        },
        response,
    )


@pytest.mark.parametrize('role, expected', [
    (const.ROLE.PERSON.SELF, True,),
    (const.ROLE.GLOBAL.EXPORTER, True),
    (const.ROLE.DEPARTMENT.HEAD, True),
    (const.ROLE.DEPARTMENT.HR_PARTNER, True),
    (const.ROLE.DEPARTMENT.HR_ANALYST, True),
    (const.ROLE.PERSON_REVIEW.REVIEWER, False),
    (const.ROLE.PERSON_REVIEW.TOP_REVIEWER, False),
])
def test_api_person_reviews_only_expected_roles(
    client,
    case_reviewers_dep_roles,
    role,
    expected
):
    case = case_reviewers_dep_roles
    person = case.person_review.person
    person_review = case.person_review

    helpers.update_model(
        person_review,
        status=const.PERSON_REVIEW_STATUS.ANNOUNCED,
        mark='B',
    )

    person = {
        const.ROLE.PERSON.SELF: person,
        const.ROLE.GLOBAL.EXPORTER: case.exporter,
        const.ROLE.DEPARTMENT.HEAD: case.head,
        const.ROLE.DEPARTMENT.HR_PARTNER: case.hr_partner,
        const.ROLE.DEPARTMENT.HR_ANALYST: case.hr_analyst,
        const.ROLE.PERSON_REVIEW.REVIEWER: case.reviewer,
        const.ROLE.PERSON_REVIEW.TOP_REVIEWER: case.top_reviewer,
    }[role]

    response = helpers.get_json(
        client=client,
        path='/v1/person-reviews/',
        login=person.login,
    )

    person_reviews = response['person_reviews']
    if expected:
        assert len(person_reviews) == 1
    else:
        assert len(person_reviews) == 0
