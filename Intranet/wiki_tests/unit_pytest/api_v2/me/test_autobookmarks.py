import pytest

from json import loads

from wiki.favorites import logic
from wiki.favorites.consts import AutoBookmarkType
from wiki.pages.api import save_page
from wiki.sync.connect.org_ctx import org_ctx

from intranet.wiki.tests.wiki_tests.common.utils import celery_eager

pytestmark = [
    pytest.mark.django_db
]


@celery_eager
def test_limit_autobookmarks(client, wiki_users, test_org, monkeypatch):
    monkeypatch.setattr(logic, 'AUTOBOOKMARK_LIMIT', 2)

    client.login(wiki_users.thasonic)
    # Запрос, чтобы создать страницы домашнего кластера, которые также попадают в автозакладки
    response = client.get('/api/v2/public/me/autobookmarks')
    assert response.status_code == 200

    with org_ctx(test_org):
        save_page('test_page1', unicode_text='some body', user=wiki_users.thasonic)
        save_page('test_page2', unicode_text='some body', user=wiki_users.thasonic)
        save_page('test_page3', unicode_text='some body', user=wiki_users.thasonic)
        save_page('test_page4', unicode_text='some body', user=wiki_users.thasonic)

    response = client.get(f'/api/v2/public/me/autobookmarks?bookmark_type={AutoBookmarkType.CREATOR}')
    assert response.status_code == 200

    response_data = loads(response.content.decode())
    assert len(response_data['results']) == 2
    assert ['testpage4', 'testpage3'] == [bookmark['page']['slug'] for bookmark in response_data['results']]

    with org_ctx(test_org):
        save_page('test_page11', unicode_text='some body', user=wiki_users.chapson)
        save_page('test_page12', unicode_text='some body', user=wiki_users.chapson)

    # Обновление автозакладок для другого пользователя не повлияло на автозакладки текущего
    response = client.get(f'/api/v2/public/me/autobookmarks?bookmark_type={AutoBookmarkType.CREATOR}')
    assert response.status_code == 200

    response_data = loads(response.content.decode())
    assert len(response_data['results']) == 2


@celery_eager
def test_get_autobookmarks(client, wiki_users, test_org):
    client.login(wiki_users.thasonic)
    response = client.get('/api/v2/public/me/autobookmarks')
    assert response.status_code == 200
    response_data = loads(response.content.decode())
    bookmarks_count = len(response_data['results'])

    with org_ctx(test_org):
        save_page('test_page', unicode_text='some body', user=wiki_users.thasonic)

    response = client.get('/api/v2/public/me/autobookmarks')
    assert response.status_code == 200

    response_data = loads(response.content.decode())
    assert len(response_data['results']) == bookmarks_count + 1

    last_bookmark = response_data['results'][0]
    assert last_bookmark['page']['slug'] == 'testpage'
    assert last_bookmark['bookmark_type'] == AutoBookmarkType.CREATOR
    assert last_bookmark['page']['is_active'] is True


@celery_eager
def test_get_editor_autobookmarks(client, wiki_users, test_page, test_org):
    client.login(wiki_users.thasonic)
    response = client.get(f'/api/v2/public/me/autobookmarks?bookmark_type={AutoBookmarkType.EDITOR}')
    assert response.status_code == 200
    response_data = loads(response.content.decode())
    bookmarks_count = len(response_data['results'])

    with org_ctx(test_org):
        save_page(test_page, unicode_text='some body', user=wiki_users.thasonic)

    response = client.get(f'/api/v2/public/me/autobookmarks?bookmark_type={AutoBookmarkType.EDITOR}')
    assert response.status_code == 200
    response_data = loads(response.content.decode())
    assert len(response_data['results']) == bookmarks_count + 1

    last_bookmark = response_data['results'][0]
    assert last_bookmark['page']['slug'] == 'testpage'
    assert last_bookmark['bookmark_type'] == AutoBookmarkType.EDITOR
    assert last_bookmark['page']['is_active'] is True
