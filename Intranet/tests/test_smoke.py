import pytest

from intranet.paste.src.coreapp.models import Code
from django.urls import reverse


@pytest.mark.django_db
def test_smoke__create_new_paste(client):
    response = client.post(
        '/',
        {
            'syntax': 'python',
            'text': 'print(123)',
        },
        content_type=client.FORM_MULTIPART,
    )
    assert response.status_code == 302

    code = Code.objects.all().first()

    assert response.get('Location') == reverse('code', kwargs={'pk': code.pk})


@pytest.mark.django_db
def test_smoke__get_main(client):
    response = client.get(
        '/',
    )
    assert response.status_code == 200


@pytest.mark.django_db
def test_smoke__create_new_paste__existing_user(client, users):
    client.login(users.thasonic).use_oauth()

    response = client.post(
        '/',
        {
            'syntax': 'python',
            'text': 'print(123)',
        },
        content_type=client.FORM_MULTIPART,
    )
    assert response.status_code == 302

    code = Code.objects.all().first()

    assert response.get('Location') == reverse('code', kwargs={'pk': code.pk})
