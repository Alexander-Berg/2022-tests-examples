import factory

from django.test import TestCase

from staff.departments.models import Department, DepartmentRoles

from staff.lib.testing import (
    StaffFactory,
    OfficeFactory,
    DepartmentFactory,
    DepartmentStaffFactory,
    GroupFactory,
    GroupMembershipFactory,
)


class AccessControlListFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'security.AccessControlList'
    group = factory.SubFactory(GroupFactory)


class DataProfileFactory(factory.DjangoModelFactory):
    class Meta:
        model = 'security.DataProfile'


class SecurityTestCase(TestCase):

    def setUp(self):
        self.redrose = OfficeFactory(name='red rose')
        self.ekb = OfficeFactory(name='ekb')

        self.level_01 = DepartmentFactory(name='level_01', parent=None)
        self.level_02 = DepartmentFactory(name='level_02', parent=self.level_01)
        self.level_03 = DepartmentFactory(name='level_03', parent=self.level_02)

        self.level_11 = DepartmentFactory(name='level_11', parent=None)
        self.level_12 = DepartmentFactory(name='level_12', parent=self.level_11)
        self.level_13 = DepartmentFactory(name='level_13', parent=self.level_12)

        # rebuild for mptt
        Department.tree.rebuild()
        level_11 = Department.objects.get(id=self.level_11.id)

        self.batman = StaffFactory(login='batman', department=level_11, office=self.redrose)
        self.batman_deputy = StaffFactory(login='batman_deputy', department=level_11, office=self.redrose)

        DepartmentStaffFactory(
            department=self.level_02,
            staff=self.batman,
            role_id=DepartmentRoles.CHIEF.value,
        )

        DepartmentStaffFactory(
            department=self.level_11,
            staff=self.batman,
            role_id=DepartmentRoles.CHIEF.value,
        )

        DepartmentStaffFactory(
            department=self.level_11,
            staff=self.batman_deputy,
            role_id=DepartmentRoles.DEPUTY.value,
        )

        self.staff_01 = StaffFactory(login='staff_01', department=self.level_01, office=self.redrose)
        self.staff_03 = StaffFactory(login='staff_03', department=self.level_03, office=self.ekb)
        self.staff_13 = StaffFactory(login='staff_13', department=self.level_13)

        self.group = GroupFactory(name='batmans', intranet_status=1)
        GroupMembershipFactory(staff=self.batman, group=self.group)
        GroupMembershipFactory(staff=self.batman_deputy, group=self.group)

        # пересохраняем струтуру
        self.resave_objects()

    def resave_objects(self):
        for subobj, data in self.__dict__.items():
            if hasattr(data, 'id'):
                setattr(self, subobj, data.__class__.objects.get(id=data.id))
