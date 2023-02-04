import pytest
import mock

import json
from typing import Optional

from django.core.urlresolvers import reverse

from staff.lib.testing import StaffFactory, StaffPhoneFactory
from staff.person.models import PHONE_KIND, PHONE_TYPES, Staff

from staff.api.dialer import (
    clean_number,
    Dialer,
    DialerPhone,
    format_name,
    get_person_main_phone,
    get_person_work_phone,
    get_phone_by_number,
)


@pytest.mark.parametrize(
    'number, result',
    [
        ('', ''),
        (12345, '12345'),
        ('+79991234567', '989991234567'),
        ('+7(999) 123-45-67', '989991234567'),
        ('+380123-45-67', '98103801234567'),
    ]
)
def test_clean_number(number, result):
    assert clean_number(number) == result


@pytest.mark.parametrize(
    'login, first_name, last_name, result',
    [
        ('login', 'foo', 'bar', 'foo bar'),
        ('login', '', 'bar', 'bar'),
        ('login', ' ', ' ', 'login'),
        ('login', '', '', 'login'),
    ]
)
def test_format_name(login, first_name, last_name, result):
    assert format_name(login, first_name, last_name) == result


def test_get_person_work_phone():
    person = Staff(
        work_phone=1234,
        login='login',
        first_name_en='foo',
        last_name_en='bar'
    )
    assert get_person_work_phone(person) == DialerPhone(
        number='1234', owner_name='foo bar', owner_login='login'
    )


@pytest.mark.django_db
def test_get_person_main_phone():
    person = StaffFactory(login='login', first_name_en='foo', last_name_en='bar')
    StaffPhoneFactory(
        number='+79991234567',
        staff=person,
        kind=PHONE_KIND.COMMON,
        type=PHONE_TYPES.MOBILE,
        position=0,
    )
    StaffPhoneFactory(
        staff=person,
        kind=PHONE_KIND.COMMON,
        type=PHONE_TYPES.MOBILE,
        position=1,
    )

    result = get_person_main_phone('login')

    assert result == DialerPhone(number='989991234567', owner_name='foo bar', owner_login='login')


@pytest.mark.django_db
def test_get_person_main_phone_only_emergency():
    person = StaffFactory(login='login', first_name_en='foo', last_name_en='bar')
    StaffPhoneFactory(
        number='+79991234567',
        staff=person,
        kind=PHONE_KIND.EMERGENCY,
        type=PHONE_TYPES.MOBILE,
        position=0,
    )

    assert get_person_main_phone('login') is None


@pytest.mark.django_db
def test_get_person_main_phone_for_dismissed():
    person = StaffFactory(is_dismissed=True)
    StaffPhoneFactory(
        number='+79991234567',
        staff=person,
        kind=PHONE_KIND.COMMON,
        type=PHONE_TYPES.MOBILE,
        position=0,
    )

    assert get_person_main_phone(person.login) is None


@pytest.mark.django_db
def test_get_person_main_phone_wo_number():
    person = StaffFactory()

    assert get_person_main_phone(person.login) is None


@pytest.mark.django_db
def test_get_phone_by_work_number():
    StaffFactory(
        work_phone=1234,
        login='login',
        first_name_en='foo',
        last_name_en='bar',
    )

    result = get_phone_by_number('1234')

    assert result == DialerPhone(number='1234', owner_name='foo bar', owner_login='login')


@pytest.mark.django_db
def test_get_phone_by_unknown_number():
    result = get_phone_by_number('12348749537485')

    assert result == DialerPhone(number='12348749537485', owner_name=None, owner_login=None)


@pytest.mark.django_db
def test_get_phone_by_mobile_number():
    # given
    person = StaffFactory(
        login='login',
        first_name_en='foo',
        last_name_en='bar',
    )
    StaffPhoneFactory(number='+79991234567', staff=person)

    # when
    result = get_phone_by_number('+79991234567')

    # then
    assert result == DialerPhone(number='989991234567', owner_name='foo bar', owner_login='login')


@pytest.mark.django_db
def test_get_phone_by_duplicated_mobile_number():
    # given
    phone_number = '+79991234567'
    correct_person = StaffFactory(login='login', first_name_en='foo', last_name_en='bar')

    StaffPhoneFactory(number=phone_number, staff=StaffFactory(is_dismissed=True))
    StaffPhoneFactory(number=phone_number, intranet_status=0)
    StaffPhoneFactory(number=phone_number, staff=correct_person)
    StaffPhoneFactory(number=phone_number)

    # when
    result = get_phone_by_number(phone_number)

    # then
    assert result == DialerPhone(number='989991234567', owner_name='foo bar', owner_login=correct_person.login)


def test_dialer_params():
    dialer = Dialer(
        call_from=DialerPhone(number='1234', owner_name='foo bar', owner_login='from'),
        call_to=DialerPhone(number='4321', owner_name='bar foo', owner_login='to'),
        from_='xxx',
        backto='5555',
    )

    assert dialer._params == {
        'calling': '4321',
        'caller': '1234',
        'callername': 'foo bar',
        'from': 'xxx',
        'backto': '5555',
        'callinglogin': 'to',
    }


@pytest.mark.django_db
@pytest.mark.parametrize(
    'num_from, num_to, errors', [
        (None, None, ['caller_main_phone_not_found', 'called_main_phone_not_found']),
        ('1234', None, ['called_main_phone_not_found']),
        ('1234', '1234', ['identical_numbers']),
        ('1234', '4321', []),
    ]
)
def test_view_call_main_to_main(num_from, num_to, errors, client):
    def get_person_main_phone_mock(login: str) -> Optional[DialerPhone]:
        num = num_from if login == 'tester' else num_to
        if num:
            return DialerPhone(
                number=num,
                owner_name=login,
                owner_login=login,
            )

        return None

    with mock.patch('staff.api.views.get_person_main_phone', get_person_main_phone_mock):
        with mock.patch('staff.api.views.Dialer.call') as call_method:
            url = reverse('api-call_main_to_main')

            response = client.get(url, data={'called_login': 'login'})

            assert response.status_code == 400 if errors else 200
            response_json = json.loads(response.content)
            assert response_json['success'] is not errors

            if errors:
                call_method.assert_not_called()
            else:
                call_method.assert_called_once()

            assert set(response_json['errors']) == set(errors)


@pytest.mark.django_db
def test_view_call_main_to_main_wo_get_param(client):
    url = reverse('api-call_main_to_main')

    response = client.get(url)
    assert response.status_code == 400

    response_json = json.loads(response.content)
    assert not response_json['success']
    assert set(response_json['errors']) == {'called_login_required'}
