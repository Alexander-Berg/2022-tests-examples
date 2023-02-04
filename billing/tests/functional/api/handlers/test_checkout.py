import asyncio
import json
import re
import uuid
from base64 import b64encode
from datetime import timedelta
from decimal import Decimal

import pytest
from cryptography.fernet import Fernet

from sendr_auth import CsrfChecker
from sendr_auth.entities import AuthenticationMethod
from sendr_taskqueue.worker.storage.db.entities import TaskState
from sendr_utils import alist, utcnow

from hamcrest import all_of, assert_that, equal_to, has_entries, has_properties, instance_of

from billing.yandex_pay.yandex_pay.base.entities.enums import PaymentMethodType
from billing.yandex_pay.yandex_pay.core.actions import checkout
from billing.yandex_pay.yandex_pay.core.actions.plus_backend.create_order import YandexPayPlusCreateOrderAction
from billing.yandex_pay.yandex_pay.core.actions.psp.create_entity import create_psp_entity
from billing.yandex_pay.yandex_pay.core.entities.card import Card
from billing.yandex_pay.yandex_pay.core.entities.enrollment import Enrollment
from billing.yandex_pay.yandex_pay.core.entities.enums import TSPTokenStatus, TSPType
from billing.yandex_pay.yandex_pay.core.entities.merchant import Merchant
from billing.yandex_pay.yandex_pay.core.entities.merchant_origin import MerchantOrigin
from billing.yandex_pay.yandex_pay.core.entities.psp import PSP
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.file_storage.geobase import GeobaseStorage
from billing.yandex_pay.yandex_pay.interactions import UnifiedAgentMetricPushClient
from billing.yandex_pay.yandex_pay.tests.entities import APIKind
from billing.yandex_pay.yandex_pay.tests.matchers import convert_then_match

TOKEN_MOCK = 'le-mo-na-de'
YANDEX_PAY_TRUST_CARD_ID = 'card-x123abc'


@pytest.fixture(params=(APIKind.WEB, APIKind.MOBILE))
def api_kind(request):
    return request.param


@pytest.fixture
def auth_method(api_kind):
    if api_kind == APIKind.WEB:
        return AuthenticationMethod.SESSION
    return AuthenticationMethod.OAUTH


@pytest.fixture
def api_url(api_kind):
    return {
        APIKind.WEB: '/api/v1/checkout',
        APIKind.MOBILE: '/api/mobile/v1/checkout',
    }[api_kind]


@pytest.fixture
def owner_uid(randn):
    return randn()


@pytest.fixture
def user(owner_uid):
    return User(owner_uid)


@pytest.fixture
def predefined_card_id():
    return str(uuid.uuid4())


@pytest.fixture
def merchant_id():
    return str(uuid.uuid4())


@pytest.fixture
def psp_external_id(rands):
    return rands()


@pytest.fixture(autouse=True)
async def merchant(storage, rands, merchant_id):
    return await storage.merchant.create(
        Merchant(
            merchant_id=uuid.UUID(merchant_id),
            name=rands(),
        )
    )


@pytest.fixture(autouse=True)
async def merchant_origin(storage, merchant: Merchant):
    return await storage.merchant_origin.create(MerchantOrigin(
        merchant_id=merchant.merchant_id,
        origin='https://rotten-fruits.gov:443',
    ))


@pytest.fixture(autouse=True)
async def psp(storage, psp_external_id):
    return await create_psp_entity(
        storage,
        PSP(
            psp_id=uuid.uuid4(),
            psp_external_id=psp_external_id,
            public_key='public-key',
            public_key_signature='public-key-signature',
        )
    )


@pytest.fixture
def fake_plus_order(randn, owner_uid, merchant_id, psp):
    return dict(
        order_id=randn(),
        uid=owner_uid,
        message_id='fake_message_id',
        currency='RUB',
        amount='100.00',
        cashback='5.00',
        cashback_category='0.05',
        status='new',
        psp_id=str(psp.psp_id),
        merchant_id=merchant_id,
        payment_method_type=PaymentMethodType.CARD.value,
    )


@pytest.fixture(autouse=True)
def pay_plus_backend_mock(aioresponses_mocker, fake_plus_order):
    return aioresponses_mocker.post(
        re.compile(r'^.+/api/v1/orders$'),
        status=200,
        payload={'data': fake_plus_order},
    )


@pytest.fixture
def antifraud_cashback_backend_mock(aioresponses_mocker, yandex_pay_settings):
    return aioresponses_mocker.post(
        f'{yandex_pay_settings.ANTIFRAUD_API_URL}/score',
        status=200,
        payload={
            'status': 'success',
            'action': 'ALLOW',
            'tags': [],
        },
    )


@pytest.fixture
def authentication(
    api_kind, app, yandex_pay_settings, owner_uid, user, aioresponses_mocker
):
    if api_kind == APIKind.WEB:
        aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.BLACKBOX_API_URL}.*method=sessionid.*'),
            status=200,
            payload={
                'status': {'value': 'VALID'},
                'uid': {'value': owner_uid},
                'login_id': 'login_id',
            }
        )

        key = app.server.app.file_storage.csrf_anti_forgery_key.get_actual_key()
        return {
            'headers': {
                yandex_pay_settings.API_CSRF_TOKEN_HEADER: CsrfChecker.generate_token(
                    timestamp=int(utcnow().timestamp()),
                    key=key,
                    user=user,
                    yandexuid='yandexuid'
                ),
            },
            'cookies': {
                'Session_id': 'sessionid',
                'yandexuid': 'yandexuid',
            },
        }

    aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_settings.BLACKBOX_API_URL}.*method=oauth.*'),
        status=200,
        payload={
            'status': {'value': 'VALID'},
            'oauth': {'uid': owner_uid, 'client_id': 'client_id'},
            'login_id': 'login_id',
        }
    )
    return {
        'headers': {
            'Authorization': 'OAuth 123',
        }
    }


