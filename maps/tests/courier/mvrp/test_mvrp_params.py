import copy
import dateutil.tz
import pytest
from datetime import datetime, timedelta
from http import HTTPStatus
from freezegun import freeze_time

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.env.mvrp_solver_mock import solver_request_by_task_id
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_post, local_get, local_patch
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import add_user, set_company_setting
from ya_courier_backend.models import db, UserRole, SharedVrpTask


TIME_ZONE = dateutil.tz.gettz("Europe/Moscow")
TEST_DATETIME = datetime(2019, 12, 13, 1, 1, 1, tzinfo=TIME_ZONE)
DEFAULT_EXPIRATION_TIME = TEST_DATETIME + timedelta(days=7)


@skip_if_remote
def test_custom_params_are_set_up_from_the_company(env: Environment):
    set_company_setting(env, env.default_company.id, "tasks_bucket", "some-custom-bucket")
    set_company_setting(env, env.default_shared_company.id, "apikey", "some-custom-apikey")

    path_post = "/api/v1/vrs/add/mvrp"
    get_urls = [
        "/api/v1/vrs/request/mvrp/mock_task_uuid__generic",
        "/api/v1/vrs/result/mvrp/mock_task_uuid__generic",
    ]
    data = solver_request_by_task_id["mock_task_uuid__generic"]

    result = local_post(env.client, path_post, data=data, headers=env.user_auth_headers, expected_status=HTTPStatus.ACCEPTED)
    assert result["mock_request_args"] == {"apikey": env.default_company.apikey, "bucket": "some-custom-bucket"}
    for path_get in get_urls:
        result = local_get(env.client, path_get, headers=env.user_auth_headers)
        assert result["mock_request_args"] == {"bucket": "some-custom-bucket"}

    _, user_auth = add_user(env, "test_shared_user", UserRole.dispatcher, env.default_shared_company.id)
    result = local_post(env.client, path_post, data=data, headers=user_auth, expected_status=HTTPStatus.BAD_REQUEST)
    assert result["mock_request_args"] == {"apikey": "some-custom-apikey"}
    for path_get in get_urls:
        result = local_get(env.client, path_get, headers=user_auth)
        assert result["mock_request_args"] == {}, path_get


@skip_if_remote
@pytest.mark.parametrize(
    "path_get",
    [
        "/vrs/api/v1/result/mvrp/",
        "/vrs/api/v1/result/svrp/",
        "/vrs/api/v1/log/response/",
        "/vrs/api/v1/log/request/",
    ],
)
def test_custom_params_are_forwarded_for_vrs_routes(env: Environment, path_get):
    path_get += "mock_task_uuid__generic?bucket=some-custom-bucket"
    result = local_get(env.client, path_get)
    assert result["mock_request_args"] == {"bucket": "some-custom-bucket"}


