import pytest
from aiohttp.web import json_response
from smb.common.http_client import BadGateway, ServiceUnavailable, UnknownResponse

from maps_adv.common.aiosup import (
    BalancerError,
    Data,
    EmptyReceiverList,
    IncorrectPayload,
    IncorrectToken,
    InvalidData,
    InvalidNotification,
    Notification,
    PushAction,
    Receiver,
    SupClient,
    ThrottlePolicies,
)
from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio]


input_params = dict(
    receiver=[
        Receiver(uid="some_passport"),
        Receiver(tag="geo=='117611' && (app_id LIKE 'ru.yandex.searchplugin%')"),
    ],
    notification=Notification(title="some title", body="Good weather to everyone!"),
    data=Data(push_id="any push id", push_action=PushAction.MORDA),
    schedule=dt("2020-06-01 18:00:00"),
    adjust_time_zone=True,
    ttl=200,
    extra_param1="extra_value_1",
    extra_param2="extra_value_2",
    throttle_policies=ThrottlePolicies(
        install_id="install-policy",
        device_id="device-policy",
        content_id="content-policy",
    ),
)


@pytest.mark.parametrize("dry_run, dry_run_param", [(True, 1), (False, 0)])
async def test_sends_correct_request(dry_run, dry_run_param, mock_push, sup_client):
    request_data = dict()
    request_headers = dict()

    async def _handler(request):
        nonlocal request_data, request_headers
        request_data.update(dict(url=str(request.url)))
        request_data.update(dict(body=await request.json()))
        request_data.update(dict(content_type=request.content_type))
        request_data.update(dict(charset=request.charset))
        request_headers = dict(request.headers)
        return json_response(status=200, data=response_body)

    mock_push(_handler)

    await sup_client.send_push_notifications(dry_run=dry_run, **input_params)

    assert request_data == dict(
        url=f"http://sup.server/pushes?dry_run={dry_run_param}",
        body={
            "project": "any_project_name",
            "receiver": [
                "uid: some_passport",
                "tag: geo=='117611' && (app_id LIKE 'ru.yandex.searchplugin%')",
            ],
            "notification": dict(title="some title", body="Good weather to everyone!"),
            "data": dict(push_id="any push id", push_action="morda"),
            "schedule": "2020-06-01'T'18:00:00",
            "adjust_time_zone": True,
            "ttl": 200,
            "extra_param1": "extra_value_1",
            "extra_param2": "extra_value_2",
            "throttle_policies": {
                "install_id": "install-policy",
                "device_id": "device-policy",
                "content_id": "content-policy",
            },
        },
        content_type="application/json",
        charset="UTF-8",
    )
    assert request_headers.get("Authorization") == "OAuth some_token"


async def test_sends_now_if_schedule_not_passed(mock_push, sup_client):
    input_data = input_params.copy()
    input_data.update({"schedule": None})

    request_body = dict()

    async def _handler(request):
        nonlocal request_body
        request_body = await request.json()
        return json_response(status=200, data=response_body)

    mock_push(_handler)

    await sup_client.send_push_notifications(**input_data)

    assert request_body.get("schedule") == "now"


async def test_sends_default_throttle_policies_if_not_passed(mock_push, sup_client):
    input_data = input_params.copy()
    input_data.update({"throttle_policies": None})

    request_body = dict()

    async def _handler(request):
        nonlocal request_body
        request_body = await request.json()
        return json_response(status=200, data=response_body)

    mock_push(_handler)

    await sup_client.send_push_notifications(**input_data)

    assert request_body.get("throttle_policies") == {
        "install_id": "default-install-policy",
        "device_id": "default-device-policy",
        "content_id": "default-content-policy",
    }


async def test_sends_no_throttle_policies_if_it_not_set(mock_push):
    input_data = input_params.copy()
    input_data.update({"throttle_policies": None})

    request_body = dict()

    async def _handler(request):
        nonlocal request_body
        request_body = await request.json()
        return json_response(status=200, data=response_body)

    mock_push(_handler)

    async with SupClient(
        "http://sup.server",
        auth_token="some_token",
        project="any_project_name",
        throttle_policies=None,
    ) as client:
        await client.send_push_notifications(**input_data)

    assert request_body.get("throttle_policies") is None


async def test_does_not_send_data_if_data_not_passed(mock_push, sup_client):
    input_data = input_params.copy()
    input_data.update({"data": None})

    request_body = dict()

    async def _handler(request):
        nonlocal request_body
        request_body = await request.json()
        return json_response(status=200, data=response_body)

    mock_push(_handler)

    await sup_client.send_push_notifications(**input_data)

    assert "data" not in request_body


