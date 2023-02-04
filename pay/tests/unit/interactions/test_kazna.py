import json
from datetime import datetime

import pytest
import yarl
from aioresponses import CallbackResult
from dateutil.tz import tzutc
from marshmallow import ValidationError

from hamcrest import assert_that, equal_to

from pay.bill_payments.bill_payments.interactions import KaznaClient
from pay.bill_payments.bill_payments.interactions.kazna.entities import (
    TDS,
    AdditionalData,
    ChargeData,
    DeviceChannel,
    MpiExtInfo,
    PayerDoc,
    PayerParams,
    PaymentData,
    PaymentInfoResponse,
    PaymentParams,
    PaymentStatus,
    PaymentStatusCode,
    PayRequest,
    PayType,
    SearchRequest,
    SearchStatus,
)
from pay.bill_payments.bill_payments.interactions.kazna.exceptions import (
    BaseKaznaInteractionError,
    KaznaAPIError,
    KaznaAPIErrorCode,
)
from pay.bill_payments.bill_payments.interactions.kazna.schemas import (
    DepartmentType,
    DocumentCode,
    DriverLicense,
    PayRequestSchema,
    VehicleRegistrationCertificate,
)


@pytest.fixture
async def kazna_client(create_client):
    client: KaznaClient = create_client(KaznaClient)
    client.REQUEST_RETRY_TIMEOUTS = ()
    yield client
    await client.close()


@pytest.mark.asyncio
async def test_search_complete_success(kazna_client: KaznaClient, aioresponses_mocker):
    def callback(url, **kwargs):
        assert kwargs['json'] == {
            'depType': 'gibdd',
            'payerDoc': [
                {
                    'code': '24',
                    'value': ['1111111111'],
                }
            ],
        }
        basic_charge_data = {
            'billDate': '2021-11-23T00:00:00Z',
            'amountToPay': 50000,
            'purpose': 'ШТРАФ ПО АДМИНИСТРАТИВНОМУ ПРАВОНАРУШЕНИЮ',
            'kbk': '18811630020016000140',
            'oktmo': '86701000',
            'inn': '1001041280',
            'kpp': '100101001',
            'bik': '017908101',
            'accountNumber': '03100643000000017600',
            'payeeName': 'Управление федерального казначейства',
            'payeeCorrAccount': '40102810145370000066',
            'divID': 1186190,
            'amount': 50000,
            'payerIdentifier': '2400000000001111111111643',
            'treasureBranch': '',
            'payerName': '',
        }
        return CallbackResult(
            status=200,
            payload={
                'status': 'complete',
                'charges': [
                    {
                        'payerDoc': {'code': '24', 'value': ['1111111111']},
                        'supplierBillID': '18810567534082756200',
                        'depType': 'gibdd',
                        'chargeData': {
                            **basic_charge_data,
                            'additionalData': {
                                'discountDate': '2021-12-13',
                                'discountSize': '50',
                                'legalAct': 'Часть 2 статьи 12.9 КоАП',
                                'offenseName': 'Превышение скорости движения на величину более 20, но не более 40 км/ч',
                                'offensePlace': 'Г. САРАТОВ, УЛ. ОРДЖОНИКИДЗЕ, Д.24Б (ИЗ ЦЕНТРА)',
                                'offenseDate': '2020-01-31 13:23:00',
                            },
                            'paymentData': {'sum': '10000', 'date': '2016-10-23T18:00:00Z'},
                        },
                        'ttl': '2021-11-26T19:15:52.000Z',
                    },
                    {
                        'payerDoc': {'code': '24', 'value': ['1111111111']},
                        'supplierBillID': '18810177200855150244',
                        'depType': 'gibdd',
                        'chargeData': {
                            **basic_charge_data,
                            'billDate': '2021-02-26T00:00:00Z',
                            'additionalData': [],
                        },
                        'ttl': '2021-11-26T19:15:52.000Z',
                    },
                    {
                        'payerDoc': {'code': '24', 'value': ['1111111111']},
                        'supplierBillID': '18810177200855150244',
                        'depType': 'gibdd',
                        'chargeData': {
                            **basic_charge_data,
                        },
                        'ttl': '2021-11-26T19:15:52.000Z',
                    },
                    {
                        'payerDoc': {'code': '24', 'value': ['1111111111']},
                        'supplierBillID': '18810177200855150244',
                        'depType': 'gibdd',
                        'chargeData': {
                            **basic_charge_data,
                            'additionalData': {
                                'offenseDate': '2020-01-31',
                            },
                        },
                        'ttl': '2021-11-26T19:15:52.000Z',
                    },
                ],
            },
        )

    aioresponses_mocker.post(
        'http://go.zora.yandex.net:1080',
        callback=callback,
    )
    response = await kazna_client.search(
        SearchRequest(
            department=DepartmentType.GIBDD,
            documents=[PayerDoc(code=DocumentCode.VEHICLE_REGISTRATION_CERTIFICATE, value=['1111111111'])],
        )
    )
    assert response.status == SearchStatus.COMPLETE
    assert len(response.charges) == 4

    charge = response.charges[0]

    assert charge.department == DepartmentType.GIBDD
    assert charge.supplier_bill_id == '18810567534082756200'

    expected = ChargeData(
        bill_date=datetime(2021, 11, 23, 0, 0, tzinfo=tzutc()),
        amount_to_pay=50000,
        purpose='ШТРАФ ПО АДМИНИСТРАТИВНОМУ ПРАВОНАРУШЕНИЮ',
        kbk='18811630020016000140',
        oktmo='86701000',
        inn='1001041280',
        kpp='100101001',
        bik='017908101',
        account_number='03100643000000017600',
        payee_name='Управление федерального казначейства',
        payee_corr_account='40102810145370000066',
        div_id=1186190,
        amount=50000,
        payer_identifier='2400000000001111111111643',
        treasure_branch='',
        payer_name='',
        additional_data=AdditionalData(
            discount_date=datetime(2021, 12, 13, tzinfo=tzutc()).date(),
            discount_size='50',
            legal_act='Часть 2 статьи 12.9 КоАП',
            offense_name='Превышение скорости движения на величину более 20, но не более 40 км/ч',
            offense_place='Г. САРАТОВ, УЛ. ОРДЖОНИКИДЗЕ, Д.24Б (ИЗ ЦЕНТРА)',
            offense_date=datetime(2020, 1, 31, 13, 23, 0),
        ),
        payment_data=PaymentData(
            sum=10000,
            date=datetime(2016, 10, 23, 18, 0, tzinfo=tzutc()),
        ),
    )

    assert_that(charge.charge_data, equal_to(expected))
    assert response.charges[1].charge_data.additional_data is None
    assert response.charges[2].charge_data.additional_data is None
    assert_that(
        response.charges[3].charge_data.additional_data.offense_date,
        equal_to(datetime(2020, 1, 31)),
    )


