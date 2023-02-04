
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
      "title" : "Name of series"
    }
  ]
}
"""


class EditColumnTest(BaseGridsTest):
    def _prepare_grid(self, supertag):
        self._create_grid(supertag, GRID_STRUCTURE, self.user_thasonic)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        version = loads(response.content)['data']['version']

        # добавить столбец
        self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_column=dict(
                            type='number',
                            title='Любимый герой сериала',
                            width=100,
                            required=False,
                            width_units='percent',
                        )
                    )
                ],
            ),
        )

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        return loads(response.content)['data']['version']

    def test_change_column(self):
        version = self._prepare_grid('friends')

        response_data = loads(self.client.get('/_api/frontend/{0}/.grid'.format('friends')).content)
        column_name = response_data['data']['structure']['fields'][1]['name']

        response = self.client.post(
            '/_api/frontend/friends/.grid/change',
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_column=dict(
                            name=column_name,
                            required=True,
                            width=20,
                            width_units='pixel',
                            title='Роль',
                        ),
                    )
                ],
            ),
        )

        self.assertEqual(200, response.status_code)
        response = self.client.get('/_api/frontend/friends/.grid')
        content = loads(response.content)
        columns = content['data']['structure']['fields']

        self.assertEqual(True, columns[1]['required'])
        self.assertEqual('Роль', columns[1]['title'])
        self.assertEqual('20', columns[1]['width'])
        self.assertEqual('px', columns[1]['width_units'])
        self.assertEqual(column_name, columns[1]['name'])
        self.assertEqual('number', columns[1]['type'])

    def test_change_column_mark_done(self):
        version = self._prepare_grid('friends')

        response = self.client.post(
            '/_api/frontend/friends/.grid/change',
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_column=dict(
                            type='checkbox',
                            title='Просмотрены все фильмы с ним?',
                            markdone=False,
                            required=True,
                        )
                    )
                ],
            ),
        )

        response_data = loads(self.client.get('/_api/frontend/{0}/.grid'.format('friends')).content)
        column_name = response_data['data']['structure']['fields'][2]['name']

        version = loads(response.content)['data']['version']

        # выставим галку "помечать сделанным"
        response = self.client.post(
            '/_api/frontend/friends/.grid/change',
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_column=dict(
                            name=column_name,  # нумерация начинается с нуля
                            title='Просмотрены все фильмы с ним?',
                            markdone=True,
                            required=True,
                        ),
                    )
                ],
            ),
        )

        self.assertEqual(200, response.status_code)
        response_data = loads(self.client.get('/_api/frontend/friends/.grid').content)
        self.assertEqual(True, response_data['data']['structure']['fields'][2]['markdone'])

    def test_merge_changes(self):

        # пытаемся работать с табличным списком устаревшей версии
        # chapson переместил наш столбец,
        # а мы думаем что он еще на старом месте
        # операция должна быть успешной

        version = self._prepare_grid('friends')

        response_data = loads(self.client.get('/_api/frontend/{0}/.grid'.format('friends')).content)
        column_name = response_data['data']['structure']['fields'][1]['name']

        response = self.client.post(
            '/_api/frontend/friends/.grid/change',
            dict(
                version=str(version),
                changes=[
                    dict(
                        column_moved=dict(
                            id=column_name,
                            after_id='-1',
                        )
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)

        response = self.client.post(
            '/_api/frontend/friends/.grid/change',
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_column=dict(
                            name=column_name,  # тот самый столбец, который переместился
                            required=True,
                            width=20,
                            width_units='pixel',
                            title='Роль',
                        ),
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)

        response_data = loads(self.client.get('/_api/frontend/friends/.grid').content)
        columns = response_data['data']['structure']['fields']
        self.assertEqual('Роль', columns[0]['title'])
        self.assertEqual('Name of series', columns[1]['title'])

    def test_change_multiple_column(self):
        GRID_STRUCTURE = """
{
  "title" : "List of conferences",
  "width" : "100%",
  "sorting" : [],
  "fields" : [
    {
      "name" : "staff_list",
      "title" : "List of staff",
      "type" : "staff",
      "multiple": true
    }
  ]
}
"""
        supertag = 'staff'
        self._create_grid(supertag, GRID_STRUCTURE, self.user_thasonic)

        response_data = loads(self.client.get('/_api/frontend/{0}/.grid'.format(supertag)).content)

        version = response_data['data']['version']
        column_name = response_data['data']['structure']['fields'][0]['name']

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_column=dict(
                            name=column_name, required=True, width=20, width_units='pixel', title='new title'
                        ),
                    )
                ],
            ),
        )

        self.assertEqual(200, response.status_code)
        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        content = loads(response.content)
        columns = content['data']['structure']['fields']
        self.assertEqual(True, columns[0]['required'])
        self.assertEqual(True, columns[0]['multiple'])
        self.assertEqual('new title', columns[0]['title'])
        self.assertEqual('20', columns[0]['width'])
        self.assertEqual('px', columns[0]['width_units'])
        self.assertEqual(column_name, columns[0]['name'])
        self.assertEqual('staff', columns[0]['type'])
