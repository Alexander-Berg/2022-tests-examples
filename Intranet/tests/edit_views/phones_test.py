import pytest
import json

from django.conf import settings
from django.core.urlresolvers import reverse
from phonenumbers import (
    format_number,
    parse,
    PhoneNumberFormat,
)

from staff.person.models import StaffPhone, PHONE_PROTOCOLS, PHONE_TYPES, PHONE_KIND
from staff.person_profile.edit_views.edit_phones_view import FORM_NAME
from staff.lib.testing import (
    StaffFactory,
    StaffPhoneFactory,
)


VIEW_NAME = 'profile:edit-phones'

PROTOCOL_TO_TYPE = [
    (PHONE_PROTOCOLS.ALL, PHONE_TYPES.MOBILE),
    (PHONE_PROTOCOLS.SMS, PHONE_TYPES.MOBILE),
    (PHONE_PROTOCOLS.VOICE, PHONE_TYPES.HOME),
]

PHONES = [
    ('+380977543214', '+380 97 754 3214'),
    ('+380977589714', '+380 97 758 9714'),
    ('89115359933', '+7 911 535-99-33'),
    ('9123335566', '+7 912 333-55-66'),
]

INVALID_PHONES = [
    (
        {
            'number': '+79110216239000300030030947',
            'kind': 'common_',
            'protocol': 'all_',
        },
        {
            'kind': [{
                'params': {'value': 'common_'},
                'error_key': 'choice-field-invalid_choice'}
            ],
            'protocol': [{
                'params': {'value': 'all_'},
                'error_key': 'choice-field-invalid_choice'}
            ],
            'number': [{'error_key': 'default-field-invalid'}]
        }
    ),
    (
        {
            'number': '375365075382',
            'kind': 'common',
            'protocol': 'all',
        },
        {
            'number': [{'error_key': 'default-field-invalid'}]
        }
    )
]


def get_db_objects(login):
    return StaffPhone.objects.filter(staff__login=login).order_by('position')


@pytest.mark.parametrize('protocol, phone_type', PROTOCOL_TO_TYPE)
@pytest.mark.parametrize('kind', PHONE_KIND)
@pytest.mark.parametrize('phone, result', PHONES)
@pytest.mark.django_db()
def test_add_valid_phone(client, phone, protocol, phone_type, kind, result):
    test_person = StaffFactory(
        login=settings.AUTH_TEST_USER,
    )
    url = reverse(VIEW_NAME, kwargs={'login': test_person.login})

    phone_form = {
        'type': phone_type,
        'number': phone,
        'kind': kind[0],
        'protocol': protocol,
        'description': '',
    }

    response = client.post(
        url,
        json.dumps({FORM_NAME: [phone_form]}),
        content_type='application/json',
    )
    assert response.status_code == 200, response.content
    answer = json.loads(response.content)

    latest_key_id = StaffPhone.objects.order_by('-id').first().id

    phone_form['for_digital_sign'] = False
    phone_form['number'] = result

    db_data = list(
        get_db_objects(test_person.login)
        .values('type', 'number', 'kind', 'protocol', 'description', 'for_digital_sign')
    )

    assert db_data == [phone_form]

    phone_form.pop('type')
    phone_form['id'] = latest_key_id

    assert answer == {
        'target': {
            FORM_NAME: [phone_form]
        }
    }


@pytest.mark.parametrize('phone_form, errors', INVALID_PHONES)
@pytest.mark.django_db()
def test_add_invalid_phone(client, phone_form, errors):
    test_person = StaffFactory(
        login=settings.AUTH_TEST_USER,
    )
    url = reverse(VIEW_NAME, kwargs={'login': test_person.login})

    phone_form.update({
        'type': 2,
        'description': '',
    })

    response = client.post(
        url,
        json.dumps({FORM_NAME: [phone_form]}),
        content_type='application/json'
    )
    assert response.status_code == 200, response.content
    answer = json.loads(response.content)

    assert answer == {
        'errors': {
            'phones': {
                '0': errors
            }
        }
    }


def _format_e164(number):
    return format_number(
        parse(number, 'RU'),
        PhoneNumberFormat.E164,
    )


@pytest.mark.django_db
def test_get_for_digital_sign(client):
    person = StaffFactory(login=settings.AUTH_TEST_USER)
    expecting_phone = StaffPhoneFactory(for_digital_sign=True, staff=person)
    url = reverse(VIEW_NAME, kwargs={'login': person.login})
    response = client.get(url)
    received_phone = json.loads(response.content)['phones'][0]
    assert (
        _format_e164(expecting_phone.number) == _format_e164(received_phone['number']['value']) and
        expecting_phone.id == received_phone['id']['value'] and
        received_phone['for_digital_sign']['value'] and
        received_phone['for_digital_sign']['hidden']
    )


@pytest.mark.django_db
def test_try_edit_for_digital_sign(client):
    person = StaffFactory(login=settings.AUTH_TEST_USER)
    to_update = StaffPhoneFactory(for_digital_sign=True, staff=person)
    to_delete = StaffPhoneFactory(for_digital_sign=True, staff=person)
    update_phone_info = {
        'id': to_update.id,
        'type': to_update.type,
        'number': '+380977543214',
        'kind': to_update.kind,
        'protocol': to_update.protocol,
        'description': 'my description',
    }
    url = reverse(VIEW_NAME, kwargs={'login': person.login})
    response = client.post(
        url,
        json.dumps({FORM_NAME: [update_phone_info]}),
        content_type='application/json'
    )
    received_phones = json.loads(response.content)['target']['phones']
    not_deleted = any(it['id'] == to_delete.id for it in received_phones)
    delete_err = "Forbidden to delete phone for digital sign. Answer: {}".format(received_phones)
    assert not_deleted, delete_err
    updated_phone = next(it for it in received_phones if it['id'] == to_update.id)
    number_err = "Forbidden to change number of phone for digital sign"
    assert _format_e164(updated_phone['number']) == _format_e164(to_update.number), number_err
    description_err = 'Description have to be updatable'
    assert updated_phone['description'] == update_phone_info['description'], description_err
    assert updated_phone['for_digital_sign'], 'Non-updatable fields have to stay as before'
