import pytest
from unittest import mock

import ticket_parser2.api.v1 as tp2

from infra.deploy_notifications_controller.lib.tvm_ticket_renewer import TvmTicketRenewer

pytestmark = pytest.mark.asyncio


def create_ticket(alias: str):
    return f'{alias}_ticket'


@pytest.fixture(scope='function')
def tvm() -> tp2.TvmClient:
    tvm = mock.MagicMock(tp2.TvmClient)

    def get_ticket_for_mock(
        alias: str,
        *args,
        **kwargs
    ):
        return create_ticket(alias=alias)

    tvm.get_service_ticket_for.side_effect = get_ticket_for_mock

    return tvm


default_tvm_dest_name = 'default_dest_name'


@pytest.fixture(scope='function')
def renewer(
    tvm: tp2.TvmClient,
) -> TvmTicketRenewer:
    return TvmTicketRenewer(
        dest_name=default_tvm_dest_name,
        tvm=tvm,
    )


async def test_renew_ticket(
    renewer: TvmTicketRenewer,
):
    expected_ticket = create_ticket(
        alias=renewer.dest_name,
    )

    actual_ticket = await renewer.renew_ticket()
    assert actual_ticket == expected_ticket
