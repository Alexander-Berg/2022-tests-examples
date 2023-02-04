
from unittest import skipIf

import mock
import pretend
from django.conf import settings
from django.contrib.auth import get_user_model
from django.contrib.auth.models import AnonymousUser
from django.contrib.auth.models import Group as DjangoGroup
from django.core.cache import cache
from django.core.management import call_command

from wiki.access import TYPES, set_access
from wiki.pages import access
from wiki.pages.access import access_status
from wiki.pages.access.access_status import ACCESS_DENIED, _has_access
from wiki.pages.models import Access, Page, PageWatch
from wiki.utils import timezone
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase
from intranet.wiki.tests.wiki_tests.common.ddf_compat import get
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase

now = timezone.now()

if settings.IS_INTRANET:
    from wiki.intranet.models import Group, GroupMembership

    class AccessHandlerTestCase(BaseTestCase):
        """Tests for handler "Access" """

        def setUp(self):
            super(AccessHandlerTestCase, self).setUp()
            self.setUsers()
            self.setGroupMembers()
            self.setPages()
            self.client.login('thasonic')
            self.setExternalMember()
            # OMGOSH!!!!! DATS PAGE ALREADY HAS BEEN CREATED IN BaseTestCase.setUp
            # -> BaseTestCase.setPages
            self.testinfo_page = Page.objects.get(supertag='testinfo')
            self.testinfo_page.authors.add(self.user_thasonic)
            self.testinfo_page.save()
            gem_page = get(Page, supertag='testinfo/gem', tag='testinfo/gem', last_author=None)
            child_page = get(
                Page, tag='testinfo/gem/child', supertag='testinfo/gem/child', last_author=self.user_thasonic
            )
            child_page.authors.add(self.user_thasonic)
            grp = Group(id=1000, url='no_members', created_at=now, modified_at=now)
            grp.save()
            set_access(gem_page, TYPES.RESTRICTED, self.user_chapson, groups=[grp])
            set_access(child_page, TYPES.COMMON, self.user_chapson)
            cache.clear()  # чтобы доступы не кешировались

        def test_raw_access(self):
            interpreted_access = access.interpret_raw_access(access.get_raw_access('testinfo/gem/child'))
            self.assertTrue(interpreted_access['is_common'])  # It is common access
            self.assertFalse(interpreted_access['is_owner'])  # It is common access
            self.assertEqual(len(interpreted_access['groups']), 0)  # Must be no groups
            self.assertTrue(len(interpreted_access['users']) == 0, 'Must be no users')
            self.assertEqual(
                interpreted_access['is_inherited'], False, 'it is not inherited, but set straightly on the page'
            )
            self.assertEqual(interpreted_access['is_anonymous'], False, "We don't allow anonymous access")
            self.assertEqual(interpreted_access['is_restricted'], False, 'It is free access to this page')

        def test_access_status(self):
            """
            Тест функции access.get_bulk_access_status,
            надо чтобы она совпадала с access.get_access_status"""
            page = Page.objects.get(supertag='testinfo/gem/child')
            access_status = access.get_access_status(page.supertag, self.user_thasonic)
            self.assertEqual(access_status, access.ACCESS_COMMON)

            access_status = access.get_bulk_access_status([page.supertag], self.user_thasonic)
            self.assertEqual(access_status[page.supertag], access.ACCESS_COMMON)

        def test_owner_not_in_yandex_group(self):
            tag = 'IAmFromDirectorBoard'
            get(Page, tag=tag, supertag='iamfromdirectorboard', authors=[self.user_snegovoy], page_type=Page.TYPES.PAGE)
            self.assertEqual(
                access.get_access_status(tag, self.user_snegovoy),
                access.ACCESS_COMMON,
                'Must be common access because user is owner',
            )
            tag = 'IAmFromDirectorBoard/opened_to_owner'
            page_to_owner = get(
                Page,
                tag=tag,
                supertag='iamfromdirectorboard/openedtoowner',
                authors=[self.user_snegovoy],
                page_type=Page.TYPES.PAGE,
            )
            set_access(page_to_owner, TYPES.RESTRICTED, self.user_chapson, staff_models=[self.user_snegovoy.staff])
            self.assertEqual(
                access.get_access_status(tag, self.user_snegovoy), access.ACCESS_RESTRICTED, 'Must be restricted access'
            )
            tag = 'IAmFromDirectorBoard/thasonic_please_reaD_this'
            page_to_thasonic = self.create_page(
                tag='iamfromdirectorboard/thasÅonicpleasereadthis',
            )
            page_to_thasonic.authors.add(self.user_snegovoy)
            set_access(page_to_thasonic, TYPES.RESTRICTED, self.user_chapson, staff_models=[self.user_snegovoy.staff])
            self.assertEqual(
                access.get_access_status(tag, self.user_snegovoy), access.ACCESS_RESTRICTED, 'Must be restricted access'
            )

        def test_owner_non_existing_page(self):
            testinfo_page = Page.objects.get(supertag='testinfo/testinfogem')
            testinfo_page.authors.add(self.user_thasonic)
            set_access(testinfo_page, TYPES.OWNER, self.user_chapson)
            non_existing_page = testinfo_page.tag + '/subpage'
            self.assertEqual(
                access.has_access(non_existing_page, self.user_thasonic),
                True,
                "Thasonic is author of '%s' so he must have access" % testinfo_page.tag,
            )

        def test_pagewatches(self):
            """Page testinfo/gem inherits from testinfo.
            thasonic is subscribed to testinfo AND to testinfo/bla.
            chapson changes access to testinfo to "only me"

            result: thasonic is not subscribed to testinfo/bla and testinfo
            """
            testinfo = Page.objects.get(supertag='testinfo')
            testinfo_bla = Page.objects.get(supertag='testinfo/bla')
            get(PageWatch, user='thasonic', page=testinfo)
            get(PageWatch, user='thasonic', page=testinfo_bla)
            self.client.login('chapson')
            set_access(testinfo, TYPES.OWNER, self.user_chapson)
            self.assertEqual(PageWatch.objects.filter(user='chapson', page=testinfo_bla).count(), 0)
            self.assertEqual(PageWatch.objects.filter(user='chapson', page=testinfo).count(), 0)

        def test_killing_subscriptions_from_management_command(self):
            testinfo = Page.objects.get(supertag='testinfo')
            testinfo_bla = Page.objects.get(supertag='testinfo/bla')
            get(PageWatch, user='thasonic', page=testinfo)
            get(PageWatch, user='thasonic', page=testinfo_bla)
            self.client.login('chapson')
            set_access(testinfo, TYPES.OWNER, self.user_chapson)
            call_command('gentle_killer_of_extended_permissions')
            self.assertEqual(PageWatch.objects.filter(user='chapson', page=testinfo_bla).count(), 0)
            self.assertEqual(PageWatch.objects.filter(user='chapson', page=testinfo).count(), 0)

        def test_grant_to_yandex(self):
            """Можно выбрать "ограниченный доступ" и выдать группе Яндекс

            Такой юзкейс используется, если хочется дать доступ одному внешнему консультанту,
            сохранив общий доступ всем сотрудникам Яндекса.
            Метод set_access не должен бросать исключения NoChanges
            """
            page = self.create_page(tag='АрктангенсИзАрканзаса')
            set_access(page, TYPES.RESTRICTED, self.user_thasonic, staff_models=[self.user_kolomeetz.staff])
            set_access(
                page,
                TYPES.RESTRICTED,
                self.user_thasonic,
                staff_models=[self.user_kolomeetz.staff],
                groups=[access.yandex_group()],
            )  # исключения NoChanges быть не должно
            interpreted_access = access.interpret_raw_access(access.get_raw_access(page.tag))
            self.assertTrue(access.yandex_group() in interpreted_access['groups'])

            set_access(page, TYPES.OWNER, self.user_thasonic)
            set_access(
                page, TYPES.RESTRICTED, self.user_thasonic, groups=[access.yandex_group()]
            )  # исключения NoChanges быть не должно
            interpreted_access = access.interpret_raw_access(access.get_raw_access(page.tag))
            self.assertTrue(access.yandex_group() in interpreted_access['groups'])

        def test_robot_has_access(self):
            """Спрашиваем права доступа робота к странице"""
            page = self.create_page(tag='АрктангенсИзАрканзаса')
            robot = self.get_or_create_user('_rpc_user')
            set_access(page, TYPES.RESTRICTED, self.user_chapson, staff_models=[robot.staff])
            access.get_bulk_access_status([page.supertag], robot)
            access.has_access(page.supertag, robot)

        def test_restricted_access_for_external_users_from_default(self):
            """Доступ к странице в этом тесте должен стать RESTRICTED

            Создаем страницу, доступ выдаем только пользователям не из группы Яндекс.
            wiki.pages.access.access.interpret_raw_access должна отвечать, что доступ к этой странице - restricted
            """
            (GroupMembership.objects.filter(staff=self.user_kolomeetz.staff).delete())
            page = self.create_page(tag='АрктангенсИзАрканзаса')
            set_access(page, TYPES.RESTRICTED, self.user_thasonic, staff_models=[self.user_kolomeetz.staff])

            bulk_raw_access = access.get_bulk_raw_access([page])

            interpreted_access = access.interpret_raw_access(bulk_raw_access[page])
            self.assertTrue(interpreted_access['is_restricted'])

            single_raw_access = access.get_raw_access(page.tag)

            self.assertEqual(single_raw_access, bulk_raw_access[page])

            self.assertEqual(
                access.ACCESS_RESTRICTED,
                access.get_bulk_access_status([page.supertag], self.user_kolomeetz)[page.supertag],
            )

            self.assertEqual(
                access.ACCESS_RESTRICTED,
                access.get_bulk_access_status([page.supertag], self.user_thasonic)[page.supertag],
            )

        def test_restricted_access_for_external_user_from_explicitly_common(self):
            """Доступ к странице был явно для всех, стал ограниченный для внешнего консультанта."""
            (GroupMembership.objects.filter(staff=self.user_chapson.staff).delete())
            page = self.create_page(tag='АрктангенсИзАрканзаса/Подстраница')
            set_access(page, TYPES.COMMON, self.user_thasonic)

            bulk_raw_access = access.get_bulk_raw_access([page])
            interpreted_access = access.interpret_raw_access(bulk_raw_access[page])
            self.assertTrue(interpreted_access['is_common'])
            self.assertTrue(access.has_access(page.supertag, self.user_kolomeetz))

            set_access(page, TYPES.RESTRICTED, self.user_thasonic, staff_models=[self.user_chapson.staff])

            bulk_raw_access = access.get_bulk_raw_access([page])
            interpreted_access = access.interpret_raw_access(bulk_raw_access[page])
            self.assertFalse(interpreted_access['is_common'])
            self.assertTrue(interpreted_access['is_restricted'])

            # Доступы не должны браться из кеша.
            cache.clear()
            self.assertFalse(access.has_access(page.supertag, self.user_kolomeetz))

        def test_no_double_requests(self):
            """Если доступ внешним сотрудникам выдан, заявки на них не создаются."""

        def test_grant_to_dismissed_user(self):
            """
            Доступ выдается бывшему сотруднику, который не состоит в группе Яндекс.
            """
            self.employee_group = DjangoGroup.objects.get(name=settings.IDM_ROLE_EMPLOYEE_GROUP_NAME)
            self.employee_group.user_set.remove(self.user_chapson)

            page = self.create_page(tag='АрктангенсИзАрканзаса')
            self.assertEqual(_has_access(page.supertag, self.user_chapson), ACCESS_DENIED)
            set_access(page, TYPES.RESTRICTED, self.user_thasonic, staff_models=[self.user_chapson.staff])
            self.assertNotEqual(_has_access(page.supertag, self.user_chapson), ACCESS_DENIED)

    class AccessInheritanceTest(BaseTestCase):
        """
        Проверяем, что страницы правильно наследуют правила доступа.
        """

        def setUp(self):
            super(AccessInheritanceTest, self).setUp()
            self.setGroupMembers()

            self.root = self.create_page(tag='Root', supertag='root')
            self.middle = self.create_page(tag='Root/Middle', supertag='root/middle')
            self.child = self.create_page(tag='Root/Middle/Child', supertag='root/middle/child')

            cache.clear()  # чтобы доступы не кешировались

        def test_inherit_self_access(self):
            # Наследование своих правил доступа.
            set_access(self.root, TYPES.OWNER, self.user_thasonic)
            set_access(self.middle, TYPES.COMMON, self.user_thasonic)
            set_access(self.child, TYPES.RESTRICTED, self.user_thasonic, staff_models=[self.user_kolomeetz.staff])

            status = access.get_bulk_access_status([self.child.supertag], self.user_kolomeetz)

            self.assertEqual(access.ACCESS_RESTRICTED, status[self.child.supertag])

        def test_inherit_middle_parent_access(self):
            # Наследование правил доступа ближайшего предка.
            set_access(self.root, TYPES.OWNER, self.user_thasonic)
            set_access(self.middle, TYPES.COMMON, self.user_thasonic)

            status = access.get_bulk_access_status([self.child.supertag], self.user_chapson)

            self.assertEqual(access.ACCESS_COMMON, status[self.child.supertag])

        def test_inherit_root_parent_access(self):
            # Наследование правил доступа корневого предка.
            set_access(self.root, TYPES.OWNER, self.user_thasonic)

            status = access.get_bulk_access_status([self.child.supertag], self.user_chapson)

            self.assertEqual(access.ACCESS_DENIED, status[self.child.supertag])

        def test_inherit_default_access(self):
            # Правила доступа не определены ни у самой страницы, ни у ее предков.
            status = access.get_bulk_access_status([self.child.supertag], self.user_chapson)

            self.assertEqual(access.ACCESS_COMMON, status[self.child.supertag])

    class AccessUnitTest(BaseApiTestCase):
        def test_users_have_access_to_new_pages(self):
            from wiki.intranet.models.consts import AFFILIATION

            external_user = pretend.stub(
                is_authenticated=lambda: True, staff=pretend.stub(affiliation=AFFILIATION.EXTERNAL)
            )
            self.assertNotEqual(
                _has_access('PageWhichNeverExisted', external_user),
                ACCESS_DENIED,
                'External users may create top-level pages',
            )
            internal_user = pretend.stub(
                is_authenticated=lambda: True, staff=pretend.stub(affiliation=AFFILIATION.YANDEX)
            )
            self.assertNotEqual(
                _has_access('PageWhichNeverExisted', internal_user),
                ACCESS_DENIED,
                'Yandex users may create top-level pages',
            )
            internal_user = pretend.stub(
                is_authenticated=lambda: True, staff=pretend.stub(affiliation=AFFILIATION.YAMONEY)
            )
            self.assertNotEqual(
                _has_access('PageWhichNeverExisted', internal_user),
                ACCESS_DENIED,
                'Yandex Money users may create top-level pages',
            )

        def test_parent_of_new_page(self):
            def selective_raiser(supertag):
                if supertag == 'testinfo/testinfogem/newpage':
                    return None
                raise ValueError('kakoy bred!')

            def get_parent(supertag):
                if supertag != 'testinfo/testinfogem/newpage':
                    raise ValueError('kakoy bred!')
                return pretend.stub(tag='testinfo/testinfogem', supertag='testinfo/testinfogem', get_authors=lambda: [])

            def user_django_groups(user):
                return []

            external_user = pretend.stub(
                is_authenticated=lambda: True,
                staff=pretend.stub(
                    all_groups=[],
                    user=pretend.stub(),
                ),
                all_groups=[],
            )

            with mock.patch('wiki.pages.access.access_status._get_page', selective_raiser):
                with mock.patch('wiki.pages.access.access_status.hierarchy.get_nearest_existing_parent', get_parent):
                    with mock.patch('wiki.pages.access.groups.user_django_groups', user_django_groups):
                        self.assertEqual(
                            _has_access('testinfo/testinfogem/new_page', external_user),
                            ACCESS_DENIED,
                            'External user can not access non existent subpage of common access',
                        )


