import pytest

from maps_adv.common.blackbox import BlackboxClientException

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("ssl_session_id", [None, "def"])
async def test_uses_blackbox_client(domain, blackbox_client, ssl_session_id):
    await domain.get_user_ticket(
        user_ip="1.2.3.4",
        session_id="abc",
        ssl_session_id=ssl_session_id,
    )

    blackbox_client.sessionid.assert_called_with(
        user_ip="1.2.3.4",
        session_id="abc",
        ssl_session_id=ssl_session_id,
        get_user_ticket="yes",
    )


async def test_returns_user_ticket(domain):
    ticket = await domain.get_user_ticket(user_ip="1.2.3.4", session_id="abc")

    assert ticket == "some"


async def test_propagates_exception(domain, blackbox_client):
    blackbox_client.sessionid.side_effect = BlackboxClientException

    with pytest.raises(BlackboxClientException):
        await domain.get_user_ticket(user_ip="1.2.3.4", session_id="abc")
