# coding: utf-8
from review.shortcuts import models
from django.conf import settings
from django.test import override_settings

from tests import helpers


DUMMY_PROPOSAL_ID = '5a27f29c615db107870c3c8d'


def test_staff_structure_push_success(client, person_builder):
    staff_robot = person_builder(login=settings.ROBOT_STAFF_LOGIN)
    helpers.post_json(
        client=client,
        path='/v1/staff-structure-push/',
        login=staff_robot.login,
        request={'proposal_id': DUMMY_PROPOSAL_ID},
    )

    assert models.StaffStructureChange.objects.filter(
        staff_id=DUMMY_PROPOSAL_ID
    ).exists()


def test_staff_structure_push_403(client, person_builder):
    staff_robot = person_builder()

    helpers.post_json(
        client=client,
        path='/v1/staff-structure-push/',
        login=staff_robot.login,
        request={'proposal_id': DUMMY_PROPOSAL_ID},
        expect_status=403
    )
    assert not models.StaffStructureChange.objects.filter(
        staff_id=DUMMY_PROPOSAL_ID
    ).exists()


def test_staff_structure_push_already_pushed(client, person_builder):
    staff_robot = person_builder(login=settings.ROBOT_STAFF_LOGIN)

    helpers.post_json(
        client=client,
        path='/v1/staff-structure-push/',
        login=staff_robot.login,
        request={'proposal_id': DUMMY_PROPOSAL_ID},
    )

    assert models.StaffStructureChange.objects.filter(
        staff_id=DUMMY_PROPOSAL_ID
    ).exists()

    response = helpers.post_json(
        client=client,
        path='/v1/staff-structure-push/',
        login=staff_robot.login,
        request={'proposal_id': DUMMY_PROPOSAL_ID},
    )
    assert 'exists' in response['result']
