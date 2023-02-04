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
    "product, transaction_type, expected_account, expected_event_type",
    [
        pytest.param("trip_payment", "payment", "cashless", "credit"),
        pytest.param("trip_payment", "refund", "cashless_refunds", "debit"),
        pytest.param("trip_compensation", "payment", "compensations", "credit"),
        pytest.param("trip_compensation", "refund", "compensations_refunds", "debit"),
    ],
)
async def test_cashless(
    accounts_client: AccountsClient,
    taxi_client: TaxiClient,
    create_state_builder,
    product,
    transaction_type,
    expected_account,
    expected_event_type,
):
    builder = create_state_builder()
    builder.fill_contracts()
    state = builder.built_state()

    amount = 421
    async with taxi_client.cashless(
        state,
        extended_params={
            "amount": amount,
            "product": product,
            "transaction_type": transaction_type,
        },
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))

    # проверим что транзакция дошла до аккаунтера
    hm.assert_that(state.new_transaction_ids, hm.has_length(1))

    async with accounts_client.read_balances(
        state,
        int(datetime.now().timestamp()),
        [expected_account],
        "taxi",
    ) as response:
        hm.assert_that(response.status, hm.equal_to(200))
        sign = -1 if transaction_type == "refund" else 1
        hm.assert_that(
            await response.json(),
            success_accounts_read_batch_response(
                {
                    "balances": [
                        {
                            "loc": {"type": expected_account},
                            expected_event_type: "{:.6f}".format(amount * sign),
                        }
                    ]
                }
            ),
        )
