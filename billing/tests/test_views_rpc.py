import json
import random
from datetime import datetime
from typing import Union, Callable
from uuid import uuid4
from xmlrpc import client as xmlrpclib

import pytest
from django.core.cache import cache

from bcl.banks.party_paypal.common import paypal_client, PayPalConnnector
from bcl.banks.party_raiff.common import RaiffFactoringConnector
from bcl.banks.party_sber.registry_operator import SberbankCardRegistry, SberbankSalaryRegistry
from bcl.banks.party_yoomoney.common import StatusBase
from bcl.banks.registry import PayPal, YooMoney, Sber, Unicredit, Raiffeisen, Tinkoff, Payoneer, RaiffeisenSpb, Qiwi
from bcl.core.models import Service, Payment, SalaryRegistry, states, RequestLog
from bcl.core.views.rpc import Rpc as Logic
from bcl.exceptions import BclException, NotEnoughArguments
from bcl.toolbox.utils import CipherUtils


@pytest.fixture
def make_call(request_factory, patch_tvm_auth):

    def make_call_(
        method,
        params,
        src_service_id=Service.TOLOKA,
        *,
        native: Union[bool, Callable] = False,
    ):
        logic = Logic()
        if native:
            # patch tvm authorization when using actual xmlrpc call
            patch_tvm_auth(None, src_service_id)

            if not callable(native):
                native = lambda req: req

            return logic(
                native(request_factory.post(
                    '/',
                    data=xmlrpclib.dumps(
                        (params,),
                        methodname=method,
                        encoding='utf-8'
                    ).encode('utf-8'),
                    content_type='text/xml; charset=utf-8;'
                ))
            ).content.decode('utf-8')
        # otherwise call Logic method directly
        method = getattr(logic, method)
        return method(*params, service=Service(src_service_id))

    return make_call_


def test_exception(make_call):

    def make_exception(request):
        request.__dict__['_body'] = 10200
        return request

    result = make_call('CancelPayments', {}, Service.TOLOKA, native=make_exception)
    assert "AttributeError: 'int' object has no attribute 'decode'" in result


def test_cancel_payments(
    get_source_payment, build_payment_bundle, monkeypatch, django_assert_num_queries, make_call
):
    bundle1 = build_payment_bundle(
        associate=PayPal, payment_dicts=[{'number_src': '1',}], service=True, h2h=True, account={'remote_id': 'sag'}
    )

    bundle2 = build_payment_bundle(
        associate=PayPal, payment_dicts=[{'number_src': '2',}], service=True, h2h=True, account={'remote_id': 'sag'}
    )

    bundle3 = build_payment_bundle(
        associate=PayPal, payment_dicts=[{'number_src': '3',}], service=Service.QA, h2h=True,
        account={'remote_id': 'sag'}
    )

    with django_assert_num_queries(3) as _:  # 3 запроса, включая два начала транзакций

        result = make_call('CancelPayments', [[
            bundle1.payments[0].number_src,
            bundle2.payments[0].number_src,
            '33',
            bundle3.payments[0].number_src,
        ]], Service.TOLOKA)

    assert bundle1.payments[0].number_src in result
    assert bundle2.payments[0].number_src in result
    assert result[bundle3.payments[0].number_src]['exception'] == 'payment not found'  # Транзакция другого сервиса.
    assert result['33']['exception'] == 'payment not found'

    # Далее сценарий для внешней системы с автоматизированным отвержением (см. PaymentDiscarder).

    bundle = build_payment_bundle(
        associate=PayPal, payment_dicts=[{'number_src': '4',}], service=Service.QA, h2h=True
    )

    def mock_get(*args, **kwargs):
        return {'items': [{'payout_item_id': 'xxx'}], 'payout_item_id': 'xxx'}

    def mock_post(*args, **kwargs):
        return {'items': [{'payout_item_id': 'xxx'}]}

    prefix = 'paypalrestsdk.Api.'
    monkeypatch.setattr(prefix + 'get', mock_get)
    monkeypatch.setattr(prefix + 'post', mock_post)

    # 3 запроса от PaymentDiscarder.updates_from_response()
    with django_assert_num_queries(5) as _:
        result = make_call('CancelPayments', ['4'], Service.QA, native=True)

    payment = bundle.payments[0]
    payment.refresh_from_db()

    assert '<name>success</name>' in result
    assert payment.status == states.REVOKED


def test_get_status(get_source_payment, make_call):
    payment1 = get_source_payment()
    payment2 = get_source_payment()

    result = make_call(
        'GetMultipleStatus',
        ([payment1.number_src, payment2.number_src, '123321'],),
        Service.OEBS,
    )

    assert result == [
        {'status': 'new', 'processing_notes': '', 'ic_id': payment1.number_src, 'remote_id': ''},
        {'status': 'new', 'processing_notes': '', 'ic_id': payment2.number_src, 'remote_id': ''},
        {'status': 'not found', 'processing_notes': '', 'ic_id': '123321', 'remote_id': ''},
    ]

    payment3 = get_source_payment({'status': states.ERROR})
    payment4 = get_source_payment({'status': states.ERROR}, associate=YooMoney)
    payment5 = get_source_payment({'status': states.USER_INVALIDATED})

    result = make_call(
        'GetMultipleStatus',
        ([payment3.number_src, payment4.number_src, payment5.number_src],),
        Service.OEBS,
    )

    assert result[0]['status'] == 'processing'
    assert result[0]['ic_id'] == payment3.number_src

    assert result[1]['status'] == 'denied_by_the_bank'
    assert result[1]['ic_id'] == payment4.number_src

    assert result[2]['status'] == 'user_invalidated'
    assert result[2]['ic_id'] == payment5.number_src

    payment6 = get_source_payment({'status': states.ERROR}, associate=Tinkoff)
    result = make_call(
        'GetMultipleStatus',
        ([payment6.number_src],),
        Service.OEBS,
    )

    assert result == [
        {'status': 'processing', 'processing_notes': '', 'ic_id': payment6.number_src, 'remote_id': ''},
    ]


@pytest.mark.parametrize('income_type', [None, '', '1'])
def test_set_pd(
    income_type, get_source_payment_dict, get_assoc_acc_curr, make_call
):
    associate, account, _ = get_assoc_acc_curr(Raiffeisen.id, account='40702810800000007671')
    payment_dict = get_source_payment_dict({'currency': 'RUB', 'f_acc': account.number}, associate=associate)
    payment_dict['ocherednostplatezha'] = payment_dict['priority']
    payment_dict['ic_id'] = payment_dict['number_src']
    payment_dict['docdate'] = payment_dict['date']

    name = 'Name'
    surname = 'SurName'
    midname = 'MidName'
    payment_dict['t_first_name'] = name
    payment_dict['t_last_name'] = surname
    payment_dict['t_middle_name'] = midname
    if income_type is not None:
        payment_dict['income_type'] = income_type

    result = make_call('SetPD', (payment_dict,), Service.OEBS)
    payment = Payment.getone(number=result)

    assert payment is not None
    assert payment.t_fio.split('|') == [surname, name, midname]
    assert payment.income_type == (income_type or '')


def test_set_pd_account_does_not_exists(get_source_payment_dict, make_call):
    acc_num = '66666666666666666666'

    payment_dict = get_source_payment_dict({'currency': 'RUB', 'f_acc': acc_num})
    payment_dict['ocherednostplatezha'] = payment_dict['priority']
    payment_dict['ic_id'] = payment_dict['number_src']
    payment_dict['docdate'] = payment_dict['date']

    with pytest.raises(BclException) as e:
        make_call('SetPD', (payment_dict,), Service.OEBS)

    assert f'Account {acc_num} is not registered' in e.value.msg


