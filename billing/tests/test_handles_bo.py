from unittest import mock
import pytest
import webtest

from .utils import (
    FakeReadPreferenceSettings,
    FakeServiceStartrekClient,
)


@pytest.fixture
def web_app():
    with mock.patch('billing.apikeys.apikeys.mapper.context.ReadPreferenceSettings', new=FakeReadPreferenceSettings):
        with mock.patch('billing.apikeys.apikeys.startrek_wrapper.ServiceStartrekClient', new=FakeServiceStartrekClient):
            from billing.apikeys.apikeys.handles_bo import logic
            return webtest.TestApp(logic)


def test_project_service_link_create_user_has_no_permissions(mongomock, web_app, simple_link):
    res = web_app.put_json(
        '/service/{}/project_link/{}'.format(simple_link.service_id, simple_link.project_id),
        headers={'X-User-Id': '{}'.format(simple_link.project.user.uid)},
        status='4*',
    )

    assert res.status_int == 403


def test_project_service_link_create_user_has_read_permissions(mongomock, web_app, simple_link, user_support_ro):
    res = web_app.put_json(
        '/service/{}/project_link/{}'.format(simple_link.service_id, simple_link.project_id),
        headers={'X-User-Id': '{}'.format(user_support_ro.uid)},
        status='4*',
    )

    assert res.status_int == 403


def test_project_service_link_create_admin_has_write_permissions(mongomock, web_app, simple_link, user_manager):
    res = web_app.put_json(
        '/service/{}/project_link/{}'.format(simple_link.service_id, simple_link.project_id),
        headers={'X-User-Id': '{}'.format(user_manager.uid)},
    )

    assert res.status_int == 200


def test_get_simple_user_info(mongomock, web_app, user, user_manager):
    res = web_app.get(
        '/user/{}'.format(user.uid),
        headers={'X-User-Id': '{}'.format(user_manager.uid)},
    )

    assert res.status_int == 200
    assert res.json['data']['uid'] == user.uid


def test_set_project_slots(mongomock, web_app, user, user_manager):
    res = web_app.get(
        '/user/{}'.format(user.uid),
        headers={'X-User-Id': '{}'.format(user_manager.uid)},
    )

    assert res.status_int == 200
    assert res.json['data']['n_project_slots'] == 1

    res = web_app.patch_json(
        '/user/{}'.format(user.uid),
        {'n_project_slots': 123},
        headers={'X-User-Id': '{}'.format(user_manager.uid)},
    )

    assert res.status_int in [302, 303]

    res = web_app.get(
        '/user/{}'.format(user.uid),
        headers={'X-User-Id': '{}'.format(user_manager.uid)},
    )

    assert res.json['data']['n_project_slots'] == 123
