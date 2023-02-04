from wiki.grids.logic.grids_import_redux import import_csv
from wiki.grids.models import Grid, Revision
from intranet.wiki.tests.wiki_tests.common.data_helper import read_test_asset
from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import BaseGridsTest


class ReduxTest(BaseGridsTest):
    def test_import(self):
        data = read_test_asset('csv_import/1.csv')
        n = self.get_or_create_user('neofelis')
        tag = 'users/neofelis/huge'
        grid = Grid(
            owner=n,
            last_author=n,
            status=True,
            page_type=Grid.TYPES.GRID,
            tag=tag,
            supertag=tag,
            org=None,
            title='Test',
        )
        grid.save()
        grid.authors.add(n)
        import_csv(grid, data.decode(), delimiter=',')
        grid.save()
        Revision.objects.create_from_page(grid)
