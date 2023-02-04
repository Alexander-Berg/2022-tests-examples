import pytest

from smb.common.aiotvm import CheckTicketFails
from maps_adv.geosmb.cleaner.server.lib.exceptions import NoPassportUidException

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


async def test_uses_dm(domain, dm):
    await domain.register_client_for_delete(
        tvm_user_ticket="user-ticket", request_id="abc"
    )

    assert dm.register_client_for_delete.called_once_with(
        external_id="abc", passport_uid=999
    )


async def test_raises_if_tvm_client_fails(domain, dm, aiotvm):
    aiotvm.fetch_user_uid.side_effect = CheckTicketFails

    with pytest.raises(CheckTicketFails):
        await domain.register_client_for_delete(
            tvm_user_ticket="user-ticket", request_id="abc"
        )


@pytest.mark.parametrize("passport_uid", [None, 0])
async def test_raises_if_passport_uid_cant_be_exctract_from_user_ticket(
    domain, dm, aiotvm, passport_uid
):
    aiotvm.fetch_user_uid.return_value = passport_uid

    with pytest.raises(NoPassportUidException):
        await domain.register_client_for_delete(
            tvm_user_ticket="user-ticket", request_id="abc"
        )
