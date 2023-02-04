# -*- coding: utf-8 -*-

import json
import xmlrpclib
from xmlrpclib import Fault

import datetime
import httpretty
import mock
from mock import call
import pytest
import time
import re

from lxml import etree

import balance.constants as cst
from balance import (
    mapper,
    exc,
    muzzle_util as ut,
)
from balance.application import getApplication
from balance.trust_api import balance_payments
from tests import tutils as tut
from tests.object_builder import (
    get_big_number,
    ClientBuilder,
    ProductBuilder,
    OrderBuilder,
    PersonBuilder,
    BasketBuilder,
    BasketItemBuilder,
    RequestBuilder,
    InvoiceBuilder,
    create_pay_policy_service,
    create_pay_policy_region,
    create_pay_policy_payment_method,
    TrustApiPaymentBuilder,
)

CURRENT_DT = datetime.datetime.now()
CURRENT_TS = '%d' % (time.mktime(CURRENT_DT.timetuple()) * 1000)

ERROR_TYPE_PATTERN = "Error: (\w+)\nDescription: (.*)"
TRUST_API_URL = 'https://trust-payments-dev.paysys.yandex.net:8028/trust-payments/v2/payment_methods'
SIMPLE_API_URL = 'balance-payments-dev.paysys.yandex.net:8023/simpleapi/xmlrpc'
PAYMENT_INFO_NEW = {
    'status': 'success',
    'cancel_ts': '',
    'payment_ts': '',
    'postauth_ts': '',
    'resp_code': None,
    'resp_desc': None,
    'transaction_id': '666-transaction-666',
    'ts': CURRENT_TS,
    'amount': '100',
    'currency': 'RUR',
    'developer_payload': 'payload'
}


@pytest.fixture
def client(session):
    return ClientBuilder().build(session).obj


@pytest.fixture
def person(session, client):
    return PersonBuilder(client=client, name='Name').build(session).obj


@pytest.fixture
def service_id(session):
    service_id = get_big_number()
    service_params = session.execute('select * from bo.t_service where id = 7').fetchone()
    new_params = dict(service_params)
    new_params['id'] = service_id
    new_params['cc'] = service_id

    service_insert_sql = 'insert into t_service (%s) values (%s)' % (
        ', '.join(k for k in new_params),
        ', '.join(':%s' % k for k in new_params)
    )
    session.execute(service_insert_sql, new_params)

    paysys_ccs = ['ur_trust_card_RUB', 'ph_trust_card_RUB']
    paysyses = session.query(mapper.Paysys) \
        .filter(
        mapper.Paysys.cc.in_(paysys_ccs),
        mapper.Paysys.firm_id == cst.FirmId.YANDEX_OOO
    )

    session.execute(
        'insert into t_paysys_service (service_id, paysys_id, weight, extern) values (:s_id, :ps_id, 666, 0)',
        [
            {'s_id': service_id, 'ps_id': ps.id}
            for ps in paysyses
        ]
    )
    ppp_id = create_pay_policy_service(session, service_id, cst.FirmId.YANDEX_OOO)
    create_pay_policy_payment_method(session, ppp_id, 'RUB', cst.PaymentMethodIDs.credit_card, paysyses[0].group_id)

    create_pay_policy_region(session, ppp_id, cst.RegionId.RUSSIA)

    return service_id


@pytest.fixture
def invoice(session, request_obj, person):
    paysys = session.query(mapper.Paysys).filter_by(cc='ph_trust_card_RUB', firm_id=1).one()
    return InvoiceBuilder(
        request=request_obj,
        person=person,
        paysys=paysys
    ).build(session).obj


@pytest.fixture
def request_obj(session, service_id, client):
    product = ProductBuilder(price=10, engine_id=service_id)
    order = OrderBuilder(client=client, product=product, service_id=service_id)

    return RequestBuilder(
        basket=BasketBuilder(
            rows=[BasketItemBuilder(quantity=10, order=order)]
        )
    ).build(session).obj


def get_billing_dev_payload(
    invoice,  # type: mapper.Invoice
):  # type: (...) -> str
    return json.dumps({
        'pass_params': {
            'terminal_route_data': {
                'firm_id': invoice.firm_id
            }
        }
    })