@pytest.mark.asyncio
async def test_search_error(kazna_client: KaznaClient, aioresponses_mocker):
    aioresponses_mocker.post(
        'http://go.zora.yandex.net:1080',
        status=200,
        payload={"code": 101, "message": "Некорректно указан код документа."},
    )
    with pytest.raises(BaseKaznaInteractionError) as e:
        await kazna_client.search(
            SearchRequest(
                department=DepartmentType.GIBDD,
                documents=[PayerDoc(code=DocumentCode.VEHICLE_REGISTRATION_CERTIFICATE, value=['1111111111'])],
            )
        )
    assert e.value.params['code'] == 101
    assert e.value.params['message'] == 'Некорректно указан код документа.'


@pytest.mark.asyncio
async def test_search_empty_response(kazna_client: KaznaClient, aioresponses_mocker):
    aioresponses_mocker.post(
        'http://go.zora.yandex.net:1080', status=200, payload={"status": 'complete', "charges": []}
    )

    response = await kazna_client.search(
        SearchRequest(
            department=DepartmentType.GIBDD,
            documents=[PayerDoc(code=DocumentCode.VEHICLE_REGISTRATION_CERTIFICATE, value=['1111111111'])],
        )
    )

    assert response.status == SearchStatus.COMPLETE
    assert len(response.charges) == 0


