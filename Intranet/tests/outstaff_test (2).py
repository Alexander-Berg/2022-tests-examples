from staff.reports.views.departments import yandex_structure_gen

from staff.lib.testing import StaffFactory, DepartmentFactory, DepartmentStaffFactory
from staff.departments.models import DepartmentRoles

import pytest


def generate_department_tree(root, height):
    if height == 0:
        return

    for _ in range(2):
        current_department = DepartmentFactory(parent=root)
        generate_department_tree(current_department, height-1)


@pytest.fixture()
def generate_initial_data(settings, db):
    # create two head departments
    yandex_dep = DepartmentFactory(name="Yandex")
    outstaff_dep = DepartmentFactory(name="Outstaff")
    # redefine settings
    settings.YANDEX_DEPARTMENT_ID = yandex_dep.id
    settings.OUTSTAFF_DEPARTMENT_ID = outstaff_dep.id
    # creating departent tree for each head department
    generate_department_tree(yandex_dep, 2)
    generate_department_tree(outstaff_dep, 2)

    DepartmentFactory.create_batch(10, name="Other Department")

    yandex_staff_chief = StaffFactory(department=yandex_dep)
    outstaff_chief = StaffFactory(department=outstaff_dep)

    DepartmentStaffFactory(staff=yandex_staff_chief, department=yandex_dep, role_id=DepartmentRoles.CHIEF.value)
    DepartmentStaffFactory(staff=outstaff_chief, department=outstaff_dep, role_id=DepartmentRoles.CHIEF.value)


def test_outstaff_excel(generate_initial_data):
    response = yandex_structure_gen()

    columns = True

    for data_list in response:
        # first line in response are column names, so skip it
        if columns:
            columns = False
            continue
        # 4th item contains department chain starting from root
        dep_chain = data_list[3].split(" => ")[0]
        assert dep_chain in ("Yandex", "Outstaff")
