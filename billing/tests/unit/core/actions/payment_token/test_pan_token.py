import logging
from datetime import timedelta
from decimal import Decimal
from uuid import uuid4

import pytest
from pay.lib.entities.payment_token import MITInfo

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to, has_entries, has_properties

from billing.yandex_pay.yandex_pay.core.actions.payment_token.pan_token import CreatePANTokenAction
from billing.yandex_pay.yandex_pay.core.entities.message import Message
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
from billing.yandex_pay.yandex_pay.core.exceptions import CoreCardNotFoundError
from billing.yandex_pay.yandex_pay.interactions import CardProxyClient
from billing.yandex_pay.yandex_pay.interactions.duckgo.entities.checkout import PANCheckoutResult
from billing.yandex_pay.yandex_pay.interactions.trust_paysys import TrustPaysysCardInfo, TrustPaysysClient
from billing.yandex_pay.yandex_pay.interactions.trust_paysys.exceptions import CardNotFoundTrustPaysysError

CURRENCY = 'XTS'
AMOUNT = Decimal('100.00')
GATEWAY_MERCHANT_ID = 'gateway_merchant_id'


@pytest.fixture
def payment_token(rands):
    return rands()


@pytest.fixture
def psp():
    return PSP(
        psp_external_id='psp_external_id',
        psp_id=uuid4(),
        public_key='pubkey',
        public_key_signature='pubkeysig',
    )


@pytest.fixture
def trust_card_id(rands):
    return f'card-x{rands()}'


@pytest.fixture
def message(rands):
    return Message(rands, utcnow() + timedelta(days=1))


@pytest.fixture
def paysys_card_info():
    return TrustPaysysCardInfo(
        card_id='whatever',
        card_token='card-token',
        expiration_month=utcnow().month,
        expiration_year=utcnow().year + 1,
        holder='holder',
    )


@pytest.fixture(autouse=True)
def mock_get_trust_paysys_card_info(mocker, paysys_card_info):
    mock = mocker.AsyncMock(return_value=paysys_card_info)
    return mocker.patch.object(TrustPaysysClient, 'get_card', mock)


@pytest.fixture(autouse=True)
def mock_cardproxy_pan_checkout(mocker, payment_token):
    return_value = mocker.Mock(spec=PANCheckoutResult, payment_token=payment_token)
    return mocker.patch.object(
        CardProxyClient, 'pan_checkout', mocker.AsyncMock(return_value=return_value)
    )


@pytest.fixture
def params(trust_card_id, psp, message):
    return dict(
        trust_card_id=trust_card_id,
        psp=psp,
        amount=AMOUNT,
        currency=CURRENCY,
        gateway_merchant_id=GATEWAY_MERCHANT_ID,
        message=message,
        mit_info=MITInfo(recurring=True),
    )


@pytest.mark.asyncio
async def test_success(params, payment_token):
    token = await CreatePANTokenAction(**params).run()

    assert_that(token, equal_to(payment_token))


@pytest.mark.asyncio
async def test_calls_cardproxy(params, mock_cardproxy_pan_checkout, paysys_card_info):
    await CreatePANTokenAction(**params).run()

    mock_cardproxy_pan_checkout.assert_called_once_with(
        pci_card_token=paysys_card_info.card_token,
        pan_expiration_month=paysys_card_info.expiration_month,
        pan_expiration_year=paysys_card_info.expiration_year,
        gateway_merchant_id=GATEWAY_MERCHANT_ID,
        recipient_id=params['psp'].psp_external_id,
        recipient_pub_key=params['psp'].public_key,
        recipient_pub_key_signature=params['psp'].public_key_signature,
        transaction_currency=CURRENCY,
        transaction_amount=AMOUNT,
        mit_info=params['mit_info'],
        message_id=params['message'].message_id,
        message_expiration=params['message'].expiration_epoch_ms,
    )


@pytest.mark.asyncio
async def test_calls_paysys(params, mock_get_trust_paysys_card_info, paysys_card_info):
    await CreatePANTokenAction(**params).run()

    mock_get_trust_paysys_card_info.assert_called_once_with(trust_card_id=params['trust_card_id'])


@pytest.mark.asyncio
async def test_card_not_found(params, mock_get_trust_paysys_card_info, dummy_logs):
    mock_get_trust_paysys_card_info.side_effect = CardNotFoundTrustPaysysError(
        http_code=400,
        status='fail',
        status_desc='card not found',
        http_method='get',
        service='test',
    )

    with pytest.raises(CoreCardNotFoundError):
        await CreatePANTokenAction(**params).run()

    [log] = dummy_logs()
    assert_that(
        log,
        has_properties(
            levelno=logging.ERROR,
            message='TRUST_PAYSYS_CARD_INFO_NOT_FOUND',
            _context=has_entries(
                trust_card_id=params['trust_card_id'],
                psp_id=params['psp'].psp_id,
                message_id=params['message'].message_id,
            ),
        )
    )
