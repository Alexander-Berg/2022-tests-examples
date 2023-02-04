import pytest

from billing.yandex_pay_plus.yandex_pay_plus.storage import Storage
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import TransactionStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.integration import Integration
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import (
    ThreeDSV1ChallengeRequest,
    ThreeDSV2ChallengeRequest,
)
from billing.yandex_pay_plus.yandex_pay_plus.tests.utils import beautiful_soup


@pytest.mark.asyncio
async def test_challenge_threeds_one(
    public_app,
    storage: Storage,
    authenticate_client,
    create_order,
    create_integration,
    create_transaction,
):
    authenticate_client(public_app)
    order = await create_order()
    checkout_order_id = order['checkout_order_id']
    await create_integration()
    transaction = await create_transaction(checkout_order_id)
    transaction_id = transaction['transaction_id']

    db_transaction = await storage.transaction.get(transaction_id)
    db_transaction.data.threeds.challenge_request = ThreeDSV1ChallengeRequest(
        acs_url='https://hello',
        pareq='123456',
        md='7890',
    )
    await storage.transaction.save(db_transaction)

    r = await public_app.get(
        f'/3ds/challenge/{transaction_id}',
    )
    data = await r.text()
    html = beautiful_soup(data)
    body = html.body
    inp = list(body.form.find_all('input'))

    assert body['onload'] == "setTimeout(function() { document.forms['form'].submit(); }, 10)"

    assert inp[0]['name'] == 'PaReq'
    assert inp[0]['value'] == '123456'
    assert inp[1]['name'] == 'MD'
    assert inp[1]['value'] == '7890'
    assert inp[2]['name'] == 'TermUrl'
    assert inp[2]['value'] == f'https://test.pay.yandex.ru/3ds/challenge-result/{transaction_id}'


@pytest.mark.asyncio
async def test_challenge_threeds_two(
    public_app,
    storage: Storage,
    authenticate_client,
    create_order,
    create_integration,
    create_transaction,
):
    authenticate_client(public_app)
    order = await create_order()
    checkout_order_id = order['checkout_order_id']
    await create_integration()
    transaction = await create_transaction(checkout_order_id)
    transaction_id = transaction['transaction_id']

    db_transaction = await storage.transaction.get(transaction_id)
    db_transaction.data.threeds.challenge_request = ThreeDSV2ChallengeRequest(
        acs_url='https://hello',
        creq='123456',
        session_data='7890',
    )
    await storage.transaction.save(db_transaction)

    r = await public_app.get(
        f'/3ds/challenge/{transaction_id}',
    )
    data = await r.text()
    html = beautiful_soup(data)
    body = html.body
    inp = list(body.form.find_all('input'))

    assert body['onload'] == "setTimeout(function() { document.forms['form'].submit(); }, 10)"

    assert inp[0]['name'] == 'creq'
    assert inp[0]['value'] == '123456'
    assert inp[1]['name'] == 'threeDSSessionData'
    assert inp[1]['value'] == '7890'


@pytest.mark.asyncio
async def test_challenge_wrapper(
    public_app,
    storage: Storage,
    authenticate_client,
    create_order,
    create_integration,
    create_transaction,
):
    authenticate_client(public_app)
    order = await create_order()
    checkout_order_id = order['checkout_order_id']
    await create_integration()
    transaction = await create_transaction(checkout_order_id)
    transaction_id = transaction['transaction_id']

    db_transaction = await storage.transaction.get(transaction_id)
    db_transaction.data.threeds.challenge_request = ThreeDSV2ChallengeRequest(
        acs_url='https://hello',
        creq='123456',
        session_data='7890',
    )
    await storage.transaction.save(db_transaction)

    r = await public_app.get(
        f'/3ds/challenge/{transaction_id}/wrapper',
    )
    data = await r.text()

    assert f'<iframe src="https://test.pay.yandex.ru/3ds/challenge/{transaction_id}">' in data


class TestChallengeResult:
    @pytest.mark.asyncio
    async def test_success(
        self,
        public_app,
        storage: Storage,
        authenticate_client,
        create_order,
        create_integration,
        create_transaction,
    ):
        authenticate_client(public_app)
        order = await create_order()
        checkout_order_id = order['checkout_order_id']
        await create_integration(
            creds=Integration.encrypt_creds(
                {'key': 'key', 'password': 'password', 'gateway_merchant_id': 'the-gwid'}
            )
        )
        transaction = await create_transaction(checkout_order_id)
        transaction_id = transaction['transaction_id']

        db_transaction = await storage.transaction.get(transaction_id)
        db_transaction.status = TransactionStatus.THREEDS_CHALLENGE
        db_transaction.threeds_data = ThreeDSV1ChallengeRequest(
            acs_url='https://hello',
            pareq='123456',
            md='7890',
        )
        await storage.transaction.save(db_transaction)

        public_app.session.cookie_jar.clear()
        r = await public_app.post(
            f'/3ds/challenge-result/{transaction_id}',
            data={
                'MD': '123456',
                'PaRes': '7890'
            }
        )
        data = await r.text()
        html = beautiful_soup(data)
        script = html.head.script
        normalized = script.text.replace('\n', '').replace(' ', '')
        db_transaction = await storage.transaction.get(transaction_id)

        assert normalized == (
            'functiononLoadFunction(){varpostMessageData={"type":"tds_complete","status":"AUTHORIZED"};'
            'vartargetOrigin="https://test.pay.yandex.ru";parent.postMessage(postMessageData,targetOrigin);}'
        )
        assert db_transaction.data.threeds.challenge_response.pares == '7890'
