import json

from review.core import models as core_models
from review.core import const as core_const

from tests.helpers import post_multipart_data, get_json


GLOBAL_ROLES = core_const.ROLE.GLOBAL


def test_api_idm_role_add(client, person_builder):
    person = person_builder()

    post_multipart_data(client, '/idm/add-role/', request={
        'login': person.login,
        'role': json.dumps({
            'role': GLOBAL_ROLES.VERBOSE[GLOBAL_ROLES.REVIEW_CREATOR]
        })
    })

    assert core_models.GlobalRole.objects.filter(
        person=person,
        type=GLOBAL_ROLES.REVIEW_CREATOR
    ).exists()


def test_api_idm_role_get_all(client, person_builder):
    person = person_builder()
    core_models.GlobalRole.objects.create(
        person=person,
        type=GLOBAL_ROLES.REVIEW_CREATOR,
    )

    result = get_json(client, '/idm/get-all-roles/')['users']

    assert result == [{
        'login': person.login,
        'roles': [
            GLOBAL_ROLES.VERBOSE[GLOBAL_ROLES.REVIEW_CREATOR],
        ]
    }]


def test_api_idm_role_remove(client, person_builder):
    person = person_builder()
    core_models.GlobalRole.objects.create(
        person=person,
        type=GLOBAL_ROLES.REVIEW_CREATOR,
    )

    post_multipart_data(
        client=client,
        path='/idm/remove-role/',
        request={
            'login': person.login,
            'role': json.dumps({
                'role': GLOBAL_ROLES.VERBOSE[GLOBAL_ROLES.REVIEW_CREATOR],
            })
        })

    assert not core_models.GlobalRole.objects.filter(
        person=person,
        type=GLOBAL_ROLES.REVIEW_CREATOR,
    ).exists()


def test_api_idm_get_role(client, person_builder):
    persons = [person_builder() for _ in range(15)]
    roles = [
        core_models.GlobalRole(
            person=person,
            type=GLOBAL_ROLES.SUPPORT,
        ) for person in persons
    ]
    core_models.GlobalRole.objects.bulk_create(roles)

    result_roles = []
    path = '/idm/get-roles/?per_page=10'
    while path is not None:
        result = get_json(client, path)
        result_roles += result['roles']
        path = result.get('next-url')
    assert len(result_roles) == len(persons)
