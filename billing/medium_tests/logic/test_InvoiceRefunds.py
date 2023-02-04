# -*- coding: utf-8 -*-

from xmlrpclib import Fault
from decimal import Decimal

import pytest
import hamcrest as hm

from balance import mapper
from balance.constants import OebsOperationType, InvoiceRefundStatus

import tests.object_builder as ob
from tests.balance_tests.invoices.access.access_common import create_passport


def create_cash_payment_fact(invoice, amount):
    cpf = ob.OebsCashPaymentFactBuilder(
        amount=amount,
        invoice=invoice,
        operation_type=OebsOperationType.ONLINE,
    ).build(invoice.session).obj
    invoice.session.expire_all()  # триггер
    return cpf


def create_cpf(invoice, amount, sf_avans=False):
    cpf = create_cash_payment_fact(invoice, amount)
    cpf.source_id = ob.get_big_number()
    cpf.cash_receipt_number = ob.get_big_number()
    cpf.inn = ob.get_big_number()
    cpf.customer_name = ob.get_big_number()
    cpf.bik = ob.get_big_number()
    cpf.account_name = ob.get_big_number()

    if sf_avans:
        cpf.operation_type = OebsOperationType.SF_AVANS

    invoice.session.flush()
    invoice.session.expire_all()
    return cpf


@pytest.mark.parametrize('whois', ['admin', 'user'])
@pytest.mark.parametrize('params', [
    {'CPFSum': '1',   'CPFCount': 1, 'ReceiptSum': '1',   'ReceiptCount': 1, 'Amount': '0.01', 'RefundableAmount': None,  'WithPayload': False, 'GetBy': 'InvoicePaymentID', 'Exception': True},
    {'CPFSum': '1',   'CPFCount': 1, 'ReceiptSum': '1',   'ReceiptCount': 1, 'Amount': '0.01', 'RefundableAmount': '1',   'WithPayload': False, 'GetBy': 'InvoicePaymentID'},
    {'CPFSum': '1',   'CPFCount': 3, 'ReceiptSum': '1',   'ReceiptCount': 2, 'Amount': '0.01', 'RefundableAmount': '1',   'WithPayload': False, 'GetBy': 'InvoicePaymentID'},
    {'CPFSum': '1',   'CPFCount': 1, 'ReceiptSum': '1',   'ReceiptCount': 1, 'Amount': '0.01', 'RefundableAmount': '1',   'WithPayload': True,  'GetBy': 'InvoicePaymentID'},
    {'CPFSum': '2.5', 'CPFCount': 1, 'ReceiptSum': '10',  'ReceiptCount': 1, 'Amount': '2.5',  'RefundableAmount': '2.5', 'WithPayload': False, 'GetBy': 'InvoiceID'},
    {'CPFSum': '2.5', 'CPFCount': 2, 'ReceiptSum': '10',  'ReceiptCount': 1, 'Amount': '2.5',  'RefundableAmount': '2.5', 'WithPayload': False, 'GetBy': 'InvoiceID', 'Exception': True},
    {'CPFSum': '2.5', 'CPFCount': 1, 'ReceiptSum': '10',  'ReceiptCount': 1, 'Amount': '2.5',  'RefundableAmount': '2.5', 'WithPayload': True,  'GetBy': 'InvoiceID'},
    {'CPFSum': '120', 'CPFCount': 1, 'ReceiptSum': '100', 'ReceiptCount': 1, 'Amount': '50',   'RefundableAmount': '100', 'WithPayload': False, 'GetBy': 'RequestID'},
    {'CPFSum': '120', 'CPFCount': 2, 'ReceiptSum': '100', 'ReceiptCount': 1, 'Amount': '50',   'RefundableAmount': '100', 'WithPayload': False, 'GetBy': 'RequestID', 'Exception': True},
    {'CPFSum': '120', 'CPFCount': 1, 'ReceiptSum': '100', 'ReceiptCount': 1, 'Amount': '50',   'RefundableAmount': '100', 'WithPayload': True,  'GetBy': 'RequestID'},
])
def test_CreateInvoiceRefund(session, medium_xmlrpc, params, whois):
    client = ob.ClientBuilder.construct(session)
    if whois == 'user':
        create_passport(session, [], client=client, patch_session=True)

    invoice = ob.InvoiceBuilder(client=client).build(session).obj

    for _ in range(params['ReceiptCount']):
        invoice.create_receipt(params['ReceiptSum'])

    cpf = create_cpf(invoice, params['CPFSum'])
    non_refundable_cpf = create_cpf(invoice, params['CPFSum'], sf_avans=True)
    for _ in range(1, params['CPFCount']):
        create_cpf(invoice, params['CPFSum'])

    if params['GetBy'] == 'InvoicePaymentID':
        get_by_id = cpf.id
    elif params['GetBy'] == 'InvoiceID':
        get_by_id = invoice.id
    elif params['GetBy'] == 'RequestID':
        get_by_id = invoice.request.id

    f_params = {
        'Amount':            params['Amount'],
        'RefundableAmount': params['RefundableAmount'],
        params['GetBy']:    get_by_id
    }
    if params.get("WithPayload"):
        f_params['Payload'] = {
            'transaction_num': ob.get_big_number(),
            'wallet_num':      str(ob.get_big_number())
        }

    if not params.get('Exception'):
        res = medium_xmlrpc.CreateInvoiceRefund(session.oper_id, f_params)
        refund = (
            session.query(mapper.OEBSInvoiceRefund)
            .filter(mapper.OEBSInvoiceRefund.id == res['RefundID'])
            .one()
        )

        assert Decimal(res['RefundableAmount']) == Decimal(params['RefundableAmount']) - Decimal(params['Amount'])
        assert refund.invoice_id == invoice.id
        if params.get("WithPayload"):
            assert f_params["Payload"] == refund.payload
        else:
            assert refund.payload is None
    else:
        with pytest.raises(Fault):
            medium_xmlrpc.CreateInvoiceRefund(
                session.oper_id,
                {
                    'Amount': params['Amount'],
                    'RefundableAmount': params['RefundableAmount'],
                    params['GetBy']: get_by_id
                }
            )


