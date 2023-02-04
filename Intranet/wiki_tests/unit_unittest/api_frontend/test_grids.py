from django.conf import settings
from ujson import loads

from wiki.api_core.errors.bad_request import InvalidDataSentError
from wiki.grids.models import Grid
from wiki.grids.utils.base import HASH_KEY
from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import BaseGridsTest
from intranet.wiki.tests.wiki_tests.unit_unittest.grids.base import GRID_WITH_ALL_KIND_OF_FIELDS_STRUCTURE

GRID_STRUCTURE = """
{
  "title" : "List of conferences",
  "width" : "100%",
  "sorting" : [
    {"name" : "name", "type" : "asc"},
    {"name" : "date", "type" : "desc"}
  ],
  "fields" : [
    {
      "name" : "name",
      "title" : "Name of conference",
      "width" : "200px",
      "sorting" : true,
      "required" : true
    },
    {
      "name" : "date",
      "title" : "Date of conference",
      "sorting" : false,
      "required" : false,
      "type" : "date"
    },
    {
      "name" : "is_done",
      "title" : "Is done?",
      "sorting" : true,
      "required" : false,
      "type" : "checkbox",
      "markdone" : true
    },
    {
      "name" : "how_many",
      "title" : "How many?",
      "sorting" : true,
      "required" : false,
      "type" : "number"
    }
  ]
}
"""

grid_json_struct = [
    'tag',
    'supertag',
    'url',
    'page_type',
    'is_redirect',
    'lang',
    'last_author',
    'title',
    'breadcrumbs',
    'created_at',
    'modified_at',
    'owner',
    'access',
    'version',
    'current_user_subscription',
    'user_css',
    'bookmark',
    'actuality_status',
    'qr_url',
    'comments_count',
    'is_official',
    'org',
    'comments_status',
    'authors',
    'with_new_wf',
    'is_readonly',
    'notifier',
]


class GridsTest(BaseGridsTest):
    def test_get_grid(self):
        supertag = 'grid'
        self._create_grid(supertag, GRID_STRUCTURE, self.user_thasonic)

        self._add_row(supertag, dict(name='some name', date='2013-08-12', is_done=''))

        response = self.client.get('/_api/frontend/{0}'.format(supertag))

        content = loads(response.content)

        self.assertEqual(200, response.status_code)

        for example_key in grid_json_struct:
            self.assertTrue(
                example_key in content['data'],
                '"{key}" not in {result}'.format(key=example_key, result=content['data']),
            )
        # Проверить, что нет никаких новых полей в ответе.
        for key in content['data']:
            self.assertTrue(key in grid_json_struct, '"{0}" is a new field in response'.format(key))

        self.assertEqual(content['data']['actuality_status'], 'actual')

    def test_get_grid_data(self):
        supertag = 'grid'
        self._create_grid(supertag, GRID_STRUCTURE, self.user_thasonic)

        self._add_row(supertag, dict(name='some name', date='2013-08-12', is_done=''))

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))

        content = loads(response.content)

        self.assertEqual(200, response.status_code)

        # Проверить, что в ответе нет лишних полей
        self.assertEqual({'structure', 'rows', 'version'}, set(content['data'].keys()))

        self.assertEqual(len(content['data']['rows']), 1)

        row = content['data']['rows'][0]

        self.assertTrue(all(isinstance(cell, dict) for cell in row))

        self.assertEqual('name', row[0][HASH_KEY])
        self.assertEqual('some name', row[0]['raw'])

        self.assertEqual('date', row[1][HASH_KEY])
        self.assertEqual('2013-08-12', row[1]['raw'])

        self.assertEqual('is_done', row[2][HASH_KEY])
        self.assertEqual(False, row[2]['raw'])

        self.assertEqual('how_many', row[3][HASH_KEY])
        self.assertEqual(None, row[3]['raw'])

    def test_staff_field_has_first_name_and_last_name(self):
        GRID_STRUCTURE = """
{
  "title" : "List of conferences",
  "width" : "100%",
  "sorting" : [],
  "fields" : [
    {
      "name" : "staff_field",
      "title" : "Lecturer",
      "sorting" : true,
      "type" : "staff",
      "multiple": true
    }
  ]
}
        """

        supertag = 'grid'
        self._create_grid(supertag, GRID_STRUCTURE, self.user_thasonic)

        self._add_row(
            supertag,
            dict(
                staff_field='thasonic,chapson',
            ),
        )

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))

        content = loads(response.content)

        transformed_thasonic = content['data']['rows'][0][0]['transformed'][0]
        transformed_chapson = content['data']['rows'][0][0]['transformed'][1]

        self.assertEqual('thasonic', transformed_thasonic['login'])
        self.assertEqual('Alexander', transformed_thasonic['first_name'])
        self.assertEqual('Pokatilov', transformed_thasonic['last_name'])

        self.assertEqual('chapson', transformed_chapson['login'])
        self.assertEqual('Anton', transformed_chapson['first_name'])
        self.assertEqual('Chaporgin', transformed_chapson['last_name'])

    def test_grid_revision_author_set_correctly(self):
        supertag = 'grid'
        self._create_grid(supertag, GRID_STRUCTURE, self.user_thasonic)

        response = self.client.get('/_api/frontend/{0}/.events'.format(supertag))

        events = loads(response.content)['data']['data']
        self.assertEqual(len(events), 2)
        self.assertEqual(events[0]['revision']['author']['login'], 'thasonic')
        self.assertEqual(events[1]['revision']['author']['login'], 'thasonic')

        self.client.logout()

        self.client.login('asm')

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        version = loads(response.content)['data']['version']
        self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='last',
                            data=dict(
                                name='name',
                                date='2015-01-01',
                                how_many=5,
                            ),
                        )
                    )
                ],
            ),
        )

        response = self.client.get('/_api/frontend/{0}/.events'.format(supertag))
        events = loads(response.content)['data']['data']
        self.assertEqual(len(events), 3)
        self.assertEqual(events[0]['revision']['author']['login'], 'asm')


