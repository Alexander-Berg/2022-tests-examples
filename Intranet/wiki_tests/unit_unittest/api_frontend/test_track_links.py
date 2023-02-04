
from ujson import loads

from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import create_grid
from wiki.pages.models import PageLink
from intranet.wiki.tests.wiki_tests.common.unittest_base import BaseApiTestCase
from intranet.wiki.tests.wiki_tests.common.utils import celery_eager

GRID_STRUCTURE = """
{
  "title" : "List",
  "width" : "100%",
  "sorting" : [],
  "fields" : [
    {
      "name": "name",
      "title": "Name",
      "type": "string",
      "required": true
    }
  ]
}
"""


class TrackLinksTest(BaseApiTestCase):
    def setUp(self):
        super(TrackLinksTest, self).setUp()
        self.setUsers()
        self.client.login('thasonic')
        self.page = self.create_page(
            tag='ПростоСтраница',
            body='page test',
        )

    @celery_eager
    def test_edit_page(self):
        supertag = 'article'

        page_data = {'title': 'ЙаЗаголовок', 'body': '((/ПростоСтраница))'}

        self.assertEqual(PageLink.objects.count(), 0)

        request_url = '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=supertag)
        self.client.post(request_url, data=page_data)

        self.assertEqual(PageLink.objects.count(), 1)
        page_link = PageLink.objects.get()
        self.assertEqual(page_link.from_page.supertag, supertag)
        self.assertEqual(page_link.to_page.supertag, self.page.supertag)

        page_data = {'title': 'ЙаЗаголовок', 'body': 'Нет больше ссылки'}

        request_url = '{api_url}/{page_supertag}'.format(api_url=self.api_url, page_supertag=supertag)

        self.client.post(request_url, data=page_data)
        self.assertEqual(PageLink.objects.count(), 0)

    def _prepare_grid(self, supertag, structure):
        create_grid(self, supertag, structure, self.user_thasonic)
        response = self.client.get('/_api/frontend/{0}'.format(supertag))

        version = loads(response.content)['data']['version']

        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='-1',
                            data=dict(
                                name='wat',
                            ),
                        )
                    )
                ],
            ),
        )
        return loads(response.content)['data']['version']

    @celery_eager
    def test_edit_grid_row(self):
        from wiki.grids.models import Grid

        supertag = 'grid'
        version = self._prepare_grid(supertag, GRID_STRUCTURE)

        self.assertEqual(PageLink.objects.count(), 0)

        self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_row=dict(
                            id='1',
                            data=dict(
                                name='((/ПростоСтраница))',
                            ),
                        )
                    )
                ],
            ),
        )

        self.assertEqual(PageLink.objects.count(), 1)
        page_link = PageLink.objects.get()
        self.assertEqual(page_link.from_page.supertag, supertag)
        self.assertEqual(page_link.to_page.supertag, self.page.supertag)

        grid = Grid.objects.get(supertag=supertag)

        self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(grid.get_page_version()),
                changes=[
                    dict(
                        edited_row=dict(
                            id='1',
                            data=dict(
                                name='Больше нет ссылки',
                            ),
                        )
                    )
                ],
            ),
        )

        self.assertEqual(PageLink.objects.count(), 0)

    @celery_eager
    def test_add_grid_row(self):
        supertag = 'grid'
        version = self._prepare_grid(supertag, GRID_STRUCTURE)

        self.assertEqual(PageLink.objects.count(), 0)

        self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='-1',
                            data=dict(
                                name='((/ПростоСтраница))',
                            ),
                        )
                    )
                ],
            ),
        )

        self.assertEqual(PageLink.objects.count(), 1)
        page_link = PageLink.objects.get()
        self.assertEqual(page_link.from_page.supertag, supertag)
        self.assertEqual(page_link.to_page.supertag, self.page.supertag)

        self.create_page(
            tag='ДругаяСтраница',
            body='page test',
        )

        self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(supertag),
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='1',
                            data=dict(
                                name='((/ДругаяСтраница))',
                            ),
                        )
                    )
                ],
            ),
        )

        self.assertEqual(PageLink.objects.count(), 2)
