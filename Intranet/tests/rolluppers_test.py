import random
from datetime import date
from dateutil.relativedelta import relativedelta

import pytest
from mock import patch, MagicMock

from django.conf import settings
from django.core.management import call_command
from django.test import TestCase

from staff.departments.tests.factories import HRProductFactory
from waffle.models import Switch

from staff.departments.models import HeadcountPosition as StaffHeadcountPosition
from staff.headcounts.tests.factories import HeadcountPositionFactory as StaffHeadcountPositionFactory
from staff.lib.testing import (
    CityFactory,
    CountryFactory,
    DepartmentFactory,
    OfficeFactory,
    OrganizationFactory,
    StaffFactory,
    UserFactory,
    ValueStreamFactory,
    ServiceGroupFactory,
)
from staff.map.models import COUNTRY_CODES, Placement
from staff.person.controllers import Person
from staff.person.models import AFFILIATION, Staff
from staff.person.models.extra import StaffExtraFields

from staff.oebs.constants import PERSON_POSITION_STATUS, POSITION_TYPE
from staff.oebs.controllers.rolluppers import BusinessCenterRollupper, PlacementRollupper
from staff.oebs.controllers.rolluppers.employee_rollupper import EmployeeRollupper
from staff.oebs.controllers.rolluppers.leave_balance_rollupper import days_off_and_vacancies, LeaveBalanceRollupper
from staff.oebs.controllers.rolluppers.oebs_headcounts_rollupper import HeadcountPositionsRollupper
from staff.oebs.models import (
    BusinessCenter,
    Employee,
    LeaveBalance,
    Office,
    Organization,
)
from staff.oebs.tests.factories import (
    BusinessCenterFactory,
    HeadcountPositionFactory,
    OrganizationFactory as OEBSOrganizationFactory,
    HRProductFactory as OEBSHRProductFactory,
)


class Field(object):

    def __init__(self, oebs_field, oebs_val=None, dis_field=None, dis_val=None):
        self.data = {
            'oebs_field': oebs_field,
            'oebs_value': oebs_val,
            'dis_field': oebs_field if dis_field is None else dis_field,
            'dis_value': oebs_val if dis_val is None else dis_val,
        }

    def __getitem__(self, item):
        return self.data.get(item)


class RollupperTestCase(TestCase):
    model = ''
    fields = []

    def setUp(self):
        self.oebs_instance = None
        self.dis_instance = None
        Switch(name=self.switch_name, active=True).save()

    @property
    def switch_name(self):
        return 'rollup_oebs_%s' % self.model.lower()

    def populate_oebs_instance(self):
        for field in self.fields:
            if field['oebs_value']:
                setattr(self.oebs_instance, field['oebs_field'], field['oebs_value'])
        self.oebs_instance.save()

    def assert_equation(self):
        self.dis_instance.refresh_from_db()
        for field in self.fields:
            if field['oebs_value']:
                dis_value = getattr(self.dis_instance, field['dis_field'])
                self.assertEqual(dis_value, field['dis_value'])

    def test_rollup(self):
        if self.oebs_instance is None or self.dis_instance is None:
            # Если процесс накатки очень нетривиальный
            return
        self.populate_oebs_instance()

        call_command('rollup_oebs', self.model, nolock=True)

        self.assert_equation()


def switch_flag(flag_name):
    Switch(name=flag_name, active=True).save()


@pytest.fixture
def oebs_robot():
    UserFactory(username=settings.OEBS_USER_LOGIN)


@pytest.mark.django_db
def test_headcount_positions_creation_on_rollup(oebs_robot):
    switch_flag('rollup_oebs_headcountposition')
    staff = StaffFactory()
    dep = DepartmentFactory()
    oebs_position = HeadcountPositionFactory(
        org_id=dep.id,
        state_position_type=POSITION_TYPE.OFFER_NEW,
        position_name='name',
        position_current_login=staff.login,
    )

    call_command('rollup_oebs', 'HeadcountPosition', nolock=True)

    assert 1 == StaffHeadcountPosition.objects.count()

    position = StaffHeadcountPosition.objects.first()
    assert dep.id == position.department_id
    assert oebs_position.position_code == position.code
    assert oebs_position.position_headcount == position.headcount
    # assert oebs_position.position_product_id == position.valuestream_id  # пока отключена накатка vs
    assert oebs_position.position_geo == position.geo
    assert oebs_position.position_bonus_id == position.bonus_id
    assert oebs_position.position_reward_id == position.reward_id
    assert oebs_position.position_review_id == position.review_id
    assert staff.login == position.current_login
    assert staff.id == position.current_person_id


