def test_ping(client):
    assert client.get('/ping/').status_code == 200
