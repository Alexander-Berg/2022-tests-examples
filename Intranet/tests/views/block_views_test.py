import json
import random
from datetime import datetime
from typing import Any, Dict

from django.conf import settings
from django.core.urlresolvers import reverse

from staff.budget_position.tests.utils import BudgetPositionAssignmentFactory
from staff.lib.json import dumps
from staff.lib.testing import StaffFactory, GroupFactory
from staff.umbrellas.models import UmbrellaAssignment
from staff.umbrellas.tests.factories import UmbrellaFactory


def ensure_equality(hr_partner1, hr_partner2):
    # hr_partner1 - Staff model object
    # hr_partner2 - list with attributes(fetched json)
    hr_partner1 = {
        field: getattr(hr_partner1, field) for field in hr_partner2
    }
    assert hr_partner1 == hr_partner2


def test_hr_partners_for_tamirok(create_departments_with_hrs, client, mocked_mongo):
    client.login(user=StaffFactory(login=settings.AUTH_TEST_USER).user)
    response = client.get(reverse('profile:hr_partners', kwargs={'login': 'tamirok'}))
    assert response.status_code == 200
    hr_partners = json.loads(response.content)['target']['hr_partners']
    assert len(hr_partners) == 2

    for index, hr_partner in enumerate(create_departments_with_hrs['hr_partners']):
        ensure_equality(hr_partner, hr_partners[index])


def test_hr_partners_for_guido(create_departments_with_hrs, client, mocked_mongo):
    client.login(user=StaffFactory(login=settings.AUTH_TEST_USER).user)
    response = client.get(reverse('profile:hr_partners', kwargs={'login': 'guido'}))
    assert response.status_code == 200
    hr_partners = json.loads(response.content)['target']['hr_partners']
    assert len(hr_partners) == 2
    ensure_equality(create_departments_with_hrs['guido'], hr_partners[0])


def test_hr_partners_for_david(create_departments_with_hrs, client, mocked_mongo):
    client.login(user=StaffFactory(login=settings.AUTH_TEST_USER).user)
    response = client.get(reverse('profile:hr_partners', kwargs={'login': 'david'}))
    assert response.status_code == 200
    hr_partners = json.loads(response.content)['target']['hr_partners']
    assert len(hr_partners) == 1
    ensure_equality(create_departments_with_hrs['wlame'], hr_partners[0])


def test_hr_partners_for_alex(create_departments_with_hrs, client, mocked_mongo):
    client.login(user=StaffFactory(login=settings.AUTH_TEST_USER).user)
    response = client.get(reverse('profile:hr_partners', kwargs={'login': 'alex'}))
    assert response.status_code == 200
    hr_partners = json.loads(response.content)['target']['hr_partners']
    assert len(hr_partners) == 1
    ensure_equality(create_departments_with_hrs['dmirain'], hr_partners[0])


def test_chiefs(create_departments_with_hrs, client, mocked_mongo):
    client.login(user=StaffFactory(login=settings.AUTH_TEST_USER).user)
    # check for tamirok
    response = client.get(reverse('profile:chief', kwargs={'login': 'tamirok'}))
    assert response.status_code == 200
    chief = json.loads(response.content)['target']['chief']
    ensure_equality(create_departments_with_hrs['wlame'], chief)
    # check for wlame
    response = client.get(reverse('profile:chief', kwargs={'login': 'wlame'}))
    assert response.status_code == 200
    chief = json.loads(response.content)['target']['chief']
    ensure_equality(create_departments_with_hrs['dmirain'], chief)
    # check for volozh
    # ensure that there is no chief for root department's chief
    response = client.get(reverse('profile:chief', kwargs={'login': 'volozh'}))
    assert response.status_code == 200
    assert 'chief' not in json.loads(response.content)['target']

    # check for david
    response = client.get(reverse('profile:chief', kwargs={'login': 'david'}))
    assert response.status_code == 200
    chief = json.loads(response.content)['target']['chief']
    ensure_equality(create_departments_with_hrs['volozh'], chief)


def test_departments(create_departments_with_hrs, client, mocked_mongo):
    client.login(user=StaffFactory(login=settings.AUTH_TEST_USER).user)

    response = client.get(reverse('profile:departments', kwargs={'login': 'wlame'}))

    assert response.status_code == 200
    received_deps = json.loads(response.content)['target']['departments']
    received_chain = [it.get('name') for it in received_deps]
    expected_chain = ['root', 'ch1_lvl1', 'ch1_lvl2']
    assert received_chain == expected_chain, response.content


def test_value_streams(create_departments_with_hrs, company, client, mocked_mongo):
    client.login(user=StaffFactory(login=settings.AUTH_TEST_USER).user)
    BudgetPositionAssignmentFactory(
        main_assignment=True,
        person=create_departments_with_hrs['wlame'],
        department=company.dep1,
        value_stream=company.vs_112,
        geography=company.geo_russia,
    )
    vs_tags = ','.join(f'tag-{random.randint(1, 50)}' for _ in range(random.randint(0, 3)))
    group = GroupFactory(
        url=company.vs_root.url,
        service_tags=vs_tags + ',vs',
        service_id=random.randint(1, 9999999),
    )
    umbrella_count = random.randint(0, 3)

    for _ in range(umbrella_count):
        UmbrellaFactory(value_stream=company.vs_root, intranet_status=1)
        UmbrellaFactory(value_stream=company.vs_root, intranet_status=0)

    response = client.get(reverse('profile:value_streams', kwargs={'login': 'wlame'}))

    assert response.status_code == 200
    actual_value_streams = json.loads(response.content)['target']['value_streams']
    received_chain = [it.get('name') for it in actual_value_streams]
    expected_chain = ['Вэлью Стримз', 'Вэлью Стримз 1', 'Вэлью Стримз 11', 'Вэлью Стримз 112']
    assert received_chain == expected_chain, response.content
    assert actual_value_streams[0]['abc_service_id'] == group.service_id
    assert set(actual_value_streams[0]['service_tags']) == set(group.service_tags.split(',')) - {''}


