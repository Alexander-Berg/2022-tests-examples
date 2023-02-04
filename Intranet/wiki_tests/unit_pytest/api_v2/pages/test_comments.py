import pytest
from model_mommy import mommy

from django.test import override_settings

from wiki.api_v2.public.pages.comments.exceptions import CommentingDisabled, ParentCommentNotFound
from wiki.pages.models import Comment, Page
from wiki.pages.models.consts import COMMENTS_STATUS
from intranet.wiki.tests.wiki_tests.common.acl_helper import set_access_author_only, set_access_everyone
from intranet.wiki.tests.wiki_tests.common.assert_helpers import assert_json


pytestmark = [pytest.mark.django_db]


@pytest.fixture
def page_with_comments(wiki_users, page_cluster):
    page = page_cluster['root']

    for i in range(50):
        mommy.make(Comment, user=wiki_users.thasonic, body=f'random_{i}', page=page, parent_id=None)
    return page


def test_comment_collection(wiki_users, page_with_comments, client):
    client.login(wiki_users.thasonic)
    response = client.get(f'/api/v2/public/pages/{page_with_comments.id}/comments', {'page_id': 1, 'page_size': 10})
    assert response.status_code == 200, response.json()
    response = client.get(f'/api/v2/public/pages/{page_with_comments.id}/comments', {'page_id': 6, 'page_size': 10})
    assert response.status_code == 200, response.json()
    assert not response.json()['has_next']


def test_create_comment(wiki_users, page_cluster, client):
    page_a = page_cluster['root']
    page_b = page_cluster['root/a']
    page_c: Page = page_cluster['root/b']
    set_access_author_only(page_a, [wiki_users.thasonic])

    # positive cases ===========================================

    client.login(wiki_users.thasonic)
    response = client.post(f'/api/v2/public/pages/{page_a.id}/comments', data={'body': '1a'})
    assert response.status_code == 200, response.json()
    comment_a_id = response.json()['id']

    response = client.get(f'/api/v2/public/pages/{page_a.id}/comments')

    assert response.status_code == 200, response.json()

    assert_json(
        response.json(),
        {'results': [{'body': '1a', 'parent_id': None, 'author': {'username': wiki_users.thasonic.username}}]},
    )

    # ----

    client.login(wiki_users.thasonic)
    response = client.post(f'/api/v2/public/pages/{page_b.id}/comments', data={'body': '1b'})
    assert response.status_code == 200, response.json()
    comment_b_id = response.json()['id']

    # with parent

    response = client.post(f'/api/v2/public/pages/{page_a.id}/comments', data={'body': '2a', 'parent_id': comment_a_id})
    assert response.status_code == 200, response.json()

    response = client.get(f'/api/v2/public/pages/{page_a.id}/comments')

    assert response.status_code == 200, response.json()

    assert_json(
        response.json(),
        {
            'results': [
                {'body': '2a', 'parent_id': comment_a_id, 'author': {'username': wiki_users.thasonic.username}},
                dict(),  # anything
            ]
        },
    )

    # negative cases ===================================================
    # 1. page on disabled comments

    page_c.comments_status = COMMENTS_STATUS.disabled_on_page
    page_c.save()
    response = client.post(f'/api/v2/public/pages/{page_c.id}/comments', data={'body': '1'})
    assert response.status_code == 400, response.json()
    assert response.json()['error_code'] == CommentingDisabled.error_code

    page_c.comments_status = COMMENTS_STATUS.disabled_on_cluster
    page_c.save()
    response = client.post(f'/api/v2/public/pages/{page_c.id}/comments', data={'body': '1'})
    assert response.status_code == 400, response.json()
    assert response.json()['error_code'] == CommentingDisabled.error_code

    # 2. wrong parent

    response = client.post(f'/api/v2/public/pages/{page_a.id}/comments', {'body': '2a', 'parent_id': comment_b_id})
    assert response.status_code == 400, response.json()
    assert response.json()['error_code'] == ParentCommentNotFound.error_code

    # 3. no_access - 403

    client.login(wiki_users.asm)
    response = client.post(f'/api/v2/public/pages/{page_a.id}/comments', {'body': '2a'})
    assert response.status_code == 403, response.json()


