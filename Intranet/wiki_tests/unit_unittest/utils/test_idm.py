
from django.conf import settings
from django.contrib.auth.models import Group

from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase

if settings.IS_INTRANET:

    class IdmUserRoleCheckTests(BaseApiTestCase):
        def setUp(self):
            self.setUsers()
            self.client.login('thasonic')

            self.employee_group = Group.objects.get(name=settings.IDM_ROLE_EMPLOYEE_GROUP_NAME)
            self.external_group = Group.objects.create(name=settings.IDM_ROLE_EXTERNAL_GROUP_NAME)
            self.employee_group.user_set.remove(self.user_thasonic)
            self.employee_group.user_set.remove(self.user_chapson)

        def test_self_page_200_if_external(self):
            page = self.create_page(authors_to_add=[self.user_thasonic])

            self.external_group.user_set.add(self.user_thasonic)

            request_url = '{api_url}/{supertag}'.format(
                api_url=self.api_url,
                supertag=page.supertag,
            )
            response = self.client.get(request_url)

            self.assertEqual(200, response.status_code)

        def test_others_page_403_if_external(self):
            page = self.create_page(authors_to_add=[self.user_chapson])

            self.external_group.user_set.add(self.user_thasonic)

            request_url = '{api_url}/{supertag}'.format(
                api_url=self.api_url,
                supertag=page.supertag,
            )
            response = self.client.get(request_url)

            self.assertEqual(403, response.status_code)

        def test_others_page_200_if_employee(self):
            page = self.create_page(authors_to_add=[self.user_chapson])

            self.employee_group.user_set.add(self.user_thasonic)

            request_url = '{api_url}/{supertag}'.format(
                api_url=self.api_url,
                supertag=page.supertag,
            )
            response = self.client.get(request_url)

            self.assertEqual(200, response.status_code)

        def test_query_count_remains_the_same_without_idm_check(self):
            page = self.create_page(authors_to_add=[self.user_chapson])

            self.employee_group.user_set.add(self.user_thasonic)

            queries = 16 if settings.WIKI_CODE == 'wiki' else 14

            with self.assertNumQueries(queries):
                request_url = '{api_url}/{supertag}'.format(
                    api_url=self.api_url,
                    supertag=page.supertag,
                )
                self.client.get(request_url)

        def test_query_count_increments_with_idm_check(self):
            page = self.create_page(authors_to_add=[self.user_chapson])

            self.employee_group.user_set.add(self.user_thasonic)

            queries = 17 if settings.WIKI_CODE == 'wiki' else 15

            with self.assertNumQueries(queries):
                request_url = '{api_url}/{supertag}'.format(
                    api_url=self.api_url,
                    supertag=page.supertag,
                )
                self.client.get(request_url)
