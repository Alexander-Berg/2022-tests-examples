from wiki import access
from wiki.actions.classes.base_action import ParamsWrapper
from wiki.actions.classes.grid import Grid
from wiki.grids.models import Grid as GridModel
from wiki.grids.models import Revision
from wiki.grids.utils import insert_rows
from wiki.org import get_org
from wiki.utils.errors import InputValidationError

from intranet.wiki.tests.wiki_tests.common.wiki_client import WikiClient
from intranet.wiki.tests.wiki_tests.unit_unittest.actions.base import OldHttpActionTestCase
from intranet.wiki.tests.wiki_tests.unit_unittest.grids.base import GRID_STRUCTURE


class GridTest(OldHttpActionTestCase):
    client_class = WikiClient

    def setUp(self):
        super(GridTest, self).setUp()

        thasonic = self.request.user
        chapson = self.get_or_create_user('chapson')

        self.grid = self._create_grid(
            tag='thasonic/grid',
            authors_to_add=[thasonic],
            rows=[
                {'name': 'boo', 'date': '2011-11-11', 'is_done': True},
                {'name': 'far', 'date': '2011-12-12', 'is_done': False},
            ],
        )
        self.closed_grid = self._create_grid(tag='thasonic/closed-grid', authors_to_add=[chapson])
        access.set_access(self.closed_grid, access.TYPES.OWNER, chapson)

        # absorb('request', dummy_request_for_grids())

    def _create_grid(self, tag, authors_to_add, rows=None):
        grid = GridModel()
        grid.change_structure(GRID_STRUCTURE)
        grid.last_author = authors_to_add[0]
        grid.tag = tag
        grid.supertag = tag
        grid.page_type = GridModel.TYPES.GRID
        grid.status = 1
        grid.org = get_org()
        if rows:
            insert_rows(grid, rows, None)
        grid.save()
        grid.authors.add(*authors_to_add)
        Revision.objects.create_from_page(grid)
        return grid

    def _get(self, dict_params):
        return Grid(ParamsWrapper(dict_params), self.request).json_for_get(dict_params)

    def _encode(self, dict_params, list_params=None):
        params = ParamsWrapper(dict_params, list_params)
        return Grid(params, self.request).encode_params(params)

    def test_encode_page_param(self):
        encoded_params = self._encode({'page': self.grid.tag})
        self.assertTrue('page' in encoded_params, '\'page\' must be in encoded params')
        self.assertEqual(self.grid.tag, encoded_params['page'])

    def test_encode_url_param(self):
        encoded_params = self._encode({'url': self.grid.tag})
        self.assertTrue('page' in encoded_params, '\'page\' must be in encoded params')
        self.assertEqual(self.grid.tag, encoded_params['page'])

    def test_encode_first_ordered_param(self):
        encoded_params = self._encode({}, [(None, self.grid.tag), (None, 'other-grid')])
        self.assertTrue('page' in encoded_params, '\'page\' must be in encoded params')
        self.assertEqual(self.grid.tag, encoded_params['page'])

    def test_encode_page_url_and_first_ordered_param(self):
        encoded_params = self._encode({'page': self.grid.tag, 'url': 'other-grid-1'}, [(None, 'other-grid-2')])
        self.assertTrue('page' in encoded_params, '\'page\' must be in encoded params')
        self.assertEqual(self.grid.tag, encoded_params['page'])

    def test_redirect_to_grid(self):
        redirect_grid = self.create_page(
            tag='redirect-grid',
            authors_to_add=[self.user],
        )
        redirect_grid.redirects_to = self.grid
        redirect_grid.save()

        encoded_params = self._encode({'page': redirect_grid.tag})
        self.assertEqual(self.grid.tag, encoded_params['page'])

    def test_nonexistent_grid(self):
        self.assertRaisesMessage(
            expected_exception=InputValidationError,
            expected_message='actions.Grid:NonexistentGridPage',
            callable_obj=lambda: self._encode({'page': 'no-such-grid'}),
        )

    def test_normal_grid(self):
        grid_json = self._get({'page': self.grid.tag})
        self.assertEqual(2, len(grid_json['rows']), 'Grid must contain 2 rows')

    def test_not_a_grid(self):
        page = self.create_page(tag='simple-page', authors_to_add=[self.user])
        self.assertRaisesMessage(
            expected_exception=InputValidationError,
            expected_message='actions.Grid:Not a grid',
            callable_obj=lambda: self._get({'page': page.tag}),
        )

    def test_access_denied(self):
        self.assertRaisesMessage(
            expected_exception=InputValidationError,
            expected_message='actions.Grid:AccessDenied',
            callable_obj=lambda: self._get({'page': self.closed_grid.tag}),
        )
