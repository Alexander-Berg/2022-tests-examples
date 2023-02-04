from waffle import set_flag
from review.core import const
from review.shortcuts import const as const_sh
from tests import helpers
import pytest
from .test_person_review_filters import reviewing_subordination_structure

URL = '/frontend/user/'


def test_has_add_to_calibration(
    client,
    calibration,
    calibration_role_builder,
):
    admin = calibration_role_builder(
        type=const.ROLE.CALIBRATION.ADMIN,
        calibration=calibration,
    ).person
    response = helpers.get_json(
        client,
        path=URL,
        login=admin.login,
    )
    assert response['actions']['add_person_reviews_to_calibration'] == const.OK


@pytest.mark.parametrize(
    'expected', (True, False)
)
def test_user_is_chief(
    client,
    reviewing_subordination_structure,
    expected,
):
    person = reviewing_subordination_structure['child_empl']

    if expected:
        person = reviewing_subordination_structure['root_chief']

    response = helpers.get_json(
        client,
        path=URL,
        login=person.login,
    )
    assert response['is_chief'] == expected


@pytest.mark.parametrize(
    'role, field', [(const_sh.STAFF_ROLE.HR.HR_PARTNER, 'is_hrbp'),
                    (const_sh.STAFF_ROLE.HR.HR_ANALYST, 'is_hranalyst')]
)
@pytest.mark.parametrize('expected', (True, False))
def test_user_is_hrb_or_analyst(
    client,
    role,
    field,
    hr_builder,
    person_builder,
    expected,
):

    person = person_builder()

    if expected:
        hr_builder(
            cared_person=person_builder(),
            hr_person=person,
            type=role,
        )

    response = helpers.get_json(
        client,
        path=URL,
        login=person.login,
    )
    assert response[field] == expected


def test_check_marks_format_mode(
    client,
    person,
):
    response = helpers.get_json(
        client,
        path=URL,
        login=person.login,
    )
    assert response['marks_format_mode'] == 1


def test_has_no_add_to_calibration_empty_user(
    client,
    calibration,
    calibration_role_builder,
    person_builder,
):
    calibration_role_builder(
        type=const.ROLE.CALIBRATION.ADMIN,
        calibration=calibration,
    )
    response = helpers.get_json(
        client,
        path=URL,
        login=person_builder().login,
    )
    assert response['actions']['add_person_reviews_to_calibration'] == const.NO_ACCESS


def test_has_no_add_to_calibration_archived_calibrations(
    client,
    calibration_builder,
    calibration_role_builder,
):
    calibration = calibration_builder(status=const.CALIBRATION_STATUS.ARCHIVE)
    admin = calibration_role_builder(
        type=const.ROLE.CALIBRATION.ADMIN,
        calibration=calibration,
    ).person
    response = helpers.get_json(
        client,
        path=URL,
        login=admin.login,
    )
    assert response['actions']['add_person_reviews_to_calibration'] == const.NO_ACCESS


def test_flags_enabled_for_user(client, person_builder, waffle_flag_builder):
    person = person_builder()
    flag_name = 'new_person_review_card'
    waffle_flag_builder(flag_name, users=[person])

    response = helpers.get_json(
        client,
        path=URL,
        login=person.login,
    )
    assert response['flags'] == {flag_name: True}


def test_flags_disabled_for_user(client, person_builder, waffle_flag_builder):
    person = person_builder()
    flag_name = 'new_person_review_card'
    waffle_flag_builder(flag_name, users=[])

    response = helpers.get_json(
        client,
        path=URL,
        login=person.login,
    )
    assert response['flags'] == {flag_name: False}


def test_no_flags(client, person_builder):
    person = person_builder()

    response = helpers.get_json(
        client,
        path=URL,
        login=person.login,
    )
    assert response['flags'] == {}