class GridConcurrentChangesViewTest(BaseGridsTest):
    supertag = 'grid'

    def setUp(self):
        super(GridConcurrentChangesViewTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')

    def _get_changes(self, version):
        response = self.client.get(
            '{api_url}/{supertag}/.changes/{version}'.format(
                api_url=self.api_url, supertag=self.supertag, version=version
            )
        )
        return loads(response.content)['data']

    def test_concurrent_changes(self):
        self.client.post(
            '{api_url}/{supertag}/.grid/create'.format(api_url=self.api_url, supertag=self.supertag),
            dict(title='grid title'),
        )

        response = self.client.get('{api_url}/{supertag}/.grid'.format(api_url=self.api_url, supertag=self.supertag))
        version = loads(response.content)['data']['version']

        response = self.client.post(
            '{api_url}/{supertag}/.grid/change'.format(api_url=self.api_url, supertag=self.supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_column=dict(type='string', title='Name', width=100, width_units='percent', required=False)
                    )
                ],
            ),
        )
        version = loads(response.content)['data']['version']

        data = self._get_changes(version)
        self.assertEqual(data['editors'], [])

        # добавляем две строки
        self._add_row(self.supertag, {})
        self._add_row(self.supertag, {})
        data = self._get_changes(version)
        self.assertEqual(data['editors'], [])

        self.client.login('chapson')
        data = self._get_changes(version)
        self.assertEqual(data['editors'][0]['login'], 'thasonic')
        self.assertEqual(data['editors'][0]['uid'], int(self.user_thasonic.staff.uid))

        self._add_row(self.supertag, {})
        data = self._get_changes(version)
        self.assertEqual(data['editors'][0]['login'], 'thasonic')
        self.assertEqual(data['editors'][0]['uid'], int(self.user_thasonic.staff.uid))

        self.client.login('asm')
        data = self._get_changes(version)
        editors = data['editors']
        editors.sort(key=lambda x: x['login'])
        self.assertEqual(data['editors'][0]['login'], 'chapson')
        self.assertEqual(data['editors'][0]['uid'], int(self.user_chapson.staff.uid))
        self.assertEqual(data['editors'][1]['login'], 'thasonic')
        self.assertEqual(data['editors'][1]['uid'], int(self.user_thasonic.staff.uid))


class APIGridRowsTest(BaseGridsTest):
    def setUp(self):
        super(APIGridRowsTest, self).setUp()

        # add test grid
        self._create_grid('testGrid', GRID_STRUCTURE, self.user_thasonic)

    def test_add_row(self):
        grid = Grid.objects.get(supertag='testgrid')

        response = self.client.post(
            '{api_url}/{supertag}/.grid/rows'.format(api_url=self.api_url, supertag='testGrid'),
            data={
                'version': grid.get_page_version(),
                'changes': [
                    {'added_row': {'after_id': '-1', 'data': {'name': 'Cucumber-growing party meeting', 'how_many': 3}}}
                ],
            },
        )

        self.assertEqual(response.status_code, 200)
        resp_data = loads(response.content)

        grid_in_db = Grid.objects.get(supertag='testgrid')
        actual_grid_row_id = str(grid_in_db.access_data[-1][HASH_KEY])

        self.assertEqual(resp_data['data']['id'], actual_grid_row_id)
        self.assertEqual(grid_in_db.access_data[-1]['name']['raw'], 'Cucumber-growing party meeting')


