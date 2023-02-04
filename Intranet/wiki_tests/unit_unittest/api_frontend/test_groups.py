from unittest import skipIf

from django.conf import settings
from ujson import loads

from wiki.users.models import Group
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase


class APIGroupsTest(BaseApiTestCase):
    """
    Тесты для групп
    """

    def setUp(self):
        super(APIGroupsTest, self).setUp()
        self.setUsers()
        self.setGroups()
        self.setGroupMembers()
        self.user = self.client.login('thasonic')

    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_show_group(self):
        group = Group.objects.get(url='yandex_mnt') if settings.IS_INTRANET else Group.objects.get(name='school')

        self.create_page(
            tag='TestGroups',
            supertag='testgroups',
        )

        request_url = '{api_url}/.groups/{group_id}'.format(
            api_url=self.api_url, group_id=group.dir_id if settings.IS_BUSINESS else group.id
        )

        assert_queries = 45 if not settings.WIKI_CODE == 'wiki' else 9
        with self.assertNumQueries(assert_queries):
            response = self.client.get(request_url)
        self.assertEqual(200, response.status_code)

        parsed_data = loads(response.content)['data']
        self.assertEqual(
            {'thasonic', 'kolomeetz'} if settings.IS_INTRANET else {'thasonic'},
            set([user['login'] for user in parsed_data['users']['data']]),
        )

        self.assertIn('name', parsed_data)
        if settings.IS_INTRANET:
            self.assertIn('url', parsed_data)
            self.assertIn('type', parsed_data)
            self.assertIn('externals_count', parsed_data)
        elif settings.IS_BUSINESS:
            self.assertIn('title', parsed_data)
            self.assertIn('dir_id', parsed_data)
            self.assertIn('type', parsed_data)
            self.assertNotIn('url', parsed_data)
            self.assertNotIn('externals_count', parsed_data)
        else:
            self.assertNotIn('url', parsed_data)
            self.assertNotIn('externals_count', parsed_data)
            self.assertNotIn('type', parsed_data)
            self.assertNotIn('dir_id', parsed_data)
            self.assertNotIn('title', parsed_data)

    if settings.IS_BUSINESS:

        @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
        def test_show_department(self):
            from wiki.users_biz.models import GROUP_TYPES

            group = Group.objects.get(url='yandex_mnt') if settings.IS_INTRANET else Group.objects.get(name='school')

            request_url = '{api_url}/.groups/{group_id}'.format(api_url=self.api_url, group_id=group.dir_id)

            response = self.client.get(request_url)
            self.assertEqual(200, response.status_code)

            parsed_data = loads(response.content)['data']
            self.assertEqual(parsed_data['type'], GROUP_TYPES[GROUP_TYPES.group])

            group.group_type = GROUP_TYPES.department
            group.save()

            request_url = '{api_url}/.groups/{group_id}'.format(api_url=self.api_url, group_id=group.dir_id)

            request_data = {'is_department': True}
            response = self.client.get(request_url, request_data)
            self.assertEqual(200, response.status_code)

            parsed_data = loads(response.content)['data']
            self.assertEqual(parsed_data['type'], GROUP_TYPES[GROUP_TYPES.department])
