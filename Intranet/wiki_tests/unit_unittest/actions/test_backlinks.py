from django.conf import settings
from ujson import loads

from wiki import access as wiki_access
from wiki.grids.utils import dummy_request_for_grids, insert_rows
from wiki.pages.logic.backlinks import track_links
from wiki.pages.models.page import Page
from wiki.pages.models.page_link import PageLink
from wiki.pages.models.absent_page import AbsentPage
from intranet.wiki.tests.wiki_tests.unit_unittest.actions.base import OldHttpActionTestCase

GRID_STRUCTURE = """
{
  "title" : "Grid with links",
  "width" : "100%",
  "sorting" : [],
  "fields" : [
    {
      "name" : "src",
      "title" : "Source",
      "type": "string",
      "required": true
    },
    {
      "name" : "dst",
      "title" : "Destination",
      "type": "string"
    }
  ]
}
"""


class BacklinksTest(OldHttpActionTestCase):

    """
    Linkstree action test.
    """

    action_name = 'backlinks'

    def test_simple_usage(self):
        p0 = self.create_page(tag='testpage0', body='')
        p1 = self.create_page(tag='testpage1', body='((testpage0 link0))\n((testpage3 link1))')
        p2 = self.create_page(tag='testpage2', body='((!/testpage0 link0))\n((!/testpage3 link1))')
        p3 = self.create_page(tag='testpage2/testpage3', body='((!/testpage0 link0))')

        [track_links(page, True) for page in [p0, p1, p2, p3]]

        self.assertEqual(PageLink.objects.count(), 2)
        self.assertEqual(AbsentPage.objects.count(), 3)
        self.assertTrue(AbsentPage.objects.filter(from_page_id=p1.id, to_supertag='testpage3').exists())
        self.assertTrue(AbsentPage.objects.filter(from_page_id=p2.id, to_supertag='testpage2/testpage0').exists())
        self.assertTrue(
            AbsentPage.objects.filter(from_page_id=p3.id, to_supertag='testpage2/testpage3/testpage0').exists()
        )

        response = self.get(params={'for': 'testpage0'})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response['content-type'], 'text/html')
        self.assertEqual(response.content.count(b'href="/testpage1#testpage0"'), 1)

        response = self.get(params={'for': 'testpage2/testpage3'})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response['content-type'], 'text/html')
        self.assertEqual(response.content.count(b'href="/testpage2#testpage2/testpage3"'), 1)

    def test_with_grid(self):
        wiki_host = list(settings.FRONTEND_HOSTS)[0]

        p0 = self.create_page(tag='testpage0', body='((testgrid link0))')
        p1 = self.create_page(tag='testpage1', body='((testpage0 link0))testgrid')

        grid = self.create_page(tag='testgrid', page_type=Page.TYPES.GRID)
        grid.change_structure(GRID_STRUCTURE)
        grid.save()

        insert_rows(
            grid,
            [
                {'src': '((testpage1))', 'dst': f'https://{wiki_host}/testpage0'},
                {'src': '((testgrid))', 'dst': f'https://{wiki_host}/testpage1'},
            ],
            dummy_request_for_grids(),
        )

        [track_links(page, True) for page in [p0, p1, grid]]
        self.assertEqual(PageLink.objects.count(), 5)

        response = self.get(params={'for': 'testpage0'})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response['content-type'], 'text/html')
        self.assertEqual(response.content.count(b'href="/testpage1#testpage0"'), 1)
        self.assertEqual(response.content.count(b'href="/testgrid#testpage0"'), 1)

        response = self.get(params={'for': 'testgrid'})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response['content-type'], 'text/html')
        self.assertEqual(response.content.count(b'href="/testpage0#testgrid"'), 1)
        self.assertEqual(response.content.count(b'href="/testgrid#testgrid"'), 1)

    def test_nomark(self):
        self.create_page(tag='testpage2', body='((testpage0 link0))\n((testpage1 link1))')
        response = self.get(params={'root': 'testpage2', 'levels': '3'})

        self.assertTrue(b'<fieldset' in response.content)

        response = self.get(params={'for': 'testpage2', 'nomark': ''})

        self.assertTrue(b'<fieldset' not in response.content)

    def test_access(self):
        p0 = self.create_page(tag='testpage0', body='')
        p1 = self.create_page(tag='testpage1', body='((testpage0 link0))')
        p2 = self.create_page(tag='testpage2', body='((testpage0 link0))\n((testpage1 link1))')

        [track_links(page, True) for page in [p0, p1, p2]]

        p1.last_author = self.user_chapson
        p1.save()
        p1.authors.clear()
        p1.authors.add(self.user_chapson)

        wiki_access.set_access(p1, wiki_access.TYPES.OWNER, self.user_chapson)

        response = self.get(params={'for': 'testpage0'})

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.content.count(b'href="/testpage2#testpage0"'), 1)
        self.assertEqual(response.content.count(b'href="/testpage1#testpage0"'), 0)

    def test_missing_page(self):
        """
        returns empty action
        """
        response = self.get(params={'for': 'someMissingPage'})
        self.assertEqual(response.status_code, 200)

    def test_no_for_param(self):
        """
        w/o explicit root param page from which action was requested will be used
        (in this case - default /Test)
        """
        p0 = self.create_page(tag='testpage1', body='((Test link0))')
        [track_links(page, True) for page in [p0]]

        response = self.get()

        self.assertEqual(response.status_code, 200)
        self.assertTrue(b'href="/testpage1#test"' in response.content)

    def test_render_json(self):
        p0 = self.create_page(tag='testpage0', body='')
        p1 = self.create_page(tag='testpage1', body='((testpage0 link0))')
        p2 = self.create_page(tag='testpage2', body='((testpage0 link0))\n((testpage1 link1))')

        [track_links(page, True) for page in [p0, p1, p2]]

        response = self.get(params={'for': 'testpage0', 'render_json': ''})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response['content-type'], 'application/json')
        json = loads(response.content)
        self.assertEqual(len(json), 2)
        self.assertEqual({json[0]['link'], json[1]['link']}, {'testpage2#testpage0', 'testpage1#testpage0'})

    def test_render_json_unknown_tag(self):
        self.create_page(tag='testpage0', body='')
        response = self.get(params={'for': 'unknown_page', 'render_json': ''})
        self.assertEqual(response.status_code, 404)

    def test_action_link(self):
        p0 = self.create_page(tag='testpage0', body='')
        p1 = self.create_page(tag='testpage1', body='{{include page="/testpage0"}}')
        [track_links(page, True) for page in [p0, p1]]

        self.assertEqual(PageLink.objects.count(), 1)

        response = self.get(params={'for': 'testpage0'})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response['content-type'], 'text/html')
        self.assertEqual(response.content.count(b'href="/testpage1#testpage0"'), 1)