class GridFilteringTest(BaseGridsTest):
    def setUp(self):
        super(GridFilteringTest, self).setUp()

        # add test grid
        self._create_grid('testGrid', GRID_STRUCTURE, self.user_thasonic)

        grid = Grid.objects.get(supertag='testgrid')

        response = self.client.post(
            '{api_url}/{supertag}/.grid/rows'.format(api_url=self.api_url, supertag='testGrid'),
            data={
                'version': grid.get_page_version(),
                'changes': [{'added_row': {'after_id': '-1', 'data': {'name': 'row N1', 'how_many': 3}}}],
            },
        )
        self.assertEqual(200, response.status_code)

        response = self.client.post(
            '{api_url}/{supertag}/.grid/rows'.format(api_url=self.api_url, supertag='testGrid'),
            data={
                'version': grid.get_page_version(),
                'changes': [{'added_row': {'after_id': '-1', 'data': {'name': 'row N2', 'how_many': 5}}}],
            },
        )
        self.assertEqual(200, response.status_code)

    def test_get_filtered_grid(self):
        response = self.client.get(
            '{api_url}/{supertag}/.grid?filter={filter}'.format(
                api_url=self.api_url, supertag='testGrid', filter='[how_many] = 3'
            )
        )
        self.assertEqual(200, response.status_code)
        rows = loads(response.content)['data']['rows']
        self.assertEqual(1, len(rows))
        self.assertEqual('row N1', rows[0][0]['raw'])

        structure = loads(response.content)['data']['structure']
        self.assertTrue('filter_part' in structure['fields'][3])
        self.assertEqual('[how_many] = 3', structure['fields'][3]['filter_part'])

    def test_filtering_error_handling(self):
        response = self.client.get(
            '{api_url}/{supertag}/.grid?filter={filter}'.format(api_url=self.api_url, supertag='testGrid', filter='>>>')
        )
        self.assertEqual(409, response.status_code)
        content = loads(response.content)
        self.assertEqual(content['error']['error_code'], InvalidDataSentError.error_code)

    def test_get_grid_by_columns(self):
        response = self.client.get(
            '{api_url}/{supertag}/.grid?columns={columns}'.format(
                api_url=self.api_url,
                supertag='testGrid',
                columns='is_done,name',
            )
        )
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        structure = data['structure']
        self.assertEqual(len(structure['fields']), 2)
        self.assertEqual('is_done', structure['fields'][0]['name'])
        self.assertEqual('name', structure['fields'][1]['name'])
        rows = data['rows']
        self.assertEqual(2, len(rows[0]))
        self.assertEqual('is_done', rows[0][0]['__key__'])
        self.assertEqual('name', rows[0][1]['__key__'])

    def test_columns_error_handling(self):
        response = self.client.get(
            '{api_url}/{supertag}/.grid?columns={columns}'.format(
                api_url=self.api_url, supertag='testGrid', columns='name, unknown, bla-bla'
            )
        )
        self.assertEqual(409, response.status_code)
        content = loads(response.content)
        self.assertEqual(content['error']['error_code'], InvalidDataSentError.error_code)
        self.assertEqual(content['error']['message'][0], 'Wrong columns specified in columns filter: "bla-bla,unknown"')


if settings.IS_INTRANET:

    class GridFilteringStartrekFieldsTest(BaseGridsTest):
        def setUp(self):
            super(GridFilteringStartrekFieldsTest, self).setUp()

            # add test grid
            self._create_grid('testGrid', GRID_WITH_ALL_KIND_OF_FIELDS_STRUCTURE, self.user_thasonic)

            grid = Grid.objects.get(supertag='testgrid')

            response = self.client.post(
                '{api_url}/{supertag}/.grid/rows'.format(api_url=self.api_url, supertag='testGrid'),
                data={
                    'version': grid.get_page_version(),
                    'changes': [{'added_row': {'after_id': '-1', 'data': {'8': 'STARTREK-100'}}}],
                },
            )
            self.assertEqual(200, response.status_code)

        def test_filter_on_startrek_ticket_summary(self):
            response = self.client.get(
                '{api_url}/{supertag}/.grid?filter={filter}'.format(
                    api_url=self.api_url, supertag='testGrid', filter='[9] = killa'
                )
            )
            self.assertEqual(200, response.status_code)
            rows = loads(response.content)['data']['rows']
            self.assertEqual(1, len(rows))
