import asyncio

import pytest

from maps_adv.geosmb.harmonist.server.lib.exceptions import (
    InvalidSessionId,
    MarkUpAlreadySubmitted,
)

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_saves_markup_in_db(domain, dm):
    dm.fetch_clients_creation_log.coro.return_value = {
        "parsed_input": ["some", "parsed", "strings"]
    }

    await domain.submit_markup(
        session_id="f8d049c53e04", biz_id=123, markup={"some": "params"}
    )

    dm.submit_markup.assert_called_with(
        session_id="f8d049c53e04", biz_id=123, markup={"some": "params"}
    )


async def test_returns_nothing(domain, dm):
    dm.fetch_clients_creation_log.coro.return_value = {
        "parsed_input": ["some", "parsed", "strings"],
    }
    dm.submit_markup.coro.return_value = None

    got = await domain.submit_markup(
        session_id="f8d049c53e04", biz_id=123, markup={"some": "params"}
    )

    assert got is None


async def test_raises_if_markup_already_submitted(domain, dm):
    dm.fetch_clients_creation_log.coro.return_value = {
        "markup": {"Any": "markup"},
        "parsed_input": ["some", "parsed", "strings"],
    }

    with pytest.raises(
        MarkUpAlreadySubmitted,
        match="Markup for session_id f8d049c53e04 already submitted.",
    ):
        await domain.submit_markup(
            session_id="f8d049c53e04", biz_id=123, markup={"some": "params"}
        )


async def test_does_not_update_creation_log_if_markup_already_submitted(domain, dm):
    dm.fetch_clients_creation_log.coro.return_value = {
        "markup": {"Any": "markup"},
        "parsed_input": ["some", "parsed", "strings"],
    }

    try:
        await domain.submit_markup(
            session_id="f8d049c53e04", biz_id=123, markup={"some": "params"}
        )
    except MarkUpAlreadySubmitted:
        pass

    await asyncio.sleep(0.1)

    dm.submit_markup.assert_not_called()
    dm.submit_validated_clients.assert_not_called()


async def test_raises_if_nothing_found_for_session_id(dm, domain):
    dm.fetch_clients_creation_log.coro.return_value = None

    with pytest.raises(
        InvalidSessionId,
        match="Session not found: session_id=f8d049c53e04, biz_id=123",
    ):
        await domain.submit_markup(
            session_id="f8d049c53e04", biz_id=123, markup={"some": "params"}
        )


async def test_does_not_update_creation_log_if_nothing_found_for_session_id(domain, dm):
    dm.fetch_clients_creation_log.coro.return_value = None

    try:
        await domain.submit_markup(
            session_id="f8d049c53e04", biz_id=123, markup={"some": "params"}
        )
    except InvalidSessionId:
        pass

    await asyncio.sleep(0.1)

    dm.submit_markup.assert_not_called()
    dm.submit_validated_clients.assert_not_called()
