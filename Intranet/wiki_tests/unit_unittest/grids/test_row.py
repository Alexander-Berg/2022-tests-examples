
import datetime
from unittest import skip

import pytz
import ujson as json
from django.contrib.auth import get_user_model
from django.utils.encoding import smart_bytes

from wiki.grids.filter import FilterError, is_filtered_out
from wiki.grids.models import Grid
from wiki.grids.utils import insert_rows
from wiki.org import get_org
from wiki.pages.models import Revision
from intranet.wiki.tests.wiki_tests.common.utils import locmemcache
from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import BaseGridsTest

GRID_SINGLE_SELECT = """
{
  "title" : "List of conferences",
  "sorting" : [{"name" : "year", "type" : "asc"}],
  "fields" : [
    {
      "name" : "year",
      "title" : "Year of conference",
      "width" : "200px",
      "required" : true,
      "type" : "select",
      "options" : ["2009", "2010", "2011"]
    }
  ]
}
"""

GRID_MULTIPLE_SELECT = """
{
  "title" : "List of conferences",
  "fields" : [
    {
      "name" : "year",
      "title" : "Year of conference",
      "width" : "200px",
      "type" : "select",
      "multiple" : true,
      "options" : ["2009", "2010", "2011"]
    }
  ]
}
"""


def make_a_grid(structure):
    grid = Grid()
    grid.change_structure(structure)
    grid.tag = 'thasonic/withdate'
    grid.supertag = 'thasonic/withdate'
    grid.page_type = Grid.TYPES.GRID
    grid.status = 1
    grid.last_author = get_user_model().objects.get(username='thasonic')
    grid.modified_at = datetime.datetime(2001, 1, 1, tzinfo=pytz.utc)
    grid.org = get_org()
    grid.save()
    grid.authors.add(get_user_model().objects.get(username='thasonic'))
    return grid