@pytest.fixture(autouse=True)
async def old_tasks(storage):
    old_tasks_ids = {
        task.task_id
        async for task in storage.task.find()
    }
    yield old_tasks_ids

    filters = {'task_id': lambda field: ~field.in_(old_tasks_ids)}
    async for task in storage.task.find(filters=filters):
        await storage.task.delete(task)


@pytest.fixture
def card_expiration_date():
    return utcnow() + timedelta(days=365)


@pytest.fixture(autouse=True)
async def card(storage, owner_uid, predefined_card_id, card_expiration_date):
    return await storage.card.create(
        Card(
            trust_card_id=YANDEX_PAY_TRUST_CARD_ID,
            owner_uid=owner_uid,
            tsp=TSPType.MASTERCARD,
            expire=card_expiration_date,
            last4='0000',
            card_id=uuid.UUID(predefined_card_id),
        )
    )


@pytest.fixture(autouse=True)
async def enrollment_ecommerce(storage, card, card_expiration_date):
    assert card.card_id
    return await storage.enrollment.create(
        Enrollment(
            card_id=card.card_id,
            merchant_id=None,
            tsp_token_status=TSPTokenStatus.ACTIVE,
            tsp_card_id=None,
            tsp_token_id=str(uuid.uuid4()),
            expire=card_expiration_date,
            card_last4=card.last4,
        )
    )


@pytest.fixture
def uaas_headers(yandex_pay_settings):
    pay_testitem = [
        {
            'HANDLER': yandex_pay_settings.API_UAAS_HANDLER,
            'CONTEXT': {
                'MAIN': {
                    'YANDEX_PAY_BACKEND': {'yandex_pay_plus.cashback_category': '0.15'}
                }
            }
        }
    ]
    other_testitem = [
        {
            'HANDLER': 'OTHER',
            'CONTEXT': {
                'MAIN': {
                    'OTHER': {'setting': 'fake'}
                }
            }
        }
    ]
    flags = ','.join(
        b64encode(json.dumps(each).encode()).decode()
        for each in (other_testitem, pay_testitem)
    )
    return {
        'X-Yandex-ExpFlags': flags,
        'X-Yandex-ExpBoxes': '398290,0,-1;398773,0,-1',
    }


@pytest.fixture
def sheet(merchant_id, psp_external_id):
    return {
        'version': 2,
        'currency_code': 'RUB',
        'country_code': 'ru',
        'merchant': {
            'id': merchant_id,
            'name': 'merchant-name',
            'url': 'https://url.test',
        },
        'order': {
            'id': 'order-id',
            'total': {
                'amount': '1.00',
            },
        },
        'payment_methods': [{
            'type': 'CARD',
            'gateway': psp_external_id,
            'gateway_merchant_id': 'hmnid',
            'allowed_auth_methods': ['CLOUD_TOKEN', 'PAN_ONLY'],
            'allowed_card_networks': ['MASTERCARD'],
        }],
    }


@pytest.fixture
def turn_off_tokens(yandex_pay_settings):
    yandex_pay_settings.API_TOKENS_ENABLED = False


@pytest.fixture(autouse=True)
def mock_ua_push_client(mocker):
    mock = mocker.AsyncMock(return_value=None)
    return mocker.patch.object(UnifiedAgentMetricPushClient, 'send', mock)


def trust_gateway_response(card_id, card_network, last4, owner_uid):
    return {
        'status': 'success',
        'bound_payment_methods': [{
            'region_id': 225,
            'payment_method': 'card',
            'system': card_network,
            'expiration_month': '01',
            'card_country': 'RUS',
            'binding_ts': '1586458392.247',
            'ebin_tags_version': 0,
            'card_level': 'STANDARD',
            'holder': 'Card Holder',
            'id': card_id,
            'payment_system': card_network,
            'last_paid_ts': '1586458392.247',
            'account': f'****{last4}',
            'ebin_tags': [],
            'expiration_year': '2030',
            'aliases': [
                card_id,
            ],
            'expired': False,
            'card_bank': 'SBERBANK OF RUSSIA',
            'card_id': card_id,
            'recommended_verification_type': 'standard2',
            'orig_uid': str(owner_uid),
            'binding_systems': [
                'trust'
            ]
        }],
    }


def _get_pending_tasks():
    return [
        task for task in asyncio.all_tasks()
        if task.get_name() in {checkout.CREATE_PLUS_ORDER_TASK_NAME}
    ]


