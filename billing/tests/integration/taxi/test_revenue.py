import logging
from datetime import datetime

import hamcrest as hm
import pytest

from billing.hot.tests.clients.accounts.client import Client as AccountsClient
from billing.hot.tests.clients.processor.taxi_client import Client as TaxiClient
from billing.hot.tests.lib.matchers.base import success_accounts_read_batch_response

logging.disable(logging.DEBUG)


@pytest.mark.asyncio
@pytest.mark.parametrize(
    "product, aggregation_sign, promo, expected_account, expected_event_type",
    [
        pytest.param("coupon", 1, True, "promocodes", "credit"),
        pytest.param("subvention", -1, True, "promocodes_refunds", "debit"),
        pytest.param("cargo_order", 1, False, "commissions", "debit"),
        pytest.param("childchair", -1, False, "commissions_refunds", "credit"),
    ],
)
async def test_revenue(
    accounts_client: AccountsClient,
    taxi_client: TaxiClient,
    create_state_builder,
    product,
    aggregation_sign,
    promo,
    expected_account,
    expected_event_type,
):
    builder = create_state_builder()
    builder.fill_contracts()
    state = builder.built_state()

    amount = 100
    async with taxi_client.revenue(
        state,
        extended_params={
            "amount": amount,
            "product": product,
            "aggregation_sign": aggregation_sign,
        },
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))

    amounts = [amount] if promo else [amount, amount * 1.2]
    accounts = (
        [expected_account]
        if promo
        else [expected_account, expected_account + "_with_vat"]
    )

    async with accounts_client.read_balances(
        state,
        int(datetime.now().timestamp()),
        accounts,
        "taxi",
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))
        hm.assert_that(
            await response.json(),
            success_accounts_read_batch_response(
                {
                    "balances": [
                        {
                            "loc": {"type": account},
                            expected_event_type: "{:.6f}".format(amount),
                        }
                        for amount, account in zip(amounts, accounts)
                    ]
                }
            ),
        )
