from staff.security.models import DataProfile
from staff.security.managers import AvailableObjectsManager

from .factories_test import DataProfileFactory, AccessControlListFactory, SecurityTestCase

from staff.person.models import Staff


class AvailableObjectsManagerTestCase(SecurityTestCase):

    def test_handle_dataprofiles(self):
        DataProfileFactory(
            own_department_only=True,
            own_office_only=False,
            department=None,
            office=None
        )
        DataProfileFactory(
            own_department_only=False,
            own_office_only=False,
            department=None,
            office=self.redrose
        )

        acl = AccessControlListFactory(group=self.group)
        for data_profile in DataProfile.objects.all():
            acl.dataprofiles.add(data_profile)
        acl.save()

        q = AvailableObjectsManager().handle_dataprofiles(self.batman)

        assert set(
            Staff.objects.filter(q).values_list('login', flat=True)
        ) == {
            self.staff_01.login,
            self.staff_13.login,
            self.batman.login,
            self.batman_deputy.login,
        }
