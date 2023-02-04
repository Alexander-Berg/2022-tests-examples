import base64
import json
from dataclasses import fields
from decimal import Decimal
from io import BytesIO
from urllib.parse import unquote

import pytest
from aiohttp import FormData, TCPConnector
from aiohttp.abc import AbstractStreamWriter
from marshmallow import ValidationError
from marshmallow_dataclass import class_schema
from multidict import CIMultiDict

from sendr_pytest.matchers import equal_to
from sendr_utils.schemas import BaseSchema

from hamcrest import assert_that, match_equality, not_none

from pay.lib.entities.threeds import ThreeDS2AuthenticationRequest, ThreeDSBrowserData
from pay.lib.interactions.psp.uniteller.client import AbstractUnitellerAPIClient, UnitellerCredentials
from pay.lib.interactions.psp.uniteller.entities import (
    ChargeResult,
    Cheque,
    ChequeLine,
    ChequeLineAgent,
    ChequeLineProduct,
    ChequePayment,
    Customer,
    PaymentSubjectType,
    PaymentType,
    RefundResult,
    TaxMode,
    ThreeDSV1Params,
    ThreeDSV1Result,
    ThreeDSV2Params,
    ThreeDSV2Result,
    UnitellerPaymentKind,
    UnitellerPaymentType,
    UnitellerTaxType,
)
from pay.lib.interactions.psp.uniteller.exceptions import UnitellerDataError

CHEQUE = Cheque(
    total=150.0,
    taxmode=TaxMode.COMMON,
    lines=[
        ChequeLine(
            name='Full Product',
            qty=1.5,
            price=100.0,
            vat=UnitellerTaxType.VAT_10,
            sum=150.0,
            payattr=PaymentType.FULL_PAYMENT,
            lineattr=PaymentSubjectType.PAYMENT,
            product=ChequeLineProduct(kt='aefc6cdc2ce3223315f4f46585de5217c7c8799529285b9b', coc='', ncd='', exc=9.99),
            agent=ChequeLineAgent(
                agentattr='2',
                agentphone='+798700000222',
                accopphone='+79876543210 +798700012345',
                opphone='+798700000333',
                opname='Transfer Operator Name',
                opinn='123456789',
                opaddress='Transfer Operator Address',
                operation='operation',
                suppliername='Supplier Name',
                supplierinn='123456789',
                supplierphone='+798700000111',
            ),
        )
    ],
    payments=[
        ChequePayment(
            kind=UnitellerPaymentKind.CARD,
            type=UnitellerPaymentType.BANK_CARD_OR_DIGITAL,
            amount=150.0,
        )
    ],
    customer=Customer(email='email', phone='phone'),
)


SERIALIZED_CHEQUE = {
    'payments': [{'type': 0, 'amount': 150.0, 'kind': 1}],
    'customer': {'email': 'email', 'phone': 'phone'},
    'taxmode': 0,
    'lines': [
        {
            'qty': 1.5,
            'price': 100.0,
            'name': 'Full Product',
            'payattr': 4,
            'sum': 150.0,
            'product': {'kt': 'aefc6cdc2ce3223315f4f46585de5217c7c8799529285b9b', 'ncd': '', 'exc': 9.99, 'coc': ''},
            'vat': 10,
            'lineattr': 10,
            'agent': {
                'supplierphone': '+798700000111',
                'supplierinn': '123456789',
                'opname': 'Transfer Operator Name',
                'agentphone': '+798700000222',
                'agentattr': '2',
                'accopphone': '+79876543210 +798700012345',
                'suppliername': 'Supplier Name',
                'opphone': '+798700000333',
                'opinn': '123456789',
                'opaddress': 'Transfer Operator Address',
                'operation': 'operation',
            },
        }
    ],
    'total': 150.0,
}

UNITELLER_CHARGE_URL = 'https://uniteller.test/charge'
UNITELLER_FINISH_3DS_URL = 'https://uniteller.test/finish-3ds'
UNITELLER_CONFIRM_URL = 'https://uniteller.test/confirm'
UNITELLER_REFUND_URL = 'https://uniteller.test/refund'


