import json

import pytest
from mock import patch, Mock
from staff.lib.testing import StaffFactory, OfficeFactory

from staff.person.models import DOMAIN
from staff.card_order.forms import YES_NO

import staff.card_order.controller as ctl


@pytest.fixture()
def person():
    return StaffFactory(
        login='test',
        first_name='ru_fn',
        last_name='ru_ln',
        middle_name='ru_mn',
        first_name_en='en_fn',
        last_name_en='en_ln',
        office=OfficeFactory(name='yandex'),
        mobile_phone='+7(999)5553535',
        work_email='test@auto.ru',
        position='тестер',
        position_en='tester',
        work_phone=5553535,
    )


@pytest.mark.django_db
def test_get_initial_card(person):
    card = ctl.get_initial_card(person.login)

    native = card['native'][0]
    assert len(native.keys()) == 11
    assert native['first_name']['value'] == person.first_name
    assert native['last_name']['value'] == person.last_name
    assert native['office']['value'] == person.office
    assert native['mobile_phone']['value'] == '+79995553535'
    assert native['work_phone']['value'] == person.work_phone
    assert native['email_login']['value'] == person.login
    assert native['email_domain']['value'] == DOMAIN.AUTO_RU
    assert native['position']['value'] == 'Тестер'
    assert native['other']['value'] == ''
    assert native['extra_phone']['value'] == ''
    assert native['show_fax']['value'] == 'false'

    english = card['english'][0]
    assert len(english.keys()) == 11
    assert english['first_name']['value'] == person.first_name_en
    assert english['last_name']['value'] == person.last_name_en
    assert english['office']['value'] == person.office
    assert english['mobile_phone']['value'] == '+79995553535'
    assert english['work_phone']['value'] == person.work_phone
    assert english['email_login']['value'] == person.login
    assert english['email_domain']['value'] == DOMAIN.AUTO_RU
    assert english['position']['value'] == 'Tester'
    assert english['other']['value'] == ''
    assert english['extra_phone']['value'] == ''
    assert english['show_fax']['value'] == 'false'

    back = card['back'][0]
    assert len(back.keys()) == 3
    assert back['first_name']['value'] == person.first_name_en
    assert back['last_name']['value'] == person.last_name_en
    assert back['position']['value'] == 'Tester'

    choices = card['choices']
    assert len(choices.keys()) == 3
    assert choices['show_fax'] == [{'value': value, 'label': label} for value, label in YES_NO]
    assert choices['email_domain'] == [{'value': value, 'label': label} for value, label in DOMAIN]
    assert choices['office'] == [{'value': person.office.id, 'label': person.office.name}]


@pytest.mark.django_db
def test_order(person):
    with patch('staff.card_order.objects.requests.Session.post') as request:
        request.return_value = Mock(status_code=200, json=Mock(return_value={'id': '123', 'key': '321'}))
        ctl.order(person.login, 'office_name', 'pdf_file', True, person)

        data = json.loads(request.call_args_list[1][1]['data'])
        assert request.call_count == 2
        assert data['attachmentIds'] == ['123']
        assert data['queue'] == 'TRECEPTION'
        assert data['createdBy'] == 'test'
        assert data['components'] == 44480
        assert data['type'] == 'mockup'