@pytest.mark.django_db
def test_headcount_positions_rolluper_skips_update_on_same_value():
    staff = StaffFactory()
    staff2 = StaffFactory()
    dep = DepartmentFactory()
    oebs_position = HeadcountPositionFactory(
        org_id=dep.id,
        state_position_type=POSITION_TYPE.OFFER_NEW,
        position_status=PERSON_POSITION_STATUS.OFFER,
        position_category_is_new=True,
        position_name='name',
        position_current_login=staff.login,
        position_product_id=1,
    )

    staff_position = StaffHeadcountPositionFactory(
        id=oebs_position.id,
        department=dep,
        status=PERSON_POSITION_STATUS.OFFER,
        category_is_new=True,
        name='old name',
        current_login=staff2.login,
        current_person=staff2,
        # product_id=2,
    )

    rollupper = HeadcountPositionsRollupper()

    assert not rollupper.generic_rollup(
        oebs_position,
        staff_position,
        'org_id',
        False,
        map_func='_get_department_id',
        dest_field='department_id',
    )


@pytest.mark.django_db
def test_headcount_positions_update_on_rollup(oebs_robot):
    switch_flag('rollup_oebs_headcountposition')
    staff = StaffFactory()
    staff2 = StaffFactory()
    dep = DepartmentFactory()
    oebs_position = HeadcountPositionFactory(
        org_id=dep.id,
        state_position_type=POSITION_TYPE.OFFER_NEW,
        position_status=PERSON_POSITION_STATUS.OFFER,
        position_category_is_new=True,
        position_name='name',
        position_current_login=staff.login,
        position_product_id=1,
    )

    staff_position = StaffHeadcountPositionFactory(
        id=oebs_position.id,
        department=dep,
        status=PERSON_POSITION_STATUS.OFFER,
        category_is_new=True,
        name='old name',
        current_login=staff2.login,
        current_person=staff2,
        # product_id=2,
    )

    oebs_position.departments_headcountposition = staff_position
    oebs_position.save()

    call_command('rollup_oebs', 'HeadcountPosition', nolock=True)

    assert 1 == StaffHeadcountPosition.objects.count()

    position = StaffHeadcountPosition.objects.first()
    assert dep.id == position.department_id
    assert oebs_position.position_code == position.code
    assert oebs_position.position_headcount == position.headcount
    # assert oebs_position.position_product_id == position.valuestream_id  # пока отключена накатка
    assert staff.login == position.current_login
    assert staff.id == position.current_person_id


@pytest.mark.django_db
def test_placement_creation_on_rollup():
    dis_org = OrganizationFactory()
    oebs_org = Organization.objects.create(org_id=100500, dis_organization=dis_org)

    office = OfficeFactory()
    BusinessCenter.objects.create(dis_office=office, code='some_bc')
    oebs_placement = Office.objects.create(
        location_id=1,
        location_code='2',
        location_addr='some addr',
        active_status='Открыта',
        home_work='ДА',
        taxunit_code=oebs_org.org_id,
        businesscentre_code='some_bc'
    )

    PlacementRollupper.rollup(create_absent=True)

    oebs_placement.refresh_from_db()
    placement = oebs_placement.dis_placement
    assert placement.id == oebs_placement.location_id
    assert placement.name == oebs_placement.location_code
    assert placement.addr == oebs_placement.location_addr
    assert placement.active_status
    assert placement.home_work
    assert placement.organization == dis_org
    assert placement.office == office


@pytest.mark.django_db
def test_placement_update_on_rollup():
    dis_org = OrganizationFactory()
    oebs_org = Organization.objects.create(org_id=100500, dis_organization=dis_org)
    office = OfficeFactory()

    placement = Placement.objects.create(
        name='2',
        addr='some addr',
        active_status=True,
        home_work=True,
        office=office,
        organization=dis_org,
    )
    BusinessCenter.objects.create(dis_office=office, code='some_bc')

    oebs_placement = Office.objects.create(
        location_id=placement.id,
        location_code='3',
        location_addr='some addr 2',
        active_status='Закрыто',
        home_work='НЕТ',
        taxunit_code=oebs_org.org_id,
        businesscentre_code='some_bc',
        dis_placement=placement,
    )

    PlacementRollupper.rollup(create_absent=True)

    placement.refresh_from_db()
    assert placement.id == oebs_placement.location_id
    assert placement.name == oebs_placement.location_code
    assert placement.addr == oebs_placement.location_addr
    assert not placement.active_status
    assert not placement.home_work
    assert placement.organization == dis_org
    assert placement.office == office


