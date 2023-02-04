
def test_refs_associates(api_client):

    response = api_client.get(f'/api/refs/associates/')
    assert response.ok
    assert 'alias' in response.json['data']['items'][0]


def test_refs_statuses(api_client):

    response = api_client.get(f'/api/refs/statuses/')
    assert response.ok
    items = response.json['data']['items']
    assert items
    assert items[0]['realm'] == 'payments'
    assert len(items[0]['statuses'])


def test_refs_services(api_client):

    response = api_client.get(f'/api/refs/services/')
    assert response.ok
    items = response.json['data']['items']
    assert items
    assert 'alias' in items[0]
