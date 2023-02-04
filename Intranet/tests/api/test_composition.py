import pytest
import pretend

from fastapi import status
from unittest.mock import patch

from watcher.db import Composition


@pytest.fixture
def composition_data(service_factory, role_factory, scope_factory, staff_factory, member_factory):
    service = service_factory()

    scope = scope_factory()
    role = role_factory(scope=scope)
    scope_1 = scope_factory()
    role_1 = role_factory()

    staff = staff_factory()

    for _ in range(3):
        member_factory(
            role=role,
            service=service,
        )

    member_factory(role=role_1, staff=staff, service=service)
    member_factory(role=role_1)

    return pretend.stub(
        service=service,
        staff=staff,
        role=role,
        scope=scope,
        role_1=role_1,
        scope_1=scope_1,
    )


@pytest.mark.parametrize(
    'data', [
        {'roles': []},
        {'staff': []},
        {'full_service': False},
        {'scopes': []},
        {},
    ]
)
def test_create_composition_with_empty_fields(client, composition_data, data):
    initial_data = {
        'slug': 'composition_slug',
        'name': 'test composition name',
        'service_id': composition_data.service.id,
    }
    initial_data.update(data)

    with patch('watcher.api.routes.composition.update_composition'):
        response = client.post(
            '/api/watcher/v1/composition/',
            json=initial_data,
        )
    assert response.status_code == status.HTTP_400_BAD_REQUEST, response.text
    assert response.json()['error'] == 'invalid_composition_input'


def test_create_composition(client, composition_data):
    initial_data = {
        'slug': 'composition_slug',
        'name': 'test composition name',
        'service_id': composition_data.service.id,
        'roles': [composition_data.role.id, composition_data.role_1.id],
        'scopes': [composition_data.scope.id],
        'staff': [composition_data.staff.id],
        'excluded_roles': [composition_data.role_1.id]
    }
    response = client.post(
        '/api/watcher/v1/composition/',
        json=initial_data,
    )
    assert response.status_code == status.HTTP_201_CREATED, response.text
    data = response.json()

    expected = {
        'name': 'test composition name',
        'slug': 'composition_slug',
        'service_id': composition_data.service.id,
        'full_service': False,
        'excluded_scopes':  [],
        'excluded_staff': [],
        'autoupdate': True
    }
    for key, value in expected.items():
        assert data[key] == value
    assert 'id' in data

    roles = data['roles']
    assert len(roles) == 2
    assert set(
        role['name'] for role in roles
    ) == {
        composition_data.role.name,
        composition_data.role_1.name,
    }

    scopes = data['scopes']
    assert len(scopes) == 1
    assert scopes[0]['slug'] == composition_data.scope.slug

    staff = data['staff']
    assert len(staff) == 1
    assert staff[0]['login'] == composition_data.staff.login

    excluded_roles = data['excluded_roles']
    assert len(excluded_roles) == 1
    assert excluded_roles[0]['code'] == composition_data.role_1.code

    participants = data['participants']
    assert len(participants) == 3


@pytest.mark.parametrize(('same_service', 'expected_code'), [(True, 400), (False, 201)])
def test_create_composition_unique_slug(
    client, composition_data, composition_factory,
    service_factory, same_service, expected_code,
):
    slug = 'test'
    initial_data = {
        'slug': slug,
        'name': 'test composition name',
        'service_id': composition_data.service.id,
        'staff': [composition_data.staff.id],
    }
    composition_factory(slug=slug, service=composition_data.service if same_service else service_factory())
    response = client.post(
        '/api/watcher/v1/composition/',
        json=initial_data,
    )
    assert response.status_code == expected_code, response.text
    if same_service:
        assert response.json()['detail'][0]['msg'] == {
            'ru': 'Slug пула должен быть уникальным',
            'en': 'Pool slug must be unique',
        }


