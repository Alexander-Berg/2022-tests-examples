from django.urls import reverse
from rest_framework.views import status


def test_ping(api_client):
    response = api_client.get(reverse("ping"))
    assert response.status_code == status.HTTP_200_OK
    assert str(response.content, encoding='utf8') == '{"database": {"status": "ok"}}'
