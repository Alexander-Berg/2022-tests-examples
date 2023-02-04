from django.test import TestCase
from django.test.client import RequestFactory

from staff.person.models import Staff

from staff.lib.testing import (
    UserFactory,
    StaffFactory,
    DepartmentFactory,
    OfficeFactory,
    GroupFactory,
)
from staff.dismissal.services import DismissalService

from .factories import (
    ClearanceChitTemplateFactory,
)


class DismissalSrvTestCase(TestCase):

    def setUp(self):
        GroupFactory(
            name='Яндекс',
            parent=None,
            department=None,
            service_id=None,
        )

        self.factory = RequestFactory()

        self.yandex = DepartmentFactory(name='yandex', parent=None)
        self.shmyandex = DepartmentFactory(name='shmyandex', parent=None)
        self.morozov = OfficeFactory(name='morozov')
        self.mamontov = OfficeFactory(name='mamontov')

        self.staff = StaffFactory(login='mouse', department=self.yandex,
                                  office=self.morozov)
        self.hr_user = UserFactory()
        self.hr = StaffFactory(login='hr', user=self.hr_user)

        # всегда должен быть шаблон department:*, office: *
        self.t0 = ClearanceChitTemplateFactory(office=None, department=None)

    def test_get_chit_tpl(self):
        t1 = ClearanceChitTemplateFactory(department=self.yandex, office=None)
        t2 = ClearanceChitTemplateFactory(department=None, office=self.morozov)

        t = DismissalService.get_chit_template_for(
            Staff(department=self.shmyandex, office=self.morozov)
        )
        self.assertEqual(t, t2)

        t = DismissalService.get_chit_template_for(
            Staff(department=self.yandex, office=self.mamontov)
        )
        self.assertEqual(t, t1)

        t = DismissalService.get_chit_template_for(
            Staff(department=self.shmyandex, office=self.mamontov)
        )
        self.assertEqual(t, self.t0)