def test_set_pd_yoomoney(get_assoc_acc_curr, get_payment_bundle, init_user, make_call):
    user = init_user(robot=True)

    associate = YooMoney
    account = '12345678901234567890'
    currency = 'RUB'

    _, acc, _ = get_assoc_acc_curr(associate.id, account={'number': account, 'currency_code': currency})

    result = make_call('SetPD', {
        'ic_id': '123',
        'number': '321',
        # OEBS отправляет к нам сумму в <double>, эмулируем это поведение при помощи float
        'summ': float(400000),
        'currency': currency,
        'docdate': '2019-01-29',

        'f_acc': account,
        'f_cacc': '09876543210987654321',
        'f_bic': '7777777',
        'f_bankname': 'some',
        'f_inn': '12121212121',
        'f_kpp': '2222222',
        'f_name': 'from',

        't_acc': '012345678912345678900',
        't_cacc': '98765432109876543210',
        't_bic': '8888888',
        't_bankname': 'other',
        't_inn': '33333333333',
        't_kpp': '5555555',
        't_name': 'to',

        'ground': 'theground',
        'ocherednostplatezha': 5,
        'opertype': '1',
        'login': 'automat',

    }, Service.OEBS, native=True)

    assert '<value><string>1</string></value>' in result

    payment = list(Payment.objects.all())[0]
    assert payment.user.username == user.username

    contents = bytes(payment.bundle.file.zip_raw)

    assert b'4E+5' not in contents
    assert b'400000.00' in contents


def test_get_payments(get_source_payment, get_assoc_acc_curr, django_assert_num_queries, make_call, time_freeze):

    _, account, _ = get_assoc_acc_curr(PayPal, account='someacc')
    org = account.org

    with time_freeze('2021-12-01'):

        payment1 = get_source_payment(
            service=True,
            attrs={
                'f_acc': account.number,
                'service_id': Service.TOLOKA,
                'status': states.EXPORTED_H2H,
                'metadata': {'client_id': 1111},
            }, associate=PayPal)

        get_source_payment(
            service=True,
            attrs={
                'service_id': Service.TOLOKA,
                'status': states.EXPORTED_H2H,
                'metadata': {'client_id': 1111},
            }, associate=PayPal)

        payment2 = get_source_payment(
            service=True,
            attrs={
                'service_id': Service.TOLOKA,
                'status': states.BUNDLED,
                'metadata': {'client_id': 1111},
                'number_src': '123456789'
            }, associate=PayPal)

    expected_result = {
        'status': 'exported_to_h2h',
        'service': Service.TOLOKA,
        't_acc_type': 'default',
        'doc_number': int(payment1.number),
        'currency': payment1.currency_code,
        'summ': '152.00',
        'payment_system_answer': '{}',
        'params': '{}',
        'ground': payment1.ground,
        'processing_notes': '',
        'metadata': '{"client_id": 1111}',
        'associate_id': PayPal.id,
        'f_acc': 'someacc',
        'payment_system': '044525303',
        't_acc': '',
        'transaction_id': '',
        'remote_id': '',
    }

    # недостаточно параметров
    with pytest.raises(NotEnoughArguments):
        make_call('GetPayments', [{}], Service.BILLING_PARTNERS,)

    # фильтр по организации (через счёт)
    with django_assert_num_queries(1) as _:

        result = make_call(
            'GetPayments',
            [{
                'billing_service_id': 42,
                'from_dt': '2018-03-20',
                'statuses': [states.EXPORTED_H2H],
                'org_ids': [org.id],
            }],
            Service.BILLING_PARTNERS,
        )

    assert len(result) == 1
    assert expected_result in result

    # фильтр по дате - пусто
    with django_assert_num_queries(1) as _:

        result = make_call(
            'GetPayments',
            [{
                'billing_service_id': 42,
                'from_dt': '2021-12-02',
                'statuses': [states.EXPORTED_H2H],
            }],
            Service.BILLING_PARTNERS,
        )
    assert len(result) == 0

    # фильтр по дате - все записи
    with django_assert_num_queries(1) as _:

        result = make_call(
            'GetPayments',
            [{'billing_service_id': 42, 'from_dt': '2021-11-01'}],
            Service.BILLING_PARTNERS,
        )
    assert len(result) == 3

    # фильтр по ид
    with django_assert_num_queries(1) as _:

        result = make_call(
            'GetPayments',
            [{'transaction_ids': [payment2.number_src]}],
            Service.TOLOKA,
        )

    assert len(result) == 1
    assert result[0]['status'] == 'processing'


def test_send_payment(get_assoc_acc_curr, make_call, init_user):

    associate, account, _ = get_assoc_acc_curr(PayPal.id, account='1234')

    base_params = {
        'currency': 'RUB',
        'summ': '10.15',
        't_acc': 'some@one.else',
        't_acc_type': 'email',
        'ground': 'some purpose',
    }

    # новый формат
    params = dict(base_params)
    params.update({
        'transaction_id': '12345',
        'f_acc': account.number,
        'f_bik': associate.bid,
    })
    result = make_call('SendPayment', (params,))

    doc_number_1 = result.get('doc_number')

    assert doc_number_1
    assert isinstance(doc_number_1, int)
    assert result['payment_status'] == 'new'

    params.update({
        'transaction_id': '123456',
        'params': (
            '{"t_bank_alias": "RAIFFFEISEN", "t_inn": "3664069397", "t_first_name": "test1", "t_last_name": "test"}'
        ),
    })
    make_call('SendPayment', (params,))

    payment = Payment.objects.get(number_src=params['transaction_id'])
    assert payment.t_inn == '3664069397'
    assert payment.t_fio == 'test|test1|'
    assert payment.t_bic == 'RAIFFFEISEN'

    # платежи СБП
    init_user(robot=True)
    associate, account, _ = get_assoc_acc_curr(Raiffeisen.id, account='12345')
    account.sbp_payments = True
    account.save()

    params.update({
        'f_acc': account.number,
        'f_bik': associate.bid,
        'payout_type': Payment.PAYOUT_TYPE_SBP,
    })

    # проверка отсутствия повторной сборки платежа в пакет
    params.update({
        'transaction_id': '777',
    })

    # собираем однажды
    make_call('SendPayment', (params,))
    payment = Payment.objects.get(number_src=params['transaction_id'])
    bundle_id = payment.bundle_id
    assert bundle_id

    # повторно не собираем
    make_call('SendPayment', (params,))
    payment.refresh_from_db()
    assert payment.bundle_id == bundle_id

    # проверяем отмену автоматической сборки
    account.settings['manual_batch_sbp'] = True
    account.save()

    params.update({
        'transaction_id': '778',
    })

    make_call('SendPayment', (params,))
    payment = Payment.objects.get(number_src=params['transaction_id'])
    assert not payment.bundle_id


