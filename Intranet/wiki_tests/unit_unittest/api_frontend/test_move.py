
from django.conf import settings
from django.contrib.auth import get_user_model
from ujson import loads

from wiki.pages.cluster import Cluster
from wiki.pages.models import Page
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase

User = get_user_model()


class APIMoveHandlerTest(BaseApiTestCase):
    """
    Тесты на перемещение отдельных страниц и кластеров в API
    """

    def setUp(self):
        super(APIMoveHandlerTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')
        self.cluster = Cluster('thasonic')
        self.user = User.objects.get(username='thasonic')

    def move_request(
        self,
        page,
        destination,
        move_cluster=False,
    ):
        request_url = '{api_url}/{supertag}/.move'.format(
            api_url=self.api_url,
            supertag=page.supertag,
        )
        return self.client.put(
            request_url,
            data={
                'destination': destination,
                'move_cluster': move_cluster,
            },
        )

    def check_response(self, response, expect_success=True):
        response_code = response.status_code
        if expect_success:
            self.assertEqual(response_code, 200)
            self.assertEqual(response.cookies['just_updated'].value, 'true')
        else:
            self.assertEqual(response_code, 409)

        content = loads(response.content)
        if expect_success:
            self.assertNotIn('error', content)
        else:
            self.assertIn('error', content)

    def create_page(self, **kwargs):
        if 'body' not in kwargs:
            kwargs['body'] = 'example text'  # to create wom0, wom1, body
        return super(APIMoveHandlerTest, self).create_page(**kwargs)

    def test_move_no_children_no_rename(self):
        self.create_page(tag='somewhere', supertag='somewhere')
        page = self.create_page(tag='one', supertag='one')
        child = self.create_page(tag='one/two', supertag='one/two')

        assert_queries = 87 if not settings.WIKI_CODE == 'wiki' else 45
        with self.assertNumQueries(assert_queries):
            response = self.move_request(
                page=page,
                destination='somewhere/one',
                move_cluster=False,
            )
        self.check_response(response)

        page, child = self.refresh_objects(page, child)

        self.assertEqual(page.supertag, 'somewhere/one')
        self.assertEqual(child.supertag, 'one/two')

    def test_move_with_children_no_rename(self):
        self.create_page(tag='somewhere', supertag='somewhere')
        page = self.create_page(tag='one', supertag='one')
        child = self.create_page(tag='one/two', supertag='one/two')

        assert_queries = 100 if not settings.WIKI_CODE == 'wiki' else 58
        with self.assertNumQueries(assert_queries):
            response = self.move_request(
                page=page,
                destination='somewhere/one',
                move_cluster=True,
            )
        self.check_response(response)
        page, child = self.refresh_objects(page, child)

        self.assertEqual(page.supertag, 'somewhere/one')
        self.assertEqual(child.supertag, 'somewhere/one/two')

    def test_move_no_children_with_rename(self):
        self.create_page(tag='somewhere', supertag='somewhere')
        page = self.create_page(tag='one', supertag='one')
        child = self.create_page(tag='one/two', supertag='one/two')

        response = self.move_request(
            page=page,
            destination='somewhere/first',
            move_cluster=False,
        )
        self.check_response(response)
        page, child = self.refresh_objects(page, child)

        self.assertEqual(page.supertag, 'somewhere/first')
        self.assertEqual(child.supertag, 'one/two')

    def test_move_with_children_with_rename(self):
        self.create_page(tag='somewhere', supertag='somewhere')
        page = self.create_page(tag='one', supertag='one')
        child = self.create_page(tag='one/two', supertag='one/two')

        response = self.move_request(
            page=page,
            destination='somewhere/first',
            move_cluster=True,
        )
        self.check_response(response)
        page, child = self.refresh_objects(page, child)

        self.assertEqual(page.supertag, 'somewhere/first')
        self.assertEqual(child.supertag, 'somewhere/first/two')

    def test_move_under_nonexistent_page(self):
        page = self.create_page(tag='one', supertag='one')

        response = self.move_request(
            page=page,
            destination='nowhere/first',
            move_cluster=False,
        )
        self.check_response(response)
        page = self.refresh_objects(page)

        self.assertEqual(page.supertag, 'nowhere/first')
        self.assertFalse(Page.objects.filter(supertag='nowhere').exists())

    def test_move_main_page(self):
        page = self.create_page(tag=settings.MAIN_PAGE, supertag=settings.MAIN_PAGE)

        response = self.move_request(
            page=page,
            destination='ahahaha/no',
            move_cluster=False,
        )

        self.assertEqual(response.status_code, 409)

    def test_move_metainformation(self):
        self.create_page(tag='TestMove')
        self.create_page(tag='TestMove/first')
        self.create_page(tag='TestMove/second')

        response = self.client.get('/_api/frontend/testmove/.move')
        self.assertEqual(200, response.status_code)

        data = loads(response.content)['data']
        self.assertEqual(
            data,
            {
                'subpages_count': 2,
            },
        )