@pytest.mark.asyncio
async def test_pay_success(kazna_client: KaznaClient, aioresponses_mocker):
    request = PayRequest(
        order_id='12345',
        dep_type=DepartmentType.GIBDD,
        pay_type=PayType.YANDEXPAY,
        kvit=True,
        payer_params=PayerParams(fio='Иванов Иван Иванович'),
        payment_params=PaymentParams(supplier_bill_id='18810177200855150244'),
        mpi_ext_info=MpiExtInfo(
            browser_accept_header="text/html,application/xhtml+xml,application/xml;q=0.9",
            browser_ip="127.0.0.1",
            browser_language="ru",
            browser_screen_height=1080,
            browser_screen_width=1920,
            browser_tz="-180",
            browser_user_agent="Mozilla/5.0(Windows NT 6.3; WOW64)",
            device_channel=DeviceChannel.BROWSER,
            browser_java_enabled=False,
            window_width=1920,
            window_height=1080,
            browser_color_depth=24,
            notification_url="https//mysite.com/acsreturnurl?session=12345",
            tds_notification_url="https://mysite.com/acscallbackurl?session=12345",
        ),
        yp_token='fake_token',
        return_url='https://testpartner.ru/e0462fc0872e2/',
    )

    def callback(url, **kwargs):
        data = kwargs['json']
        mpiExtInfo = data.pop('mpiExtInfo')
        assert data == {
            'orderID': '12345',
            'depType': 'gibdd',
            'payType': 'yandexpay',
            'kvit': True,
            'payerParams': {'fio': 'Иванов Иван Иванович'},
            'ypToken': 'fake_token',
            'returnUrl': 'https://testpartner.ru/e0462fc0872e2/',
            'sign': request.sign,
            'paymentParams': {'supplierBillID': '18810177200855150244'},
        }
        assert json.loads(mpiExtInfo) == {
            'browserIp': '127.0.0.1',
            'browserUserAgent': 'Mozilla/5.0(Windows NT 6.3; WOW64)',
            'windowWidth': 1920,
            'browserScreenHeight': 1080,
            'browserLanguage': 'ru',
            'browserTz': '-180',
            'windowHeight': 1080,
            'tdsNotificationURL': 'https://mysite.com/acscallbackurl?session=12345',
            'deviceChannel': '02',
            'browserAcceptHeader': 'text/html,application/xhtml+xml,application/xml;q=0.9',
            'browserJavaEnabled': False,
            'notificationURL': 'https//mysite.com/acsreturnurl?session=12345',
            'browserColorDepth': 24,
            'browserScreenWidth': 1920,
        }

        return CallbackResult(
            status=200,
            payload={
                'paymentID': 17447,
                '3ds': {
                    'acsUrl': 'https://testemulators.tprs.ru/emulator/mobimoneycard/crypto/emitent',
                    'termUrl': 'https://test3ds.tprs.ru/redirect/d84044d9c07a3da187561aa836a5dcfa',
                    'paReq': 'RXRvIHRlc3Rvdml5IHBhcmVxIGRseWEgcHJpbWVyYSAxMjM0NTY3ODkwISEhISE=',
                    'md': '789',
                },
            },
        )

    aioresponses_mocker.post(
        'http://go.zora.yandex.net:1080',
        callback=callback,
    )
    response = await kazna_client.pay(request)

    assert response.payment_id == 17447
    assert response.tds == TDS(
        acs_url='https://testemulators.tprs.ru/emulator/mobimoneycard/crypto/emitent',
        term_url='https://test3ds.tprs.ru/redirect/d84044d9c07a3da187561aa836a5dcfa',
        pa_req='RXRvIHRlc3Rvdml5IHBhcmVxIGRseWEgcHJpbWVyYSAxMjM0NTY3ODkwISEhISE=',
        md='789',
    )


@pytest.mark.asyncio
async def test_pay_error(kazna_client: KaznaClient, aioresponses_mocker):
    aioresponses_mocker.post(
        'http://go.zora.yandex.net:1080',
        status=200,
        payload={'code': 102, 'message': 'Отсутствуют обязательный параметр fio'},
    )
    with pytest.raises(KaznaAPIError) as e:
        await kazna_client.pay(
            PayRequest(
                order_id='12345',
                dep_type=DepartmentType.GIBDD,
                pay_type=PayType.YANDEXPAY,
                kvit=True,
                amount=50000,
                payer_params=PayerParams(fio=''),
                payment_params=PaymentParams(supplier_bill_id='18810177200855150244'),
                yp_token='fake_token',
                return_url='https://testpartner.ru/e0462fc0872e2/',
            )
        )

    assert e.value.params['code'] == 102
    assert e.value.params['message'] == 'Отсутствуют обязательный параметр fio'


