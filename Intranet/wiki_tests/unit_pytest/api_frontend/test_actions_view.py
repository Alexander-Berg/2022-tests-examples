import pytest

pytestmark = [pytest.mark.django_db]


def test_action_tree(client, wiki_users, page_cluster):
    root_page = page_cluster['root']
    client.login(wiki_users.thasonic)
    request_params = {
        'action_name': 'tree',
        'depth': 4,
        'nomark': 'false',
        'show_redirects': 'false',
        'show_grids': 'true',
        'show_files': 'false',
        'show_owners': 'false',
        'authors': '',
        'show_titles': 'true',
        'show_created_at': 'false',
        'show_modified_at': 'false',
        'sort_by': 'title',
        'sort': 'asc',
        'page': root_page.slug,
    }
    response = client.get(f'/_api/frontend/{root_page.slug}/.actions_view', data=request_params)
    assert response.status_code == 200
    response_data = response.json()['data']
    assert response_data['page'] == {'cluster': 'root', 'url': '/root', 'type': 'P', 'title': root_page.title}
    assert len(response_data['subpages']) == 3
