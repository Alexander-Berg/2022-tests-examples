
from ujson import loads

from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import BaseGridsTest


class ChangeGridBaseTest(BaseGridsTest):
    GRID_STRUCTURE = """
    {
      "title" : "Лист",
      "width" : "100%",
      "sorting" : [],
      "fields" : [
        {
          "name" : "name",
          "title" : "Name of series"
        }
      ]
    }
    """

    def setUp(self):
        super(ChangeGridBaseTest, self).setUp()
        self.supertag = 'gri'
        self._create_grid(self.supertag, self.GRID_STRUCTURE, self.user_thasonic)

    def get_current_version(self):
        response = self.client.get('/_api/frontend/{0}/.grid'.format(self.supertag))
        return loads(response.content)['data']['version']

    def _test_invalid(self, changes):
        data = dict(version=str(self.get_current_version()))
        if changes:
            data.update(changes)
        response = self.client.post('/_api/frontend/{0}/.grid/change'.format(self.supertag), data)
        self.assertEqual(409, response.status_code)


class ChangeGridTest(ChangeGridBaseTest):
    def test_invalid(self):
        self._test_invalid(None)
        self._test_invalid({})
        self._test_invalid({'changes': 10})
        self._test_invalid({'changes': None})
        self._test_invalid({'changes': []})
        self._test_invalid({'changes': [None]})
        self._test_invalid({'changes': [10]})
        self._test_invalid({'changes': [{10: 20}]})
