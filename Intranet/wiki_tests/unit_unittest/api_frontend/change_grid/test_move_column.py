
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


class MoveColumnTest(BaseGridsTest):
    def _prepare_grid(self, supertag):
        self._create_grid(supertag, GRID_STRUCTURE, self.user_thasonic)
        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        version = loads(response.content)['data']['version']

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(added_row=dict(after_id='-1', data=dict(name='some name', date='2013-08-12', is_done='')))
                ],
            ),
        )
        self.assertEqual(200, response.status_code)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        return loads(response.content)['data']['version']

    def test_move_column_to_beginning(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        column_moved=dict(
                            id='date',
                            after_id='-1',
                        )
                    )
                ],
            ),
        )

        self.assertEqual(200, response.status_code)
        content = loads(response.content)
        self.assertEqual(content['data']['success'], True)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        content = loads(response.content)
        columns = content['data']['structure']['fields']

        self.assertEqual(columns[0]['name'], 'date')
        self.assertEqual(columns[1]['name'], 'name')
        self.assertEqual(columns[2]['name'], 'is_done')

    def test_move_column_left(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        column_moved=dict(
                            id='is_done',
                            after_id='name',
                        )
                    )
                ],
            ),
        )

        self.assertEqual(200, response.status_code)
        content = loads(response.content)
        self.assertEqual(content['data']['success'], True)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        content = loads(response.content)
        columns = content['data']['structure']['fields']

        self.assertEqual(columns[0]['name'], 'name')
        self.assertEqual(columns[1]['name'], 'is_done')
        self.assertEqual(columns[2]['name'], 'date')

    def test_move_column_right(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        column_moved=dict(
                            id='name',
                            after_id='date',
                        )
                    )
                ],
            ),
        )

        self.assertEqual(200, response.status_code)
        content = loads(response.content)
        self.assertEqual(content['data']['success'], True)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        content = loads(response.content)
        columns = content['data']['structure']['fields']

        self.assertEqual(columns[0]['name'], 'date')
        self.assertEqual(columns[1]['name'], 'name')
        self.assertEqual(columns[2]['name'], 'is_done')

    def test_error_on_conflict(self):
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

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        column_moved=dict(
                            id='date',
                            after_id='-1',
                        )
                    )
                ],
            ),
        )
        self.assertEqual(409, response.status_code)
        data = loads(response.content)
        self.assertEqual(data['error']['error_code'], 'NO_SUCH_COLUMN')

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        column_moved=dict(
                            id='name',
                            after_id='date',
                        )
                    )
                ],
            ),
        )
        self.assertEqual(409, response.status_code)
        data = loads(response.content)
        self.assertEqual(data['error']['error_code'], 'OPERATION_MAKES_NO_SENSE')

    def test_move_into_itself(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        column_moved=dict(
                            id='name',
                            after_id=-1,
                        )
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        content = loads(response.content)
        columns = content['data']['structure']['fields']

        self.assertEqual(columns[0]['name'], 'name')
        self.assertEqual(columns[1]['name'], 'date')
        self.assertEqual(columns[2]['name'], 'is_done')