def get_real_request_id(real_session, test_name):
    with real_session.begin():
        query = '''
        select object_id from bo.t_test_committed_objects
        where test_name = :test_name and object_name = 'request'
        '''
        res = real_session.execute(query, {'test_name': test_name}).fetchone()
        if res:
            request_id = res.object_id
        else:
            request = RequestBuilder().build(real_session).obj
            real_session.add(request)
            query = '''
            insert into bo.t_test_committed_objects (test_name, object_name, object_id)
            VALUES (:test_name, 'request', :request_id)
            '''
            real_session.execute(query, {'test_name': test_name, 'request_id': request.id})
            request_id = request.id

    return request_id


def test_pay_request_locks(session, medium_xmlrpc):
    session.oracle_namespace_lock('test_pay_request_locks', lockmode='exclusive', timeout=10)

    real_session = getApplication().real_new_session()
    request_id = get_real_request_id(real_session, 'test_pay_request_locks')

    with real_session.begin():
        request = real_session.query(mapper.Request).with_lockmode('update_nowait').getone(request_id)
        with pytest.raises(Fault) as exc_info:
            medium_xmlrpc.PayRequest(
                session.oper_id,
                {
                    'request_id': request.id,
                    'payment_method_id': 'card-666',
                    'currency': 'RUB',
                    'person_id': 123456
                }
            )
        msg = 'Request {} is locked by another operation'.format(request.id)
        assert tut.get_exception_code(exc_info.value, 'msg') == msg


def check_required_ans(call_res):
    required_ans = {
        'cancel_dt': None,
        'payment_dt': None,
        'postauth_dt': None,
        'resp_code': None,
        'resp_desc': None,
        'transaction_id': '666-transaction-666',
        'dt': CURRENT_DT,
        'amount': '100',
        'currency': 'RUR',
        'developer_payload': 'payload',
    }
    assert required_ans == call_res


@pytest.fixture
def patch_get_payment(session, request_obj):
    def _payment_from_id(payment_id):
        invoice = sorted(request_obj.invoices, key=lambda i: i.id)[-1]
        return ut.Struct(transaction_id=payment_id, invoice=invoice, invoice_id=invoice.id)

    import balance.trust_api.balance_payments as api
    return mock.patch.object(api.BalancePaymentsApi, '_payment_from_id', side_effect=_payment_from_id)


def register_payment_methods(payment_method, firm_id, currency, id, status_code=200):
    httpretty.register_uri(
        httpretty.GET,
        'https://trust-payments-dev.paysys.yandex.net:8028/trust-payments/v2/payment_methods',
        json.dumps({"status": "success",
                    "enabled_payment_methods": [{"payment_method": payment_method,
                                                 "firm_id": firm_id,
                                                 "currency": currency,
                                                 "id": id
                                                 }],
                    }),
        status=status_code)


def test_wrong_paymethod(session, medium_xmlrpc, request_obj, person):
    register_payment_methods("bank", 1, "RUB", 'card-6666')

    with pytest.raises(xmlrpclib.Fault) as exc_info:
        medium_xmlrpc.PayRequest(
            session.oper_id,
            {
                'request_id': request_obj.id,
                'payment_method_id': 'card-6666',
                'currency': 'RUB',
                'person_id': person.id
            }
        )

    assert 'No payment options available' in exc_info.value.faultString
    assert [] == request_obj.invoices
    assert 1 == request_obj.seq


def test_anon(
    session, service_id, medium_xmlrpc, request_obj,
    person, patch_get_payment,
):
    old_oper_id = session.oper_id
    session.oper_id = 0

    import balance.trust_api.balance_payments as api
    patch_api = mock.patch.object(api.BalancePaymentsApi, 'do_call',
                                  side_effect=[{'transaction_id': '666-transaction-666'},
                                               PAYMENT_INFO_NEW])

    with patch_api as a, patch_get_payment:
        call_res = medium_xmlrpc.PayRequest(
            0,
            {
                'request_id': request_obj.id,
                'payment_method_id': 'trust_web_page',
                'currency': 'RUB',
                'person_id': person.id
            }
        )
    session.oper_id = old_oper_id

    assert 'invoice_id' in call_res
    assert 'request_id' in call_res

    invoice_id = call_res.pop('invoice_id')
    request_id = call_res.pop('request_id')
    assert str(request_obj.id) == str(request_id)
    assert [i.id for i in request_obj.invoices] == [invoice_id]

    check_required_ans(call_res)

    a.assert_has_calls([
        call(
            'CreatePaymentForInvoice',
            invoice_id=invoice_id,
            paymethod_id=u'trust_web_page',
            developer_payload=get_billing_dev_payload(request_obj.invoices[0]),
        ),
        call('StartTrustAPIPayment', transaction_id=u'666-transaction-666')
    ],)


