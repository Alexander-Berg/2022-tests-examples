import pretend
import pytest

from mock import patch
from django.conf import settings
from django.core.urlresolvers import reverse
from django.test import override_settings

from plan.services import models as services_models
from common import factories

pytestmark = pytest.mark.django_db


@pytest.fixture
def data(db, staff_factory):
    factories.ServiceTypeFactory(code='undefined')
    staff1 = staff_factory()
    service1 = factories.ServiceFactory(slug=settings.ABC_DEFAULT_SERVICE_PARENT_SLUG)

    fixture = pretend.stub(
        staff1=staff1,
        service1=service1,
    )
    return fixture


@pytest.mark.parametrize('disabling_api', ['', 'POST:services:service_create'])
def test_create(disabling_api, client, data):
    """
    Никаких ошибок валидации быть не должно при передаче имени
    сервиса в формате:
        'name': {'ru': 'blala', 'en': 'blala'}
    """
    client.login(data.staff1.login)

    new_service_data = {
        'name': {
            'ru': 'бла бла хороший сервис',
            'en': 'blah blah good service',
        },
        'slug': 'goodservice',
        'owner': data.staff1.id,
        'description': 'blah blah blah',
    }

    with override_settings(OBSOLETE_URLS=[disabling_api]):
        response = client.json.post(
            reverse('services:service_create'),
            data=new_service_data,
        )

    if disabling_api == 'POST:services:service_create':
        assert response.status_code == 404

    else:
        assert response.status_code == 200

        result = response.json()['content']

        assert type(result) == dict
        assert 'new_service_id' in result
        srv_id = result['new_service_id']
        service = services_models.Service.objects.get(id=srv_id)

        assert service.name == new_service_data['name']['ru']
        assert service.name_en == new_service_data['name']['en']
        assert service.readonly_state == services_models.Service.CREATING
        assert service.readonly_start_time is not None
        assert service.membership_inheritance is False


def test_same_slug(client, data):
    client.login(data.staff1.login)

    new_service_data = {
        'name': {
            'ru': 'same',
            'en': 'same',
        },
        'slug': settings.ABC_DEFAULT_SERVICE_PARENT_SLUG,
        'owner': data.staff1.id,
        'description': 'blah blah blah',
    }

    response = client.json.post(
        path='/service-actions/create/',
        data=new_service_data,
    )

    assert response.status_code == 400

    error = response.json()['error']
    assert error['code'] == 'validation_error'
    assert error['extra']['slug'][0] == 'Сервис с таким слагом уже существует.'


@pytest.mark.parametrize('is_superuser', [True, False])
def test_parent_base_non_leaf(client, data, is_superuser):
    data.staff1.user.is_superuser = is_superuser
    data.staff1.user.save()
    client.login(data.staff1.login)

    parent = factories.ServiceFactory(parent=None, is_base=True)
    factories.ServiceFactory(parent=parent, is_base=True)

    new_service_data = {
        'name': {
            'ru': 'aaaa',
            'en': 'bbbb',
        },
        'slug': 'aaaa',
        'owner': data.staff1.id,
        'description': 'blah blah blah',
        'move_to': parent.id,
    }

    response = client.json.post(
        path='/service-actions/create/',
        data=new_service_data,
    )

    if is_superuser:
        assert response.status_code == 200
    else:
        assert response.status_code == 400
        error = response.json()['error']
        assert error['code'] == 'validation_error'
        assert error['extra']['move_to'][0] == 'Вы не можете сделать сервис потомком "%s".' % parent.name


def test_right_slug(client, data):
    client.login(data.staff1.login)

    for slug in ('h1234', 'hello', 'h3h3', 'h3-h3', 'h3_h3', 'h3-h3_he', '1hello_world'):
        new_service_data = {
            'name': {
                'ru': 'Имя сервиса {}'.format(slug),
                'en': 'Service name {}'.format(slug),
            },
            'slug': slug,
            'owner': data.staff1.id,
            'description': 'blah blah blah',
        }
        with patch('plan.services.tasks.register_service'):
            with patch('plan.idm.manager.Manager._run_request'):
                response = client.json.post(
                    path='/service-actions/create/',
                    data=new_service_data,
                )

        assert response.status_code == 200


def test_wrong_slug(client, data):
    client.login(data.staff1.login)

    slug = '1234'
    new_service_data = {
        'name': {
            'ru': 'Имя сервиса {}'.format(slug),
            'en': 'Service name {}'.format(slug),
        },
        'slug': slug,
        'owner': data.staff1.id,
        'description': 'blah blah blah',
    }

    response = client.json.post(
        path='/service-actions/create/',
        data=new_service_data,
    )

    assert response.status_code == 400

    error = response.json()['error']
    assert error['code'] == 'validation_error'
    assert error['extra']['slug'][0] == 'Слаг не может состоять из одних цифр.'


def test_same_name(client, data):
    client.login(data.staff1.login)

    new_service_data = {
        'name': {
            'ru': data.service1.name,
            'en': 'xxx',
        },
        'slug': 'xxx',
        'owner': data.staff1.id,
        'description': 'blah blah blah',
    }

    response = client.json.post(
        path='/service-actions/create/',
        data=new_service_data,
    )

    assert response.status_code == 400

    error = response.json()['error']
    assert error['code'] == 'validation_error'
    assert error['extra']['name']['ru'][0] == 'Service name should be unique'


def test_same_name_en(client, data):
    client.login(data.staff1.login)

    new_service_data = {
        'name': {
            'ru': 'xxx',
            'en': data.service1.name_en,
        },
        'slug': 'xxx',
        'owner': data.staff1.id,
        'description': 'blah blah blah',
    }

    response = client.json.post(
        path='/service-actions/create/',
        data=new_service_data,
    )

    assert response.status_code == 400

    error = response.json()['error']
    assert error['code'] == 'validation_error'
    assert error['extra']['name']['en'][0] == 'English Service name should be unique'


def test_same_name_with_department(client, data):
    dep = factories.DepartmentFactory()

    client.login(data.staff1.login)

    new_service_data = {
        'name': {
            'ru': dep.name,
            'en': 'xxx',
        },
        'slug': 'xxx',
        'owner': data.staff1.id,
        'description': 'blah blah blah',
    }

    response = client.json.post(
        path='/service-actions/create/',
        data=new_service_data,
    )

    assert response.status_code == 400

    error = response.json()['error']
    assert error['code'] == 'validation_error'
    assert error['extra']['name']['ru'][0] == 'Названия департаментов и сервисов не могут пересекаться'
