
from intranet.wiki.tests.wiki_tests.common.base_testcase import BaseTestCase
from wiki import access
from wiki.api_frontend.serializers.autocomplete import AutocompletePageArraySerializer
from wiki.pages.cluster import Cluster
from wiki.pages.models import Page
from wiki.utils.injector import clear


class ClusterListTest(BaseTestCase):
    def test(self):
        self.setGroupMembers()
        self.cluster = Cluster('thasonic')
        self.user = self.user_thasonic

        self.create_page(tag='root', supertag='root')

        self.create_page(tag='root/middle1', supertag='root/middle1')
        self.create_page(tag='root/middle2', supertag='root/middle2')

        self.create_page(tag='root/middle1/leaf1', supertag='root/middle1/leaf1')
        self.create_page(tag='root/middle1/leaf2', supertag='root/middle1/leaf2')

        self.create_page(tag='root/middle2/leaf1', supertag='root/middle2/leaf1')
        self.create_page(tag='root/middle2/leaf2', supertag='root/middle2/leaf2')

        def _list(supertag):
            return AutocompletePageArraySerializer(supertag, context={'user': self.user}).data

        self.assertEqual(
            {
                'total': 1,
                'data': [{'url': '/root', 'supertag': 'root', 'tag': 'root', 'user_access': 'allowed-common'}],
            },
            _list('ro'),
        )
        self.assertEqual(
            {
                'total': 1,
                'data': [{'url': '/root', 'supertag': 'root', 'tag': 'root', 'user_access': 'allowed-common'}],
            },
            _list('root'),
        )
        self.assertEqual(
            {
                'total': 2,
                'data': [
                    {
                        'url': '/root/middle1',
                        'supertag': 'root/middle1',
                        'tag': 'root/middle1',
                        'user_access': 'allowed-common',
                    },
                    {
                        'url': '/root/middle2',
                        'supertag': 'root/middle2',
                        'tag': 'root/middle2',
                        'user_access': 'allowed-common',
                    },
                ],
            },
            _list('root/'),
        )
        self.assertEqual(
            {
                'total': 1,
                'data': [
                    {
                        'url': '/root/middle2',
                        'supertag': 'root/middle2',
                        'tag': 'root/middle2',
                        'user_access': 'allowed-common',
                    }
                ],
            },
            _list('root/middle2'),
        )
        self.assertEqual(
            {
                'total': 2,
                'data': [
                    {
                        'url': '/root/middle2/leaf1',
                        'supertag': 'root/middle2/leaf1',
                        'tag': 'root/middle2/leaf1',
                        'user_access': 'allowed-common',
                    },
                    {
                        'url': '/root/middle2/leaf2',
                        'supertag': 'root/middle2/leaf2',
                        'tag': 'root/middle2/leaf2',
                        'user_access': 'allowed-common',
                    },
                ],
            },
            _list('root/middle2/l'),
        )

        # Сделаем root/middle2/leaf1 закрытой для пользователя, она должна исчезнуть из списка.
        # Информация о доступе также должна отображаться в данных.

        page = Page.active.get(supertag='root/middle2/leaf1')
        page.authors.clear()
        page.authors.add(self.user_chapson)
        access.set_access(page, access.TYPES.OWNER, self.user_chapson)

        page = Page.active.get(supertag='root/middle2/leaf2')
        access.set_access(page, access.TYPES.OWNER, self.user_chapson)

        self.assertEqual(
            {
                'total': 1,
                'data': [
                    {
                        'url': '/root/middle2/leaf2',
                        'supertag': 'root/middle2/leaf2',
                        'tag': 'root/middle2/leaf2',
                        'user_access': 'allowed-restricted',
                    }
                ],
            },
            _list('root/middle2/l'),
        )

        clear()
