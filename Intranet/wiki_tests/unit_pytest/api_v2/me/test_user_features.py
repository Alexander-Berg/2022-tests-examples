import pytest

from django.conf import settings
from django.test import override_settings
from waffle.testutils import override_switch

from wiki.api_core.waffle_switches import DEFAULT_NEW_UI
from wiki.api_v2.public.me.exceptions import UserFeatureChangeForbidden
from wiki.favorites_v2.dao import create_bookmark, get_or_create_folder
from wiki.favorites.models import Bookmark, Tag
from wiki.org import org_ctx
from wiki.pages.logic.subscription import create_watch
from wiki.subscriptions.models import Subscription
from wiki.users.consts import UserFeatureCode, UserFeatureStatus
from wiki.users.logic import features as user_features
from wiki.users.logic.settings import get_user_setting, set_user_setting
from wiki.users.models.user_feature import UserFeature

from intranet.wiki.tests.wiki_tests.common.utils import celery_eager

pytestmark = [pytest.mark.django_db]

ME_API_URL = '/api/v2/public/me'
DATA_UI_SETTING_NAME = 'data_ui_web'


def test_get_default_user_features(client, wiki_users):
    for default_true in [True, False]:
        status = UserFeatureStatus.ENABLED if default_true else UserFeatureStatus.DISABLED

        with override_switch(DEFAULT_NEW_UI, active=default_true):
            client.login(wiki_users.thasonic)
            response = client.get(f'{ME_API_URL}/features')
            assert response.status_code == 200

            response_data = response.json()['results']
            assert len(response_data) == 1
            assert response_data[0]['code'] == UserFeatureCode.DATA_UI_WEB
            assert response_data[0]['status'] == status

            client.login(wiki_users.robot_wiki)
            response = client.get(f'{ME_API_URL}/features')
            assert response.status_code == 200

            response_data = response.json()['results']
            assert len(response_data) == 1
            assert response_data[0]['code'] == UserFeatureCode.DATA_UI_WEB
            assert response_data[0]['status'] == UserFeatureStatus.DISABLED


def test_get_enabled_user_features(client, wiki_users):
    user_features.enable_feature(wiki_users.thasonic, UserFeatureCode.DATA_UI_WEB)
    client.login(wiki_users.thasonic)
    response = client.get(f'{ME_API_URL}/features')
    assert response.status_code == 200

    response_data = response.json()['results']
    assert len(response_data) == 1
    assert response_data[0]['code'] == UserFeatureCode.DATA_UI_WEB
    assert response_data[0]['status'] == UserFeatureStatus.PENDING_ENABLE


@override_switch(DEFAULT_NEW_UI, active=True)
def test_get_disabled_user_features(client, wiki_users):
    user_features.disable_feature(wiki_users.thasonic, UserFeatureCode.DATA_UI_WEB)
    client.login(wiki_users.thasonic)
    response = client.get(f'{ME_API_URL}/features')
    assert response.status_code == 200

    response_data = response.json()['results']
    assert len(response_data) == 1
    assert response_data[0]['code'] == UserFeatureCode.DATA_UI_WEB
    assert response_data[0]['status'] == UserFeatureStatus.PENDING_DISABLE


def test_enable_feature(client, wiki_users):
    with pytest.raises(UserFeature.DoesNotExist):
        UserFeature.objects.get(user=wiki_users.thasonic, feature_code=UserFeatureCode.DATA_UI_WEB)

    assert get_user_setting(wiki_users.thasonic, DATA_UI_SETTING_NAME) is False

    client.login(wiki_users.thasonic)
    response = client.post(f'{ME_API_URL}/enable_feature', {'feature_code': UserFeatureCode.DATA_UI_WEB})
    assert response.status_code == 200

    UserFeature.objects.get(
        user=wiki_users.thasonic, feature_code=UserFeatureCode.DATA_UI_WEB, status=UserFeatureStatus.PENDING_ENABLE
    )
    assert get_user_setting(wiki_users.thasonic, DATA_UI_SETTING_NAME) is True


def test_enable_unknown_feature(client, wiki_users):
    client.login(wiki_users.thasonic)
    response = client.post(f'{ME_API_URL}/enable_feature', {'feature_code': 'unknown'})
    assert response.status_code == 400

    response_data = response.json()
    assert response_data['error_code'] == 'VALIDATION_ERROR'