@pytest.mark.parametrize('whois', ['admin', 'user'])
@pytest.mark.parametrize('params', [
    {'GetBy': 'InvoicePaymentID', "RefundsCount": 3},
    {'GetBy': 'RefundID', "RefundsCount": 1},
])
def test_GetInvoiceRefundStatus(session, medium_xmlrpc, params, whois):
    client = ob.ClientBuilder.construct(session)
    if whois == 'user':
        create_passport(session, [], client=client, patch_session=True)

    invoice = ob.InvoiceBuilder(client=client).build(session).obj
    invoice.create_receipt(1 * params["RefundsCount"])
    cpf = create_cpf(invoice, 1 * params["RefundsCount"])
    non_refundable_cpf = create_cpf(invoice, 1 * params['RefundsCount'], sf_avans=True)
    refunds = []

    expected_res = []

    for i in range(params["RefundsCount"]):
        refunds.append(
            medium_xmlrpc.CreateInvoiceRefund(
                session.oper_id,
                {
                    'Amount': 1,
                    'RefundableAmount': 1 * (params["RefundsCount"] - i),
                    'InvoicePaymentID': cpf.id
                }
            )
        )
        session.flush()
        refund = (
            session.query(mapper.OEBSInvoiceRefund)
                .filter(mapper.OEBSInvoiceRefund.id == refunds[-1]['RefundID'])
                .one()
        )
        expected_res.append(
            {
                'RefundID': str(refund.id),
                'Amount': str(refund.amount),
                'StatusCode': refund.status_code,
                'StatusDescr': refund.status_descr,
                'IsFailed': refund.is_failed,
                'IsFinished': refund.is_finished,
                'IsLocked': refund.is_locked,
                'UnlockAllowed': refund.unlock_allowed
            }
        )

    res = medium_xmlrpc.GetInvoiceRefundStatus(
        session.oper_id,
        {
            params['GetBy']: refunds[0]['RefundID'] if params['GetBy'] == 'RefundID' else cpf.id
        }
    )

    hm.assert_that(res, hm.has_length(params["RefundsCount"]))
    for row in res:
        hm.assert_that(
            row,
            hm.any_of(
                *[
                    hm.has_entries(elem)
                    for elem in expected_res
                ]
            )
        )


