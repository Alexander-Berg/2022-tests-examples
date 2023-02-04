from pprint import pprint

import pytest

from intranet.wiki.tests.wiki_tests.common.skips import only_intranet
from intranet.wiki.tests.wiki_tests.common.assert_helpers import assert_json

pytestmark = [pytest.mark.django_db]


def test_crud_403(client, wiki_users, page_cluster):
    client.login(wiki_users.thasonic)
    response = client.get('/api/v2/support/featured_pages/groups')
    assert response.status_code == 403


def test_get(client, support_client, page_cluster, featured_pages_msk, featured_pages_spb):
    response = support_client.get('/api/v2/support/featured_pages/groups')
    assert response.status_code == 200
    assert len(response.json()['results']) == 2


def test_get__cursor(client, support_client, page_cluster, featured_pages_msk, featured_pages_spb):
    # first page
    response = support_client.get('/api/v2/support/featured_pages/groups?page_size=1')
    assert response.status_code == 200

    res = response.json()
    assert len(res['results']) == 1
    assert res['results'][0]['id'] == featured_pages_msk.id
    assert res['prev_cursor'] is None and res['next_cursor']

    # second page
    response = support_client.get(f'/api/v2/support/featured_pages/groups?cursor={res["next_cursor"]}')
    assert response.status_code == 200

    res = response.json()
    assert len(res['results']) == 1
    assert res['results'][0]['id'] == featured_pages_spb.id
    assert res['prev_cursor'] and res['next_cursor'] is None


def test_get_one(client, support_client, page_cluster, featured_pages_msk, featured_pages_spb):
    response = support_client.get(f'/api/v2/support/featured_pages/groups/{featured_pages_msk.id}/')
    assert response.status_code == 200


def test_patch(client, support_client, page_cluster, featured_pages_msk, featured_pages_spb):
    response = support_client.post(
        f'/api/v2/support/featured_pages/groups/{featured_pages_msk.id}/',
        data={'title': 'Московские офисы', 'rank': 100500},
    )

    assert response.status_code == 200
    featured_pages_msk.refresh_from_db()
    assert featured_pages_msk.rank == 100500
    assert featured_pages_msk.title == 'Московские офисы'

    response = support_client.post(
        f'/api/v2/support/featured_pages/groups/{featured_pages_msk.id}/',
        data={
            'links': [
                {
                    'title': 'Auto.Ru',
                    'rank': 10,
                    'external_url': 'https://auto.ru',
                },
                {
                    'title': 'Best Search',
                    'rank': 20,
                    'external_url': 'https://ya.ru',
                },
                {'rank': 30, 'page': {'id': page_cluster['root'].id}},
                {'rank': 15, 'page': {'slug': page_cluster['root/a'].slug}},
            ]
        },
    )
    assert response.status_code == 200, response.json()
    assert_json(
        response.json(),
        {
            'rank': 100500,
            'links': [
                {'external_url': 'https://auto.ru', 'title': 'Auto.Ru'},
                {'page': {'slug': 'root/a'}},
                {'external_url': 'https://ya.ru', 'title': 'Best Search'},
                {'page': {'slug': 'root'}},
            ],
        },
    )


@only_intranet
def test_patch_geo(client, support_client, page_cluster, featured_pages_msk, featured_pages_spb, geo):
    patch = {'visibility': {'geo': {'city': geo.telaviv.id}}}

    response = support_client.post(f'/api/v2/support/featured_pages/groups/{featured_pages_msk.id}/', data=patch)

    assert response.status_code == 200, response.json()
    assert_json(response.json(), patch)


def test_delete(client, support_client, page_cluster, featured_pages_msk, featured_pages_spb):
    response = support_client.delete(f'/api/v2/support/featured_pages/groups/{featured_pages_msk.pk}/')
    assert response.status_code == 204

    response = support_client.get('/api/v2/support/featured_pages/groups')
    assert response.status_code == 200
    assert_json(response.json(), {'results': [{'id': featured_pages_spb.id}]})


def test_create(client, support_client, page_cluster, featured_pages_msk, featured_pages_spb):
    response = support_client.post(
        '/api/v2/support/featured_pages/groups',
        data={
            'title': 'Московские офисы',
            'rank': 100500,
            'visibility': {'affilation': 'staff'},
            'links': [
                {
                    'title': 'Auto.Ru',
                    'rank': 10,
                    'external_url': 'https://auto.ru',
                },
                {
                    'title': 'Best Search',
                    'rank': 20,
                    'external_url': 'https://ya.ru',
                },
                {'rank': 30, 'page': {'id': page_cluster['root'].id}},
            ],
        },
    )
    assert_json(
        response.json(),
        {
            'links': [
                {'external_url': 'https://auto.ru', 'title': 'Auto.Ru'},
                {'external_url': 'https://ya.ru', 'title': 'Best Search'},
                {'page': {'slug': 'root'}},
            ]
        },
    )
    pprint(response.json())
    assert response.status_code == 200
