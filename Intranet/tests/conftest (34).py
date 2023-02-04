from factory.django import mute_signals
import pytest

from django.db.models import signals

from staff.departments.models import DepartmentRoles
from staff.lib.testing import DepartmentFactory, StaffFactory, DepartmentStaffFactory


@pytest.fixture()
def create_departments_with_hrs(kinds):
    """
        дерево департаментов
                d - department without hr_partner and chief
                h - department with hr_partner and/or chief

    lvl            h
                  / \
     1           h   h
                /|   |\
     2         h d   h h
              /  |  |  |
     3       d  d   d  d
    """
    with mute_signals(signals.pre_save, signals.post_save):
        root = DepartmentFactory(name='root')
        ch1_lvl1 = DepartmentFactory(parent=root, name='ch1_lvl1', url='ch1_lvl11')
        ch1_lvl2 = DepartmentFactory(parent=ch1_lvl1, name='ch1_lvl2')
        ch1_lvl3 = DepartmentFactory(parent=ch1_lvl2, name='ch1_lvl3')
        ch2_lvl2 = DepartmentFactory(parent=ch1_lvl1, name='ch2_lvl2')
        ch2_lvl3 = DepartmentFactory(parent=ch2_lvl2, name='ch2_lvl3')

        ch2_lvl1 = DepartmentFactory(parent=root, name='ch2_lvl1')
        ch3_lvl2 = DepartmentFactory(parent=ch2_lvl1, name='ch3_lvl2')
        ch3_lvl3 = DepartmentFactory(parent=ch3_lvl2, name='ch3_lvl3')
        ch4_lvl2 = DepartmentFactory(parent=ch2_lvl1, name='ch4_lvl2')
        ch4_lvl3 = DepartmentFactory(parent=ch4_lvl2, name='ch4_lvl3')

        volozh = StaffFactory(
            first_name='Arcadii',
            last_name='Volozh',
            middle_name='Urievich',
            login='volozh',
            department=root,
        )
        dmirain = StaffFactory(
            first_name='Dmitriy',
            last_name='Prokofiev',
            middle_name='Urievich',
            login='dmirain',
            department=ch1_lvl1,
        )
        wlame = StaffFactory(
            first_name='Vladimir',
            last_name='Spasskiy',
            middle_name='Urievich',
            login='wlame',
            department=ch1_lvl2,
        )
        guido = StaffFactory(
            first_name='Guido',
            last_name='Rossum',
            middle_name='',
            login='guido',
            department=ch1_lvl2,
        )
        babenko = StaffFactory(
            first_name='Maxim',
            last_name='Babenko',
            middle_name='Urievich',
            login='babenko',
            department=ch2_lvl1,
        )
        david = StaffFactory(
            first_name='David',
            last_name='Beazley',
            middle_name='',
            login='david',
            department=ch4_lvl3,
        )
        alex = StaffFactory(
            first_name='Alexis',
            last_name='Sanchez',
            middle_name='',
            login='alex',
            department=ch3_lvl3,
        )
        denis_p = StaffFactory(
            first_name='Denis',
            last_name='Sanchez',
            middle_name='',
            login='denis-p',
            department=ch1_lvl1,
        )

        DepartmentStaffFactory(staff=volozh, department=root, role_id=DepartmentRoles.HR_PARTNER.value)
        DepartmentStaffFactory(staff=volozh, department=root, role_id=DepartmentRoles.CHIEF.value)
        DepartmentStaffFactory(staff=volozh, department=ch1_lvl1, role_id=DepartmentRoles.CURATOR_BU.value)

        DepartmentStaffFactory(staff=dmirain, department=ch1_lvl1, role_id=DepartmentRoles.HR_PARTNER.value)
        DepartmentStaffFactory(staff=dmirain, department=ch1_lvl1, role_id=DepartmentRoles.CHIEF.value)
        DepartmentStaffFactory(staff=dmirain, department=ch3_lvl2, role_id=DepartmentRoles.HR_PARTNER.value)

        DepartmentStaffFactory(staff=wlame, department=ch1_lvl2, role_id=DepartmentRoles.HR_PARTNER.value)
        DepartmentStaffFactory(staff=wlame, department=ch1_lvl2, role_id=DepartmentRoles.CHIEF.value)
        DepartmentStaffFactory(staff=wlame, department=ch4_lvl2, role_id=DepartmentRoles.HR_PARTNER.value)

        DepartmentStaffFactory(staff=guido, department=ch1_lvl2, role_id=DepartmentRoles.HR_PARTNER.value)
        DepartmentStaffFactory(staff=babenko, department=ch2_lvl1, role_id=DepartmentRoles.HR_PARTNER.value)

        DepartmentStaffFactory(staff=denis_p, department=ch1_lvl1, role_id=DepartmentRoles.DEPUTY.value)
        DepartmentStaffFactory(staff=denis_p, department=ch2_lvl1, role_id=DepartmentRoles.DEPUTY.value)

        tamirok = StaffFactory(login='tamirok', department=ch1_lvl3)
        hr_partners = [guido, wlame]
        return locals()
