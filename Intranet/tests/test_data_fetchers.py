from django.utils import timezone
from django.conf import settings

import pytest

from staff.stats.libraries import reports_library
from staff.stats.enums import OrganizationTypes, OfficeTypes
from staff.map.models import Office
from staff.person.models import Organization
from staff.lib.tests.pytest_fixtures import (
    OrganizationFactory,
    OfficeFactory,
    DepartmentFactory,
    StaffFactory,
)
from staff.preprofile.tests.utils import PreprofileFactory, PREPROFILE_STATUS


@pytest.mark.django_db
def test_femida_preprofiles(company):
    now = timezone.now()
    now_date = now.strftime('%Y-%m-%d')

    department = DepartmentFactory()
    PreprofileFactory(
        department=department,
        join_at=timezone.now().date() + timezone.timedelta(days=1),
        organization=OrganizationFactory(),
        office=OfficeFactory(),
    )

    child_department = DepartmentFactory(parent=department)
    p1 = PreprofileFactory(
        department=child_department,
        office=OfficeFactory(),
        join_at=timezone.now().date(),
        status=PREPROFILE_STATUS.CLOSED,
    )
    StaffFactory(login=p1.login)

    other_department = DepartmentFactory()
    no_organization = Organization.objects.get(id=settings.ROBOTS_ORGANIZATION_ID)
    p2 = PreprofileFactory(
        department=other_department,
        organization=no_organization,
        office=OfficeFactory(is_virtual=True),
        join_at=timezone.now().date(),
        status=PREPROFILE_STATUS.CLOSED,
    )
    StaffFactory(login=p2.login)

    homie_office = Office.objects.get(id=settings.HOMIE_OFFICE_ID)
    PreprofileFactory(
        department=other_department,
        organization=OrganizationFactory(),
        join_at=timezone.now().date() + timezone.timedelta(days=1),
        femida_offer_id=1,
        office=homie_office,
    )

    report_class = reports_library['preprofiles']
    result = report_class().get_data(fielddate=now_date)

    checks = [
        {
            'fielddate': now_date,
            'department': [department.id],
            'organization': 'ALL',
            'office': 'ALL',
            'preprofiles_created': 2,
            'femida_preprofiles_created': 0,
            'preprofiles_closed': 1,
            'femida_preprofiles_closed': 0,
        },
        {
            'fielddate': now_date,
            'department': [department.id, child_department.id],
            'organization': 'ALL',
            'office': 'ALL',
            'preprofiles_created': 1,
            'femida_preprofiles_created': 0,
            'preprofiles_closed': 1,
            'femida_preprofiles_closed': 0,
        },
        {
            'fielddate': now_date,
            'department': [other_department.id],
            'organization': OrganizationTypes.no_organization,
            'office': OfficeTypes.virtual,
            'preprofiles_created': 1,
            'femida_preprofiles_created': 0,
            'preprofiles_closed': 1,
            'femida_preprofiles_closed': 0,
        },
        {
            'fielddate': now_date,
            'department': [other_department.id],
            'organization': 'ALL',
            'office': 'ALL',
            'preprofiles_created': 2,
            'femida_preprofiles_created': 1,
            'preprofiles_closed': 1,
            'femida_preprofiles_closed': 0,
        },
    ]
    for checks_id, check in enumerate(checks):
        assert check in result, checks_id