@pytest.mark.asyncio
async def test_unauthorized(
    app,
    api_url,
    yandex_pay_settings,
    aioresponses_mocker,
    predefined_card_id,
    sheet,
):
    params = {
        'card_id': predefined_card_id,
        'sheet': sheet,
    }
    aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_settings.BLACKBOX_API_URL}.*'),
        status=200,
        payload={'status': {'value': 'INVALID', 'id': 5}, 'error': 'signature has bad format or is broken'},
    )

    r = await app.post(
        api_url,
        json=params,
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(401))
    assert_that(
        json_body,
        equal_to({
            'code': 401,
            'status': 'fail',
            'data': {
                'message': 'MISSING_CREDENTIALS',
            }
        })
    )


@pytest.mark.asyncio
@pytest.mark.usefixtures('turn_off_tokens')
async def test_should_respond_bad_request_if_tokens_are_disabled(
    app,
    api_url,
    yandex_pay_settings,
    aioresponses_mocker,
    predefined_card_id,
    sheet,
    authentication,
):
    params = {
        'card_id': predefined_card_id,
        'sheet': sheet,
    }
    aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_settings.TRUST_PAYMENTS_API_URL}.*'),
        status=200,
        payload={
            'status': 'success',
            'bound_payment_methods': [],
        },
    )

    r = await app.post(
        api_url,
        json=params,
        **authentication,
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(400))
    assert_that(
        json_body,
        equal_to({
            'code': 400,
            'status': 'fail',
            'data': {
                'message': 'INVALID_AUTH_METHOD',
            }
        })
    )


@pytest.mark.asyncio
async def test_restricted_region(
    app,
    api_url,
    yandex_pay_settings,
    aioresponses_mocker,
    card,
    predefined_card_id,
    authentication,
    sheet,
):
    params = {
        'card_id': predefined_card_id,
        'sheet': sheet,
    }

    aioresponses_mocker.get(
        re.compile(f'^{yandex_pay_settings.TRUST_PAYMENTS_API_URL}.*'),
        status=200,
        payload=trust_gateway_response(
            card_id=card.trust_card_id,
            card_network=card.tsp.value.upper(),
            last4=card.last4,
            owner_uid=owner_uid,
        ),
    )

    yandex_pay_settings.API_CHECK_REGION = True

    region_id = 4030
    app.server.app.file_storage.geobase = GeobaseStorage(forbidden_regions=[{'region_id': region_id}])
    authentication.setdefault('headers', {}).update({'x-region-id': str(region_id), 'x-region-suspected': '1'})

    r = await app.post(
        api_url,
        json=params,
        **authentication,
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(403))
    assert_that(
        json_body,
        equal_to({
            'code': 403,
            'status': 'fail',
            'data': {
                'message': 'FORBIDDEN_REGION',
            }
        })
    )


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'extra_methods',
    [
        [{'type': 'CASH'}],
        [{'type': 'CASH'}, {'type': 'SPLIT'}],
    ]
)
async def test_cash_payment_type(app, api_url, sheet, authentication, extra_methods):
    sheet['payment_methods'].extend(extra_methods)
    params = {
        'payment_method_type': 'CASH',
        'sheet': sheet,
    }
    r = await app.post(
        api_url,
        json=params,
        **authentication,
    )
    json_body = await r.json()

    assert_that(r.status, equal_to(200))
    assert_that(
        json_body['data'],
        equal_to(
            {
                'payment_token': None,
                'payment_method_info': {'type': 'CASH'},
            }
        )
    )


