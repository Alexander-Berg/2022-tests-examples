
import datetime
import time

import pytz

from wiki.grids.models import Grid
from wiki.grids.utils import insert_rows
from wiki.grids.utils.base import HASH_KEY
from wiki.notifications.generators.base import EventTypes
from wiki.notifications.models import PageEvent
from wiki.org import get_org
from wiki.pages.models import Revision
from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import BaseGridsTest
from intranet.wiki.tests.wiki_tests.unit_unittest.grids.base import (
    GRID_STRUCTURE,
    GRID_WITHOUT_FIELDS,
    NEW_GRID_STRUCTURE,
)


class ModelsGridsTest(BaseGridsTest):
    def test_grid_sync_data(self):
        """staff fields in data must be updated with "transformed" and "sort" values"""
        grid = Grid(
            tag='thasonic/grid7',
            supertag='thasonic/grid7',
            last_author=self.user_thasonic,
            status=1,
            page_type=Grid.TYPES.GRID,
            modified_at=datetime.datetime(2001, 1, 1, tzinfo=pytz.utc),
            org=get_org(),
        )
        grid.save()
        grid.authors.add(self.user_thasonic)
        grid.change_structure(NEW_GRID_STRUCTURE)
        grid.save()
        Revision.objects.create_from_page(grid)

        self.client.login('thasonic')

        self._add_row(grid.supertag, {'participants': 'chapson', 'date': '2010-10-10'})

        updated_grid = Grid.objects.get(id=grid.id)
        self.assertTrue('sort' in updated_grid.access_data[0]['participants'])
        self.assertTrue('transformed' in updated_grid.access_data[0]['participants'])
        self.assertEqual('Anton Chaporgin', updated_grid.access_data[0]['participants']['transformed']['chapson'])
        self.assertEqual('Chaporgin Anton', updated_grid.access_data[0]['participants']['sort'])

        self._edit_row(grid.supertag, 1, {'participants': 'kolomeetz', 'date': '2010-10-10'})

        updated_grid = Grid.objects.get(id=grid.id)
        self.assertEqual('Kolomeetz Konstantin', updated_grid.access_data[0]['participants']['sort'])
        self.assertEqual(
            'Konstantin Kolomeetz', updated_grid.access_data[0]['participants']['transformed']['kolomeetz']
        )

    def test_grid_serialized_properties(self):
        grid = Grid(tag='somewhere', supertag='somewhere')
        grid.change_structure(GRID_STRUCTURE)
        self.assertFalse(bool(grid.access_data), 'No data yet')
        self.assertFalse(bool(grid.access_idx), 'No indexes yet')
        data = {'name': 'Sussex search', 'date': '2011-05-10'}

        hash1 = insert_rows(grid, [data], None)[0]

        self.assertFalse(len(grid.access_data) != 1, 'Must be one row')
        self.assertFalse(len(grid.access_idx) != 1, 'Must be one index')
        self.assertEqual(0, grid.access_idx[hash1])  # 'Idx must point to 0'
        self.assertEqual({'raw': '2011-05-10'}, grid.access_data[grid.access_idx[hash1]]['date'])

        data = {
            'name': 'iCode',
            'date': '2011-06-30',
        }
        hash = insert_rows(grid, [data], None, hash1)[0]

        self.assertTrue(hash in grid.access_idx)  # "Didn't add hash to index"
        self.assertEqual(2, len(grid.access_data))  # "Must be 2 rows"
        self.assertEqual(0, grid.access_idx[hash1])  # "First row is not second"
        self.assertEqual(1, grid.access_idx[hash])  # "Second row is not second"

        before = dict(grid.access_data[grid.access_idx[hash]])
        grid.change(hash, {'name': 'Yandex SHAD'})
        after = grid.access_data[grid.access_idx[hash]]
        self.assertFalse(after['name'] == before['name'], 'I changed name')
        self.assertTrue(after['date'] == before['date'], 'I did not change date')
        self.assertTrue(len(grid.access_data) == 2, 'Length of data list changed')
        self.assertTrue(len(grid.access_idx) == 2, 'Length of index dictionary changed')

        grid.remove([hash])
        self.assertTrue(len(grid.access_data) == 1, 'I removed a row')
        self.assertTrue(len(grid.access_idx) == 1, 'I removed a row')
        self.assertFalse(hash in grid.access_idx, 'I removed this row')

    def test_structure_changes(self):
        grid = Grid(supertag='somewhere', tag='somewhere')
        grid.change_structure(GRID_STRUCTURE)
        grid.title = 'Some grid title'
        data = [
            {
                'name': 'Sussex search',
                'date': '2011-05-10',
            },
            {'name': 'iCode', 'date': '2011-06-30'},
        ]
        insert_rows(grid, data, None)

        structure_updated_at1 = grid.structure_updated_at

        grid.change_structure(NEW_GRID_STRUCTURE)
        self.assertEqual(grid.title, 'List of conference', 'Must have changed grid title')
        structure_updated_at2 = grid.structure_updated_at
        self.assertTrue(len(grid.access_data) == 2, 'Grid has two rows')
        self.assertTrue('participants' in grid.access_data[0], "Grid has field \"participants\"")
        self.assertEqual(
            grid.access_data[0]['participants'],
            {'raw': ''},
            "\"participants\" is an empty field and must be empty",
        )
        self.assertTrue(bool(grid.access_data[0]['date']), "\"date\" must not be empty")
        self.assertFalse('name' in grid.access_data[0], "\"name\" must not be empty")
        self.assertNotEqual(structure_updated_at1, structure_updated_at2, 'structure_updated_at must have been changed')
        grid.change_structure(GRID_WITHOUT_FIELDS)

    def test_insert_action_POST(self):
        self._create_gorilla_grids()

        grid = Grid.objects.get(id=self.grid.id)
        data_updated_at1 = grid.modified_at
        before_pageevent = PageEvent.objects.filter(event_type=EventTypes.edit, page=self.grid).count()
        conf_name = 'Spring of Yandex Input Output'

        # wait so that time passes
        time.sleep(1)

        self._add_row(self.grid.supertag, {'name': conf_name, 'date': '2012-06-30'}, after_id=self.hash1)

        after_pageevent = PageEvent.objects.filter(event_type=EventTypes.edit, page=self.grid).count()
        self.assertEqual(before_pageevent + 1, after_pageevent, 'Must have created 1 page event')
        PageEvent.objects.filter(event_type=EventTypes.edit, page=self.grid)[before_pageevent]
        grid = Grid.objects.get(id=self.grid.id)
        data_updated_at2 = grid.modified_at
        self.assertNotEqual(data_updated_at1, data_updated_at2, 'data_updated_at must have changed')
        new_hash = grid.access_data[1][HASH_KEY]
        self.assertEqual(len(grid.access_data), 3, 'Must be 3 data rows')
        self.assertEqual(grid.access_idx[self.hash1] + 1, grid.access_idx[new_hash], 'Must be next to hash1')
        field = grid.access_data[grid.access_idx[new_hash]]
        self.assertEqual(field['name']['raw'], conf_name, 'Name must be conf_name')

        self._add_row(
            self.grid.supertag,
            {'name': 'Anton Chaporgin Yandex Main Programmer', 'date': '2012-06-30 14:46:00'},
            expected_status_code=409,
        )

        grid = Grid.objects.get(id=self.grid.id)
        self.assertEqual(len(grid.access_data), 3, 'The data rows must not have been changed')

    def test_edit_action_POST(self):
        self._create_gorilla_grids()

        grid = Grid.objects.get(id=self.grid.id)
        data_updated_at1 = grid.modified_at

        time.sleep(1)
        before_pageevent = PageEvent.objects.filter(event_type=EventTypes.edit, page=self.grid).count()

        self._edit_row(
            self.grid.supertag,
            self.hash,
            {
                'name': 'lifestyle for you',
                'date': '2011-06-30',
            },
        )

        grid = Grid.objects.get(id=self.grid.id)
        data_updated_at2 = grid.modified_at
        self.assertNotEqual(data_updated_at1, data_updated_at2, 'Must have changed data_updated_at')
        data = grid.access_data[grid.access_idx[self.hash]]
        self.assertEqual(data['name']['raw'], 'lifestyle for you', "Code must change \"name\" field")
        self.assertEqual(data['date']['raw'], '2011-06-30', "Code must change \"date\" field")
        after_pageevent = PageEvent.objects.filter(event_type=EventTypes.edit, page=self.grid).count()
        self.assertEqual(before_pageevent + 1, after_pageevent, 'Must have created 1 page event')
        PageEvent.objects.filter(event_type=EventTypes.edit, page=self.grid)[before_pageevent]

        self._edit_row(
            self.grid.supertag,
            self.hash,
            {
                'name': 'Chapson is a bad guy',
                'date': '2011/07/06',
            },
            expected_status_code=409,
        )

        grid = Grid.objects.get(id=self.grid.id)
        data = grid.access_data[grid.access_idx[self.hash]]
        self.assertNotEqual(data['date'], '2011-07-06', 'Date must not have been changed')

        self._edit_row(
            self.grid.supertag,
            self.hash,
            {
                'date': '2011-06-30',
            },
        )

        self._edit_row(
            self.grid.supertag,
            self.hash,
            {
                'is_done': '',
            },
        )

    def test_remove_action(self):
        self._create_gorilla_grids()

        grid = Grid.objects.get(id=self.grid.id)
        data_updated_at1 = grid.modified_at
        access_data = self.grid.access_data
        time.sleep(1)
        before_pageevent = PageEvent.objects.filter(event_type=EventTypes.edit, page=self.grid).count()
        self._remove_row(self.grid.supertag, self.hash)
        self._remove_row(self.grid.supertag, self.hash1)
        after_pageevent = PageEvent.objects.filter(event_type=EventTypes.edit, page=self.grid).count()
        self.assertEqual(before_pageevent + 1, after_pageevent, 'Must have created 1 page event')
        PageEvent.objects.filter(event_type=EventTypes.edit, page=self.grid)[before_pageevent]
        grid = Grid.objects.get(id=self.grid.id)
        data_updated_at2 = grid.modified_at
        self.assertNotEqual(data_updated_at1, data_updated_at2, 'Must have changed data_updated_at')
        self.assertTrue(len(grid.access_data) == 0, 'Must be no data')
        #        self.failUnless(len(grid.access_idx.keys()) == 0, "Must be no idxes")
        grid.access_data = access_data
        grid.save()

    def test_bad_structure(self):
        self._create_gorilla_grids()

        grid = Grid.objects.get(id=self.grid.id)
        grid.change_structure(BAD_STRUCTURE)
        self.assertTrue('<script />' in grid.body)  # так разрешено, потому что title эскейпится при выводе
        self.assertTrue('<s></a>' not in grid.body)  # а вот так не разрешено, потому что эти поля показываются как есть


BAD_STRUCTURE = """
{
  "title" : "<script />",
  "done" : true,
  "fields" : [{
    "name" : "<b></b>",
    "title" : "<s></a>",
    "type" : "string<a>"
  }]
}
"""
