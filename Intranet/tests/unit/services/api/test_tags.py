import pytest
import pretend

from mock import patch
from django.contrib.auth.models import Permission
from django.test import override_settings
from rest_framework.reverse import reverse

from plan.staff.models import Staff
from plan.services.models import ServiceTag
from plan.services.tasks import calculate_gradient_fields
from common import factories

pytestmark = pytest.mark.django_db


@pytest.fixture
def data_tags(metaservices):
    factories.ServiceTypeFactory(code='undefined')
    service = factories.ServiceFactory()
    bu = factories.ServiceTagFactory(slug='bu')
    vs = factories.ServiceTagFactory(slug='vs')
    umb = factories.ServiceTagFactory(slug='umb')
    outline = factories.ServiceTagFactory(slug='outline')
    create_service_data = {
        'name': {'ru': 'some_tags_service', 'en': 'some_tags_service'},
        'slug': 'some_tags_service',
        'parent': service.id,
        'owner': factories.StaffFactory().login,
    }
    return pretend.stub(
        service=service,
        create_service_data=create_service_data,

        tag_vs=vs,
        tag_umb=umb,
        tag_outline=outline,
        tag_bu=bu,
    )


@pytest.mark.parametrize('tag_owner_login, status_code', [
    ('first', 200),
    ('second', 403),
])
def test_validate_service_tags_permissions(client, staff_factory, tag_owner_login, status_code):
    """
    Для редактирования сервисного тега нужны права, если права на его редактирование выданы какому-либо пользователю.
    """
    tag = factories.ServiceTagFactory()
    first = staff_factory(login='first')
    staff_factory(login='second')
    tag_owner = Staff.objects.get(login=tag_owner_login)
    tag_owner.user.user_permissions.add(Permission.objects.get(codename=f'change_service_tag_{tag.slug}'))

    client.login(first.login)
    response = client.json.get(
        reverse('api-frontend:service-validate-tags-list'),
        {'tags': [tag.slug]},
    )

    assert response.status_code == status_code


@pytest.mark.parametrize('tag', ['vs', 'bu'])
@pytest.mark.parametrize(('is_superuser', 'status_code'), [
    (True, 200),
    (False, 403)
])
@pytest.mark.parametrize('create', [False, 'services-api', 'api-v3', 'api-v4'])
def test_validate_gradient_tags(client, data_tags, tag, is_superuser, status_code, make_staff_superuser, staff_factory, create):
    """
    Теги VS или BU могут добавлять только супер-котики.
    """

    staff = data_tags.service.owner
    if is_superuser:
        staff = make_staff_superuser(staff)
    else:
        staff = staff_factory('full_access', staff,)

    client.login(staff.login)
    tags = [tag, 'new']
    if create:
        if is_superuser:
            status_code = 201
        data = {'tags': [ServiceTag.objects.get(slug=tag).pk]}
        data.update(data_tags.create_service_data)
        with patch('plan.services.tasks.register_service'):
            response = client.json.post(
                reverse(f'{create}:service-list'),
                data=data,
            )
    else:
        response = client.json.get(
            reverse('api-frontend:service-validate-tags-list'),
            {
                'parent': data_tags.service.id,
                'tags': tags,
            },
        )
    assert response.status_code == status_code

    if status_code == 403:
        result = response.json()
        assert result['error']['message']['ru'] == f'У вас недостаточно прав для добавления тега {tag}.'


@pytest.mark.parametrize('create', [False, 'services-api', 'api-v3', 'api-v4'])
def test_validate_two_gradient_tags(client, data_tags, staff_factory, create):
    staff = staff_factory('full_access', data_tags.service.owner, )

    client.login(staff.login)
    if create:
        data = {'tags': [
            ServiceTag.objects.get(slug='umb').pk,
            ServiceTag.objects.get(slug='bu').pk,
        ]}
        data.update(data_tags.create_service_data)

        response = client.json.post(
            reverse(f'{create}:service-list'),
            data=data,
        )
    else:
        response = client.json.get(
            reverse('api-frontend:service-validate-tags-list'),
            {
                'parent': data_tags.service.id,
                'tags': ['umb', 'bu'],
            },
        )
    assert response.status_code == 400

    result = response.json()
    assert result['error']['message']['ru'] == 'Нельзя добавить несколько градиентных тегов одновременно.'


