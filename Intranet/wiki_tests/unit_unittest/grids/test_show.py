
from django.utils.encoding import smart_bytes

from wiki.grids.filter import FilterError, filter_grid, get_filter_parts
from intranet.wiki.tests.wiki_tests.common.utils import locmemcache
from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import BaseGridsTest


@locmemcache('grid_filter')
class ShowTest(BaseGridsTest):
    create_user_clusters = True

    def setUp(self):
        super(ShowTest, self).setUp()
        self._create_gorilla_grids()

    def _filter(self, filter):
        data = {}
        hashes = None
        try:
            hashes, _ = filter_grid(filter, self.grid)
        except FilterError as error:
            data['__filter_error__'] = smart_bytes(error)
        else:
            data['__filter_parts__'] = get_filter_parts(filter)
        return hashes, data

    def test_filter(self):
        hashes, data = self._filter("[name] ~ 'sea', [date] between 2010-05-01 and 2010-05-31")
        self.assertEqual(len(hashes), 1)

    def test_filter_with_errors_contains_error_message(self):
        hashes, data = self._filter('this cannot be parsed')
        self.assertTrue(len(data.get('__filter_error__')) > 0)

    def test_filter_with_errors_returns_no_rows(self):
        hashes, data = self._filter('this cannot be parsed')
        self.assertIsNone(hashes)

    def test_filter_parts(self):
        hashes, data = self._filter("[name] ~ 'sea', [date] between 2010-05-01 and 2010-05-31")
        self.assertIn('name', data['__filter_parts__'])
        self.assertIn('date', data['__filter_parts__'])

    def test_filter_parts_join_clauses(self):
        hashes, data = self._filter("[name] ~ 'sea', [date] = 2010-05-10, [name] ~ sex")
        self.assertEqual(data['__filter_parts__']['name'].count('name'), 2)

    def test_filter_parts_not_executed_on_errors(self):
        hashes, data = self._filter('some invalid filter')
        self.assertNotIn('__filter_parts__', data)
