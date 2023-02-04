
from ujson import loads

from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.change_grid.base import ChangeGridBaseTest

ORIG_TITLE = 'Лист of conferences'


class ChangeGridTitle(ChangeGridBaseTest):
    GRID_STRUCTURE = (
        """
    {
      "title" : "%s",
      "width" : "100%%",
      "sorting" : [],
      "fields" : [
        {
          "name" : "name",
          "title" : "Name of series"
        }
      ]
    }
    """
        % ORIG_TITLE
    )

    def test_valid(self):
        new_title = 'ну тайтл'
        response = self.client.post(
            '/_api/frontend/{0}/.grid/change'.format(self.supertag),
            dict(
                version=str(self.get_current_version()),
                changes=[
                    dict(
                        title_changed=dict(
                            title=new_title,
                        )
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)

        response = self.client.get('/_api/frontend/{0}/.grid'.format(self.supertag))
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual(new_title, data['structure']['title'])

        response = self.client.get('/_api/frontend/{0}'.format(self.supertag))
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual(new_title, data['title'])

    def test_invalid(self):
        # используем метод предка для проверки невалидного запроса
        self._test_invalid({'changes': [{'title_changed': None}]})
        self._test_invalid({'changes': [{'title_changed': {}}]})
        self._test_invalid({'changes': [{'title_changed': {'title': ' '}}]})
        # следующая проверка стала валидной в rest_framework 3 (!)
        # title = CharField, но 10 - валидное значение теперь.
        # self._test_invalid({'changes': [{'title_changed': {'title': 10}}]})

        response = self.client.get('/_api/frontend/{0}'.format(self.supertag))
        self.assertEqual(200, response.status_code)
        data = loads(response.content)['data']
        self.assertEqual(ORIG_TITLE, data['title'])
