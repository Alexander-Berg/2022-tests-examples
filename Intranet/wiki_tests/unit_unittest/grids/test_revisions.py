
from wiki.grids.models import Grid
from wiki.notifications.models import PageEvent
from wiki.pages.models import Revision
from wiki.utils import timezone
from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import BaseGridsTest

SIMPLE_GRID = """
{
  "title": "grid of checkboxes",
  "sorting": [],
  "fields": [
    {
      "name" : "what_is_love",
      "title" : "What is love?",
      "sorting" : true,
      "required" : false
    },
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

SIMPLE_GRID_AFTER_DONE_COLUMN_REMOVAL = """
{
  "title": "grid of checkboxes",
  "sorting": [],
  "fields": [
    {
      "name" : "what_is_love",
      "title" : "What is love?",
      "sorting" : true,
      "required" : false
    }
  ]
}
"""


class RevisionsTest(BaseGridsTest):
    def setUp(self):
        super(RevisionsTest, self).setUp()
        self.setGroupMembers()

    def test_events_created(self):
        """Test that page events are collapsed properly

        thasonic@
        * creation +1 edit page event, revision
        * insert + 1 page event, revision

        chapson@
        * insert + 1 events, revision
        * edit row + 0 events, add revision
        * change structure + 1 page event, revision
        * insert row + 1 event, revision
        * delete row - 0 event, no revision


        @see http://wiki.yandex-team.ru/WackoWiki/dev/grid/versions#problizhajjsheebudushhee"""
        grid_supertag = 'thasonic/revision'

        revisions_qs = Revision.objects.filter(page__supertag=grid_supertag)
        page_events = PageEvent.objects.filter(page__supertag=grid_supertag)
        qs = Grid.objects.all()
        before_revisions = revisions_qs.count()
        before_pevs = page_events.filter(event_type=PageEvent.EVENT_TYPES.create).count()
        before_grids = qs.count()

        self._create_grid(grid_supertag, SIMPLE_GRID, self.user_thasonic)

        grid = Grid.objects.get(supertag=grid_supertag)

        self.assertEqual(before_grids + 1, qs.count())  # added a grid
        self.assertEqual(before_revisions + 2, revisions_qs.count())  # added 1 revision
        self.assertEqual(before_pevs + 1, page_events.filter(event_type=PageEvent.EVENT_TYPES.create).count())
        last_pe = page_events.filter(author__username='thasonic').order_by('-id')[0]

        before_revisions = revisions_qs.count()
        before_pevs = page_events.count()

        self._add_row(grid_supertag, {'what_is_love': 'thasonic writes about it', 'done': ''})

        grid = Grid.objects.get(supertag=grid_supertag)
        key = grid.access_data[0]['__key__']

        self.assertEqual(before_revisions + 1, revisions_qs.count())
        self.assertEqual(before_pevs + 1, page_events.count())
        last_pe = page_events.filter(author__username='thasonic').order_by('-id')[0]
        self.assertEqual(last_pe.event_type, PageEvent.EVENT_TYPES.edit)
        self.assertTrue(key in last_pe.meta['data'])
        self.assertEqual(1, len(last_pe.meta['data'][key]))
        self.assertEqual(PageEvent.EVENT_TYPES.create, last_pe.meta['data'][key][0]['type'])
        self.assertTrue('revision_id' in last_pe.meta)

        self.client.login('kolomeetz')
        before_revisions = revisions_qs.count()
        before_pevs = page_events.count()

        self._add_row(grid_supertag, {'what_is_love': 'chapson writes about it', 'done': ''})

        grid = Grid.objects.get(supertag=grid_supertag)
        key = grid.access_data[0]['__key__']

        self.assertEqual(before_revisions + 1, revisions_qs.count())
        self.assertEqual(before_pevs + 1, page_events.count())

        revisions_qs.order_by('-id')[0]

        last_pe = page_events.order_by('-id')[0]
        last_pe.timeout = timezone.now()  # Make last PageEvent "too old"
        last_pe.save()

        before_revisions = revisions_qs.count()
        before_pevs = page_events.count()

        self._edit_row(grid_supertag, key, {'what_is_love': 'chapson already wrote about it', 'done': 'On'})

        self.assertEqual(before_revisions + 1, revisions_qs.count())  # revision is created (the previous is too old)
        self.assertEqual(before_pevs + 1, page_events.count())  # one page event created

        self._edit_row(grid_supertag, key, {'what_is_love': 'vladmos already wrote about it', 'done': 'On'})

        self.assertEqual(before_pevs + 1, page_events.count())  # no page event created

        last_pe = page_events.order_by('-id')[0]
        before_last_pe = page_events.order_by('-id')[1]
        self.assertEqual(2, len(last_pe.meta['data'][key]))
        self.assertEqual(PageEvent.EVENT_TYPES.create, before_last_pe.meta['data'][key][0]['type'])
        self.assertEqual(PageEvent.EVENT_TYPES.edit, last_pe.meta['data'][key][0]['type'])
        self.assertEqual(PageEvent.EVENT_TYPES.edit, last_pe.meta['data'][key][1]['type'])

        before_revisions = revisions_qs.count()
        before_pevs = page_events.count()

        self._remove_column(grid_supertag, 'done')

        self.assertEqual(before_revisions + 1, revisions_qs.count())  # revision is created
        self.assertEqual(before_pevs + 1, page_events.count())
        last_pe = page_events.order_by('-id')[0]
        self.assertEqual(PageEvent.EVENT_TYPES.edit, last_pe.event_type)
        self.assertEqual('structure', last_pe.meta['object_of_changes'])
        self.assertEqual(1, len(last_pe.meta['structure']))

        before_revisions = revisions_qs.count()
        before_pevs = page_events.count()

        self._add_row(grid_supertag, {'what_is_love': 'chapson writes about it'})

        grid = Grid.objects.get(supertag=grid_supertag)
        key = grid.access_data[0]['__key__']

        self._remove_row(grid_supertag, key)

        self.assertEqual(before_revisions + 2, revisions_qs.count())
        self.assertEqual(before_pevs + 1, page_events.count())
        last_pe = page_events.order_by('-id')[0]

        self.assertEqual(PageEvent.EVENT_TYPES.delete, last_pe.meta['data'][key][1]['type'])
        self.assertEqual(PageEvent.EVENT_TYPES.create, last_pe.meta['data'][key][0]['type'])
        self.assertEqual(1, len(last_pe.meta['data']))
