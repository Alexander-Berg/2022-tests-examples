
from ujson import loads
from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.change_grid.base import ChangeGridBaseTest


class ChangeGridSorting(ChangeGridBaseTest):
    GRID_STRUCTURE = """
    {
      "title" : "Лист of conferences",
      "width" : "100%",
      "sorting" : [],
      "fields" : [
        {
          "name" : "c1",
          "title" : "тайтл1"
        },
        {
          "name" : "c2",
          "title" : "тайтл2"
        }
      ]
    }
    """

    def setUp(self):
        super(ChangeGridSorting, self).setUp()

        self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(self.supertag),
            dict(
                version=str(self.get_current_version()),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='-1',
                            data=dict(
                                c1='a',
                                c2='b',
                            ),
                        )
                    )
                ],
            ),
        )

    def _test_valid(self, input, expected):
        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(self.supertag),
            dict(version=str(self.get_current_version()), changes=[dict(sorting_changed=dict(sorting=input))]),
        )
        self.assertEqual(200, response.status_code)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(self.supertag))
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual(data['structure']['sorting'], expected)

    def test_valid_sorting(self):
        self._test_valid([{'c1': 'asc'}], [{'name': 'c1', 'type': 'asc'}])

    def test_valid_sorting_2(self):
        self._test_valid(
            [{'c2': 'asc'}, {'c1': 'desc'}], [{'name': 'c2', 'type': 'asc'}, {'name': 'c1', 'type': 'desc'}]
        )

    def test_invalid(self):
        # используем метод предка для проверки невалидного запроса
        self._test_invalid({'changes': [{'sorting': None}]})
        self._test_invalid({'changes': [{'sorting': 10}]})
        self._test_invalid({'changes': [{'sorting': []}]})
        self._test_invalid({'changes': [{'sorting': [10]}]})
        self._test_invalid({'changes': [{'sorting': [{10: 20}]}]})
        self._test_invalid({'changes': [{'sorting': [{'c1': 'zzz'}]}]})
        self._test_invalid({'changes': [{'sorting': [{'zzz': 'asc'}]}]})
        self._test_invalid({'changes': [{'sorting': [{'c1': 'asc'}, {'c1': 'zzz'}]}]})
        self._test_invalid({'changes': [{'sorting': [{'c1': 'asc'}, {'zzz': 'desc'}]}]})

        response = self.client.get('/_api/frontend/{0}/.grid'.format(self.supertag))
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual(data['structure']['sorting'], [])
