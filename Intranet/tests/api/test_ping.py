
async def test_ping_route(client):
    response = await client.get(
        '/api/common/ping',
    )
    assert response.status_code == 200, response.text
    data = response.json()
    assert data == {'status': 'ok'}
