from functools import partial
from waffle.testutils import override_switch

import pretend
import pytest
from django.conf import settings
from django.core.urlresolvers import reverse

from plan.roles.models import Role
from common import factories

pytestmark = pytest.mark.django_db


@pytest.fixture
def data(db):
    department = factories.DepartmentFactory(name='Тулзы', name_en='Tools')
    staff1 = factories.StaffFactory(department=department, login='arkadiy')
    staff2 = factories.StaffFactory(department=department, login='ilya')

    service1 = factories.ServiceFactory(
        name='Fooname', slug='fooservice',
        state='develop', url='https://foo.example.com',
        name_en='some_eng_name',
    )
    service2 = factories.ServiceFactory(
        name='Aaa', slug='aaa', state='closed', url='https://foo.example.com')

    boss_role = factories.RoleFactory(code=Role.EXCLUSIVE_OWNER, name='BIG_BOSS')
    staff_role = factories.RoleFactory(name='BASE_STAFF_RU', name_en='BASE_STAFF')
    custom_role = factories.RoleFactory(name='Custom_RU', name_en='Custom', service=service1)

    sm1 = factories.ServiceMemberFactory(service=service1, role=boss_role, staff=staff1)
    sm2 = factories.ServiceMemberFactory(service=service1, role=staff_role, staff=staff2)

    category = factories.ResourceTagCategoryFactory()
    tag = factories.ResourceTagFactory(service=service1, category=category)
    resource = factories.ResourceFactory(external_id='xxx')
    sr = factories.ServiceResourceFactory(resource=resource, service=service1, state='granted')

    fixture = pretend.stub(
        department=department,
        staff1=staff1,
        staff2=staff2,
        service1=service1,
        service2=service2,
        boss_role=boss_role,
        staff_role=staff_role,
        custom_role=custom_role,
        sm1=sm1,
        sm2=sm2,
        tag=tag,
        resource=resource,
        sr=sr
    )
    return fixture


@pytest.mark.parametrize('query', ['fooname', 'eng'])
@pytest.mark.parametrize('staff_role', ['own_only_viewer', 'services_viewer', 'full_access'])
def test_search_service(data, staff_factory, staff_role, client, query):
    client.login(staff_factory(staff_role).login)

    response = client.json.get(
        reverse('multic:multic'),
        data={'format': 'json', 'types': 'service', 'q': query}
    )

    if staff_role == 'own_only_viewer':
        assert response.status_code == 403

    else:
        assert response.status_code == 200

        json = response.json()
        assert len(json) == 1
        assert json[0]['name'] == 'Fooname'
        assert json[0]['state'] == 'develop'


@pytest.mark.parametrize('role', [Role.RESPONSIBLE, Role.RESPONSIBLE_FOR_DUTY, Role.DUTY])
@pytest.mark.parametrize('target_service', ['direct', 'ancestor', 'descendant', None])
def test_filter_by_duty_responsible(client, staff_factory, role, target_service):
    staff = staff_factory()

    service_parent = factories.ServiceFactory()
    service = factories.ServiceFactory(parent=service_parent)
    service_desc = factories.ServiceFactory(parent=service)
    services = {
        'direct': (service, 2),
        'ancestor': (service_parent, 3),
        'descendant': (service_desc, 1),
    }
    if target_service:
        factories.ServiceMemberFactory(
            role=factories.RoleFactory(code=role),
            staff=staff,
            service=services[target_service][0]
        )

    client.login(staff.login)
    response = client.json.get(
        reverse('multic:multic'),
        data={
            'format': 'json', 'types': 'service',
            'service__only_duty_responsible': True,
        }
    )
    assert response.status_code == 200
    data = response.json()
    if not target_service or role == Role.DUTY:
        assert len(data) == 0
    else:
        assert len(data) == services[target_service][1]


