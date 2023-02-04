import pytest
from django.core.urlresolvers import reverse

pytestmark = pytest.mark.django_db


def test_language(client):
    response = client.get(reverse('ping'))
    assert response['Content-Language'] == 'en'

    response = client.get(
        reverse('ping'),
        HTTP_ACCEPT_LANGUAGE='ru',
    )
    assert response['Content-Language'] == 'ru'

    response = client.get(
        reverse('ping'),
        HTTP_ACCEPT_LANGUAGE='en',
    )
    assert response['Content-Language'] == 'en'

    response = client.get(
        reverse('ping'),
        HTTP_ACCEPT_LANGUAGE='xx',
    )
    assert response['Content-Language'] == 'en'