def test_probe_payment(
    get_assoc_acc_curr, mock_yoomoney_request_processor, response_mock, make_call
):
    associate, account, _ = get_assoc_acc_curr(YooMoney, account='1234')

    def call(status):

        error = ''

        if status == StatusBase.yoomoney_status_error:
            error = 'error="30"'

        with response_mock(
            'POST https://bo-demo02.yamoney.ru:9094/webservice/deposition/api/testDeposition -> 200 :'
            '<?xml version="1.0" encoding="UTF-8"?>'
            f'<testDepositionResponse clientOrderId="12345" status="{status}" '
            f'processedDT="2011-07-01T20:38:01.000Z" {error}/>'''
        ):
            result = make_call('ProbePayment', {
                'transaction_id': '12345',
                'f_acc': account.number,
                'f_bik': associate.bid,
                'currency': 'RUB',
                'summ': '10.15',
                't_acc': 'some@one.else',

            }, Service.TOLOKA, native=True)

        return result

    result = call(status=StatusBase.yoomoney_status_success)
    assert '<name>status_remote</name>\n<value><string>0</string></value>' in result
    assert '<name>status_bcl</name>\n<value><int>7</int></value>' in result

    result = call(status=StatusBase.yoomoney_status_error)
    assert '<name>status_remote</name>\n<value><string>30</string></value>' in result
    assert '<name>status_bcl</name>\n<value><int>2</int></value>' in result
    assert '<string>Технические проблемы на стороне Яндекс.Денег.</string>' in result


def test_pingpong_get_onboarding(response_mock, make_call):

    with response_mock(
        'POST https://test-ppapi.pingpongx.com/token/get -> 200:'
        '{"code": "0000", "message": "SUCCESS", "data": {"a": "b"}}'
    ):
        result = make_call('PingPongGetOnboardingLink', [{
            'seller_id': 1,
            'currency': 1,
            'country': 1,
            'store_name': 1,
            'store_url': 1,
            'notify_url': 1,
        }, None], Service.MARKET)

        assert result == {'a': 'b'}


def test_pingpong_get_seller_data(response_mock, make_call):
    with response_mock(
        'POST https://test-ppapi.pingpongx.com/account/status -> 200:'
        '{"code": "0000", "message": "SUCCESS", "data": {"a": "b"}}'
    ):
        result = make_call('PingPongGetSellerStatus', [{'seller_id': 1}, None], Service.MARKET)

        assert result == {'a': 'b'}


def test_cancel_payment(get_source_payment, make_call):
    payment = get_source_payment()

    result = make_call('CancelPayment', [payment.number_src, None], Service.OEBS)
    assert result == 'ok'

    payment.refresh_from_db()
    assert payment.status == states.CANCELLED


def test_get_statement_empty(get_assoc_acc_curr, make_call):
    _, account, _ = get_assoc_acc_curr(Sber)

    # итоговая
    result = make_call('GetStatement', [account, datetime.now(), None], Service.OEBS)

    assert result['credit_turnover'] == 0
    assert result['statement_body'] == []

    # промежуточная
    result = make_call('GetStatement', [account, datetime.now(), True], Service.OEBS)

    assert result['credit_turnover'] == '0'
    assert result['statement_body'] == []


def test_get_statement_corp_card_raiff(get_proved, make_call):
    """Проверяет заполнение date_valuated в ответе GetStatement для Райффайзен банка."""
    payment = get_proved(Raiffeisen)[0]
    register = payment.register

    result = make_call(
        'GetStatement',
        [register.account, register.statement_date, None],
        Service.OEBS
    )

    assert result['statement_body'][0]['docdatevaluated'] == str(payment.date_valuated)
    assert result['account'] == 'fakeacc'


def test_get_last_loaded_statement(get_proved, make_call):
    """Проверяет, что в ответе GetStatement отдаетсяя последняя загруженная выписка"""
    get_proved(Unicredit)

    payment = get_proved(Unicredit, proved_pay_kwargs={'summ': '55', 'number': '1234'})[0]
    register = payment.register

    result = make_call(
        'GetStatement',
        [register.account, register.statement_date, None],
        Service.OEBS
    )

    assert result['statement_body'][0]['summ'] == '55.00'
    assert result['statement_body'][0]['doc_number'] == '1234'


def test_get_statement_perf(
    get_source_payment, get_proved, django_assert_num_queries, make_call
):
    associate = Unicredit

    proved_pays = get_proved(associate, proved_pay_kwargs=[{}, {}, {}, {}])

    register = proved_pays[0].register

    for proved_pay in proved_pays:
        proved_pay.payment = get_source_payment()
        proved_pay.save()

    with django_assert_num_queries(3) as _:
        result = make_call(
            'GetStatement',
            [register.account, register.statement_date, False],
            Service.OEBS
        )
        assert len(result['statement_body']) == 4


