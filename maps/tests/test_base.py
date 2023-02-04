import http.client


def test_initial_state(garden_client):
    response = garden_client.get("/")
    assert response.status_code == http.client.OK
    return response.get_json()


def test_dev_links(garden_client):
    response = garden_client.get("/dev_links/")
    assert response.status_code == http.client.OK
    return response.get_json()