@pytest.mark.parametrize(
    'additional_params',
    [
        {},
        {'payment_mode': 'reccurent', 'lang': 'ru'}
    ],
)
def test_ok_w_person(
    session, medium_xmlrpc, request_obj, person,
    additional_params, patch_get_payment,
):
    register_payment_methods("card", 1, "RUB", 'card-666')

    import balance.trust_api.balance_payments as api
    patch_api = mock.patch.object(api.BalancePaymentsApi, 'do_call',
                                  side_effect=[{'transaction_id': '666-transaction-666'},
                                               PAYMENT_INFO_NEW])
    with patch_api as calls, patch_get_payment:
        call_res = medium_xmlrpc.PayRequest(
            session.oper_id,
            {
                'request_id': request_obj.id,
                'payment_method_id': 'card-666',
                'currency': 'RUB',
                'person_id': person.id,
                'payment_mode': additional_params.get('payment_mode'),
                'lang': additional_params.get('lang')
            }
        )

    assert 'invoice_id' in call_res
    assert 'request_id' in call_res

    invoice_id = call_res.pop('invoice_id')
    request_id = call_res.pop('request_id')
    assert str(request_obj.id) == str(request_id)
    assert [i.id for i in request_obj.invoices] == [invoice_id]
    assert request_obj.payment_uid == '666-transaction-666'

    check_required_ans(call_res)
    if additional_params:
        calls.assert_has_calls([
            call(
                'CreatePaymentForInvoice',
                invoice_id=invoice_id,
                passport_id=session.oper_id,
                paymethod_id=u'card-666',
                payment_mode='reccurent',
                lang='ru',
                developer_payload=get_billing_dev_payload(request_obj.invoices[0]),
            ),
            call('StartTrustAPIPayment', transaction_id=u'666-transaction-666'),
        ])
    else:
        calls.assert_has_calls([
            call(
                'CreatePaymentForInvoice',
                invoice_id=invoice_id,
                passport_id=session.oper_id,
                paymethod_id=u'card-666',
                developer_payload=get_billing_dev_payload(request_obj.invoices[0]),
            ),
            call('StartTrustAPIPayment', transaction_id=u'666-transaction-666'),
        ])


def test_ok_double_call_payment_uid(
    session, medium_xmlrpc, request_obj,
    person, patch_get_payment,
):
    register_payment_methods("card", 1, "RUB", 'card-666')

    import balance.trust_api.balance_payments as api
    patch_api = mock.patch.object(api.BalancePaymentsApi, 'do_call',
                                  side_effect=[{'transaction_id': '666-transaction-666'},
                                               PAYMENT_INFO_NEW])
    with patch_api as calls, patch_get_payment:
        first_call_res = medium_xmlrpc.PayRequest(
            session.oper_id,
            {
                'request_id': request_obj.id,
                'payment_method_id': 'card-666',
                'currency': 'RUB',
                'person_id': person.id
            }
        )

    first_invoice_id = first_call_res['invoice_id']
    first_invoice = session.query(mapper.Invoice).get(first_invoice_id)

    calls.assert_has_calls([
        call(
            'CreatePaymentForInvoice',
            invoice_id=first_invoice_id,
            passport_id=session.oper_id,
            paymethod_id=u'card-666',
            developer_payload=get_billing_dev_payload(first_invoice),
        ),
        call(
            'StartTrustAPIPayment',
            transaction_id=u'666-transaction-666'
        ),
    ])

    assert request_obj.payment_uid == '666-transaction-666'

    patch_api_second_time = mock.patch.object(api.BalancePaymentsApi, 'do_call',
                                              side_effect=[PAYMENT_INFO_NEW])

    with patch_api_second_time as calls, patch_get_payment:
        call_res = medium_xmlrpc.PayRequest(
            session.oper_id,
            {
                'request_id': request_obj.id,
                'payment_method_id': 'card-666',
                'currency': 'RUB',
                'person_id': person.id
            }
        )

    assert 'invoice_id' in call_res
    assert 'request_id' in call_res

    invoice_id = call_res.pop('invoice_id')
    assert invoice_id == first_invoice_id
    request_id = call_res.pop('request_id')
    assert str(request_obj.id) == str(request_id)
    assert [i.id for i in request_obj.invoices] == [invoice_id]
    assert request_obj.payment_uid == '666-transaction-666'

    check_required_ans(call_res)

    calls.assert_has_calls([call('StartTrustAPIPayment', transaction_id=u'666-transaction-666')], )