def test_filter_service_1(client):
    type_service = factories.ServiceTypeFactory(
        name='Сервис',
        name_en='Service',
        code='undefined'
    )

    type_team = factories.ServiceTypeFactory(
        name='Команда',
        name_en='Team',
        code='team'
    )

    service_1 = factories.ServiceFactory(
        name='Notteam',
        service_type=type_service)

    service_2 = factories.ServiceFactory(
        name='Team',
        service_type=type_team)

    # отфильтруем вcе сервисы
    response = client.json.get(
        reverse('multic:multic'),
        data={'format': 'json', 'types': 'service'}
    )

    assert response.status_code == 200
    assert len(response.json()) == 2

    # отфильтруем тип сервис
    response = client.json.get(
        reverse('multic:multic'),
        data={'format': 'json', 'types': 'service', 'service__service_type':  type_service.code}
    )

    assert response.status_code == 200
    assert len(response.json()) == 1
    assert response.json()[0]['name'] == 'Notteam'
    assert response.json()[0]['is_base_non_leaf'] == service_1.is_base_non_leaf()

    # отфильтруем тип команда
    response = client.json.get(
        reverse('multic:multic'),
        data={'format': 'json', 'types': 'service', 'service__service_type': type_team.code}
    )

    assert response.status_code == 200
    assert len(response.json()) == 1
    assert response.json()[0]['name'] == 'Team'
    assert response.json()[0]['is_base_non_leaf'] == service_2.is_base_non_leaf()


@pytest.mark.parametrize('has_available_type', (True, False))
@pytest.mark.parametrize('switch_active', (True, False))
def test_search_for_move(data, client, has_available_type, switch_active):
    service_type = factories.ServiceTypeFactory()
    service = data.service1
    if has_available_type:
        service.service_type.available_parents.add(service_type)

    target_service = data.service2
    target_service.service_type = service_type
    target_service.save()

    with override_switch(settings.SWITCH_CHECK_ALLOWED_PARENT_TYPE, active=switch_active):
        response = client.json.get(
            reverse('multic:multic'),
            data={
                'format': 'json',
                'types': 'service',
                'q': target_service.slug,
                'from_service_id': service.id,
                'check_type': '1',
            }
        )
    assert response.status_code == 200

    data = response.json()
    if switch_active and not has_available_type:
        assert len(data) == 0
    else:
        assert len(data) == 1
        assert data[0]['slug'] == target_service.slug


