import json
from base64 import b64decode
from dataclasses import fields
from decimal import Decimal
from typing import Mapping

import pytest
from aiohttp import TCPConnector
from marshmallow import ValidationError
from marshmallow_dataclass import class_schema

from sendr_utils import without_none
from sendr_utils.schemas import BaseSchema

from hamcrest import all_of, assert_that, equal_to, has_property, match_equality, not_none

from pay.lib.entities.threeds import ThreeDS2AuthenticationRequest, ThreeDSBrowserData
from pay.lib.interactions.psp.payture.client import AbstractPaytureAPIClient
from pay.lib.interactions.psp.payture.entities import (
    AddInfo,
    AgentInfo,
    Block3DSV1Required,
    Block3DSV2Required,
    BlockSuccess,
    ChargeResult,
    Cheque,
    ChequePosition,
    Order,
    OrderState,
    PaytureAPICredentials,
    RefundResult,
    SupplierInfo,
    UnblockResult,
)
from pay.lib.interactions.psp.payture.exceptions import PaytureDataError
from pay.lib.tests.helpers import parse_form_data_naive

PAYTURE_API_URL = 'https://api.payture.test'

CHEQUE = Cheque(
    customer_contact='user@email.com',
    positions=[
        ChequePosition(
            quantity=2.0,
            price=12.34,
            tax=6,
            text='Item',
            agent_type=1,
            agent_info=AgentInfo(
                payment_agent_operation='operation',
                payment_agent_phone_numbers=['+79995554444'],
                payment_operator_name='name',
                payment_operator_address='address',
                payment_operator_inn='123123123',
                payment_operator_phone_number=['+79995553333'],
                payment_transfer_operator_phone_numbers=['+79995552222'],
            ),
            payment_method_type=1,
            customs_declaration_number='123',
            excise=1.23,
            manufacturer_country_code='RU',
            payment_subject_type=2,
            nomenclature_code='aaa',
            supplier_inn='12345678',
            supplier_info=SupplierInfo(name='supplier', phone_numbers=['+79995554444']),
            unit_of_measurement='kg',
        )
    ],
)
SERIALIZED_CHEQUE = {
    "CustomerContact": "user@email.com",
    "Positions": [
        {
            "Excise": 1.23,
            "Text": "Item",
            "SupplierInfo": {"Name": "supplier", "PhoneNumbers": ["+79995554444"]},
            "AgentType": 1,
            "UnitOfMeasurement": "kg",
            "CustomsDeclarationNumber": "123",
            "Price": 12.34,
            "Tax": 6,
            "PaymentMethodType": 1,
            "PaymentSubjectType": 2,
            "NomenclatureCode": "aaa",
            "SupplierINN": "12345678",
            "AgentInfo": {
                "PaymentOperatorName": "name",
                "PaymentAgentPhoneNumbers": ["+79995554444"],
                "PaymentOperatorPhoneNumber": ["+79995553333"],
                "PaymentAgentOperation": "operation",
                "PaymentOperatorAddress": "address",
                "PaymentOperatorINN": "123123123",
                "PaymentTransferOperatorPhoneNumbers": ["+79995552222"],
            },
            "Quantity": 2,
            "ManufacturerCountryCode": "RU",
        }
    ],
}


class PaytureAPITestClient(AbstractPaytureAPIClient):
    BASE_URL = PAYTURE_API_URL
    DEBUG = False
    REQUEST_RETRY_TIMEOUTS = ()
    CONNECTOR = TCPConnector()


class TestErrorHandling:
    @pytest.mark.asyncio
    async def test_api_error(self, payture_api_client, aioresponses_mocker):
        aioresponses_mocker.post(
            f'{PAYTURE_API_URL}/api/Error',
            body="""<Block Success="False" ErrCode="DUPLICATE_ORDER_ID" />""",
        )

        with pytest.raises(PaytureDataError) as exc_info:
            await payture_api_client.post(
                interaction_method='something',
                url=payture_api_client.endpoint_url('/api/Error'),
            )

        assert_that(exc_info.value.response_status, equal_to('DUPLICATE_ORDER_ID'))


