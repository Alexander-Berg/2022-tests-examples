from dataclasses import replace
from uuid import uuid4

import pytest
from pay.lib.entities.enums import CardNetwork

from sendr_pytest.matchers import equal_to

from hamcrest import assert_that

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import IntegrationStatus, TransactionStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.integration import Integration
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.psp import PSP
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import (
    SplitOrderMetaData,
    SplitTransactionData,
    ThreeDSV2ChallengeRequest,
    ThreeDSV2ChallengeResponse,
    Transaction,
    TransactionData,
    TransactionThreeDSData,
)


@pytest.mark.asyncio
async def test_create(storage, make_transaction):
    transaction = make_transaction()

    created = await storage.transaction.create(transaction)

    transaction.created = created.created
    transaction.updated = created.updated
    assert_that(
        created,
        equal_to(transaction),
    )


@pytest.mark.asyncio
async def test_get(storage, make_transaction, checkout_order):
    transaction = make_transaction()

    created = await storage.transaction.create(transaction)
    created.order = checkout_order

    got = await storage.transaction.get(created.transaction_id)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_skip_related(storage, make_transaction):
    transaction = make_transaction()

    created = await storage.transaction.create(transaction)

    got = await storage.transaction.get(created.transaction_id, skip_related=True)

    assert_that(
        got,
        equal_to(created),
    )


@pytest.mark.asyncio
async def test_get_by_checkout_order_id(storage, make_transaction):
    await storage.transaction.create(make_transaction(status=TransactionStatus.FAILED))
    transaction = await storage.transaction.create(make_transaction(status=TransactionStatus.THREEDS_CHALLENGE))

    assert_that(
        await storage.transaction.get_last_not_failed(transaction.checkout_order_id),
        equal_to(transaction),
    )


@pytest.mark.asyncio
async def test_find_by_checkout_order_id(storage, make_transaction, another_checkout_order):
    await storage.transaction.create(make_transaction(checkout_order_id=another_checkout_order.checkout_order_id))
    expected = [
        await storage.transaction.create(make_transaction(status=TransactionStatus.FAILED)),
        await storage.transaction.create(make_transaction(status=TransactionStatus.THREEDS_CHALLENGE)),
    ]

    assert_that(
        await storage.transaction.find_by_checkout_order_id(expected[0].checkout_order_id),
        equal_to(expected),
    )


@pytest.mark.asyncio
async def test_get_not_found(storage):
    with pytest.raises(Transaction.DoesNotExist):
        await storage.transaction.get(uuid4())


@pytest.mark.asyncio
async def test_save(storage, make_transaction):
    created = await storage.transaction.create(make_transaction())
    created.status = TransactionStatus.AUTHORIZED
    created.data.threeds.challenge_response.cres = 'cres2'

    saved = await storage.transaction.save(created)

    created.updated = saved.updated
    assert_that(
        saved,
        equal_to(created),
    )


@pytest.fixture
async def checkout_order(stored_checkout_order):
    return stored_checkout_order


@pytest.fixture
async def another_checkout_order(storage, entity_checkout_order, stored_merchant):
    return await storage.checkout_order.create(
        replace(
            entity_checkout_order,
            order_id=uuid4(),
            merchant_id=stored_merchant.merchant_id,
        )
    )


@pytest.fixture
async def integration(storage, psp, stored_merchant):
    return await storage.integration.create(
        Integration(
            merchant_id=stored_merchant.merchant_id,
            psp_id=psp.psp_id,
            status=IntegrationStatus.DEPLOYED,
            creds=Integration.encrypt_creds({
                'key': 'the-key',
                'password': 'the-password',
                'gateway_merchant_id': 'gw-mid',
            }),
        )
    )


@pytest.fixture
async def psp(storage, rands):
    return await storage.psp.create(
        PSP(psp_external_id=rands(), psp_id=uuid4())
    )


@pytest.fixture
def make_transaction(storage, checkout_order, entity_threeds_authentication_request, integration):
    def _make_transaction(**kwargs):
        transaction = Transaction(
            checkout_order_id=checkout_order.checkout_order_id,
            status=TransactionStatus.NEW,
            integration_id=integration.integration_id,
            card_id='card-x1234',
            card_last4='1234',
            card_network=CardNetwork.MIR,
            reason='the-reason',
            data=TransactionData(
                user_ip='192.0.2.1',
                threeds=TransactionThreeDSData(
                    authentication_request=entity_threeds_authentication_request,
                    challenge_request=ThreeDSV2ChallengeRequest(
                        acs_url='https://acs_url.test',
                        creq='creq',
                        session_data='session_data',
                    ),
                    challenge_response=ThreeDSV2ChallengeResponse(
                        cres='cres',
                    ),
                ),
                split=SplitTransactionData(
                    checkout_url='https://split-checkout-url.test',
                    order_meta=SplitOrderMetaData(
                        order_id='split-order-id',
                    ),
                ),
            ),
            message_id=f'0:{uuid4()}'
        )
        for key in kwargs:
            setattr(transaction, key, kwargs[key])
        return transaction
    return _make_transaction
