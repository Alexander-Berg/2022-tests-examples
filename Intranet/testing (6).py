"""
Utils for testing purposes.
"""
import contextlib
import json
import random
from copy import copy
from datetime import datetime, date, timedelta
from functools import partial
from typing import Any, Dict, Optional

import factory
from factory import Faker

from django.contrib.auth.models import Permission

from staff.django_api_auth.models import Token
from staff.django_intranet_notifications.models import Route

from staff.budget_position.models import BudgetPosition
from staff.keys.models import SSHKey
from staff.map.models.logs import LogAction
from staff.proposal.models import DepartmentAttrs
from staff.departments.models import (
    DEPARTMENT_CATEGORY,
    InstanceClass,
    Department,
    DepartmentKind,
    DepartmentStaff,
    DepartmentRole,
    DepartmentInterfaceSection,
    Geography,
)
from staff.groups.models import Group, GroupMembership, GROUP_TYPE_CHOICES
from staff.map.models import (
    Device,
    Office,
    Floor,
    Table,
    TableReserve,
    TableBook,
    Room,
    Region,
    City,
    Country,
    Placement,
    Log as MapLog,
    RoomUsage,
    SourceTypes,
)
from staff.person.models import (
    AFFILIATION,
    GENDER,
    PASSPORT_TYPE,
    DiscountCard,
    Occupation,
    Organization,
    Passport,
    PersonADInformation,
    StaffPhone,
    Staff,
    Visa,
)
from staff.person.models.person import WORK_MODES
from staff.survey.models import Survey
from staff.users.models import User


class TimeStampedFactory(factory.DjangoModelFactory):
    created_at = factory.LazyAttribute(lambda x: datetime.now())
    modified_at = factory.LazyAttribute(lambda x: datetime.now())


class UserFactory(factory.DjangoModelFactory):
    class Meta:
        model = User

    username = factory.Sequence('username{}'.format)


class DepartmentRoleFactory(factory.DjangoModelFactory):
    class Meta:
        model = DepartmentRole


class PermissionFactory(factory.DjangoModelFactory):
    class Meta:
        model = Permission

    name = factory.Sequence(lambda x: 'name{}'.format(x))
    codename = factory.Sequence(lambda x: 'codename{}'.format(x))


def fill_department_group(obj: Group, *args, **kwargs):
    dep = obj.department
    if dep:
        obj.name = dep.name
        obj.code = dep.code
        obj.url = dep.url
        obj.intranet_status = dep.intranet_status
        parent_group = dep.parent.group if dep.parent else None
        if not parent_group:
            parent_group, _ = Group.objects.get_or_create(
                url='__departments__',
                defaults={
                    'name': '__departments__',
                    'type': GROUP_TYPE_CHOICES.DEPARTMENT,
                    'created_at': datetime.now(),
                    'modified_at': datetime.now(),
                }
            )
        obj.parent = parent_group


class GroupFactory(TimeStampedFactory):
    class Meta:
        model = Group
        django_get_or_create = ('url',)

    url = factory.Sequence('group_url_{}'.format)

    fill_by_department = factory.PostGeneration(fill_department_group)


class ServiceGroupFactory(GroupFactory):
    type = GROUP_TYPE_CHOICES.SERVICE


class ServiceRoleGroupFactory(GroupFactory):
    type = GROUP_TYPE_CHOICES.SERVICEROLE


class DepartmentFactory(TimeStampedFactory):
    class Meta:
        model = Department

    from_staff_id = factory.Sequence(lambda x: x)
    url = factory.Sequence('url_{}'.format)
    name = factory.Sequence(str)
    category = DEPARTMENT_CATEGORY.NONTECHNICAL
    group = factory.RelatedFactory(
        GroupFactory,
        'department',
        type=GROUP_TYPE_CHOICES.DEPARTMENT,
    )
    instance_class = InstanceClass.DEPARTMENT.value


class ValueStreamFactory(TimeStampedFactory):
    class Meta:
        model = Department

    from_staff_id = factory.Sequence(lambda x: x)
    url = factory.Sequence('svc_vs_{}'.format)
    name = factory.Sequence(str)
    category = DEPARTMENT_CATEGORY.NONTECHNICAL
    instance_class = InstanceClass.VALUESTREAM.value