def _try_edit_delete(client, page, c_1):
    response = client.post(f'/api/v2/public/pages/{page.id}/comments/{c_1.id}', data={'body': 'xxx'})

    assert response.status_code == 200, response.json()
    response = client.get(f'/api/v2/public/pages/{page.id}/comments/{c_1.id}')
    assert response.status_code == 200, response.json()
    assert response.json()['body'] == 'xxx'

    response = client.delete(f'/api/v2/public/pages/{page.id}/comments/{c_1.id}')
    assert response.status_code == 204

    response = client.get(f'/api/v2/public/pages/{page.id}/comments/{c_1.id}')
    assert response.status_code == 404, response.json()

    response = client.post(f'/api/v2/public/pages/{page.id}/comments/{c_1.id}', {'body': 'abc'})
    assert response.status_code == 404, response.json()


def _try_edit_delete_403(client, page, c_1):
    response = client.post(f'/api/v2/public/pages/{page.id}/comments/{c_1.id}', data={'body': 'xxx'})

    assert response.status_code == 403, response.json()

    response = client.delete(f'/api/v2/public/pages/{page.id}/comments/{c_1.id}')
    assert response.status_code == 403


def test_edit_delete_comment(wiki_users, page_cluster, client):
    page = page_cluster['root']
    c_1 = mommy.make(Comment, user=wiki_users.thasonic, body='random', page=page, parent_id=None)
    c_2 = mommy.make(Comment, user=wiki_users.asm, body='random', page=page, parent_id=None)
    c_3 = mommy.make(Comment, user=wiki_users.volozh, body='random', page=page, parent_id=None)
    set_access_author_only(page, [wiki_users.thasonic, wiki_users.asm, wiki_users.volozh])

    # positive cases ===========================================

    client.login(wiki_users.thasonic)
    _try_edit_delete(client, page, c_1)  # Автор комментария может делать что хочет
    _try_edit_delete(client, page, c_2)  # Автор cтраницы  может делать что хочет

    # negative cases ===========================================
    set_access_everyone(page)

    client.login(wiki_users.chapson)
    _try_edit_delete_403(client, page, c_3)  # Нельзя редактировать или удалять чужие комменты


def test_comment__cow(wiki_users, client):
    client.login(wiki_users.thasonic)

    idx, slug = -101, 'cow-users'
    copy_on_write_info = {'id': idx, 'ru': {'title': 'Личные разделы пользователей', 'template': 'pages/ru/users.txt'}}

    with override_settings(COPY_ON_WRITE_TAGS={slug: copy_on_write_info}, COPY_ON_WRITE_IDS={idx: slug}):
        response = client.get(f'/api/v2/public/pages/{idx}/comments')
        assert response.status_code == 200, response.json()
        assert response.json()['results'] == []


def test_create_comment__cow(wiki_users, client):
    client.login(wiki_users.thasonic)

    idx, slug = -101, 'cow-users'
    copy_on_write_info = {'id': idx, 'ru': {'title': 'Личные разделы пользователей', 'template': 'pages/ru/users.txt'}}

    with override_settings(COPY_ON_WRITE_TAGS={slug: copy_on_write_info}, COPY_ON_WRITE_IDS={idx: slug}):
        response = client.post(f'/api/v2/public/pages/{idx}/comments', data={'body': '1a'})
        assert response.status_code == 200, response.json()

    comment_id = response.json()['id']

    assert Page.objects.get(supertag=slug).comments == 1
    assert Comment.objects.filter(id=comment_id).exists()


def test_create_comment__cow__already_exists(wiki_users, client, test_page):
    client.login(wiki_users.thasonic)

    idx, slug = -101, test_page.slug
    copy_on_write_info = {'id': idx, 'ru': {'title': test_page.title, 'template': 'pages/ru/users.txt'}}

    with override_settings(COPY_ON_WRITE_TAGS={slug: copy_on_write_info}, COPY_ON_WRITE_IDS={idx: slug}):
        # get comments
        response = client.get(f'/api/v2/public/pages/{idx}/comments')
        assert response.status_code == 200, response.json()
        assert response.json()['results'] == []

        # add comment
        response = client.post(f'/api/v2/public/pages/{idx}/comments', data={'body': '1a'})
        assert response.status_code == 200, response.json()

        comment_id = response.json()['id']
        assert Page.objects.get(supertag=slug, id=test_page.id).comments == 1
        assert Comment.objects.filter(id=comment_id).exists()

        # get comments again
        response = client.get(f'/api/v2/public/pages/{idx}/comments')
        assert response.status_code == 200, response.json()
        assert len(response.json()['results']) == 1
