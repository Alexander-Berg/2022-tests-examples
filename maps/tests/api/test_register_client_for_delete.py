from datetime import datetime

import pytest

from smb.common.aiotvm.lib import BaseTvmException
from maps_adv.common.helpers import Any
from maps_adv.geosmb.cleaner.server.lib.exceptions import NoPassportUidException

pytestmark = [pytest.mark.asyncio]

url = "/1/takeout/delete/"


async def test_returns_ok_for_success(api):
    res = await api.post(
        url,
        headers={"X-Ya-User-Ticket": "u-ticket"},
        params={"request_id": "abc"},
        expected_status=200,
    )

    assert res == {"status": "ok"}


async def test_returns_400_if_required_params_are_missed(api):
    res = await api.post(
        url, headers={"X-Ya-User-Ticket": "u-ticket"}, expected_status=400
    )

    assert res == b"request_id query param is missed or empty"


async def test_returns_error_if_user_ticket_is_missed(api):
    res = await api.post(url, params={"request_id": "abc"}, expected_status=200)

    assert res == {
        "status": "error",
        "errors": [
            {"code": "missed_user_ticket", "message": "TVM user ticket is missed"}
        ],
    }


async def test_returns_error_if_user_ticket_is_bad(aiotvm, api):
    aiotvm.fetch_user_uid.side_effect = BaseTvmException(
        {
            "error": "invalid ticket format",
            "debug_string": "debug",
            "logging_string": "bad-ticket",
        }
    )

    res = await api.post(
        url,
        headers={"X-Ya-User-Ticket": "u-ticket"},
        params={"request_id": "abc"},
        expected_status=200,
    )

    assert res == {
        "status": "error",
        "errors": [
            {
                "code": "bad_user_ticket",
                "message": "{'error': 'invalid ticket format', "
                "'debug_string': 'debug', 'logging_string': 'bad-ticket'}",
            }
        ],
    }


async def test_returns_error_if_user_ticket_without_passport_uid(aiotvm, api):
    aiotvm.fetch_user_uid.side_effect = NoPassportUidException

    res = await api.post(
        url,
        headers={"X-Ya-User-Ticket": "u-ticket"},
        params={"request_id": "abc"},
        expected_status=200,
    )

    assert res == {
        "status": "error",
        "errors": [
            {
                "code": "no_passport_uid",
                "message": "No passport uid for TVM user ticket",
            }
        ],
    }


async def test_saves_request_to_db(api, aiotvm, con):
    await api.post(
        url,
        headers={"X-Ya-User-Ticket": "u-ticket"},
        params={"request_id": "abc"},
        expected_status=200,
    )

    rows = await con.fetch("SELECT * FROM delete_requests")
    assert [dict(row) for row in rows] == [
        {
            "id": Any(int),
            "external_id": "abc",
            "passport_uid": 123,
            "processed_at": None,
            "created_at": Any(datetime),
        }
    ]
