from datetime import datetime

import hamcrest as hm
import pytest

from billing.library.python.calculator.models.personal_account import ServiceCode

from billing.hot.tests.clients.accounts.client import Client as AccountsClient
from billing.hot.tests.clients.processor.actotron_act_rows_client import Client as ActotronActRowsClient
from billing.hot.tests.lib.matchers.base import (
    success_accounts_read_batch_response,
    success_processor_response_entries,
)
from billing.hot.tests.lib.state import contract

testcases = [
    pytest.param(False, None),
    pytest.param(
        True,
        [
            {
                'loc': {
                    'type': 'commissions',
                },
                'credit': '{:.6f}'.format(200),
            },
            {
                'loc': {
                    'type': 'commissions_acted',
                },
                'debit': '{:.6f}'.format(200),
            },
            {
                'loc': {
                    'type': 'commission_refunds',
                },
                'debit': '{:.6f}'.format(100),
            },
            {
                'loc': {
                    'type': 'commission_refunds_acted',
                },
                'credit': '{:.6f}'.format(100),
            },
        ],
    )
]


@pytest.mark.parametrize('dry_run', [True, False])
@pytest.mark.parametrize('withholding_commissions_from_payments, balances_req', testcases)
@pytest.mark.asyncio
async def test_acts(
    accounts_client: AccountsClient,
    actotron_act_rows_client: ActotronActRowsClient,
    create_state_builder,
    yandex_firm_id: int,
    dry_run: bool,
    withholding_commissions_from_payments: bool,
    balances_req: list[dict],
):
    builder = create_state_builder()
    builder.with_firm(firm_id=yandex_firm_id)
    builder.with_withholding_commissions_from_payments(
        withholding_commissions_from_payments
    )
    builder.with_service_code(ServiceCode.YANDEX_SERVICE)
    builder.fill_contracts(
        contracts=[contract.BnplContract.generate()],
        namespace='bnpl',
        dry_run=dry_run,
        filter='Firm'
    )
    state = builder.built_state()

    async with actotron_act_rows_client.acts(
        state,
        contract_type=contract.BnplContract,
        namespace='bnpl',
        act_sum_positive=200,
        act_sum_negative=-100,
        act_sum_wo_vat_positive=20,
        act_sum_wo_vat_negative=-10,
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))
        hm.assert_that(await response.json(), success_processor_response_entries({
            'event': {
                'tariffer_payload': {
                    'dry_run': dry_run,
                },
            }
        }))

    accounts = [
        'commissions',
        'commission_refunds',
        'commissions_acted',
        'commission_refunds_acted',
    ]

    async with accounts_client.read_balances(
        state,
        int(datetime.now().timestamp()),
        accounts,
        'bnpl',
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))
        hm.assert_that(await response.json(), success_accounts_read_batch_response({
            'balances': balances_req,
        }))
