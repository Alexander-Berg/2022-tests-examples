"""Tests preorder adding API."""

import pytest
import http.client

from infra.walle.server.tests.lib.util import BOT_PROJECT_ID, TestCase, patch
from walle import restrictions
from walle.clients import abc, bot, staff
from walle.preorders import PREORDER_DISPENSER_BOTS
from walle.util.misc import drop_none


@pytest.fixture
def test(mp, request):
    mp.function(staff.check_login, side_effect=lambda login, *args, **kwargs: login)
    return TestCase.create(request)


def test_unauthenticated(test, unauthenticated):
    result = test.api_client.post("/v1/preorders", data={"id": 1, "project": "id"})
    assert result.status_code == http.client.UNAUTHORIZED
    test.preorders.assert_equal()


def test_unauthorized_by_project(test):
    project = test.mock_project(dict(id="some-id", owners=[]))
    result = test.api_client.post("/v1/preorders", data={"id": 1, "project": project.id})
    assert result.status_code == http.client.FORBIDDEN
    assert result.json[
        "message"
    ] == "Authorization failure: You must be '{}' project owner to perform this request.".format(project.id)
    test.preorders.assert_equal()


def test_invalid_owner(mp, request):
    mp.function(bot.get_preorder_info, return_value={"owner": "invalid-user", "bot_project_id": BOT_PROJECT_ID})
    mock_check_login = mp.function(staff.check_login, side_effect=staff.InvalidLoginError("invalid-user"))
    test = TestCase.create(request)

    project = test.mock_project(dict(id="some-id", owners=[test.api_user]))
    result = test.api_client.post("/v1/preorders", data={"id": 1, "project": project.id})
    assert result.status_code == http.client.CONFLICT
    assert result.json["message"] == (
        "Can't add the preorder to Wall-E: it has an invalid owner '{}'.".format("invalid-user")
    )

    mock_check_login.assert_called_once_with("invalid-user", allow_dismissed=True)
    test.preorders.assert_equal()


@patch(
    "walle.clients.bot.get_preorder_info", return_value={"owner": "some-other-user", "bot_project_id": BOT_PROJECT_ID}
)
def test_unauthorized_by_preorder(get_preorder_info, test):
    project = test.mock_project(dict(id="some-id", owners=[test.api_user]))
    result = test.api_client.post("/v1/preorders", data={"id": 1, "project": project.id})
    assert result.status_code == http.client.FORBIDDEN
    assert result.json["message"] == "Authorization failure: You must be owner of #1 preorder to perform this request."
    get_preorder_info.assert_called_once_with(1)
    test.preorders.assert_equal()


@pytest.mark.parametrize(
    "slug,code",
    (
        ("hardware_management", "hardware_resources_owner"),
        ("services_management", "product_head"),
        ("hardware_management", "hardware_resources_manager"),
    ),
)
@patch(
    "walle.clients.bot.get_preorder_info",
    return_value={"owner": PREORDER_DISPENSER_BOTS[0], "bot_project_id": BOT_PROJECT_ID},
)
def test_authorized_by_abc(get_preorder_info, test, mp, monkeypatch_locks, slug, code):
    mp.function(
        abc.get_service_members,
        return_value=[
            {"person": {"login": TestCase.api_user, "is_robot": False}, "role": {"scope": {"slug": slug}, "code": code}}
        ],
    )
    mp.function(bot.get_planner_id_by_bot_project_id, return_value=11111)
    project = test.mock_project(dict(id="some-id", owners=[test.api_user]))
    preorder = test.preorders.mock(
        dict(
            id=1,
            owner=PREORDER_DISPENSER_BOTS[0],
            project=project.id,
            prepare=False,
            processed=False,
            bot_project=BOT_PROJECT_ID,
            audit_log_id=None,
        ),
        save=False,
    )

    result = test.api_client.post("/v1/preorders", data={"id": preorder.id, "project": project.id})
    assert result.status_code == http.client.CREATED
    assert result.json == preorder.to_api_obj()

    get_preorder_info.assert_called_once_with(1)
    test.preorders.assert_equal()


@patch(
    "walle.clients.bot.get_preorder_info",
    return_value={"owner": PREORDER_DISPENSER_BOTS[0], "bot_project_id": BOT_PROJECT_ID},
)
def test_unauthorized_by_abc(get_preorder_info, test, mp, monkeypatch_locks):
    mp.function(
        abc.get_service_members,
        return_value=[
            {
                "person": {"login": TestCase.api_user, "is_robot": False},
                "role": {"scope": {"slug": "hardware_management"}, "code": "some-unknown-code"},
            }
        ],
    )
    mp.function(bot.get_planner_id_by_bot_project_id, return_value=11111)
    project = test.mock_project(dict(id="some-id", owners=[test.api_user]))
    preorder = test.preorders.mock(
        dict(
            id=1,
            owner=PREORDER_DISPENSER_BOTS[0],
            project=project.id,
            prepare=False,
            processed=False,
            bot_project=BOT_PROJECT_ID,
            audit_log_id=None,
        ),
        save=False,
    )

    result = test.api_client.post("/v1/preorders", data={"id": preorder.id, "project": project.id})
    assert result.status_code == http.client.FORBIDDEN

    get_preorder_info.assert_called_once_with(1)


