import datetime
import pytest

from django.test import override_settings

from wiki.pages.models import Revision
from intranet.wiki.tests.wiki_tests.common.assert_helpers import assert_json

pytestmark = [pytest.mark.django_db]


@pytest.fixture
def page_with_revisions(wiki_users, page_cluster):
    page = page_cluster['root']
    page.title = 'latest'
    page.body = 'latest content'
    page.save()

    Revision.objects.filter(page=page).delete()

    revs = []
    for i in range(10):
        new_rev: Revision = Revision.objects.produce_from_page(page)
        new_rev.mds_storage_id = None
        new_rev.created_at = datetime.datetime(year=2021, month=11, day=i + 1)
        new_rev.body = f'foo_bar_{i}'
        new_rev.title = f'megatitle{i}'
        new_rev.save()
        revs.append(new_rev)
    return page, revs


def test_revision__diff(wiki_users, page_with_revisions, client):
    page, revs = page_with_revisions
    client.login(wiki_users.thasonic)
    response = client.get(
        f'/api/v2/public/pages/{page.id}/revisions/diff?revision_a={revs[0].id}&revision_b={revs[1].id}'
    )
    assert response.status_code == 200, response.json()
    assert response.json() == {
        'content': {'diff': [[['=', 'foo_bar_'], ['-', '0'], ['+', '1']]], 'type': 'page'},
        'title': 'megatitle<del>0</del><ins>1</ins>',
    }


def test_revision__last_revision_id(wiki_users, page_with_revisions, client):
    page, revs = page_with_revisions
    client.login(wiki_users.thasonic)
    response = client.get(f'/api/v2/public/pages/{page.id}?fields=last_revision_id')
    assert response.status_code == 200, response.json()
    assert response.json()['last_revision_id'] == revs[-1].id


def test_revision__old_rev(wiki_users, page_with_revisions, client):
    page, revs = page_with_revisions
    client.login(wiki_users.thasonic)
    response = client.get(f'/api/v2/public/pages/{page.id}?revision_id={revs[0].id}&fields=content')
    assert response.status_code == 200, response.json()

    assert_json(
        response.json(),
        {
            'content': 'foo_bar_0',
            'title': 'megatitle0',
            'active_revision': {'id': revs[0].id, 'author': {'username': 'thasonic'}},
        },
    )

    response = client.get(f'/api/v2/public/pages?slug={page.slug}&revision_id={revs[1].id}&fields=content')
    assert response.status_code == 200, response.json()

    assert_json(response.json(), {'content': 'foo_bar_1', 'title': 'megatitle1'})

    response = client.get(f'/api/v2/public/pages?slug={page.slug}&fields=content')
    assert response.status_code == 200, response.json()

    assert_json(response.json(), {'content': 'latest content', 'title': 'latest'})


def test_revision__cursor(wiki_users, page_with_revisions, client):
    page, _ = page_with_revisions
    client.login(wiki_users.thasonic)

    # first page
    response = client.get(f'/api/v2/public/pages/{page.id}/revisions', {'page_size': 8})
    assert response.status_code == 200, response.json()

    res = response.json()
    created_at = [rev['created_at'] for rev in res['results']]

    assert len(res['results']) == 8
    assert sorted(created_at, reverse=True) == created_at
    assert res['prev_cursor'] is None and res['next_cursor']

    # second page
    response = client.get(f'/api/v2/public/pages/{page.id}/revisions', {'cursor': res['next_cursor']})
    assert response.status_code == 200, response.json()

    res = response.json()
    assert len(res['results']) == 2
    assert res['prev_cursor'] and res['next_cursor'] is None

    # move back
    response = client.get(f'/api/v2/public/pages/{page.id}/revisions', {'cursor': res['prev_cursor'], 'page_size': 4})
    assert response.status_code == 200, response.json()

    res = response.json()
    created_at = [rev['created_at'] for rev in res['results']]

    assert len(res['results']) == 4
    assert sorted(created_at, reverse=True) == created_at
    assert res['prev_cursor'] and res['next_cursor']


def test_revision__cow(wiki_users, client):
    client.login(wiki_users.thasonic)

    idx, slug = -101, 'cow-users'
    copy_on_write_info = {'id': idx, 'ru': {'title': 'Личные разделы пользователей', 'template': 'pages/ru/users.txt'}}

    with override_settings(COPY_ON_WRITE_TAGS={slug: copy_on_write_info}, COPY_ON_WRITE_IDS={idx: slug}):
        response = client.get(
            f'/api/v2/public/pages/{idx}/revisions',
        )
        assert response.status_code == 200, response.json()
        assert response.json()['results'] == []
