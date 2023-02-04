
from django.conf import settings
from ujson import loads

from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import BaseGridsTest


class SpecialHandleWithFullDocumentTest(BaseGridsTest):
    """
    Тесты специальной ручки для фронтэнда, которая возвращает после изменения
    табличного списка весь табличный список.

    """

    def _prepare_grid_for_adding_column(self, supertag):
        GRID_STRUCTURE = """
{
  "title" : "List of conferences",
  "width" : "100%",
  "sorting" : [],
  "fields" : [
    {
      "name" : "name",
      "title" : "Name of conference"
    }
  ]
}
"""
        self._create_grid(supertag, GRID_STRUCTURE, self.user_thasonic)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))

        return loads(response.content)['data']['version']

    def test_add_string_column(self):
        supertag = 'grid'
        version = self._prepare_grid_for_adding_column(supertag)

        assert_queries = 22 if not settings.WIKI_CODE == 'wiki' else 20
        with self.assertNumQueries(assert_queries):
            response = self.client.post(
                '/_api/frontend/{0}/.grid/change_and_get_document'.format(supertag),
                dict(
                    version=str(version),
                    changes=[
                        dict(
                            added_column=dict(
                                type='string',
                                title='Любимый герой сериала',
                                width=100,
                                width_units='percent',
                                required=False,
                            )
                        )
                    ],
                ),
            )

        self.assertEqual(200, response.status_code)
        content = loads(response.content)

        self.assertEqual(content['data']['success'], True)
        self.assertTrue('grid' in content['data'])
        self.assertTrue('rows' in content['data']['grid'])
        self.assertTrue('structure' in content['data']['grid'])
        self.assertTrue('version' in content['data']['grid'])

        columns = content['data']['grid']['structure']['fields']

        self.assertEqual(2, len(columns))

        self.assertEqual('string', columns[1]['type'])
        self.assertEqual('Любимый герой сериала', columns[1]['title'])
        self.assertEqual('100', columns[1]['width'])
        self.assertEqual('%', columns[1]['width_units'])
        self.assertEqual(False, columns[1]['required'])

    def _prepare_grid_for_removal(self, supertag):
        GRID_STRUCTURE = """
{
  "title" : "List of conferences",
  "width" : "100%",
  "sorting" : [],
  "fields" : [
    {
      "name" : "name",
      "title" : "Name of conference"
    },
    {
      "name" : "date",
      "title" : "Date of conference"
    },
    {
      "name" : "is_done",
      "title" : "Is done?"
    }
  ]
}
"""
        self._create_grid(supertag, GRID_STRUCTURE, self.user_thasonic)

        self._add_row(supertag, dict(name='some name', date='2013-08-12', is_done=''))

        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        return loads(response.content)['data']['version']

    def test_remove_column(self):
        supertag = 'grid'
        version = self._prepare_grid_for_removal(supertag)

        assert_queries = 21 if not settings.WIKI_CODE == 'wiki' else 19
        with self.assertNumQueries(assert_queries):
            response = self.client.post(
                '/_api/frontend/{0}/.grid/change_and_get_document'.format(supertag),
                dict(
                    version=str(version),
                    changes=[dict(removed_column=dict(name='date'))],
                ),
            )
        self.assertEqual(200, response.status_code)
        content = loads(response.content)

        self.assertTrue('grid' in content['data'])
        self.assertTrue('rows' in content['data']['grid'])
        self.assertTrue('structure' in content['data']['grid'])
        self.assertTrue('version' in content['data']['grid'])

        columns = content['data']['grid']['structure']['fields']
        self.assertEqual(2, len(columns))
        self.assertEqual('name', columns[0]['name'])
        self.assertEqual('is_done', columns[1]['name'])
