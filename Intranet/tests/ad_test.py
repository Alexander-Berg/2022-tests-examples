import mock
import pytest

import random
from typing import Iterable, List, Tuple

from staff.lib.testing import StaffFactory, DepartmentFactory, OfficeFactory, CityFactory, CountryFactory

from staff.person.ad import _get_person_data, get_ad_person_data, _remove_accents
from staff.person.models import AFFILIATION


def _ldap_mock(with_ms_exch_mailbox_guid: bool):
    def _ldap_search_mock(search_dn: Tuple[str], query: str, fields: Iterable[str], scope: int = None) -> List[Tuple]:
        if set(fields) == {'pwdLastSet', 'mail', 'telephoneNumber', 'msDS-UserPasswordExpiryTimeComputed'}:
            return [
                (
                    'CN=Roman Puzikov,CN=Users,DC=ld,DC=yandex,DC=ru',
                    {
                        'telephoneNumber': [b'6845'],
                        'pwdLastSet': [b'132065331231582428'],
                        'mail': [b'puroman@yandex-team.ru'],
                        'msDS-UserPasswordExpiryTimeComputed': [b'132065331231582428'],
                    }
                )
            ]
        elif set(fields) == {'homeMDB', 'msExchMailboxGuid'}:
            if with_ms_exch_mailbox_guid:
                return [
                    (
                        'CN=Roman Puzikov,CN=Users,DC=ld,DC=yandex,DC=ru',
                        {
                            'homeMDB': [
                                b'CN=MailDB22-4,CN=Databases,CN=Exchange Administrative Group (FYDIBOHF23SPDLT),'
                                b'CN=Administrative Groups,CN=Yandex,CN=Microsoft Exchange,CN=Services,'
                                b'CN=Configuration,DC=ld,DC=yandex,DC=ru'
                            ],
                            'msExchMailboxGuid': [b'\xc7b\x13^\x00\xc8\x03C\xa4\x02\x80\x97\x02\x04\x8a7']
                        }
                    )
                ]
            else:
                return [('CN=Roman Puzikov,CN=Users,DC=ld,DC=yandex,DC=ru', {})]

    return _ldap_search_mock


def test_has_exchange():
    with mock.patch('staff.lib.ldap_helpers._ldap_search', _ldap_mock(with_ms_exch_mailbox_guid=True)):
        roman_person_data = get_ad_person_data(login='puroman')
    assert roman_person_data['has_exchange'] is True

    with mock.patch('staff.lib.ldap_helpers._ldap_search', _ldap_mock(with_ms_exch_mailbox_guid=False)):
        roman_person_data = get_ad_person_data(login='puroman')
    assert roman_person_data['has_exchange'] is False


@pytest.mark.django_db
def test_get_person_data(django_assert_num_queries):
    post_code = f'{random.randint(10000, 999999)}'
    person = StaffFactory(
        office=OfficeFactory(
            name_en='Red Rose',
            address1_en=f'address {post_code} yy',
            city=CityFactory(name_en='Moscow', country=CountryFactory(name_en='Russia')),
        ),
        position_en=f'position{random.random()}',
        department=DepartmentFactory(name_en=f'department name en {random.random()}'),
        affiliation=AFFILIATION.YANDEX,
        work_email=f'work_email{random.random()}',
        lang_ui='ru',
    )

    with django_assert_num_queries(0):
        person_data = _get_person_data(person)

    assert person_data['department'] == person.department.name_en
    assert person_data['title'] == person.position_en
    assert person_data['streetAddress'] == person.office.address1_en
    assert person_data['co'] == person.office.city.country.name_en
    assert person_data['l'] == person.office.city.name_en
    assert person_data['postalCode'] == post_code
    assert person_data['physicalDeliveryOfficeName'] == person.office.name_en
    assert person_data['employeeType'] == 'yandex'
    assert person_data['extensionAttribute3'] == person.work_email
    assert person_data['yaFirstNameEn'] == person.first_name_en
    assert person_data['yaLastNameEn'] == person.last_name_en
    assert person_data['yaFirstNameRu'] == person.first_name
    assert person_data['yaLastNameRu'] == person.last_name
    assert person_data['preferredLanguage'] == 'ru-RU'


def test_remove_accents():
    assert 'Еромлай' == _remove_accents('Еромлай')
    assert 'Еромлайa' == _remove_accents('Еромлайä')
