
from django.conf import settings
from django.test import override_settings
from ujson import loads

from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import BaseGridsTest

GRID_STRUCTURE = '''
{
  "title" : "List of conferences",
  "width" : "100%%",
  "sorting" : [],
  "fields" : [
    {
      "name" : "latest_film",
      "title" : "latest film",
      "type": "string",
      "required": true
    },
    {
      "name" : "number",
      "type": "number",
      "title" : "Number of films"
    },
    {
      "name" : "actor",
      "type": "staff",
      "title" : "actor"
    },
    {
      "name" : "friends",
      "type": "staff",
      "multiple": true,
      "title" : "friends"
    },
    {
      "name" : "year_of_birth",
      "type": "select",
      "multiple": false,
      "title" : "year of birth",
      "options": ["1969", "1950", "1985", "1986"]
    },
    {
      "name" : "years_of_films",
      "type": "select",
      "multiple": true,
      "title" : "years of films",
      "options": ["2001", "2002", "2003", "2004"]
    },
    {
      "name" : "last_aired_film_date",
      "type": "date",
      "title" : "last aired film date"
    },
    {
      "name" : "cool_actor",
      "type": "checkbox",
      "title" : "is a cool actor"
    }%s
  ]
}
''' % (
    ''',
    {
      "name" : "tracker_ticket",
      "type": "ticket",
      "title" : "Ticket in tracker"
    }'''
    if settings.IS_INTRANET or settings.IS_BUSINESS
    else ''
)


class EditRowTest(BaseGridsTest):
    def _prepare_grid(self, supertag, structure):
        self._create_grid(supertag, structure, self.user_thasonic)
        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        version = loads(response.content)['data']['version']
        row_data = dict(
            latest_film='Boston Legal',
            number=10,
            actor='dannycrane',
            friends='thasonic, Arkadiy',
            year_of_birth=['1950'],
            years_of_films=['2004'],
            last_aired_film_date='2012-09-25',
            cool_actor=True,
        )
        if settings.IS_INTRANET or settings.IS_BUSINESS:
            row_data['tracker_ticket'] = 'WIKI-5555'
        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(version=str(version), changes=[dict(added_row=dict(after_id='-1', data=row_data))]),
        )

        return loads(response.content)['data']['version']

    def test_edit_row(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag, GRID_STRUCTURE)

        row_data = dict(
            latest_film='We\'re the Millers',
            number=4,
            actor='jenifferaniston',
            friends='jasonsoudeykis',
            year_of_birth=['1969'],
            years_of_films=['2004'],
            last_aired_film_date='2013-08-03',
            cool_actor=False,
        )
        if settings.IS_INTRANET or settings.IS_BUSINESS:
            row_data['tracker_ticket'] = 'WIKI-5555'

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(version=str(version), changes=[dict(edited_row=dict(id='1', data=row_data))]),
        )

        import logging

        sql_logger = logging.getLogger('django.db.backends')
        sql_logger.setLevel(logging.INFO)

        self.assertEqual(200, response.status_code)
        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        content = loads(response.content)
        row = content['data']['rows'][0]
        self.assertEqual('We\'re the Millers', row[0]['raw'])
        self.assertEqual('4', row[1]['raw'])
        self.assertEqual(['jenifferaniston'], row[2]['raw'])
        self.assertEqual(['jasonsoudeykis'], row[3]['raw'])
        self.assertEqual(['1969'], row[4]['raw'])
        self.assertEqual(['2004'], row[5]['raw'])
        self.assertEqual('2013-08-03', row[6]['raw'])
        self.assertEqual(False, row[7]['raw'])
        if settings.IS_INTRANET or settings.IS_BUSINESS:
            self.assertEqual('WIKI-5555', row[8]['raw'])

    def test_save_nonexistent_option(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag, GRID_STRUCTURE)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_row=dict(
                            id='1',
                            data=dict(
                                year_of_birth='3000',
                            ),
                        )
                    )
                ],
            ),
        )

        self.assertEqual(409, response.status_code)
        data = loads(response.content)
        self.assertEqual(data['error']['error_code'], 'NO_SUCH_OPTION')

    def test_edit_nonexistent(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag, GRID_STRUCTURE)

        row_data = dict(
            latest_film='We\'re the Millers',
            number=4,
            actor='jenifferaniston',
            friends='jasonsoudeykis',
            year_of_birth=['1969'],
            years_of_films=['2004'],
            last_aired_film_date='2013-08-03',
            cool_actor=True,
        )
        if settings.IS_INTRANET or settings.IS_BUSINESS:
            row_data['tracker_ticket'] = 'WIKI-5555'
        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(version=str(version), changes=[dict(edited_row=dict(id='11231', data=row_data))]),
        )

        self.assertEqual(409, response.status_code)
        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        content = loads(response.content)
        rows = content['data']['rows']
        self.assertEqual(1, len(rows))
        # строка не изменилась
        self.assertEqual('Boston Legal', rows[0][0]['raw'])

    def test_edit_invalid_request(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag, GRID_STRUCTURE)

        row_data = dict(
            latest_film='We\'re the Millers',
            number=0,
            actor='jenifferaniston',
            friends='jasonsoudeykis',
            year_of_birth=['1961'],
            years_of_films=['2004'],
            # неверный формат для даты
            last_aired_film_date='201111-08-03',
            cool_actor=True,
        )
        if settings.IS_INTRANET or settings.IS_BUSINESS:
            row_data['tracker_ticket'] = 'WIKI-5555'
        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(version=str(version), changes=[dict(edited_row=dict(id='1', data=row_data))]),
        )

        self.assertEqual(409, response.status_code)
        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        content = loads(response.content)
        rows = content['data']['rows']
        self.assertEqual(1, len(rows))
        # строка не изменилась
        self.assertEqual('Boston Legal', rows[0][0]['raw'])

    def test_conflict(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag, GRID_STRUCTURE)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_row=dict(
                            id='1',
                            data=dict(
                                actor='aaa',
                            ),
                        )
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),  # не последняя версия
                changes=[
                    dict(
                        edited_row=dict(
                            id='1',
                            data=dict(
                                actor='bbb',
                            ),
                        )
                    )
                ],
            ),
        )
        self.assertEqual(409, response.status_code)
        data = loads(response.content)
        self.assertEqual(data['error']['error_code'], 'ROW_HAS_CHANGED')

        self.client.login('chapson')
        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),  # не последняя версия
                changes=[
                    dict(
                        edited_row=dict(
                            id='1',
                            data=dict(
                                friends='ccc',
                            ),
                        )
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),  # не последняя версия
                changes=[
                    dict(
                        edited_row=dict(
                            id='1',
                            data=dict(
                                actor='ddd',
                            ),
                        )
                    )
                ],
            ),
        )
        self.assertEqual(409, response.status_code)
        data = loads(response.content)
        self.assertEqual(data['error']['error_code'], 'ROW_HAS_CHANGED')

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),  # не последняя версия
                changes=[
                    dict(
                        edited_row=dict(
                            id='1',
                            data=dict(
                                cool_actor=False,
                                actor='ddd',
                            ),
                        )
                    )
                ],
            ),
        )
        self.assertEqual(409, response.status_code)
        data = loads(response.content)
        self.assertEqual(data['error']['error_code'], 'ROW_HAS_CHANGED')

    @override_settings(LIMIT__WIKI_TEXT_FOR_GRID_CELL__BYTES=50)
    def test_max_grid_cell_size(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag, GRID_STRUCTURE)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_row=dict(
                            id='1',
                            data=dict(
                                latest_film='Килла' * 10,
                            ),
                        )
                    )
                ],
            ),
        )
        self.assertEqual(409, response.status_code)
        self.assertEqual('Maximum grid cell size 0 Kb exceeded', response.data['error']['message'][0])

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_row=dict(
                            id='1',
                            data=dict(
                                latest_film='Killa' * 10,
                            ),
                        )
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)


