
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


class MoveRowTest(BaseGridsTest):
    def _prepare_grid(self, supertag):
        self._create_grid(supertag, GRID_STRUCTURE, self.user_thasonic)
        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        version = loads(response.content)['data']['version']
        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(added_row=dict(after_id='-1', data=dict(name='Denny Crane', date='2013-08-22', is_done='')))
                ],
            ),
        )
        self.assertEqual(200, response.status_code)

        version = loads(response.content)['data']['version']
        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(added_row=dict(after_id='1', data=dict(name='Shirley Schmidt', date='2013-08-23', is_done='')))
                ],
            ),
        )
        self.assertEqual(200, response.status_code)

        return loads(response.content)['data']['version']

    def test_move_row(self):
        supertag = 'bostonlegal'
        version = self._prepare_grid(supertag)

        # первая строка "Denny Crane", а вторая "Shirley Schmidt"
        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(version=str(version), changes=[dict(row_moved=dict(id='1', after_id='2', before_id='-1'))]),
        )

        # теперь первая строка "Shirley Schmidt", вторая "Denny Crane"

        self.assertEqual(200, response.status_code)
        content = loads(response.content)
        self.assertEqual(content['data']['success'], True)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        content = loads(response.content)

        second_row = content['data']['rows'][1]
        self.assertEqual(second_row[0]['raw'], 'Denny Crane')

    def test_move_row_to_top(self):
        supertag = 'bostonlegal'
        self._prepare_grid(supertag)

        # первая строка "Denny Crane", а вторая "Shirley Schmidt"
        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(changes=[dict(row_moved=dict(id='2', after_id='-1', before_id='1'))]),
        )

        # теперь первая строка "Shirley Schmidt", вторая "Denny Crane"

        self.assertEqual(200, response.status_code)
        content = loads(response.content)
        self.assertEqual(content['data']['success'], True)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        content = loads(response.content)

        second_row = content['data']['rows'][1]
        self.assertEqual(second_row[0]['raw'], 'Denny Crane')

    def test_move_non_existent_row(self):
        supertag = 'bostonlegal'
        self._prepare_grid(supertag)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(changes=[dict(row_moved=dict(id='1231', after_id='1', before_id='-1'))]),  # нет такого
        )

        self.assertEqual(409, response.status_code)
        data = loads(response.content)
        self.assertEqual(data['error']['error_code'], 'NO_SUCH_ROW')

    def test_move_after_nonexistent_row_before_existent(self):
        supertag = 'bostonlegal'
        version = self._prepare_grid(supertag)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(version=str(version), changes=[dict(added_row=dict(after_id='2', before_id='-1', data={}))]),
        )
        self.assertEqual(200, response.status_code)
        loads(response.content)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                changes=[
                    dict(
                        removed_row=dict(
                            id='2',
                        )
                    )
                ]
            ),
        )
        self.assertEqual(200, response.status_code)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(changes=[dict(row_moved=dict(id='1', after_id='2', before_id='3'))]),
        )
        self.assertEqual(200, response.status_code)

    def test_move_after_nonexistent_last_row(self):
        supertag = 'bostonlegal'
        version = self._prepare_grid(supertag)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(version=str(version), changes=[dict(added_row=dict(after_id='2', before_id='-1', data={}))]),
        )
        self.assertEqual(200, response.status_code)
        loads(response.content)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(changes=[dict(row_moved=dict(id='1', after_id='2', before_id='-1'))]),
        )
        self.assertEqual(200, response.status_code)

    def test_move_in_nonexistent_place(self):
        supertag = 'bostonlegal'
        self._prepare_grid(supertag)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(changes=[dict(row_moved=dict(id='111', after_id='11121', before_id='11122'))]),  # нет такой строки
        )

        # нельзя передвигать строку указав несуществующий id
        self.assertEqual(409, response.status_code)
        data = loads(response.content)
        self.assertEqual(data['error']['error_code'], 'NO_SUCH_ROW')
