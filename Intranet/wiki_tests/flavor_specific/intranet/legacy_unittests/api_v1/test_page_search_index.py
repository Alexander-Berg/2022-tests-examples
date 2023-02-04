# flake8: noqa: E501
# содержит длинные фикстуры

import calendar

from django.conf import settings

from wiki.api_v1.views import PageSearchIndexSerializer
from wiki.grids.utils import dummy_request_for_grids
from wiki.pages.logic.keywords import update_keywords
from wiki.pages.models import Page
from wiki.personalisation.user_cluster import create_personal_page
from wiki.utils.yandex_server_context import yaserver_context
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class PageSearchIndexViewTest(BaseApiTestCase):
    def setUp(self):
        super(PageSearchIndexViewTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')
        self.user = self.user_thasonic

    def test_page_meta_data(self):
        page = self.create_page(tag='Помидорка/Кабачок/Огурец', body='')

        update_keywords(page, self.user, ['vegans', 'лопата'])

        edited_timestamp = calendar.timegm(page.modified_at.timetuple())
        expected = {
            'title': 'Page',
            'usercluster': 0,
            'wiki_page_supertag': 'pomidorka/kabachok/ogurec',
            'wiki_page_tag': '\u041f\u043e\u043c\u0438\u0434\u043e\u0440\u043a\u0430/\u041a\u0430\u0431\u0430\u0447\u043e\u043a/\u041e\u0433\u0443\u0440\u0435\u0446',
            'wiki_page_url': '/pomidorka/kabachok/ogurec',
            'acl_users_whitelist': ['thasonic'],
            'ctime': calendar.timegm(page.created_at.timetuple()),
            'mtime': edited_timestamp,
            'descendants_count': 0,
            'editors': [{'count': 1, 'last_edit': edited_timestamp, 'login': 'thasonic'}],
            'favorited_count': 0,
            'public': 1,
            'linked_from_count': 0,
            'modifiers_count': 1,
            'owner_full_name': '\u0410\u043b\u0435\u043a\u0441\u0430\u043d\u0434\u0440 \u041f\u043e\u043a\u0430\u0442\u0438\u043b\u043e\u0432',
            'owner_full_name_en': 'Alexander Pokatilov',
            'owner_login': 'thasonic',
            'authors': [
                {
                    'author_full_name': '\u0410\u043b\u0435\u043a\u0441\u0430\u043d\u0434\u0440 \u041f\u043e\u043a\u0430\u0442\u0438\u043b\u043e\u0432',
                    'author_full_name_en': 'Alexander Pokatilov',
                    'author_login': 'thasonic',
                }
            ],
            'parents': [
                {'supertag': 'pomidorka'},
                {'supertag': 'pomidorka/kabachok'},
            ],
            'is_documentation': 0,
            'breadcrumbs': [
                {'name': 'pomidorka', 'url': '/pomidorka'},
                {'name': 'kabachok', 'url': '/pomidorka/kabachok'},
                {'name': '\u041e\u0433\u0443\u0440\u0435\u0446', 'url': '/pomidorka/kabachok/ogurec'},
            ],
            'cluster_one': 'pomidorka',
            'cluster_one_name': 'pomidorka',
            'cluster_one_supertag': 'pomidorka',
            'cluster_one_tag': 'pomidorka',
            'cluster_one_url': '/pomidorka',
            'comments_count': 0,
            'doc_group': 'wiki_%d' % page.id,
            'frontend_host': settings.NGINX_HOST,
            'files_count': 0,
            'keywords': ['vegans', 'лопата'],
            'is_official': 0,
            'acl_groups_whitelist': [str(self.group_yandex.id)],
        }

        actual = PageSearchIndexSerializer(context={'request': dummy_request_for_grids()}).load_yaserver_context(page)

        # флапает, так как может быть еще обновление после того как попало в индекс на следующей секунде

        del actual['mtime']
        del expected['mtime']

        self.assertEqual(expected, actual)

        # response = self.client.get('/_api/v1/pages/pomidorka/kabachok/ogurec/.search-index')
        # self.assertEqual(200, response.status_code)

    def test_302_for_redirect(self):
        self.client.use_cookie_auth()
        redirect = self.create_page(tag='redirect', body='')
        target = self.create_page(tag='target', body='')

        redirect.redirects_to = target
        redirect.save()

        response = self.client.get('/_api/v1/pages/redirect/.search-index')

        self.assertEqual(302, response.status_code)

    def test_is_user_cluster(self):
        """
        test meta-data for all 3 possible user cluster page tags
        """
        thasonic = self.user_thasonic.staff
        create_personal_page(self.user_thasonic)

        cluster0 = Page.objects.get(supertag='users/thasonic')
        cluster1 = self.create_page(tag='{0}'.format(thasonic.login))
        cluster2 = self.create_page(tag='{0}'.format(thasonic.wiki_name))

        self.assertEqual(yaserver_context(cluster0)['is_user_cluster'], 1)
        self.assertEqual(yaserver_context(cluster1)['is_user_cluster'], 1)
        self.assertEqual(yaserver_context(cluster2)['is_user_cluster'], 1)

        # subpages from user cluster must be marker as is_user_cluster too
        cluster3 = self.create_page(tag='users/thasonic/child0')
        cluster4 = self.create_page(tag='{0}/child1/subchild'.format(thasonic.login))
        cluster5 = self.create_page(tag='{0}/ребенок'.format(thasonic.wiki_name))

        self.assertEqual(yaserver_context(cluster3)['is_user_cluster'], 1)
        self.assertEqual(yaserver_context(cluster4)['is_user_cluster'], 1)
        self.assertEqual(yaserver_context(cluster5)['is_user_cluster'], 1)
