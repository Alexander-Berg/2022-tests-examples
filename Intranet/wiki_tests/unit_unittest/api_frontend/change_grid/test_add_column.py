from unittest import skipIf

from django.conf import settings
from django.test import override_settings
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
    }
  ]
}
"""


class AddColumnTest(BaseGridsTest):
    def _prepare_grid(self, supertag):
        self._create_grid(supertag, GRID_STRUCTURE, self.user_thasonic)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))

        return loads(response.content)['data']['version']

    def test_add_string_column(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        some_field='some',
                        added_column=dict(
                            type='string',
                            title='Любимый герой сериала',
                            width=100,
                            width_units='percent',
                            required=False,
                        ),
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

        self.assertEqual(2, len(columns))

        self.assertEqual('string', columns[1]['type'])
        self.assertEqual('Любимый герой сериала', columns[1]['title'])
        self.assertEqual('100', columns[1]['width'])
        self.assertEqual('%', columns[1]['width_units'])
        self.assertEqual(False, columns[1]['required'])

    def test_data_is_updated(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag)

        self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='-1',
                            data=dict(
                                name='some name',
                            ),
                        ),
                    ),
                ],
            ),
        )

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_column=dict(
                            type='number',
                            title='Сколько вешать в граммах?',
                            required=False,
                        )
                    ),
                ],
            ),
        )

        self.assertEqual(200, response.status_code)
        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        content = loads(response.content)
        first_row = content['data']['rows'][0]
        self.assertEqual('', first_row[1]['raw'])

    def test_add_column_with_empty_title(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[dict(added_column=dict(type='string', width=100, width_units='percent', required=False))],
            ),
        )
        self.assertEqual(200, response.status_code)
        content = loads(response.content)
        self.assertEqual(content['data']['success'], True)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        content = loads(response.content)
        columns = content['data']['structure']['fields']

        self.assertEqual(2, len(columns))

        self.assertEqual('string', columns[1]['type'])
        self.assertEqual('', columns[1]['title'])
        self.assertEqual('100', columns[1]['width'])
        self.assertEqual('%', columns[1]['width_units'])
        self.assertEqual(False, columns[1]['required'])

    @override_settings(LIMIT__GRID_COLS_COUNT=2)
    def test_max_columns_count(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[dict(added_column=dict(type='string', width=100, width_units='percent', required=False))],
            ),
        )
        self.assertEqual(200, response.status_code)
        version = response.data['data']['version']

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[dict(added_column=dict(type='string', width=100, width_units='percent', required=False))],
            ),
        )
        self.assertEqual(409, response.status_code)
        self.assertEqual('Maximum grid columns count exceeded', response.data['error']['message'][0])

    if settings.IS_INTRANET or settings.IS_BUSINESS:

        @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
        def test_add_ticket_assignee_column(self):
            supertag = 'grid'
            version = self._prepare_grid(supertag)

            # столбец тикет
            response = self.client.post(
                '/_api/frontend/{0}/.grid/change'.format(supertag),
                dict(
                    version=str(version),
                    changes=[
                        dict(
                            added_column=dict(
                                type='ticket', title='Тикет', width=100, width_units='percent', required=False
                            )
                        )
                    ],
                ),
            )
            self.assertEqual(200, response.status_code)

            response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
            self.assertEqual(200, response.status_code)
            data = loads(response.content)
            ticket_column_name = data['data']['structure']['fields'][1]['name']
            version = data['data']['version']

            # добавить строку с данными тикета (проверка фикса баги WIKI-7216)
            response = self.client.post(
                '/_api/frontend/{0}/.grid/change'.format(supertag),
                dict(
                    version=str(version),
                    changes=[
                        dict(
                            added_row=dict(
                                after_id='-1', data=dict([('name', 'some name'), (ticket_column_name, 'STARTREK-100')])
                            )
                        )
                    ],
                ),
            )
            self.assertEqual(200, response.status_code)

            # столбец исполнитель тикета
            response = self.client.post(
                '/_api/frontend/{0}/.grid/change'.format(supertag),
                dict(
                    version=str(version),
                    changes=[
                        dict(
                            added_column=dict(
                                type='ticket-assignee',
                                title='assignee',
                                width=100,
                                width_units='percent',
                                required=False,
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

            self.assertEqual(3, len(columns))

            last_column = columns[2]
            self.assertEqual('ticket-assignee', last_column['type'])
            self.assertEqual('assignee', last_column['title'])
            self.assertEqual('100', last_column['width'])
            self.assertEqual('%', last_column['width_units'])
            self.assertEqual(False, last_column['required'])

            ticket_assignee_cell = content['data']['rows'][0][2]
            self.assertIn('login', ticket_assignee_cell['transformed'][0])
            self.assertIn('last_name', ticket_assignee_cell['transformed'][0])
            self.assertIn('first_name', ticket_assignee_cell['transformed'][0])

        def test_add_ticket_column(self):
            supertag = 'grid'
            version = self._prepare_grid(supertag)

            self.client.post(
                '/_api/frontend/{0}/.grid/change'.format(supertag),
                dict(
                    version=str(version),
                    changes=[
                        dict(
                            added_row=dict(
                                after_id='-1',
                                data=dict(
                                    name='some name',
                                ),
                            ),
                        ),
                    ],
                ),
            )

            # столбец тикет
            response = self.client.post(
                '/_api/frontend/{0}/.grid/change'.format(supertag),
                dict(
                    version=str(version),
                    changes=[
                        dict(
                            added_column=dict(
                                type='ticket', title='Тикет', width=100, width_units='percent', required=False
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

            self.assertEqual(2, len(columns))

            self.assertEqual('ticket', columns[1]['type'])
            self.assertEqual('Тикет', columns[1]['title'])
            self.assertEqual('100', columns[1]['width'])
            self.assertEqual('%', columns[1]['width_units'])
            self.assertEqual(False, columns[1]['required'])

            # столбец поле тикета
            response = self.client.post(
                '/_api/frontend/{0}/.grid/change'.format(supertag),
                dict(
                    version=str(version),
                    changes=[
                        dict(
                            added_column=dict(
                                type='ticket-subject', title='сабжект', width=100, width_units='percent', required=False
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

            self.assertEqual(3, len(columns))

            self.assertEqual('ticket-subject', columns[2]['type'])
            self.assertEqual('сабжект', columns[2]['title'])
            self.assertEqual('100', columns[2]['width'])
            self.assertEqual('%', columns[2]['width_units'])
            self.assertEqual(False, columns[2]['required'])

        def test_add_ticket_status_column(self):
            supertag = 'grid'
            version = self._prepare_grid(supertag)

            self.client.post(
                '/_api/frontend/{0}/.grid/change'.format(supertag),
                dict(
                    version=str(version),
                    changes=[
                        dict(
                            added_row=dict(
                                after_id='-1',
                                data=dict(
                                    name='some name',
                                ),
                            ),
                        ),
                    ],
                ),
            )

            # столбец тикет
            response = self.client.post(
                '/_api/frontend/{0}/.grid/change'.format(supertag),
                dict(
                    version=str(version),
                    changes=[
                        dict(
                            added_column=dict(
                                type='ticket', title='Тикет', width=100, width_units='percent', required=False
                            )
                        )
                    ],
                ),
            )
            self.assertEqual(200, response.status_code)
            content = loads(response.content)
            self.assertEqual(content['data']['success'], True)

            # столбец статус тикета
            response = self.client.post(
                '/_api/frontend/{0}/.grid/change'.format(supertag),
                dict(
                    version=str(version),
                    changes=[
                        dict(
                            added_column=dict(
                                type='ticket-status', title='status', width=100, width_units='percent', required=False
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

            self.assertEqual(3, len(columns))

            last_column = columns[2]
            self.assertEqual('ticket-status', last_column['type'])
            self.assertEqual('status', last_column['title'])
            self.assertEqual('100', last_column['width'])
            self.assertEqual('%', last_column['width_units'])
            self.assertEqual(False, last_column['required'])
