import pytest
import json
import factory

from django.conf import settings
from django.core.urlresolvers import reverse

from staff.person.models import Staff

from staff.person.models import VALIDATION_TYPES
from staff.lib.testing import StaffFactory, ContactFactory


class ContactTypeFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'person.ContactType'
    url_pattern = '%s'


def check_person():
    test_person = Staff.objects.get(login=settings.AUTH_TEST_USER)

    assert test_person.home_email == 'test@test.com'
    assert test_person.jabber == ''
    assert test_person.icq == ''
    assert test_person.login_skype == 'valid'
    assert test_person.login_lj == 'http://lj.com/init'
    assert test_person.home_page == ''


@pytest.mark.django_db()
def test_edit_contacts(client):
    ct_email = ContactTypeFactory(id=1, validation_type=VALIDATION_TYPES.EMAIL)
    ct_jabber = ContactTypeFactory(id=2, validation_type=VALIDATION_TYPES.EMAIL)
    ct_icq = ContactTypeFactory(id=3, validation_type=VALIDATION_TYPES.LOGIN)
    ct_skype = ContactTypeFactory(id=4, validation_type=VALIDATION_TYPES.LOGIN)
    ct_home_page = ContactTypeFactory(id=7, validation_type=VALIDATION_TYPES.URL)
    ct_lj = ContactTypeFactory(id=8, validation_type=VALIDATION_TYPES.URL)

    test_person = StaffFactory(
        login=settings.AUTH_TEST_USER,
        home_email='init@home.ru',
        jabber='init@jabber.com',
        icq='111111',
        login_skype='init',
        login_lj='http://lj.com/init',
        home_page='http://init.ru',
    )

    # для обновления контакта
    c_email = ContactFactory(
        person=test_person,
        contact_type=ct_email,
        account_id=test_person.home_email,
        position=1,
    )
    # для удаления
    ContactFactory(
        person=test_person,
        contact_type=ct_icq,
        account_id=test_person.icq,
        position=2,
    )
    # для обновленя типа
    c_home_page = ContactFactory(
        person=test_person,
        contact_type=ct_home_page,
        account_id=test_person.home_page,
        position=3,
    )

    data = {'contacts': [
        # Обновляем валдиный url, меняя тип и порядок
        {
            'id': str(c_home_page.id),
            'contact_type': str(ct_lj.id),
            'account_id': c_home_page.account_id,
            'private': 'false',
        },
        # Обновляем валидным email
        {
            'id': str(c_email.id),
            'contact_type': str(ct_email.id),
            'account_id': 'test@test.com',
            'private': 'false',
        },
        # Добавляем валидный login
        {
            'id': '',
            'contact_type': str(ct_skype.id),
            'account_id': 'valid',
            'private': 'false',
        },
    ]}

    response = client.post(
        reverse('profile:edit-contacts', kwargs={'login': test_person.login}),
        json.dumps(data),
        content_type='application/json'
    )

    assert response.status_code == 200, response.content

    answer = json.loads(response.content)

    assert answer == {
        'target': {
            'contacts': [
                {
                    'account_url': 'http://init.ru',
                    'icon_url': '',
                    'name': '',
                    'account_id': 'http://init.ru',
                    'private': False},
                {
                    'account_url': 'test@test.com',
                    'icon_url': '',
                    'name': '',
                    'account_id': 'test@test.com',
                    'private': False
                },
                {
                    'account_url': 'valid',
                    'icon_url': '',
                    'name': '',
                    'account_id': 'valid',
                    'private': False
                }
            ]
        }
    }

    check_person()

    data['contacts'] = [
        # Добавляем не валидный email
        {
            'id': '',
            'contact_type': str(ct_jabber.id),
            'account_id': 'invalid'
        },
        # Добавляем не валидный url
        {
            'id': '',
            'contact_type': str(ct_home_page.id),
            'account_id': 'invalid'
        },
        # Добавляем не валидный contact_type
        {
            'id': '',
            'contact_type': '100500',
            'account_id': 'valid'
        },
    ]

    response = client.post(
        reverse('profile:edit-contacts', kwargs={'login': test_person.login}),
        json.dumps(data),
        content_type='application/json'
    )

    assert response.status_code == 200, response.content

    answer = json.loads(response.content)

    assert answer == {'errors': {'contacts': {
        '0': {'account_id': [{'error_key': 'staff-invalid_email_contact'}]},
        '1': {'account_id': [{'error_key': 'staff-invalid_url_contact'}]},
        '2': {'contact_type': [{'error_key': 'modelchoice-field-invalid_choice'}]}
    }}}

    check_person()
