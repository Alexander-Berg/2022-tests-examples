import pytest

from django.test import override_settings

from intranet.wiki.tests.wiki_tests.common.acl_helper import set_access_author_only


@pytest.mark.django_db
def test_page_edit__background(client, wiki_users, test_page):
    client.login(wiki_users.thasonic)

    backgrounds = {
        99: {'id': 99, 'color': 'rgba(73, 160, 246, 0.3)', 'type': 'color'},
        1: {'id': 1, 'url': 'https://yandex.net/1.jpg', 'preview': 'https://yandex.net/1_small.jpg', 'type': 'image'},
    }

    assert test_page.background_id is None

    with override_settings(PAGE_BACKGROUNDS_IDS=backgrounds):
        response = client.post(f'/api/v2/public/pages/{test_page.id}?fields=background', {'background': 99})

    assert response.status_code == 200
    assert response.json()['background'] == backgrounds[99]
    assert response.json()['title'] == test_page.title

    test_page.refresh_from_db()
    assert test_page.background_id == 99


@pytest.mark.django_db
def test_page_edit__background__invalid_id(client, wiki_users, test_page):
    client.login(wiki_users.thasonic)

    with override_settings(PAGE_BACKGROUNDS_IDS={}):
        response = client.post(f'/api/v2/public/pages/{test_page.id}?fields=background', {'background': 1234})

    assert response.status_code == 404


@pytest.mark.django_db
def test_page_edit__background__no_access(client, wiki_users, test_page):
    client.login(wiki_users.thasonic)

    set_access_author_only(test_page, new_authors=[wiki_users.asm])

    with override_settings(PAGE_BACKGROUNDS=[]):
        response = client.post(f'/api/v2/public/pages/{test_page.id}?fields=background', {'background': 99})

    assert response.status_code == 403


@pytest.mark.django_db
def test_page_edit__background__default(client, wiki_users, test_page):
    client.login(wiki_users.thasonic)

    test_page.background_id = 123
    test_page.save()

    with override_settings(PAGE_BACKGROUNDS=[]):
        response = client.post(f'/api/v2/public/pages/{test_page.id}?fields=background', {'background': None})

    assert response.status_code == 200
    assert response.json()['background'] is None

    test_page.refresh_from_db()
    assert test_page.background_id is None
