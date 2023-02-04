from django.urls import reverse


def test_ping(client):
    url = reverse('ping')
    response = client.get(url)
    assert response.status_code == 200, response.content
    assert response.content.decode('utf-8') == "I'm alive!"
