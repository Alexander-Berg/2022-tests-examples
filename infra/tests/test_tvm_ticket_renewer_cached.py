import pytest
from unittest import mock

from infra.deploy_notifications_controller.lib.tvm_ticket_renewer import TvmTicketRenewer, CachedTvmTicketRenewer

pytestmark = pytest.mark.asyncio


default_tvm_ticket = 'default_tvm_ticket'


@pytest.fixture(scope='function')
def renewer() -> TvmTicketRenewer:
    renewer = mock.MagicMock(TvmTicketRenewer)

    def renew_ticket_mock(
        *args,
        **kwargs
    ):
        return default_tvm_ticket

    renewer.renew_ticket.side_effect = renew_ticket_mock

    return renewer


@pytest.fixture(scope='function')
def cached_renewer(
    renewer: TvmTicketRenewer,
) -> CachedTvmTicketRenewer:
    return CachedTvmTicketRenewer(
        renewer=renewer,
    )


async def test_get_ticket_correct_value(
    cached_renewer: CachedTvmTicketRenewer,
):
    expected_ticket = default_tvm_ticket

    actual_ticket_first = await cached_renewer.get_ticket()
    assert actual_ticket_first == expected_ticket

    actual_ticket_second = await cached_renewer.get_ticket()
    assert actual_ticket_second == expected_ticket


async def test_get_ticket_renew_called_once(
    cached_renewer: CachedTvmTicketRenewer,
):
    await cached_renewer.get_ticket()
    cached_renewer._renewer.renew_ticket.assert_called_once()
    cached_renewer._renewer.renew_ticket.reset_mock()

    await cached_renewer.get_ticket()
    cached_renewer._renewer.renew_ticket.assert_not_called()
    cached_renewer._renewer.renew_ticket.reset_mock()
