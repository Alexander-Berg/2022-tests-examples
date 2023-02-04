import pytest

from django.conf import settings

from wiki.favorites.management.commands.sync_favorites_v2 import (
    do_full_sync,
    sync_user_autobookmarks,
    sync_user_bookmarks,
)
from wiki.favorites_v2.dao import create_bookmark, get_or_create_folder
from wiki.favorites_v2.tasks.update_autofolders import update_autofolders
from wiki.favorites.models import AutoBookmark, Bookmark, Tag

from intranet.wiki.tests.wiki_tests.common.utils import celery_eager

pytestmark = [pytest.mark.django_db]


def _create_bookmark(user, page, folder_name):
    folder = get_or_create_folder(user, folder_name)
    url = f'https://{settings.NGINX_HOST}/{page.supertag}'
    return create_bookmark(folder, page.title, url)


def test_sync_bookmarks(wiki_users, page_cluster, test_org, test_org_ctx):
    user = wiki_users.thasonic
    page = page_cluster['root']
    folder = 'test_folder'
    _create_bookmark(user, page, folder)

    assert Bookmark.objects.count() == 0
    assert Tag.objects.count() == 0

    sync_user_bookmarks(user, test_org)

    bookmarks = Bookmark.objects.all()
    tags = Tag.objects.all()
    assert len(bookmarks) == 1
    assert len(tags) == 1
    assert bookmarks[0].page_id == page.id
    assert tags[0].name == folder


@celery_eager
def test_sync_autobookmarks(wiki_users, page_cluster, test_org):
    update_autofolders()
    user = wiki_users.thasonic

    assert AutoBookmark.objects.count() == 0

    sync_user_autobookmarks(user, test_org)

    assert Bookmark.objects.count() == 0
    assert Tag.objects.count() == 0
    assert AutoBookmark.objects.count() == len(page_cluster)


@celery_eager
def test_full_sync(wiki_users, page_cluster, test_org, test_org_ctx, legacy_subscr_favor):
    user = wiki_users.thasonic
    page = page_cluster['root']
    folder = 'test_folder'
    _create_bookmark(user, page, folder)
    update_autofolders()

    assert Bookmark.objects.count() == 0
    assert Tag.objects.count() == 0
    assert AutoBookmark.objects.count() == 0

    do_full_sync()

    assert Bookmark.objects.count() == 1
    assert Tag.objects.count() == 1
    assert AutoBookmark.objects.count() == len(page_cluster)
