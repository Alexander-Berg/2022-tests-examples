import pytest
from mock import patch

from wiki.grids.utils import dummy_request_for_grids, insert_rows

pytestmark = [
    pytest.mark.django_db,
]


def test_page_html(client, test_page, wiki_users):
    client.login(wiki_users.thasonic)
    with patch('wiki.utils.wfaas.client.WfaasClient.raw_to_html', return_value=''):
        response = client.get(f'/_api/v1/pages/{test_page.supertag}/.html')
        assert response.status_code == 200


def test_grid_html(client, test_grid, wiki_users):
    client.login(wiki_users.thasonic)
    insert_rows(
        test_grid,
        [
            {'src': 'source1', 'dst': 'destination1'},
            {'src': 'source2'},
            {'src': 'source3', 'dst': 'destination2', 'staff': 'chapson'},
        ],
        dummy_request_for_grids(),
    )
    test_grid.save()

    with patch('wiki.utils.wfaas.client.WfaasClient.raw_to_html', return_value=''):
        response = client.get(f'/_api/v1/pages/{test_grid.supertag}/.html')
        assert response.status_code == 200


def test_wysiwyg_html(client, test_wysiwyg, wiki_users):
    client.login(wiki_users.thasonic)
    with patch('wiki.utils.wfaas.client.WfaasClient.yfm_to_html', return_value=''):
        response = client.get(f'/_api/v1/pages/{test_wysiwyg.supertag}/.html')
    assert response.status_code == 200