def test_update_refs(client, composition_factory, role_factory, scope_session):
    role_1 = role_factory()
    role_2 = role_factory()
    role_3 = role_factory()
    composition = composition_factory(
        roles=[role_1, role_2],
        excluded_roles=[role_3],
    )

    patch_data = {'roles': [role_2.id, role_3.id], 'excluded_roles': []}

    with patch('watcher.api.routes.composition.update_composition') as update_composition_mock:
        response = client.patch(
            f'/api/watcher/v1/composition/{composition.id}',
            json=patch_data,
        )

    assert response.status_code == status.HTTP_200_OK, response.text
    update_composition_mock.assert_called_once()
    data = response.json()
    assert data['excluded_roles'] == []
    assert {role['code'] for role in data['roles']} == {role_2.code, role_3.code}

    scope_session.refresh(composition)
    assert len(composition.excluded_roles) == 0
    assert {role.code for role in composition.roles} == {role_2.code, role_3.code}


def test_update_composition(client, composition_factory):
    composition = composition_factory()
    patch_data = {'name': 'somenew', 'full_service': True}
    with patch('watcher.api.routes.composition.update_composition') as update_composition_mock:
        response = client.patch(
            f'/api/watcher/v1/composition/{composition.id}',
            json=patch_data,
        )
    assert response.status_code == status.HTTP_200_OK, response.text
    update_composition_mock.assert_called_once()
    data = response.json()
    assert data['name'] == 'somenew'


def test_update_composition_full_service(client, composition_factory, composition_data, role_factory, scope_session):
    """
    Проверяем что если передан full_service - role/scopes/staff существующие затираются, а
    excluded нет
    """
    composition = composition_factory(service=composition_data.service)
    new_role = role_factory()
    composition.roles.append(new_role)
    scope_session.add(composition)
    scope_session.commit()

    patch_data = {
        'full_service': True,
        'excluded_roles': [new_role.id],
    }
    with patch('watcher.api.routes.composition.update_composition') as update_composition_mock:
        response = client.patch(
            f'/api/watcher/v1/composition/{composition.id}',
            json=patch_data,
        )
    assert response.status_code == status.HTTP_200_OK, response.text
    update_composition_mock.assert_called_once()
    data = response.json()
    assert data['full_service']
    assert data['roles'] == []

    scope_session.refresh(composition)

    assert not composition.roles
    assert not composition.scopes
    assert not composition.staff
    assert len(composition.excluded_roles) == 1


def test_update_composition_full_service_fail(client, composition_factory, composition_data, role_factory, scope_session):
    composition = composition_factory(service=composition_data.service)
    new_role = role_factory()
    composition.roles.append(new_role)
    scope_session.add(composition)
    scope_session.commit()

    patch_data = {
        'full_service': True,
        'roles': [composition_data.role.id],
        'scopes': [composition_data.scope.id],
        'staff': [composition_data.staff.id],
        'excluded_roles': [new_role.id],
    }
    response = client.patch(
        f'/api/watcher/v1/composition/{composition.id}',
        json=patch_data,
    )
    assert response.status_code == status.HTTP_400_BAD_REQUEST, response.text


def test_update_composition_full_service_on_object_fail(client, composition_factory, composition_data, role_factory, scope_session):
    composition = composition_factory(service=composition_data.service, full_service=True)

    patch_data = {
        'roles': [composition_data.role.id],
        'scopes': [composition_data.scope.id],
        'staff': [composition_data.staff.id],
    }
    response = client.patch(
        f'/api/watcher/v1/composition/{composition.id}',
        json=patch_data,
    )
    assert response.status_code == status.HTTP_400_BAD_REQUEST, response.text


def test_update_composition_full_service_on_object_success(client, composition_factory, composition_data, role_factory, scope_session):
    composition = composition_factory(service=composition_data.service, full_service=True)

    patch_data = {
        'full_service': False,
        'roles': [composition_data.role.id],
        'scopes': [composition_data.scope.id],
        'staff': [composition_data.staff.id],
    }
    response = client.patch(
        f'/api/watcher/v1/composition/{composition.id}',
        json=patch_data,
    )
    assert response.status_code == status.HTTP_200_OK, response.text

    data = response.json()
    assert not data['full_service']
    participants = data['participants']
    assert len(participants) == 4

    scope_session.refresh(composition)

    assert not composition.full_service
    assert {role.id for role in composition.roles} == {composition_data.role.id}
    assert {scopes.id for scopes in composition.scopes} == {composition_data.scope.id}
    assert {staff.id for staff in composition.staff} == {composition_data.staff.id}


