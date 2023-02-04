import pytest

from mock import patch
from waffle.testutils import override_switch

from wiki.api_core.waffle_switches import ELASTIC
from wiki.cloudsearch.cloudsearch_client import CLOUD_SEARCH_CLIENT
from wiki.cloudsearch.utils import find_by_search_uuid, render_document_for_indexation
from wiki.grids.models import Grid
from wiki.grids.utils import changes_of_structure
from wiki.utils.supertag import tag_to_supertag
from wiki.utils.wfaas.client import WfaasClient

pytestmark = [pytest.mark.django_db]

GRID_STRUCTURE = """
{
  "title" : "List of conferences",
  "width" : "100%%",
  "fields" : [
    {
      "name" : "number",
      "type": "number",
      "title" : "Number of films"
    },
    {
      "name" : "actor",
      "type": "string",
      "title" : "actor"
    }
  ]
}
"""


@patch.object(CLOUD_SEARCH_CLIENT, '_send_message')
def test_grid_calls_search(test_send_message, client, api_url, wiki_users, groups):
    with override_switch(ELASTIC, active=True):
        groups.group_org_42.user_set.add(wiki_users.thasonic)
        client.login(wiki_users.thasonic)
        tag = 'testtag'

        response = client.post(
            f'{api_url}/{tag}/.grid/create',
            dict(title='grid title'),
        )
        assert response.status_code == 200

        grid = Grid.active.get(supertag=tag_to_supertag(tag))
        previous_structure = grid.access_structure.copy()

        grid.change_structure(GRID_STRUCTURE)
        grid.save()
        changes_of_structure(wiki_users.thasonic, grid, previous_structure)

        response = client.post(
            '{api_url}/{supertag}/.grid/change'.format(api_url=api_url, supertag=tag),
            data={
                'version': grid.get_page_version(),
                'changes': [
                    {
                        'added_row': {
                            'after_id': 'last',
                            'data': {'number': 1, 'actor': 'chapson'},
                        }
                    }
                ],
            },
        )

        assert 200 == response.status_code

        assert 2 == test_send_message.call_count


@patch.object(WfaasClient, 'raw_to_html')
def test_grid_formatter(test_raw_to_html, client, api_url, wiki_users, groups):
    with override_switch(ELASTIC, active=True):
        tag = 'testtag'
        formatted_body = 'kek'
        test_raw_to_html.return_value = formatted_body
        groups.group_org_42.user_set.add(wiki_users.thasonic)
        client.login(wiki_users.thasonic)

        client.post(
            f'{api_url}/{tag}/.grid/create',
            dict(title='grid title'),
        )

        grid = Grid.active.get(supertag=tag_to_supertag(tag))
        response = client.post('/_api/frontend/.get_document', data={'uuid': grid.get_search_uuid()})
        real_data = render_document_for_indexation(find_by_search_uuid(grid.get_search_uuid()))
        # print(real_data)
        real_data['document']['body'] = formatted_body
        assert real_data == response.json()['data']
