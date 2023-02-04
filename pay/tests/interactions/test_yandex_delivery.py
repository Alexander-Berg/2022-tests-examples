import re
from datetime import datetime, timezone
from decimal import Decimal

import pytest
from aiohttp import TCPConnector

from sendr_pytest.mocks import explain_call_asserts

from hamcrest import assert_that, equal_to, has_entries, match_equality, not_none

from pay.lib.entities.cart import ItemReceipt
from pay.lib.entities.contact import Contact
from pay.lib.entities.enums import DeliveryCategory
from pay.lib.entities.receipt import TaxType
from pay.lib.entities.shipping import Address, Location, ShippingWarehouse, YandexDeliveryOption
from pay.lib.interactions.yandex_delivery.client import AbstractYandexDeliveryClient, CommonParams
from pay.lib.interactions.yandex_delivery.entities import (
    AcceptClaimResponse,
    CancelClaimResponse,
    CancelState,
    CheckPriceResponse,
    Claim,
    ClaimStatus,
    DeliveryInterval,
    DeliveryMethodInfo,
    ErrorMessage,
    GetDeliveryMethodsResponse,
    Item,
    ItemSize,
    Offer,
    Pricing,
    SameDayDeliveryMethodInfo,
)
from pay.lib.interactions.yandex_delivery.exceptions import (
    BadRequestInteractionError,
    CancelNotAvailableInteractionError,
    UnauthorizedInteractionError,
    UnknownInteractionError,
)

dummy_use_fixture = [explain_call_asserts]

YANDEX_DELIVERY_URL = 'https://ydlv.test'


class YandexDeliveryTestClient(AbstractYandexDeliveryClient):
    BASE_URL = YANDEX_DELIVERY_URL
    DEBUG = False
    REQUEST_RETRY_TIMEOUTS = ()
    CONNECTOR = TCPConnector()


@pytest.mark.parametrize(
    'interaction_method, error_code, expected_exc',
    (
        (
            'undefined',
            'unauthorized',
            UnauthorizedInteractionError,
        ),
        (
            'undefined',
            '400',
            BadRequestInteractionError,
        ),
        (
            'undefined',
            '409',
            UnknownInteractionError,
        ),
        (
            'undefined',
            'free_cancel_is_unavailable',
            CancelNotAvailableInteractionError,
        ),
        (
            'cancel_claim',
            'inappropriate_status',
            CancelNotAvailableInteractionError,
        ),
    ),
)
@pytest.mark.asyncio
async def test_common_errors(aioresponses_mocker, delivery_client, error_code, interaction_method, expected_exc):
    url = 'https://error.test'
    aioresponses_mocker.get(
        url,
        status=400,
        payload={
            'code': error_code,
            'message': 'massage',
        },
    )

    with pytest.raises(expected_exc):
        await delivery_client.get(
            interaction_method, url, common_client_params=CommonParams(language='', auth_token='')
        )


