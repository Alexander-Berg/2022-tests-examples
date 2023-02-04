
from ujson import loads

from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import BaseGridsTest

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


class RemoveRowTest(BaseGridsTest):
    def _prepare_grid(self, supertag):
        self._create_grid(supertag, GRID_STRUCTURE, self.user_thasonic)
        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        version = loads(response.content)['data']['version']
        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[dict(added_row=dict(after_id='-1', data=dict(name='row 1', date='2013-08-12', is_done='')))],
            ),
        )

        version = loads(response.content)['data']['version']
        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[dict(added_row=dict(after_id='-1', data=dict(name='row 2', date='2012-10-10', is_done='')))],
            ),
        )

        version = loads(response.content)['data']['version']
        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[dict(added_row=dict(after_id='-1', data=dict(name='row 3', date='2011-11-11', is_done='')))],
            ),
        )

        return loads(response.content)['data']['version']

    def test_remove_row(self):
        supertag = 'grid'
        self._prepare_grid(supertag)

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                changes=[dict(removed_row=dict(id='2'))],
            ),
        )

        self.assertEqual(200, response.status_code)
        response = self.client.get('/_api/frontend/{0}/.grid'.format(supertag))
        content = loads(response.content)
        rows = content['data']['rows']
        self.assertEqual(2, len(rows))
        self.assertEqual('row 3', rows[0][0]['raw'])
        self.assertEqual('row 1', rows[1][0]['raw'])

    def test_page_event_saved(self):
        from wiki.notifications.models import PageEvent

        supertag = 'grid'
        self._prepare_grid(supertag)

        self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                changes=[dict(removed_row=dict(id='2'))],
            ),
        )
        last_pe = list(PageEvent.objects.filter(page__supertag=supertag).order_by('created_at'))[-1]
        self.assertEqual(last_pe.event_type, PageEvent.EVENT_TYPES.edit)
        self.assertEqual(last_pe.meta['diff'], {'deleted': {'2': {}}, 'added': {'1': {}, '3': {}, '2': {}}})

    def test_remove_non_existent_row(self):
        supertag = 'grid'
        self._prepare_grid(supertag)
        # нет такой строки
        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                changes=[dict(removed_row=dict(id='1231'))],
            ),
        )

        self.assertEqual(200, response.status_code)
