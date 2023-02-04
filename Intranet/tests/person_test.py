from datetime import date
import mock

import pytest

from django.conf import settings
from django.contrib.auth.models import ContentType, Permission
from django.test import TestCase

from staff.anketa.models import OfficeSetting
from staff.departments.models import Department
from staff.lib.testing import (
    StaffFactory,
    DepartmentFactory,
    DepartmentRoleFactory,
    OfficeFactory,
    PermissionFactory, DepartmentStaffFactory,
)

from staff.person.controllers import Person
from staff.person.models import Staff, AFFILIATION
from staff.person.survey_email import get_office_services, send_mails
from staff.person.notifications import send_welcome_mail, YANDEX_MARKET_DEPARTMENT_URL


def create_structure():
    from staff.departments.models import Department

    # Departments
    dep_yandex = DepartmentFactory(
        id=settings.YANDEX_DEPARTMENT_ID,
        url='dep_yandex',
        parent=None,
        name='Яндекс',
    )
    dep_ext = DepartmentFactory(
        id=settings.EXT_DEPARTMENT_ID,
        url='dep_ext',
        parent=None,
        name='Внешние',
    )
    dep_yamoney = DepartmentFactory(
        id=settings.YAMONEY_DEPARTMENT_ID,
        url='dep_yamoney',
        parent=None,
        name='Яндекс',
    )
    dep_sa = DepartmentFactory(
        id=settings.SEARCH_ASESSORS_DEPARTMENT_ID,
        url='dep_sa',
        parent=None,
        name='Ассесоры поиска',
    )
    dep_subyandex = DepartmentFactory(
        url='dep_subyandex',
        parent=dep_yandex,
        name='СубЯндекс',
    )
    dep_subext = DepartmentFactory(
        url='dep_subext',
        parent=dep_ext,
        name='СубВнешние',
    )
    dep_subyamoney = DepartmentFactory(
        url='dep_subyamoney',
        parent=dep_yamoney,
        name='СубЯденьги',
    )
    dep_subsa = DepartmentFactory(
        url='dep_subsa',
        parent=dep_sa,
        name='Суб Ассесоры поиска',
    )

    Department.tree.rebuild()

    # Employees
    StaffFactory(
        department=dep_yandex,
        login='dep_yandex'
    )
    StaffFactory(
        department=dep_subyandex,
        login='dep_subyandex'
    )
    StaffFactory(
        department=dep_ext,
        login='dep_ext'
    )
    StaffFactory(
        department=dep_subext,
        login='dep_subext'
    )
    StaffFactory(
        department=dep_yamoney,
        login='dep_yamoney'
    )
    StaffFactory(
        department=dep_subyamoney,
        login='dep_subyamoney'
    )
    StaffFactory(
        department=dep_sa,
        login='dep_sa'
    )
    StaffFactory(
        department=dep_subsa,
        login='dep_subsa'
    )


@pytest.mark.django_db()
def test_actualize_affiliation():
    from staff.person.effects.base import actualize_affiliation
    from staff.person.models import Staff, AFFILIATION

    create_structure()

    result = {}

    for person in Staff.objects.filter(login__startswith='dep'):
        actualize_affiliation(person)
        result[person.login] = person.affiliation

    assert result == {
        'dep_yandex': AFFILIATION.YANDEX,
        'dep_subyandex': AFFILIATION.YANDEX,
        'dep_ext': AFFILIATION.EXTERNAL,
        'dep_subext': AFFILIATION.EXTERNAL,
        'dep_yamoney': AFFILIATION.YAMONEY,
        'dep_subyamoney': AFFILIATION.YAMONEY,
        'dep_sa': AFFILIATION.EXTERNAL,
        'dep_subsa': AFFILIATION.EXTERNAL,
    }


class GetOfficeServicesTest(TestCase):
    def setUp(self):
        self.office = OfficeFactory(name='testoffice')
        self.officesettings = OfficeSetting.objects.create(office=self.office)
        self.staff = StaffFactory(login='testuser', office=self.office)

    def test_no_services(self):
        result = get_office_services(self.staff)
        self.assertEqual(
            result, {'helpdesk': False, 'cadre': False, 'adaptation': False}
        )

    def test_services(self):
        services = {'helpdesk': True, 'cadre': False, 'adaptation': True}
        for service, value in services.items():
            setattr(self.officesettings, '%s_service' % service, value)
        self.officesettings.save()

        result = get_office_services(self.staff)

        self.assertIsInstance(result, dict)
        self.assertEqual(result, services)


