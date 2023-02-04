import pytest

from wiki import access as wiki_access

pytestmark = [pytest.mark.django_db]


def test_simple_tree(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)
    slug = 'root'
    response = client.get(f'/api/v2/public/pages/navigation_tree?slug={slug}')
    assert response.status_code == 200
    expected_response = {
        'node': {
            'children_count': 3,
            'is_missing': False,
            'page_id': page_cluster[slug].id,
            'page_type': 'page',
            'slug': slug,
            'title': page_cluster[slug].title,
        },
        'children': [{
            'children_count': 4,
            'page_id': page_cluster['root/a'].id,
            'page_type': 'page',
            'slug': 'root/a',
            'title': page_cluster['root/a'].title,
            'is_missing': False
        }, {
            'children_count': 2,
            'page_id': page_cluster['root/b'].id,
            'page_type': 'page',
            'slug': 'root/b',
            'title': page_cluster['root/b'].title,
            'is_missing': False
        }, {
            'children_count': 0,
            'page_id': page_cluster['root/c'].id,
            'page_type': 'page',
            'slug': 'root/c',
            'title': page_cluster['root/c'].title,
            'is_missing': False
        }],
        'has_next': False,
    }
    assert response.json() == expected_response


def test_last_node(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)
    slug = 'root/a/aa'
    response = client.get(f'/api/v2/public/pages/navigation_tree?slug={slug}')
    assert response.status_code == 200
    expected_response = {
        'node': {
            'children_count': 0,
            'is_missing': False,
            'page_id': page_cluster[slug].id,
            'page_type': 'page',
            'slug': slug,
            'title': page_cluster[slug].title,
        },
        'children': [],
        'has_next': False,
    }
    assert response.json() == expected_response


def test_with_skips(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)
    slug = 'root/a'
    response = client.get(f'/api/v2/public/pages/navigation_tree?slug={slug}')
    assert response.status_code == 200
    expected_response = {
        'node': {
            'children_count': 3,
            'is_missing': False,
            'page_id': page_cluster[slug].id,
            'page_type': 'page',
            'slug': slug,
            'title': page_cluster[slug].title,
        },
        'children': [{
            'children_count': 0,
            'page_id': page_cluster['root/a/aa'].id,
            'page_type': 'page',
            'slug': 'root/a/aa',
            'title': page_cluster['root/a/aa'].title,
            'is_missing': False
        }, {
            'children_count': 1,
            'page_id': page_cluster['root/a/ad'].id,
            'page_type': 'page',
            'slug': 'root/a/ad',
            'title': page_cluster['root/a/ad'].title,
            'is_missing': False
        }, {
            'children_count': 1,
            'page_id': None,
            'page_type': None,
            'slug': 'root/a/ac',
            'title': 'root/a/ac',
            'is_missing': True
        }],
        'has_next': False,
    }
    assert response.json() == expected_response


def test_limit(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)
    slug = 'root'

    response = client.get(f'/api/v2/public/pages/navigation_tree?slug={slug}&page_size=1')
    assert response.status_code == 200

    expected_response = {
        'node': {
            'children_count': 1,
            'is_missing': False,
            'page_id': page_cluster[slug].id,
            'page_type': 'page',
            'slug': slug,
            'title': page_cluster[slug].title,
        },
        'children': [{
            'children_count': 4,
            'page_id': page_cluster['root/a'].id,
            'page_type': 'page',
            'slug': 'root/a',
            'title': page_cluster['root/a'].title,
            'is_missing': False
        }],
        'has_next': True,
    }
    assert response.json() == expected_response


def test_access_node(client, wiki_users, page_cluster):
    slug = 'root'
    page_cluster[slug].authors.remove(wiki_users.thasonic)
    wiki_access.set_access(page_cluster[slug], wiki_access.TYPES.OWNER, wiki_users.chapson)
    client.login(wiki_users.thasonic)

    response = client.get(f'/api/v2/public/pages/navigation_tree?slug={slug}')
    assert response.status_code == 200
    expected_response = {
        'node': {
            'children_count': 3,
            'is_missing': False,
            'page_id': page_cluster[slug].id,
            'page_type': None,
            'slug': slug,
            'title': slug,
        },
        'children': [{
            'children_count': 4,
            'page_id': page_cluster['root/a'].id,
            'page_type': 'page',
            'slug': 'root/a',
            'title': page_cluster['root/a'].title,
            'is_missing': False
        }, {
            'children_count': 2,
            'page_id': page_cluster['root/b'].id,
            'page_type': 'page',
            'slug': 'root/b',
            'title': page_cluster['root/b'].title,
            'is_missing': False
        }, {
            'children_count': 0,
            'page_id': page_cluster['root/c'].id,
            'page_type': 'page',
            'slug': 'root/c',
            'title': page_cluster['root/c'].title,
            'is_missing': False
        }],
        'has_next': False,
    }
    assert response.json() == expected_response


def test_access_subpage(client, wiki_users, page_cluster):
    page_cluster['root/a'].authors.remove(wiki_users.thasonic)
    wiki_access.set_access(page_cluster['root/a'], wiki_access.TYPES.OWNER, wiki_users.chapson)
    client.login(wiki_users.thasonic)
    slug = 'root'

    response = client.get(f'/api/v2/public/pages/navigation_tree?slug={slug}')
    assert response.status_code == 200
    expected_response = {
        'node': {
            'children_count': 3,
            'is_missing': False,
            'page_id': page_cluster[slug].id,
            'page_type': 'page',
            'slug': slug,
            'title': page_cluster[slug].title,
        },
        'children': [{
            'children_count': 4,
            'page_id': page_cluster['root/a'].id,
            'page_type': None,
            'slug': 'root/a',
            'title': 'root/a',
            'is_missing': False,
        }, {
            'children_count': 2,
            'page_id': page_cluster['root/b'].id,
            'page_type': 'page',
            'slug': 'root/b',
            'title': page_cluster['root/b'].title,
            'is_missing': False,
        }, {
            'children_count': 0,
            'page_id': page_cluster['root/c'].id,
            'page_type': 'page',
            'slug': 'root/c',
            'title': page_cluster['root/c'].title,
            'is_missing': False,
        }],
        'has_next': False,
    }
    assert response.json() == expected_response


def test_nonexistent_slug(client, wiki_users, page_cluster):
    nonexistent_with_children = 'root/a/ac'

    client.login(wiki_users.thasonic)
    response = client.get(f'/api/v2/public/pages/navigation_tree?slug={nonexistent_with_children}')
    assert response.status_code == 200
    expected_response = {
        'node': {
            'children_count': 1,
            'is_missing': True,
            'page_id': None,
            'page_type': None,
            'slug': nonexistent_with_children,
            'title': nonexistent_with_children,
        },
        'children': [{
            'children_count': 0,
            'page_id': page_cluster['root/a/ac/bd'].id,
            'page_type': 'page',
            'slug': 'root/a/ac/bd',
            'title': page_cluster['root/a/ac/bd'].title,
            'is_missing': False
        }],
        'has_next': False,
    }
    assert response.json() == expected_response