@pytest.mark.django_db
def test_office_rollup():
    oebs_bc = BusinessCenterFactory(code='12345', name_ru='БЦ', name_en='BC', staff_usage='ДА')
    BusinessCenterRollupper.rollup(create_absent=True)
    oebs_bc.refresh_from_db()

    office = oebs_bc.dis_office
    assert office.oebs_businesscenter.code == '12345'
    assert office.name == 'БЦ'
    assert office.name_en == 'BC'
    assert office.intranet_status == 1

    oebs_bc.staff_usage = 'НЕТ'
    oebs_bc.name_ru = 'БЦ ру'
    oebs_bc.save()
    BusinessCenterRollupper.rollup(create_absent=True)

    office.refresh_from_db()
    assert office.oebs_businesscenter.code == '12345'
    assert office.name == 'БЦ'
    assert office.intranet_status == 0

    oebs_bc.staff_usage = 'ДА'
    oebs_bc.save()
    BusinessCenterRollupper.rollup(create_absent=True)

    office.refresh_from_db()
    assert office.oebs_businesscenter.code == '12345'
    assert office.name == 'БЦ ру'
    assert office.intranet_status == 1

    StaffFactory(office=office)

    oebs_bc.staff_usage = 'НЕТ'
    oebs_bc.save()
    BusinessCenterRollupper.rollup(create_absent=True)

    office.refresh_from_db()
    assert office.oebs_businesscenter.code == '12345'
    assert office.intranet_status == 0


class OrganizationRollupperTestCase(RollupperTestCase):
    model = 'Organization'
    fields = [
        Field('name_ru', 'Кек', 'name'),
        Field('name_en', 'Kek'),
    ]

    def setUp(self):
        super(OrganizationRollupperTestCase, self).setUp()
        self.dis_instance = OrganizationFactory()
        self.oebs_instance = Organization(
            dis_organization=self.dis_instance,
            staff_usage='Y',
        )
        self.oebs_instance.save()


class EmployeeRollupperTestCase(RollupperTestCase):
    model = 'Employee'

    fields = [
        Field('last_name', 'Тестированная', 'oebs_last_name'),
        Field('first_name', 'Зульфия', 'oebs_first_name'),
        Field('middle_names', 'Борисовна', 'oebs_middle_name'),
        Field('concatenated_address', 'Третья ул. Строителей д.5 кв. 12',
              'oebs_address'),
        Field('manage_org_name', 'OOO "Эксор"', 'oebs_manage_org_name'),
        Field('nda_end_date', '2012/12/31 01:23:45',
              dis_val=date(2012, 12, 31)),
        Field('contract_end_date', '2013/01/23 23:32:31',
              dis_val=date(2013, 1, 23)),
        Field('byod_access', 'Нет', dis_val=False),
        Field('legal_entity_org_id', dis_field='organization'),
        Field('wiretap', 'Y', dis_val=True),
        Field('staff_agreement', 'Y', dis_val=True),
        Field('staff_biometric_agreement', 'Y', dis_val=True),
    ]

    def setUp(self):
        super(EmployeeRollupperTestCase, self).setUp()
        self.organization = OrganizationFactory()
        self.oebs_organization = Organization(
            org_id=1,
            name_ru='Организация',
            dis_organization=self.organization
        )
        self.oebs_organization.save()

        self.dis_instance = StaffFactory(guid='teststaffguid')
        self.oebs_instance = Employee(
            person_guid=self.dis_instance.guid,
            person_type='Сотрудник',
            dis_staff=self.dis_instance,
            legal_entity_org_id=self.oebs_organization.org_id,
        )
        self.oebs_instance.save()

    def assert_equation(self):

        person = Person(self.dis_instance)
        for field in self.fields:
            person_value = (
                getattr(person, field['dis_field'], getattr(person.instance, field['dis_field'], None))
            )
            self.assertEqual(person_value, field['dis_value'])


class LeaveBallanceRollupperTestCase(RollupperTestCase):
    model = 'LeaveBalance'
    fields = [
        Field('leave_balance_default', '12', 'vacation', 12.0),
        Field('leave_balance_company', '12,22', 'extra_vacation', 12.22),
        Field('time_off', '0,2', 'paid_day_off', 0.2),
    ]

    def setUp(self):
        super(LeaveBallanceRollupperTestCase, self).setUp()
        self.dis_instance = StaffFactory(
            office=OfficeFactory(
                city=CityFactory(name_en='Moscow', country=CountryFactory(name_en='Russia', code='ru'))
            )
        )

        self.oebs_instance = LeaveBalance(
            dis_staff=self.dis_instance,
        )