def test_load_salary_card_registry(get_salary_contract, make_call):
    contract = get_salary_contract(Sber, number='38140753')

    registry_data = {
        'contract_number': '38140753',
        'employees': [{'address': {'apartment_number': '1513',
                                   'city': {'name': 'test', 'short_name': 'tm'},
                                   'country': {'code': 'RU', 'name': 'Russia', 'short_name': 'ru'},
                                   'district': {'name': 'test', 'short_name': 'tm'},
                                   'house_block': '1',
                                   'house_number': '13',
                                   'index': '125080',
                                   'region': {'name': 'test', 'short_name': 'tm'},
                                   'settlement': {'name': 'test', 'short_name': 'tm'},
                                   'street': {'name': 'test', 'short_name': 'tm'}},
                       'address_of_residence': {'apartment_number': '1513',
                                                'city': {'name': 'test', 'short_name': 'tm'},
                                                'country': {'code': 'RU', 'name': 'Russia',
                                                            'short_name': 'ru'},
                                                'district': {'name': 'test', 'short_name': 'tm'},
                                                'house_block': '1',
                                                'house_number': '13',
                                                'index': '125080',
                                                'region': {'name': 'test', 'short_name': 'tm'},
                                                'settlement': {'name': 'test', 'short_name': 'tm'},
                                                'street': {'name': 'test', 'short_name': 'tm'}},
                       'address_of_work': {'apartment_number': '1513',
                                           'city': {'name': 'test', 'short_name': 'tm'},
                                           'country': {'code': 'RU', 'name': 'Russia', 'short_name': 'ru'},
                                           'district': {'name': 'test', 'short_name': 'tm'},
                                           'house_block': '1',
                                           'house_number': '13',
                                           'index': '125080',
                                           'region': {'name': 'test', 'short_name': 'tm'},
                                           'settlement': {'name': 'test', 'short_name': 'tm'},
                                           'street': {'name': 'test', 'short_name': 'tm'}},
                       'birthday_date': '2018-03-22 10:30:00',
                       'bonus_member_number': 30706,
                       'bonus_program': 'sd',
                       'branch_office_number': '6666',
                       'citizenship': 'Russia',
                       'embossed_text': {'field1': 'test', 'field2': 'test'},
                       'first_name': 'test',
                       'secret_word': 'itsmysecret',
                       'product': 'MC_CORPORATE',
                       'identify_card': {'card_type': 'ZVExBnSOlJ',
                                         'card_type_code': 712,
                                         'issue_date': '2018-03-22',
                                         'issued_by': 'test',
                                         'number': '770-070/21/1678',
                                         'series': 'xxx',
                                         'subdivision_code': '666'},
                       'foreigner_card': {'card_type': 'ZVExBnSOlJ',
                                         'card_type_code': 712,
                                         'issue_date': '2018-03-22',
                                         'number': '0001011/1111/11',
                                         'series': '',
                                         'valid_date': '2018-03-22'},
                       'last_name': 'test',
                       'mobile_phone': '915123456711111',
                       'office_number': '6666',
                       'patronymic': 'test',
                       'place_of_birthday': {'apartment_number': '1513',
                                             'city': {'name': 'test', 'short_name': 'tm'},
                                             'country': {'code': 'RU', 'name': 'Russia',
                                                         'short_name': 'ru'},
                                             'district': {'name': 'test', 'short_name': 'tm'},
                                             'house_block': '1',
                                             'house_number': '13',
                                             'index': '125080',
                                             'region': {'name': 't'*51, 'short_name': 'tm'},
                                             'settlement': {'name': 'test', 'short_name': 'tm'},
                                             'street': {'name': 'test', 'short_name': 'tm'}},
                       'position': 'test',
                       'record_id': '1',
                       'resident': False,
                       'sex': 'M'}],
        'registry_date': '2018-03-22',
        'registry_number': u'002',
        'registry_guid': str(uuid4()),
    }

    result = make_call('LoadCardRegistry', registry_data, Service.OEBS, native=True)

    assert '<value><string>ok</string></value>' in result
    assert SalaryRegistry.objects.count() == 1

    # Проверим, что при сериализации в словарь попадут данные по умолчанию,
    # даже если узел с данными отсутствует в исходных данных.
    # В нашем случае не был передан узел card_type в корне документа.
    registry = SberbankCardRegistry.objects.get()
    reg_contents = registry.outgoing_compile()[1].decode('cp1251')
    assert '<ВидВклада КодВидаВклада="51" КодПодвидаВклада="20" КодВалюты="810"/>' in reg_contents
    assert CipherUtils.decipher_text(registry.employees[0]['secret_word']) == 'itsmysecret'

    result = make_call('ReceiveCardRegistryResponse', {
        'registry_number': registry_data['registry_number'],
        'registry_guid': registry_data['registry_guid'],
        'contract_number': contract.number
    }, Service.OEBS, native=True)

    assert '<name>status</name>\n<value><int>0</int></value>' in result

    result = make_call('LoadCardRegistry', registry_data, Service.OEBS, native=True)
    assert 'SalaryAlreadyExistsError: registry 002 already exists' in result
    assert SalaryRegistry.objects.count() == 1

    registry_data.update({'registry_guid': str(uuid4())})
    result = make_call('LoadCardRegistry', registry_data, Service.OEBS, native=True)
    assert '<value><string>ok</string></value>' in result
    assert SalaryRegistry.objects.count() == 2

    result = make_call('ReceiveCardRegistryResponse', {
        'registry_number': registry_data['registry_number'],
        'contract_number': contract.number
    }, Service.OEBS, native=True)

    assert 'SberbankCardRegistry' in result

    registry_data['employees'][0].update({'card_type': {'card_subtype_code': '6', 'card_type_code': '31'}})

    reg_id = uuid4()
    registry_data.update({'registry_guid': str(reg_id)})
    make_call('LoadCardRegistry', registry_data, Service.OEBS, native=True)
    reg_contents = SberbankCardRegistry.objects.get(registry_id=reg_id).outgoing_compile()[1].decode('cp1251')
    assert SalaryRegistry.objects.count() == 3
    assert '<ВидВклада КодВидаВклада="31" КодПодвидаВклада="6" КодВалюты="810"/>' in reg_contents

    registry_data.update({'contract_number': '00000000'})
    result = make_call('LoadCardRegistry', registry_data, Service.OEBS, native=True)

    assert '<value><string>salary contract 00000000 not found</string></value>' in result
    assert SalaryRegistry.objects.count() == 3


def test_load_salary_registry(get_salary_contract, make_call):
    contract = get_salary_contract(Sber, number='38140753')

    registry_data = {
        'contract_number': '38140753',
        'employees':
        [
            {
                'patronymic': 'Петрович',
                'first_name': 'Иван',
                'last_name': 'Федоров',
                'office_number': '123',
                'branch_office_number': '1234',
                'record_id': 1,
                'personal_account': '40702810200001005224',
                'amount': '199.99',
                'currency_code': 643
            }
        ],
        'registry_date': '2018-03-22',
        'registry_number': '002',
        'registry_guid': str(uuid4()),
    }

    result = make_call('LoadSalaryRegistry', registry_data, Service.OEBS, native=True)

    assert '<value><string>ok</string></value>' in result
    assert SalaryRegistry.objects.count() == 1

    result = make_call('ReceiveSalaryRegistryResponse', {
        'registry_number': registry_data['registry_number'],
        'registry_guid': registry_data['registry_guid'],
        'contract_number': contract.number
    }, Service.OEBS, native=True)

    assert '<name>status</name>\n<value><int>0</int></value>' in result

    result = make_call('LoadSalaryRegistry', registry_data, Service.OEBS, native=True)
    assert 'SalaryAlreadyExistsError: registry 002 already exists' in result
    assert SalaryRegistry.objects.count() == 1

    registry_data.update({'registry_guid': str(uuid4())})
    result = make_call('LoadSalaryRegistry', registry_data, Service.OEBS, native=True)
    assert '<value><string>ok</string></value>' in result
    assert SalaryRegistry.objects.count() == 2

    reg_id = uuid4()
    registry_data.update({'registry_guid': str(reg_id)})
    registry_data.update({'income_type': '1'})
    registry_data.update({'transfer_code': '01'})
    registry_data['employees'][0].update({'amount_deduct': '11.10'})

    make_call('LoadSalaryRegistry', registry_data, Service.OEBS, native=True)
    registry = SberbankSalaryRegistry.objects.get(registry_id=reg_id)
    assert registry.income_type == registry_data['income_type']
    assert registry.transfer_code == registry_data['transfer_code']
    assert str(registry.employees[0].amount_deduct) == registry_data['employees'][0]['amount_deduct']


def test_no_sentry(mocker, monkeypatch, make_call):
    func_capture = mocker.patch('bcl.core.views.rpc.capture_exception')

    def create_token(*args):
        raise paypal_client.exceptions.BadRequest('bad')

    monkeypatch.setattr(PayPalConnnector, 'create_token', create_token)

    result = make_call(
        'PayPalTokenCreateFromAuthorizationCode',
        {
            'AuthorizationCode': 'x',
            'RedirectUrl': 'y',
        },
        Service.TOLOKA, native=True
    )

    assert 'bad' in result
    assert not func_capture.called

    result = make_call(
        'PayPalTokenCreateFromAuthorizationCode',
        {},
        Service.TOLOKA, native=True
    )
    assert 'is mandatory' in result
    assert func_capture.called


def test_get_login_link_payoneer(response_mock, make_call):

    def call(audit_id=56025866):

        with response_mock(
            'POST https://api.sandbox.payoneer.com/v2/programs/100101370/payees/login-link -> 200:'
            f'{{"audit_id":{audit_id},"code":0,"description":"Success",'
            '"login_link":"https://payouts.sandbox.payoneer.com/partners/lp.aspx?'
            'token=32a0597872d0485db0428914e3a3b628916497879E"}'
        ):
            result_call = make_call('PayoneerGetLoginLink', {
                'program_id': '100101370',
                'payee_id': '14327891927',
                'options': '{"one": "two"}',
            }, Service.TOLOKA, native=True)

        return result_call

    result = call()
    assert '<name>audit_id</name>\n<value><string>56025866</string></value>' in result
    assert '<name>code</name>\n<value><int>0</int></value>' in result
    assert '<name>description</name>\n<value><string>Success</string></value>' in result
    assert ('<name>login_link</name>\n<value><string>https://payouts.sandbox.payoneer.com/partners/lp.aspx?'
            'token=32a0597872d0485db0428914e3a3b628916497879E</string></value>') in result

    result = call(2166201939)
    assert '<name>audit_id</name>\n<value><string>2166201939</string></value>' in result


