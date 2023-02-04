from django.conf import settings
from django.contrib.auth import get_user_model
from ujson import loads

from wiki.sync.connect import OPERATING_MODE_NAMES
from wiki.sync.connect.models import OperatingMode
from wiki.org import get_user_orgs, org_ctx, org_user
from wiki.pages.logic.hierarchy import get_nearest_existing_parent, get_parent
from wiki.pages.models import Page, Revision
from wiki.utils.timezone import now
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase

User = get_user_model()
"""
Различные тесты на то, что бекенд умеет работать со множеством организаций.
"""

if settings.IS_BUSINESS:

    class WikiApiViewsInOrgCtxTests(BaseApiTestCase):
        def setUp(self):
            super(WikiApiViewsInOrgCtxTests, self).setUp()

            self.org_7 = self.get_or_create_org(dir_id='7')
            self.org_42 = self.get_or_create_org(dir_id='42')

            self.setUsers()
            self.client.login('thasonic')

        def test_wiki_api_views_get(self):
            # Проверяем, что все view, являющиеся наследниками WikiApiView,
            # берут контекст организации из request.org, на примере GET ручки .raw.

            org_7 = self.org_7
            org_42 = self.org_42

            page = Page(supertag='page', tag='page', org=org_7, last_author=self.user_thasonic, modified_at=now())
            page.body = 'Страница организации 7'
            page.save()
            page.authors.add(self.user_thasonic)
            Revision.objects.create_from_page(page)

            page = Page(supertag='page', tag='page', org=org_42, last_author=self.user_thasonic, modified_at=now())
            page.body = 'Страница организации 42'
            page.save()
            page.authors.add(self.user_thasonic)
            Revision.objects.create_from_page(page)

            request_url = '{api_url}/page/.raw'.format(
                api_url=self.api_url,
            )

            response = self.client.get(request_url)

            self.assertEqual(200, response.status_code)
            data = loads(response.content)['data']
            self.assertEqual('Страница организации 42', data['body'])

        def test_wiki_api_views_post(self):
            # Проверяем, что все view, являющиеся наследниками WikiApiView,
            # берут контекст организации из request.org, на примере POST ручки .redirect.

            org_7 = self.org_7
            org_42 = self.org_42

            page = Page(
                supertag='redirect', tag='redirect', org=org_7, last_author=self.user_thasonic, modified_at=now()
            )
            page.body = 'Редирект организации 7'
            page.save()
            page.authors.add(self.user_thasonic)
            Revision.objects.create_from_page(page)

            page = Page(supertag='page', tag='page', org=org_7, last_author=self.user_thasonic, modified_at=now())
            page.body = 'Страница организации 7'
            page.save()
            page.authors.add(self.user_thasonic)
            Revision.objects.create_from_page(page)

            page = Page(
                supertag='redirect', tag='redirect', org=org_42, last_author=self.user_thasonic, modified_at=now()
            )
            page.body = 'Редирект организации 42'
            page.save()
            page.authors.add(self.user_thasonic)
            Revision.objects.create_from_page(page)

            page = Page(supertag='page', tag='page', org=org_42, last_author=self.user_thasonic, modified_at=now())
            page.body = 'Страница организации 42'
            page.save()
            page.authors.add(self.user_thasonic)
            Revision.objects.create_from_page(page)

            request_url = '{api_url}/redirect/.redirect'.format(
                api_url=self.api_url,
            )

            response = self.client.post(request_url, data={'redirect_to_tag': 'page'})
            self.assertEqual(200, response.status_code)

            redirect_42 = Page.active.get(supertag='redirect', org=org_42)
            page_42 = Page.active.get(supertag='page', org=org_42)

            self.assertEqual(redirect_42.redirect_target().id, page_42.id)

            redirect_7 = Page.active.get(supertag='redirect', org=org_7)

            self.assertIsNone(redirect_7.redirects_to)

    class PageHierarchyInOrgCtxTests(BaseTestCase):
        def setUp(self):
            super(PageHierarchyInOrgCtxTests, self).setUp()

            self.setUsers()

            self.org_7 = self.get_or_create_org(dir_id='7')
            self.org_42 = self.get_or_create_org(dir_id='42')

            page = Page(supertag='root', tag='root', modified_at=now())
            page.save()
            page.authors.add(self.user_chapson)
            self.page_root_without_org = page

            page = Page(supertag='root/middle', tag='root/middle', modified_at=now())
            page.save()
            page.authors.add(self.user_chapson)
            self.page_middle_without_org = page

            page = Page(supertag='root/middle/leaf', tag='root/middle/leaf', modified_at=now())
            page.save()
            page.authors.add(self.user_chapson)
            self.page_leaf_without_org = page

            page = Page(supertag='root', tag='root', org=self.org_7, modified_at=now())
            page.save()
            page.authors.add(self.user_chapson)
            self.page_root_org_7 = page

            page = Page(supertag='root/middle/leaf', tag='root/middle/leaf', org=self.org_7, modified_at=now())
            page.save()
            page.authors.add(self.user_chapson)
            self.page_leaf_org_7 = page

            page = Page(supertag='root/middle', tag='root/middle', org=self.org_42, modified_at=now())
            page.save()
            page.authors.add(self.user_chapson)
            self.page_middle_org_42 = page

            page = Page(supertag='root/middle/leaf', tag='root/middle/leaf', org=self.org_42, modified_at=now())
            page.save()
            page.authors.add(self.user_chapson)
            self.page_leaf_org_42 = page

        def test_get_descendants_from_manager(self):
            with org_ctx(self.org_7):
                pages = Page.active.get_descendants('root')
                self.assertEqual(1, len(pages))
                self.assertEqual(self.page_leaf_org_7.id, pages[0].id)

                pages = Page.active.get_descendants('root', include_page=True)
                self.assertEqual(2, len(pages))
                self.assertEqual(self.page_root_org_7.id, pages[0].id)
                self.assertEqual(self.page_leaf_org_7.id, pages[1].id)

            with org_ctx(self.org_42):
                pages = Page.active.get_descendants('root')
                self.assertEqual(2, len(pages))
                self.assertEqual(self.page_middle_org_42.id, pages[0].id)
                self.assertEqual(self.page_leaf_org_42.id, pages[1].id)

                pages = Page.active.get_descendants('root', include_page=True)
                self.assertEqual(2, len(pages))
                self.assertEqual(self.page_middle_org_42.id, pages[0].id)
                self.assertEqual(self.page_leaf_org_42.id, pages[1].id)

        def test_get_descendants_from_page(self):
            # При запросе descendants через объект страницы нужно фильтровать по организации самой страницы.

            # У модели свой контекст организации с организацией, взятой из нее.
            # Внешний контекст не должен повлиять на нее.
            with org_ctx(self.org_42):
                self.assertEqual(2, len(self.page_root_without_org.descendants))

                pages = self.page_root_org_7.descendants
                self.assertEqual(1, len(pages))
                self.assertEqual(self.page_leaf_org_7.id, pages[0].id)

            # У модели свой контекст организации с организацией, взятой из нее.
            # Внешний контекст не должен повлиять на нее.
            with org_ctx(self.org_7):
                pages = self.page_middle_org_42.descendants
                self.assertEqual(1, len(pages))
                self.assertEqual(self.page_leaf_org_42.id, pages[0].id)

        def test_get_parent_from_page(self):
            # При запросе parent через объект страницы нужно фильтровать по организации самой страницы.

            # У модели свой контекст организации с организацией, взятой из нее.
            # Внешний контекст не должен повлиять на нее.
            with org_ctx(self.org_42):
                self.assertIsNone(get_parent(self.page_root_without_org))
                self.assertEqual(self.page_root_without_org.id, get_parent(self.page_middle_without_org).id)
                self.assertEqual(self.page_middle_without_org.id, get_parent(self.page_leaf_without_org).id)

                self.assertIsNone(get_parent(self.page_root_org_7))
                self.assertIsNone(get_parent(self.page_leaf_org_7))

            # У модели свой контекст организации с организацией, взятой из нее.
            # Внешний контекст не должен повлиять на нее.
            with org_ctx(self.org_7):
                self.assertIsNone(get_parent(self.page_middle_org_42))
                self.assertEqual(self.page_middle_org_42.id, get_parent(self.page_leaf_org_42).id)

        def test_get_parent_from_supertag(self):
            # При запросе parent через супертэг страницы нужно фильтровать по организации из запроса.

            with org_ctx(self.org_42):
                # Выглядит странно, но так должно работать.
                self.assertEqual(self.page_middle_org_42, get_parent(self.page_leaf_org_7.supertag))

        def test_get_nearest_parent_from_page(self):
            # При запросе nearest_existing_parent через объект страницы нужно фильтровать по организации самой страницы.

            # У модели свой контекст организации с организацией, взятой из нее.
            # Внешний контекст не должен повлиять на нее.
            with org_ctx(self.org_42):
                self.assertIsNone(get_nearest_existing_parent(self.page_root_without_org))
                self.assertEqual(
                    self.page_root_without_org.id, get_nearest_existing_parent(self.page_middle_without_org).id
                )
                self.assertEqual(
                    self.page_middle_without_org.id, get_nearest_existing_parent(self.page_leaf_without_org).id
                )

                self.assertIsNone(get_nearest_existing_parent(self.page_root_org_7))
                self.assertEqual(self.page_root_org_7.id, get_nearest_existing_parent(self.page_leaf_org_7).id)

            # У модели свой контекст организации с организацией, взятой из нее.
            # Внешний контекст не должен повлиять на нее.
            with org_ctx(self.org_7):
                self.assertIsNone(get_nearest_existing_parent(self.page_middle_org_42))
                self.assertEqual(self.page_middle_org_42.id, get_nearest_existing_parent(self.page_leaf_org_42).id)

        def test_get_nearest_parent_from_supertag(self):
            # При запросе nearest_existing_parent через супертэг страницы нужно фильтровать по организации из запроса.

            with org_ctx(self.org_7):
                # Выглядит странно, но так должно работать.
                self.assertEqual(self.page_root_org_7, get_nearest_existing_parent(self.page_leaf_org_42.supertag))

        def test_get_ancestors(self):
            # У модели свой контекст организации с организацией, взятой из нее.
            # Внешний контекст не должен повлиять на нее.
            with org_ctx(self.org_42):
                self.assertEqual(0, len(self.page_root_without_org.ancestors))

                self.assertEqual(1, len(self.page_middle_without_org.ancestors))

                self.assertEqual(2, len(self.page_leaf_without_org.ancestors))

                self.assertEqual(0, len(self.page_root_org_7.ancestors))

                pages = self.page_leaf_org_7.ancestors
                self.assertEqual(1, len(pages))
                self.assertEqual(self.page_root_org_7.id, pages[0].id)

            # У модели свой контекст организации с организацией, взятой из нее.
            # Внешний контекст не должен повлиять на нее.
            with org_ctx(self.org_7):
                self.assertEqual(0, len(self.page_middle_org_42.ancestors))

                pages = self.page_leaf_org_42.ancestors
                self.assertEqual(1, len(pages))
                self.assertEqual(self.page_middle_org_42.id, pages[0].id)

        def test_get_breadcrumbs(self):
            # У модели свой контекст организации с организацией, взятой из нее.
            # Внешний контекст не должен повлиять на нее.
            with org_ctx(self.org_42):
                self.assertEqual(
                    [
                        {'url': '/root', 'tag': 'root', 'is_active': True, 'name': 'root', 'title': ''},
                        {'url': '/root/middle', 'tag': 'root/middle', 'is_active': True, 'name': 'middle', 'title': ''},
                        {
                            'url': '/root/middle/leaf',
                            'tag': 'root/middle/leaf',
                            'is_active': True,
                            'name': 'leaf',
                            'title': '',
                        },
                    ],
                    self.page_leaf_without_org.breadcrumbs,
                )
                self.assertEqual(
                    [
                        {'url': '/root', 'tag': 'root', 'is_active': True, 'name': 'root', 'title': ''},
                        {
                            'url': '/root/middle',
                            'tag': 'root/middle',
                            'is_active': False,
                            'name': 'middle',
                            'title': 'middle',
                        },
                        {
                            'url': '/root/middle/leaf',
                            'tag': 'root/middle/leaf',
                            'is_active': True,
                            'name': 'leaf',
                            'title': '',
                        },
                    ],
                    self.page_leaf_org_7.breadcrumbs,
                )

            # У модели свой контекст организации с организацией, взятой из нее.
            # Внешний контекст не должен повлиять на нее.
            with org_ctx(self.org_7):
                self.assertEqual(
                    [
                        {'url': '/root', 'tag': 'root', 'is_active': False, 'name': 'root', 'title': 'root'},
                        {'url': '/root/middle', 'tag': 'root/middle', 'is_active': True, 'name': 'middle', 'title': ''},
                    ],
                    self.page_middle_org_42.breadcrumbs,
                )

    class FilterUserByOrgTests(BaseTestCase):
        def setUp(self):
            OperatingMode.objects.get_or_create(name=OPERATING_MODE_NAMES.free)

        def test_org_user(self):
            org_7 = self.get_or_create_org(dir_id='7')
            org_42 = self.get_or_create_org(dir_id='42')

            User.objects.create_user('gleb1', 'gleb1@yandex-team.ru', '1')

            user_org_7 = User.objects.create_user('gleb2', 'gleb2@yandex-team.ru', '1')
            user_org_7.orgs.set([org_7])
            user_org_7.save()

            user_org_42 = User.objects.create_user('gleb3', 'gleb3@yandex-team.ru', '1')
            user_org_42.orgs.set([org_42])
            user_org_42.save()

            user_org_7_and_42 = User.objects.create_user('gleb4', 'gleb4@yandex-team.ru', '1')
            user_org_7_and_42.orgs.set([org_7, org_42])
            user_org_7_and_42.save()

            self.assertEqual(4, User.objects.filter(username__startswith='gleb').count())

            users = org_user().filter(username__startswith='gleb')

            with org_ctx(org_7):
                users = org_user().filter(username__startswith='gleb')
                self.assertEqual(2, len(users))
                self.assertEqual(user_org_7.id, users[0].id)
                self.assertEqual(user_org_7_and_42.id, users[1].id)

                self.assertEqual(user_org_7.id, org_user().get(username='gleb2').id)

            with org_ctx(org_42):
                users = org_user().filter(username__startswith='gleb')
                self.assertEqual(2, len(users))
                self.assertEqual(user_org_42.id, users[0].id)
                self.assertEqual(user_org_7_and_42.id, users[1].id)

                self.assertEqual(user_org_42.id, org_user().get(username='gleb3').id)

        def get_user_orgs(self):
            org_7 = self.get_or_create_org(dir_id='7')
            org_42 = self.get_or_create_org(dir_id='42')

            user = User.objects.create_user('gleb', 'gleb@yandex-team.ru', '1')

            self.assertEqual([], get_user_orgs(user))

            user.orgs.set([org_7])
            user.save()

            orgs = get_user_orgs(user)

            self.assertEqual(1, len(orgs))
            self.assertEqual(org_7.id, orgs[0].id)

            user.orgs.set([org_7, org_42])
            user.save()

            orgs = get_user_orgs(user)

            self.assertEqual(2, len(orgs))
            self.assertEqual(org_7.id, orgs[0].id)
            self.assertEqual(org_42.id, orgs[1].id)
