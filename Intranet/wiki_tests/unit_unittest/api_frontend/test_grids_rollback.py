
from wiki.grids.models import Grid
from wiki.notifications.models import PageEvent
from wiki.pages.models import Revision
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


class GridRollbackViewTest(BaseGridsTest):
    def _create_gorilla_grid(self):
        supertag = 'pulca'
        self._create_grid(supertag, GRID_STRUCTURE, self.user_thasonic)

        self._add_row(supertag, dict(name='some name', date='2013-08-12', is_done=''))
        self._add_row(supertag, dict(name='another name', date='2015-06-11', is_done=''))

        return Grid.active.get(supertag=supertag)

    def test_rollback_to_revision(self):
        grid = self._create_gorilla_grid()

        self.assertEqual(4, Revision.objects.filter(page=grid).count())
        self.assertEqual(2, len(grid.access_data))

        revision_of_first_added_row = Revision.objects.filter(page=grid).order_by('id')[2]

        edit_events_qs = PageEvent.objects.filter(page=grid, event_type=PageEvent.EVENT_TYPES.edit)
        edit_events_before = edit_events_qs.count()

        response = self.client.post(
            '{api_url}/{grid_supertag}/.grid/revisions/{revision_id}/rollback'.format(
                api_url=self.api_url, grid_supertag=grid.supertag, revision_id=revision_of_first_added_row.id
            ),
        )

        grid = self.refresh_objects(grid)

        self.assertEqual(200, response.status_code)
        self.assertEqual(1, len(grid.access_data))
        self.assertEqual(edit_events_before + 1, edit_events_qs.count())
        self.assertEqual('old_revision', edit_events_qs.order_by('-id')[0].meta['object_of_changes'])

    def test_rollback_to_nonexistent_revision(self):
        grid = self._create_gorilla_grid()

        response = self.client.post(
            '{api_url}/{grid_supertag}/.grid/revisions/{revision_id}/rollback'.format(
                api_url=self.api_url, grid_supertag=grid.supertag, revision_id='583904583'
            ),
        )

        self.assertEqual(404, response.status_code)
