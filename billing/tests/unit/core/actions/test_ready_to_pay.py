import itertools
import uuid

import pytest

from sendr_utils import utcnow

from hamcrest import assert_that, equal_to

from billing.yandex_pay.yandex_pay.base.entities.enums import PaymentMethodType
from billing.yandex_pay.yandex_pay.core.actions.ready_to_pay import ReadyToPayAction
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enums import AuthMethod, CardNetwork, TSPType
from billing.yandex_pay.yandex_pay.core.entities.merchant import Merchant
from billing.yandex_pay.yandex_pay.core.entities.merchant_origin import MerchantOrigin
from billing.yandex_pay.yandex_pay.core.entities.payment_sheet import PaymentMethod
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.core.exceptions import (
    CoreInsecureMerchantOriginSchemaError, CoreMerchantOriginNotFound
)
from billing.yandex_pay.yandex_pay.interactions import TrustPaymentsClient
from billing.yandex_pay.yandex_pay.interactions.trust_payments import TrustPaymentMethod
from billing.yandex_pay.yandex_pay.utils.stats import forbidden_user_agent, forbidden_user_agent_os_family
from billing.yandex_pay.yandex_pay.utils.user_agent import UserAgentInfo

OWNER_UID = 5555
PREDEFINED_CARD_ID = 'aaf024bb-2f0e-4cad-9010-40328ffcae9a'
MERCHANT_ID = uuid.UUID('789b29e6-d8f2-4e14-8c3f-33679ca590e3')
MERCHANT_ORIGIN = 'https://market.yandex.ru'
MERCHANT_ORIGIN_CANONICAL = 'https://market.yandex.ru:443'
TRUST_CARD_ID = 'card-x1a1234567a12abcd12345a1a'


@pytest.fixture
def user():
    return User(OWNER_UID)


@pytest.fixture(autouse=True)
async def merchant(storage):
    return await storage.merchant.create(
        Merchant(
            merchant_id=MERCHANT_ID,
            name='the-name',
        )
    )


@pytest.fixture(autouse=True)
async def merchant_origin(storage, merchant: Merchant):
    return await storage.merchant_origin.create(MerchantOrigin(
        merchant_id=merchant.merchant_id,
        origin=MERCHANT_ORIGIN_CANONICAL,
    ))


@pytest.fixture(autouse=True)
async def card(storage):
    return await storage.card.create(
        Card(
            trust_card_id=TRUST_CARD_ID,
            owner_uid=OWNER_UID,
            tsp=TSPType.MASTERCARD,
            expire=utcnow(),
            last4='0000',
            card_id=uuid.UUID(PREDEFINED_CARD_ID),
        )
    )


@pytest.fixture(autouse=True)
def payment_method():
    return PaymentMethod(
        method_type=PaymentMethodType.CARD,
        gateway='some_gateway',
        gateway_merchant_id='some_gateway_merchant_id',
        allowed_card_networks=[CardNetwork.MASTERCARD],
        allowed_auth_methods=[AuthMethod.PAN_ONLY, AuthMethod.CLOUD_TOKEN],
    )


@pytest.fixture
def trust_payment_method():
    return TrustPaymentMethod(
        id=TRUST_CARD_ID,
        card_id=TRUST_CARD_ID,
        binding_systems=['trust'],
        orig_uid=str(OWNER_UID),
        payment_method='card',
        system='MasterCard',
        payment_system='MasterCard',
        expiration_month='9',
        expiration_year='2099',
        card_bank='SBERBANK OF RUSSIA',
        expired=False,
        account='1111****1234',
        last_paid_ts=utcnow(),
        binding_ts=utcnow(),
    )


@pytest.fixture(autouse=True)
def mock_trust_gateway_lpm(mocker, trust_payment_method):
    return mocker.patch.object(
        TrustPaymentsClient,
        'get_payment_methods',
        mocker.AsyncMock(
            return_value=[trust_payment_method],
        ),
    )


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'settings_to_overwrite',
    [{'MERCHANT_ORIGIN_VALIDATION_IS_MANDATORY': True}],
    indirect=True,
)
async def test_ready_to_pay__when_user_has_suitable_payment_method(
    mocker, trust_payment_method, payment_method, user
):
    mocker.patch.object(
        TrustPaymentsClient,
        'get_payment_methods',
        mocker.AsyncMock(
            return_value=[trust_payment_method],
        ),
    )

    result = await ReadyToPayAction(
        user=user,
        merchant_id=MERCHANT_ID,
        merchant_origin=MERCHANT_ORIGIN,
        payment_methods=[payment_method],
        existing_payment_method_required=True,
    ).run()

    assert result['is_ready_to_pay']