class TestMobileBlock:
    @pytest.mark.asyncio
    async def test_request(self, payture_api_client, params, get_rendered_request):
        await payture_api_client.mobile_block(**params)

        parsed_req = await get_rendered_request()
        assert_that(
            parsed_req,
            equal_to(
                without_none(
                    {
                        'Key': 'payture-key',
                        'OrderId': 'test-001',
                        'Amount': '50000',
                        'PayToken': 'PAYMENTTOKEN',
                        'CustomFields': match_equality(not_none()),
                    }
                )
            ),
        )
        assert_that(
            parsed_req['CustomFields'],
            equal_to(
                {
                    'IP': '192.0.2.1',
                    'ChallengeNotificationURL': 'https://challenge.test',
                    'BrowserData': match_equality(not_none()),
                }
            ),
        )
        assert_that(
            parsed_req['CustomFields']['BrowserData'],
            equal_to(
                {
                    'AcceptHeader': 'accept-header',
                    'ColorDepth': 'TWENTY_FOUR_BITS',
                    'Ip': '192.0.2.1',
                    'Language': 'EN',
                    'ScreenHeight': 1080,
                    'ScreenWidth': 1920,
                    'WindowHeight': 1080,
                    'WindowWidth': 1920,
                    'Timezone': '-180',
                    'UserAgent': 'user-agent',
                    'JavaEnabled': False,
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_checkout_payment(self, payture_api_client, params, get_rendered_request):
        params['is_checkout'] = True
        await payture_api_client.mobile_block(**params)

        parsed_req = await get_rendered_request()
        assert_that(parsed_req['CustomFields']['YaPayCheckout'], equal_to('1'))

    @pytest.mark.asyncio
    async def test_request_with_cheque(self, payture_api_client, params, get_rendered_request):
        params['cheque'] = CHEQUE
        await payture_api_client.mobile_block(**params)

        parsed_req = await get_rendered_request()
        assert_that(parsed_req['Cheque'], equal_to(SERIALIZED_CHEQUE))

    @pytest.mark.parametrize(
        'param_language, request_language',
        (
            ('en-US', 'EN'),
            ('ru-RU', 'RU'),
            ('ru', 'RU'),
            ('rus', 'RU'),
            ('en', 'EN'),
            ('run', 'EN'),
        ),
    )
    @pytest.mark.asyncio
    async def test_request_with_languages(
        self, payture_api_client, params, get_rendered_request, param_language, request_language
    ):
        params['threeds_authentication_request'].browser_data.language = param_language
        await payture_api_client.mobile_block(**params)

        parsed_req = await get_rendered_request()
        assert_that(parsed_req['CustomFields']['BrowserData']['Language'], equal_to(request_language))

    @pytest.mark.parametrize(
        'param_ip, request_ip',
        (
            ('192.0.2.1', '192.0.2.1'),
            ('2001:db8:b081:a407::1:23', '2001:0db8:b081:a407:0000:0000:0001:0023'),
        ),
    )
    @pytest.mark.asyncio
    async def test_request_with_ip(self, payture_api_client, params, get_rendered_request, param_ip, request_ip):
        params['threeds_authentication_request'].browser_data.ip = param_ip
        await payture_api_client.mobile_block(**params)

        parsed_req = await get_rendered_request()
        assert_that(parsed_req['CustomFields']['BrowserData']['Ip'], equal_to(request_ip))

    @pytest.mark.asyncio
    async def test_result_success(self, payture_api_client, mock_block_success, credentials, params):
        result = await payture_api_client.mobile_block(**params)

        assert_that(
            result,
            equal_to(
                BlockSuccess(
                    add_info=AddInfo(
                        auth_code='ABC',
                        ref_number='DEF',
                    ),
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_result_3dsv1_required(self, payture_api_client, aioresponses_mocker, credentials, params):
        aioresponses_mocker.post(
            f'{PAYTURE_API_URL}/api/MobileBlock',
            body="""
                <Block
                    OrderId="test-001"
                    Success="3DS"
                    Amount="1000"
                    ACSUrl="https://acs.test"
                    ThreeDSKey="tds-key"
                    PaReq="pa-req"
                    ThreeDSVersion="1.0"
                />
            """,
        )

        result = await payture_api_client.mobile_block(**params)

        assert_that(
            result,
            equal_to(
                Block3DSV1Required(
                    acs_url='https://acs.test',
                    pa_req='pa-req',
                    three_ds_key='tds-key',
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_result_3dsv2_required(self, payture_api_client, aioresponses_mocker, credentials, params):
        aioresponses_mocker.post(
            f'{PAYTURE_API_URL}/api/MobileBlock',
            body="""
                <Block
                    OrderId="test-001"
                    Success="3DS"
                    Amount="1000"
                    ACSUrl="https://acs.test"
                    CReq="c-req"
                    ThreeDSSessionData="threeds2-sess-data"
                    ThreeDSVersion="2.1"
                />
            """,
        )

        result = await payture_api_client.mobile_block(**params)

        assert_that(
            result,
            equal_to(
                Block3DSV2Required(
                    acs_url='https://acs.test',
                    creq='c-req',
                    threeds_session_data='threeds2-sess-data',
                )
            ),
        )

    @pytest.fixture
    def params(self, credentials):
        return {
            'credentials': credentials,
            'order_id': 'test-001',
            'amount': Decimal('500'),
            'user_ip': '192.0.2.1',
            'payment_token': 'PAYMENTTOKEN',
            'threeds_authentication_request': ThreeDS2AuthenticationRequest(
                challenge_notification_url='https://challenge.test',
                browser_data=ThreeDSBrowserData(
                    accept_header='accept-header',
                    ip='192.0.2.1',
                    java_enabled=False,
                    language='EN',
                    screen_color_depth=24,
                    screen_height=1080,
                    screen_width=1920,
                    window_height=1080,
                    window_width=1920,
                    timezone=-180,
                    user_agent='user-agent',
                ),
            ),
        }

    @pytest.fixture
    def mock_block_success(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            f'{PAYTURE_API_URL}/api/MobileBlock',
            body="""
                <Block OrderId="test-001" Success="True" Amount="1000">
                  <AddInfo Key="AuthCode" Value="ABC"/>
                  <AddInfo Key="RefNumber" Value="DEF"/>
                </Block>
            """,
        )

    @pytest.fixture
    def get_rendered_request(self, mock_block_success):
        async def _get_rendered_request():
            parsed_req = await parse_form_data_naive(mock_block_success.call_args.kwargs['data'])
            parsed_req['CustomFields'] = unparse_semicolon_separated_formdata(parsed_req['CustomFields'])
            parsed_req['CustomFields']['BrowserData'] = json.loads(
                b64decode(parsed_req['CustomFields']['BrowserData']).decode('utf-8')
            )
            if 'Cheque' in parsed_req:
                parsed_req['Cheque'] = json.loads(b64decode(parsed_req['Cheque']).decode('utf-8'))
            return parsed_req

        return _get_rendered_request


class TestBlock3DS:
    @pytest.fixture(autouse=True)
    def mock_block_3ds(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            f'{PAYTURE_API_URL}/api/Block3DS',
            body="""
                <Block OrderId="test-001" Key="payture-key" Success="True" Amount="1000" Points="0">
                  <AddInfo Key="AuthCode" Value="A225081"/>
                  <AddInfo Key="RefNumber" Value="A2819904897109"/>
                </Block>
            """,
        )

    @pytest.mark.parametrize(
        'additional_params, expected_body_part',
        (
            pytest.param({'pa_res': 'pa-res'}, {'PaRes': 'pa-res'}, id='3ds-1'),
            pytest.param({'cres': 'cres'}, {'CRes': 'cres'}, id='3ds-2'),
        ),
    )
    @pytest.mark.asyncio
    async def test_call(self, payture_api_client, mock_block_3ds, credentials, additional_params, expected_body_part):
        await payture_api_client.block_3ds(credentials=credentials, order_id='test-001', **additional_params)

        assert_that(
            await parse_form_data_naive(mock_block_3ds.call_args.kwargs['data']),
            equal_to(
                {
                    'OrderId': 'test-001',
                    'Key': 'payture-key',
                    **expected_body_part,
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_result(self, payture_api_client, credentials):
        result = await payture_api_client.block_3ds(
            credentials=credentials, order_id='test-001', pa_res='pa-res', cres='creq'
        )

        assert_that(
            result,
            equal_to(BlockSuccess(add_info=AddInfo(auth_code='A225081', ref_number='A2819904897109'))),
        )


class TestCharge:
    @pytest.fixture(autouse=True)
    def mock_charge(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            f'{PAYTURE_API_URL}/api/Charge',
            body='<Charge Success="True" OrderId="test-001" Amount="1000"/>',
        )

    @pytest.mark.parametrize(
        'amount, expected_amount, cheque, expected_cheque',
        (
            (Decimal('500'), '50000', CHEQUE, SERIALIZED_CHEQUE),
            (None, None, None, None),
        ),
    )
    @pytest.mark.asyncio
    async def test_call(
        self, payture_api_client, mock_charge, credentials, amount, expected_amount, cheque, expected_cheque
    ):
        await payture_api_client.charge(credentials=credentials, order_id='test-001', amount=amount, cheque=cheque)

        parsed_req = await parse_form_data_naive(mock_charge.call_args.kwargs['data'])
        if 'Cheque' in parsed_req:
            parsed_req['Cheque'] = json.loads(b64decode(parsed_req['Cheque']).decode('utf-8'))
        assert_that(
            parsed_req,
            equal_to(
                without_none(
                    {'Amount': expected_amount, 'OrderId': 'test-001', 'Key': 'payture-key', 'Cheque': expected_cheque}
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_result(self, payture_api_client, credentials):
        result = await payture_api_client.charge(credentials=credentials, order_id='test-001', amount=Decimal('500'))

        assert_that(
            result,
            all_of(
                equal_to(ChargeResult(raw_new_amount=1000)),
                has_property('new_amount', Decimal('10')),
            ),
        )


class TestRefund:
    @pytest.fixture(autouse=True)
    def mock_refund(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            f'{PAYTURE_API_URL}/api/Refund',
            body='<Refund Success="True" OrderId="49c8dda8-0ecc-89fa-d596-44cf21f0748e" NewAmount="1000"/>',
        )

    @pytest.mark.parametrize(
        'amount, expected_amount, cheque, expected_cheque',
        (
            (Decimal('500'), '50000', CHEQUE, SERIALIZED_CHEQUE),
            (None, None, None, None),
        ),
    )
    @pytest.mark.asyncio
    async def test_call(
        self, payture_api_client, mock_refund, credentials, amount, expected_amount, cheque, expected_cheque
    ):
        await payture_api_client.refund(credentials=credentials, order_id='test-001', amount=amount, cheque=cheque)

        parsed_req = await parse_form_data_naive(mock_refund.call_args.kwargs['data'])
        if 'Cheque' in parsed_req:
            parsed_req['Cheque'] = json.loads(b64decode(parsed_req['Cheque']).decode('utf-8'))
        assert_that(
            parsed_req,
            equal_to(
                without_none(
                    {
                        'Amount': expected_amount,
                        'OrderId': 'test-001',
                        'Key': 'payture-key',
                        'Password': 'payture-password',
                        'Cheque': expected_cheque,
                    }
                )
            ),
        )

    @pytest.mark.asyncio
    async def test_result(self, payture_api_client, credentials):
        result = await payture_api_client.refund(credentials=credentials, order_id='test-001', amount=Decimal('500'))

        assert_that(
            result,
            all_of(
                equal_to(RefundResult(raw_new_amount=1000)),
                has_property('new_amount', Decimal('10')),
            ),
        )


class TestUnblock:
    @pytest.fixture(autouse=True)
    def mock_unblock(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            f'{PAYTURE_API_URL}/api/Unblock',
            body='<Refund Success="True" OrderId="49c8dda8-0ecc-89fa-d596-44cf21f0748e" NewAmount="1000"/>',
        )

    @pytest.mark.asyncio
    async def test_call(self, payture_api_client, mock_unblock, credentials):
        await payture_api_client.unblock(credentials=credentials, order_id='test-001')

        assert_that(
            await parse_form_data_naive(mock_unblock.call_args.kwargs['data']),
            equal_to(
                {
                    'OrderId': 'test-001',
                    'Key': 'payture-key',
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_result(self, payture_api_client, mock_unblock, credentials):
        result = await payture_api_client.unblock(credentials=credentials, order_id='test-001')

        assert_that(
            result,
            all_of(
                equal_to(UnblockResult(raw_new_amount=1000)),
                has_property('new_amount', Decimal('10')),
            ),
        )


class TestGetOrder:
    @pytest.fixture(autouse=True)
    def mock_get_order(self, aioresponses_mocker):
        return aioresponses_mocker.post(
            f'{PAYTURE_API_URL}/api/GetState',
            body="""
                <GetState Success="True" OrderId="test-001" Amount="1000" RRN="1234567" State="Authorized">
                  <AddInfo Key="AuthCode" Value="ABC"/>
                  <AddInfo Key="RefNumber" Value="DEF"/>
                </GetState>
            """,
        )

    @pytest.mark.asyncio
    async def test_call(self, payture_api_client, mock_get_order, credentials):
        await payture_api_client.get_order(credentials=credentials, order_id='test-001')

        assert_that(
            await parse_form_data_naive(mock_get_order.call_args.kwargs['data']),
            equal_to(
                {
                    'OrderId': 'test-001',
                    'Key': 'payture-key',
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_result(self, payture_api_client, mock_get_order, credentials):
        result = await payture_api_client.get_order(credentials=credentials, order_id='test-001')

        assert_that(
            result,
            equal_to(
                Order(
                    order_id='test-001',
                    state=OrderState.AUTHORIZED,
                    raw_amount=1000,
                    rrn='1234567',
                    add_info=AddInfo(auth_code='ABC', ref_number='DEF'),
                )
            ),
        )


class TestCredentialsSchema:
    VALID_RAW_CREDS = {'key': 'key', 'password': 'password', 'gateway_merchant_id': 'gateway-merchant-id'}
    SCHEMA_CLS = class_schema(PaytureAPICredentials, base_schema=BaseSchema)

    @pytest.mark.parametrize('field', [field.name for field in fields(PaytureAPICredentials)])
    def test_when_field_omitted__raises(self, field):
        creds = {k: v for k, v in self.VALID_RAW_CREDS.items() if k != field}

        with pytest.raises(ValidationError) as exc_info:
            self.SCHEMA_CLS().load(creds)

        assert_that(exc_info.value.normalized_messages(), equal_to({field: ['Missing data for required field.']}))

    @pytest.mark.parametrize('field', [field.name for field in fields(PaytureAPICredentials)])
    def test_when_field_empty__raises(self, field):
        creds = dict(self.VALID_RAW_CREDS)
        creds[field] = ''

        with pytest.raises(ValidationError) as exc_info:
            self.SCHEMA_CLS().load(creds)

        assert_that(exc_info.value.normalized_messages(), equal_to({field: ['String should not be empty.']}))


def unparse_semicolon_separated_formdata(data: str) -> Mapping[str, str]:
    return dict(item.split('=', 1) for item in data.split(';'))


@pytest.fixture
async def payture_api_client(create_interaction_client) -> PaytureAPITestClient:
    client = create_interaction_client(PaytureAPITestClient)
    yield client
    await client.close()


@pytest.fixture
def credentials():
    return PaytureAPICredentials(key='payture-key', password='payture-password', gateway_merchant_id='payture-gw')