def test_ok_double_call_no_payment_uid(
    session, medium_xmlrpc, request_obj,
    invoice, person, patch_get_payment,
):
    register_payment_methods("card", 1, "RUB", 'card-666')

    import balance.trust_api.balance_payments as api
    patch_api = mock.patch.object(api.BalancePaymentsApi, 'do_call',
                                  side_effect=[{'transaction_id': '666-transaction-666'},
                                               PAYMENT_INFO_NEW])

    TrustApiPaymentBuilder(transaction_id='666-transaction-666', invoice=invoice).build(session)
    assert request_obj.payment_uid is None

    with patch_api as calls, patch_get_payment:
        call_res = medium_xmlrpc.PayRequest(
            session.oper_id,
            {
                'request_id': request_obj.id,
                'payment_method_id': 'card-666',
                'currency': 'RUB',
                'person_id': person.id
            }
        )
    session.expire_all()

    assert 'invoice_id' in call_res
    assert 'request_id' in call_res

    created_invoice_id = call_res.pop('invoice_id')
    request_id = call_res.pop('request_id')
    assert str(request_obj.id) == str(request_id)
    assert sorted(i.id for i in request_obj.invoices) == sorted([invoice.id, created_invoice_id])
    assert request_obj.payment_uid == '666-transaction-666'

    check_required_ans(call_res)

    calls.assert_has_calls(
        [
            call(
                'CreatePaymentForInvoice',
                invoice_id=created_invoice_id,
                passport_id=session.oper_id,
                paymethod_id=u'card-666',
                developer_payload=get_billing_dev_payload(request_obj.invoices[0]),
            ),
            call('StartTrustAPIPayment', transaction_id=u'666-transaction-666'),
        ]
    )


def test_ok_double_call_with_invoice(
    session, medium_xmlrpc, request_obj,
    person, invoice, patch_get_payment,
):
    register_payment_methods("card", 1, "RUB", 'card-666')

    import balance.trust_api.balance_payments as api
    patch_api = mock.patch.object(api.BalancePaymentsApi, 'do_call',
                                  side_effect=[{'transaction_id': '666-transaction-666'},
                                               PAYMENT_INFO_NEW])
    with patch_api as calls, patch_get_payment:
        call_res = medium_xmlrpc.PayRequest(
            session.oper_id,
            {
                'request_id': request_obj.id,
                'payment_method_id': 'card-666',
                'currency': 'RUB',
                'person_id': person.id
            }
        )

    assert 'invoice_id' in call_res
    assert 'request_id' in call_res

    res_invoice_id = call_res.pop('invoice_id')
    assert res_invoice_id != invoice.id
    request_id = call_res.pop('request_id')
    assert str(request_obj.id) == str(request_id)
    assert sorted(i.id for i in request_obj.invoices) == sorted([invoice.id, res_invoice_id])
    assert request_obj.payment_uid == '666-transaction-666'

    check_required_ans(call_res)

    calls.assert_has_calls(
        [
            call(
                'CreatePaymentForInvoice',
                invoice_id=res_invoice_id,
                passport_id=session.oper_id,
                paymethod_id=u'card-666',
                developer_payload=get_billing_dev_payload(request_obj.invoices[0]),
            ),
            call('StartTrustAPIPayment', transaction_id=u'666-transaction-666')],
    )


@pytest.mark.parametrize('status_code', [404, 502])
def test_error_502_payment_methods(session, medium_xmlrpc, request_obj, person, patch_get_payment,
                                   status_code):
    register_payment_methods("card", 1, "RUB", 'card-666', status_code=status_code)

    import balance.trust_api.balance_payments as api
    patch_api = mock.patch.object(api.BalancePaymentsApi, 'do_call',
                                  side_effect=[{'transaction_id': '666-transaction-666'},
                                               PAYMENT_INFO_NEW])
    with patch_api as calls, patch_get_payment, pytest.raises(Fault) as exc_info:
        response = medium_xmlrpc.PayRequest(
            session.oper_id,
            {
                'request_id': request_obj.id,
                'payment_method_id': 'card-666',
                'currency': 'RUB',
                'person_id': person.id
            }
        )

    if status_code == 404:
        match = re.match(ERROR_TYPE_PATTERN, exc_info.value.faultString)
        assert match.group(2) == '404 Client Error: Not Found for url: {}?show_enabled=1&show_bound=1'.format(
            TRUST_API_URL)
    else:
        error_text = 'Error in connecting to trust api: 502 Server Error: Bad Gateway for url: {}' \
                     '?show_enabled=1&show_bound=1'.format(TRUST_API_URL)
        assert tut.get_exception_code(exc_info.value, 'code') == 'TRUST_API_CONNECT_EXCEPTION'
        assert tut.get_exception_code(exc_info.value, 'msg') == error_text