def test_search_service_by_slug(data, client):
    response = client.json.get(
        reverse('multic:multic'),
        data={'format': 'json', 'types': 'service', 'q': 'fooservice'}
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == 1
    assert json[0]['slug'] == 'fooservice'


def test_sort_services(data, client):
    response = client.json.get(
        reverse('multic:multic'),
        data={'format': 'json', 'types': 'service', 'q': ''}
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == 3
    assert json[0]['slug'] == 'fooservice'
    assert json[2]['slug'] == 'aaa'


@pytest.mark.parametrize(('staff_role', 'result'), [
    ('own_only_viewer', [settings.HAS_OWN_ONLY_VIEWER]),
    ('services_viewer', [settings.HAS_SERVICES_VIEWER]),
    ('full_access', []),
])
def test_search_staff(data, client, staff_factory, staff_role, result):
    staff = factories.StaffFactory(department=data.department, login='abc_ext')
    factories.InternalRoleFactory(staff=staff, role=staff_role)

    response = client.json.get(
        reverse('multic:multic'),
        data={'format': 'json', 'types': 'staff', 'q': 'abc_'}
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == 1
    assert json[0]['login'] == 'abc_ext'
    assert json[0]['abc_ext'] == result


@pytest.mark.parametrize('from_service', [True, False])
def test_search_staff_filter_by_service(data, client, staff_factory, from_service):

    staff = factories.StaffFactory(department=data.department, login='abc_ext')
    if from_service:
        factories.ServiceMemberFactory(staff=staff, service=data.service1)
    response = client.json.get(
        reverse('multic:multic'),
        data={
            'format': 'json', 'types': 'staff',
            'q': 'abc_', 'from_service_id': data.service1.id,
        }
    )
    assert response.status_code == 200

    json = response.json()
    if from_service:
        assert len(json) == 1
    else:
        assert len(json) == 0


def test_search_staff_filter_by_multiple_service(data, client, staff_factory):

    staff = factories.StaffFactory(department=data.department, login='abc_ext')
    factories.ServiceMemberFactory(staff=staff, service=data.service1)
    factories.ServiceMemberFactory(staff=staff, service=data.service2)
    staff_1 = factories.StaffFactory(department=data.department, login='abc_ext_1')
    factories.ServiceMemberFactory(staff=staff_1, service=data.service2)

    response = client.json.get(
        reverse('multic:multic'),
        data={
            'format': 'json', 'types': 'staff',
            'q': 'abc_', 'from_service_id': f'{data.service1.id},{data.service2.id}',
        }
    )
    assert response.status_code == 200

    data = response.json()
    assert len(data) == 2
    assert set(obj['login'] for obj in data) == {staff.login, staff_1.login}


@pytest.mark.parametrize(('query', 'count'), [('qwerty', 0), ('ilya', 1)])
def test_search_staff_or_department(data, client, query, count):
    response = client.json.get(
        reverse('multic:multic'),
        data={'format': 'json', 'types': 'staff|department', 'q': query}
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == count
    if count:
        assert json[0]['login'] == 'ilya'


@pytest.mark.parametrize(('staff_role_1', 'result_1'), [
    ('own_only_viewer', [settings.HAS_OWN_ONLY_VIEWER]),
    ('services_viewer', [settings.HAS_SERVICES_VIEWER]),
    ('full_access', []),
])
@pytest.mark.parametrize(('staff_role_2', 'result_2'), [
    ('own_only_viewer', [settings.HAS_OWN_ONLY_VIEWER]),
    ('services_viewer', [settings.HAS_SERVICES_VIEWER]),
    ('full_access', []),
])
def test_search_department(client, staff_role_1, staff_role_2, result_1, result_2):
    department = factories.DepartmentFactory(name='Департамент', name_en='Department')
    staff_1 = factories.StaffFactory(department=department)
    factories.InternalRoleFactory(staff=staff_1, role=staff_role_1)

    staff_2 = factories.StaffFactory(department=department)
    factories.InternalRoleFactory(staff=staff_2, role=staff_role_2)

    response = client.json.get(
        reverse('multic:multic'),
        data={'format': 'json', 'types': 'department', 'q': 'Департ'}
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == 1
    assert json[0]['name'] == 'Department'

    assert set(json[0]['abc_ext']) == set(result_1) | set(result_2)


def test_search_role(data, client):
    response = client.json.get(
        reverse('multic:multic'),
        data={'format': 'json', 'types': 'role', 'q': 'STAFF'}
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == 1
    assert json[0]['_text'] == 'BASE_STAFF'
    assert json[0]['service_id'] is None

    assert json[0]['_name']['ru'] == data.staff_role.name
    assert json[0]['_name']['en'] == data.staff_role.name_en


def test_search_role_by_service(data, client):
    response = client.json.get(
        reverse('multic:multic'),
        data={'format': 'json', 'types': 'role', 'q': 'Custom', 'role__service': data.service1.id}
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == 1
    assert json[0]['_text'] == 'Custom'
    assert json[0]['service_id'] == data.service1.id

    assert json[0]['_name']['ru'] == data.custom_role.name
    assert json[0]['_name']['en'] == data.custom_role.name_en

    response = client.json.get(
        reverse('multic:multic'),
        data={'format': 'json', 'types': 'role', 'q': 'Custom', 'role__service': data.service2.id}
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == 0


def test_search_resource_tags(data, client):
    response = client.json.get(
        reverse('multic:multic'),
        data={'format': 'json', 'types': 'resource_tag', 'q': ''}
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == 1
    assert json[0]['_text'] == data.tag.name
    assert json[0]['slug'] == data.tag.slug
    assert json[0]['category__name'] == data.tag.category.name
    assert json[0]['service_id'] == data.tag.service.pk


def test_search_resource_tags_empty_service(client):
    factories.ResourceTagFactory(service=None)

    response = client.json.get(
        reverse('multic:multic'),
        data={'format': 'json', 'types': 'resource_tag', 'q': ''}
    )

    json = response.json()
    assert len(json) == 1
    assert json[0]['service_id'] is None


def test_search_resource_tags_by_service(data, client):
    response = client.json.get(
        reverse('multic:multic'),
        data={'format': 'json', 'types': 'resource_tag', 'q': '', 'resource_tag__service': data.service1.id}
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == 1
    assert json[0]['_id'] == data.tag.id

    response = client.json.get(
        reverse('multic:multic'),
        data={'format': 'json', 'types': 'resource_tag', 'q': '', 'resource_tag__service': data.service2.id}
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == 0


def test_search_free_resource_tags(data, client):
    free_tag = factories.ResourceTagFactory(service=None, name='ZZZ')

    response = client.json.get(
        reverse('multic:multic'),
        data={'format': 'json', 'types': 'resource_tag', 'q': '', 'resource_tag__service': data.service1.id}
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == 2
    assert json[0]['_id'] == data.tag.id
    assert json[1]['_id'] == free_tag.id


def test_search_resources(client):
    r1 = factories.ResourceFactory(external_id='external_id1')
    r2 = factories.ResourceFactory(external_id='external_id2')

    response = client.json.get(
        reverse('multic:multic'),
        data={
            'format': 'json',
            'types': 'resource',
            'q': r1.external_id,
        }
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == 1
    assert json[0]['_text'] == r1.name
    assert json[0]['external_id'] == r1.external_id
    assert json[0]['_id'] == r1.pk

    response = client.json.get(
        reverse('multic:multic'),
        data={
            'format': 'json',
            'types': 'resource',
            'q': 'Res',
        }
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == 2

    response = client.json.get(
        reverse('multic:multic'),
        data={
            'format': 'json',
            'types': 'resource',
            'resource__type': r2.type.pk,
        }
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == 1
    assert json[0]['_id'] == r2.pk


def test_search_service_resources(data, client):
    response = client.json.get(
        reverse('multic:multic'),
        data={
            'format': 'json',
            'types': 'service_resource',
            'q': data.resource.external_id,
            'service_resource__service': data.service1.id,
        }
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == 1
    assert json[0]['_text'] == data.resource.name
    assert json[0]['external_id'] == data.resource.external_id
    assert json[0]['resource_id'] == data.resource.pk

    response = client.json.get(
        reverse('multic:multic'),
        data={
            'format': 'json',
            'types': 'service_resource',
            'q': data.resource.external_id,
            'service_resource__service': data.service2.id,
        }
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == 0


def test_search_resource_types_by_supplier(client):
    service_1 = factories.ServiceFactory(name='Сервис')
    factories.ResourceTypeFactory(supplier=service_1, form_id=1)
    service_2 = factories.ServiceFactory(name='Другое имя')
    resource_type_2 = factories.ResourceTypeFactory(supplier=service_2, form_id=2)
    response = client.json.get(
        reverse('multic:multic'),
        {
            'format': 'json',
            'types': 'resource_form_type',
            'q': 'руг',
        }
    )
    assert response.status_code == 200
    assert len(response.json()) == 1
    assert response.json()[0]['_id'] == resource_type_2.id


def test_search_resource_types_multiple_consumers(client):
    rt1 = factories.ResourceTypeFactory(has_multiple_consumers=True, is_enabled=True)
    rt2 = factories.ResourceTypeFactory(form_id=123, is_enabled=True)
    factories.ResourceTypeFactory()
    response = client.json.get(
        reverse('multic:multic'),
        {
            'format': 'json',
            'types': 'resource_form_type',
            'q': '',
        }
    )
    assert response.status_code == 200
    assert len(response.json()) == 2
    assert {x['_id'] for x in response.json()} == {rt1.id, rt2.id}


@pytest.fixture
def types_data():
    resource_type = factories.ResourceTypeFactory(form_id=1, name='Type 1')
    category = factories.ResourceTypeCategoryFactory()
    resource_type_with_category = factories.ResourceTypeFactory(form_id=2, category=category, name='Type 2')
    factories.ResourceTypeFactory(form_id=3, is_enabled=False)
    factories.ResourceTypeFactory()
    factories.ResourceTypeFactory()
    return [resource_type, resource_type_with_category]


def test_resource_type(types_data, client):
    response = client.json.get(
        reverse('multic:multic'),
        data={
            'format': 'json',
            'types': 'resource_form_type',
        }
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == 2
    assert json[0]['_text'] == types_data[0].name
    assert json[0]['_id'] == types_data[0].pk
    assert json[0]['name'] == types_data[0].name
    assert json[0]['supplier__name'] == types_data[0].supplier.name


def test_resource_type_by_supplier(types_data, client):
    response = client.json.get(
        reverse('multic:multic'),
        data={
            'format': 'json',
            'types': 'resource_form_type',
            'resource_form_type__supplier': types_data[0].supplier.pk,
        }
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == 1
    assert json[0]['_id'] == types_data[0].pk


def test_resource_type_by_category(types_data, client):
    response = client.json.get(
        reverse('multic:multic'),
        data={
            'format': 'json',
            'types': 'resource_form_type',
            'resource_form_type__category': types_data[1].category.pk,
        }
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == 1
    assert json[0]['_id'] == types_data[1].pk


def test_service_tag(client):
    tag = factories.ServiceTagFactory()

    response = client.json.get(
        reverse('multic:multic'),
        data={
            'format': 'json',
            'types': 'service_tag',
        }
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == 1

    assert json[0]['id'] == tag.id
    assert json[0]['_text'] == tag.name
    assert json[0]['name'] == tag.name
    assert json[0]['name_en'] == tag.name_en
    assert json[0]['color'] == tag.color
    assert json[0]['slug'] == tag.slug


def test_resource_tag(client):
    tag = factories.ResourceTagFactory(service=factories.ServiceFactory())
    tag2 = factories.ResourceTagFactory(service=factories.ServiceFactory())
    global_tag = factories.ResourceTagFactory(service=None)
    resource_type = factories.ResourceTypeFactory(supplier=tag2.service)

    query = {
        'format': 'json',
        'types': 'resource_tag',
        'resource_tag__resource_type': resource_type.id
    }
    request = partial(client.json.get, reverse('multic:multic'), data=query)

    # если у типа нет тегов, отдаем глобальные или теги, привязанные к поставщику
    response = request()
    assert response.status_code == 200
    json = response.json()
    assert len(json) == 2
    assert {json[0]['_id'], json[1]['_id']} == {global_tag.id, tag2.id}

    # если у типа есть теги, отдаем только их
    resource_type.tags.add(tag)

    response = request()
    assert response.status_code == 200
    json = response.json()
    assert len(json) == 1
    assert json[0]['_id'] == tag.id


def test_type_category(client):
    category = factories.ResourceTypeCategoryFactory()

    response = client.json.get(
        reverse('multic:multic'),
        data={
            'format': 'json',
            'types': 'type_category',
        }
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == 1

    assert json[0]['id'] == category.id
    assert json[0]['_text'] == category.name
    assert json[0]['name'] == category.name
    assert json[0]['name_en'] == category.name_en
    assert json[0]['slug'] == category.slug


def test_type_category_wilter_by_active_types(client):
    category1 = factories.ResourceTypeCategoryFactory()
    category2 = factories.ResourceTypeCategoryFactory()
    factories.ResourceTypeFactory(category=category1)
    factories.ResourceTypeFactory(category=category2, is_enabled=False)

    response = client.json.get(
        reverse('multic:multic'),
        data={
            'format': 'json',
            'types': 'type_category',
            'type_category__with_types': True
        }
    )
    assert response.status_code == 200

    json = response.json()
    assert len(json) == 1
    assert json[0]['id'] == category1.id
