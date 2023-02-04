from django.test import TestCase

from staff.lib.testing import (
    DepartmentFactory,
    OfficeFactory,
    GroupFactory,
    StaffFactory,
    GroupMembershipFactory,
)

from staff.security.tests.factories_test import (
    DataProfileFactory, AccessControlListFactory)

from staff.dismissal.models import Dismissal

from .factories import (
    DismissalFactory,
    CheckPointFactory,
    CheckPointTemplateFactory
)


class DismissalManagerTestCase(TestCase):

    def setUp(self):
        self.yandex = DepartmentFactory(name='yandex', parent=None)
        self.yamoney = DepartmentFactory(name='yamoney', parent=None)

        self.redrose = OfficeFactory(name='red rose')
        self.comode = OfficeFactory(name='comode')

        self.group = GroupFactory(name='HR', intranet_status=1)

        self.randomuser = StaffFactory()

        self.super = DataProfileFactory(
            own_department_only=False, own_office_only=False,
            department=None, office=None)
        self.own_dep = DataProfileFactory(
            own_department_only=True, own_office_only=False,
            department=None, office=None)
        self.redrose_only = DataProfileFactory(
            own_department_only=False, own_office_only=False,
            department=None, office=self.redrose)

    def test_ACL_rights(self):
        # """
        # Тесты на фильтрацию заявок при наличии ACL-прав
        # """
        super_hr = StaffFactory()
        redrose_hr = StaffFactory()

        super_hrs = GroupFactory(name='super HRs', intranet_status=1)
        GroupMembershipFactory(staff=super_hr, group=super_hrs)

        redrose_hrs = GroupFactory(name='red HRs', intranet_status=1)
        GroupMembershipFactory(staff=redrose_hr, group=redrose_hrs)

        acl = AccessControlListFactory(group=super_hrs)
        acl.dataprofiles.add(self.super)
        acl.save()

        acl = AccessControlListFactory(group=redrose_hrs)
        acl.dataprofiles.add(self.redrose_only)
        acl.save()

        dr1 = DismissalFactory(
            staff=self.randomuser,
            office=self.redrose,
            department=self.yandex
        )

        dr2 = DismissalFactory(
            staff=self.randomuser,
            office=self.comode,
            department=self.yamoney
        )

        dr3 = DismissalFactory(
            staff=redrose_hr,
            office=self.redrose,
            department=self.yandex
        )

        DismissalFactory(
            staff=super_hr,
            office=self.comode,
            department=self.yamoney
        )

        # первому доступны все (кроме себя самого)
        self.assertEqual(
            list(Dismissal.objects.available_for(super_hr)),
            [dr1, dr2, dr3]
        )

        # второму только заявки для Красной розы (но не на себя)
        self.assertEqual(
            list(Dismissal.objects.available_for(redrose_hr)),
            [dr1]
        )

        # а обычному пользователю недоступно ничего
        self.assertEqual(
            list(Dismissal.objects.available_for(self.randomuser)),
            []
        )

    def test_responsibility(self):
        # """
        # Тесты на фильтрацию заявок при наличии ответственности за пункт
        # """
        # helpdesk отвечает за пункт и должен иметь доступ к заявке
        helpdesk = StaffFactory()

        d1 = DismissalFactory(department=self.yandex, office=self.redrose)
        hd_tpl = CheckPointTemplateFactory()
        hd_tpl.responsibles.add(helpdesk)
        hd_tpl.save()

        CheckPointFactory(dismissal=d1, template=hd_tpl)

        self.assertEqual(
            list(Dismissal.objects.available_for(helpdesk)),
            [d1]
        )

        # а обычному пользователю недоступна заявка
        self.assertEqual(
            list(Dismissal.objects.available_for(self.randomuser)),
            []
        )

    def test_ACL_and_responsibility(self):
        # """
        # Тесты на фильтрацию заявок при наличии двух типов прав
        # """
        # руководитель хелпов видит как заявки своего департамента, так и те за
        # которые назначен ответсвенным
        help_dep = DepartmentFactory()
        helpdesk_head = StaffFactory(department=help_dep)

        # ACL
        heads = GroupFactory(name='heads', intranet_status=1)
        GroupMembershipFactory(staff=helpdesk_head, group=heads)

        acl = AccessControlListFactory(group=heads)
        acl.dataprofiles.add(self.own_dep)
        acl.save()

        d1 = DismissalFactory(department=help_dep)

        # responsibility
        d2 = DismissalFactory(department=self.yandex, office=self.redrose)
        tpl = CheckPointTemplateFactory()
        tpl.responsibles.add(helpdesk_head)
        tpl.save()

        CheckPointFactory(dismissal=d2, template=tpl)

        self.assertEqual(
            list(Dismissal.objects.available_for(helpdesk_head).order_by('id')),
            sorted([d1, d2], key=lambda d: d.id)
        )

        # а обычному пользователю так все и недоступно
        self.assertEqual(
            list(Dismissal.objects.available_for(self.randomuser)),
            []
        )