@patch(
    "walle.clients.bot.get_preorder_info",
    return_value={"owner": PREORDER_DISPENSER_BOTS[0], "bot_project_id": BOT_PROJECT_ID},
)
def test_unauthorized_by_abc_with_sudo(get_preorder_info, test, mp, monkeypatch_locks):
    mp.config("authorization.admins", [test.api_user])
    mp.function(
        abc.get_service_members,
        return_value=[
            {
                "person": {"login": TestCase.api_user, "is_robot": False},
                "role": {"scope": {"slug": "hardware_management"}, "code": "hardware_resources_manager"},
            }
        ],
    )
    mp.function(bot.get_planner_id_by_bot_project_id, return_value=11111)
    project = test.mock_project(dict(id="some-id", owners=[test.api_user]))
    preorder = test.preorders.mock(
        dict(
            id=1,
            owner=PREORDER_DISPENSER_BOTS[0],
            project=project.id,
            prepare=False,
            processed=False,
            bot_project=BOT_PROJECT_ID,
            audit_log_id=None,
        ),
        save=False,
    )

    result = test.api_client.post("/v1/preorders?sudo=1", data={"id": preorder.id, "project": project.id})
    assert result.status_code == http.client.CREATED
    assert result.json == preorder.to_api_obj()

    get_preorder_info.assert_called_once_with(1)
    test.preorders.assert_equal()


@patch(
    "walle.clients.bot.get_preorder_info", return_value={"owner": TestCase.api_user, "bot_project_id": BOT_PROJECT_ID}
)
def test_add(get_preorder_info, test, monkeypatch_locks):
    project = test.mock_project(dict(id="some-id", owners=[test.api_user]))
    preorder = test.preorders.mock(
        dict(
            id=1,
            owner=test.api_user,
            project=project.id,
            prepare=False,
            processed=False,
            bot_project=BOT_PROJECT_ID,
            audit_log_id=None,
        ),
        save=False,
    )

    result = test.api_client.post("/v1/preorders", data={"id": preorder.id, "project": project.id})
    assert result.status_code == http.client.CREATED
    assert result.json == preorder.to_api_obj()

    get_preorder_info.assert_called_once_with(1)
    test.preorders.assert_equal()


@patch(
    "walle.clients.bot.get_preorder_info", return_value={"owner": TestCase.api_user, "bot_project_id": BOT_PROJECT_ID}
)
def test_add_with_prepare(get_preorder_info, test, monkeypatch_locks):
    project = test.mock_project(dict(id="some-id", owners=[test.api_user]))
    preorder = test.preorders.mock(
        dict(
            id=1,
            owner=test.api_user,
            project=project.id,
            processed=False,
            bot_project=BOT_PROJECT_ID,
            audit_log_id=None,
            prepare=True,
        ),
        save=False,
    )

    result = test.api_client.post("/v1/preorders", data={"id": preorder.id, "project": project.id, "prepare": True})
    assert result.status_code == http.client.CREATED
    assert result.json == preorder.to_api_obj()

    get_preorder_info.assert_called_once_with(1)
    test.preorders.assert_equal()


@pytest.mark.parametrize(
    "request_provisioner,preorder_provisioner",
    (
        (None, TestCase.project_provisioner),
        (TestCase.host_provisioner, TestCase.host_provisioner),
    ),
)
@patch(
    "walle.clients.bot.get_preorder_info", return_value={"owner": TestCase.api_user, "bot_project_id": BOT_PROJECT_ID}
)
@patch("walle.clients.inventory.check_deploy_configuration")
def test_add_with_custom_prepare(
    check_deploy_config, get_preorder_info, test, monkeypatch_locks, request_provisioner, preorder_provisioner
):
    project = test.mock_project(dict(id="some-id", owners=[test.api_user]))
    preorder = test.preorders.mock(
        dict(
            id=1,
            owner=test.api_user,
            project=project.id,
            processed=False,
            bot_project=BOT_PROJECT_ID,
            audit_log_id=None,
            prepare=True,
            provisioner=preorder_provisioner,
            deploy_config=test.host_deploy_config,
            restrictions=[restrictions.AUTOMATION],
        ),
        save=False,
    )

    result = test.api_client.post(
        "/v1/preorders",
        data=drop_none(
            {
                "id": preorder.id,
                "project": project.id,
                "prepare": True,
                "provisioner": request_provisioner,
                "deploy_config": preorder.deploy_config,
                "restrictions": preorder.restrictions,
            }
        ),
    )
    assert result.status_code == http.client.CREATED
    assert result.json == preorder.to_api_obj()

    get_preorder_info.assert_called_once_with(1)
    check_deploy_config.assert_called_once_with(preorder.provisioner, preorder.deploy_config, None)
    test.preorders.assert_equal()


@patch("walle.clients.bot.get_preorder_info", side_effect=bot.InvalidPreorderIdError(1))
def test_add_invalid(get_preorder_info, test):
    project = test.mock_project(dict(id="some-id", owners=[test.api_user]))

    result = test.api_client.post("/v1/preorders", data={"id": 1, "project": project.id})
    assert result.status_code == http.client.NOT_FOUND
    assert result.json["message"] == "Preorder #1 doesn't exist in BOT."

    get_preorder_info.assert_called_once_with(1)
    test.preorders.assert_equal()


def test_add_invalid_project(test):
    result = test.api_client.post("/v1/preorders", data={"id": 1, "project": "some-invalid-id"})
    assert result.status_code == http.client.BAD_REQUEST
    assert result.json["message"] == "The specified project ID doesn't exist."
    test.preorders.assert_equal()
