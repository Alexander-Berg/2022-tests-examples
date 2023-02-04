
import datetime

from wiki.grids.models import Grid
from wiki.grids.utils import changes_of_structure, insert_rows
from wiki.org import get_org
from wiki.utils.supertag import tag_to_supertag
from wiki.pages.models import Page, Revision
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase

GRID_STRUCTURE = """
{
  "title" : "List of conferences",
  "width" : "100%",
  "sorting" : [
    {"name" : "name", "type" : "asc"},
    {"name" : "date", "type" : "desc"}
    ],
  "done" : true,
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
    }
  ]
}
"""

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
    }
  ]
}
"""


# Есть тесты, которые создают гриды, но при этом их не хотелось
# бы наследовать от BaseGridsTest. Для них сделана эта helper-функция.
# test_instance должен быть унаследован от BaseApiTestCase.
def create_grid(test_instance, grid_tag, grid_structure, user):
    response = test_instance.client.post(
        '{api_url}/{tag}/.grid/create'.format(api_url=test_instance.api_url, tag=grid_tag),
        dict(title='grid title'),
    )
    test_instance.assertEqual(200, response.status_code)

    grid = Grid.active.get(supertag=tag_to_supertag(grid_tag))

    previous_structure = grid.access_structure.copy()

    grid.change_structure(grid_structure)
    grid.save()

    changes_of_structure(user, grid, previous_structure)
    return grid


class BaseGridsTest(BaseApiTestCase):
    def setUp(self):
        super(BaseGridsTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')

    def _create_grid(self, tag, structure, user):
        create_grid(self, tag, structure, user)

    def _add_row(self, grid_tag, row_data, after_id='-1', expected_status_code=200):
        grid = Grid.active.get(supertag=tag_to_supertag(grid_tag))
        response = self.client.post(
            '{api_url}/{supertag}/.grid/change'.format(api_url=self.api_url, supertag=grid_tag),
            data={
                'version': grid.get_page_version(),
                'changes': [
                    {
                        'added_row': {
                            'after_id': after_id,
                            'data': row_data,
                        }
                    }
                ],
            },
        )
        self.assertEqual(expected_status_code, response.status_code)

    def _edit_row(self, grid_tag, row_id, new_row_data, expected_status_code=200):
        grid = Grid.active.get(supertag=tag_to_supertag(grid_tag))
        response = self.client.post(
            '{api_url}/{supertag}/.grid/change'.format(api_url=self.api_url, supertag=grid_tag),
            data={
                'version': grid.get_page_version(),
                'changes': [
                    {
                        'edited_row': {
                            'id': row_id,
                            'data': new_row_data,
                        }
                    }
                ],
            },
        )
        self.assertEqual(expected_status_code, response.status_code)

    def _move_row(self, grid_tag, row_id, after_id, before_id, expected_status_code=200):
        grid = Grid.active.get(supertag=tag_to_supertag(grid_tag))
        response = self.client.post(
            '{api_url}/{supertag}/.grid/change'.format(api_url=self.api_url, supertag=grid_tag),
            data={
                'version': grid.get_page_version(),
                'changes': [
                    {
                        'row_moved': {
                            'id': row_id,
                            'after_id': after_id,
                            'before_id': before_id,
                        }
                    }
                ],
            },
        )
        self.assertEqual(expected_status_code, response.status_code)

    def _remove_row(self, grid_tag, row_id, expected_status_code=200):
        grid = Grid.active.get(supertag=tag_to_supertag(grid_tag))
        response = self.client.post(
            '{api_url}/{supertag}/.grid/change'.format(api_url=self.api_url, supertag=grid_tag),
            data={
                'version': grid.get_page_version(),
                'changes': [
                    {
                        'removed_row': {
                            'id': row_id,
                        }
                    }
                ],
            },
        )
        self.assertEqual(expected_status_code, response.status_code)

    def _remove_column(self, grid_tag, column_name, expected_status_code=200):
        grid = Grid.active.get(supertag=tag_to_supertag(grid_tag))
        response = self.client.post(
            '{api_url}/{supertag}/.grid/change'.format(api_url=self.api_url, supertag=grid_tag),
            data={
                'version': grid.get_page_version(),
                'changes': [
                    {
                        'removed_column': {
                            'name': column_name,
                        }
                    }
                ],
            },
        )
        self.assertEqual(expected_status_code, response.status_code)

    def _get_grid(self, grid_tag):
        return self.client.get('{api_url}/{supertag}/.grid'.format(api_url=self.api_url, supertag=grid_tag))

    def _create_gorilla_grids(self):
        # Создает всякие гриды семейства приматов, заимствованные
        # из старых тестов гридов.
        grid = Grid()
        grid.change_structure(GRID_STRUCTURE)
        grid.tag = 'thasonic/грид'
        grid.supertag = 'thasonic/grid'
        grid.page_type = Page.TYPES.GRID
        grid.status = 1
        grid.last_author = self.user_thasonic
        grid.modified_at = datetime.datetime(2001, 1, 1)
        data = [
            {
                'name': 'Sussex search',
                'date': '2010-05-10',
                'is_done': False,
            },
            {
                'name': 'iCode',
                'date': '2011-06-30',
                'is_done': True,
            },
        ]
        self.hash1, self.hash = insert_rows(grid, data, None)
        grid.org = get_org()
        grid.save()
        grid.authors.add(self.user_thasonic)
        self.grid = grid
        Revision.objects.create_from_page(grid)

        grid = Grid()
        grid.change_structure(CHECKBOX_GRID)
        grid.tag = 'thasonic/checkboxgrid'
        grid.supertag = 'thasonic/checkboxgrid'
        grid.page_type = Page.TYPES.GRID
        grid.status = 1
        grid.last_author = self.user_thasonic
        grid.modified_at = datetime.datetime(2001, 1, 1)
        insert_rows(
            grid,
            [
                {'done': True},
                {'done': False},
            ],
            None,
        )
        grid.org = get_org()
        grid.save()
        grid.authors.add(self.user_thasonic)
        self.checkbox_grid = grid
        Revision.objects.create_from_page(grid)