def test_get_payee_status_payoneer(response_mock, make_call):

    def call(payee_id, body, status_code=200):

        with response_mock(
            f'GET https://api.sandbox.payoneer.com/v2/programs/100101370/payees/{payee_id}/status '
            f'-> {status_code} :{body}'
        ):
            result_call = make_call('PayoneerGetPayeeStatus', {
                'program_id': '100101370',
                'payee_id': payee_id,
            }, Service.TOLOKA, native=True)

        return result_call
    result = call('123321123', '{"audit_id":56026044,"code":0,"description":"Success","status":"ACTIVE"}')
    assert '<name>audit_id</name>\n<value><string>56026044</string></value>' in result
    assert '<name>code</name>\n<value><int>0</int></value>' in result
    assert '<name>description</name>\n<value><string>Success</string></value>' in result
    assert '<name>status</name>\n<value><string>ACTIVE</string></value>' in result

    result = call(
        '12332198903',
        '{"audit_id": 56026052, "code": 10005, "description": "Payee was not found", '
        '"hint": "Please ensure that the payee has registered with Payoneer"}', 404
    )
    assert '<name>method</name>\n<value><string>yandex_balalayka.PayoneerGetPayeeStatus</string></value>' in result
    assert '<name>status</name>\n<value><string>error</string></value>' in result
    assert '<name>traceback</name>\n<value><string>Traceback (most recent call last)' in result
    assert 'Payee was not found' in result
    assert 'UserHandledException' in result


def test_probe_payment_payoneer(get_assoc_acc_curr, response_mock, make_call):
    associate = Payoneer

    _, acc, _ = get_assoc_acc_curr(
        associate.id, account={'number': '111222333', 'currency_code': 'USD', 'remote_id': 'toloka-100101370'}
    )

    def call(payee_id, body, status_code=200):

        with response_mock(
            f'GET https://api.sandbox.payoneer.com/v2/programs/100101370/payees/{payee_id}/status'
            f'-> {status_code} :{body}'
        ):
            result_call = make_call('ProbePayment', {
                'transaction_id': '12345',
                'f_acc': acc.number,
                'f_bik': associate.bid,
                'currency': 'USD',
                'summ': '23.15',
                't_acc': payee_id,

            }, Service.TOLOKA, native=True)

        return result_call

    result = call('123321123', '{"audit_id":56026044,"code":0,"description":"Success","status":"ACTIVE"}')
    assert '<name>status_bcl</name>\n<value><int>7</int></value>' in result
    assert '<name>status_remote</name>\n<value><int>0</int></value>' in result
    assert '<name>status_remote_hint</name>\n<value><string>Success. Audit ID: 56026044</string></value>' in result

    result = call(
        '12332198903',
        '{"audit_id": 56026052, "code": 10005, "description": "Payee was not found", '
        '"hint": "Please ensure that the payee has registered with Payoneer"}', 404
    )
    assert '<name>status_bcl</name>\n<value><int>2</int></value>' in result
    assert '<name>status_remote</name>\n<value><int>10005</int></value>' in result
    assert ('<name>status_remote_hint</name>\n<value><string>'
            'Payee was not found. Audit ID: 56026052</string></value>') in result


def test_account_info(
    get_source_payment, get_assoc_acc_curr, get_payment_bundle, make_call
):
    """Тест ручки отображения блокировок счетов."""
    _, account, _ = get_assoc_acc_curr(Raiffeisen.id, account='407028103000010')

    account.blocked = account.HARD_BLOCKED
    account.save()

    result = make_call(
        'GetAccountInfo',
        {'number': '407028103000010'},
        Service.OEBS, native=True,
    )

    assert '<name>blocked</name>\n<value><int>2</int></value>\n' in result

    account.blocked = account.SOFT_BLOCKED
    account.save()

    result = make_call(
        'GetAccountInfo',
        {'number': '407028103000010'},
        Service.TOLOKA, native=True,
    )

    assert '<name>blocked</name>\n<value><int>1</int></value>\n' in result

    account.blocked = account.NON_BLOCKED
    account.save()

    result = make_call(
        'GetAccountInfo',
        {"number": '407028103000010'},
        Service.TOLOKA, native=True,
    )

    assert '<name>blocked</name>\n<value><int>0</int></value>\n' in result


NOW_DATE = datetime.now()

ADDRESS_PARAMS = {
    'city': {'name': 'Москва', 'short_name': 'г'},
    'street': {'name': 'Льва Толстого', 'short_name': 'ул'},
    'district': {'name': 'Московский', 'short_name': ''},
    'country': {'code': '', 'name': 'Российская Федерация', 'short_name': ''},
    'region': {'name': 'Москва', 'short_name': 'г'},
    'settlement': {'name': '', 'short_name': ''},
    'house_block': '', 'apartment_number': '15',
    'index': '125080', 'house_number': '16',
}

CARD_PARAMS = {
    'registry_date': NOW_DATE,
    'registry_guid': '696d371c-feef-4e2a-9d88-2784c52808f1',
    'registry_number': '723',
    'contract_number': '044525700_10',
    'employees': [
        {
            'first_name': 'Иван',
            'last_name': 'Иванов',
            'citizenship': 'Российская Федерация',
            'resident': True,
            'address_of_work': ADDRESS_PARAMS,
            'place_of_birthday': ADDRESS_PARAMS,
            'address_of_residence': ADDRESS_PARAMS,
            'embossed_text': {'field2': 'IVAN', 'field1': 'IVANOV'},
            'sex': 'M',
            'card_type': {'card_type_code': '51', 'card_subtype_code': '5'},
            'patronymic': 'Петрович', 'mobile_phone': '9151234567',
            'office_number': '203', 'branch_office_number': '',
            'birthday_date': NOW_DATE,
            'record_id': '1',
            'position': 'Стажер',
            'identify_card': {
                'issue_date': NOW_DATE, 'series': '1112',
                'issued_by': 'ОВД "Ховрино" гор. Москвы',
                'number': '123456',
                'card_type': 'Паспорт гражданина Российской Федерации',
                'subdivision_code': '123-456', 'card_type_code': 21
            },
            'address': ADDRESS_PARAMS,
            'addrs_coincide': True,
            'citizenship_country_iso': '643',
            'snils': '18471320368',
            'register_country_iso': '643'
        }
    ]
}

SALARY_PARAMS = {
    'registry_date': NOW_DATE,
    'registry_guid': '6f6ecfef-ee18-4ee6-872d-1ef2dab912f2',
    'registry_number': '730',
    'contract_number': '38142973',
    'employees':
        [
            {
                'patronymic': 'Петрович',
                'first_name': 'Иван',
                'last_name': 'Федоров',
                'office_number': '123',
                'branch_office_number': '1234',
                'record_id': 1,
                'personal_account': '40702810200001005224',
                'amount': '19999',
                'currency_code': 643
            }
        ]
}

DISSMISS_PARAMS = {
    'registry_date': NOW_DATE,
    'registry_guid': '6f6ecfef-ee18-4ee6-872d-1ef2dab912f2',
    'registry_number': '730',
    'contract_number': '38142973',
    'employees':
        [
            {
                'patronymic': 'Петрович',
                'first_name': 'Иван',
                'last_name': 'Федоров',
                'dismissal_date': NOW_DATE,
                'record_id': 1,
                'personal_account': '40702810200001005224'
            }
        ]

}