class TestYandexPayCard:
    @pytest.fixture
    def params(self, predefined_card_id, sheet):
        sheet['payment_methods'][0]['allowed_auth_methods'] = ['CLOUD_TOKEN']
        return {
            'card_id': predefined_card_id,
            'sheet': sheet,
        }

    @pytest.fixture
    def mastercard_checkout_response_body(self):
        return {
            'code': 200,
            'data': {
                'masked_card': {
                    'dateOfCardCreated': '2020-12-05T23:53:59.579Z',
                    'dateOfCardLastUsed': '2020-12-10T13:02:04.196Z',
                    'digitalCardData': {
                        'artUri': (
                            'https://sbx.assets.mastercard.com/'
                            'card-art/combined-image-asset/c911d734-2f4b-4326-8363-afb0ffe2bc2b.png'
                        ),
                        'descriptorName': 'Example Bank Product Configuration',
                        'status': 'ACTIVE',
                    },
                    'maskedBillingAddress': None,
                    'panBin': '520473',
                    'panExpirationMonth': '01',
                    'panExpirationYear': '2099',
                    'panLastFour': '4784',
                    'paymentAccountReference': '',
                    'paymentCardDescriptor': '',
                    'paymentCardType': 'CREDIT',
                    'srcDigitalCardId': '198e7a1f-c253-46dc-92d9-520356ed6dee'
                },
                'masked_consumer': {
                    'dateConsumerAdded': '2020-12-05T23:52:14.183Z',
                    'maskedConsumerIdentity': {
                        'identityType': 'EXTERNAL_ACCOUNT_ID',
                        'maskedIdentityValue': '4053525715'
                    },
                    'maskedFullName': '',
                    'maskedMobileNumber': {},
                    'srcConsumerId': '11b91ca2-b98e-482f-891c-3c45167b9070',
                    'status': 'ACTIVE'
                },
                'payment_token': TOKEN_MOCK,
            },
            'status': 'success'
        }

    @pytest.fixture
    def expected_json_body(self, card):
        return {
            'code': 200,
            'status': 'success',
            'data': {
                'payment_token': TOKEN_MOCK,
                'payment_method_info': {
                    'type': 'CARD',
                    'card_last4': card.last4,
                    'card_network': card.tsp.value.upper(),
                    'mit_info': {'recurring': False, 'deferred': False},
                }
            }
        }

    @pytest.fixture
    def mock_duckgo_mastercard_checkout(
        self, yandex_pay_settings, aioresponses_mocker, mastercard_checkout_response_body
    ):
        return aioresponses_mocker.post(
            re.compile(f'^{yandex_pay_settings.DUCKGO_API_URL}.*'),
            status=200,
            payload=mastercard_checkout_response_body
        )

    @pytest.fixture(autouse=True)
    async def setup(
        self,
        yandex_pay_settings,
        aioresponses_mocker,
        mastercard_checkout_response_body,
        card,
        antifraud_cashback_backend_mock,
        mock_duckgo_mastercard_checkout
    ):
        yandex_pay_settings.DUCKGO_FERNET_KEY = Fernet.generate_key()

        aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.TRUST_PAYMENTS_API_URL}.*'),
            status=200,
            payload=trust_gateway_response(
                card_id=card.trust_card_id,
                card_network=card.tsp.value.upper(),
                last4=card.last4,
                owner_uid=owner_uid,
            ),
        )

    @pytest.mark.asyncio
    async def test_should_respond_with_payment_token(
        self,
        app,
        api_url,
        yandex_pay_settings,
        params,
        expected_json_body,
        authentication,
        antifraud_cashback_backend_mock,
        pay_plus_backend_mock,
    ):
        yandex_pay_settings.DUCKGO_FERNET_KEY = Fernet.generate_key()

        r = await app.post(
            api_url,
            json=params,
            **authentication,
        )
        json_body = await r.json()

        assert_that(r.status, equal_to(200))
        assert_that(json_body, equal_to(expected_json_body))

    @pytest.mark.asyncio
    async def test_plus_order_task_is_eventually_created(
        self,
        api_url,
        auth_method,
        app,
        params,
        old_tasks,
        yandex_pay_settings,
        owner_uid,
        psp,
        sheet,
        card,
        storage,
        run_action,
        mocker,
        uaas_headers,
        authentication,
        antifraud_cashback_backend_mock,
        pay_plus_backend_mock,
    ):
        yandex_pay_settings.DUCKGO_FERNET_KEY = Fernet.generate_key()
        spy = mocker.spy(checkout, 'generate_message')

        headers = authentication.pop('headers', {}) | uaas_headers
        r = await app.post(
            api_url,
            json=params,
            headers=headers,
            **authentication,
        )
        assert_that(r.status, equal_to(200))

        await asyncio.gather(*_get_pending_tasks())
        # plus backend task
        filters = {
            'task_type': 'run_action',
            'action_name': YandexPayPlusCreateOrderAction.action_name,
            'task_id': lambda field: ~field.in_(old_tasks),
        }
        [plus_backend_task] = await alist(storage.task.find(filters=filters))
        assert_that(
            plus_backend_task,
            has_properties(
                state=TaskState.PENDING,
                params=has_entries(
                    max_retries=10,
                    action_kwargs=has_entries(
                        user={
                            'uid': owner_uid,
                            'login_id': 'login_id',
                            'tvm_ticket': None,
                            'auth_method': auth_method.value,
                            'is_yandexoid': False,
                        },
                        message_id=spy.spy_return.message_id,
                        amount=all_of(
                            instance_of(str),
                            convert_then_match(Decimal, Decimal('1.00')),
                        ),
                        user_ip='127.0.0.1',
                        currency='RUB',
                        psp_id=str(psp.psp_id),
                        merchant={
                            'id': str(sheet['merchant']['id']),
                            'name': sheet['merchant']['name'],
                            'url': sheet['merchant']['url'],
                        },
                        user_agent=instance_of(str),
                        trust_card_id=card.trust_card_id,
                        payment_method_type=PaymentMethodType.CARD.value,
                    )
                )
            )
        )
        await run_action(
            action_cls=YandexPayPlusCreateOrderAction,
            action_kwargs=YandexPayPlusCreateOrderAction.deserialize_kwargs(
                plus_backend_task.params['action_kwargs']
            ),
        )
        # ensure plus back is called
        pay_plus_backend_mock.assert_called_once()

    @pytest.mark.asyncio
    async def test_pay_plus_backend_is_called(
        self,
        app,
        api_url,
        yandex_pay_settings,
        params,
        authentication,
        pay_plus_backend_mock,
        sheet,
        psp,
        owner_uid,
        uaas_headers,
        mocker,
        card,
    ):
        yandex_pay_settings.SHOULD_CREATE_ORDER_IN_PAY_PLUS = True
        yandex_pay_settings.ASYNC_CASHBACK_ORDER_CREATION_ENABLED = False

        spy = mocker.spy(checkout, 'generate_message')

        headers = authentication.pop('headers', {}) | uaas_headers
        r = await app.post(
            api_url,
            json=params,
            headers=headers,
            **authentication,
        )

        assert_that(r.status, equal_to(200))
        await asyncio.gather(*_get_pending_tasks())

        pay_plus_backend_mock.assert_called_once()
        _, call_kwargs = pay_plus_backend_mock.call_args
        assert_that(
            call_kwargs,
            has_entries(
                json=dict(
                    uid=owner_uid,
                    amount='1.00',
                    currency='RUB',
                    psp_id=str(psp.psp_id),
                    merchant={
                        'id': str(sheet['merchant']['id']),
                        'name': sheet['merchant']['name'],
                        'url': sheet['merchant']['url'],
                    },
                    message_id=spy.spy_return.message_id,
                    trust_card_id=YANDEX_PAY_TRUST_CARD_ID,
                    last4=card.last4,
                    cashback_category_id='0.15',
                    country_code='ru',
                    order_basket={'id': 'order-id', 'total': {'amount': '1.00', 'label': None}, 'items': None},
                    card_network='MASTERCARD',
                    card_id=str(card.card_id),
                    antifraud_external_id=None,
                    payment_method_type=PaymentMethodType.CARD.value,
                )
            )
        )

    @pytest.mark.asyncio
    async def test_response_on_blocked_psp(
        self,
        app,
        api_url,
        params,
        storage,
        authentication,
        psp,
    ):
        psp.is_blocked = True
        await storage.psp.save(psp)

        r = await app.post(
            api_url,
            json=params,
            **authentication,
        )
        json_body = await r.json()

        assert_that(r.status, equal_to(403))
        assert_that(json_body, equal_to({
            'status': 'fail',
            'code': 403,
            'data': {
                'message': 'PSP_ACCOUNT_ERROR',
            },
        }))

    @pytest.mark.asyncio
    async def test_response_on_blocked_merchant(
        self,
        app,
        api_url,
        params,
        storage,
        authentication,
        merchant,
    ):
        merchant.is_blocked = True
        await storage.merchant.save(merchant)

        r = await app.post(
            api_url,
            json=params,
            **authentication,
        )
        json_body = await r.json()

        assert_that(r.status, equal_to(403))
        assert_that(json_body, equal_to({
            'status': 'fail',
            'code': 403,
            'data': {
                'message': 'MERCHANT_ACCOUNT_ERROR',
            },
        }))

    @pytest.mark.asyncio
    async def test_when_mit_allowed__mit_info_handled_correctly(
        self,
        app,
        api_url,
        yandex_pay_settings,
        params,
        expected_json_body,
        authentication,
        mock_duckgo_mastercard_checkout,
        pay_plus_backend_mock,
    ):
        params['sheet']['recurring_options'] = {
            'type': 'RECURRING',
        }
        params['mit_customer_choices'] = {'allowed': True}

        r = await app.post(
            api_url,
            json=params,
            **authentication,
            raise_for_status=True
        )

        assert_that(
            mock_duckgo_mastercard_checkout.call_args.kwargs['json']['mit_info'],
            equal_to({'recurring': True, 'deferred': False}),
        )
        data = await r.json()
        assert_that(
            data['data']['payment_method_info']['mit_info'],
            equal_to({'deferred': False, 'recurring': True}),
        )


