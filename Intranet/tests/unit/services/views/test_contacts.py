import pretend
import pytest
from django.conf import settings
from django.core.urlresolvers import reverse
from mock import patch

from plan.services import models as services_models
from common import factories

pytestmark = pytest.mark.django_db


@pytest.fixture
def data(db):
    staff1 = factories.StaffFactory()
    service1 = factories.ServiceFactory(slug=settings.ABC_DEFAULT_SERVICE_PARENT_SLUG)
    fixture = pretend.stub(
        staff1=staff1,
        service1=service1,
    )
    return fixture


def test_get_contacts(client, data):
    contact_type = factories.ContactTypeFactory(
        code='tracker_startrek',
        validator='STARTREK',
    )

    contact = factories.ServiceContactFactory(
        type=contact_type,
        service=data.service1,
        title='Очередь',
        content='PLAN',
    )

    response = client.json.get(
        reverse('services:service', args=[data.service1.pk]),
        {
            'fields': 'contacts',
        }
    )
    assert response.status_code == 200

    data = response.json()['content']['service']
    assert len(data['contacts']) == 1
    assert data['contacts'][0]['title'] == contact.title
    assert data['contacts'][0]['content'] == contact.content
    assert data['contacts'][0]['type'] == contact.type.code
    assert data['contacts'][0]['id'] == contact.pk
    assert data['contacts'][0]['url'] == 'https://st.yandex-team.ru/%s' % contact.content


def test_add_contact(client, data):
    contact_type = factories.ContactTypeFactory(
        code='tracker_startrek',
        validator='STARTREK',
    )

    with patch('plan.common.utils.startrek.get_tracker_type_by_queue') as get_tracker:
        get_tracker.return_value = 'startrek'

        response = client.json.post(
            reverse('services:service_action_contacts_replace', args=[data.service1.pk]),
            {
                'contacts': [
                    {
                        'content': 'PLAN',
                        'title': 'Очередь',
                        'title_en': 'Queue',
                        'contact_type': contact_type.code,
                    },
                    {
                        'content': 'PLANN',
                        'title': 'Очередь2',
                        'contact_type': contact_type.code,
                    }
                ]
            }
        )

    assert response.status_code == 200, response.content

    assert services_models.ServiceContact.objects.filter(service=data.service1).count() == 2
    contact_one = services_models.ServiceContact.objects.get(service=data.service1, content='PLAN')
    assert contact_one.title == 'Очередь'
    assert contact_one.title_en == 'Queue'
    assert contact_one.type == contact_type

    contact_two = services_models.ServiceContact.objects.get(service=data.service1, content='PLANN')
    assert contact_two.title == 'Очередь2'
    assert contact_two.type == contact_type


def test_add_contact_frontend(client, data):
    contact_type = factories.ContactTypeFactory(
        code='tracker_startrek',
        validator='STARTREK',
    )

    with patch('plan.common.utils.startrek.get_tracker_type_by_queue') as get_tracker:
        get_tracker.return_value = 'startrek'

        response = client.json.put(
            reverse('api-frontend:service-contact-replace',),
            {
                'service_id': data.service1.pk,
                'contacts': [
                    {
                        'content': 'PLAN',
                        'title': 'Очередь',
                        'title_en': 'Queue',
                        'contact_type': contact_type.code,
                    },
                    {
                        'content': 'PLANN',
                        'title': 'Очередь2',
                        'contact_type': contact_type.code,
                    }
                ]
            }
        )

    assert response.status_code == 200, response.content

    assert services_models.ServiceContact.objects.filter(service=data.service1).count() == 2
    contact_one = services_models.ServiceContact.objects.get(service=data.service1, content='PLAN')
    assert contact_one.title == 'Очередь'
    assert contact_one.title_en == 'Queue'
    assert contact_one.type == contact_type

    contact_two = services_models.ServiceContact.objects.get(service=data.service1, content='PLANN')
    assert contact_two.title == 'Очередь2'
    assert contact_two.type == contact_type


def test_add_wrong_contact(client, data):
    contact_type = factories.ContactTypeFactory(code='url')

    response = client.json.post(
        reverse('services:service_action_contacts_replace', args=[data.service1.pk]),
        {
            'contacts': [
                {
                    'content': 'https://abc-prestable.yandex-team.ru/',
                    'title': 'Урл valid',
                    'contact_type': contact_type.code,
                },
                {
                    'content': 'xxx',
                    'title': 'Урл invalid',
                    'contact_type': contact_type.code,
                },
                {
                    'content': 'https://abc-prestable.yandex-team.ru/',
                    'title': 'Урл another valid',
                    'contact_type': contact_type.code,
                },
            ]
        }
    )

    assert response.status_code == 400
    assert response.json()['error']['code'] == 'validation_error'
    assert response.json()['error']['message']['ru'] == 'Неверное значение контакта'
    assert response.json()['error']['extra'] == {'contacts': [{}, {'content': 'Неверное значение контакта'}, {}]}


def test_edit_contacts(client, data):
    contact_type = factories.ContactTypeFactory(
        code='email_ml',
        validator='MAILLIST',
    )

    contact = factories.ServiceContactFactory(
        type=contact_type,
        service=data.service1,
        title='рассылка',
        content='a',
    )

    response = client.json.post(
        reverse('services:service_action_contacts_replace', args=[data.service1.pk]),
        {
            'contacts': [
                {
                    'content': 'b@yandex-team.ru',
                    'title': 'рассылка 2',
                    'contact_type': contact_type.code,
                }
            ]
        }
    )

    assert response.status_code == 200
    assert response.json() == {'content': {}, 'error': {}}
    assert not services_models.ServiceContact.objects.filter(pk=contact.pk).exists()

    contacts = services_models.ServiceContact.objects.filter(service=data.service1)

    assert len(contacts) == 1
    assert contacts[0].service == data.service1
    assert contacts[0].content == 'b'
    assert contacts[0].title == 'рассылка 2'
    assert contacts[0].type == contact_type


def test_edit_contacts_position(client, data):
    contact_type = factories.ContactTypeFactory(
        code='email_ml',
        validator='MAILLIST',
    )

    contact = factories.ServiceContactFactory(
        type=contact_type,
        service=data.service1,
        title='рассылка',
        content='a',
        position=0,
    )

    contact_2 = factories.ServiceContactFactory(
        type=contact_type,
        service=data.service1,
        title='рассылка 2',
        content='b',
        position=1,
    )

    response = client.json.post(
        reverse('services:service_action_contacts_replace', args=[data.service1.pk]),
        {
            'contacts': [
                {
                    'content': 'b@yandex-team.ru',
                    'title': 'рассылка 2',
                    'contact_type': contact_type.code,
                },
                {
                    'content': 'a@yandex-team.ru',
                    'title': 'рассылка',
                    'contact_type': contact_type.code,
                }
            ]
        }
    )

    assert response.status_code == 200
    assert response.json() == {'content': {}, 'error': {}}
    contacts = services_models.ServiceContact.objects.filter(service=data.service1)

    assert len(contacts) == 2

    contact.refresh_from_db()
    contact_2.refresh_from_db()

    assert contact.position == 1
    assert contact_2.position == 0