PARAMS_BY_TYPE = {
    'card': CARD_PARAMS,
    'salary': SALARY_PARAMS,
    'dismissal': DISSMISS_PARAMS
}


@pytest.mark.parametrize(
    'associate, reg_type, expected_state, contract_num, org_name, org_inn, org_acc',
    [
        (Sber, 'card', 'счетОткрыт', '38143037', 'ООО Яндекс.Технологии', '7704414297', '40702810538000111471'),
        (Tinkoff, 'card', 'счетОткрыт', '044525974_151283', 'ООО Партия еды', '7801292630', '30232810100000000305'),
        (Raiffeisen, 'card', 'Исполнена', '044525700_10', 'ООО Кинопоиск', '7710688352', '40702810900000021641'),
        (RaiffeisenSpb, 'card', 'Исполнена', '044030723_1', 'ООО Партия еды', '7801292630', '47422810101000015027'),

        (Tinkoff, 'salary', 'Зачислено', '044525974_151283', 'ООО Партия еды', '7801292630', '30232810100000000305'),
        (Sber, 'salary', 'Зачислено', '38143037', 'ООО Яндекс.Технологии', '7704414297', '40702810538000111471'),
        (Raiffeisen, 'salary', 'Исполнена', '044525700_10', 'ООО Кинопоиск', '7710688352', '40702810900000021641'),
        (RaiffeisenSpb, 'salary', 'Исполнена', '044030723_1', 'ООО Партия еды', '7801292630', '47422810101000015027'),

        (Tinkoff, 'dismissal', 'счетОткрыт', '044525974_151283', 'ООО Партия еды', '7801292630', '30232810100000000305'),
        (Raiffeisen, 'dismissal', 'Исполнена', '044525700_10', 'ООО Кинопоиск', '7710688352', '40702810900000021641'),
        (RaiffeisenSpb, 'dismissal', 'Исполнена', '044030723_1', 'ООО Партия еды', '7801292630',
         '47422810101000015027'),
    ]
)
def test_salary_registries(
    associate, reg_type, expected_state, contract_num, org_name, org_inn, org_acc,
    get_salary_contract, read_fixture, make_call
):

    def prepare_file_by_bank(
        file_data, params, org_name='ООО ГИС Технологии', org_inn='7702688528', org_acc='40702810600001430560'
    ):

        if associate_raiff:
            file_data = file_data.replace('23.07.2019', params['registry_date'].strftime('%d.%m.%Y'))
            file_data = file_data.replace('org_name', org_name).replace('org_inn', org_inn).replace('org_acc', org_acc)
            file_data = file_data.replace('reg_num', params['registry_number'].zfill(6))
            file_data = file_data.replace('registry_guid', params['registry_guid'])
            file_data = file_data.replace('registry_guid', '')

        else:
            file_data = file_data.replace('2019-07-26', params['registry_date'].strftime('%Y-%m-%d'))
            file_data = file_data.replace('reg_num', params['registry_number'])
            file_data = file_data.replace('registry_guid', params['registry_guid'])

        return file_data

    get_salary_contract(associate, org={'name': org_name, 'inn': org_inn}, account=org_acc, number=contract_num)

    params = PARAMS_BY_TYPE[reg_type]
    associate_raiff = associate in (Raiffeisen, RaiffeisenSpb)

    template_answer = 'salary/%s_%s_answer.%s' % (
        Raiffeisen.alias if associate_raiff else associate.alias, reg_type, 'csv' if associate_raiff else 'xml'
    )

    template_name = 'salary/%s_%s.%s' % (
        Raiffeisen.alias if associate_raiff else associate.alias, reg_type, 'csv' if associate_raiff else 'xml')

    params.update(
        {
            'registry_guid': str(uuid4()),
            'registry_number': '%s%s' % (
                random.randint(1, 9) if associate_raiff else '', random.randint(100, 999)
            ),
            'registry_date': datetime(NOW_DATE.year, NOW_DATE.month, NOW_DATE.day),
            'contract_number': contract_num
        }
    )
    if associate_raiff:
        params['employees'][0].update(
            {'target_bik': '044525225', 'card_number': '1234567898765432'}
        )
    result = make_call(f'Load{reg_type.capitalize()}Registry', params, Service.OEBS, native=True)
    assert '<string>ok</string></value>' in result
    registry_obj = SalaryRegistry.objects.get(associate_id=associate.id, registry_id=params['registry_guid'])

    registry = make_call(
        f'Receive{reg_type.capitalize()}RegistryResponse',
        {
            'registry_number': params['registry_number'],
            'registry_guid': params['registry_guid'],
            'contract_number': params['contract_number']
        },
        Service.OEBS, native=True
    )

    assert '<name>status</name>\n<value><int>0</int></value>' in registry
    assert '<name>employees</name>\n<value><array><data>\n</data></array>' in registry
    salary_class = getattr(
        associate.registry_operator, f'type_salary'
        if reg_type == 'salary' else 'type_dismiss'
        if reg_type == 'dismissal' else 'type_cards')

    downloaded_file = salary_class.objects.get(
        registry_id=params['registry_guid']).outgoing_compile()[1].decode('cp1251')

    file_data = read_fixture(template_name, decode='cp1251')

    assert downloaded_file == prepare_file_by_bank(file_data, params, org_name, org_inn, org_acc)

    if reg_type != 'dismissal':

        parsed_reg = salary_class.incoming_parse(
            prepare_file_by_bank(
                read_fixture(template_answer, decode='cp1251'), params, org_name, org_inn, org_acc
            ).encode('cp1251')
        )

        registry_out = salary_class.incoming_get_registry(associate.registry_operator, parsed_reg)
        registry_out.incoming_save(parsed_reg)

        registry = make_call(
            f'Receive{reg_type.capitalize()}RegistryResponse',
            {
                'registry_number': params['registry_number'],
                'registry_guid': params['registry_guid'],
                'contract_number': params['contract_number']
            },
            Service.OEBS, native=True
        )
        registry_obj.refresh_from_db()

        assert '<name>status</name>\n<value><int>1</int></value>' in registry
        assert '<name>response</name>' in registry
        assert '<name>processing_notes</name>' in registry
        assert registry_obj.status == states.REGISTER_ANSWER_LOADED
        assert registry_obj.employees[0].result == expected_state


def test_qiwi_shop_output_config(
    response_mock, get_assoc_acc_curr, monkeypatch, make_call
):
    _, acc, _ = get_assoc_acc_curr(Qiwi, account='17')
    acc.remote_id = 'toloka'
    acc.save()

    def mock_log(*args, **kwargs):
        pass

    monkeypatch.setattr(RequestLog, 'add', mock_log)

    def qiwi_call(status_code, body):
        with response_mock(
            f'POST https://api-test.contactpay.io/gateway/v1/shop_output_config/shop -> {status_code} :{body}'
        ):
            result_call = make_call(
                'QiwiGetConfig',
                {'shop_id': acc.number},
                Service.TOLOKA, native=True
            )
            return result_call

    result = qiwi_call(
        200,
        '{"result": true, "message": "Ok", "error_code": 0, '
        '"data": [{"id": 10, "name": "card-usd", "rating": 2, '
        '"payways": [{"alias": "some-payway-usd", "currency": 978, "min_amount": 100, "max_amount": 1000000, '
        '"fee_config": {"percent": 0.1, "fix": 0.3, "min": 10, "max": 55}, '
        '"account_info_config": {"account": {"regex": "d{9,15}$", "title": "Номер Qiwi кошелька"}}}]}]}'
    )
    assert '<name>result</name>\n<value><boolean>1</boolean></value>' in result
    assert '<name>message</name>\n<value><string>Ok</string></value>' in result

    result = qiwi_call(
        200,
        '{"result": false, "message": "Shop (id = 1) is not found", "error_code": 11, "data": null}'
    )
    assert '<value><string>yandex_balalayka.QiwiGetConfig</string></value>' in result
    assert 'UserHandledException' in result