class GeographyDepartmentFactory(TimeStampedFactory):
    class Meta:
        model = Department

    from_staff_id = factory.Sequence(lambda x: x)
    url = factory.Sequence('geo_{}'.format)
    name = factory.Sequence(str)
    instance_class = InstanceClass.GEOGRAPHY.value


class GeographyFactory(factory.DjangoModelFactory):
    oebs_code = factory.Sequence(lambda n: 'code_%d' % n)
    name = factory.Sequence(lambda n: 'geography_%d' % n)
    name_en = factory.Sequence(lambda n: 'geography_%d' % n)
    st_translation_id = factory.Sequence(lambda n: n)
    created_at = factory.LazyAttribute(lambda x: datetime.now())
    modified_at = factory.LazyAttribute(lambda x: datetime.now())
    department_instance = factory.SubFactory(GeographyDepartmentFactory)

    class Meta:
        model = Geography


class DepartmentInterfaceSectionFactory(TimeStampedFactory):
    class Meta:
        model = DepartmentInterfaceSection


class DepartmentKindFactory(TimeStampedFactory):
    class Meta:
        model = DepartmentKind


class DepartmentAttrsFactory(factory.DjangoModelFactory):
    class Meta:
        model = DepartmentAttrs


class OfficeFactory(TimeStampedFactory):
    class Meta:
        model = Office

    from_staff_id = factory.Sequence(lambda x: x)
    is_virtual = False
    name = factory.Sequence(lambda x: str(x))
    name_en = factory.Sequence(lambda x: str(x))


class BudgetPositionFactory(factory.DjangoModelFactory):
    class Meta:
        model = BudgetPosition

    code = factory.Sequence(lambda x: x + 1)
    headcount = 1


class StaffFactory(TimeStampedFactory):
    class Meta:
        model = Staff

    login = factory.LazyAttribute(lambda x: x.login_ld)
    login_ld = factory.Sequence('user{}'.format)
    user = factory.SubFactory(UserFactory)
    department = factory.SubFactory(DepartmentFactory)
    tz = 'UTC'
    lang_ui = 'ru'
    native_lang = 'ru'
    join_at = factory.LazyAttribute(lambda x: datetime.now())
    uid = factory.Sequence('11200000000{}'.format)
    gender = GENDER.MALE
    office = factory.SubFactory(OfficeFactory)
    budget_position = factory.SubFactory(BudgetPositionFactory)
    affiliation = AFFILIATION.YANDEX
    first_name = factory.LazyAttribute(lambda x: Faker('first_name', locale='ru_RU').generate())
    first_name_en = factory.LazyAttribute(lambda x: Faker('first_name').generate())
    last_name = factory.LazyAttribute(lambda x: Faker('last_name', locale='ru_RU').generate())
    last_name_en = factory.LazyAttribute(lambda x: Faker('last_name').generate())
    work_mode = WORK_MODES.OFFICE


class PersonADInformationFactory(factory.DjangoModelFactory):
    class Meta:
        model = PersonADInformation

    person = factory.SubFactory(StaffFactory)
    password_expires_at = factory.LazyAttribute(lambda x: datetime.now())


class StaffPhoneFactory(TimeStampedFactory):
    class Meta:
        model = StaffPhone

    staff = factory.SubFactory(StaffFactory)
    number = factory.Sequence(lambda n: '+7999666{:0>4}'.format(n))
    intranet_status = 1


class OccupationFactory(TimeStampedFactory):
    class Meta:
        model = Occupation

    name = factory.LazyAttribute(
        lambda x: ''.join(word.capitalize() for word in Faker('job').generate().split())
        + str(random.randint(0, 10000000)),
    )
    description = factory.LazyAttribute(lambda x: Faker('job').generate())
    description_en = factory.LazyAttribute(lambda x: Faker('job').generate())
    group_review = ''
    group_bonus = ''
    group_reward = ''


class DepartmentStaffFactory(factory.DjangoModelFactory):
    class Meta:
        model = DepartmentStaff


class GroupMembershipFactory(factory.DjangoModelFactory):
    class Meta:
        model = GroupMembership

    staff = factory.SubFactory(StaffFactory)
    group = factory.SubFactory(GroupFactory)


class FloorFactory(TimeStampedFactory):
    class Meta:
        model = Floor

    from_staff_id = factory.Sequence(lambda x: x)
    num = factory.Sequence(lambda x: x)
    office = factory.SubFactory(OfficeFactory)


class ContactFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'person.Contact'


class ContactTypeFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'person.ContactType'
    url_pattern = '%s'


class TableFactory(TimeStampedFactory):
    class Meta:
        model = Table

    from_staff_id = factory.Sequence(lambda x: x)
    num = factory.Sequence(lambda x: x)
    floor = factory.SubFactory(FloorFactory)


class TableReserveFactory(TimeStampedFactory):
    class Meta:
        model = TableReserve

    table = factory.SubFactory(TableFactory)


class TableBookFactory(TimeStampedFactory):
    class Meta:
        model = TableBook

    staff = factory.SubFactory(StaffFactory)
    table = factory.SubFactory(TableFactory)
    date_from = factory.LazyAttribute(lambda x: datetime.now())
    date_to = factory.LazyAttribute(lambda x: datetime.now())


class RoomFactory(TimeStampedFactory):
    class Meta:
        model = Room

    floor = factory.SubFactory(FloorFactory)


class RegionFactory(factory.DjangoModelFactory):
    class Meta:
        model = Region

    name = factory.sequence('Region {}'.format)
    group = factory.SubFactory(GroupFactory, type=GROUP_TYPE_CHOICES.SERVICE)
    floor = factory.SubFactory(FloorFactory)


class RoomUsageFactory(factory.DjangoModelFactory):
    class Meta:
        model = RoomUsage

    room = factory.SubFactory(RoomFactory)
    source = SourceTypes.PACS.value


class DeviceFactory(TimeStampedFactory):
    class Meta:
        model = Device

    from_staff_id = factory.Sequence(lambda x: x)
    floor = factory.SubFactory(FloorFactory)
    name = factory.Sequence('name{}'.format)


class OrganizationFactory(TimeStampedFactory):
    class Meta:
        model = Organization

    from_staff_id = factory.Sequence(lambda x: x)
    st_translation_id = factory.Sequence(lambda x: x)
    name = factory.LazyAttribute(lambda x: factory.Faker('company', locale='ru_RU').generate()[:50])
    name_en = factory.LazyAttribute(lambda x: factory.Faker('company').generate()[:50])


class CityFactory(TimeStampedFactory):
    class Meta:
        model = City


class CountryFactory(TimeStampedFactory):
    class Meta:
        model = Country

    geo_base_id = factory.Sequence(lambda x: x)


class RouteFactory(factory.DjangoModelFactory):
    class Meta:
        model = Route

    transport_id = factory.Sequence('transport_{}'.format)
    params = factory.Sequence('params_set_{}'.format)


class PlacementFactory(factory.DjangoModelFactory):
    class Meta:
        model = Placement

    active_status = True


class TokenFactory(factory.DjangoModelFactory):
    class Meta:
        model = Token


class SurveyFactory(factory.DjangoModelFactory):
    class Meta:
        model = Survey

    start_at = factory.LazyAttribute(lambda x: datetime.now())
    end_at = factory.LazyAttribute(lambda x: datetime.now())


class PassportFactory(factory.DjangoModelFactory):
    class Meta:
        model = Passport

    doc_type = PASSPORT_TYPE.INTERNAL

    issue_country = 'The Galactic Empire'
    country_code = 'sith'

    number = factory.Sequence('42{}'.format)
    issue_date = factory.LazyAttribute(lambda x: date.today())
    issued_by = 'Palpatin office'

    first_name = factory.LazyAttribute(lambda x: x.person.first_name)
    last_name = factory.LazyAttribute(lambda x: x.person.last_name)


class VisaFactory(factory.DjangoModelFactory):
    class Meta:
        model = Visa

    country = 'The Galactic Empire'
    number = factory.Sequence('42{}'.format)
    issue_date = factory.LazyAttribute(lambda x: date.today())


class DiscountCardFactory(factory.DjangoModelFactory):
    class Meta:
        model = DiscountCard

    number = factory.Sequence('42{}'.format)


class SSHKeyFactory(factory.DjangoModelFactory):
    class Meta:
        model = SSHKey


def _choose_random_log_model_name(*args):
    ALLOWED_MODELS = [Room, Table, Device]
    return MapLog.get_model_name(
        random.choice(ALLOWED_MODELS)
    )


def _choose_random_log_action(*args):
    return random.choice(LogAction.choices())[0]