async def test_returns_push_request_result_data(mock_push, sup_client):
    mock_push(json_response(status=200, data=response_body))

    got = await sup_client.send_push_notifications(dry_run=True, **input_params)

    assert got == {
        "id": "1538131142619000-4568210906199431056",
        "receiver": ["login:861cae2a060f47c88e7dea0422cc9699", "uid:15678"],
        "strace": {
            "chunks": 1,
            "receivers": 1,
            "processingTime": [],
            "resolveTime": 0,
            "downloadTime": 18,
        },
    }


async def test_does_not_return_strace_if_dry_run_false(mock_push, sup_client):
    mock_push(json_response(status=200, data=response_body))

    got = await sup_client.send_push_notifications(dry_run=False, **input_params)

    assert got == {
        "id": "1538131142619000-4568210906199431056",
        "receiver": ["login:861cae2a060f47c88e7dea0422cc9699", "uid:15678"],
    }


@pytest.mark.parametrize("receiver", [None, []])
async def test_raises_for_empty_receiver_list(receiver, sup_client):
    input_data = input_params.copy()
    input_data.update({"receiver": receiver})

    with pytest.raises(EmptyReceiverList):
        await sup_client.send_push_notifications(**input_data)


@pytest.mark.parametrize("notification", [None, dict(some="notification")])
async def test_raises_if_invalid_notification_passed(notification, sup_client):
    input_data = input_params.copy()
    input_data.update({"notification": notification})

    with pytest.raises(InvalidNotification) as exc:
        await sup_client.send_push_notifications(**input_data)

    assert exc.value.args == ("Notification must be passed.",)


async def test_raises_if_invalid_data_passed(sup_client):
    input_data = input_params.copy()
    input_data.update({"data": dict(some="data")})

    with pytest.raises(InvalidData) as exc:
        await sup_client.send_push_notifications(**input_data)

    assert exc.value.args == ("Data param must be Data entity object.",)


@pytest.mark.parametrize(
    "status, exception", [(400, IncorrectPayload), (401, IncorrectToken)]
)
async def test_raises_for_known_error_responses(
    status, exception, mock_push, sup_client
):
    mock_push(json_response(status=status))

    with pytest.raises(exception):
        await sup_client.send_push_notifications(**input_params)


async def test_raises_for_unknown_response(mock_push, sup_client):
    mock_push(json_response(status=532))

    with pytest.raises(UnknownResponse):
        await sup_client.send_push_notifications(**input_params)


@pytest.mark.parametrize("status", (502, 503, 504))
async def test_retries_for_expected_statuses(status, mock_push, sup_client):
    mock_push(json_response(status=status))
    mock_push(json_response(status=200, data=response_body))

    await sup_client.send_push_notifications(**input_params)


@pytest.mark.parametrize(
    "response_status, expected_exception",
    [(502, BadGateway), (503, ServiceUnavailable), (504, BalancerError)],
)
async def test_raises_if_retrying_fails(
    response_status, expected_exception, mock_push, sup_client
):
    mock_push(json_response(status=response_status))
    mock_push(json_response(status=response_status))

    with pytest.raises(expected_exception):
        await sup_client.send_push_notifications(**input_params)


response_body = {
    "id": "1538131142619000-4568210906199431056",
    "receiver": ["login:861cae2a060f47c88e7dea0422cc9699", "uid:15678"],
    "ttl": 864,
    "schedule_date_time": "2018-05-14T17:11:56.611",
    "schedule_timezone": "Z",
    "data": {
        "any": "value",
        "push_id": "jHTvaxZcXw.382e62db-df52-432b-a9b9-f7453f3397d2",
    },
    "notification": {
        "title": "title",
        "body": "the body",
        "icon": "http://avatars.mds.yandex.net/yandex.png",
    },
    "project": "psuh",
    "max_expected_receivers": 2,
    "android_features": {
        "vibrationOn": True,
        "iconBackgroundColor": "black",
        "priority": 0,
        "excludeNotification": False,
        "silent": False,
        "ledType": 0,
        "led": "",
        "soundType": 0,
        "sound": "",
    },
    "ios_features": {"content_available": True, "mutable_content": False},
    "throttle_policies": {
        "INSTALL_ID": {"default_install_id": {"hourly": 2, "daily": 5}},
        "DEVICE_ID": {"default_device_id": {"hourly12": 5}},
    },
    "strace": {
        "receivers": 1,
        "chunks": 1,
        "processingTime": [],
        "resolveTime": 0,
        "downloadTime": 18,
    },
    "is_data_only": False,
    "transport": "Native",
    "spread_interval": 0,
    "priority": 2799,
    "requestTime": 0,
    "updatedAt": 1.526317916611e9,
}
