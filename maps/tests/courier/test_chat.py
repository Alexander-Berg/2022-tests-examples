from http import HTTPStatus

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import Environment, local_post, local_patch, local_delete, local_get
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import add_user

from ya_courier_backend.models import UserRole


CHAT_DATA = {"chat_id": "Chat id from Yandex.Messenger"}
NEW_CHAT_DATA = {"chat_id": "New chat id from Yandex.Messenger"}


def _get_path(company_id, courier_id):
    return f"/api/v1/companies/{company_id}/couriers/{courier_id}/chats"


def _get_path_with_id(company_id, courier_id, chat_id):
    return f"/api/v1/companies/{company_id}/couriers/{courier_id}/chats/{chat_id}"


def _get_path_with_courier_ids(company_id, courier_ids):
    courier_ids_str = ','.join(str(courier_ids) for courier_ids in courier_ids)
    return f"/api/v1/companies/{company_id}/chats?courier_ids={courier_ids_str}"


@skip_if_remote
def test_chat(env: Environment):
    app_id, app_auth = add_user(env, "app", UserRole.app)

    chat = local_post(env.client, _get_path(env.default_company.id, env.default_courier.id), headers=app_auth,
                      data=CHAT_DATA, expected_status=HTTPStatus.OK)
    assert {"id", "chat_id"} == set(chat.keys())

    chats = local_get(env.client, _get_path(env.default_company.id, env.default_courier.id), headers=app_auth,
                      expected_status=HTTPStatus.OK)
    assert chat in chats

    resp = local_get(env.client, _get_path_with_id(env.default_company.id, env.default_courier.id, chat["id"]),
                     headers=app_auth, expected_status=HTTPStatus.OK)
    assert chat == resp

    resp = local_get(env.client, _get_path_with_courier_ids(env.default_company.id, [env.default_courier.id]),
                     headers=app_auth, expected_status=HTTPStatus.OK)
    assert [{**chat, 'courier_id': env.default_courier.id}] == resp

    unknown_courier_id = env.default_courier.id + 1000
    resp = local_get(env.client,
                     _get_path_with_courier_ids(env.default_company.id, [env.default_courier.id, unknown_courier_id]),
                     headers=app_auth, expected_status=HTTPStatus.OK)
    assert [{**chat, 'courier_id': env.default_courier.id}] == resp

    resp = local_patch(env.client, _get_path_with_id(env.default_company.id, env.default_courier.id, chat["id"]),
                       headers=app_auth, data=NEW_CHAT_DATA, expected_status=HTTPStatus.OK)
    chat["chat_id"] = NEW_CHAT_DATA["chat_id"]
    assert chat == resp

    resp = local_delete(env.client, _get_path_with_id(env.default_company.id, env.default_courier.id, chat["id"]),
                        headers=app_auth, expected_status=HTTPStatus.OK)
    assert chat == resp

    chats = local_get(env.client, _get_path(env.default_company.id, env.default_courier.id), headers=app_auth,
                      expected_status=HTTPStatus.OK)
    assert chats == []


@skip_if_remote
def test_cannot_add_two_chats(env: Environment):
    app_id, app_auth = add_user(env, "app", UserRole.app)

    local_post(env.client, _get_path(env.default_company.id, env.default_courier.id), headers=app_auth, data=CHAT_DATA,
               expected_status=HTTPStatus.OK)
    local_post(env.client, _get_path(env.default_company.id, env.default_courier.id), headers=app_auth, data=CHAT_DATA,
               expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_patch_extra_fields(env: Environment):
    app_id, app_auth = add_user(env, "app", UserRole.app)
    invalid_patch_data = {"id": 123}

    chat = local_post(env.client, _get_path(env.default_company.id, env.default_courier.id), headers=app_auth,
                      data=CHAT_DATA, expected_status=HTTPStatus.OK)
    local_patch(env.client, _get_path_with_id(env.default_company.id, env.default_courier.id, chat["id"]),
                headers=app_auth, data=invalid_patch_data, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
