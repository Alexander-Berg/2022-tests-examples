import ujson as json
from django.conf import settings

from wiki.grids.models import Grid
from wiki.grids.utils import grid_as_table, insert_rows
from wiki.grids.utils.dashes import remove_dash
from wiki.pages.models import Revision
from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import BaseGridsTest
from intranet.wiki.tests.wiki_tests.unit_unittest.grids.base import GRID_TICKET_STRUCTURE, TICKET_GRID


class TicketRowTest(BaseGridsTest):
    def setUp(self):
        super(TicketRowTest, self).setUp()
        self._create_gorilla_grids()

    def test_ticket_grid_wiki_3400(self):
        self.grid.change_structure(GRID_TICKET_STRUCTURE)
        key = self.grid.access_data[0]['__key__']
        self.grid.remove([key])
        self.grid.save()
        Revision.objects.create_from_page(self.grid)

        before_len = len(self.grid.access_data)

        self._add_row(self.grid.supertag, {'ticket': 'badticka', 'todo': True}, expected_status_code=409)

        self._add_row(self.grid.supertag, {'ticket': 'WIKI-1', 'todo': 'do the wiki'})

        renewed = Grid.objects.get(supertag=self.grid.supertag)
        key = renewed.access_data[0]['__key__']

        self.assertEqual(before_len + 1, len(renewed.access_data))
        renewed_row = renewed.access_data[renewed.access_idx[key]]

        self.assertEqual(2, len(renewed_row['ticket']))
        self.assertEqual('WIKI-1', renewed_row['ticket']['raw'])
        self.assertTrue(settings.STARTREK_CONFIG['INTERFACE_URL'] + '/' + 'WIKI-1' in renewed_row['ticket']['view'])

        # submit the ticket
        self._edit_row(self.grid.supertag, key, {'ticket': 'WIKI-2'})

        renewed = Grid.objects.get(supertag=self.grid.supertag)

        self.assertTrue(all(tmp in renewed.access_data[0]['ticket'] for tmp in ('raw', 'view')))

        # some random text
        self._edit_row(self.grid.supertag, key, {'todo': 'Jump for joy!'})

        renewed = Grid.objects.get(supertag=self.grid.supertag)

        self.assertTrue(all(tmp in renewed.access_data[0] for tmp in ('todo', 'ticket')))
        self.assertEqual('WIKI-2', renewed.access_data[0]['ticket']['raw'])
        self.assertEqual('Jump for joy!', renewed.access_data[0]['todo']['raw'])

    def test_ticket_type_fields(self):
        self.grid.change_structure(TICKET_GRID)
        self.grid.save()
        Revision.objects.create_from_page(self.grid)

        self._add_row(self.grid.supertag, {'ticket': 'STARTREK-100'})

        result = self._get_grid(self.grid.supertag)
        result = json.loads(result.content)['data']['rows'][0]

        def _get_cell_by_key(row, key):
            for cell in row:
                if cell['__key__'] == key:
                    return cell
            raise ValueError('No cell with key=%s in %s' % (key, row))

        # стартрек-тикет с максимумом значений
        assignee = _get_cell_by_key(result, 'assignee')
        self.assertEqual(['chapson'], assignee['raw'])
        self.assertEqual('Chaporgin', assignee['transformed'][0]['last_name'])
        reporter = _get_cell_by_key(result, 'reporter')
        self.assertEqual(['thasonic'], reporter['raw'])
        self.assertEqual('Pokatilov', reporter['transformed'][0]['last_name'])
        self.assertEqual('Task', _get_cell_by_key(result, 'type')['raw'])
        self.assertEqual('Closed', _get_cell_by_key(result, 'status')['raw'])
        self.assertEqual('killa', _get_cell_by_key(result, 'subject')['raw'])
        self.assertTrue('Critical' in _get_cell_by_key(result, 'priority')['raw'])
        ticket = _get_cell_by_key(result, 'ticket')
        self.assertEqual('STARTREK-100', ticket['raw'])
        created = _get_cell_by_key(result, 'createdat')
        self.assertEqual('2013-06-12', created['raw'])
        self.assertEqual('2013-06-12', created['view'])
        updated = _get_cell_by_key(result, 'updatedat')
        self.assertEqual('2015-04-06', updated['raw'])
        self.assertEqual('2015-04-06', updated['view'])
        original_estimation = _get_cell_by_key(result, 'originalestimation')
        self.assertEqual('3w 4d', original_estimation['raw'])
        self.assertEqual('3w 4d', original_estimation['view'])
        fix_versions = _get_cell_by_key(result, 'fixversions')
        self.assertEqual('External wiki formatter,<br>Поддержка', fix_versions['raw'])
        self.assertEqual('External wiki formatter,<br>Поддержка', fix_versions['view'])
        components = _get_cell_by_key(result, 'components')
        self.assertEqual('% Formatter,<br>% Фронтенд и Вёрстка', components['raw'])
        self.assertEqual('% Formatter,<br>% Фронтенд и Вёрстка', components['view'])
        story_points = _get_cell_by_key(result, 'storypoints')
        self.assertEqual('3.5', story_points['raw'])
        self.assertEqual('3.5', story_points['view'])
        self.assertEqual(13, len(result))

        # стартрек-тикет с минимумом значений
        self._add_row(self.grid.supertag, {'ticket': 'STARTREK-101'})

        result = self._get_grid(self.grid.supertag)
        result = json.loads(result.content)['data']['rows'][0]

        self.assertEqual([], _get_cell_by_key(result, 'assignee')['raw'])
        reporter = _get_cell_by_key(result, 'reporter')
        self.assertEqual(['thasonic'], reporter['raw'])
        self.assertEqual('Pokatilov', reporter['transformed'][0]['last_name'])
        self.assertEqual('Task', _get_cell_by_key(result, 'type')['raw'])
        self.assertEqual('Closed', _get_cell_by_key(result, 'status')['raw'])
        self.assertEqual('gorilla', _get_cell_by_key(result, 'subject')['raw'])
        self.assertTrue('Critical' in _get_cell_by_key(result, 'priority')['raw'])
        ticket = _get_cell_by_key(result, 'ticket')
        self.assertEqual('STARTREK-101', ticket['raw'])
        created = _get_cell_by_key(result, 'createdat')
        self.assertEqual('2013-06-12', created['raw'])
        self.assertEqual('2013-06-12', created['view'])
        updated = _get_cell_by_key(result, 'updatedat')
        self.assertEqual('2015-04-06', updated['raw'])
        self.assertEqual('2015-04-06', updated['view'])
        original_estimation = _get_cell_by_key(result, 'originalestimation')
        self.assertEqual('', original_estimation['raw'])
        self.assertEqual('', original_estimation['view'])
        fix_versions = _get_cell_by_key(result, 'fixversions')
        self.assertEqual([], fix_versions['raw'])
        self.assertEqual([], fix_versions['view'])
        components = _get_cell_by_key(result, 'components')
        self.assertEqual([], components['raw'])
        self.assertEqual([], components['view'])
        story_points = _get_cell_by_key(result, 'storypoints')
        self.assertEqual('', story_points['raw'])
        self.assertEqual('', story_points['view'])
        self.assertEqual(13, len(result))

    def test_nonexisting_ticket(self):
        self.grid.change_structure(TICKET_GRID)
        self.grid.save()
        Revision.objects.create_from_page(self.grid)

        # кладем несуществующий тикет в грид
        self._add_row(self.grid.supertag, {'ticket': 'WIKI-10000'})

    def test_remove_nonbreaking_dash(self):
        # Проверяем, что удаляется "&#8209;" в функции wiki.grids.utils.dashes.remove_dash
        grid = self.grid
        grid.change_structure(None, already_parsed={'fields': [{'name': '0', 'type': 'ticket'}]})
        grid.access_data = [
            {
                '0': {
                    'raw': 'WIKI-100',
                    'view': "<span class=\"jiraissue\"><a href=\"/WIKI-100\">WIKI&#8209;100</a></span>",
                },
                '__key__': 1,
            }
        ]
        grid.save()
        grid = Grid.objects.get(supertag=self.grid.supertag)
        remove_dash(grid)
        self.assertTrue('&#8209;' not in grid.access_data[0]['0']['view'])

    def test_empty_ticket(self):
        # Не должно быть ошибки при отображении табличного списка, у которого указано пустое поле "ticket"
        #
        # For example:
        # grid: ticket, ticket-assignee
        # row: empty ticket field
        # displaying read-only grid will result in an error, because there will be no ticket-assignee cell in that row
        self.grid.change_structure(TICKET_GRID)
        self.grid.save()

        self.grid.access_data = []
        self.grid.access_idx = {}
        insert_rows(
            self.grid,
            [
                {
                    'ticket': '',
                }
            ],
            None,
        )
        self.grid.save()

        grid = Grid.objects.get(id=self.grid.id)

        grid_as_table(grid.access_structure, grid.get_rows(None), do_sort=True)
