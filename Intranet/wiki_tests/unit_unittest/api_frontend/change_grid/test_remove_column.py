
from ujson import loads

from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import BaseGridsTest

GRID_STRUCTURE = """
{
  "title" : "List of conferences",
  "width" : "100%",
  "sorting" : [],
  "fields" : [
    {
      "name" : "name",
      "title" : "Name of conference"
    },
    {
      "name" : "date",
      "title" : "Date of conference"
    },
    {
      "name" : "is_done",
      "title" : "Is done?"
    }
  ]
}
"""


class RemoveColumnTest(BaseGridsTest):
    def _prepare_grid(self, supertag):
        self._create_grid(supertag, GRID_STRUCTURE, self.user_thasonic)

        self._add_row(supertag, dict(name='some name', date='2013-08-12', is_done=''))

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        return loads(response.content)['data']['version']

    def test_remove_column(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[dict(removed_column=dict(name='date'))],
            ),
        )

        self.assertEqual(200, response.status_code)
        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        content = loads(response.content)
        columns = content['data']['structure']['fields']
        self.assertEqual(2, len(columns))
        self.assertEqual('name', columns[0]['name'])
        self.assertEqual('is_done', columns[1]['name'])

    def test_remove_nonexistent_column(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[dict(removed_column=dict(name='no such name'))],
            ),
        )

        # можно удалять несуществующие столбы.
        self.assertEqual(200, response.status_code)
        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        content = loads(response.content)
        columns = content['data']['structure']['fields']
        self.assertEqual(3, len(columns))

    def test_column_data_is_removed(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[dict(removed_column=dict(name='date'))],
            ),
        )

        self.assertEqual(200, response.status_code)
        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        content = loads(response.content)
        row = content['data']['rows'][0]
        self.assertEqual('some name', row[0]['raw'])
        self.assertEqual('', row[1]['raw'])
