from uuid import uuid4

import pytest

from sendr_pytest.mocks import mock_action  # noqa

from hamcrest import assert_that, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.transaction.get_view import GetTransactionViewAction
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import TransactionAction, TransactionStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.transaction import (
    ThreeDSV2ChallengeRequest,
    TransactionActionView,
    TransactionData,
    TransactionThreeDSData,
)


@pytest.fixture
def headers():
    return {
        'x-pay-session-id': 'sessid',
    }


@pytest.fixture
def transaction_id():
    return uuid4()


@pytest.fixture
async def transaction_view(transaction_id, entity_threeds_authentication_request):
    return TransactionActionView(
        transaction_id=transaction_id,
        checkout_order_id=uuid4(),
        status=TransactionStatus.THREEDS_CHALLENGE,
        integration_id=uuid4(),
        card_id='card-x1234',
        data=TransactionData(
            user_ip='192.0.2.1',
            threeds=TransactionThreeDSData(
                authentication_request=entity_threeds_authentication_request,
                challenge_request=ThreeDSV2ChallengeRequest(
                    acs_url='https://hello.test',
                    creq='123456',
                    session_data='7890',
                )
            ),
        ),
        version=1,
        action=TransactionAction.IFRAME,
        action_url='https://yapay.test',
    )


@pytest.fixture(autouse=True)
def mock_get_action(mock_action, transaction_view):  # noqa
    return mock_action(GetTransactionViewAction, return_value=transaction_view)


@pytest.mark.asyncio
async def test_calls_action(
    public_app,
    mock_user_authentication,
    entity_auth_user,
    headers,
    mock_get_action,
    transaction_id,
):
    await public_app.get(
        f'/api/public/v1/transactions/{transaction_id}',
        headers=headers,
    )

    mock_get_action.assert_run_once_with(
        user=entity_auth_user,
        transaction_id=transaction_id,
    )


@pytest.mark.asyncio
async def test_returned(
    public_app,
    mock_user_authentication,
    headers,
    transaction_id,
):
    r = await public_app.get(
        f'/api/public/v1/transactions/{transaction_id}',
        headers=headers,
    )

    expected_response = {
        'transaction_id': str(transaction_id),
        'status': 'THREEDS_CHALLENGE',
        'version': 1,
        'action': 'IFRAME',
        'action_url': 'https://yapay.test',
        'reason': None,
    }
    assert_that(r.status, equal_to(200))
    response_data = await r.json()
    assert_that(response_data['data'], equal_to({'transaction': expected_response}))


@pytest.mark.asyncio
async def test_returned_without_action(
    public_app,
    mock_user_authentication,
    headers,
    transaction_id,
    transaction_view,
):
    transaction_view.status = TransactionStatus.CHARGED
    transaction_view.action = None
    transaction_view.action_url = None

    r = await public_app.get(
        f'/api/public/v1/transactions/{transaction_id}',
        headers=headers,
    )

    expected_response = {
        'transaction_id': str(transaction_id),
        'status': 'CHARGED',
        'version': 1,
        'action': None,
        'action_url': None,
        'reason': None,
    }
    assert_that(r.status, equal_to(200))
    response_data = await r.json()
    assert_that(response_data['data'], equal_to({'transaction': expected_response}))
