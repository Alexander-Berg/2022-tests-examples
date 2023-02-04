
import datetime
from unittest import skipIf

from django.conf import settings

from wiki import access as wiki_access
from wiki.grids.models import Grid
from wiki.pages.models import Page
from wiki.utils.timezone import make_aware_current
from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import BaseGridsTest

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


class GridCloneViewTest(BaseGridsTest):
    @skipIf(settings.IS_BUSINESS, 'wait for fixing for business')
    def test_clone_access(self):
        supertag = 'grid'
        self._create_grid(supertag, GRID_STRUCTURE, self.user_thasonic)

        self._add_row(supertag, dict(name='some name', date='2013-08-12', is_done=''))

        grids = Grid.objects.all()
        before = grids.count()

        page = Page(
            supertag='search', tag='search', modified_at=make_aware_current(datetime.datetime(2010, 10, 10, 10, 10, 10))
        )
        page.save()
        page.authors.add(self.user_chapson)
        wiki_access.set_access(page, wiki_access.TYPES.OWNER, self.user_chapson)

        response = self.client.post(
            '{api_url}/{grid_supertag}/.grid/clone'.format(api_url=self.api_url, grid_supertag=supertag),
            {
                'destination': 'search/emptypage/loseaccess',
                'with_data': 'true',
            },
        )
        self.assertEqual(403, response.status_code)
        self.assertIn(
            b'Forbidden, cause you will not have access to the grid',
            response.content,
        )
        self.assertEqual(before, grids.count())

        response = self.client.post(
            '{api_url}/{grid_supertag}/.grid/clone'.format(api_url=self.api_url, grid_supertag=supertag),
            {
                'destination': 'a/pagE/for/grid',
                'with_data': 'true',
            },
        )

        self.assertEqual(200, response.status_code)
        self.assertEqual(before + 1, grids.count())
