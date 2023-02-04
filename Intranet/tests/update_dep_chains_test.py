import pytest

from staff.lib.testing import DepartmentStaffFactory

from staff.departments.models import DepartmentRoles
from staff.departments.update_dep_chains import get_department_chains


@pytest.mark.django_db
def test_department_chief_chains_generator(company_with_module_scope):
    company = company_with_module_scope
    persons = company.persons
    chains = get_department_chains()

    assert chains[company['yandex'].id]['chiefs'] == [
        persons['yandex-chief'].id
    ]
    assert chains[company['dep1'].id]['chiefs'] == [
        persons['dep1-chief'].id,
        persons['yandex-chief'].id
    ]
    assert chains[company['dep2'].id]['chiefs'] == [
        persons['dep2-chief'].id,
        persons['yandex-chief'].id
    ]
    assert chains[company['dep11'].id]['chiefs'] == [
        persons['dep11-chief'].id,
        persons['dep1-chief'].id,
        persons['yandex-chief'].id
    ]
    assert chains[company['dep12'].id]['chiefs'] == [
        persons['dep12-chief'].id,
        persons['dep1-chief'].id,
        persons['yandex-chief'].id
    ]
    assert chains[company['dep111'].id]['chiefs'] == [
        persons['dep111-chief'].id,
        persons['dep11-chief'].id,
        persons['dep1-chief'].id,
        persons['yandex-chief'].id
    ]
    assert company['removed1'].id not in chains


@pytest.mark.django_db
def test_department_hr_partners_generator(company_with_module_scope):
    company = company_with_module_scope
    persons = company.persons

    DepartmentStaffFactory(
        department=company['dep111'],
        staff=persons['dep111-person'],
        role_id=DepartmentRoles.HR_PARTNER.value,
    )

    DepartmentStaffFactory(
        department=company['dep111'],
        staff=persons['dep1-hr-partner'],
        role_id=DepartmentRoles.HR_PARTNER.value,
    )

    chains = get_department_chains()

    assert chains[company['yandex'].id]['hr_partners'] == []
    assert chains[company['dep1'].id]['hr_partners'] == [persons['dep1-hr-partner'].id]
    assert chains[company['dep2'].id]['hr_partners'] == [persons['dep2-hr-partner'].id]
    assert chains[company['dep11'].id]['hr_partners'] == [persons['dep1-hr-partner'].id]
    assert chains[company['dep111'].id]['hr_partners'] == [persons['dep111-person'].id, persons['dep1-hr-partner'].id]