@pytest.mark.parametrize('status_code', [404, 502])
def test_error_502_simpleapi(session, medium_xmlrpc, request_obj, person, status_code):
    register_payment_methods("card", 1, "RUB", 'card-666', status_code=200)

    def get_response():
        response = etree.Element('root')
        return response

    httpretty.register_uri(
        httpretty.POST,
        'https://balance-payments-dev.paysys.yandex.net:8023/simpleapi/xmlrpc',
        get_response(),
        status=status_code)
    with pytest.raises(Fault) as exc_info:
        response = medium_xmlrpc.PayRequest(
            session.oper_id,
            {
                'request_id': request_obj.id,
                'payment_method_id': 'card-666',
                'currency': 'RUB',
                'person_id': person.id
            }
        )
    if status_code == 404:
        match = re.match(ERROR_TYPE_PATTERN, exc_info.value.faultString)
        assert match.group(2) == '<ProtocolError for {}: 404 Not Found>'.format(SIMPLE_API_URL)
    else:
        error_text = 'Error in connecting to trust api: <ProtocolError for {}: 502 Bad Gateway>'.format(
            SIMPLE_API_URL)
        assert tut.get_exception_code(exc_info.value, 'code') == 'TRUST_API_CONNECT_EXCEPTION'
        assert tut.get_exception_code(exc_info.value, 'msg') == error_text


def check_same_dict(
    original_dict,  # type: dict[K, Any]
    updated_dict,  # type: dict[K, Any]
    skip_keys=(),  # type: Iterable[K]
):
    for k, v in original_dict.items():
        if k not in skip_keys:
            assert updated_dict[k] == v


@pytest.mark.parametrize(
    'dev_payload',
    [
        {},
        {'foo': 'bar'},
        {'pass_params': {}},
        {'pass_params': {
            'terminal_route_data': {}
        }},
        {'pass_params': {
            'terminal_route_data': {'try': 'this'}
        }},
    ]
)
def test_billing_payload_populate(invoice, patch_get_payment, dev_payload):
    processed_payload = balance_payments.populate_terminal_route_data(invoice, developer_payload=dev_payload)

    assert processed_payload['pass_params']['terminal_route_data']['firm_id'] == invoice.firm.id

    check_same_dict(dev_payload, processed_payload, skip_keys=['pass_params'])
    if 'pass_params' not in dev_payload:
        return

    check_same_dict(
        dev_payload['pass_params'],
        processed_payload['pass_params'],
        skip_keys=['terminal_route_data'],
    )

    if 'terminal_route_data' not in dev_payload['pass_params']:
        return

    check_same_dict(
        dev_payload['pass_params']['terminal_route_data'],
        processed_payload['pass_params']['terminal_route_data'],
    )


@pytest.mark.parametrize(
    'dev_payload',
    [
        {'pass_params': []},
        {'pass_params': {
            'terminal_route_data': []
        }},
        {'pass_params': {
            'terminal_route_data': {'firm_id': 3}
        }},
    ]
)
def test_billing_payload_populate_failure(invoice, patch_get_payment, dev_payload):
    with pytest.raises(exc.BALANCE_PAYMENTS_VALIDATION_EXCEPTION):
        balance_payments.populate_terminal_route_data(invoice, developer_payload=dev_payload)


@pytest.mark.parametrize(
    ['input_payload', 'expected_parsed_payload'],
    [
        ('666', 666),
        ('{"foo": "bar"}', {u'foo': u'bar'}),
        (u'{"foo": "абв"}', {u'foo': u'абв'}),
        ('{}', {}),
        (None, {})
    ],
)
def test_parse_developer_payload(input_payload, expected_parsed_payload):
    assert balance_payments.parse_developer_payload(input_payload) == expected_parsed_payload


@pytest.mark.parametrize(
    'input_payload',
    [
        "",
        "{abc}",
        "abc:xyz",
    ]
)
def test_parse_developer_payload_fails(input_payload):
    with pytest.raises(exc.BALANCE_PAYMENTS_VALIDATION_EXCEPTION) as e:
        balance_payments.parse_developer_payload(input_payload)