class UnitellerAPITestClient(AbstractUnitellerAPIClient):
    DEBUG = False
    REQUEST_RETRY_TIMEOUTS = ()
    CONNECTOR = TCPConnector()
    CHARGE_URL = UNITELLER_CHARGE_URL
    CONFIRM_URL = UNITELLER_CONFIRM_URL
    FINISH_3DS_URL = UNITELLER_FINISH_3DS_URL
    REFUND_URL = UNITELLER_REFUND_URL

    CHARGE_RECEIPT_URL = UNITELLER_CHARGE_URL
    CONFIRM_RECEIPT_URL = UNITELLER_CONFIRM_URL
    FINISH_3DS_RECEIPT_URL = UNITELLER_FINISH_3DS_URL
    REFUND_RECEIPT_URL = UNITELLER_REFUND_URL


@pytest.fixture
async def uniteller_api_client(create_interaction_client) -> UnitellerAPITestClient:
    client = create_interaction_client(UnitellerAPITestClient)
    yield client
    await client.close()


@pytest.fixture
def credentials():
    return UnitellerCredentials(login='123', password='456', gateway_merchant_id='uniteller-gw')


@pytest.fixture
def params(credentials):
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


class TestErrorHandling:
    @pytest.mark.asyncio
    async def test_api_error(self, uniteller_api_client, params, aioresponses_mocker):
        aioresponses_mocker.post(
            UNITELLER_CHARGE_URL,
            body="""<?xml version="1.0" encoding="UTF-8"?>
            <UnitellerAPI>
                <ResultYandexPay>
                    <Result>40</Result>
                    <ErrorMessage>Ошибка сохранения данных платежа.</ErrorMessage>
                </ResultYandexPay>
            </UnitellerAPI>""",
        )
        with pytest.raises(UnitellerDataError) as exc_info:
            await uniteller_api_client.charge(**params)

        assert_that(exc_info.value.response_status, equal_to('40'))
        assert_that(exc_info.value.message, equal_to('Ошибка сохранения данных платежа.'))