def test_disable_feature(client, wiki_users):
    feature = UserFeature.objects.create(
        user=wiki_users.thasonic, feature_code=UserFeatureCode.DATA_UI_WEB, status=UserFeatureStatus.ENABLED
    )
    set_user_setting(wiki_users.thasonic, DATA_UI_SETTING_NAME, True)
    client.login(wiki_users.thasonic)
    response = client.post(f'{ME_API_URL}/disable_feature', {'feature_code': UserFeatureCode.DATA_UI_WEB})
    assert response.status_code == 200

    feature.refresh_from_db()
    assert feature.status == UserFeatureStatus.PENDING_DISABLE
    assert get_user_setting(wiki_users.thasonic, DATA_UI_SETTING_NAME) is False


@celery_eager
def test_disable_not_enabled_feature(client, wiki_users):
    client.login(wiki_users.thasonic)
    assert UserFeature.objects.filter(user=client.user).exists() is False

    response = client.post(f'{ME_API_URL}/disable_feature', {'feature_code': UserFeatureCode.DATA_UI_WEB})
    assert response.status_code == 200
    assert UserFeature.objects.filter(user=client.user, status='DISABLED').exists() is True


def test_enable_feature__forbidden_enable_new_ui(client, wiki_users):
    client.login(wiki_users.thasonic)

    assert get_user_setting(wiki_users.thasonic, DATA_UI_SETTING_NAME) is False

    # forbidden
    with override_settings(FORBIDDEN_USERS_TO_CHANGE_UI=[wiki_users.thasonic.username]):
        response = client.post(f'{ME_API_URL}/enable_feature', {'feature_code': UserFeatureCode.DATA_UI_WEB})
        assert response.status_code == 403
        assert response.json()['error_code'] == UserFeatureChangeForbidden.error_code

    assert get_user_setting(wiki_users.thasonic, DATA_UI_SETTING_NAME) is False

    # enable
    with override_settings(FORBIDDEN_USERS_TO_CHANGE_UI=[]):
        response = client.post(f'{ME_API_URL}/enable_feature', {'feature_code': UserFeatureCode.DATA_UI_WEB})
        assert response.status_code == 200

    assert get_user_setting(wiki_users.thasonic, DATA_UI_SETTING_NAME) is True

    # robot forbidden
    client.login(wiki_users.robot_wiki)
    with override_settings(FORBIDDEN_USERS_TO_CHANGE_UI=[]):
        response = client.post(f'{ME_API_URL}/enable_feature', {'feature_code': UserFeatureCode.DATA_UI_WEB})
        assert response.status_code == 403
        assert response.json()['error_code'] == UserFeatureChangeForbidden.error_code

    assert get_user_setting(wiki_users.robot_wiki, DATA_UI_SETTING_NAME) is False


@celery_eager
def test_data_ui_feature(client, wiki_users, test_page, test_org, legacy_subscr_favor):
    user = wiki_users.thasonic

    create_watch(test_page, user, False)
    with org_ctx(test_org):
        folder = get_or_create_folder(user, 'test_folder')
        url = f'https://{settings.NGINX_HOST}/{test_page.supertag}'
        create_bookmark(folder, test_page.title, url)

    assert Bookmark.objects.count() == 0
    assert Tag.objects.count() == 0
    assert Subscription.objects.count() == 0

    client.login(user)
    response = client.post(f'{ME_API_URL}/enable_feature', {'feature_code': UserFeatureCode.DATA_UI_WEB})
    assert response.status_code == 200
    feature = UserFeature.objects.get(user=user, feature_code=UserFeatureCode.DATA_UI_WEB)
    assert feature.status == UserFeatureStatus.ENABLED

    bookmarks = Bookmark.objects.all()
    tags = Tag.objects.all()
    subscriptions = Subscription.objects.all()
    assert len(bookmarks) == 1
    assert len(tags) == 1
    assert len(subscriptions) == 1
    assert bookmarks[0].page_id == test_page.id
    assert tags[0].name == folder.name
    assert subscriptions[0].page == test_page
    assert subscriptions[0].user == user
