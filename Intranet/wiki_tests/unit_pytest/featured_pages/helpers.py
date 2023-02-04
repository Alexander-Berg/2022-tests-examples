from intranet.wiki.tests.wiki_tests.common.assert_helpers import assert_json


def _assert_msk(client):
    response = client.get('/api/v2/public/me/featured_pages')
    assert response.status_code == 200
    assert_json(response.json(), {'groups': [{'title': 'Москва'}]})


def _assert_spb(client):
    response = client.get('/api/v2/public/me/featured_pages')
    assert response.status_code == 200
    assert_json(response.json(), {'groups': [{'title': 'Питер'}]})


def _assert_both(client):
    response = client.get('/api/v2/public/me/featured_pages')
    assert response.status_code == 200
    assert_json(response.json(), {'groups': [{'title': 'Москва'}, {'title': 'Питер'}]})
