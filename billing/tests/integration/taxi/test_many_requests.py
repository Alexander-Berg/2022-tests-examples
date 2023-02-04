import asyncio

import pytest

from billing.hot.tests.lib.state import state
from billing.hot.tests.lib.polling import poll


async def poll_accounts(
    accounts_client,
    st,
    request: dict,
    interval_seconds: float = 1,
    timeout_seconds: float = 100000,
):
    async def poll_body():
        async with accounts_client.write_batch(st, request) as response:
            if response.status == 201:
                return response.status
        raise poll.RetryError

    return await poll.poll(
        poll_body, interval_seconds=interval_seconds, timeout_seconds=timeout_seconds
    )


async def poll_payout_by_client(
    payout_client, st, interval_seconds: float = 1, timeout_seconds: float = 30
):
    async def poll_body():
        async with payout_client.payout_by_client(st) as response:
            if response.status == 201:
                return response.status
        raise poll.RetryError

    return await poll.poll(
        poll_body, interval_seconds=interval_seconds, timeout_seconds=timeout_seconds
    )


def create_request_to_accounter(
    accounts_client, st, with_netting: bool = False, with_cpf: bool = False
) -> dict:
    params = {"event_type": "taxi"}
    contract_states = {}
    if with_cpf:
        invoices = [
            accounts_client.writebatch_renderer.fill_invoice(st, {"amount": 100})
        ]
        contract_states = accounts_client.writebatch_renderer.fill_contract_state(
            st, invoices=invoices, extended_params={"payment_amount": 100}
        )
    if with_netting:
        params["event_type"] += ":payout"
    info = accounts_client.writebatch_renderer.fill_info(
        st, contract_states=contract_states
    )
    event = accounts_client.writebatch_renderer.fill_event(st)
    request = accounts_client.writebatch_renderer.render_write_batch_request(
        st, extended_params=params, info=info, events=[event]
    )
    return request


async def send_one_request(create_state_builder, payout_client, accounts_client):
    st = state.PipelineState.generate()
    builder = create_state_builder(st)
    builder.fill_contracts(dry_run=True, migrate=False)
    st = builder.built_state()

    request = create_request_to_accounter(accounts_client, st, with_netting=True)
    await asyncio.wait_for(poll_accounts(accounts_client, st, request), 10)

    # Создаем выплату
    await asyncio.wait_for(poll_payout_by_client(payout_client, st), 100000)


@pytest.skip("TODO: skip reason", allow_module_level=True)
@pytest.mark.asyncio
async def test_many_payouts(create_state_builder, payout_client, accounts_client):
    """
    Создаем нагрузку в n заявок на систему выплат
    """
    n = 1000
    await asyncio.gather(
        *[
            send_one_request(create_state_builder, payout_client, accounts_client)
            for _ in range(n)
        ]
    )
