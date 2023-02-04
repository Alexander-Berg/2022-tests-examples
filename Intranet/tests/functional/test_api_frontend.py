# coding: utf-8



import pytest

from django.conf import settings

from at.api.frontend import views
from at.common import Types
from at.common.models import Posts, Entryxmlcontent
from at.common.utils import get_connection

pytestmark = pytest.mark.django_db

LOGIN = settings.YAUTH_TEST_USER['login']
UID = settings.YAUTH_TEST_USER['uid']
CLUB_FEED_ID = settings.AT_TEST_CLUB['feed_id']
CLUB_SLUG = settings.AT_TEST_CLUB['name']
CLUB_POST_ITEM_NO = BLOG_POST_ITEM_NO = 1
CLUB_POST_COMMENT_ID = 7  # for yaru club
DUMMY_COUNT = 10

# быстро и неряшливо тестируем не разваливается ли API и не сыплет
# ли ошибками при простейших параметрах
TEST_PARAMS = {
    'feed_id': CLUB_FEED_ID,
    'uid': UID,
    'feed_ids': str(CLUB_FEED_ID),
    'feed_id_str': str(CLUB_FEED_ID),
    'ids_string': '.'.join(map(str, [
        CLUB_FEED_ID, CLUB_POST_ITEM_NO
    ])),
    'id': '.'.join(map(str, [
        CLUB_FEED_ID, CLUB_POST_ITEM_NO, CLUB_POST_COMMENT_ID
    ])),
    'comment_id': str(CLUB_POST_COMMENT_ID),
    'user_ids': str(UID),
    'person_id': UID,
    'item_no': CLUB_POST_ITEM_NO,
    'part': 'at',
    'query': 'at',
    'post_types_str': 'text',
    'types': 'text',
    'queue': 'AT',
    'page_num': 1,
    'page': 1,
    'tb': 10,
    'slug': CLUB_SLUG,
    'source_id': CLUB_FEED_ID,
    'host_person_id': CLUB_FEED_ID,
    'tag': 'at',
    'tag_name': 'at',
    'uid_str': str(UID),
    'parent_id': 0,
    'last_seen_id': 0,
    'no': 'yes',
}

ENDPOINTS = []


@pytest.fixture
def prepare_db_data():
    Posts.objects.create(person_id=CLUB_FEED_ID,
                         post_no=CLUB_POST_ITEM_NO,
                         author_uid=UID,
                         store_time='2000-11-11 11:11+0000',
                         store_time_month_year='2000-11-11',
                         item_time='2000-11-11 11:11+0000',
                         post_type=2,
                         deleted=0,
                         public=True,
                         store_time_usec=99999,
                         access_type=1,
                         last_updated='2000-11-11 11:11+0000',
                         children_count=0,
                         rubric_id=13,
                         popular_category=99,
                         )
    Entryxmlcontent.objects.create(feed_id=CLUB_FEED_ID,
                                   post_no=CLUB_POST_ITEM_NO,
                                   comment_id=0
                                   )


def get_data(endpoint):
    meta = views.get_endpoint_meta(endpoint)
    if meta['http_method'] != 'GET':
        return
    if meta['broken']:
        return

    form_cls = meta['form_cls']

    data = {}

    if form_cls:
        for name, field in list(form_cls.declared_fields.items()):
            if name in TEST_PARAMS:
                data[name] = TEST_PARAMS[name]
            elif name.endswith(('count', 'size', 'limit', 'pageLength')):
                data[name] = DUMMY_COUNT
            elif name.endswith('uid'):
                data[name] = UID
            elif name.endswith('feed'):
                data[name] = CLUB_FEED_ID
            elif not field.required:
                continue
            else:
                return
    return data


SKIP_ENDPOINTS = [
    # oauth, isearch and shit
    'get_ml_suggest',
    'get_trackers_projects',
]

SQLITE_SKIP_ENDPOINTS = [
    'all_friends_block',
    'read_friends_light',
    'all_community_block',
    'get_tag_suggest',
    'get_tag_id',
    'find_relations_str',
    'get_banned_persons',
    'get_comment',
    'get_rubrics_list',
    'get_user_communities_paged_light',
    'get_tag_cloud_to_xml',
    'get_post_safe',
    'get_feed_trends',
    'get_tags_xml_paged',
    'get_all_widget_settings',
    'get_community_options',
    'find_relations'
]

if hasattr(settings, 'USE_SQLITE_IN_PY_TEST') and settings.USE_SQLITE_IN_PY_TEST:
    SKIP_ENDPOINTS += SQLITE_SKIP_ENDPOINTS

for endpoint in set(views.endpoints) - set(SKIP_ENDPOINTS):
    data = get_data(endpoint)
    if data:
        ENDPOINTS.append((endpoint, data))


@pytest.mark.usefixtures("prepare_db_data")
@pytest.mark.parametrize(
    'method_name, params',
    ENDPOINTS,
)
def test_get_with_200_status_front(client, method_name, params):
    path = '/api/frontend/%s/' % method_name
    response = client.get(path, params)
    assert response.status_code == 200


@pytest.mark.skip('Not work in teamcity for some reasons')
@pytest.mark.usefixtures("prepare_db_data")
@pytest.mark.parametrize(
    'method_name, params',
    ENDPOINTS,
)
def test_get_with_sane_response(client, method_name, params):
    path = '/api/frontend/%s/' % method_name
    response = client.get(path, params)
    content = response.content.decode('utf-8')
    assert 'error' not in content
