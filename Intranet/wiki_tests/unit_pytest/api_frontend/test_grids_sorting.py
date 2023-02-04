import pytest

from ujson import dumps, loads

from intranet.wiki.tests.wiki_tests.common import grid_helper

pytestmark = [pytest.mark.django_db]

GRID_STRUCTURE = """
{
  "title" : "List of conferences",
  "width" : "100%%",
  "sorting" : %s,
  "fields" : [
    {
      "name" : "number",
      "type": "number",
      "title" : "Number of films"
    },
    {
      "name" : "actor",
      "type": "staff",
      "title" : "actor"
    }
  ]
}
"""


class TestSortGrid:
    supertag = 'grid'

    def _prepare_grid(self, client, api_url, sorting, user):
        grid_helper.create_grid(client, api_url, self.supertag, GRID_STRUCTURE % dumps(sorting), user)

        # получить табличный список вида
        # 1 | chapson
        # 3 |
        # 2 | thasonic
        # 4 |
        # 1 | dannydevito
        rows = [
            {'number': 1, 'actor': 'chapson'},
            {'number': 3, 'actor': ['']},
            {'number': 2, 'actor': 'thasonic'},
            {'number': 4, 'actor': ''},
            {'number': 1, 'actor': 'dannydevito'},
        ]
        for row in rows:
            grid_helper.add_row(client, api_url, self.supertag, row, 'last')

        response = client.get(f'/_api/frontend/{self.supertag}/.grid')
        assert response.status_code == 200
        return loads(response.content)['data']['rows']

    def test_sort_by_number(self, client, api_url, wiki_users):
        client.login('thasonic')
        self._prepare_grid(client, api_url, [{'name': 'number', 'type': 'asc'}], wiki_users.thasonic)
        response = client.get(f'/_api/frontend/{self.supertag}/.grid?sort_number=desc')
        result = loads(response.content)['data']['rows']
        assert result[0][0]['raw'] == '4'
        assert result[1][0]['raw'] == '3'
        assert result[2][0]['raw'] == '2'
        assert result[3][0]['raw'] == '1'
        assert result[4][0]['raw'] == '1'
        assert len(result) == 5

    def test_sort_by_actor(self, client, api_url, wiki_users):
        client.login('thasonic')
        self._prepare_grid(client, api_url, [{'name': 'number', 'type': 'asc'}], wiki_users.thasonic)
        response = client.get(f'/_api/frontend/{self.supertag}/.grid?sort_actor=desc')
        assert response.status_code == 200
        result = loads(response.content)['data']['rows']
        # Сортировка по значению 'i_first_name i_last_name' не по логинам
        assert result[0][1]['raw'] == ['chapson']
        assert result[1][1]['raw'] == ['thasonic']
        assert result[2][1]['raw'] == ['dannydevito']
        assert result[3][1]['raw'] == []
        assert result[4][1]['raw'] == []
        assert len(result) == 5
