import json

import pytest

from django.conf import settings
from django.contrib.auth.models import Permission
from django.core.files.uploadedfile import SimpleUploadedFile
from django.core.urlresolvers import reverse

from staff.departments.models import DepartmentRoles
from staff.lib.testing import DepartmentStaffFactory, DepartmentFactory
from staff.preprofile.views.import_views import outstaff_mass_import

FIELDS = [
    'number',
    'last_name',
    'first_name',
    'middle_name',
    'first_name_en',
    'last_name_en',
    'login',
    'gender',
    'email',
    'phone',
    'address',
    'department',
    'position',
    'citizenship',
    'hardware_profile',
    'office',
]


def add_outstaff_perm(tester, dep):
    DepartmentStaffFactory(department=dep, staff=tester.get_profile(), role_id=DepartmentRoles.CHIEF.value)
    tester.user_permissions.add(Permission.objects.get(codename='can_outstaff'))


def make_outstaff_dep(company):
    result = DepartmentFactory(
        id=settings.OUTSTAFF_DEPARTMENT_ID,
        parent=company.dep2,
        name='outstaff',
        code='outstaff',
        url='yandex_dep2_outstaff',
    )
    return result


@pytest.mark.django_db()
def test_outstaff_mass_import_will_return_400_wrong_tempalte(company, rf, tester):
    outstaff = company.outstaff
    add_outstaff_perm(tester, outstaff)

    request = rf.post(reverse('preprofile:outstaff_mass_import'))
    request.user = tester

    request.FILES['import_file'] = SimpleUploadedFile('file.xlsx', 'somedata'.encode('utf-8'))
    result = outstaff_mass_import(request)

    assert result.status_code == 400

    assert 'incorrect_template' == json.loads(result.content)['errors'][''][0]['code']
