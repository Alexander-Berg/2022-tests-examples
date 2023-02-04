
from ujson import loads
from urllib.parse import urlencode

from django.conf import settings
from django.test import override_settings
from intranet.wiki.tests.wiki_tests.common.utils import encode_multipart_formdata
from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import BaseGridsTest
from wiki.api_core.errors.bad_request import InvalidDataSentError
from wiki.grids.models import Grid
from wiki.grids.readers import CsvReader
from wiki.notifications.models import PageEvent

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
      "name" : "manager",
      "title" : "staff name",
      "sorting" : true,
      "required" : false,
      "type" : "staff"
    }
  ]
}
"""


class ImportGridTest(BaseGridsTest):
    def setUp(self):
        super(ImportGridTest, self).setUp()
        self.supertag = 'testGrid'

    def _create_godzilla_grid(self):
        # add test grid
        self._create_grid(self.supertag, GRID_STRUCTURE, self.user_thasonic)

        grid = Grid.objects.get(supertag='testgrid')

        response = self.client.post(
            '{api_url}/{supertag}/.grid/rows'.format(api_url=self.api_url, supertag='testGrid'),
            data={
                'version': grid.get_page_version(),
                'changes': [{'added_row': {'after_id': '-1', 'data': {'name': 'row N1', 'manager': 'chapson'}}}],
            },
        )
        self.assertEqual(200, response.status_code)

        response = self.client.post(
            '{api_url}/{supertag}/.grid/rows'.format(api_url=self.api_url, supertag='testGrid'),
            data={
                'version': grid.get_page_version(),
                'changes': [{'added_row': {'after_id': '1', 'data': {'name': 'row N2', 'manager': 'thasonic'}}}],
            },
        )
        self.assertEqual(200, response.status_code)

        return grid

    def _upload_test_file(self, file_name, data, query_params=None):

        content_type, body = encode_multipart_formdata([('someField', 'somedata')], [('filefield', file_name, data)])

        url = '{api_url}/.import/grid/upload?'.format(api_url=self.api_url)
        if query_params:
            url += urlencode(query_params)
        response = self.client.post(url, data=body, content_type=content_type)
        self.assertEqual(200, response.status_code)
        resp_data = loads(response.content)['data']

        return resp_data

    def test_upload_import_file(self):
        data = 'column1;column2;column3\nыыы;эээ;123'

        resp_data = self._upload_test_file('test.csv', data)
        self.assertIn('rows', resp_data)
        self.assertIn('cache_key', resp_data)
        self.assertIn('row_count', resp_data)
        self.assertIn('column_count', resp_data)
        self.assertEqual(2, resp_data['row_count'])
        self.assertEqual(3, resp_data['column_count'])
        rows = resp_data['rows']
        self.assertEqual(2, len(rows))
        self.assertIn('column1', rows[0])
        self.assertIn('ыыы', rows[1])

    def test_encoding_input_data(self):
        # UTF-8
        data = 'column1;column2\nыыы;эээ'

        resp_data = self._upload_test_file('test.csv', data)
        rows = resp_data['rows']
        self.assertIn('ыыы', rows[1])

        # windows-1251
        data = 'column1;column2\nыыы;эээ'.encode('windows-1251')
        # @todo если не передать charset явно, то кодировка определяется неверно - 'ISO-8859-2' вместо 'windows-1251'
        resp_data = self._upload_test_file('test.csv', data, query_params={'charset': 'windows-1251'})
        rows = resp_data['rows']
        self.assertIn('ыыы', rows[1])

    def test_parse_import_data(self):
        # windows-1251
        data = "column1,column2,column3\n'ыыы','эээ','123'".encode('windows-1251')
        resp_data = self._upload_test_file('test.csv', data, query_params={'charset': 'windows-1251'})
        cache_key = resp_data['cache_key']

        url = '{api_url}/.import/grid/parse?'.format(api_url=self.api_url)

        url += urlencode(
            {'key': cache_key, 'charset': 'windows-1251', 'omit_first': True, 'delimiter': ',', 'quotechar': '\''}
        )
        assert_queries = 4 if not settings.WIKI_CODE == 'wiki' else 2
        with self.assertNumQueries(assert_queries):
            response = self.client.get(url)
        self.assertEqual(200, response.status_code)

        resp_data = loads(response.content)['data']
        rows = resp_data['rows']
        self.assertEqual(1, len(rows))
        self.assertIn('ыыы', rows[0])

    def test_import_data_to_existing_grid(self):
        self._create_godzilla_grid()

        data = 'column1;column2;column3\nSTARTREK-2000;elisei;aaa'

        resp_data = self._upload_test_file('test.csv', data)
        cache_key = resp_data['cache_key']

        url = '{api_url}/{page_supertag}/.grid/import?'.format(api_url=self.api_url, page_supertag=self.supertag)

        url += urlencode(
            {
                'key': cache_key,
                'charset': 'windows-1251',
                'omit_first': True,
                'icolumn_1_to': 'manager',
                'icolumn_2_enabled': False,
            }
        )

        assert_queries = 11 if not settings.WIKI_CODE == 'wiki' else 9
        with self.assertNumQueries(assert_queries):
            response = self.client.get(url)

        self.assertEqual(200, response.status_code)

        resp_data = loads(response.content)['data']

        self.assertEqual(3, len(resp_data['import_columns']))
        self.assertEqual(None, resp_data['import_columns'][0]['icolumn_0_to'])
        self.assertEqual('manager', resp_data['import_columns'][1]['icolumn_1_to'])
        self.assertEqual('staff', resp_data['import_columns'][1]['icolumn_1_type'])

        self.assertEqual(3, resp_data['column_count'])
        self.assertEqual(1, resp_data['row_count'])
        self.assertEqual(4, len(resp_data['existing_columns']))
        existing_rows = resp_data['existing_rows']
        self.assertEqual(2, len(existing_rows))
        imported_rows = resp_data['imported_rows']
        self.assertEqual(1, len(imported_rows))
        self.assertEqual(2, len(resp_data['fields']))

    def test_save_imported_data(self):
        grid = self._create_godzilla_grid()

        data = 'column1;column2;column3\nыыы;эээ;fff'.encode('windows-1251')

        resp_data = self._upload_test_file('test.csv', data)
        cache_key = resp_data['cache_key']

        edit_events_qs = PageEvent.objects.filter(page=grid, event_type=PageEvent.EVENT_TYPES.edit)
        edit_events_before = edit_events_qs.count()

        assert_queries = 17 if not settings.WIKI_CODE == 'wiki' else 15
        with self.assertNumQueries(assert_queries):
            response = self.client.post(
                '{api_url}/{page_supertag}/.grid/import'.format(api_url=self.api_url, page_supertag=self.supertag),
                dict(
                    {
                        'key': cache_key,
                        'charset': 'windows-1251',
                        'omit_first': True,
                        'icolumn_0_to': 'name',
                        'icolumn_2_enabled': False,
                    }
                ),
            )

        self.assertEqual(200, response.status_code)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(self.supertag))
        result = loads(response.content)['data']['rows']
        self.assertEqual(len(result), 3)
        self.assertEqual('row N1', result[0][0]['raw'])
        self.assertEqual('row N2', result[1][0]['raw'])
        self.assertEqual('ыыы', result[2][0]['raw'])
        self.assertEqual(5, len(loads(response.content)['data']['structure']['fields']))
        self.assertEqual(edit_events_before + 1, edit_events_qs.count())

    def test_import_to_column_with_wrong_data_type(self):
        self._create_godzilla_grid()

        data = 'column1;column2\nыыы;эээ'

        resp_data = self._upload_test_file('test.csv', data)
        cache_key = resp_data['cache_key']

        # импортируем в существующий столбец
        response = self.client.post(
            '{api_url}/{page_supertag}/.grid/import'.format(api_url=self.api_url, page_supertag=self.supertag),
            dict({'key': cache_key, 'omit_first': True, 'icolumn_0_to': 'date'}),
        )

        self.assertEqual(409, response.status_code)
        resp_error = loads(response.content)['error']
        self.assertEqual(InvalidDataSentError.error_code, resp_error['error_code'])
        self.assertEqual(1, len(resp_error['message']))

        # импортируем в новый столбец
        response = self.client.post(
            '{api_url}/{page_supertag}/.grid/import'.format(api_url=self.api_url, page_supertag=self.supertag),
            dict({'key': cache_key, 'omit_first': True, 'icolumn_1_type': 'date'}),
        )

        self.assertEqual(409, response.status_code)
        resp_error = loads(response.content)['error']
        self.assertEqual(1, len(resp_error['message']))

    def test_save_imported_data_to_new_grid(self):
        data = "column1;column2;column3\n'value1','value2','value3'\n'aaa','bbb',"

        resp_data = self._upload_test_file('test.csv', data)
        cache_key = resp_data['cache_key']

        supertag = 'new_grid'

        create_events_qs = PageEvent.objects.filter(event_type=PageEvent.EVENT_TYPES.create)
        create_events_before = create_events_qs.count()

        assert_queries = 13 if not settings.WIKI_CODE == 'wiki' else 11
        with self.assertNumQueries(assert_queries):
            response = self.client.post(
                '{api_url}/{page_supertag}/.grid/import'.format(api_url=self.api_url, page_supertag=supertag),
                dict(
                    {
                        'key': cache_key,
                        'omit_first': True,
                        'delimiter': ',',
                        'quotechar': "'",
                        'icolumn_2_enabled': False,
                    }
                ),
            )

        self.assertEqual(200, response.status_code)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        result = loads(response.content)['data']['rows']
        self.assertEqual(len(result), 2)
        self.assertEqual('value1', result[0][0]['raw'])
        self.assertEqual('value2', result[0][1]['raw'])
        self.assertEqual('aaa', result[1][0]['raw'])
        self.assertEqual('bbb', result[1][1]['raw'])
        self.assertEqual(2, len(loads(response.content)['data']['structure']['fields']))
        self.assertEqual(create_events_before + 1, create_events_qs.count())

    def test_save_imported_data_to_new_grid_with_select_column(self):
        data = "column1;column2\n'value1','value2'\n'aaa','bbb'"

        resp_data = self._upload_test_file('test.csv', data)
        cache_key = resp_data['cache_key']

        supertag = 'new_grid'
        response = self.client.post(
            '{api_url}/{page_supertag}/.grid/import'.format(api_url=self.api_url, page_supertag=supertag),
            dict(
                {'key': cache_key, 'omit_first': True, 'delimiter': ',', 'quotechar': "'", 'icolumn_1_type': 'select'}
            ),
        )

        self.assertEqual(200, response.status_code)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        fields = loads(response.content)['data']['structure']['fields']
        self.assertEqual(2, len(fields))
        self.assertEqual('select', fields[1]['type'])
        self.assertTrue('options' in fields[1])
        self.assertListEqual(['bbb', 'value2'], sorted(fields[1]['options']))

    def test_wrong_cache_key(self):
        supertag = 'new_grid'
        response = self.client.post(
            '{api_url}/{page_supertag}/.grid/import'.format(api_url=self.api_url, page_supertag=supertag),
            dict({'key': 'blabla', 'omit_first': True, 'delimiter': ',', 'quotechar': "'", 'icolumn_2_enabled': False}),
        )
        self.assertEqual(409, response.status_code)

    def test_csv_reader(self):
        csv_data_rU = (
            '3ka.com.tr,no,xxx,\r\n724oyuncak.com,no,xxx,\r\nBatan Gemi(2),no,batangemi.com,\r\n'
            'Bele_e Gitti,no,belesegitti.com,\r\nBeyaz Burada,no,beyazburada.com,\r\n'
        )

        csv_data_rU_r = csv_data_rU.replace('\r\n', '\r')  # \r newline TOOLSADMIN-133
        csv_data_rU_n = csv_data_rU.replace('\r\n', '\n')  # \n newline

        for data in (csv_data_rU, csv_data_rU_r, csv_data_rU_n):
            reader = CsvReader(data, delimiter=',', quotechar='"').open()

            self.assertEqual(reader.column_count, 4)
            self.assertEqual(reader.row_count, 5)

    @override_settings(
        LIMIT__GRID_ROWS_COUNT=3,
    )
    def test_max_rows_count(self):
        url = '{api_url}/.import/grid/upload?'.format(api_url=self.api_url)

        file_name = 'test.csv'

        data = 'a;b\nc;d\ne;f\ng;h'

        content_type, body = encode_multipart_formdata([('someField', 'somedata')], [('filefield', file_name, data)])

        response = self.client.post(url, data=body, content_type=content_type)

        self.assertEqual(409, response.status_code)
        self.assertEqual('Maximum grid rows count exceeded', response.data['error']['message'][0])

    @override_settings(
        LIMIT__GRID_ROWS_COUNT=3,
    )
    def test_max_rows_count_existing_grid(self):
        grid = self._create_godzilla_grid()

        data = 'a;b\nc;d'

        resp_data = self._upload_test_file('test.csv', data)
        cache_key = resp_data['cache_key']

        url = '{api_url}/{page_supertag}/.grid/import?'.format(api_url=self.api_url, page_supertag=grid.supertag)

        url += urlencode({'key': cache_key})

        response = self.client.get(url)

        self.assertEqual(409, response.status_code)
        self.assertEqual('Maximum grid rows count exceeded', response.data['error']['message'][0])

    @override_settings(
        LIMIT__GRID_COLS_COUNT=2,
    )
    def test_max_columns_count(self):
        url = '{api_url}/.import/grid/upload?'.format(api_url=self.api_url)

        file_name = 'test.csv'

        data = 'a;b;c;d\ne;f;g;h'

        content_type, body = encode_multipart_formdata([('someField', 'somedata')], [('filefield', file_name, data)])

        response = self.client.post(url, data=body, content_type=content_type)

        self.assertEqual(409, response.status_code)
        self.assertEqual('Maximum grid columns count exceeded', response.data['error']['message'][0])

    @override_settings(
        LIMIT__WIKI_TEXT_FOR_GRID_CELL__BYTES=5,
    )
    def test_max_grid_cell_size(self):
        url = '{api_url}/.import/grid/upload?'.format(api_url=self.api_url)

        file_name = 'test.csv'

        data = 'qqq\nabcdef'

        content_type, body = encode_multipart_formdata([('someField', 'somedata')], [('filefield', file_name, data)])

        response = self.client.post(url, data=body, content_type=content_type)

        self.assertEqual(409, response.status_code)
        self.assertEqual('Maximum page size 0 Kb exceeded', response.data['error']['message'][0])