@pytest.mark.django_db()
def test_skip_rollup_for_leave_balance(map_models):
    person1 = StaffFactory(
        paid_day_off=1,
        vacation=1,
        extra_vacation=2,
        affiliation=AFFILIATION.YANDEX,
        office=map_models['offices']['KR']
    )

    person2 = StaffFactory(
        paid_day_off=1,
        vacation=1,
        extra_vacation=2,
        affiliation=AFFILIATION.YANDEX,
        office=map_models['offices']['MRP']
    )

    assert person1.office.get_country_code() == COUNTRY_CODES.RUSSIA
    assert person2.office.get_country_code() == COUNTRY_CODES.BELARUS

    leave_balance1 = LeaveBalance.objects.create(
        leave_balance_default='5',
        leave_balance_company='5',
        time_off='5',
        dis_staff=person1
    )

    leave_balance2 = LeaveBalance.objects.create(
        leave_balance_default='5',
        leave_balance_company='5',
        time_off='5',
        dis_staff=person2
    )

    LeaveBalanceRollupper.rollup(create_absent=True)

    person1 = Staff.objects.get(pk=person1.pk)
    person2 = Staff.objects.get(pk=person2.pk)

    assert person1.paid_day_off == days_off_and_vacancies(leave_balance1.time_off)
    assert person1.vacation == days_off_and_vacancies(leave_balance1.leave_balance_default)
    assert person1.extra_vacation == days_off_and_vacancies(leave_balance1.leave_balance_company)

    assert person2.paid_day_off != days_off_and_vacancies(leave_balance2.time_off)
    assert person2.vacation != days_off_and_vacancies(leave_balance2.leave_balance_default)
    assert person2.extra_vacation != days_off_and_vacancies(leave_balance2.leave_balance_company)


@pytest.mark.django_db()
def test_skip_rollup_for_employee(map_models):
    org = OrganizationFactory()
    oebs_org = OEBSOrganizationFactory(dis_organization=org)
    oebs_org.org_id = oebs_org.id
    oebs_org.save(update_fields=('org_id',))

    person1 = StaffFactory(organization=org)
    person2 = StaffFactory(organization=org)
    person3 = StaffFactory(organization=org)

    StaffExtraFields.objects.create(staff=person1)
    StaffExtraFields.objects.create(staff=person2)
    StaffExtraFields.objects.create(staff=person3)

    assert not person1.extra.byod_access
    assert not person2.extra.byod_access
    assert not person3.extra.byod_access

    Employee.objects.create(
        dis_staff=person1,
        last_name=person1.last_name,
        first_name=person1.first_name,
        byod_access='y',
        actual_termination_date='',
        legal_entity_org_id=oebs_org.id,
    )
    Employee.objects.create(
        dis_staff=person2,
        last_name=person2.last_name,
        first_name=person2.first_name,
        byod_access='y',
        actual_termination_date=(date.today() - relativedelta(years=1)).strftime('%Y/%m/%d %H:%M:%S'),
        legal_entity_org_id=oebs_org.id,
    )
    Employee.objects.create(
        dis_staff=person3,
        last_name=person3.last_name,
        first_name=person3.first_name,
        byod_access='y',
        actual_termination_date=(date.today() + relativedelta(years=1)).strftime('%Y/%m/%d %H:%M:%S'),
        legal_entity_org_id=oebs_org.id,
    )

    EmployeeRollupper.rollup(create_absent=True)

    person1 = Staff.objects.get(pk=person1.pk)
    person2 = Staff.objects.get(pk=person2.pk)
    person3 = Staff.objects.get(pk=person3.pk)

    assert person1.extra.byod_access
    assert not person2.extra.byod_access
    assert person3.extra.byod_access


@pytest.mark.django_db
def test_hr_product_rollup_value_stream(oebs_robot):
    switch_flag('rollup_oebs_hrproduct')
    abc_id = random.randint(20, 400)

    abc_service = ServiceGroupFactory(intranet_status=1, service_id=abc_id)
    new_vs = ValueStreamFactory(url=abc_service.url)
    dis_instance = HRProductFactory()
    OEBSHRProductFactory(
        product_id=dis_instance.id,
        dis_hr_product=dis_instance,
        service_abc=str(abc_id),
    )

    rollup_mock = MagicMock()

    with patch(
        'staff.oebs.controllers.rolluppers.hr_product_rollupper.ValueStreamsRollupController',
        return_value=rollup_mock,
    ):
        call_command('rollup_oebs', 'HRProduct', nolock=True)
        rollup_mock.rollup.assert_called_once_with()

    dis_instance.refresh_from_db()
    assert dis_instance.value_stream_id == new_vs.id
