# coding: utf-8



import pytest

from django.conf import settings
from django.shortcuts import reverse
from django.core.urlresolvers import resolve

from at.common.models import Posts, Entryxmlcontent, Postcategories

pytestmark = pytest.mark.django_db

LOGIN = settings.YAUTH_TEST_USER['login']
UID = settings.YAUTH_TEST_USER['uid']
CLUB_FEED_ID = settings.AT_TEST_CLUB['feed_id']
CLUB_SLUG = settings.AT_TEST_CLUB['name']
CLUB_POST_ITEM_NO = BLOG_POST_ITEM_NO = 1

DIRECT_ENDPOINTS = [
    ('blog_person', (UID,)),
    # ('blog_posts', (UID,)),
    # ('blog_post', (UID, BLOG_POST_ITEM_NO)),
    ('blog_comment', (UID, BLOG_POST_ITEM_NO)),
    # ('club', (CLUB_FEED_ID,)),
    # ('club_posts', (CLUB_FEED_ID,)),
    # ('club_post', (CLUB_FEED_ID, CLUB_POST_ITEM_NO)),
    ('club_comment', (CLUB_FEED_ID, CLUB_POST_ITEM_NO)),
]

FORMATS = [
    'json',
    '',
]


@pytest.fixture
def prepare_db_data():
    Posts.objects.create(person_id=CLUB_FEED_ID,
                         post_no=CLUB_POST_ITEM_NO,
                         author_uid=UID,
                         store_time='2000-11-11 11:11:00+0000',
                         store_time_month_year='2000-11-11',
                         item_time='2000-11-11 11:11:00+0000',
                         post_type=2,
                         deleted=0,
                         public=True,
                         store_time_usec=99999,
                         access_type=1,
                         last_updated='2000-11-11 11:11:00+0000',
                         children_count=0,
                         rubric_id=13,
                         popular_category=99,
                         )
    Posts.objects.create(person_id=UID,
                         post_no=BLOG_POST_ITEM_NO,
                         author_uid=UID,
                         store_time='2000-11-11 11:11:00+0000',
                         store_time_month_year='2000-11-11',
                         item_time='2000-11-11 11:11:00+0000',
                         post_type=2,
                         deleted=0,
                         public=True,
                         store_time_usec=99999,
                         access_type=1,
                         last_updated='2000-11-11 11:11:00+0000',
                         children_count=0,
                         rubric_id=13,
                         popular_category=99,
                         )
    Entryxmlcontent.objects.create(feed_id=CLUB_FEED_ID,
                                   post_no=CLUB_POST_ITEM_NO,
                                   comment_id=0
                                   )
    Entryxmlcontent.objects.create(feed_id=UID,
                                   post_no=BLOG_POST_ITEM_NO,
                                   comment_id=0
                                   )
    Postcategories.objects.create(feed_id=CLUB_FEED_ID,
                                  post_no=CLUB_POST_ITEM_NO,
                                  deleted=0,
                                  cat_id=2
                                  )
    Postcategories.objects.create(feed_id=UID,
                                  post_no=BLOG_POST_ITEM_NO,
                                  deleted=0,
                                  cat_id=2
                                  )


@pytest.mark.usefixtures("prepare_db_data")
@pytest.mark.parametrize(
    'urlname, args',
    DIRECT_ENDPOINTS,
)
@pytest.mark.parametrize(
    'format',
    FORMATS,
)
def test_get_with_200_status(client, urlname, args, format):
    response = client.get(reverse(urlname, args=args), {'format': format})
    assert response.status_code == 200


@pytest.mark.usefixtures("prepare_db_data")
@pytest.mark.parametrize(
    'urlname, args',
    DIRECT_ENDPOINTS,
)
@pytest.mark.parametrize(
    'format',
    FORMATS,
)
def test_get_with_something_sane_data(client, urlname, args, format):
    response = client.get(reverse(urlname, args=args), {'format': format})
    for arg in args:
        assert str(arg).encode('utf-8') in response.content


REDIRECT_ENDPOINTS = [
    ('club', (CLUB_SLUG,), 'club', {'feed_id': str(CLUB_FEED_ID)}),
]


@pytest.mark.usefixtures("prepare_db_data")
@pytest.mark.parametrize(
    'urlname, args, redirect_urlname, redirect_kwargs',
    REDIRECT_ENDPOINTS,
)
def test_get_redirect(client, urlname, args, redirect_urlname, redirect_kwargs):
    response = client.get(reverse(urlname, args=args))
    assert response.status_code == 302
    resolved = resolve(response.url)
    assert resolved.url_name == redirect_urlname
    assert resolved.kwargs == redirect_kwargs