def test_value_stream_chief(create_departments_with_hrs, company, client, mocked_mongo):
    client.login(user=StaffFactory(login=settings.AUTH_TEST_USER).user)
    BudgetPositionAssignmentFactory(
        main_assignment=True,
        person=create_departments_with_hrs['wlame'],
        department=company.dep1,
        value_stream=company.vs_112,
        geography=company.geo_russia,
    )

    response = client.get(reverse('profile:value_stream_chief', kwargs={'login': 'wlame'}))

    assert response.status_code == 200
    actual_value_stream_chief = json.loads(response.content)['target']['value_stream_chief']
    assert actual_value_stream_chief['login'] == getattr(company, 'vs112-chief').login, response.content


def test_umbrellas(create_departments_with_hrs, company, client, mocked_mongo):
    client.login(user=StaffFactory(login=settings.AUTH_TEST_USER).user)
    BudgetPositionAssignmentFactory(
        main_assignment=True,
        person=create_departments_with_hrs['wlame'],
        department=company.dep1,
        value_stream=company.vs_root,
        geography=company.geo_russia,
    )
    umbrella = UmbrellaFactory(value_stream=company.vs_root)
    assignment = UmbrellaAssignment.objects.create(
        person=create_departments_with_hrs['wlame'],
        umbrella=umbrella,
        engagement=random.random(),
        engaged_from=datetime.now(),
    )

    response = client.get(reverse('profile:umbrellas', kwargs={'login': 'wlame'}))

    assert response.status_code == 200, response.content
    actual_umbrellas = json.loads(response.content)['target']['umbrellas']
    expected_assignment = _format_umbrella_assignment(assignment)
    assert actual_umbrellas == [expected_assignment], response.content


def test_umbrellas_wildcard_assignment(create_departments_with_hrs, company, client, mocked_mongo):
    client.login(user=StaffFactory(login=settings.AUTH_TEST_USER).user)
    BudgetPositionAssignmentFactory(
        main_assignment=True,
        person=create_departments_with_hrs['wlame'],
        department=company.dep1,
        value_stream=company.vs_root,
        geography=company.geo_russia,
    )
    umbrella = UmbrellaFactory(value_stream=company.vs_root)
    assignment = UmbrellaAssignment.objects.create(
        person=create_departments_with_hrs['wlame'],
        umbrella=umbrella,
        engagement=random.random(),
        engaged_from=datetime.now(),
    )

    response = client.get(reverse('profile:umbrellas', kwargs={'login': 'wlame'}))

    assert response.status_code == 200, response.content
    actual_umbrellas = json.loads(response.content)['target']['umbrellas']
    expected_assignment = _format_umbrella_assignment(assignment)
    assert actual_umbrellas == [expected_assignment], response.content


def test_umbrellas_on_parent_value_stream(create_departments_with_hrs, company, client, mocked_mongo):
    client.login(user=StaffFactory(login=settings.AUTH_TEST_USER).user)
    BudgetPositionAssignmentFactory(
        main_assignment=True,
        person=create_departments_with_hrs['wlame'],
        department=company.dep1,
        value_stream=company.vs_112,
        geography=company.geo_russia,
    )
    UmbrellaFactory(value_stream=company.vs_root)
    assignment = UmbrellaAssignment.objects.create(
        person=create_departments_with_hrs['wlame'],
        umbrella=None,
        engagement=random.random(),
        engaged_from=datetime.now(),
    )

    response = client.get(reverse('profile:umbrellas', kwargs={'login': 'wlame'}))

    assert response.status_code == 200, response.content
    actual_umbrellas = json.loads(response.content)['target']['umbrellas']
    expected_assignment = _format_umbrella_assignment(assignment)
    assert actual_umbrellas == [expected_assignment], response.content


def _format_umbrella_assignment(assignment: UmbrellaAssignment) -> Dict[str, Any]:
    assignment.refresh_from_db()
    raw_dict = {
        'engaged_from': assignment.engaged_from,
        'engaged_to': assignment.engaged_to,
        'engagement': assignment.engagement,
        'umbrella': None,
    }

    if assignment.umbrella:
        raw_dict['umbrella'] = {
            'id': assignment.umbrella.id,
            'issue_key': assignment.umbrella.issue_key,
            'name': assignment.umbrella.name,
            'goal_id': assignment.umbrella.goal_id,
        }

    return json.loads(dumps(raw_dict))


def test_curators(create_departments_with_hrs, client, mocked_mongo):
    client.login(user=StaffFactory(login=settings.AUTH_TEST_USER).user)

    response = client.get(reverse('profile:curators', kwargs={'login': 'dmirain'}))
    assert response.status_code == 200
    curators = json.loads(response.content)['target']['curators']
    assert len(curators) == 1
    assert curators[0] == {
        'curator': {'first_name': 'Arcadii', 'last_name': 'Volozh', 'login': 'volozh'},
        'department': {'name': 'ch1_lvl1', 'url': 'ch1_lvl11'},
        'role': 'CURATOR_BU',
    }

    response = client.get(reverse('profile:curators', kwargs={'login': 'wlame'}))
    assert response.status_code == 200
    curators = json.loads(response.content)['target']['curators']
    assert not curators