else:

    class AnonymousAccessTest(BaseTestCase):
        """
        Актуально для школьных вик — там есть спецрежим доступа:
        «Анонимный может читать, залогиненный может писать».
        """

        def setUp(self):
            super(AnonymousAccessTest, self).setUp()
            self.setPages()
            self.homepage = Page.objects.get(supertag='homepage')
            User = get_user_model()
            self.user = User()
            self.user_anonymous = AnonymousUser()

        def test_anonymous_can_read_anonymous_open_page(self):
            Access.objects.create(page=self.homepage, is_anonymous=True)

            access = _has_access(
                tag=self.homepage.supertag,
                user=self.user_anonymous,
                privilege='read',
            )
            self.assertEqual(access, access_status.ACCESS_COMMON)

        @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
        def test_anonymous_cant_write_anonymous_open_page(self):
            Access.objects.create(page=self.homepage, is_anonymous=True)

            access = _has_access(
                tag=self.homepage.supertag,
                user=self.user_anonymous,
                privilege='write',
            )
            self.assertEqual(access, access_status.ACCESS_DENIED)

        def test_authorized_can_read_anonymous_open_page(self):
            Access.objects.create(page=self.homepage, is_anonymous=True)

            access = _has_access(
                tag=self.homepage.supertag,
                user=self.user,
                privilege='read',
            )
            self.assertEqual(access, access_status.ACCESS_COMMON)

        def test_authorized_can_write_anonymous_open_page(self):
            Access.objects.create(page=self.homepage, is_anonymous=True)

            access = _has_access(
                tag=self.homepage.supertag,
                user=self.user,
                privilege='write',
            )
            self.assertEqual(access, access_status.ACCESS_COMMON)
