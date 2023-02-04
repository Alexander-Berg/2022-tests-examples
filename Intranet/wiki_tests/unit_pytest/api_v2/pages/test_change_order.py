import pytest

from json import loads

from wiki import access as wiki_access

pytestmark = [
    pytest.mark.django_db
]


def test_change_order_before(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)
    moved_page = page_cluster['root/c']
    assert moved_page.rank > page_cluster['root/a'].rank
    response = client.post(f'/api/v2/public/pages/{moved_page.id}/change_order?next_to_slug=root/a&position=before')
    assert response.status_code == 200
    moved_page.refresh_from_db()
    assert page_cluster['root/b'].rank > moved_page.rank > page_cluster['root/a'].rank

    response = client.get('/api/v2/public/navtree/open_node?parent_slug=root')
    assert response.status_code == 200
    response_data = response.json()
    assert [child['slug'] for child in response_data['children']['results']] == ['root/b', 'root/c', 'root/a']


def test_change_order_after(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)
    moved_page = page_cluster['root/c']
    assert moved_page.rank > page_cluster['root/b'].rank > page_cluster['root/a'].rank
    response = client.post(f'/api/v2/public/pages/{moved_page.id}/change_order?next_to_slug=root/b&position=after')
    assert response.status_code == 200
    moved_page.refresh_from_db()
    assert page_cluster['root/b'].rank > moved_page.rank > page_cluster['root/a'].rank

    response = client.get('/api/v2/public/navtree/open_node?parent_slug=root')
    assert response.status_code == 200
    response_data = response.json()
    assert [child['slug'] for child in response_data['children']['results']] == ['root/b', 'root/c', 'root/a']


def test_change_order_mixed(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)
    root_a = page_cluster['root/a']
    root_b = page_cluster['root/b']
    root_c = page_cluster['root/c']
    assert root_c.rank > root_b.rank
    response = client.post(f'/api/v2/public/pages/{root_c.id}/change_order?next_to_slug=root/a&position=before')
    assert response.status_code == 200
    root_c.refresh_from_db()
    assert root_b.rank > root_c.rank > root_a.rank

    response = client.post(f'/api/v2/public/pages/{root_b.id}/change_order?next_to_slug=root/a&position=before')
    assert response.status_code == 200
    root_b.refresh_from_db()
    assert root_c.rank > root_b.rank > root_a.rank

    response = client.post(f'/api/v2/public/pages/{root_a.id}/change_order?next_to_slug=root/c&position=after')
    assert response.status_code == 200
    root_a.refresh_from_db()
    assert root_c.rank > root_a.rank > root_b.rank

    response = client.get('/api/v2/public/navtree/open_node?parent_slug=root')
    assert response.status_code == 200
    response_data = response.json()
    assert [child['slug'] for child in response_data['children']['results']] == ['root/c', 'root/a', 'root/b']


def test_access_cluster(client, wiki_users, page_cluster):
    root_page = page_cluster['root']
    root_page.authors.remove(wiki_users.thasonic)
    wiki_access.set_access(root_page, wiki_access.TYPES.OWNER, wiki_users.chapson)

    client.login(wiki_users.thasonic)
    moved_page = page_cluster['root/c']
    response = client.post(f'/api/v2/public/pages/{moved_page.id}/change_order?next_to_slug=root/a&position=after')
    assert response.status_code == 403


def test_different_cluster(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)
    moved_page = page_cluster['root/c']
    response = client.post(f'/api/v2/public/pages/{moved_page.id}/change_order?next_to_slug=root/a/aa&position=before')
    assert response.status_code == 400
    response_data = response.json()
    assert 'is different from next_to_slug cluster' in response_data['debug_message']


def test_pages_the_same(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)
    moved_page = page_cluster['root/c']
    response = client.post(f'/api/v2/public/pages/{moved_page.id}/change_order?next_to_slug=root/c&position=before')
    assert response.status_code == 400
    response_data = response.json()
    assert 'are the same' in response_data['debug_message']