@pytest.mark.parametrize('user_agent_is_allowed', [True, False])
@pytest.mark.asyncio
async def test_ready_to_pay_checks_user_agent_info(
    mocker,
    trust_payment_method,
    user_agent_is_allowed,
    user,
):
    """
    Action использует check_user_agent_is_allowed и ориентируется на его вердикт.
    """
    action_user_agent_check_mock = mocker.patch.object(
        ReadyToPayAction,
        'check_user_agent_is_allowed',
        mocker.Mock(
            return_value=user_agent_is_allowed,
        ),
    )

    result = await ReadyToPayAction(
        user=user,
        merchant_id=MERCHANT_ID,
        merchant_origin=MERCHANT_ORIGIN,
        payment_methods=[],
        existing_payment_method_required=False,
    ).run()

    action_user_agent_check_mock.assert_called_once()
    assert result['is_ready_to_pay'] == user_agent_is_allowed


class TestCheckUserAgent:
    def test_allowed(self):
        browsers = [
            'Chrome', 'ChromeMobile', 'Chromium', 'YandexBrowser', 'YandexBrowserLite',
            'Safari', 'MobileSafari', 'Firefox', 'YandexSearch',
        ]
        os_families = [
            'iOS', 'Android', 'Bada', 'BlackBerry', 'ChromeOS', 'FirefoxOS', 'FreeBSD', 'Java', 'Linux', 'MacOS',
            'MeeGo', 'NetBSD', 'OpenBSD', 'Orbis', 'RIMTabletOS', 'SunOS', 'Symbian', 'Tizen', 'Unknown',
            'UnknownNix', 'WebOS', 'Windows', 'WindowsMobile', 'WindowsPhone', 'WindowsRT',
        ]

        def allowed():
            yield None
            yield UserAgentInfo(user_agent='', data={})
            for browser_name, os_family in itertools.product(browsers, os_families):
                yield UserAgentInfo(
                    'user_agent',
                    {
                        'BrowserName': browser_name,
                        'OSFamily': os_family,
                    }
                )

        for user_agent_info in allowed():
            assert ReadyToPayAction.check_user_agent_is_allowed(user_agent_info)

    @pytest.mark.parametrize('browsers, os_families', (
        pytest.param(
            ['YandexLauncher'],
            [
                'iOS', 'Android', 'Bada', 'BlackBerry', 'ChromeOS', 'FirefoxOS', 'FreeBSD', 'Java', 'Linux', 'MacOS',
                'MeeGo', 'NetBSD', 'OpenBSD', 'Orbis', 'RIMTabletOS', 'SunOS', 'Symbian', 'Tizen', 'Unknown',
                'UnknownNix', 'WebOS', 'Windows', 'WindowsMobile', 'WindowsPhone', 'WindowsRT',
            ],
            id='test-forbidden-browsers',
        ),
    ))
    def test_not_allowed(self, browsers, os_families):

        def not_allowed():
            for browser_name, os_family in itertools.product(browsers, os_families):
                yield UserAgentInfo(
                    'user_agent',
                    {
                        'BrowserName': browser_name,
                        'OSFamily': os_family,
                    },
                )

        for user_agent_info in not_allowed():
            assert not ReadyToPayAction.check_user_agent_is_allowed(user_agent_info)

        opera = {'Opera', 'Opera Touch', 'OperaMobile', 'OperaMini'}
        for o in opera:
            assert not ReadyToPayAction.check_user_agent_is_allowed(
                UserAgentInfo('user_agent', {
                    'BrowserName': o,
                    'OSFamily': 'iOS',
                })
            )