class EditSingleCellTest(BaseGridsTest):
    def _prepare_grid(self, supertag, structure):
        self._create_grid(supertag, structure, self.user_thasonic)
        response = self.client.get(self.api_url + '/' + supertag)
        self.assertEqual(response.status_code, 200)

        version = loads(response.content)['data']['version']
        response = self.client.post(
            self.api_url + '/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='-1',
                            data=dict(
                                latest_film='Boston Legal',
                                number=10,
                            ),
                        )
                    )
                ],
            ),
        )
        self.assertEqual(response.status_code, 200)

        return loads(response.content)['data']['version']

    def test_edit_row(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag, GRID_STRUCTURE)

        response = self.client.post(
            self.api_url + '/{0}/.grid/change_get_cell'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_row=dict(
                            id='1',
                            data=dict(
                                latest_film='We\'re the Millers',
                            ),
                        )
                    )
                ],
            ),
        )

        self.assertEqual(200, response.status_code)
        result = loads(response.content)['data']
        self.assertTrue('raw' in result['cell'])
        self.assertEqual(result['cell']['raw'], 'We\'re the Millers')

    def test_save_empty_option(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag, GRID_STRUCTURE)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change_get_cell'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_row=dict(
                            id='1',
                            data=dict(
                                year_of_birth='',
                            ),
                        )
                    )
                ],
            ),
        )

        self.assertEqual(200, response.status_code)
        result = loads(response.content)['data']
        self.assertTrue('raw' in result['cell'])
        self.assertEqual(result['cell']['raw'], [])