class SurveyMailingTest(TestCase):

    class _MockSurveyMailNotificationClass():
        def __init__(self):
            self.init_data = {}
            self.send_data = {}
            self.mails_sent = 0

        def send(self, **kwargs):
            self.send_data = kwargs
            self.mails_sent += 1

        def __call__(self, *args, **kwargs):
            self.init_data = kwargs
            return self

    def setUp(self):
        self.office = OfficeFactory(name='testoffice')
        self.officesettings = OfficeSetting.objects.create(
            office=self.office,
            helpdesk_service=True
        )
        self.staff = StaffFactory(
            login='testuser',
            first_name='Zulfia',
            work_email='testuser@yandex-team.ru',
            office=self.office
        )
        person = Person(self.staff)
        person.date_survey_letter = date.today()
        person.save()

    def test_mail_sent(self):
        m = self._MockSurveyMailNotificationClass()
        expected_context = {
            'recipient': self.staff.first_name,
            'survey_url': (
                'http://yandex.hr-department.sgizmo.com/s3/?ya=1ea955a692'
                '3ab0bb24729df26a7b5d03ac64ad66&kadr=False&help=True&adapt=False'
            ),
        }
        with mock.patch('staff.person.notifications.SurveyMailNotification', side_effect=m):
            send_mails()

            self.assertEqual(m.mails_sent, 1)

            self.assertIn(self.staff.work_email, m.send_data['recipients'])

            self.assertEqual(m.init_data['target'], 'SURVEY_MAILING')
            self.assertEqual(m.init_data['context'], expected_context)


@pytest.fixture
def market_preparation(company):

    from staff.person.models import WelcomeEmailTemplate, StaffExtraFields

    WelcomeEmailTemplate.objects.create(text='text', text_en='text_en')

    # Обзываем dep1 Маркетом
    market_dep = company.dep1
    market_dep.url = YANDEX_MARKET_DEPARTMENT_URL
    market_dep.save()
    market_dep.group.url = market_dep.url
    market_dep.group.save()

    for person in company.persons.values():
        StaffExtraFields.objects.create(
            staff=person,
            is_welcome_mail_sent=False
        )
    return market_dep


def test_not_sending_welcome_to_market(market_preparation):
    market_persons_logins = Staff.objects.filter(
        department__tree_id=market_preparation.tree_id,
        department__lft__gte=market_preparation.lft,
        department__rght__lte=market_preparation.rght,
    ).values_list('login', flat=True)

    with mock.patch('staff.person.notifications._send_welcome', return_value=mock.Mock()) as sender_mock:
        send_welcome_mail()
        assert sender_mock.call_count == (
            Staff.objects
            .filter(affiliation=AFFILIATION.YANDEX)
            .exclude(login__in=market_persons_logins)
            .exclude(is_robot=True)
            .count()
        )


@pytest.mark.django_db
def test_yandex_employee_is_internal():
    person = StaffFactory(affiliation=AFFILIATION.YANDEX)
    assert person.is_internal()


@pytest.mark.django_db
def test_yandex_money_employee_is_internal():
    person = StaffFactory(affiliation=AFFILIATION.YAMONEY)
    assert person.is_internal()


@pytest.mark.django_db
def test_robot_employee_is_not_internal():
    person = StaffFactory(is_robot=True, affiliation=AFFILIATION.EXTERNAL)
    assert not person.is_internal()


@pytest.mark.django_db
def test_external_employee_with_perm_is_internal():
    person = StaffFactory(affiliation=AFFILIATION.EXTERNAL)
    person.user.user_permissions.add(Permission.objects.get(codename='can_view_staff'))
    assert person.is_internal()


@pytest.mark.django_db
def test_external_employee_is_not_internal():
    person = StaffFactory(affiliation=AFFILIATION.EXTERNAL)
    assert not person.is_internal()


@pytest.mark.django_db
def test_outstaff_filtering_doesnt_filter_for_yandex(company):
    permission_codename = 'test_perm'
    PermissionFactory(
        content_type=ContentType.objects.get_for_model(Department),
        codename=permission_codename,
    )
    person = StaffFactory(affiliation=AFFILIATION.YANDEX)

    filter_q = person.departments_by_outstaff_perm_query('django_intranet_stuff.test_perm')

    expected_departments = {dep.id for dep in company['departments'].values()}
    actual_departments = set(Department.objects.filter(filter_q).values_list('id', flat=True))
    assert expected_departments.issubset(actual_departments)


@pytest.mark.django_db
def test_outstaff_filtering_doesnt_filter_for_yamoney(company):
    permission_codename = 'test_perm'
    PermissionFactory(
        content_type=ContentType.objects.get_for_model(Department),
        codename=permission_codename,
    )
    person = StaffFactory(affiliation=AFFILIATION.YAMONEY)

    filter_q = person.departments_by_outstaff_perm_query('django_intranet_stuff.test_perm')

    expected_departments = {dep.id for dep in company['departments'].values()}
    actual_departments = set(Department.objects.filter(filter_q).values_list('id', flat=True))
    assert expected_departments.issubset(actual_departments)


@pytest.mark.django_db
def test_outstaff_filtering_works(company):
    permission = PermissionFactory(
        content_type=ContentType.objects.get_for_model(Department),
        codename='test_perm',
    )
    person = StaffFactory(affiliation=AFFILIATION.EXTERNAL)
    role = DepartmentRoleFactory(id='TEST')
    DepartmentStaffFactory(role=role, department=company.dep2, staff=person)
    role.permissions.add(permission)

    filter_q = person.departments_by_outstaff_perm_query('django_intranet_stuff.test_perm')

    [actual_departments] = Department.objects.filter(filter_q).values_list('id', flat=True)
    assert actual_departments == company.dep2.id
