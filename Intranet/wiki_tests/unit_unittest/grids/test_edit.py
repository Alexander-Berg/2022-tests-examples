
from wiki.grids.models import Grid
from wiki.notifications.generators.base import EventTypes
from wiki.notifications.models import PageEvent
from wiki.pages.models import PageWatch, Revision
from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import BaseGridsTest
from intranet.wiki.tests.wiki_tests.unit_unittest.grids.base import NEW_GRID_STRUCTURE


class EditTest(BaseGridsTest):
    def test_new_grid(self):
        grid_supertag = 'wiki/evolution'

        before = Grid.objects.all().count()
        before_pageevent = PageEvent.objects.filter(event_type=EventTypes.create).count()

        self._create_grid(grid_supertag, NEW_GRID_STRUCTURE, self.user_thasonic)

        after = Grid.objects.all().count()
        self.assertEqual(before + 1, after, 'Must have created 1 grid')
        result_grid = Grid.objects.get(supertag=grid_supertag)
        after_pageevent = PageEvent.objects.filter(event_type=EventTypes.create, page=result_grid).count()
        self.assertEqual(before_pageevent + 1, after_pageevent, 'Must have create 1 PageEvent')
        revisions = Revision.objects.filter(page=result_grid)
        self.assertEqual(revisions.count(), 2, 'Must have created two revisions')
        new_grid = Grid.objects.filter(supertag=grid_supertag)
        self.assertNotEqual(list(new_grid), [], 'Grid must have been created')

    def test_wiki_3304(self):
        """chapson, who subscribed to a cluster, must be subscribed to a new subgrid.
        But kolomeetz must not. Thasonic creates the grid, so he must also be subscribed"""
        wiki_page = self.create_page(tag='wiki', authors_to_add=[self.user_kolomeetz])
        PageWatch(user=self.user_chapson.username, page=wiki_page, is_cluster=True).save()
        PageWatch(user=self.user_kolomeetz.username, page=wiki_page, is_cluster=False).save()
        grid_supertag = 'wiki/evolution'
        self.assertEqual(0, Grid.objects.filter(supertag=grid_supertag).count())
        self._create_grid(grid_supertag, NEW_GRID_STRUCTURE, self.user_thasonic)
        self.assertEqual(1, Grid.objects.filter(supertag=grid_supertag).count())
        grid = Grid.objects.get(supertag=grid_supertag)
        self.assertEqual(0, PageWatch.objects.filter(page=grid, user=self.user_kolomeetz.username).count())
        self.assertEqual(
            1, PageWatch.objects.filter(page=grid, user=self.user_chapson.username, is_cluster=True).count()
        )
        self.assertEqual(
            1, PageWatch.objects.filter(page=grid, user=self.user_thasonic.username, is_cluster=True).count()
        )
