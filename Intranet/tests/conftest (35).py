from copy import deepcopy
from datetime import datetime, date
from mock import Mock, patch
import pytest

from django.core.urlresolvers import reverse

from staff.groups.models import GROUP_TYPE_CHOICES
from staff.lib.testing import StaffFactory, GroupFactory, OfficeFactory, CityFactory, OrganizationFactory
from staff.preprofile.models import Preprofile, CANDIDATE_TYPE, EMPLOYMENT


@pytest.yield_fixture
def base_form():
    with patch('staff.preprofile.login_validation.validate_for_dns'):
        with patch('staff.preprofile.login_validation.validate_in_ldap'):
            yield {
                'first_name': 'first',
                'last_name': 'last',
                'first_name_en': 'first',
                'last_name_en': 'last',
                'join_at': date.today().isoformat(),
                'gender': 'M',
                'candidate_type': CANDIDATE_TYPE.NEW_EMPLOYEE,
                'login': 'somelogin',
                'lang_ui': 'ru',
                'position': 'Developer',
                'employment_type': EMPLOYMENT.FULL,
                'phone': '88002000600',
            }


@pytest.fixture
def tester():
    tester = StaffFactory(login='tester')
    return tester.user


@pytest.fixture
def tester_request(tester, rf):
    def wrapper(viewname):
        request = rf.get(reverse(viewname))
        request.user = tester
        return request

    return wrapper


def model_field(name):
    [field] = [field for field in Preprofile._meta.local_fields if field.name == name]
    return field


@pytest.yield_fixture
def disable_preprofile_modified_auto_now():
    field = model_field('modified_at')
    field.auto_now = False
    yield None

    field.auto_now = True


@pytest.yield_fixture
def disable_preprofile_created_auto_now():
    field = model_field('created_at')
    field.auto_now_add = False
    yield None

    field.auto_now_add = True


@pytest.fixture
def stub_requests(monkeypatch):
    import staff.lib.requests
    import requests

    def stub(*args, **kwargs):
        raise requests.Timeout()

    monkeypatch.setattr(
        staff.lib.requests,
        'get',
        stub
    )
    monkeypatch.setattr(
        staff.lib.requests,
        'post',
        stub
    )


@pytest.fixture
def mock_side_effects(stub_requests, stub_side_effects, mocked_mongo, monkeypatch):
    from staff.person.controllers import effects
    monkeypatch.setattr(effects, 'unblock_login_in_passport', Mock())

    import staff.person_profile.controllers.digital_sign
    monkeypatch.setattr(
        staff.person_profile.controllers.digital_sign,
        '_connect_phone_in_oebs',
        lambda l, n: (True, ''),
    )

    import staff.preprofile.adopt_api
    monkeypatch.setattr(staff.preprofile.adopt_api, 'deprive_gold_and_silver_crown_through_proposal', Mock())


@pytest.fixture
def abc_services():
    now = datetime.now()

    service_root = GroupFactory(
        name='__services__', url='__services__',
        service_id=None, department=None,
        parent=None,
        created_at=now, modified_at=now,
        type=GROUP_TYPE_CHOICES.SERVICE,
    )

    group_staff = GroupFactory(
        name='staff', url='staff', code='staff',
        service_id=123, department=None,
        parent=service_root,
        created_at=now, modified_at=now,
        type=GROUP_TYPE_CHOICES.SERVICE,
    )

    return [group_staff.code]


@pytest.fixture
def red_rose_office(db):
    return OfficeFactory(name='RedRose', city=CityFactory(name='Moscow'))


@pytest.fixture
def yndx_org():
    return OrganizationFactory(name='Yandex')


@pytest.fixture
def base_of_organizations_and_offices():
    retval = Mock()
    retval.as_dict = {
        'red_rose_office': OfficeFactory(name='RedRose', city=CityFactory(name='Moscow')),
        'tel_aviv_office': OfficeFactory(name='TelAviv', city=CityFactory(name='TelAviv')),
        'sochi_office': OfficeFactory(name='Sochi', city=CityFactory(name='Sochi')),
        'kinopoisk_org': OrganizationFactory(name='Kinopoisk'),
        'edadeal_org': OrganizationFactory(name='Edadeal'),
        'yndx_org': OrganizationFactory(name='Yandex'),
    }
    params_in_url = deepcopy(retval.as_dict)
    params_in_url.pop('sochi_office')
    params_in_url.pop('edadeal_org')

    def part_url(value, is_office):
        return '%s=%s' % ('office' if is_office else 'org', value.id)

    retval.as_url = '/preprofile?%s' % '&'.join(
        part_url(obj, 'office' in name) for name, obj in params_in_url.items()
    )
    return retval


@pytest.fixture
def company(company_with_module_scope):
    return company_with_module_scope


@pytest.fixture
def settings(settings_with_module_scope):
    return settings_with_module_scope