class RowTest(BaseGridsTest):
    def setUp(self):
        super(RowTest, self).setUp()
        grid = Grid()
        grid.change_structure(GRID_SINGLE_SELECT)
        grid.tag = 'thasonic/grid3'
        grid.supertag = 'thasonic/grid3'
        grid.page_type = Grid.TYPES.GRID
        grid.status = 1
        grid.last_author = self.user_thasonic
        grid.modified_at = datetime.datetime(2001, 1, 1, tzinfo=pytz.utc)
        self.single_select_grid = grid
        grid.org = get_org()
        grid.save()
        grid.authors.add(self.user_thasonic)
        Revision.objects.create_from_page(grid)

        grid = Grid()
        grid.change_structure(GRID_MULTIPLE_SELECT)
        grid.tag = 'thasonic/grid4'
        grid.supertag = 'thasonic/grid4'
        grid.page_type = Grid.TYPES.GRID
        grid.status = 1
        grid.last_author = self.user_thasonic
        grid.modified_at = datetime.datetime(2001, 1, 1, tzinfo=pytz.utc)
        self.multiple_select_grid = grid
        grid.org = get_org()
        grid.save()
        grid.authors.add(self.user_thasonic)
        Revision.objects.create_from_page(grid)

    def test_single_select_fields(self):
        # I can set 1 value in single select field and update it
        self._add_row(self.single_select_grid.supertag, {'year': '2009'})

        grid = Grid.objects.get(id=self.single_select_grid.id)
        data = grid.access_data

        self.assertEqual(data[0]['year']['raw'][0], '2009')

        self._edit_row(self.single_select_grid.supertag, data[0]['__key__'], {'year': '2010'})

        grid = Grid.objects.get(id=self.single_select_grid.id)
        data = grid.access_data

        self.assertEqual(data[0]['year']['raw'][0], '2010')
        self.assertEqual(len(data[0]['year']['raw']), 1)

    def test_multiple_select_fields(self):
        # I can set a few values in single select field and replace them
        self._add_row(self.multiple_select_grid.supertag, {'year': ['2009', '2010']})

        grid = Grid.objects.get(id=self.multiple_select_grid.id)
        data = grid.access_data

        self.assertEqual(data[0]['year']['raw'], ['2009', '2010'])

        self._edit_row(self.multiple_select_grid.supertag, data[0]['__key__'], {'year': ['2010', '2011']})

        grid = Grid.objects.get(id=self.multiple_select_grid.id)
        data = grid.access_data

        self.assertEqual(data[0]['year']['raw'], ['2010', '2011'])
        self.assertEqual(len(data[0]['year']['raw']), 2)

    def test_wiki_3384(self):
        from wiki.pages.models import Access

        self.client.login('chapson')
        access = Access(is_owner=True, page=self.single_select_grid)
        access.save()

        self._add_row(self.single_select_grid.supertag, {'year': '2009'}, expected_status_code=403)

    def test_structure_unaware_requests(self):
        # Грид позволяет сохранять запросы, не знающие структуру грида.
        # WIKI-4367
        self._add_row(self.multiple_select_grid.supertag, {})

        grid = Grid.objects.get(id=self.multiple_select_grid.id)
        self.assertEqual(1, len(grid.access_data))
        self.assertTrue('year' in grid.access_data[0])

    def test_no_dubious_requests(self):
        # Неверным считается запрос, в котором есть столбцы, которых нет в структуре грида.
        # WIKI-4367
        self._add_row(self.multiple_select_grid.supertag, {'dubious': ''}, expected_status_code=409)

        hashes = insert_rows(self.multiple_select_grid, [{'year': '2010'}], None)
        self.multiple_select_grid.save()

        self._edit_row(self.multiple_select_grid.supertag, hashes[0], {'dubious': ''}, expected_status_code=409)

    def test_cannot_add_row_with_unexistent_select_options(self):
        # В select нельзя дописывать произвольные варианты.
        self._add_row(self.single_select_grid.supertag, {'year': '2009'})

        grid = Grid.objects.get(id=self.single_select_grid.id)
        data = grid.access_data

        self._edit_row(self.single_select_grid.supertag, data[0]['__key__'], {'year': '1999'}, expected_status_code=409)

        self._add_row(self.single_select_grid.supertag, {'year': '2000'}, expected_status_code=409)

    def test_empty_date(self):
        grid = make_a_grid(GRID_WITH_DATE)
        Revision.objects.create_from_page(grid)
        self._add_row(grid.supertag, {'year': ''})

    @skip('WIKI-9427')
    def test_auto_escaping(self):
        request_string = '/%s/.row.json?action=insert' % (self.grid.supertag,)
        bad_string = '<a></a>'
        response = self.client.post(request_string, {'date': '', 'name': bad_string})
        result = json.loads(response.content)
        self.assertTrue('&lt;a&gt;&lt;/a&gt;' in result['data']['name']['view'].strip())
        request_string = '/%s/.row.json?action=edit&row_id=%s' % (self.grid.supertag, result['__key__'])
        response = self.client.post(request_string, {'date': '', 'name': '<script />'})
        result = json.loads(response.content)
        self.assertTrue('&lt;script /&gt;' in result['data']['name']['view'].strip())

    def test_wiki_3351(self):
        """ if after=='last' insert into the end of list """
        self._add_row(self.single_select_grid.supertag, {'year': '2009'})
        self._add_row(self.single_select_grid.supertag, {'year': '2010'}, after_id='last')

        grid = Grid.objects.get(id=self.single_select_grid.id)
        data = grid.access_data

        self.assertEqual('2010', data[1]['year']['raw'][0])

    def test_move_row(self):
        # Ручка перемещения строки табличного списка wiki-3348
        grid = self.single_select_grid
        row_1, row_2, row_3 = insert_rows(
            grid,
            [
                {'year': 2009},
                {'year': 2010},
                {'year': 2011},
            ],
            None,
        )
        grid.save()
        Revision.objects.create_from_page(grid)

        self._move_row(self.single_select_grid.supertag, row_id=row_3, after_id=row_1, before_id=row_2)

        renewed_grid = Grid.objects.get(supertag=grid.supertag)
        data = renewed_grid.access_data
        self.assertEqual(data[0]['__key__'], row_1)
        self.assertEqual(data[1]['__key__'], row_3)
        self.assertEqual(data[2]['__key__'], row_2)

    def test_checkbox_field(self):
        # Поле типа checkbox jira/wiki-3403
        self._create_gorilla_grids()

        self._add_row(self.checkbox_grid.supertag, {'done': ''})

        grid = Grid.objects.get(id=self.checkbox_grid.id)

        self.assertEqual(False, grid.access_data[0]['done']['raw'])

        key = grid.access_data[0]['__key__']

        self._edit_row(self.checkbox_grid.supertag, key, {'done': 'On'})

        grid = Grid.objects.get(id=self.checkbox_grid.id)

        self.assertEqual(True, grid.access_data[0]['done']['raw'])

        self._edit_row(self.checkbox_grid.supertag, key, {'done': ''})

        grid = Grid.objects.get(id=self.checkbox_grid.id)

        self.assertEqual(False, grid.access_data[0]['done']['raw'])

        CHECKBOX_GRID = """
{
  "title": "grid of checkboxes",
  "sorting": [],
  "fields": [
    {
      "name" : "done",
      "title" : "Is done?",
      "sorting" : true,
      "required" : false,
      "markdone" : true,
      "type" : "checkbox"
    },
    {
      "name" : "first",
      "title" : "Some text",
      "sorting" : true,
      "required" : false,
      "type" : "string"
    },
    {
      "name" : "last",
      "title" : "Some text",
      "sorting" : true,
      "required" : false,
      "type" : "string"
    }
  ]
}
"""
        self.checkbox_grid.change_structure(CHECKBOX_GRID)
        self.checkbox_grid.save()
        Revision.objects.create_from_page(self.checkbox_grid)

        self._add_row(self.checkbox_grid.supertag, {'first': 'Anton', 'last': 'Chaporgin'})

        grid = Grid.objects.get(id=self.checkbox_grid.id)
        key = grid.access_data[0]['__key__']

        self._edit_row(self.checkbox_grid.supertag, key, {'done': 'On'})
        self._edit_row(self.checkbox_grid.supertag, key, {'first': 'Mark'})

        grid = Grid.objects.get(id=self.checkbox_grid.id)

        self.assertEqual(grid.access_data[0]['last']['raw'], 'Chaporgin')
        self.assertEqual(grid.access_data[0]['first']['raw'], 'Mark')

    def test_wiki_3396(self):
        # Test wiki formatting in string-like fields
        self._create_gorilla_grids()

        data_to_format = """=== Diablo 3
**Probably** the best game for chapson.
        """

        self._add_row(self.grid.supertag, {'name': data_to_format})

        grid = Grid.objects.get(id=self.grid.id)
        key = grid.access_data[0]['__key__']

        data = grid.access_data[grid.access_idx[key]]
        self.assertEqual(data['name']['raw'], data_to_format)


