
def test_ping_route(client):
    response = client.get(
        '/api/watcher/common/ping',
    )
    assert response.status_code == 200, response.text
    data = response.json()
    assert data == {'status': 'ok'}
