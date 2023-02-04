from django.contrib.auth import get_user_model

from wiki.grids.logic import operations
from wiki.grids.models import Grid
from wiki.pages.models import PageWatch
from wiki.users.logic import set_user_setting

from intranet.wiki.tests.wiki_tests.common.wiki_django_testcase import WikiDjangoTestCase
from intranet.wiki.tests.wiki_tests.unit_unittest.grids import base


class CloneTest(WikiDjangoTestCase):
    def setUp(self):
        self.user_one = get_user_model().objects.create(username='user_one')
        self.user_two = get_user_model().objects.create(username='user_two')
        self.original = base.create_grid(
            structure=base.GRID_STRUCTURE,
            tag='original',
            owner=self.user_one,
            title='Оригинал',
            formatter_version='300',
            description='I am real',
            keywords='help',
            data=[
                {
                    'name': 'i am name',
                    'date': '2013-01-05',
                    'is_done': True,
                }
            ],
        )

    def _do_clone(self, destination_tag='Клон', with_data=False):
        operations.clone(
            grid=self.original,
            user=self.user_one,
            destination_tag=destination_tag,
            with_data=with_data,
        )
        return Grid.objects.get(tag=destination_tag)

    def test_attrs_correctly_cloned(self):
        cloned = self._do_clone()

        self.assertEqual(cloned.supertag, 'klon')
        self.assertEqual(list(cloned.get_authors()), [self.user_one])
        self.assertEqual(cloned.last_author, self.user_one)
        self.assertEqual(cloned.title, 'Оригинал')
        self.assertEqual(cloned.page_type, Grid.TYPES.GRID)
        self.assertEqual(cloned.formatter_version, '300')
        self.assertEqual(cloned.description, 'I am real')
        self.assertEqual(cloned.keywords, 'help')

    def test_structure_correctly_cloned(self):
        cloned = self._do_clone()

        # body somehow cached and have no format for date field, so we need to
        # refetch for now — need to debug access* fields sometime
        original = self.refresh_objects(self.original)

        self.assertEqual(cloned.access_structure, original.access_structure)

    def test_data_correctly_cloned(self):
        cloned = self._do_clone(with_data=True)

        self.assertEqual(cloned.access_data, self.original.access_data)

    def test_watchers_cloned(self):
        set_user_setting(self.user_two, 'new_subscriptions', False)

        PageWatch.objects.create(
            user=self.user_two.username,
            page=self.original,
            is_cluster=True,
        )
        cloned = self._do_clone(destination_tag='Оригинал/Клон')

        inherited_watch = cloned.pagewatch_set.get(is_cluster=True).user
        self.assertEqual(self.user_two.username, inherited_watch)

        direct_watchers = cloned.pagewatch_set.get(is_cluster=False).user
        self.assertEqual(self.user_one.username, direct_watchers)
