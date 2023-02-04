import pytest

from wiki.pages.models import PageLink
from intranet.wiki.tests.wiki_tests.common.acl_helper import set_access_author_only
from model_mommy import mommy


@pytest.mark.django_db
def test_descendants_view(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)

    all_descendants = {
        'root/a',
        'root/a/aa',
        'root/a/ad',
        'root/a/ac/bd',
        'root/a/ad/bc',
        'root/b',
        'root/b/bd',
        'root/b/bd/bc',
        'root/c',
    }

    page = page_cluster['root']
    response = client.get(f'/api/v2/public/pages/{page.id}/descendants', {'page_size': 25})
    assert response.status_code == 200
    assert {q['slug'] for q in response.json()['results']} == all_descendants

    set_access_author_only(page_cluster['root/b'], [wiki_users.asm])
    set_access_author_only(page_cluster['root/b/bd'], [wiki_users.asm])
    set_access_author_only(page_cluster['root/b/bd/bc'], [wiki_users.asm])
    response = client.get(f'/api/v2/public/pages/{page.id}/descendants', {'page_size': 25})

    all_descendants = {a for a in all_descendants if not a.startswith('root/b')}

    assert response.status_code == 200
    assert {q['slug'] for q in response.json()['results']} == all_descendants


@pytest.mark.django_db
def test_descendants_view__cursor(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)
    page = page_cluster['root']

    # get first page
    response = client.get(f'/api/v2/public/pages/{page.id}/descendants', {'page_size': 2})
    assert response.status_code == 200

    res = response.json()
    assert len(res['results']) == 2
    assert res['prev_cursor'] is None and res['next_cursor']

    # get next page
    response = client.get(f'/api/v2/public/pages/{page.id}/descendants', {'page_size': 2, 'cursor': res['next_cursor']})
    assert response.status_code == 200

    res = response.json()
    assert len(res['results']) == 2
    assert res['prev_cursor'] and res['next_cursor']


@pytest.mark.django_db
def test_backlinks_view(client, wiki_users, page_cluster):
    page = page_cluster['root']
    slugs = set()
    for slug, p in page_cluster.items():
        if p == page:
            continue
        slugs.add(slug)
        mommy.make(PageLink, from_page=p, to_page=page)

    client.login(wiki_users.thasonic)

    response = client.get(f'/api/v2/public/pages/{page.id}/backlinks', {'page_id': 1, 'page_size': 25})
    assert response.status_code == 200
    assert {q['slug'] for q in response.json()['results']} == slugs


@pytest.mark.django_db
def test_backlinks_view__cursor(client, wiki_users, page_cluster):
    page = page_cluster['root']

    slugs = ['root/a', 'root/a/aa', 'root/a/ad', 'root/b', 'root/b/bd', 'root/c']

    for slug in slugs:
        mommy.make(PageLink, from_page=page_cluster[slug], to_page=page)

    client.login(wiki_users.thasonic)

    # 1.get first page
    response = client.get(f'/api/v2/public/pages/{page.id}/backlinks', {'page_size': 3})
    assert response.status_code == 200

    res = response.json()
    assert [q['slug'] for q in res['results']] == slugs[:3]
    assert res['next_cursor']
    assert res['prev_cursor'] is None

    # 2. get next page
    response = client.get(f'/api/v2/public/pages/{page.id}/backlinks', {'page_size': 3, 'cursor': res['next_cursor']})
    assert response.status_code == 200

    res = response.json()
    assert [q['slug'] for q in res['results']] == slugs[3:6]
    assert res['next_cursor'] is None
    assert res['prev_cursor']