class MapLogFactory(factory.DjangoModelFactory):
    class Meta:
        model = MapLog

    who = factory.SubFactory(UserFactory)
    created_at = factory.LazyAttribute(lambda x: datetime.now())
    model_pk = factory.Sequence(lambda x: x)
    model_name = factory.LazyAttribute(_choose_random_log_model_name)
    action = factory.LazyAttribute(_choose_random_log_action)


def create_staff(login=None, permissions=(), *args, **kwargs):
    """
    Shortcut function to create Staff instance, corresponding User instance
    and set permissions by full name ("app.codename").
    """
    if not login:
        login = "testuser%s" % random.randint(10**4, 10**7-1)
    kwargs['login'] = login
    kwargs['user__username'] = login
    staff = StaffFactory(*args, **kwargs)
    user = staff.user
    for perm in permissions:
        app_label, codename = perm.split('.')
        try:
            perm_obj = Permission.objects.get(
                content_type__app_label=app_label,
                codename=codename)
        except Permission.DoesNotExist:
            # Include invalid permission name explicitely in error for an
            # easier time fixing this.
            raise Permission.DoesNotExist("Unknown permission: %s" % perm)
        user.user_permissions.add(perm_obj)
    user.save()
    return staff


@contextlib.contextmanager
def passport_override(staff):
    """
    Decorator that sets authorized staff in PassportAuthMiddleware,
    to be used with django.test.Client:
    with passport_override(volozh):
        client.get("/informaion/for/heads/only")
    """
    from django.test.utils import override_settings
    with override_settings(AUTH_TEST_USER=staff.login):
        yield


class ApiMethodTester(object):
    """
    Helper class to quickly test api methods.
    Uses chaining semantics - each called function returns a modified object.
    For examples of usage, see staff/rfid/tests/test_views.py
    """
    def __init__(self, method, url):
        self.method_caller = partial(method, url)
        self.method = method
        self.staff = None
        self.result = None

    def clone(self):
        return copy(self)

    def with_staff(self, staff):
        obj = self.clone()
        obj.staff = staff
        return obj

    def with_perms(self, *permissions):
        staff = create_staff(permissions=permissions)
        return self.with_staff(staff)

    def assert_status(self, status_code):
        """
        Check correct status code is returned using default call parameters
        and given permissions.
        """
        if not self.result:
            obj = self.call()
        else:
            obj = self
        res = obj.result
        assert res.status_code == status_code, (res.status_code, str(res))
        return obj

    def call(self, data='', **kwargs):
        obj = self.clone()
        data = data or kwargs
        request_params = {}

        if 'content_type' in kwargs:
            request_params['content_type'] = kwargs['content_type']
            if kwargs['content_type'].startswith('application/json'):
                if isinstance(data, dict) and 'content_type' in data:
                    del data['content_type']
                data = json.dumps(data)
        request_params['data'] = data
        with passport_override(self.staff):
            obj.result = self.method_caller(**request_params)
        return obj

    def json(self):
        return json.loads(self.result.content)


class SFormErrResponse:
    code: str = ''
    params: Optional[dict] = None

    def __init__(self, json_data: dict):
        self.code = json_data.get('code')
        self.params = json_data.get('params', None)


def get_sform_err_by_field(err_dict: dict, field: str) -> SFormErrResponse:
    errs_list = err_dict.get('errors', {}).get(field, [])
    if errs_list:
        return SFormErrResponse(errs_list[0])


def get_sform_general_err(err_dict: dict) -> SFormErrResponse:
    return get_sform_err_by_field(err_dict, '')


def get_random_datetime(start: Optional[datetime] = None, end: Optional[datetime] = None) -> datetime:
    start = start or datetime(2000, 1, 1)
    end = end or start + timedelta(days=21 * 365)
    delta = end - start
    int_delta = (delta.days * 24 * 60 * 60) + delta.seconds
    random_second = random.randrange(int_delta)
    return start + timedelta(seconds=random_second)


def get_random_date(start: Optional[datetime] = None, end: Optional[datetime] = None) -> date:
    return get_random_datetime(start, end).date()


def verify_forms_error_code(verification_result: Dict[str, Any], key: str, code: str) -> None:
    assert verification_result
    errors = verification_result.get('errors', {})
    assert key in errors
    assert code in (err['code'] for err in errors[key]), errors[key]


@contextlib.contextmanager
def ctx_combine(*managers):
    with contextlib.ExitStack() as stack:
        yield [stack.enter_context(m) for m in managers]