@pytest.mark.parametrize('parent_tag', [None, 'umb', 'outline'])
@pytest.mark.parametrize('create', [False, 'services-api', 'api-v3', 'api-v4'])
def test_validate_umb_tag(parent_tag, client, data_tags, staff_factory, create):
    staff = staff_factory('full_access', data_tags.service.owner, )
    parent = data_tags.service
    message = 'Прямой предок сервиса не является valuestream.'

    if parent_tag:
        """
        parent (тег VS + valuestrem == parent)
        |
        |_ parent.parent(тег UMB + valuestrem == parent.parent.parent)
        """

        parent.tags.add(data_tags.tag_vs)
        parent.parent = factories.ServiceFactory(parent=factories.ServiceFactory())
        parent.save()
        parent.parent.tags.add(ServiceTag.objects.get(slug=parent_tag))
        parent.parent.parent.tags.add(data_tags.tag_vs)
        calculate_gradient_fields(parent.parent.parent.id)

        message = 'Выше по дереву уже есть контур или зонтик.'

    client.login(staff.login)
    if create:
        data = {'tags': [
            ServiceTag.objects.get(slug='umb').pk,
        ]}
        data.update(data_tags.create_service_data)

        response = client.json.post(
            reverse(f'{create}:service-list'),
            data=data,
        )
    else:
        response = client.json.get(
            reverse('api-frontend:service-validate-tags-list'),
            {
                'parent': data_tags.service.id,
                'tags': ['umb'],
            },
        )
    assert response.status_code == 400

    result = response.json()
    assert result['error']['message']['ru'] == message


@pytest.mark.parametrize('parent_tag', [None, 'outline'])
@pytest.mark.parametrize('create', [False, 'services-api', 'api-v3', 'api-v4'])
def test_validate_outline_tag(parent_tag, client, data_tags, staff_factory, create):
    staff = staff_factory('full_access', data_tags.service.owner, )
    parent = data_tags.service

    parent.parent = factories.ServiceFactory()
    parent.save()

    if parent_tag:
        parent.tags.add(data_tags.tag_outline)
        parent.parent.tags.add(data_tags.tag_vs)
        calculate_gradient_fields(parent.parent.id)

    else:
        parent.tags.add(data_tags.tag_vs)

    client.login(staff.login)
    if create:
        data = {'tags': [
            ServiceTag.objects.get(slug='outline').pk,
        ]}
        data.update(data_tags.create_service_data)

        response = client.json.post(
            reverse(f'{create}:service-list'),
            data=data,
        )
    else:
        response = client.json.get(
            reverse('api-frontend:service-validate-tags-list'),
            {
                'parent': data_tags.service.id,
                'tags': ['outline'],
            },
        )
    assert response.status_code == 400

    result = response.json()
    assert result['error']['message']['ru'] == 'Выше по дереву нет зонтика или уже есть контур.'


@override_settings(RESTRICT_OEBS_TAGS=True)
@pytest.mark.parametrize('create', [False, 'services-api', 'api-v3', 'api-v4'])
def test_validate_oebs_tags(client, data_tags, staff_factory, create):
    staff = staff_factory('full_access', data_tags.service.owner, )
    factories.ServiceTagFactory(slug='oebs_use_for_hr')
    factories.ServiceTagFactory(slug='oebs_use_for_revenue')

    client.login(staff.login)
    if create:
        data = {'tags': [
            ServiceTag.objects.get(slug='oebs_use_for_hr').pk,
            ServiceTag.objects.get(slug='oebs_use_for_revenue').pk,
        ]}
        data.update(data_tags.create_service_data)

        response = client.json.post(
            reverse(f'{create}:service-list'),
            data=data,
        )
    else:
        response = client.json.get(
            reverse('api-frontend:service-validate-tags-list'),
            {
                'parent': data_tags.service.id,
                'tags': ['oebs_use_for_hr', 'oebs_use_for_revenue'],
            },
        )
    assert response.status_code == 400

    result = response.json()
    assert result['error']['message']['ru'] == 'Редактирование тегов связанных с OEBS запрещено'
