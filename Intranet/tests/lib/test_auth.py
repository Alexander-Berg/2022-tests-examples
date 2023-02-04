# coding: utf-8
from tests import helpers


def test_allowed_anyone(client, person, settings):
    settings.ALLOWED_LOGINS = ''
    settings.ALLOWED_SLUGS = ''
    helpers.get_json(
        client=client,
        path='/frontend/const/',
        login=person.login,
    )


def test_only_allowed_logins(client, person_builder, settings):
    person_one, person_two = person_builder(), person_builder()
    settings.ALLOWED_LOGINS = person_one.login
    settings.ALLOWED_SLUGS = ''
    helpers.get_json(
        client=client,
        path='/frontend/const/',
        login=person_one.login,
    )
    helpers.get_json(
        client=client,
        path='/frontend/const/',
        login=person_two.login,
        expect_status=403,
        json_response=False,
    )


def test_only_allowed_slugs_direct_dep(client, person_builder, settings):
    person_one, person_two = person_builder(), person_builder()
    settings.ALLOWED_LOGINS = ''
    settings.ALLOWED_SLUGS = person_one.department.slug
    helpers.get_json(
        client=client,
        path='/frontend/const/',
        login=person_one.login,
    )
    helpers.get_json(
        client=client,
        path='/frontend/const/',
        login=person_two.login,
        expect_status=403,
        json_response=False,
    )


def test_only_allowed_slugs_parent_dep(
    client,
    person_builder,
    department_root,
    department_child_builder,
    settings,
):
    child_department = department_child_builder(parent=department_root)
    person_one = person_builder(department=child_department)
    person_two = person_builder()
    settings.ALLOWED_LOGINS = ''
    settings.ALLOWED_SLUGS = department_root.slug
    helpers.get_json(
        client=client,
        path='/frontend/const/',
        login=person_one.login,
    )
    helpers.get_json(
        client=client,
        path='/frontend/const/',
        login=person_two.login,
        expect_status=403,
        json_response=False,
    )
