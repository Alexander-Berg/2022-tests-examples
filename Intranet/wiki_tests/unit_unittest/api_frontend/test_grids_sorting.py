
from django.conf import settings
from ujson import dumps, loads

from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import BaseGridsTest

GRID_STRUCTURE = """
{
  "title" : "List of conferences",
  "width" : "100%%",
  "sorting" : %s,
  "fields" : [
    {
      "name" : "number",
      "type": "number",
      "title" : "Number of films"
    },
    {
      "name" : "actor",
      "type": "staff",
      "title" : "actor"
    }
  ]
}
"""


class SortGridTest(BaseGridsTest):
    supertag = 'grid'

    def add_row(self, supertag, data):
        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        version = loads(response.content)['data']['version']
        self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(version=str(version), changes=[dict(added_row=dict(after_id='last', data=data))]),
        )

    @staticmethod
    def get_two_values_from_grid(row_number, grid_data):
        result = grid_data[row_number][0]['raw'], grid_data[row_number][1]['raw'][0]
        return result

    def _prepare_grid(self, sorting):
        self._create_grid(self.supertag, GRID_STRUCTURE % dumps(sorting), self.user_thasonic)

        # получить табличный список вида
        # 1 | dannycrane
        # 3 | chapson
        # 2 | thasonic
        # 1 | dannydevito
        self.add_row(
            self.supertag,
            dict(
                number=1,
                actor='dannycrane',
            ),
        )
        self.add_row(
            self.supertag,
            dict(
                number=3,
                actor='chapson',
            ),
        )
        self.add_row(
            self.supertag,
            dict(
                number=2,
                actor='thasonic',
            ),
        )
        self.add_row(
            self.supertag,
            dict(
                number=1,
                actor='dannydevito',
            ),
        )
        response = self.client.get('/_api/frontend/{0}/.grid'.format(self.supertag))

        return loads(response.content)['data']['rows']

    def test_sort_by_request(self):
        self._prepare_grid([{'name': 'number', 'type': 'asc'}])
        assert_queries = 14 if not settings.WIKI_CODE == 'wiki' else 12
        with self.assertNumQueries(assert_queries):
            response = self.client.get('/_api/frontend/{0}/.grid?sort_number=desc'.format(self.supertag))
        result = loads(response.content)['data']['rows']
        self.assertEqual('3', result[0][0]['raw'])
        self.assertEqual('2', result[1][0]['raw'])
        self.assertEqual('1', result[2][0]['raw'])
        self.assertEqual('1', result[3][0]['raw'])
        self.assertEqual(len(result), 4)

    def test_do_not_sort_by_erroneous_request(self):
        result = self._prepare_grid([])

        def is_ok(grid_data):
            self.assertEqual(self.get_two_values_from_grid(0, grid_data), ('1', 'dannycrane'))
            self.assertEqual(self.get_two_values_from_grid(1, grid_data), ('3', 'chapson'))
            self.assertEqual(self.get_two_values_from_grid(2, grid_data), ('2', 'thasonic'))
            self.assertEqual(self.get_two_values_from_grid(3, grid_data), ('1', 'dannydevito'))

        is_ok(result)

        # несуществующий столбец
        response = self.client.get('/_api/frontend/{0}/.grid?sort_aaaaa=asc'.format(self.supertag))
        result_sorted = loads(response.content)['data']['rows']
        is_ok(result_sorted)