def test_update_slug_not_success(client, composition_factory):
    composition = composition_factory()
    patch_data = {'slug': 'somenew'}
    with patch('watcher.api.routes.composition.update_composition') as update_composition_mock:
        response = client.patch(
            f'/api/watcher/v1/composition/{composition.id}',
            json=patch_data,
        )
    assert response.status_code == status.HTTP_422_UNPROCESSABLE_ENTITY, response.text
    update_composition_mock.assert_not_called()


def test_update_staff_not_success(client, composition_factory, composition_data):
    composition = composition_factory()
    patch_data = {'staff': [composition_data.staff.id]}

    response = client.patch(
        f'/api/watcher/v1/composition/{composition.id}',
        json=patch_data,
    )
    assert response.status_code == status.HTTP_400_BAD_REQUEST, response.text


def test_update_role_not_success(client, composition_factory, composition_data, role_factory):
    composition = composition_factory()
    role = role_factory(service=composition_data.service)
    patch_data = {'roles': [role.id]}

    response = client.patch(
        f'/api/watcher/v1/composition/{composition.id}',
        json=patch_data,
    )
    assert response.status_code == status.HTTP_400_BAD_REQUEST, response.text


def test_delete_composition(client, composition_factory, scope_session):
    composition = composition_factory()

    response = client.delete(
        f'/api/watcher/v1/composition/{composition.id}'
    )
    assert response.status_code == status.HTTP_204_NO_CONTENT, response.text
    assert not scope_session.query(Composition).filter(
        Composition.id==composition.id,
    ).count()


def test_delete_composition_failed(
    client, schedule_factory, composition_factory, slot_factory,
    interval_factory, revision_factory, scope_session
):
    schedule = schedule_factory()
    composition = composition_factory(service=schedule.service)
    interval = interval_factory(schedule=schedule)
    revision = revision_factory(schedule=schedule, state='active')
    slot_factory(composition=composition, interval=interval)
    interval.revision = revision
    scope_session.add(interval)
    scope_session.commit()

    response = client.delete(
        f'/api/watcher/v1/composition/{composition.id}'
    )

    assert response.status_code == status.HTTP_400_BAD_REQUEST, response.text
    assert response.json()['error'] == 'bad_request'


def test_get_composition(client, composition_factory, staff_factory, scope_session):
    composition = composition_factory()
    staff = staff_factory()
    composition.participants.append(staff)

    scope_session.add(composition)
    scope_session.commit()

    response = client.get(
        f'/api/watcher/v1/composition/{composition.id}'
    )
    assert response.status_code == status.HTTP_200_OK, response.text
    data = response.json()

    assert data['id'] == composition.id
    assert data['slug'] == composition.slug
    assert data['roles'] == []
    assert len(data['participants']) == 1
    assert data['participants'][0]['uid'] == int(staff.uid)


def test_get_composition_with_schedules(
    client, scope_session, schedule_factory, interval_factory,
    revision_factory, composition_factory, slot_factory
):
    schedule = schedule_factory()
    composition = composition_factory(service=schedule.service)
    interval = interval_factory(schedule=schedule)
    revision = revision_factory(schedule=schedule, state='active')
    slot_factory(composition=composition, interval=interval)
    interval.revision = revision
    scope_session.add(interval)
    scope_session.commit()

    response = client.get(
        f'/api/watcher/v1/composition/{composition.id}',
        params={'fields': 'schedules'},
    )
    assert response.status_code == status.HTTP_200_OK, response.text
    data = response.json()
    assert data['id'] == composition.id
    assert len(data['schedules']) == 1
    assert data['schedules'][0]['slug'] == schedule.slug


