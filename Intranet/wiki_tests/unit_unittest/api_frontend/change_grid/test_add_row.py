
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
      "type": "string"
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
      "options": ["1931", "1950", "1985", "1986"]
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
      "name" : "date",
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

SIMPLE_GRID = """
{
  "title" : "List of conferences",
  "width" : "100%",
  "sorting" : [],
  "fields" : [
    {
      "name" : "latest_film",
      "title" : "latest film",
      "type": "string"
    }
  ]
}
"""


class AddRowTest(BaseGridsTest):
    def _prepare_grid(self, supertag, structure):
        self._create_grid(supertag, structure, self.user_thasonic)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        self.assertEqual(response.status_code, 200)

        return loads(response.content)['data']['version']

    def test_add_rows_to_the_beginning(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag, GRID_STRUCTURE)
        row_data = dict(
            latest_film='Boston Legal',
            number=10,
            actor='dannycrane',
            friends=['thasonic', 'Arkadiy'],
            year_of_birth=['1950'],
            years_of_films=['2004'],
            last_aired_film_date='2012-09-25',
            cool_actor=True,
        )
        if settings.IS_INTRANET or settings.IS_BUSINESS:
            row_data['tracker_ticket'] = 'WIKI-1'
        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(version=str(version), changes=[dict(added_row=dict(after_id='-1', data=row_data))]),
        )

        self.assertEqual(200, response.status_code)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))

        rows = loads(response.content)['data']['rows']
        self.assertEqual(1, len(rows))

    def test_add_rows_one_by_one(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag, SIMPLE_GRID)
        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='-1',
                            data=dict(
                                latest_film='Boston Legal',
                            ),
                        )
                    )
                ],
            ),
        )
        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='-1',
                            data=dict(
                                latest_film='Hudsonian Hide Park',
                            ),
                        )
                    )
                ],
            ),
        )

        self.assertEqual(200, response.status_code)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        rows = loads(response.content)['data']['rows']
        self.assertEqual(2, len(rows))
        self.assertEqual('Hudsonian Hide Park', rows[0][0]['raw'])
        self.assertEqual('Boston Legal', rows[1][0]['raw'])

    def test_add_to_the_end(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag, SIMPLE_GRID)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='-1',
                            data=dict(
                                latest_film='Boston Legal',
                            ),
                        )
                    )
                ],
            ),
        )
        new_version = loads(response.content)['data']['version']

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(new_version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='1',
                            data=dict(
                                latest_film='Hudsonian Hide Park',
                            ),
                        )
                    )
                ],
            ),
        )

        self.assertEqual(200, response.status_code)
        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        rows = loads(response.content)['data']['rows']
        self.assertEqual(2, len(rows))
        self.assertEqual('Boston Legal', rows[0][0]['raw'])
        self.assertEqual('Hudsonian Hide Park', rows[1][0]['raw'])

    def test_add_after_nonexistent(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag, SIMPLE_GRID)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='-1',
                            data=dict(
                                latest_film='Boston Legal',
                            ),
                        )
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)
        version = loads(response.content)['data']['version']

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        removed_row=dict(
                            id='1',
                        )
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='1',
                            before_id='-1',
                            data=dict(
                                latest_film='Hudsonian Hide Park',
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
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='1',
                            before_id='-1',
                            data=dict(
                                latest_film='Park Pobedy',
                            ),
                        )
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)
        version = loads(response.content)['data']['version']

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        removed_row=dict(
                            id='2',
                        )
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='2',
                            before_id='3',
                            data=dict(
                                latest_film='Gagarinsky Park',
                            ),
                        )
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        rows = loads(response.content)['data']['rows']
        self.assertEqual(2, len(rows))
        self.assertEqual('Gagarinsky Park', rows[0][0]['raw'])
        self.assertEqual('Park Pobedy', rows[1][0]['raw'])

    def test_save_nonexistent_column(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag, GRID_STRUCTURE)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='-1',
                            data=dict(
                                no_such_column='3000',
                            ),
                        )
                    )
                ],
            ),
        )

        self.assertEqual(409, response.status_code)
        data = loads(response.content)
        self.assertEqual(data['error']['error_code'], 'NO_SUCH_COLUMN')

    def test_save_nonexistent_option(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag, GRID_STRUCTURE)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='-1',
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

    @override_settings(LIMIT__GRID_ROWS_COUNT=1)
    def test_max_rows_count(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag, GRID_STRUCTURE)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='-1',
                            data=dict(
                                latest_film='Boston Legal',
                            ),
                        )
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)
        version = response.data['data']['version']

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='-1',
                            data=dict(
                                latest_film='Lalka International',
                            ),
                        )
                    )
                ],
            ),
        )
        self.assertEqual(409, response.status_code)
        self.assertEqual('Maximum grid rows count exceeded', response.data['error']['message'][0])

    @override_settings(LIMIT__WIKI_TEXT_FOR_GRID_CELL__BYTES=5)
    def test_max_grid_cell_size(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag, GRID_STRUCTURE)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='-1',
                            data=dict(
                                latest_film='Килла',
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
                        added_row=dict(
                            after_id='-1',
                            data=dict(
                                latest_film='Killa',
                            ),
                        )
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)


GRID_WITH_REQUIRED = """
{
  "title" : "List of conferences",
  "width" : "100%",
  "sorting" : [],
  "fields" : [
    {
      "name" : "name",
      "title" : "Person",
      "type": "string",
      "required": true
    },
    {
      "name" : "height",
      "type": "number",
      "title" : "Height"
    }
  ]
}
"""


class AddEmptyRowTest(BaseGridsTest):
    def _prepare_grid(self, supertag, structure):
        self._create_grid(supertag, structure, self.user_thasonic)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        return loads(response.content)['data']['version']

    def test_add_new_row(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag, GRID_WITH_REQUIRED)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(version=str(version), changes=[dict(added_row=dict(after_id='-1', data=dict()))]),
        )

        self.assertEqual(200, response.status_code)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        data = loads(response.content)['data']['rows']
        self.assertEqual(1, len(data))