def test_probe_payment_acc_not_found(make_call):
    result = make_call(
        'ProbePayment', {
            'transaction_id': '12345',
            'f_acc': '7382478',
            'f_bik': Qiwi.bid,
            'currency': 'RUB',
            'summ': 101,
            't_acc': '79991112233',
            't_acc_type': 'wallet',
        }, Service.TOLOKA, native=True
    )
    assert 'UserHandledException' in result


def test_qiwi_probe_payment(
    response_mock, get_assoc_acc_curr, monkeypatch, make_call
):
    associate, acc, _ = get_assoc_acc_curr(Qiwi, account='17')
    acc.remote_id = 'toloka'
    acc.save()

    def mock_log(*args, **kwargs):
        pass
    monkeypatch.setattr(RequestLog, 'add', mock_log)

    def qiwi_call(status_code, body):
        with response_mock(
            f'POST https://api-test.contactpay.io/gateway/v1/check_account -> {status_code} :{body}'
        ):
            result_call = make_call(
                'ProbePayment', {
                    'transaction_id': '12345',
                    'f_acc': acc.number,
                    'f_bik': associate.bid,
                    'currency': 'RUB',
                    'summ': 101,
                    't_acc': '79991112233',
                    't_acc_type': 'wallet',
                }, Service.TOLOKA, native=True
            )
            return result_call

    result = qiwi_call(
        200,
        '{"result":true, "message":"Ok", "error_code":0, '
        '"data":{"account_info":{"first_name":"Ivan","last_name":"Ivanov"}, "result":true,"provider_status":1}}'
    )
    assert '<name>status_bcl</name>\n<value><int>10</int></value>' in result
    assert '<name>status_remote</name>\n<value><int>1</int></value>' in result

    result = qiwi_call(
        200,
        '{"result": false, "message": "Shop (id = 1) is not found", "error_code": 11, "data": null}'
    )
    assert '<name>status_bcl</name>\n<value><int>2</int></value>' in result
    assert '<name>status_remote</name>\n<value><int>11</int></value>' in result


def test_qiwi_payment_calculation(
    get_assoc_acc_curr, response_mock, make_call
):
    _, account, _ = get_assoc_acc_curr(Qiwi, account='17')

    def do_try(*, status, body):

        with response_mock(f'POST https://api-test.contactpay.io/gateway/v1/withdraw/try -> {status} :{body}'):

            result_call = make_call(
                'QiwiPaymentCalculation', {
                    'shop_id': account.number,
                    'shop_currency': 643,
                    'payway': 'qiwi_topup_rub_to_wallet[mock_server,success]',
                    'amount': 101,
                    'amount_type': 'ps_amount',
                }, Service.TOLOKA, native=True
            )
            return result_call

    # Пользователь активен.
    result = do_try(
        status=200,
        body='{"data":{"account_info_config":{"account":{"regex":"^[0-9]{11,12}$","title":"your account"}},"info":{},'
             '"payee_receive":101.0,"ps_currency":643,"shop_currency":643,"shop_write_off":103.02},"error_code":0,'
             '"message":"Ok","result":true}'
    )

    assert '<name>result</name>\n<value><boolean>1</boolean></value>' in result

    # Пользователь неактивен.
    result = do_try(
        status=200,
        body='{"result": false, "message": "Shop (id = 1) is not found", "error_code": 11, "data": null}'
    )
    assert 'UserHandledException: {"result": false, "message": "Shop (id = 1) is not found"' in result


def test_get_fps_banks(mocker, get_assoc_acc_curr, response_mock, make_call):
    _, account, _ = get_assoc_acc_curr(Raiffeisen, account='11111111')
    org = account.org
    org.connection_id = 'probki'
    org.save()

    result = make_call('FPSBankList', {'acc_num': account.number}, Service.TOLOKA, native=True)
    assert 'ArgumentError' in result

    result = make_call('FPSBankList', {'bik': Sber.bid}, Service.TOLOKA, native=True)
    assert 'ArgumentError' in result

    account.sbp_payments = True
    account.save()
    cache.clear()

    with response_mock(
        'GET https://test.ecom.raiffeisen.ru/api/payout/v1/sbp/banks/ -> 200 : '
        '[{"alias": "RAIFFFEISEN", "name": "Райффайзенбанк"}]'
    ):
        result1 = make_call('FPSBankList', {'acc_num': account.number}, Service.TOLOKA, native=True)
        result2 = make_call('FPSBankList', {'bik': Raiffeisen.bid}, Service.TOLOKA, native=True)
    assert 'RAIFFFEISEN' in result1
    assert 'RAIFFFEISEN' in result2

    func_capture = mocker.patch('bcl.core.views.rpc.capture_exception')

    with response_mock(
        'GET https://test.ecom.raiffeisen.ru/api/payout/v1/sbp/banks/ -> 500 : '
    ):
        make_call('FPSBankList', {'bik': Raiffeisen.bid}, Service.TOLOKA, native=True)
    assert not func_capture.called


def test_raiff_fps_probe_payment(
    response_mock, get_assoc_acc_curr, monkeypatch, make_call
):
    _, account, _ = get_assoc_acc_curr(Raiffeisen, account={'number': '11111111', 'sbp_payments': True})
    org = account.org
    org.connection_id = 'probki'

    org.save()

    use_sandbox = False

    def mock_log(*args, **kwargs):
        pass

    monkeypatch.setattr(RequestLog, 'add', mock_log)
    raiff_answer_sucess = (
        '{"id": "1404fhr7i272a2", "account": "40700000000000000000", "amount": 1110.01, "currency": "RUB", '
        '"payoutMethod": "SBP", "payoutParams": { "phone": "79191234567", "bankAlias": "RAIFFEISEN", '
        '"firstName": "Петр", "middleName": "Петрович", "lastName": "Петров", "inn": 123456789101}, '
        '"incomeTypeCode": "1", "extra": {"contract": "1234567/89012"}, "status": {"value": "PENDING", '
        '"date": "2019-07-11T14:45:13.000Z"}, "createDate": "2019-07-11T14:45:13.000Z"}'
    )

    def raiff_call(status_code=200, raiff_answer=None):

        with response_mock(
            [f'POST https://test.ecom.raiffeisen.ru/api/payout/v1/payouts/draft -> {status_code} : {raiff_answer}'],
            bypass=use_sandbox
        ):
            result_call = make_call(
                'ProbePayment', {
                    'transaction_id': '12345',
                    'f_acc': account.number,
                    'f_bik': Raiffeisen.bid,
                    'currency': 'RUB',
                    'summ': 101,
                    't_acc': '79191234567',
                    'ground': 'test',
                    'params': (
                        '{"bank_alias": "RAIFFEISEN", "t_first_name": "Петр", "t_last_name": "Петров",'
                        '"t_middle_name": "Петрович", "t_inn": 123456789101, "income_type": "1"}'
                    )
                }, Service.TOLOKA, native=True
            )
            return result_call

    result = raiff_call(200, raiff_answer_sucess)
    assert '<name>status_bcl</name>\n<value><int>7</int></value>' in result
    assert '<name>status_remote</name>\n<value><string>PENDING</string></value>' in result

    result = raiff_call(500, 'ERROR')
    assert '<name>traceback</name>' in result

    raiff_answer_sucess = raiff_answer_sucess.replace('PENDING', 'DECLINED')
    result = raiff_call(200, raiff_answer_sucess)
    assert '<name>status_bcl</name>\n<value><int>2</int></value>' in result
    assert '<name>status_remote</name>\n<value><string>DECLINED</string></value>' in result