def test_list_composition(client, composition_factory, watcher_robot, composition_participants_factory):
    composition = composition_factory()

    composition_participants_factory(
        staff=watcher_robot,
        composition=composition,
    )

    response = client.get(
        '/api/watcher/v1/composition/',
        params={'filter': f'id={composition.id}'}
    )
    assert response.status_code == status.HTTP_200_OK, response.text
    data = response.json()['result']
    assert len(data) == 1
    composition_response = data[0]
    assert composition_response['id'] == composition.id
    assert composition_response['slug'] == composition.slug
    assert len(composition_response['participants']) == 1
    assert composition_response['participants'][0]['login'] == watcher_robot.login
    assert 'roles' not in composition_response


def test_filter_with_comma(client, composition_factory):
    composition = composition_factory(
        name='test smo,sker name',
        slug='test_some_slug',
    )
    composition_factory()

    response = client.get(
        '/api/watcher/v1/composition/',
        params={'filter': r'name__ilike=smo\,sker'}
    )
    assert response.status_code == status.HTTP_200_OK, response.text
    data = response.json()['result']
    assert len(data) == 1
    assert data[0]['id'] == composition.id


@pytest.mark.parametrize(
    ('filter_by', 'is_match'),
    (
        ['name__ilike=smth', False],
        ['name__ilike=smosker', True],
        ['name__ilike=smOSKer', True],
        ['slug__ilike=smosker', False],
        ['slug__ilike=some', True],
    )
)
def test_list_composition_filter_ilike(client, composition_factory, filter_by, is_match):
    composition = composition_factory(
        name='test smosker name',
        slug='test_some_slug',
    )
    composition_factory()

    response = client.get(
        '/api/watcher/v1/composition/',
        params={'filter': filter_by}
    )
    assert response.status_code == status.HTTP_200_OK, response.text
    data = response.json()['result']
    if is_match:
        assert len(data) == 1
        assert data[0]['id'] == composition.id
    else:
        assert len(data) == 0


@pytest.mark.parametrize(
    ('filter_by', 'is_match'),
    (
        ['name__ilike=Дежурный', True],
        ['name__ilike=дежурный', True],
        ['name__ilike=ДЕЖУРНЫЙ', True],
        ['name__ilike=д', True],
        ['name__ilike=деж', True],
        ['name__ilike=дижурный', False],
    )
)
def test_list_composition_filter_ilike_russian(client, composition_factory, filter_by, is_match):
    composition = composition_factory(
        name='Дежурный',
        slug='test_some_slug',
    )
    composition_factory()

    response = client.get(
        '/api/watcher/v1/composition/',
        params={'filter': filter_by}
    )
    assert response.status_code == status.HTTP_200_OK, response.text
    data = response.json()['result']
    if is_match:
        assert len(data) == 1
        assert data[0]['id'] == composition.id
    else:
        assert len(data) == 0


@pytest.mark.parametrize('autoupdate', (True, False))
def test_recalculate_composition(
    client, composition_factory, slot_factory, staff_factory,
    member_factory, scope_session, autoupdate
):
    composition = composition_factory(autoupdate=autoupdate)
    staff = staff_factory()
    member_factory(staff=staff, service=composition.service)
    composition.staff.append(staff)
    slot = slot_factory(composition=composition)

    scope_session.add(composition)
    scope_session.commit()
    with patch('watcher.api.routes.composition.start_people_allocation') as mock_start_people_allocation:
        response = client.post(
            f'/api/watcher/v1/composition/{composition.id}/recalculate'
        )
    assert response.status_code == status.HTTP_200_OK, response.text
    mock_start_people_allocation.delay.assert_called_once_with(
        schedules_group_id=slot.interval.schedule.schedules_group_id
    )
    data = response.json()

    assert data['id'] == composition.id
    assert data['slug'] == composition.slug
    assert data['roles'] == []
    assert len(data['participants']) == 1
    assert data['participants'][0]['uid'] == int(staff.uid)
