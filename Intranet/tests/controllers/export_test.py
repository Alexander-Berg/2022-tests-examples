import pytest

import random

from staff.groups.objects import ServiceGroupCtl
from staff.lib.testing import StaffFactory, GroupFactory

from staff.umbrellas.controllers import export_umbrella_assignments, export_umbrellas
from staff.umbrellas.models import Umbrella
from staff.umbrellas.tests.factories import UmbrellaAssignmentFactory, UmbrellaFactory


@pytest.mark.django_db
def test_export_umbrella_assignments():
    person = StaffFactory()
    assignment = UmbrellaAssignmentFactory(person=person)

    expected_engagement = {
        'engaged_from': assignment.engaged_from,
        'engaged_to': assignment.engaged_to,
        'engagement': assignment.engagement,
        'goal': assignment.umbrella.issue_key,
    }

    result = export_umbrella_assignments([person.login])

    assert result == {person.login: [expected_engagement]}


@pytest.mark.django_db
def test_export_umbrellas():
    umbrella = UmbrellaFactory()
    service_group = GroupFactory(url=umbrella.value_stream.url, service_id=random.randint(1, 42342))

    expected_umbrella = {
        'goal': umbrella.issue_key,
        'goal_id': umbrella.goal_id,
        'name': umbrella.name,
        'value_stream': {
            'name': umbrella.value_stream.name,
            'name_en': umbrella.value_stream.name_en,
            'url': umbrella.value_stream.url,
            'abc_service_id': service_group.service_id,
        },
    }

    result = export_umbrellas(None, 2)

    assert result == {'result': [expected_umbrella]}


@pytest.mark.django_db
def test_export_umbrellas_with_removed_group():
    umbrella = UmbrellaFactory()
    service_group = GroupFactory(url=umbrella.value_stream.url, service_id=random.randint(1, 42342))
    ServiceGroupCtl(group=service_group).delete()

    expected_umbrella = {
        'goal': umbrella.issue_key,
        'goal_id': umbrella.goal_id,
        'name': umbrella.name,
        'value_stream': {
            'name': umbrella.value_stream.name,
            'name_en': umbrella.value_stream.name_en,
            'url': umbrella.value_stream.url,
            'abc_service_id': None,
        },
    }

    result = export_umbrellas(None, 2)

    assert result == {'result': [expected_umbrella]}


@pytest.mark.django_db
def test_export_umbrellas_batching():
    batch_size = 3
    second_page_size = random.randint(1, batch_size - 1)

    for _ in range(batch_size + second_page_size):
        GroupFactory(url=UmbrellaFactory().value_stream.url, service_id=random.randint(1, 42342))

    first_page_result = export_umbrellas(None, batch_size)
    assert 'continuation_token' in first_page_result
    assert len(first_page_result['result']) == batch_size

    second_page_result = export_umbrellas(first_page_result['continuation_token'], batch_size)
    assert 'continuation_token' not in second_page_result
    assert len(second_page_result['result']) == second_page_size

    expected_umbrellas = set(Umbrella.objects.active().values_list('issue_key', flat=True))
    returned_umbrellas = {x['goal'] for x in first_page_result['result'] + second_page_result['result']}
    assert returned_umbrellas == expected_umbrellas
