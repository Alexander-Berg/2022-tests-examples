from datetime import datetime, timedelta, timezone

import aiohttp
import pytest
from aiohttp import web

from maps_adv.common.email_sender import IncorrectParams, MailingListSource
from maps_adv.common.helpers import dt

pytestmark = [pytest.mark.asyncio]

input_params = dict(
    title="Test campaign",
    subject="Subject of letter",
    from_email="sender@yandex.ru",
    from_name="Yandex.Maps",
    body="letter HTML",
    mailing_list_source=MailingListSource.IN_PLACE,
    mailing_list_params=[{"email": "ivan@yandex.ru"}],
    schedule_dt=dt("2020-01-01 00:00:01"),
    unsubscribe_list_slug="unsubscribe-list",
    tags=("tag_1", "tags_2"),
)


def make_input_params(**overrides):
    params = input_params.copy()
    params.update(**overrides)

    return params


success_response = {"id": 111, "slug": "campaign-slug", "campaign_is_scheduled": True}


async def test_sends_correct_request(client, mock_sender_api):
    request_path = None
    request_headers = None
    request_params = None

    async def sender_handler(request):
        nonlocal request_path, request_headers, request_params
        request_path = request.path
        request_params = await request.json()
        request_headers = dict(request.headers)
        return web.json_response(status=200, data=success_response)

    mock_sender_api(sender_handler)

    await client.schedule_promo_campaign(**input_params)

    assert request_path == "/api/0/kek-account/automation/promoletter"
    assert request_params == {
        "title": "Test campaign",
        "subject": "Subject of letter",
        "from_addr": {"name": "Yandex.Maps", "email": "sender@yandex.ru"},
        "letter_body": "letter HTML",
        "segment": {
            "template": "singleuselist",
            "params": {"recipients": [{"email": "ivan@yandex.ru"}]},
        },
        "schedule_time": "2020-01-01T00:00:01+0000",
        "unsubscribe_list": "unsubscribe-list",
        "tags": ["tag_1", "tags_2"],
    }
    assert request_headers.get("Content-Type") == "application/json"
    assert (
        request_headers.get("Authorization")
        == aiohttp.BasicAuth("account-token").encode()
    )


async def test_returns_campaign_details_if_success(client, mock_sender_api):
    mock_sender_api(web.json_response(status=200, data=success_response))

    got = await client.schedule_promo_campaign(**input_params)

    assert got == success_response


@pytest.mark.parametrize(
    "recipients_params, expected_request_segment",
    [
        (
            dict(
                mailing_list_source=MailingListSource.YT,
                mailing_list_params=["//path/to/yt/table/at/hahn"],
            ),
            {"template": "ytlist", "params": {"path": "//path/to/yt/table/at/hahn"}},
        ),
        (
            dict(
                mailing_list_source=MailingListSource.IN_PLACE,
                mailing_list_params=[
                    {"email": "ivan@yandex.ru"},
                    {"email": "olga@yandex.ru", "params": {"key": "value"}},
                ],
            ),
            {
                "template": "singleuselist",
                "params": {
                    "recipients": [
                        {"email": "ivan@yandex.ru"},
                        {"email": "olga@yandex.ru", "params": {"key": "value"}},
                    ]
                },
            },
        ),
    ],
)
async def test_sends_recipient_lists_correctly(
    client, mock_sender_api, recipients_params, expected_request_segment
):
    request_params = None

    async def sender_handler(request):
        nonlocal request_params
        request_params = await request.json()
        return web.json_response(status=200, data=success_response)

    mock_sender_api(sender_handler)

    await client.schedule_promo_campaign(**make_input_params(**recipients_params))

    assert request_params["segment"] == expected_request_segment


async def test_sends_allowed_stat_domains_if_provided(mock_sender_api, client):
    request_params = None

    async def sender_handler(request):
        nonlocal request_params
        request_params = await request.json()
        return web.json_response(status=200, data=success_response)

    mock_sender_api(sender_handler)

    await client.schedule_promo_campaign(
        **make_input_params(allowed_stat_domains=["yandex.ru", "ya.ru"])
    )

    assert request_params["allowed_stat_domains"] == ["yandex.ru", "ya.ru"]


async def test_respects_timezone_for_schedule_time(mock_sender_api, client):
    request_params = None

    async def sender_handler(request):
        nonlocal request_params
        request_params = await request.json()
        return web.json_response(status=200, data=success_response)

    mock_sender_api(sender_handler)

    schedule_dt = datetime(
        2020, 1, 1, 0, 0, 0, tzinfo=timezone(offset=timedelta(hours=11, minutes=33))
    )
    await client.schedule_promo_campaign(**make_input_params(schedule_dt=schedule_dt))

    assert request_params["schedule_time"] == "2020-01-01T00:00:00+1133"


@pytest.mark.parametrize(
    "wrong_params, expected_error",
    [
        (
            {"schedule_dt": datetime(2020, 1, 1, 0, 0, 0)},
            "schedule_dt must be aware object",
        ),
        (
            {
                "mailing_list_source": MailingListSource.YT,
                "mailing_list_params": ["a", "b"],
            },
            "For YT mailing list source mailing_list_params "
            "must be single-element list with path to YT table.",
        ),
        (
            {"mailing_list_source": MailingListSource.YT, "mailing_list_params": [1]},
            "For YT mailing list source mailing_list_params "
            "must be single-element list with path to YT table.",
        ),
        (
            {
                "mailing_list_source": MailingListSource.IN_PLACE,
                "mailing_list_params": {"unknown_field": 123},
            },
            "Bad mailing_list_params format. "
            "Must be list of dicts with only email/params keys.",
        ),
    ],
)
async def test_raises_if_incorrect_input(
    client, mock_sender_api, wrong_params, expected_error
):
    bad_input_params = make_input_params(**wrong_params)

    with pytest.raises(IncorrectParams) as exc_info:
        await client.schedule_promo_campaign(**bad_input_params)

    assert exc_info.value.args == (expected_error,)


@pytest.mark.parametrize("mailing_list_source", MailingListSource)
@pytest.mark.parametrize("mailing_list_params", [None, []])
async def test_raises_if_mailing_list_params_has_no_params(
    client, mock_sender_api, mailing_list_source, mailing_list_params
):
    bad_input_params = make_input_params(
        mailing_list_source=mailing_list_source, mailing_list_params=mailing_list_params
    )

    with pytest.raises(IncorrectParams) as exc_info:
        await client.schedule_promo_campaign(**bad_input_params)

    assert exc_info.value.args == ("No params in mailing_list_params",)
