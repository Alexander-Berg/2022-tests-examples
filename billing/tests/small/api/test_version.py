from django.urls import reverse
from rest_framework.views import status


def test_version(api_client):
    response = api_client.get(reverse("version"))
    assert response.status_code == status.HTTP_200_OK
    assert str(response.content, encoding='utf8') == '{"version": "file does not exist: /version.ini"}'