class TestTrustCard:
    TRUST_CARD_ID = 'card-x1a1234567a12abcd12345a1a'
    TRUST_CARD_OBJECT_ID = '1a1234567a12abcd12345a1a'
    CARD_LAST4 = '4444'
    CARD_NETWORK = 'MASTERCARD'

    @pytest.fixture
    def params(self, api_kind, sheet):
        return {
            'card_id': self.TRUST_CARD_ID,
            'merchant_origin': 'https://rotten-fruits.gov' if api_kind == APIKind.WEB else None,
            'sheet': sheet,
        }

    @pytest.fixture
    def expected_json_body(self, card):
        return {
            'code': 200,
            'status': 'success',
            'data': {
                'payment_token': TOKEN_MOCK,
                'payment_method_info': {
                    'type': 'CARD',
                    'card_last4': self.CARD_LAST4,
                    'card_network': 'MASTERCARD',
                    'mit_info': {'recurring': False, 'deferred': False},
                }
            }
        }

    @pytest.fixture(autouse=True)
    def setup(self, aioresponses_mocker, yandex_pay_settings, owner_uid):
        aioresponses_mocker.post(
            re.compile(f'^{yandex_pay_settings.CARD_PROXY_API_URL}.*'),
            status=200,
            payload={'status': 'success', 'data': {'payment_token': TOKEN_MOCK}}
        )
        aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.TRUST_PAYMENTS_API_URL}.*'),
            status=200,
            payload=trust_gateway_response(
                card_id=self.TRUST_CARD_ID,
                card_network=self.CARD_NETWORK,
                last4=self.CARD_LAST4,
                owner_uid=owner_uid,
            ),
        )
        aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.TRUST_PAYSYS_API_URL}/yapay/v1/cards/{self.TRUST_CARD_OBJECT_ID}'),
            status=200,
            payload={
                'status': 'success',
                'card_id': self.TRUST_CARD_OBJECT_ID,
                'card_token': 'card-token',
                'expiration_month': 1,
                'expiration_year': 2020,
                'holder': None,
            },
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'settings_to_overwrite',
        [{'MERCHANT_ORIGIN_VALIDATION_IS_MANDATORY': True}],
        indirect=True,
    )
    async def test_should_respond_with_payment_token(
        self,
        app,
        api_url,
        yandex_pay_settings,
        params,
        expected_json_body,
        authentication,
        antifraud_cashback_backend_mock,
    ):
        yandex_pay_settings.DUCKGO_FERNET_KEY = Fernet.generate_key()

        r = await app.post(
            api_url,
            json=params,
            **authentication,
        )
        json_body = await r.json()

        assert_that(r.status, equal_to(200))
        assert_that(json_body, equal_to(expected_json_body))

    @pytest.mark.asyncio
    async def test_plus_order_task_is_eventually_created(
        self,
        yandex_pay_settings,
        api_url,
        auth_method,
        app,
        antifraud_cashback_backend_mock,
        authentication,
        pay_plus_backend_mock,
        old_tasks,
        run_action,
        params,
        owner_uid,
        psp,
        sheet,
        storage,
        mocker,
    ):
        yandex_pay_settings.DUCKGO_FERNET_KEY = Fernet.generate_key()
        spy = mocker.spy(checkout, 'generate_message')

        r = await app.post(
            api_url,
            json=params,
            **authentication,
        )
        assert_that(r.status, equal_to(200))

        await asyncio.gather(*_get_pending_tasks())
        # plus backend task
        filters = {
            'task_type': 'run_action',
            'action_name': YandexPayPlusCreateOrderAction.action_name,
            'task_id': lambda field: ~field.in_(old_tasks),
        }
        [plus_backend_task] = await alist(storage.task.find(filters=filters))
        assert_that(
            plus_backend_task,
            has_properties(
                state=TaskState.PENDING,
                params=has_entries(
                    max_retries=10,
                    action_kwargs=has_entries(
                        user={
                            'uid': owner_uid,
                            'login_id': 'login_id',
                            'tvm_ticket': None,
                            'auth_method': auth_method.value,
                            'is_yandexoid': False,
                        },
                        message_id=spy.spy_return.message_id,
                        amount=all_of(
                            instance_of(str),
                            convert_then_match(Decimal, Decimal('1.00')),
                        ),
                        user_ip='127.0.0.1',
                        currency='RUB',
                        psp_id=str(psp.psp_id),
                        merchant={
                            'id': str(sheet['merchant']['id']),
                            'name': sheet['merchant']['name'],
                            'url': sheet['merchant']['url'],
                        },
                        user_agent=instance_of(str),
                        trust_card_id=self.TRUST_CARD_ID,
                        payment_method_type=PaymentMethodType.CARD.value,
                    )
                )
            )
        )
        await run_action(
            action_cls=YandexPayPlusCreateOrderAction,
            action_kwargs=YandexPayPlusCreateOrderAction.deserialize_kwargs(
                plus_backend_task.params['action_kwargs']
            ),
        )

        pay_plus_backend_mock.assert_called_once()


