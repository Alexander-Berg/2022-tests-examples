import pytest
from django.conf import settings as django_settings
from django.test import override_settings
from mock import patch
from waffle.testutils import override_switch

from wiki.api_core.waffle_switches import DEFAULT_NEW_UI
from wiki.api_v2.public.me.settings_view import DEFAULT_SETTINGS_DICT, MAX_SETTINGS_SIZE, UntypedSettingsTooBig
from intranet.wiki.tests.wiki_tests.common.skips import only_intranet, only_biz

pytestmark = [pytest.mark.django_db]


@patch('wiki.api_v2.public.me.settings_view.staff')
@patch('wiki.api_v2.public.me.settings_view.set_user_lang_ui')
def test_get_set_settings(staff_mock, set_user_lang_ui_mock, client, wiki_users, page_cluster, organizations, groups):
    client.login(wiki_users.thasonic)
    settings = DEFAULT_SETTINGS_DICT.copy()
    settings['show_features_pages'] = django_settings.IS_INTRANET

    # reset all data
    response = client.post('/api/v2/public/me/settings', data=settings)
    assert response.status_code == 200
    assert_user_settings(client, settings)

    # change code_theme to idea
    patch = {'code_theme': 'idea'}
    response = client.post('/api/v2/public/me/settings', data=patch)
    assert response.status_code == 200
    settings.update(patch)
    assert_user_settings(client, settings)

    # change propose_content_translation to True
    patch = {'propose_content_translation': True}
    response = client.post('/api/v2/public/me/settings', data=patch)
    assert response.status_code == 200
    settings.update(patch)
    assert_user_settings(client, settings)

    # change show_features_pages
    patch = {'show_features_pages': not settings['show_features_pages']}
    response = client.post('/api/v2/public/me/settings', data=patch)
    assert response.status_code == 200
    assert_user_settings(client, settings)  # no change

    # change code_theme to bad value
    patch = {'code_theme': 'pycharm'}
    response = client.post('/api/v2/public/me/settings', data=patch)
    assert response.status_code == 400

    # broken settings must return defaults
    wiki_users.thasonic.profile = {'code_theme': 'BROKEN'}
    wiki_users.thasonic.save()

    assert_user_settings(client, DEFAULT_SETTINGS_DICT)


def _update_settings(client, patch, data):
    response = client.post('/api/v2/public/me/settings', data={'untyped_settings': patch})
    assert response.status_code == 200, response.json()

    response = client.get('/api/v2/public/me/settings')
    assert response.status_code == 200
    assert response.json()['untyped_settings'] == data


def test_set_untyped_settings(client, wiki_users, page_cluster, organizations, groups):
    client.login(wiki_users.thasonic)
    data = {'foo': 'bar', 'popup_shown': False}
    _update_settings(client, data, data)
    patch = {'popup_shown': True, 'new_one': 123}
    data.update(patch)
    _update_settings(client, patch, data)

    patch = {'exploit': 'A' * MAX_SETTINGS_SIZE}

    response = client.post('/api/v2/public/me/settings', data={'untyped_settings': patch})
    assert response.status_code == 400
    assert response.json()['error_code'] == UntypedSettingsTooBig.error_code


def assert_user_settings(client, s):
    response = client.get('/api/v2/public/me/settings')
    user_settings = response.json()

    for key, default in DEFAULT_SETTINGS_DICT.items():
        if key == 'language' and django_settings.IS_INTRANET:
            continue

        assert user_settings[key] == s[key]


@only_intranet
def test_disable_featured_pages__get__intranet(client, wiki_users, groups, add_user_to_group):
    with override_settings(DISABLE_FEATURED_PAGES_GROUP_IDS=[groups.root_group.id]):
        # 1. thasonic in root_group and side_group -> False
        user = wiki_users.thasonic
        add_user_to_group(groups.root_group, user=user)
        add_user_to_group(groups.side_group, user=user)

        response = client.login(user).get('/api/v2/public/me/settings')
        assert response.json()['show_features_pages'] is False, response.json()

        # 2. volozh in child_group -> False  (nested root_group)
        user = wiki_users.volozh
        add_user_to_group(groups.child_group, user=user)

        response = client.login(user).get('/api/v2/public/me/settings')
        assert response.json()['show_features_pages'] is False, response.json()

        # 2. asm in side_group -> True
        user = wiki_users.asm
        add_user_to_group(groups.side_group, user=user)

        response = client.login(user).get('/api/v2/public/me/settings')
        assert response.json()['show_features_pages'] is True, response.json()


@only_biz
def test_disable_featured_pages__get__biz(client, wiki_users, groups, add_user_to_group):
    """Biz - always False"""
    client.login(wiki_users.thasonic)

    response = client.get('/api/v2/public/me/settings')
    assert response.json()['show_features_pages'] is False, response.json()


def test_new_ui(client, wiki_users):
    client.login(wiki_users.thasonic)

    # get default value
    for default_true in [False, True]:
        with override_switch(DEFAULT_NEW_UI, active=default_true):
            response = client.get('/api/v2/public/me/settings')
            assert response.status_code == 200, response.json()
            assert response.json()['data_ui_web'] is default_true, response.json()

    # get user`s value
    for default_true in [False, True]:
        with override_switch(DEFAULT_NEW_UI, active=default_true):
            need_value = not default_true
            response = client.post('/api/v2/public/me/settings', data={'data_ui_web': need_value})
            assert response.status_code == 200, response.json()

            response = client.get('/api/v2/public/me/settings')
            assert response.status_code == 200, response.json()
            assert response.json()['data_ui_web'] is need_value, response.json()

    # robot - default False
    client.login(wiki_users.robot_wiki)
    for default_true in [False, True]:
        with override_switch(DEFAULT_NEW_UI, active=default_true):
            response = client.get('/api/v2/public/me/settings')
            assert response.status_code == 200, response.json()
            assert response.json()['data_ui_web'] is False, response.json()