class TestGetDeliveryMethods:
    @pytest.mark.asyncio
    async def test_request(self, delivery_client, params, delivery_mock):
        await delivery_client.get_delivery_methods(**params)

        delivery_mock.assert_called_once_with(
            headers={
                'Accept-Language': 'ru',
                'Authorization': 'Bearer tokeh',
                'X-Request-Id': match_equality(not_none()),
            },
            json={'start_point': [0.12345, 50.403020]},
        )

    @pytest.mark.asyncio
    async def test_request_without_optional(self, delivery_client, params, delivery_mock):
        REQUIRED_PARAMS = ('auth_token', 'start_point')
        params = {k: v for k, v in params.items() if k in REQUIRED_PARAMS}

        await delivery_client.get_delivery_methods(**params)

        delivery_mock.assert_called_once_with(
            headers={
                'Accept-Language': 'en',
                'Authorization': 'Bearer tokeh',
                'X-Request-Id': match_equality(not_none()),
            },
            json={'start_point': [0.12345, 50.403020]},
        )

    @pytest.mark.asyncio
    async def test_response(self, delivery_client, params, delivery_mock):
        response = await delivery_client.get_delivery_methods(**params)

        assert_that(
            response,
            equal_to(
                GetDeliveryMethodsResponse(
                    express_delivery=DeliveryMethodInfo(allowed=False),
                    same_day_delivery=SameDayDeliveryMethodInfo(
                        allowed=True,
                        available_intervals=[
                            DeliveryInterval(
                                from_=datetime(2022, 1, 1, 0, 0, 0, tzinfo=timezone.utc),
                                to=datetime(2022, 1, 1, 6, 0, 0, tzinfo=timezone.utc),
                            )
                        ],
                    ),
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_response_without_optionals(self, delivery_client, params, aioresponses_mocker):
        aioresponses_mocker.post(
            re.compile(rf'{YANDEX_DELIVERY_URL}/v1/delivery-methods(\?.*)?'),
            status=200,
            payload={
                'express_delivery': {'allowed': False},
                'same_day_delivery': {'allowed': False},
            },
        )

        response = await delivery_client.get_delivery_methods(**params)

        assert_that(
            response,
            equal_to(
                GetDeliveryMethodsResponse(
                    express_delivery=DeliveryMethodInfo(allowed=False),
                    same_day_delivery=SameDayDeliveryMethodInfo(allowed=False),
                )
            ),
        )

    @pytest.fixture
    def params(self):
        return {
            'auth_token': 'tokeh',
            'start_point': Location(longitude=0.12345, latitude=50.403020),
            'language': 'ru',
        }

    @pytest.fixture
    def delivery_mock(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            re.compile(rf'{YANDEX_DELIVERY_URL}/v1/delivery-methods(\?.*)?'),
            status=200,
            payload={
                'express_delivery': {'allowed': False},
                'same_day_delivery': {
                    'allowed': True,
                    'available_intervals': [{'from': '2022-01-01T00:00:00+00:00', 'to': '2022-01-01T06:00:00+00:00'}],
                },
            },
        )


class TestCheckPrice:
    @pytest.mark.asyncio
    async def test_request(self, delivery_client, params, delivery_mock):
        await delivery_client.check_price(**params)

        delivery_mock.assert_called_once_with(
            headers={
                'Accept-Language': 'ru',
                'Authorization': 'Bearer tokeh',
                'X-Request-Id': match_equality(not_none()),
            },
            json={
                'items': [
                    {
                        'cost_value': '10.00',
                        'quantity': 1,
                        'pickup_point': 0,
                        'weight': 1.5,
                        'cost_currency': 'RUB',
                        'title': '',
                        'droppof_point': 1,
                        'size': {'length': 3.0, 'width': 2.0, 'height': 1.5},
                    }
                ],
                'route_points': [{'coordinates': [0.12345, 50.403020]}, {'coordinates': [0, 90]}],
                'requirements': {
                    'same_day_data': {
                        'delivery_interval': {
                            'from': '2022-01-01T00:00:00+00:00',
                            'to': '2022-01-01T06:00:00+00:00',
                        }
                    }
                },
            },
        )

    @pytest.mark.asyncio
    async def test_request_without_optionals(self, delivery_client, params, delivery_mock):
        REQUIRED_PARAMS = ('auth_token', 'items', 'route_points')
        params = {k: v for k, v in params.items() if k in REQUIRED_PARAMS}

        await delivery_client.check_price(**params)

        delivery_mock.assert_called_once_with(
            headers={
                'Accept-Language': 'en',
                'Authorization': 'Bearer tokeh',
                'X-Request-Id': match_equality(not_none()),
            },
            json={
                'items': [
                    {
                        'quantity': 1,
                        'title': '',
                        'weight': 1.5,
                        'cost_value': '10.00',
                        'size': {'length': 3.0, 'height': 1.5, 'width': 2.0},
                        'droppof_point': 1,
                        'cost_currency': 'RUB',
                        'pickup_point': 0,
                    }
                ],
                'route_points': [{'coordinates': [0.12345, 50.403020]}, {'coordinates': [0, 90]}],
                'requirements': {},
            },
        )

    @pytest.mark.asyncio
    async def test_response(self, delivery_client, params):
        response = await delivery_client.check_price(**params)

        assert_that(
            response,
            equal_to(
                CheckPriceResponse(
                    price=Decimal('10.00'),
                )
            ),
        )

    @pytest.fixture
    def params(self):
        return {
            'auth_token': 'tokeh',
            'items': [
                Item(
                    cost_currency='RUB',
                    cost_value='10.00',
                    droppof_point=1,
                    pickup_point=0,
                    title='',
                    size=ItemSize(height=1.5, width=2, length=3),
                    weight=1.5,
                    quantity=1,
                )
            ],
            'route_points': [Location(longitude=0.12345, latitude=50.403020), Location(longitude=0, latitude=90)],
            'language': 'ru',
            'same_day_delivery_interval': DeliveryInterval(
                from_=datetime(2022, 1, 1, 0, 0, 0, tzinfo=timezone.utc),
                to=datetime(2022, 1, 1, 6, 0, 0, tzinfo=timezone.utc),
            ),
        }

    @pytest.fixture(autouse=True)
    def delivery_mock(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            re.compile(rf'{YANDEX_DELIVERY_URL}/v1/check-price(\?.*)?'),
            status=200,
            payload={
                'price': '10.00',
            },
        )


class TestCreateClaim:
    @pytest.mark.asyncio
    async def test_request(self, delivery_client, params, delivery_mock):
        await delivery_client.create_claim(**params)

        assert_that(
            delivery_mock.call_args.kwargs,
            has_entries(
                headers={
                    'Accept-Language': 'ru',
                    'Authorization': 'Bearer tokeh',
                    'X-Request-Id': match_equality(not_none()),
                },
                params={
                    'request_id': 'reqid',
                },
                json={
                    'emergency_contact': {'phone': 'phone-2', 'name': 'C D'},
                    'route_points': [
                        {
                            'address': {
                                'fullname': 'Russia, Tver, Sovetskaya, 22',
                                'country': 'Russia',
                                'street': 'Sovetskaya',
                                'city': 'Tver',
                                'coordinates': [35.9065, 56.860486],
                            },
                            'point_id': 0,
                            'visit_order': 1,
                            'contact': {'phone': 'phone-1', 'name': 'A B'},
                            'type': 'source',
                        },
                        {
                            'address': {
                                'fullname': 'Russia, Tver, Sovetskaya, 33',
                                'country': 'Russia',
                                'street': 'Sovetskaya',
                                'city': 'Tver',
                                'coordinates': [35.9, 56.86],
                            },
                            'point_id': 1,
                            'visit_order': 2,
                            'contact': {'phone': 'phone-1', 'name': 'Y R', 'email': 'email'},
                            'type': 'destination',
                        },
                    ],
                    'items': [
                        {
                            'pickup_point': 0,
                            'title': '',
                            'weight': 1.5,
                            'size': {'width': 2.0, 'height': 1.5, 'length': 3.0},
                            'cost_value': '10.00',
                            'quantity': 1,
                            'droppof_point': 1,
                            'cost_currency': 'RUB',
                        }
                    ],
                    'same_day_data': {
                        'delivery_interval': {'from': '2022-01-01T00:00:00+00:00', 'to': '2022-01-01T06:00:00+00:00'}
                    },
                },
            ),
        )

    @pytest.mark.asyncio
    async def test_response(self, delivery_client, params):
        response = await delivery_client.create_claim(**params)

        assert_that(
            response,
            equal_to(
                Claim(
                    id='claim-id',
                    version=3,
                    revision=1,
                    status=ClaimStatus.ESTIMATING,
                    updated_ts=datetime(2022, 2, 22, tzinfo=timezone.utc),
                )
            ),
        )

    @pytest.fixture
    def params(self):
        return {
            'auth_token': 'tokeh',
            'request_id': 'reqid',
            'items': [
                Item(
                    cost_currency='RUB',
                    cost_value='10.00',
                    droppof_point=1,
                    pickup_point=0,
                    title='',
                    size=ItemSize(height=1.5, width=2, length=3),
                    weight=1.5,
                    quantity=1,
                )
            ],
            'warehouse': ShippingWarehouse(
                address=Address(
                    country='Russia',
                    locality='Tver',
                    street='Sovetskaya',
                    building='22',
                    location=Location(56.860486, 35.906500),
                ),
                contact=Contact(phone='phone-1', first_name='A', last_name='B'),
                emergency_contact=Contact(phone='phone-2', first_name='C', last_name='D'),
            ),
            'shipping_address': Address(
                country='Russia',
                locality='Tver',
                street='Sovetskaya',
                building='33',
                location=Location(56.86, 35.9),
            ),
            'shipping_contact': Contact(phone='phone-1', email='email', first_name='Y', last_name='R'),
            'delivery_option': YandexDeliveryOption(
                amount=Decimal(1),
                category=DeliveryCategory.TODAY,
                title='TODAY',
                allowed_payment_methods=[],
                yandex_delivery_option_id='opt-id',
                receipt=ItemReceipt(tax=TaxType.VAT_20),
                from_datetime=datetime(2022, 1, 1, 0, 0, 0, tzinfo=timezone.utc),
                to_datetime=datetime(2022, 1, 1, 6, 0, 0, tzinfo=timezone.utc),
            ),
            'language': 'ru',
        }

    @pytest.fixture(autouse=True)
    def delivery_mock(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            re.compile(rf'{YANDEX_DELIVERY_URL}/v2/claims/create(\?.*)?'),
            status=200,
            payload={
                'id': 'claim-id',
                'version': 3,
                'revision': 1,
                'status': 'estimating',
                'updated_ts': '2022-02-22T00:00:00+00:00',
            },
        )


class TestGetClaim:
    @pytest.mark.asyncio
    async def test_request(self, delivery_client, delivery_mock, params):
        await delivery_client.get_claim(**params)

        assert_that(
            delivery_mock.call_args.kwargs,
            has_entries(
                headers={
                    'Accept-Language': 'ru',
                    'Authorization': 'Bearer tokeh',
                    'X-Request-Id': match_equality(not_none()),
                },
                params={
                    'claim_id': 'claim-id',
                },
            ),
        )

    @pytest.mark.asyncio
    async def test_response(self, delivery_client, params):
        response = await delivery_client.get_claim(**params)

        assert_that(
            response,
            equal_to(
                Claim(
                    id='claim-id',
                    version=3,
                    revision=1,
                    status=ClaimStatus.DELIVERY_ARRIVED,
                    updated_ts=datetime(2022, 2, 22, tzinfo=timezone.utc),
                    pricing=Pricing(
                        currency='RUB',
                        final_price=Decimal('12.50'),
                        offer=Offer(
                            offer_id='28ae5f1d72364468be3f5e26cd6a66bf',
                            price=Decimal('12.50'),
                            valid_until=datetime(2022, 2, 23, tzinfo=timezone.utc),
                        ),
                    ),
                    error_messages=[
                        ErrorMessage(code='some_error', message='Some error'),
                    ],
                )
            ),
        )

    @pytest.fixture
    def params(self):
        return {
            'auth_token': 'tokeh',
            'claim_id': 'claim-id',
            'language': 'ru',
        }

    @pytest.fixture(autouse=True)
    def delivery_mock(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            re.compile(rf'{YANDEX_DELIVERY_URL}/v2/claims/info(\?.*)?'),
            status=200,
            payload={
                'id': 'claim-id',
                'version': 3,
                'revision': 1,
                'status': 'delivery_arrived',
                'updated_ts': '2022-02-22T00:00:00+00:00',
                'pricing': {
                    'currency': 'RUB',
                    'final_price': '12.50',
                    'offer': {
                        'offer_id': '28ae5f1d72364468be3f5e26cd6a66bf',
                        'price': '12.50',
                        'valid_until': '2022-02-23T00:00:00+00:00',
                    },
                },
                'error_messages': [{'code': 'some_error', 'message': 'Some error'}],
            },
        )


class TestAcceptClaim:
    @pytest.mark.asyncio
    async def test_request(self, delivery_client, delivery_mock, params):
        await delivery_client.accept_claim(**params)

        assert_that(
            delivery_mock.call_args.kwargs,
            has_entries(
                headers={
                    'Accept-Language': 'ru',
                    'Authorization': 'Bearer tokeh',
                    'X-Request-Id': match_equality(not_none()),
                },
                params={'claim_id': 'claim-id'},
                json={'version': 3},
            ),
        )

    @pytest.mark.asyncio
    async def test_response(self, delivery_client, params):
        response = await delivery_client.accept_claim(**params)

        assert_that(
            response,
            equal_to(
                AcceptClaimResponse(
                    id='claim-id',
                    status=ClaimStatus.ACCEPTED,
                    version=4,
                )
            ),
        )

    @pytest.fixture
    def params(self):
        return {
            'auth_token': 'tokeh',
            'claim_id': 'claim-id',
            'version': 3,
            'language': 'ru',
        }

    @pytest.fixture(autouse=True)
    def delivery_mock(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            re.compile(rf'{YANDEX_DELIVERY_URL}/v1/claims/accept(\?.*)?'),
            status=200,
            payload={
                'id': 'claim-id',
                'status': 'accepted',
                'version': 4,
            },
        )


class TestGetCancelInfo:
    @pytest.mark.asyncio
    async def test_request(self, delivery_client, delivery_mock, params):
        await delivery_client.get_cancel_info(**params)

        assert_that(
            delivery_mock.call_args.kwargs,
            has_entries(
                headers={
                    'Accept-Language': 'ru',
                    'Authorization': 'Bearer tokeh',
                    'X-Request-Id': match_equality(not_none()),
                },
                params={'claim_id': 'claim-id'},
            ),
        )

    @pytest.mark.asyncio
    async def test_response(self, delivery_client, params):
        response = await delivery_client.get_cancel_info(**params)

        assert_that(response, equal_to(CancelState.FREE))

    @pytest.fixture
    def params(self):
        return {
            'auth_token': 'tokeh',
            'claim_id': 'claim-id',
            'language': 'ru',
        }

    @pytest.fixture(autouse=True)
    def delivery_mock(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            re.compile(rf'{YANDEX_DELIVERY_URL}/v2/claims/cancel-info(\?.*)?'),
            status=200,
            payload={'cancel_state': 'free'},
        )


class TestCancelClaim:
    @pytest.mark.asyncio
    async def test_request(self, delivery_client, delivery_mock, params):
        await delivery_client.cancel_claim(**params)

        assert_that(
            delivery_mock.call_args.kwargs,
            has_entries(
                headers={
                    'Accept-Language': 'ru',
                    'Authorization': 'Bearer tokeh',
                    'X-Request-Id': match_equality(not_none()),
                },
                params={'claim_id': 'claim-id'},
                json={
                    'version': 5,
                    'cancel_state': 'free',
                },
            ),
        )

    @pytest.mark.asyncio
    async def test_response(self, delivery_client, params):
        response = await delivery_client.cancel_claim(**params)

        assert_that(
            response,
            equal_to(CancelClaimResponse(status=ClaimStatus.CANCELLED, id="24137f6774b64b7bae2ed895be332d08")),
        )

    @pytest.fixture
    def params(self):
        return {
            'auth_token': 'tokeh',
            'claim_id': 'claim-id',
            'version': 5,
            'cancel_state': CancelState.FREE,
            'language': 'ru',
        }

    @pytest.fixture(autouse=True)
    def delivery_mock(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            re.compile(rf'{YANDEX_DELIVERY_URL}/v2/claims/cancel(\?.*)?'),
            status=200,
            payload={
                "id": "24137f6774b64b7bae2ed895be332d08",
                "skip_client_notify": False,
                "status": "cancelled",
                "user_request_revision": "1",
                "version": 1,
            },
        )


@pytest.fixture
async def delivery_client(create_interaction_client):
    client = create_interaction_client(YandexDeliveryTestClient)
    yield client
    await client.close()