class TestPay:
    @pytest.mark.asyncio
    async def test_call_with_cheque_and_without_send_receipt_flag(
        self, aioresponses_mocker, uniteller_api_client, params
    ):
        params['cheque'] = CHEQUE
        params['credentials'].send_receipt = False

        mock_pay = aioresponses_mocker.post(
            UNITELLER_CHARGE_URL,
            body="""<?xml version="1.0" encoding="UTF-8"?>
            <UnitellerAPI>
                <ResultYandexPay>
                    <Result>0</Result>
                    <ErrorMessage></ErrorMessage>
                    <AuthCode>123</AuthCode>
                    <RRN>456</RRN>
                </ResultYandexPay>
            </UnitellerAPI>
            """,
        )

        await uniteller_api_client.charge(**params)

        request_data = await parse_form_data_naive(mock_pay.call_args.kwargs['data'])
        assert_that(
            request_data,
            equal_to(
                {
                    'ShopID': params['credentials'].gateway_merchant_id,
                    'OrderID': params['order_id'],
                    'Subtotal': str(params['amount']),
                    'PaymentToken': params['payment_token'],
                    'Currency': 'RUB',
                    'TermUrl': params['threeds_authentication_request'].challenge_notification_url,
                    'BrowserInfo': match_equality(not_none()),
                    'ClientIp': params['user_ip'],
                    'IsPreAuth': '0',
                    'Signature': match_equality(not_none()),
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_call_with_cheque(self, aioresponses_mocker, uniteller_api_client, params):
        params['cheque'] = CHEQUE
        params['credentials'].send_receipt = True

        mock_pay = aioresponses_mocker.post(
            UNITELLER_CHARGE_URL,
            body="""<?xml version="1.0" encoding="UTF-8"?>
            <UnitellerAPI>
                <ResultYandexPay>
                    <Result>0</Result>
                    <ErrorMessage></ErrorMessage>
                    <AuthCode>123</AuthCode>
                    <RRN>456</RRN>
                </ResultYandexPay>
            </UnitellerAPI>
            """,
        )

        await uniteller_api_client.charge(**params)

        request_data = await parse_form_data_naive(mock_pay.call_args.kwargs['data'])
        assert_that(
            request_data,
            equal_to(
                {
                    'ShopID': params['credentials'].gateway_merchant_id,
                    'OrderID': params['order_id'],
                    'Subtotal': str(params['amount']),
                    'PaymentToken': params['payment_token'],
                    'Currency': 'RUB',
                    'TermUrl': params['threeds_authentication_request'].challenge_notification_url,
                    'BrowserInfo': match_equality(not_none()),
                    'ClientIp': params['user_ip'],
                    'IsPreAuth': '0',
                    'Signature': match_equality(not_none()),
                    'ReceiptSignature': match_equality(not_none()),
                    'Receipt': match_equality(not_none()),
                }
            ),
        )

        assert_that(
            json.loads(base64.b64decode(request_data['Receipt'])),
            equal_to(SERIALIZED_CHEQUE),
        )

    @pytest.mark.asyncio
    async def test_charge_without_3ds(self, aioresponses_mocker, uniteller_api_client, params):
        auth_code = '6E3A0D'
        rrn = '212420205837'

        mock_pay = aioresponses_mocker.post(
            UNITELLER_CHARGE_URL,
            body=f"""<?xml version="1.0" encoding="UTF-8"?>
            <UnitellerAPI>
                <ResultYandexPay>
                    <Result>0</Result>
                    <ErrorMessage></ErrorMessage>
                    <AuthCode>{auth_code}</AuthCode>
                    <RRN>{rrn}</RRN>
                </ResultYandexPay>
            </UnitellerAPI>
            """,
        )

        response = await uniteller_api_client.charge(**params)

        assert response == ChargeResult(
            result=0,
            auth_code=auth_code,
            rrn=rrn,
        )

        request_data = await parse_form_data_naive(mock_pay.call_args.kwargs['data'])
        assert_that(
            request_data,
            equal_to(
                {
                    'ShopID': params['credentials'].gateway_merchant_id,
                    'OrderID': params['order_id'],
                    'Subtotal': str(params['amount']),
                    'PaymentToken': params['payment_token'],
                    'Currency': 'RUB',
                    'TermUrl': params['threeds_authentication_request'].challenge_notification_url,
                    'BrowserInfo': match_equality(not_none()),
                    'ClientIp': params['user_ip'],
                    'IsPreAuth': '0',
                    'Signature': match_equality(not_none()),
                }
            ),
        )

        browser_info = json.loads(base64.b64decode(request_data['BrowserInfo']))
        browser_data: ThreeDSBrowserData = params['threeds_authentication_request'].browser_data
        assert_that(
            browser_info,
            equal_to(
                {
                    'accept': browser_data.accept_header,
                    'colorDepth': browser_data.screen_color_depth,
                    'language': browser_data.language,
                    'screenHeight': browser_data.screen_height,
                    'screenWidth': browser_data.screen_width,
                    'windowHeight': browser_data.window_height,
                    'windowWidth': browser_data.window_width,
                    'timezoneOffset': browser_data.timezone,
                    'userAgent': browser_data.user_agent,
                    'javaEnabled': browser_data.java_enabled,
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_charge_with_3ds1(self, aioresponses_mocker, uniteller_api_client, params):
        aioresponses_mocker.post(
            UNITELLER_CHARGE_URL,
            body="""<?xml version="1.0" encoding="UTF-8"?>
            <UnitellerAPI>
                <ResultYandexPay>
                    <Result>0</Result>
                    <ErrorMessage></ErrorMessage>
                    <PaymentAttemptID>123</PaymentAttemptID>
                    <Is3DS>1</Is3DS>
                    <RedirectParams>
                        <URL>http://acs.test</URL>
                        <PaReq>123</PaReq>
                        <TermUrl>http://term.test</TermUrl>
                        <MD>456</MD>
                    </RedirectParams>
                    <RedirectForm>&lt;div>some-html-here&lt;/div></RedirectForm>
                </ResultYandexPay>
            </UnitellerAPI>
            """,
        )

        response = await uniteller_api_client.charge(**params)

        assert response == ThreeDSV1Result(
            result=0,
            error_message=None,
            payment_attempt_id='123',
            redirect_params=ThreeDSV1Params(
                acs_url='http://acs.test',
                pa_req='123',
                term_url='http://term.test',
                md='456',
            ),
            redirect_form='<div>some-html-here</div>',
        )

    @pytest.mark.asyncio
    async def test_charge_with_3ds2(self, aioresponses_mocker, uniteller_api_client, params):
        aioresponses_mocker.post(
            UNITELLER_CHARGE_URL,
            body="""<?xml version="1.0" encoding="UTF-8"?>
            <UnitellerAPI>
                <ResultYandexPay>
                    <Result>0</Result>
                    <ErrorMessage></ErrorMessage>
                    <PaymentAttemptID>123</PaymentAttemptID>
                    <Is3DS>1</Is3DS>
                    <Version3DS>2</Version3DS>
                    <RedirectParams>
                        <URL>http://acs.test</URL>
                        <creq>123</creq>
                        <threeDSSessionData>456</threeDSSessionData>
                    </RedirectParams>
                </ResultYandexPay>
            </UnitellerAPI>
            """,
        )

        response = await uniteller_api_client.charge(**params)

        assert response == ThreeDSV2Result(
            result=0,
            error_message=None,
            payment_attempt_id='123',
            redirect_params=ThreeDSV2Params(
                acs_url='http://acs.test',
                creq='123',
                threeds_session_data='456',
            ),
        )

    @pytest.mark.asyncio
    async def test_confirm(self, aioresponses_mocker, uniteller_api_client, params):
        mock_confirm = aioresponses_mocker.post(
            UNITELLER_CONFIRM_URL,
            body="""<?xml version="1.0" encoding="UTF-8"?>
            <UnitellerAPI>
                <Result>0</Result>
            </UnitellerAPI>
            """,
        )

        await uniteller_api_client.confirm(
            credentials=params['credentials'],
            order_id=params['order_id'],
        )

        request_data = await parse_form_data_naive(mock_confirm.call_args.kwargs['data'])
        assert_that(
            request_data,
            equal_to(
                {
                    'ShopID': params['credentials'].gateway_merchant_id,
                    'OrderID': params['order_id'],
                    'Signature': match_equality(not_none()),
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_refund(self, aioresponses_mocker, uniteller_api_client, params):
        mock_confirm = aioresponses_mocker.post(
            UNITELLER_REFUND_URL,
            body="""<?xml version="1.0" encoding="utf-8" standalone="yes"?>
            <unitellerresult firstcode="" secondcode="" count="1">
                <orders>
                    <order>
                        <ordernumber>12</ordernumber>
                        <response_code>AS000</response_code>
                        <recommendation></recommendation>
                        <message></message>
                        <comment></comment>
                        <date>2022.05.16 12:33:37</date>
                        <total>10.00</total>
                        <currency>RUB</currency>
                        <cardtype>visa</cardtype>
                        <cardnumber>400000******2487</cardnumber>
                        <lastname></lastname>
                        <firstname></firstname>
                        <middlename></middlename>
                        <address></address>
                        <email></email>
                        <approvalcode>44FF81</approvalcode>
                        <cvc2>0</cvc2>
                        <cardholder></cardholder>
                        <ipaddress>-</ipaddress>
                        <billnumber>213612604410</billnumber>
                        <bankname></bankname>
                        <status>Canceled</status>
                        <error_code></error_code>
                        <error_comment></error_comment>
                        <packetdate>16.05.2022 12:33:37</packetdate>
                        <paymenttype>0</paymenttype>
                        <phone></phone>
                        <idata></idata>
                    </order>
                </orders>
            </unitellerresult>
            """,
        )

        response = await uniteller_api_client.refund(
            credentials=params['credentials'],
            order_id=params['order_id'],
        )

        assert response == RefundResult(
            status='Canceled',
            error_code=None,
            order_number='12',
            total='10.00',
            response_code='AS000',
        )
        request_data = await parse_form_data_naive(mock_confirm.call_args.kwargs['data'])
        assert_that(
            request_data,
            equal_to(
                {
                    'Format': '3',
                    'Shop_ID': params['credentials'].gateway_merchant_id,
                    'OrderID': params['order_id'],
                    'Signature': match_equality(not_none()),
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_partial_refund(self, aioresponses_mocker, uniteller_api_client, params):
        mock_confirm = aioresponses_mocker.post(
            UNITELLER_REFUND_URL,
            body="""<?xml version="1.0" encoding="utf-8" standalone="yes"?>
            <unitellerresult firstcode="" secondcode="" count="1">
                <orders>
                    <order>
                        <ordernumber>12</ordernumber>
                        <response_code>AS000</response_code>
                        <recommendation></recommendation>
                        <message></message>
                        <comment></comment>
                        <date>2022.05.16 12:33:37</date>
                        <total>10.00</total>
                        <currency>RUB</currency>
                        <cardtype>visa</cardtype>
                        <cardnumber>400000******2487</cardnumber>
                        <lastname></lastname>
                        <firstname></firstname>
                        <middlename></middlename>
                        <address></address>
                        <email></email>
                        <approvalcode>44FF81</approvalcode>
                        <cvc2>0</cvc2>
                        <cardholder></cardholder>
                        <ipaddress>-</ipaddress>
                        <billnumber>213612604410</billnumber>
                        <bankname></bankname>
                        <status>Canceled</status>
                        <error_code></error_code>
                        <error_comment></error_comment>
                        <packetdate>16.05.2022 12:33:37</packetdate>
                        <paymenttype>0</paymenttype>
                        <phone></phone>
                        <idata></idata>
                    </order>
                </orders>
            </unitellerresult>
            """,
        )

        await uniteller_api_client.refund(
            credentials=params['credentials'], order_id=params['order_id'], amount=Decimal('10')
        )

        request_data = await parse_form_data_naive(mock_confirm.call_args.kwargs['data'])
        assert_that(
            request_data,
            equal_to(
                {
                    'Format': '3',
                    'Shop_ID': params['credentials'].gateway_merchant_id,
                    'OrderID': params['order_id'],
                    'Subtotal_P': '10',
                    'Signature': match_equality(not_none()),
                }
            ),
        )

    @pytest.mark.asyncio
    async def test_finish_3ds(self, aioresponses_mocker, uniteller_api_client, params):
        attempt_id = '123'
        pa_res = '456'
        auth_code = '6E3A0D'
        rrn = '212420205837'

        mock_finish_3ds = aioresponses_mocker.post(
            UNITELLER_FINISH_3DS_URL,
            body=f"""<?xml version="1.0" encoding="UTF-8"?>
            <UnitellerAPI>
                <ResultYandexPay>
                    <Result>0</Result>
                    <ErrorMessage></ErrorMessage>
                    <AuthCode>{auth_code}</AuthCode>
                    <RRN>{rrn}</RRN>
                </ResultYandexPay>
            </UnitellerAPI>
            """,
        )

        response = await uniteller_api_client.finish_3ds(
            credentials=params['credentials'],
            payment_attempt_id=attempt_id,
            pa_res=pa_res,
        )

        assert response == ChargeResult(
            result=0,
            auth_code=auth_code,
            rrn=rrn,
        )

        request_data = await parse_form_data_naive(mock_finish_3ds.call_args.kwargs['data'])
        assert_that(
            request_data,
            equal_to(
                {
                    'ShopID': params['credentials'].gateway_merchant_id,
                    'PaymentAttemptID': attempt_id,
                    'PaRes': pa_res,
                    'Signature': match_equality(not_none()),
                }
            ),
        )


class TestCredentialsSchema:
    VALID_RAW_CREDS = {
        'login': 'login',
        'password': 'password',
        'gateway_merchant_id': 'gateway-merchant-id',
        'send_receipt': False,
    }
    SCHEMA_CLS = class_schema(UnitellerCredentials, base_schema=BaseSchema)

    @pytest.mark.parametrize(
        'field', [field.name for field in fields(UnitellerCredentials) if field.name != 'send_receipt']
    )
    def test_when_field_omitted__raises(self, field):
        creds = {k: v for k, v in self.VALID_RAW_CREDS.items() if k != field}

        with pytest.raises(ValidationError) as exc_info:
            self.SCHEMA_CLS().load(creds)

        assert_that(exc_info.value.normalized_messages(), equal_to({field: ['Missing data for required field.']}))

    @pytest.mark.parametrize('field', [field.name for field in fields(UnitellerCredentials)])
    def test_when_field_empty__raises(self, field):
        creds = dict(self.VALID_RAW_CREDS)
        creds[field] = ''

        with pytest.raises(ValidationError) as exc_info:
            self.SCHEMA_CLS().load(creds)
        msg = 'String should not be empty.'
        if field == 'send_receipt':
            msg = 'Not a valid boolean.'
        assert_that(exc_info.value.normalized_messages(), equal_to({field: [msg]}))


async def parse_form_data_naive(form_data: FormData):
    class BufferWriter(AbstractStreamWriter):
        def __init__(self):
            self._buffer = BytesIO()

        async def write(self, chunk: bytes) -> None:
            """Write chunk into stream."""
            self._buffer.write(chunk)

        async def write_eof(self, chunk: bytes = b"") -> None:
            self._buffer.write(chunk)

        async def drain(self) -> None:
            pass

        def enable_compression(self, encoding: str = "deflate") -> None:
            pass

        def enable_chunking(self) -> None:
            pass

        async def write_headers(self, status_line: str, headers: "CIMultiDict[str]") -> None:
            pass

        def get_value(self) -> bytes:
            return self._buffer.getvalue()

    writer = BufferWriter()
    await form_data().write(writer)
    data = writer.get_value().decode('utf-8')
    return dict(map(unquote, item.split('=', 1)) for item in data.split('&'))