def test_valid_task_id(env: Environment):
    invalid_task_id = 'some_id@'
    path_import = f"/api/v1/vrs/result/mvrp/{invalid_task_id}"
    local_get(env.client, path_import, headers=env.user_auth_headers, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?initial-status=new&task_id={invalid_task_id}"
    local_post(env.client, path_import, headers=env.user_auth_headers, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
@pytest.mark.parametrize(
    "path_get",
    [
        "/vrs/api/v1/result/mvrp/",
        "/vrs/api/v1/result/svrp/",
        "/vrs/api/v1/log/request/",
        "/vrs/api/v1/log/response/",
        "/api/v1/vrs/result/mvrp/",
        "/api/v1/vrs/request/mvrp/",
    ],
)
def test_custom_params_are_set_up_from_the_company_that_shared(env: Environment, path_get):
    set_company_setting(env, env.default_company.id, "tasks_bucket", "some-custom-bucket")
    path = "/api/v1/vrs/shared/mvrp/mock_task_uuid__generic"
    res = local_post(env.client, path, headers=env.user_auth_headers)

    path_get += res["shared_id"]
    result = local_get(env.client, path_get, headers=env.user_auth_headers)

    assert result["mock_request_args"] == {"bucket": "some-custom-bucket"}


@skip_if_remote
def test_mvrp_task_import_works_for_shared_task(env: Environment):
    path = "/api/v1/vrs/shared/mvrp/mock_task_uuid__generic"
    res = local_post(env.client, path, headers=env.user_auth_headers)

    path_import = f"/api/v1/companies/{env.default_company.id}/mvrp_task?task_id={res['shared_id']}"
    local_post(env.client, path_import, headers=env.user_auth_headers, expected_status=HTTPStatus.OK)


@skip_if_remote
def test_no_apikey_results_in_unprocessable_entity(env: Environment):
    set_company_setting(env, env.default_company.id, "apikey", None)

    path = "/api/v1/vrs/add/mvrp"
    data = solver_request_by_task_id["mock_task_uuid__generic"]
    local_post(
        env.client, path, data=data, headers=env.user_auth_headers, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY
    )


@skip_if_remote
@freeze_time(TEST_DATETIME)
@pytest.mark.parametrize(
    "task_id",
    [
        "mock_task_uuid__generic",
        "mock_task_uuid__queued_task",
    ],
)
def test_shared_vrp_task_record_is_created_after_sharing(env: Environment, task_id):
    path = f"/api/v1/vrs/shared/mvrp/{task_id}"
    res = local_post(env.client, path, headers=env.user_auth_headers)
    assert "shared_id" in res
    assert res["expires_at"] == {
        "value": int(DEFAULT_EXPIRATION_TIME.timestamp()),
        "text": DEFAULT_EXPIRATION_TIME.astimezone(dateutil.tz.tzutc()).isoformat(),
    }

    with env.flask_app.app_context():
        actual_tasks = [x.as_dict() for x in db.session.query(SharedVrpTask).all()]
        expected_tasks = [
            {
                "id": actual_tasks[0]["id"],
                "shared_id": actual_tasks[0]["shared_id"],
                "task_id": task_id,
                "company_id": env.default_company.id,
                "who_shared": env.default_user.login,
                "expires_at": DEFAULT_EXPIRATION_TIME.timestamp(),
            }
        ]
        assert actual_tasks == expected_tasks


@skip_if_remote
def test_shared_vrp_is_not_created_if_task_does_not_exist(env: Environment):
    path = "/api/v1/vrs/shared/mvrp/non_existing_task"
    local_post(env.client, path, headers=env.user_auth_headers, expected_status=HTTPStatus.GONE)
    with env.flask_app.app_context():
        actual_tasks = [x.as_dict() for x in db.session.query(SharedVrpTask).all()]
        assert actual_tasks == []


@skip_if_remote
def test_shared_vrp_is_not_created_if_internal_error_happens(env: Environment):
    path = "/api/v1/vrs/shared/mvrp/mock_task_uuid__service_unavailable"
    local_post(env.client, path, headers=env.user_auth_headers, expected_status=HTTPStatus.INTERNAL_SERVER_ERROR)
    with env.flask_app.app_context():
        actual_tasks = [x.as_dict() for x in db.session.query(SharedVrpTask).all()]
        assert actual_tasks == []


@skip_if_remote
@pytest.mark.parametrize("delay_h", [1, 3])
@pytest.mark.parametrize(
    "path_get_result",
    [
        "/vrs/api/v1/result/mvrp/",
        "/vrs/api/v1/result/svrp/",
        "/vrs/api/v1/log/response/",
    ],
)
def test_shared_vrp_is_expiring_after_company_delay_setting(env: Environment, delay_h, path_get_result):
    path_company = f"/api/v1/companies/{env.default_company.id}"
    patch_data = {"shared_task_expiration_delay_h": delay_h}
    local_patch(env.client, path_company, data=patch_data, headers=env.user_auth_headers)

    with freeze_time(TEST_DATETIME) as freezed_time:
        task_id = "mock_task_uuid__generic"

        path = f"/api/v1/vrs/shared/mvrp/{task_id}"
        shared_id = local_post(env.client, path, headers=env.user_auth_headers)["shared_id"]

        freezed_time.tick(delta=timedelta(hours=delay_h))

        local_get(env.client, f"/vrs/api/v1/log/request/{task_id}", expected_status=HTTPStatus.OK)
        mvrp_result = local_get(env.client, f"{path_get_result}{task_id}")
        mvrp_shared_result = local_get(env.client, f"{path_get_result}{shared_id}")

        assert mvrp_result == mvrp_shared_result

        freezed_time.tick(delta=timedelta(seconds=1))

        local_get(env.client, f"/vrs/api/v1/log/request/{task_id}", expected_status=HTTPStatus.OK)
        local_get(env.client, f"{path_get_result}{task_id}", expected_status=HTTPStatus.OK)
        local_get(env.client, f"{path_get_result}{shared_id}", expected_status=HTTPStatus.GONE)


@skip_if_remote
@freeze_time(TEST_DATETIME)
@pytest.mark.parametrize("custom_expiration", [1629288303, None])
def test_shared_expiration_time_can_be_configured_by_superusers(env: Environment, custom_expiration):
    data = {"expires_at": custom_expiration}

    path = "/api/v1/vrs/shared/mvrp/mock_task_uuid__generic"
    res = local_post(env.client, path, data=data, headers=env.user_auth_headers)
    assert res["expires_at"]["value"] == DEFAULT_EXPIRATION_TIME.timestamp()

    res = local_post(env.client, path, headers=env.superuser_auth_headers)
    assert res["expires_at"]["value"] == DEFAULT_EXPIRATION_TIME.timestamp()

    res = local_post(env.client, path, data=data, headers=env.superuser_auth_headers)
    assert res["expires_at"] == custom_expiration or res["expires_at"]["value"] == custom_expiration

    with freeze_time(DEFAULT_EXPIRATION_TIME + timedelta(days=1)):
        local_get(env.client, "/vrs/api/v1/log/request/mock_task_uuid__generic", expected_status=HTTPStatus.OK)
        mvrp_result = local_get(env.client, "/vrs/api/v1/result/mvrp/mock_task_uuid__generic")
        mvrp_shared_result = local_get(env.client, f"/vrs/api/v1/result/mvrp/{res['shared_id']}")

        assert mvrp_result == mvrp_shared_result


@skip_if_remote
@pytest.mark.parametrize("is_super", [True, False])
@pytest.mark.parametrize("company_id", ["none", "own", "another"])
def test_other_company_zone(env: Environment, is_super, company_id):
    path_zones = f"/api/v1/reference-book/companies/{env.default_another_company.id}/zones"
    zone = {
        "number": "test_company_zone",
        "color_fill": "#010101",
        "color_edge": "#020202",
        "polygon": {
            "coordinates": [[[0, 0], [0, 1], [1, 1], [1, 0], [0, 0]]],
            "type": "Polygon"
        },
    }
    local_post(env.client, path_zones, headers=env.superuser_auth_headers, data=[zone])

    if company_id == "another":
        path_mvrp = f"/api/v1/vrs/add/mvrp?company_id={env.default_another_company.id}"
        if is_super:
            status = HTTPStatus.ACCEPTED
        else:
            status = HTTPStatus.FORBIDDEN
    elif company_id == "own":
        path_mvrp = f"/api/v1/vrs/add/mvrp?company_id={env.default_company.id}"
        status = HTTPStatus.UNPROCESSABLE_ENTITY
    else:
        path_mvrp = "/api/v1/vrs/add/mvrp"
        status = HTTPStatus.UNPROCESSABLE_ENTITY

    if is_super:
        headers = env.superuser_auth_headers
    else:
        headers = env.user_auth_headers

    data = copy.deepcopy(solver_request_by_task_id["mock_task_uuid__generic"])
    data["vehicles"][0]["allowed_zones"] = ["test_company_zone"]
    local_post(env.client, path_mvrp, data=data, headers=headers, expected_status=status)


@skip_if_remote
def test_json_bom_content_from_solver_is_parsed(env: Environment):
    get_urls = [
        "/api/v1/vrs/request/mvrp/mock_task_uuid__json_bom",
        "/api/v1/vrs/result/mvrp/mock_task_uuid__json_bom",
    ]
    for path_get in get_urls:
        local_get(env.client, path_get, headers=env.user_auth_headers, expected_status=HTTPStatus.OK)


@pytest.mark.parametrize(
    ('method', 'path', 'params', 'data', 'lang', 'response'),
    [
        ('GET', '/api/v1/vrs/request/mvrp/mock_task_uuid__json_bom', {}, {}, 'ru_RU',
         {'id': 'айди', 'message': 'Сообщение'}),
        ('GET', '/api/v1/vrs/children', {'parent_task_id': 'mock_task_uuid__json_bom'}, {}, 'ru_RU',
         {'id': 'айди', 'message': 'Сообщение'}),
        ('GET', '/api/v1/vrs/result/mvrp/mock_task_uuid__json_bom', {}, {}, 'ru_RU',
         {'id': 'айди', 'message': 'Сообщение'}),
        ('GET', '/api/v1/vrs/result/svrp/mock_task_uuid__json_bom', {}, {}, 'ru_RU',
         {'id': 'айди', 'message': 'Сообщение'}),
        ('POST', '/api/v1/vrs/add/mvrp', {'company_id': 1},
         copy.deepcopy(solver_request_by_task_id["mock_task_uuid__generic"]), 'ru_RU',
         {'id': 'айди', 'message': 'Сообщение'}),
        ('GET', '/vrs/api/v1/result/mvrp/mock_task_uuid__json_bom', {}, {}, 'ru_RU',
         {'id': 'айди', 'message': 'Сообщение'}),
        ('GET', '/vrs/api/v1/log/request/mock_task_uuid__json_bom', {}, {}, 'ru_RU',
         {'id': 'айди', 'message': 'Сообщение'}),
        ('GET', '/api/v1/vrs/request/mvrp/mock_task_uuid__json_bom', {}, {}, 'some_LANG',
         {'id': 'id', 'message': 'Message'}),
    ],
)
def test_mvrp_lang(env: Environment, method, path, params, data, lang, response):
    params.update({'lang': lang})
    match method:
        case 'GET':
            assert local_get(env.client, path, query=params, headers=env.user_auth_headers) == response
        case 'POST':
            assert local_post(env.client, path, query=params, data=data, headers=env.user_auth_headers) == response
        case _:
            assert False


@skip_if_remote
def test_cant_get_svrp_by_mvrp_handler(env):
    path_mvrp = '/api/v1/vrs/result/mvrp/mock_task_uuid__svrp_generic'
    local_get(env.client, path_mvrp, headers=env.user_auth_headers, expected_status=HTTPStatus.NOT_FOUND)