@pytest.mark.asyncio
async def test_payment_info_success(kazna_client: KaznaClient, aioresponses_mocker):
    aioresponses_mocker.get(
        'http://go.zora.yandex.net:1080',
        status=200,
        payload={
            'paymentID': 17447,
            'orderID': '12345',
            'status': {'code': '1', 'name': 'created'},
            'depType': 'gibdd',
            'kvit': True,
            'amount': 50000,
            'totalSum': 51150,
            'payType': 'card',
            'paymentParams': {'supplierBillID': '18810177200855150244'},
            'result': [
                {
                    'DepartmentalInfo.DrawerStatus': '20',
                    'DepartmentalInfo.CBC': '18811630020016000140',
                    'DepartmentalInfo.OKATO': '86701000',
                    'DepartmentalInfo.PaytReason': '0',
                    'DepartmentalInfo.TaxPeriod': '0',
                    'DepartmentalInfo.DocNo': '24;1111111111',
                    'DepartmentalInfo.DocDate': '26.02.2021',
                    'DepartmentalInfo.TaxPaytKind': '0',
                    'PayerName': 'ООО НКО «МОБИ.ДЕНЬГИ-тест»//Иванов Иван Иванович//',
                    'PayerInn': '0',
                    'PayerKPP': '0',
                    'PayeeName': 'Управление федерального казначейства',
                    'PayeePersonalAcc': '03100643000000010200',
                    'PayeeCorrespAcc': '40102810545370000068',
                    'PayeeBic': '018142016',
                    'PayeeInn': '1001041280',
                    'PayeeKPP': '100101001',
                    'Purpose': 'ШТРАФ ПО АДМИНИСТРАТИВНОМУ ПРАВОНАРУШЕНИЮ, № 18810886586956606898',
                    'AccDocNo': '706',
                }
            ],
        },
    )
    response = await kazna_client.payment_info('17447')
    expected = PaymentInfoResponse(
        payment_id=17447,
        order_id='12345',
        status=PaymentStatus(code=PaymentStatusCode.CREATED, name='created'),
        dep_type=DepartmentType.GIBDD,
        kvit=True,
        amount=50000,
        total_sum=51150,
        pay_type=PayType.CARD,
        payment_params=PaymentParams(supplier_bill_id='18810177200855150244'),
        result=[
            {
                'DepartmentalInfo.DrawerStatus': '20',
                'DepartmentalInfo.CBC': '18811630020016000140',
                'DepartmentalInfo.OKATO': '86701000',
                'DepartmentalInfo.PaytReason': '0',
                'DepartmentalInfo.TaxPeriod': '0',
                'DepartmentalInfo.DocNo': '24;1111111111',
                'DepartmentalInfo.DocDate': '26.02.2021',
                'DepartmentalInfo.TaxPaytKind': '0',
                'PayerName': 'ООО НКО «МОБИ.ДЕНЬГИ-тест»//Иванов Иван Иванович//',
                'PayerInn': '0',
                'PayerKPP': '0',
                'PayeeName': 'Управление федерального казначейства',
                'PayeePersonalAcc': '03100643000000010200',
                'PayeeCorrespAcc': '40102810545370000068',
                'PayeeBic': '018142016',
                'PayeeInn': '1001041280',
                'PayeeKPP': '100101001',
                'Purpose': 'ШТРАФ ПО АДМИНИСТРАТИВНОМУ ПРАВОНАРУШЕНИЮ, № 18810886586956606898',
                'AccDocNo': '706',
            }
        ],
    )

    assert response == expected


@pytest.mark.asyncio
async def test_payment_info_not_found(kazna_client: KaznaClient, aioresponses_mocker):
    aioresponses_mocker.get(
        'http://go.zora.yandex.net:1080',
        status=200,
        payload={'code': 107, 'message': 'Платёж не найден'},
    )
    with pytest.raises(BaseKaznaInteractionError) as e:
        await kazna_client.payment_info('99999999999')

    assert e.value.params['code'] == 107
    assert e.value.params['message'] == 'Платёж не найден'


@pytest.mark.asyncio
async def test_get_subscription(kazna_client: KaznaClient, aioresponses_mocker):
    aioresponses_mocker.get(
        'http://go.zora.yandex.net:1080',
        status=200,
        payload={'payerDoc': [{'code': '22', 'value': ['1111111111']}]},
    )
    result = await kazna_client.get_subscription('subscription_id')

    assert_that(result, equal_to([PayerDoc(code=DocumentCode.DRIVER_LICENSE, value=['1111111111'])]))


@pytest.mark.asyncio
async def test_unsubscribe_success(kazna_client: KaznaClient, aioresponses_mocker):
    def callback(url, **kwargs):
        data = kwargs['json']
        assert_that(
            data, equal_to({'subscribe': 'subscription_id', 'payerDoc': [{'code': '22', 'value': ['1111111111']}]})
        )
        return CallbackResult(status=200, payload={'status': 0})

    aioresponses_mocker.post('http://go.zora.yandex.net:1080', callback=callback)

    documents = [PayerDoc(code=DocumentCode.DRIVER_LICENSE, value=['1111111111'])]
    await kazna_client.unsubscribe('subscription_id', documents)