@pytest.mark.asyncio
async def test_now_allowed_should_increment_forbidden_browser_metrics(mocker, user):
    os = 'MyOsFamily'

    prev_forbidden = forbidden_user_agent.get()[0][1]
    prev_forbidden_os_faimly = forbidden_user_agent_os_family.labels(os).get()[0][1]

    mocker.patch.object(
        ReadyToPayAction,
        'check_user_agent_is_allowed',
        mocker.Mock(
            return_value=False,
        ),
    )

    await ReadyToPayAction(
        user=user,
        merchant_id=MERCHANT_ID,
        merchant_origin=MERCHANT_ORIGIN,
        payment_methods=[],
        existing_payment_method_required=False,
        user_agent_info=UserAgentInfo('ua', {'BrowserName': 'ForbiddenBrowser', 'OSFamily': os}),
    ).run()

    assert forbidden_user_agent.get()[0][1] - prev_forbidden == 1
    assert forbidden_user_agent_os_family.labels(os).get()[0][1] - prev_forbidden_os_faimly == 1


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'settings_to_overwrite',
    [{'MERCHANT_ORIGIN_VALIDATION_IS_MANDATORY': True}],
    indirect=True,
)
async def test_not_ready_to_pay__when_no_trust_payment_methods(
    mocker, payment_method, user
):
    """
    Временный тест на промежуточное поведение:
    пока просто проверяем, что список методов оплаты юзера не пуст.
    """

    mocker.patch.object(
        TrustPaymentsClient,
        'get_payment_methods',
        mocker.AsyncMock(
            return_value=[],
        ),
    )

    result = await ReadyToPayAction(
        user=user,
        merchant_id=MERCHANT_ID,
        merchant_origin=MERCHANT_ORIGIN,
        payment_methods=[payment_method],
        existing_payment_method_required=True,
    ).run()

    assert not result['is_ready_to_pay']


@pytest.mark.asyncio
@pytest.mark.parametrize('settings_to_overwrite', [{'MERCHANT_ORIGIN_VALIDATION_IS_MANDATORY': True}], indirect=True)
@pytest.mark.parametrize('existing_method_required, expected_result', (
    (True, False),
    (False, True),
))
async def test_ready_to_pay__when_no_user(
    mocker,
    payment_method,
    existing_method_required,
    expected_result,
):
    trust_mock = mocker.patch.object(TrustPaymentsClient, 'get_payment_methods', mocker.AsyncMock())

    result = await ReadyToPayAction(
        user=None,
        merchant_id=MERCHANT_ID,
        merchant_origin=MERCHANT_ORIGIN,
        payment_methods=[payment_method],
        existing_payment_method_required=existing_method_required,
    ).run()

    assert_that(result['is_ready_to_pay'], equal_to(expected_result))
    trust_mock.assert_not_called()


@pytest.mark.asyncio
@pytest.mark.parametrize('settings_to_overwrite', [{'MERCHANT_ORIGIN_VALIDATION_IS_MANDATORY': False}], indirect=True)
@pytest.mark.parametrize('merchant_origin', [
    None,
    'http://someshop.market.yandex.ru',
    'https://someshop.market.yandex.ru',
    'https://someshop.market.yandex.ru:443',
])
async def test_merchant_origin_ignored(merchant_origin, user):
    result = await ReadyToPayAction(
        user=user,
        merchant_id=MERCHANT_ID,
        merchant_origin=merchant_origin,
        payment_methods=[],
        existing_payment_method_required=False,
    ).run()

    assert result['is_ready_to_pay']


@pytest.mark.asyncio
@pytest.mark.parametrize('settings_to_overwrite', [
    {'MERCHANT_ORIGIN_VALIDATION_IS_MANDATORY': True}
], indirect=True)
@pytest.mark.parametrize('merchant_origin', [
    None,
    'https://someshop.market.yandex.ru',
    'https://someshop.market.yandex.ru:443',
])
async def test_merchant_origin_not_found_error(merchant_origin, user):
    """
    Если передан какой-то ориджин, то валидация происходит всегда.
    """
    with pytest.raises(CoreMerchantOriginNotFound):
        await ReadyToPayAction(
            user=user,
            merchant_id=MERCHANT_ID,
            merchant_origin=merchant_origin,
            payment_methods=[],
            existing_payment_method_required=False,
        ).run()


@pytest.mark.asyncio
@pytest.mark.parametrize('settings_to_overwrite', [
    {'MERCHANT_ORIGIN_VALIDATION_IS_MANDATORY': True},
], indirect=True)
@pytest.mark.parametrize('merchant_origin', [
    'http://market.yandex.ru',
    'ftp://market.yandex.ru',
    'market.yandex.ru',
    'market.yandex.ru:9000',
])
async def test_merchant_origin_insecure_schema_error(merchant_origin, user):
    """
    Если передан какой-то ориджин, но схема не HTTPS,
    то будет ошибка схемы в ориджине.
    """
    with pytest.raises(CoreInsecureMerchantOriginSchemaError):
        await ReadyToPayAction(
            user=user,
            merchant_id=MERCHANT_ID,
            merchant_origin=merchant_origin,
            payment_methods=[],
            existing_payment_method_required=False,
        ).run()
