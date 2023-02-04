from dataclasses import replace

import pytest

from sendr_auth import User

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.threeds.challenge import ThreeDSChallengeAction
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import (
    ThreeDSChallengeNotFoundError,
    TransactionNotFoundError,
    UnsupportedThreeDSVersion,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage import Storage
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import TransactionStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import (
    ThreeDSV1ChallengeRequest,
    ThreeDSV1ChallengeResponse,
    ThreeDSV2ChallengeRequest,
    Transaction,
    TransactionData,
    TransactionThreeDSData,
)
from billing.yandex_pay_plus.yandex_pay_plus.storage.mappers.transaction import TransactionMapper
from billing.yandex_pay_plus.yandex_pay_plus.tests.utils import beautiful_soup


@pytest.fixture
async def checkout_order(storage, stored_checkout_order):
    return await storage.checkout_order.save(
        replace(
            stored_checkout_order,
            uid=7777,
        )
    )


@pytest.fixture
def transaction(checkout_order, entity_threeds_authentication_request, stored_integration):
    return Transaction(
        checkout_order_id=checkout_order.checkout_order_id,
        status=TransactionStatus.AUTHORIZED,
        integration_id=stored_integration.integration_id,
        data=TransactionData(
            user_ip='192.0.2.1',
            threeds=TransactionThreeDSData(
                authentication_request=entity_threeds_authentication_request,
            ),
        )
    )


@pytest.mark.asyncio
async def test_threeds_one(storage: Storage, transaction):
    transaction.data.threeds.challenge_request = ThreeDSV1ChallengeRequest(
        acs_url='https://hello.test',
        pareq='123456',
        md='7890',
    )
    transaction = await storage.transaction.create(transaction)

    challenge = await ThreeDSChallengeAction(
        user=User(uid=7777),
        transaction_id=transaction.transaction_id
    ).run()

    html = beautiful_soup(challenge)
    body = html.body
    form = body.form
    inp = list(form.find_all('input'))

    assert body['onload'] == "setTimeout(function() { document.forms['form'].submit(); }, 10)"
    assert form['action'] == 'https://hello.test'

    assert inp[0]['name'] == 'PaReq'
    assert inp[0]['value'] == '123456'
    assert inp[1]['name'] == 'MD'
    assert inp[1]['value'] == '7890'
    assert inp[2]['name'] == 'TermUrl'
    assert inp[2]['value'] == f'https://test.pay.yandex.ru/3ds/challenge-result/{transaction.transaction_id}'


@pytest.mark.asyncio
async def test_threeds_two(storage: Storage, transaction):
    transaction.data.threeds.challenge_request = ThreeDSV2ChallengeRequest(
        acs_url='https://hello.test',
        creq='123456',
        session_data='7890',
    )
    transaction = await storage.transaction.create(transaction)

    challenge = await ThreeDSChallengeAction(
        user=User(uid=7777),
        transaction_id=transaction.transaction_id
    ).run()

    html = beautiful_soup(challenge)
    body = html.body
    form = body.form
    inp = list(form.find_all('input'))

    assert body['onload'] == "setTimeout(function() { document.forms['form'].submit(); }, 10)"
    assert form['action'] == 'https://hello.test'
    assert inp[0]['name'] == 'creq'
    assert inp[0]['value'] == '123456'
    assert inp[1]['name'] == 'threeDSSessionData'
    assert inp[1]['value'] == '7890'


@pytest.mark.asyncio
async def test_wrong_uid(storage: Storage, transaction):
    transaction = await storage.transaction.create(transaction)
    with pytest.raises(TransactionNotFoundError):
        await ThreeDSChallengeAction(
            user=User(uid=8888),
            transaction_id=transaction.transaction_id
        ).run()


@pytest.mark.asyncio
async def test_no_challenge(storage: Storage, transaction):
    transaction.data.threeds.challenge_request = None
    transaction = await storage.transaction.create(transaction)
    with pytest.raises(ThreeDSChallengeNotFoundError):
        await ThreeDSChallengeAction(
            user=User(uid=7777),
            transaction_id=transaction.transaction_id
        ).run()


@pytest.mark.asyncio
async def test_challenge_already_fulfilled(storage: Storage, transaction):
    transaction.data.threeds.challenge_request = ThreeDSV1ChallengeRequest(
        acs_url='https://hello.test',
        pareq='123456',
        md='7890',
    )
    transaction.data.threeds.challenge_response = ThreeDSV1ChallengeResponse(
        pares='pares'
    )
    transaction = await storage.transaction.create(transaction)
    with pytest.raises(ThreeDSChallengeNotFoundError):
        await ThreeDSChallengeAction(
            user=User(uid=7777),
            transaction_id=transaction.transaction_id
        ).run()


@pytest.mark.asyncio
async def test_unknown_challenge(storage: Storage, mocker, transaction):
    transaction = await storage.transaction.create(transaction)
    transaction.data.threeds.challenge_request = object()
    mocker.patch.object(TransactionMapper, 'get', mocker.AsyncMock(return_value=transaction))

    with pytest.raises(UnsupportedThreeDSVersion):
        await ThreeDSChallengeAction(
            user=User(uid=7777),
            transaction_id=transaction.transaction_id
        ).run()