class TestAntifraudChallenges:
    TRUST_CARD_ID = 'card-x1a1234567a12abcd12345a1a'
    TRUST_CARD_OBJECT_ID = '1a1234567a12abcd12345a1a'
    CARD_LAST4 = '4444'
    CARD_NETWORK = 'MASTERCARD'

    @pytest.fixture(autouse=True)
    def setup(self, yandex_pay_settings, aioresponses_mocker, sheet):
        sheet['payment_methods'][0]['verification_details'] = True
        yandex_pay_settings.DUCKGO_FERNET_KEY = Fernet.generate_key()
        aioresponses_mocker.post(
            re.compile(f'^{yandex_pay_settings.CARD_PROXY_API_URL}.*'),
            status=200,
            payload={'status': 'success', 'data': {'payment_token': TOKEN_MOCK}}
        )
        aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.TRUST_PAYMENTS_API_URL}.*'),
            status=200,
            payload=trust_gateway_response(
                card_id=self.TRUST_CARD_ID,
                card_network=self.CARD_NETWORK,
                last4=self.CARD_LAST4,
                owner_uid=owner_uid,
            ),
        )
        aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.TRUST_PAYSYS_API_URL}/yapay/v1/cards/{self.TRUST_CARD_OBJECT_ID}'),
            status=200,
            payload={
                'status': 'success',
                'card_id': self.TRUST_CARD_OBJECT_ID,
                'card_token': 'card-token',
                'expiration_month': 1,
                'expiration_year': 2020,
                'holder': None,
            },
        )

    @pytest.fixture
    def params(self, api_kind, sheet):
        return {
            'card_id': self.TRUST_CARD_ID,
            'merchant_origin': 'https://rotten-fruits.gov',
            'sheet': sheet,
            'challenge_return_path': 'https://sibay.com',
        }

    @pytest.mark.asyncio
    async def test_challenge_is_not_required(
        self,
        app,
        api_url,
        aioresponses_mocker,
        params,
        authentication,
        yandex_pay_settings,
    ):
        aioresponses_mocker.post(
            f'{yandex_pay_settings.ANTIFRAUD_API_URL}/score',
            status=200,
            payload={
                'status': 'success',
                'action': 'ALLOW',
                'tags': [],
            },
        )

        r = await app.post(
            api_url,
            json=params,
            **authentication,
        )

        assert r.status == 200

    @pytest.mark.asyncio
    async def test_challenge_is_required(
        self,
        app,
        api_url,
        aioresponses_mocker,
        params,
        authentication,
        yandex_pay_settings,
    ):
        aioresponses_mocker.post(
            f'{yandex_pay_settings.ANTIFRAUD_API_URL}/score',
            status=200,
            payload={
                'status': 'success',
                'action': 'ALLOW',
                'tags': [{'url': 'some_test_url'}]
            }
        )
        expected_body = {
            'status': 'fail',
            'code': 403,
            'data': {
                'message': 'CHALLENGE_REQUIRED',
                'params': {
                    'challenge_url': 'some_test_url'
                }
            }
        }

        r = await app.post(
            api_url,
            json=params,
            **authentication,
        )
        json_body = await r.json()

        assert r.status == 403
        assert json_body == expected_body

    @pytest.mark.asyncio
    async def test_challenge_denied(
        self,
        app,
        api_url,
        aioresponses_mocker,
        params,
        authentication,
        yandex_pay_settings,
    ):
        aioresponses_mocker.post(
            f'{yandex_pay_settings.ANTIFRAUD_API_URL}/score',
            status=200,
            payload={
                'status': 'success',
                'action': 'DENY',
            }
        )
        expected_body = {
            'status': 'fail',
            'code': 403,
            'data': {
                'message': 'CHALLENGE_DENIED',
            }
        }

        r = await app.post(
            api_url,
            json=params,
            **authentication,
        )
        json_body = await r.json()

        assert r.status == 403
        assert json_body == expected_body

    @pytest.mark.asyncio
    async def test_challenge_fatal_error(
        self,
        app,
        api_url,
        aioresponses_mocker,
        params,
        authentication,
        yandex_pay_settings,
    ):
        aioresponses_mocker.post(
            f'{yandex_pay_settings.ANTIFRAUD_API_URL}/score',
            status=200,
            payload={
                'status': 'error',
            }
        )

        r = await app.post(
            api_url,
            json=params,
            **authentication,
        )

        assert r.status == 500

    @pytest.mark.asyncio
    async def test_challenge_return_url_is_not_passed(
        self,
        app,
        api_url,
        aioresponses_mocker,
        params,
        authentication,
        yandex_pay_settings,
    ):
        del params['challenge_return_path']
        aioresponses_mocker.post(
            f'{yandex_pay_settings.ANTIFRAUD_API_URL}/score',
            status=200,
            payload={
                'status': 'error',
            }
        )
        expected_body = {
            'status': 'fail',
            'code': 400,
            'data': {
                'message': 'CHALLENGE_RETURN_PATH_REQUIRED',
            }
        }

        r = await app.post(
            api_url,
            json=params,
            **authentication,
        )
        json_body = await r.json()

        assert r.status == 400
        assert json_body == expected_body


