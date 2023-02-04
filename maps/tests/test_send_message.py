import pytest
from aiohttp import BasicAuth
from aiohttp.web import json_response

from maps_adv.common.email_sender import IncorrectParams

pytestmark = [pytest.mark.asyncio]


async def test_sends_correct_request(client, mock_api_transaction_send):
    request_path = None
    request_headers = None
    request_params = None

    async def handler(request):
        nonlocal request_path, request_headers, request_params
        request_path = request.path
        request_params = await request.json()
        request_headers = dict(request.headers)
        return json_response(status=200, data={})

    mock_api_transaction_send("template_code", handler)

    await client.send_message(
        args={"key": "value"},
        asynchronous=False,
        for_testing=True,
        template_code="template_code",
        to_email="cool-guy@yandex-team.ru",
        subject="Email subject",
        from_email="no-reply@yandex.ru",
        from_name="Анонимный отправитель",
    )

    assert request_path == "/api/0/kek-account/transactional/template_code/send"
    assert request_params == {
        "args": {"key": "value"},
        "async": False,
        "to_email": "cool-guy@yandex-team.ru",
        "for_testing": True,
        "from_email": "no-reply@yandex.ru",
        "from_name": "Анонимный отправитель",
    }
    assert request_headers.get("Content-Type") == "application/json"
    assert request_headers.get("Authorization") == BasicAuth("account-token").encode()


async def test_sends_async_by_default(client, mock_api_transaction_send):
    request_params = None

    async def handler(request):
        nonlocal request_params
        request_params = await request.json()
        return json_response(status=200, data={})

    mock_api_transaction_send("template_code", handler)

    await client.send_message(
        args={"key": "value"},
        to_email="cool-guy@yandex-team.ru",
        template_code="template_code",
        for_testing=True,
    )

    assert request_params["async"] is True


@pytest.mark.parametrize(
    "kw",
    (
        {
            "args": {},
            "for_testing": False,
            "subject": None,
            "from_email": None,
            "from_name": None,
        },
        {},
    ),
)
async def test_some_parameters_not_sent_if_not_passed_of_false(
    kw, client, mock_api_transaction_send
):
    request_params = None

    async def handler(request):
        nonlocal request_params
        request_params = await request.json()
        return json_response(status=200, data={})

    mock_api_transaction_send("template_code", handler)

    await client.send_message(
        asynchronous=False,
        template_code="template_code",
        to_email="cool-guy@yandex-team.ru",
        **kw,
    )

    assert "args" not in request_params
    assert "for_testing" not in request_params
    assert "headers" not in request_params
    assert "from_email" not in request_params
    assert "from_name" not in request_params


async def test_raises_if_from_name_passed_without_email(client):
    with pytest.raises(IncorrectParams):
        await client.send_message(
            asynchronous=False,
            template_code="template_code",
            to_email="cool-guy@yandex-team.ru",
            from_name="Имя от",
        )