def test_get_banks_payments(
    get_source_payment, get_assoc_acc_curr, django_assert_num_queries, make_call
):
    _, account, _ = get_assoc_acc_curr(Raiffeisen, account='someacc')

    now = datetime.now()
    def create_payment_and_check_status(payment_status, expected_status):

        payment = get_source_payment(
            service=True,
            attrs={
                'f_acc': account.number,
                'service_id': Service.TOLOKA,
                'update_dt': now,
                'status': payment_status,
                'metadata': {'client_id': 1111},
                'number_src': str(uuid4()),
            }, associate=Raiffeisen)

        result = make_call(
            'GetPayments',
            [{'transaction_ids': [payment.number_src]}],
            Service.TOLOKA,
        )

        assert len(result) == 1
        assert result[0]['status'] == expected_status

    create_payment_and_check_status(states.EXPORTED_H2H, 'processing')
    create_payment_and_check_status(states.PROCESSING, 'processing')
    create_payment_and_check_status(states.DECLINED_BY_BANK, 'error')
    create_payment_and_check_status(states.COMPLETE, 'exported_to_h2h')


def test_send_creditor_to_check(response_mock, monkeypatch, make_call):
    authorization_answer = {'access_token': '1234', 'id_token': '5678'}

    # не json
    with response_mock([
        f'POST https://extest.openapi.raiffeisen.ru/token -> 200:{json.dumps(authorization_answer)}',
        'POST https://extest.openapi.raiffeisen.ru/payables-finance/creditors/send-to-check -> 400:bad',
    ]):
        result = make_call(
            'SendCreditorToCheck',
            {'bik': Raiffeisen.bid, 'name': 'test', 'inn': '12334', 'ogrn': '123'},
            Service.MBI_BILLING,
            native=True
        )
    assert 'UserHandledException: {"message": "bad", "data": ""}' in result

    result = make_call(
        'SendCreditorToCheck',
        {'bik': Raiffeisen.bid, 'inn': '12334', 'ogrn': '123'},
        Service.MBI_BILLING,
        native=True
    )
    assert 'XMLRPCInvalidParam' in result

    # успешный случай
    with response_mock(
        [
            f'POST https://extest.openapi.raiffeisen.ru/token -> 200 : {json.dumps(authorization_answer)}',
            'POST https://extest.openapi.raiffeisen.ru/payables-finance/creditors/send-to-check -> 200 : '
            '{"id": "26415d4a-de6b-11eb-ba80-0242ac130004", "fullNameRus": "ОАО Поставляю все", "inn": 7704357909, '
            '"ogrn": 116774649139, "status": {"status": "FETCHED", "statusDate": "2021-12-31T12:12:12.111111"}}'
        ]
    ):
        result = make_call(
            'SendCreditorToCheck',
            {'bik': Raiffeisen.bid, 'name': 'test', 'inn': '12334', 'ogrn': '123'},
            Service.MBI_BILLING,
            native=True
        )
    assert '2021-12-31T12:12:12.111111' in result

    # успешный случай с КПП
    with response_mock(
        [
            f'POST https://extest.openapi.raiffeisen.ru/token -> 200 : {json.dumps(authorization_answer)}',
            'POST https://extest.openapi.raiffeisen.ru/payables-finance/creditors/send-to-check -> 200 : '
            '{"id": "1", "fullNameRus": "ОАО Поставляю все", "inn": "12334", "kpp": "12345", '
            '"ogrn": 123, "status": {"status": "FETCHED", "statusDate": "2021-12-31T12:12:12.111111"}}'
        ]
    ):
        result = make_call(
            'SendCreditorToCheck',
            {'bik': Raiffeisen.bid, 'name': 'test', 'inn': '12334', 'ogrn': '123', 'kpp': '12345'},
            Service.MBI_BILLING,
            native=True
        )
    assert 'string>12345<' in result

    monkeypatch.setattr(RaiffFactoringConnector, '_auth', lambda *args, **kwargs: authorization_answer.values())

    with response_mock(
        'POST https://extest.openapi.raiffeisen.ru/payables-finance/creditors/send-to-check -> 400 : '
        '{"internalCode": 1, "description": "Atribute is mandatory"}'
    ):
        result = make_call(
            'SendCreditorToCheck',
            {'bik': Raiffeisen.bid, 'name': 'test', 'inn': '12334', 'ogrn': '123'},
            Service.MBI_BILLING,
            native=True
        )
    assert 'Atribute is mandatory' in result


def test_get_creditors(response_mock, monkeypatch, make_call):
    monkeypatch.setattr(RaiffFactoringConnector, '_auth', lambda *args, **kwargs: ('1234', '5678'))

    result = make_call(
        'GetCreditors',
        {'bik': Raiffeisen.bid, 'inn': '12334', 'ogrn': '123'},
        Service.MBI_BILLING,
        native=True
    )
    assert 'ArgumentError' in result

    def get_crediitors(params, url):
        params.update({'bik': Raiffeisen.bid})

        with response_mock(
            [
                f'GET https://extest.openapi.raiffeisen.ru/payables-finance/creditors/{url} -> 200 : '
                '[{"id": "26415d4a-de6b-11eb-ba80-0242ac130004", "fullName": "ОАО Поставляю все", "inn": 7704357909, '
                '"ogrn": 116774649139, "status": {"status": "FETCHED", "statusDate": "2021-12-31T12:12:12.111111"}}]'
            ]
        ):
            return make_call(
                'GetCreditors',
                params,
                Service.MBI_BILLING,
                native=True
            )
    result = get_crediitors(
        {'statuses': ['FETCHED', 'TEST'], 'page': 1}, url='by-statuses?statuses=FETCHED&statuses=TEST&size=500&page=1'
    )
    assert '2021-12-31T12:12:12.111111' in result

    result = get_crediitors(
        {'name': 'test', 'inn': '12334', 'ogrn': '123'}, url='by-requisites?fullNameRus=test&inn=12334&ogrn=123'
    )
    assert '2021-12-31T12:12:12.111111' in result

    with response_mock(
        [
            'GET https://extest.openapi.raiffeisen.ru/payables-finance/creditors/by-statuses?statuses=FETCHED&size=500&page=1 -> '
            '400 : {"internalCode": 1, "description": "Atribute is mandatory"}'
        ]
    ):
        result = make_call(
            'GetCreditors',
            {'statuses': ['FETCHED'], 'page': 1, 'bik': Raiffeisen.bid},
            Service.MBI_BILLING,
            native=True
        )
    assert 'Atribute is mandatory' in result