class TestSplitCheckout:
    @pytest.fixture
    def split_url(self, yandex_pay_settings):
        return re.compile(f'^{yandex_pay_settings.SPLIT_API_URL}/order/create$')

    @pytest.fixture
    def split_response(self):
        order_id = uuid.uuid4()
        return {
            'order_id': str(order_id),
            'checkout_url': f'https://test.bnpl.yandex.ru/checkout/{order_id}',
        }

    @pytest.fixture(autouse=True)
    def mock_split(self, aioresponses_mocker, split_url, split_response):
        return aioresponses_mocker.post(
            url=split_url,
            status=200,
            payload=split_response,
        )

    @pytest.fixture
    def mock_trust_card_proxy(self, aioresponses_mocker, yandex_pay_settings):
        return aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.CARD_PROXY_API_URL}.*'),
            status=200,
            payload={'status': 'success', 'data': {'payment_token': TOKEN_MOCK}},
        )

    @pytest.fixture
    def mock_trust_gateway(self, aioresponses_mocker, yandex_pay_settings, card, owner_uid):
        return aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.TRUST_PAYMENTS_API_URL}.*'),
            status=200,
            payload=trust_gateway_response(
                card_id=card.trust_card_id,
                card_network=card.tsp.value.upper(),
                last4=card.last4,
                owner_uid=owner_uid,
            ),
        )

    @pytest.fixture
    def mock_trust_paysys(self, aioresponses_mocker, yandex_pay_settings, card, owner_uid):
        obj_id = YANDEX_PAY_TRUST_CARD_ID.removeprefix('card-x')
        return aioresponses_mocker.get(
            re.compile(f'^{yandex_pay_settings.TRUST_PAYSYS_API_URL}/yapay/v1/cards/{obj_id}'),
            status=200,
            payload={
                'status': 'success',
                'card_id': obj_id,
                'card_token': 'card-token',
                'expiration_month': 1,
                'expiration_year': 2020,
                'holder': None,
            },
        )

    @pytest.fixture
    def zora_mock(self, aioresponses_mocker, yandex_pay_settings):
        return aioresponses_mocker.post(
            f'{yandex_pay_settings.ZORA_URL}',
            status=200,
            payload={
                'code': 200
            }
        )

    @pytest.fixture
    def expected_handler_response(self, split_response):
        return {
            'code': 200,
            'status': 'success',
            'data': {
                'payment_method_info': {
                    'type': 'SPLIT',
                    'split_meta': split_response,
                    'card_network': 'MASTERCARD',
                    'card_last4': '0000',
                },
                'payment_token': None,
            }
        }

    @pytest.mark.asyncio
    async def test_split_checkout_with_pay_card(
        self,
        app,
        api_url,
        sheet,
        expected_handler_response,
        predefined_card_id,
        authentication,
        mock_trust_gateway,
        zora_mock,
        pay_plus_backend_mock,
        owner_uid,
        psp,
        card,
        antifraud_cashback_backend_mock,
        yandex_pay_settings,
    ):
        sheet['payment_methods'].extend([{'type': 'SPLIT'}, {'type': 'CASH'}])
        params = {
            'payment_method_type': 'SPLIT',
            'sheet': sheet,
            'card_id': predefined_card_id,
        }

        r = await app.post(
            api_url,
            json=params,
            **authentication,
            raise_for_status=True,
        )

        assert_that(await r.json(), equal_to(expected_handler_response))
        mock_trust_gateway.assert_called_once()
        expected_plus_payload = dict(
            uid=owner_uid,
            amount='1.00',
            currency='RUB',
            psp_id=yandex_pay_settings.SPLIT_PSP_INTERNAL_ID,
            merchant={
                'id': str(sheet['merchant']['id']),
                'name': sheet['merchant']['name'],
                'url': sheet['merchant']['url'],
            },
            message_id=f"2:{sheet['merchant']['id']}_{sheet['order']['id']}",
            trust_card_id=YANDEX_PAY_TRUST_CARD_ID,
            last4=card.last4,
            cashback_category_id=None,
            country_code='ru',
            order_basket={'id': sheet['order']['id'], 'total': {'amount': '1.00', 'label': None}, 'items': None},
            card_network='MASTERCARD',
            card_id=str(card.card_id),
            antifraud_external_id=None,
            payment_method_type=PaymentMethodType.SPLIT.value,
        )
        pay_plus_backend_mock.assert_called_once()
        assert_that(
            pay_plus_backend_mock.call_args.kwargs,
            has_entries(json=expected_plus_payload),
        )

    @pytest.mark.asyncio
    async def test_split_checkout_with_trust_card(
        self,
        app,
        api_url,
        sheet,
        expected_handler_response,
        authentication,
        mock_trust_gateway,
        mock_trust_card_proxy,
        mock_trust_paysys,
        zora_mock,
        pay_plus_backend_mock,
        owner_uid,
        psp,
        antifraud_cashback_backend_mock,
        yandex_pay_settings,
        card: Card,
    ):
        sheet['payment_methods'].extend([{'type': 'SPLIT'}, {'type': 'CASH'}])
        params = {
            'payment_method_type': 'SPLIT',
            'sheet': sheet,
            'card_id': card.trust_card_id,
        }

        r = await app.post(
            api_url,
            json=params,
            **authentication,
            raise_for_status=True,
        )

        assert_that(await r.json(), equal_to(expected_handler_response))
        mock_trust_gateway.assert_called_once()
        expected_plus_payload = dict(
            uid=owner_uid,
            amount='1.00',
            currency='RUB',
            psp_id=yandex_pay_settings.SPLIT_PSP_INTERNAL_ID,
            merchant={
                'id': str(sheet['merchant']['id']),
                'name': sheet['merchant']['name'],
                'url': sheet['merchant']['url'],
            },
            message_id=f"2:{sheet['merchant']['id']}_{sheet['order']['id']}",
            trust_card_id=card.trust_card_id,
            last4=card.last4,
            cashback_category_id=None,
            country_code='ru',
            order_basket={'id': sheet['order']['id'], 'total': {'amount': '1.00', 'label': None}, 'items': None},
            card_network='MASTERCARD',
            card_id=None,
            antifraud_external_id=None,
            payment_method_type=PaymentMethodType.SPLIT.value,
        )
        pay_plus_backend_mock.assert_called_once()
        assert_that(
            pay_plus_backend_mock.call_args.kwargs,
            has_entries(json=expected_plus_payload),
        )

    @pytest.mark.asyncio
    async def test_split_checkout_without_card_not_allowed(
        self,
        app,
        api_url,
        sheet,
        expected_handler_response,
        authentication,
        mock_trust_gateway,
        zora_mock,
        pay_plus_backend_mock,
        predefined_card_id,
    ):
        sheet['payment_methods'] = [{'type': 'SPLIT'}, {'type': 'CASH'}]
        params = {
            'payment_method_type': 'SPLIT',
            'sheet': sheet,
        }

        r = await app.post(
            api_url,
            json=params,
            **authentication,
        )

        assert_that(r.status, equal_to(404))
        assert_that(await r.json(), equal_to({
            'status': 'fail',
            'code': 404,
            'data': {
                'message': 'CARD_NOT_FOUND'
            }
        }))
        mock_trust_gateway.assert_not_called()

    @pytest.mark.asyncio
    async def test_split_checkout_merchant_validation_error(
        self,
        app,
        api_url,
        sheet,
        aioresponses_mocker,
        yandex_pay_settings,
        authentication,
        predefined_card_id,
        mock_trust_gateway,
        antifraud_cashback_backend_mock,
    ):
        params = {
            'payment_method_type': 'SPLIT',
            'sheet': sheet,
            'card_id': predefined_card_id,
        }

        aioresponses_mocker.post(
            f'{yandex_pay_settings.ZORA_URL}',
            status=400,
            payload={
                'code': 400,
                'reason': 'some reason',
                'reason_code': 'ORDER_NOT_FOUND',
            }
        )

        r = await app.post(
            api_url,
            json=params,
            **authentication,
        )

        assert_that(r.status, equal_to(400))
        assert_that(await r.json(), equal_to({
            'data': {'message': 'MERCHANT_ORDER_VALIDATION_ERROR'},
            'code': 400,
            'status': 'fail',
        }))