@pytest.mark.parametrize('whois', ['admin', 'user'])
@pytest.mark.parametrize('params', [
    {'StatusCode': InvoiceRefundStatus.export_failed,    'NewStatusCode': InvoiceRefundStatus.failed_unlocked},
    {'StatusCode': InvoiceRefundStatus.failed,           'NewStatusCode': InvoiceRefundStatus.failed_unlocked},
    {'StatusCode': InvoiceRefundStatus.not_exported,     'Exception': True},
    {'StatusCode': InvoiceRefundStatus.exported,         'Exception': True},
    {'StatusCode': InvoiceRefundStatus.oebs_reconciled,  'Exception': True},
    {'StatusCode': InvoiceRefundStatus.oebs_transmitted, 'Exception': True},
    {'StatusCode': InvoiceRefundStatus.successful,       'Exception': True},
    {'StatusCode': InvoiceRefundStatus.failed_unlocked,  'Exception': True},
])
def test_UnlockInvoiceRefund(session, medium_xmlrpc, params, whois):
    client = ob.ClientBuilder.construct(session)
    if whois == 'user':
        create_passport(session, [], client=client, patch_session=True)

    invoice = ob.InvoiceBuilder(client=client).build(session).obj
    invoice.create_receipt(1)
    cpf = create_cpf(invoice, 1)
    refund = medium_xmlrpc.CreateInvoiceRefund(
        session.oper_id,
        {
            'Amount': 1,
            'RefundableAmount': 1,
            'InvoicePaymentID': cpf.id
        }
    )
    session.query(mapper.InvoiceRefund).getone(refund['RefundID']).set_status(params["StatusCode"])

    if not params.get("Exception"):
        result = medium_xmlrpc.UnlockInvoiceRefund(
            session.oper_id,
            refund['RefundID']
        )
        assert result['StatusCode'] == params['NewStatusCode']
    else:
        with pytest.raises(Fault):
            medium_xmlrpc.UnlockInvoiceRefund(
                session.oper_id,
                refund['RefundID']
            )


@pytest.mark.parametrize('whois', ['admin', 'user'])
@pytest.mark.parametrize('params', [
    {'count': 0},
    {'count': 1},
    {'count': 1, 'turn_on_rows': True},
    {'count': 2, 'withPayload': True},
    {'count': 3},
])
def test_GetInvoicePayments(session, medium_xmlrpc, params, whois):
    client = ob.ClientBuilder.construct(session)
    if whois == 'user':
        create_passport(session, [], client=client, patch_session=True)

    invoice = ob.InvoiceBuilder(client=client).build(session).obj
    invoice.create_receipt(1 * (params['count'] + 1))
    if params.get('turn_on_rows', False):
        invoice.turn_on_rows()
    cpfs = []
    refunds = []

    for i in range(params['count']):
        cpfs.append(create_cpf(invoice, 1))
        create_cpf(invoice, 1, sf_avans=True)
        f_params = {
            'Amount': 0.5,
            'RefundableAmount': 1,
            'InvoicePaymentID': cpfs[i].id,
        }
        if params.get('withPayload'):
            f_params['Payload'] = {'transaction_num': 123, 'wallet_num': '123'}
        refunds.append(medium_xmlrpc.CreateInvoiceRefund(session.oper_id, f_params) if i % 2 == 1 else None)

    expected_res = [
        {
            "PaymentNumber": cpfs[i].payment_number,
            "InvoiceEID": cpfs[i].invoice.external_id,
            "InvoiceID": invoice.id,
            "SourceType": cpfs[i].source_type,
            "InvoicePaymentID": cpfs[i].xxar_cash_fact_id,
            "INN": cpfs[i].inn,
            "CustomerName": cpfs[i].customer_name,
            "BIK": cpfs[i].bik,
            "AccountName": cpfs[i].account_name,
            "RefundableAmount": '1' if refunds[i] is None else '0.5'
        } for i in range(params['count'])
    ]

    res = medium_xmlrpc.GetInvoicePayments(session.oper_id, {'InvoiceID': invoice.id})

    hm.assert_that(res, hm.has_length(params['count']))
    for row in res:
        hm.assert_that(
            row,
            hm.any_of(
                *[
                    hm.has_entries(elem)
                    for elem in expected_res
                ]
            )
        )
        if params.get('withPayload') is not None:
            assert row['EditableRefundRequisites'] == []