NUMBER_GRID = """
{
  "title" : "List of tickets",
  "width" : "100%",
  "sorting" : [],
  "fields" : [
    {
      "name" : "suns",
      "title" : "Number of suns you have seen",
      "type" : "number"
    }
  ]
}"""


class NumberRowTest(BaseGridsTest):
    def setUp(self):
        super(NumberRowTest, self).setUp()
        self._create_gorilla_grids()

    def test_assignee_field(self):
        # Поле "assignee" wiki-3536
        self.grid.change_structure(NUMBER_GRID)
        self.grid.save()
        Revision.objects.create_from_page(self.grid)

        self._add_row(self.grid.supertag, {'suns': 14051519191182387})

        grid = Grid.objects.get(id=self.grid.id)
        result = grid.access_data[0]

        self.assertEqual(14051519191182387, result['suns']['raw'])

        self._edit_row(self.grid.supertag, result['__key__'], {'suns': '14051519191182387'})

        grid = Grid.objects.get(id=self.grid.id)
        result = grid.access_data[0]

        self.assertEqual(14051519191182387, result['suns']['raw'])

        self._add_row(self.grid.supertag, {'suns': '10.01'})

        grid = Grid.objects.get(id=self.grid.id)
        result = grid.access_data[0]

        self.assertEqual(10.01, result['suns']['raw'])


GRID_WITH_DATE = """
{
  "title" : "List of conferences",
  "fields" : [
    {
      "name" : "year",
      "title" : "Year of conference",
      "width" : "200px",
      "type" : "date",
      "multiple" : true
    }
  ]
}
"""


@locmemcache('grid_filter')
class FilteredRowTest(BaseGridsTest):
    def setUp(self):
        super(FilteredRowTest, self).setUp()
        self._create_gorilla_grids()

    def _filter(self, filter_text):
        # Это ок, т.к. во всех тестах мы добавляем вставляем ровно одну строку.
        grid = Grid.objects.get(id=self.grid.id)
        row_id = Grid.objects.get(id=self.grid.id).access_data[0]['__key__']
        result = {}
        try:
            result['__filtered_out__'] = is_filtered_out(filter_text, grid, row_id)
        except FilterError as error:
            result['__filtered_out__'] = None
            result['__filter_error__'] = smart_bytes(error)
        return result

    def test_insert_compliant_row(self):
        self._add_row(self.grid.supertag, {'name': 'John', 'date': '2011-11-11', 'is_done': True})
        result = self._filter('[is_done] = 1')
        self.assertFalse(result['__filtered_out__'])

    def test_insert_uncompliant_row(self):
        self._add_row(self.grid.supertag, {'name': 'John', 'date': '2011-11-11', 'is_done': True})
        result = self._filter('[is_done] = 0')
        self.assertTrue(result['__filtered_out__'])

    def test_insert_row_with_unparsible_filter(self):
        self._add_row(self.grid.supertag, {'name': 'John', 'date': '2011-11-11', 'is_done': True})
        result = self._filter('all work and no play')
        self.assertTrue(result.get('__filter_error__'))

    def test_edit_compliant_row(self):
        self._edit_row(self.grid.supertag, '1', {'is_done': True})
        result = self._filter('[is_done] = 1')
        self.assertFalse(result['__filtered_out__'])

    def test_edit_uncompliant_row(self):
        self._edit_row(self.grid.supertag, '1', {'is_done': True})
        result = self._filter('[is_done] = 0')
        self.assertTrue(result['__filtered_out__'])

    def test_edit_row_with_unparsible_filter(self):
        self._edit_row(self.grid.supertag, '1', {'is_done': True})
        result = self._filter('all work and no play')
        self.assertTrue(result.get('__filter_error__'))