@pytest.mark.asyncio
async def test_unsubscribe_fail(kazna_client: KaznaClient, aioresponses_mocker):
    aioresponses_mocker.post(
        'http://go.zora.yandex.net:1080',
        status=200,
        payload={'status': '1'},
    )

    documents = [PayerDoc(code=DocumentCode.DRIVER_LICENSE, value=['1111111111'])]

    with pytest.raises(BaseKaznaInteractionError):
        await kazna_client.unsubscribe('subscription_id', documents)


@pytest.mark.asyncio
async def test_pay_sign(bill_payments_settings):
    bill_payments_settings.KAZNA_SALT = 'SA9QXHKV'
    request = PayRequest(
        order_id='12345',
        dep_type=DepartmentType.GIBDD,
        pay_type=PayType.YANDEXPAY,
        kvit=True,
        payer_params=PayerParams(fio='Иванов Иван Иванович'),
        payment_params=PaymentParams(supplier_bill_id='18810177170712879661'),
        yp_token='fake_token',
        return_url='https://testpartner.ru/e0462fc0872e2/',
    )
    data, err = PayRequestSchema().dump(request)

    assert not err
    assert data['sign'] == '9192ab4b5a622b94fde0d2dbc52bfde3'  # значение из примера документации


def test_documents_validation_vehicle_registration():
    schema = VehicleRegistrationCertificate()

    schema.load_one({'code': DocumentCode.VEHICLE_REGISTRATION_CERTIFICATE, 'value': ['0134567890']})
    schema.load_one({'code': DocumentCode.VEHICLE_REGISTRATION_CERTIFICATE, 'value': ['19АЯ567890']})

    for invalid in [
        {'code': DocumentCode.VEHICLE_REGISTRATION_CERTIFICATE, 'value': ['123456789']},
        {'code': DocumentCode.VEHICLE_REGISTRATION_CERTIFICATE, 'value': ['0 АА567890']},
        {'code': DocumentCode.VEHICLE_REGISTRATION_CERTIFICATE, 'value': ['11FF567890']},
    ]:
        with pytest.raises(ValidationError):
            schema.load_one(invalid)


def test_documents_validation_driver_license():
    schema = DriverLicense()

    schema.load_one({'code': DocumentCode.DRIVER_LICENSE, 'value': ['0134567890']})
    schema.load_one({'code': DocumentCode.DRIVER_LICENSE, 'value': ['19АЯ567890']})

    for invalid in [
        {'code': DocumentCode.DRIVER_LICENSE, 'value': ['123456789']},
        {'code': DocumentCode.DRIVER_LICENSE, 'value': ['0 АА567890']},
        {'code': DocumentCode.DRIVER_LICENSE, 'value': ['11FF567890']},
    ]:
        with pytest.raises(ValidationError):
            schema.load_one(invalid)


@pytest.mark.parametrize(
    'tds_params, expected_url',
    (
        pytest.param(
            {'pa_req': 'ppp', 'term_url': 'ttt', 'md': 'mmm'},
            yarl.URL('https://acs.test/path?PaReq=ppp&TermUrl=ttt&MD=mmm'),
            id='3ds-v1',
        ),
        pytest.param(
            {'creq': 'qqq'},
            yarl.URL('https://acs.test/path?creq=qqq'),
            id='3ds-v2',
        ),
    ),
)
def test_tds_to_url(tds_params, expected_url):
    tds = TDS(acs_url='https://acs.test/path', **tds_params)

    url = tds.to_url()

    assert_that(url.with_query({}), equal_to(expected_url.with_query({})))
    assert_that(dict(url.query), equal_to(dict(expected_url.query)))


@pytest.mark.parametrize(
    'returned_code, expected_error_code',
    (
        pytest.param(104, KaznaAPIErrorCode.INVALID_SIGN, id='valid-code'),
        pytest.param(100000, None, id='unknown-code'),
        pytest.param(None, None, id='invalid-code-null'),
        pytest.param('xxx-yyy', None, id='invalid-code-str'),
    ),
)
def test_kazna_eror_code(returned_code, expected_error_code):
    error = KaznaAPIError(method='method', service='service', status_code=200, params={'code': returned_code})

    assert_that(error.kazna_code, equal_to(expected_error_code))
