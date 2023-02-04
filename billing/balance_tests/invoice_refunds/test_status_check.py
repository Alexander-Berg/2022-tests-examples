# -*- coding: utf-8 -*-

import uuid
import json
import random

import pytest
import mock
import hamcrest

from balance import exc
from balance.constants import *

from cluster_tools.invoice_refunds_status_check import process_batch

from tests import object_builder as ob
from tests.balance_tests.invoice_refunds.common import (
    create_order,
    create_invoice,
    create_ocpf,
)

pytestmark = [
    pytest.mark.invoice_refunds,
]


def create_oebs_refund(session, amount=100):
    client = ob.ClientBuilder().build(session).obj
    order = create_order(client)
    invoice = create_invoice(client, order, 1001, amount)
    cpf = create_ocpf(invoice, amount)

    refund = ob.InvoiceRefundBuilder(
        invoice=cpf.invoice,
        payment_id=cpf.id,
        amount=amount,
    ).build(session).obj
    refund.set_status(InvoiceRefundStatus.exported)
    refund.system_uid = str(uuid.uuid4().int)
    session.flush()
    return refund


def mock_post(answer, status_code=200):
    return_value = mock.MagicMock(
        status_code=status_code,
        text=json.dumps(answer),
        json=lambda: answer
    )
    return mock.patch('requests.post', return_value=return_value)


@pytest.fixture(autouse=True)
def oebs_config():
    def get_component_cfg_mocked(cfg, component_id, should_expand=True, expand_data=None, resolvers=None):
        if component_id == 'oebs_api':
            return {
                'Url': 'https://oebsapi-billing.testing.oebs.yandex-team.ru/oebsapi/rest/',
                'CloudUrl': 'https://oebsapi-test.mba.yandex-team.ru/rest/',
                'Timeout': 60,
                'Token': 'aaaaaaaaa',
            }
        return get_component_cfg(cfg, component_id, should_expand, expand_data, resolvers)

    with mock.patch('butils.application.plugins.components_cfg.get_component_cfg', get_component_cfg_mocked):
        yield


@pytest.mark.parametrize(
    'answer, req_status_code, req_status_descr, req_payload',
    [
        pytest.param(
            {'status': 'NEW'},
            InvoiceRefundStatus.exported,
            None,
            {},
            id='new'
        ),
        pytest.param(
            {'status': 'TRANSMITTED', 'payment_num': '666', 'payment_date': '06.06.6666'},
            InvoiceRefundStatus.oebs_transmitted,
            None,
            {'payment_num': '666', 'payment_date': '06.06.6666'},
            id='transmitted'
        ),
        pytest.param(
            {'status': 'RECONCILED', 'payment_num': '777', 'payment_date': '07.07.7777'},
            InvoiceRefundStatus.oebs_reconciled,
            None,
            {'payment_num': '777', 'payment_date': '07.07.7777'},
            id='reconciled'
        ),
        pytest.param(
            {'status': 'VOID', 'void_reason': 'Рожей не вышел'},
            InvoiceRefundStatus.failed,
            u'Рожей не вышел',
            {},
            id='fail'
        ),
    ]
)
def test_statuses(session, answer, req_status_code, req_status_descr, req_payload, service_ticket_mock):
    refund = create_oebs_refund(session)

    answer.update(
        request_id=int(refund.system_uid),
        entity_id=uuid.uuid4().int
    )
    req_payload['entity_id'] = answer['entity_id']

    with mock_post({'result': 'SUCCESS', 'requests': [answer]}) as mock_obj:
        process_batch(session, [refund.id])
    session.flush()
    session.expire_all()

    hamcrest.assert_that(
        refund,
        hamcrest.has_properties(
            status_code=req_status_code,
            status_descr=req_status_descr,
            payload=req_payload
        )
    )
    hamcrest.assert_that(
        mock_obj,
        hamcrest.has_properties(
            call_count=1,
            call_args=hamcrest.contains(
                hamcrest.contains(hamcrest.contains_string('billingImport')),
                hamcrest.has_entry(
                    'data',
                    hamcrest.contains_string('"requests": [%s]' % refund.system_uid)
                )
            )
        )
    )


def test_batch(session, service_ticket_mock):
    refunds = [create_oebs_refund(session) for _ in range(4)]

    oebs_statuses = [
        'NEW',
        'TRANSMITTED',
        'RECONCILED',
        'VOID'
    ]
    ref_statuses = [
        InvoiceRefundStatus.exported,
        InvoiceRefundStatus.oebs_transmitted,
        InvoiceRefundStatus.oebs_reconciled,
        InvoiceRefundStatus.failed
    ]

    answers = [{'request_id': int(r.system_uid), 'status': s} for r, s in zip(refunds, oebs_statuses)]
    random.shuffle(answers)
    with mock_post({'result': 'SUCCESS', 'requests': answers}) as mock_obj:
        process_batch(session, [r.id for r in refunds])
    session.flush()
    session.expire_all()

    hamcrest.assert_that(
        refunds,
        hamcrest.contains(*[
            hamcrest.has_properties(status_code=status)
            for status in ref_statuses
        ])
    )
    hamcrest.assert_that(
        mock_obj,
        hamcrest.has_properties(
            call_count=1,
            call_args=hamcrest.contains(
                hamcrest.contains(hamcrest.contains_string('billingImport')),
                hamcrest.has_entry(
                    'data',
                    hamcrest.all_of(*[
                        hamcrest.contains_string(str(r.system_uid))
                        for r in refunds
                    ])
                )
            )
        )
    )


def test_unmatched_answer(session, service_ticket_mock):
    refund_ok = create_oebs_refund(session)
    refund_missed = create_oebs_refund(session)

    answers = [
        {'request_id': int(refund_ok.system_uid), 'status': 'TRANSMITTED'},
        {'request_id': uuid.uuid4().int, 'status': 'TRANSMITTED'},
    ]
    random.shuffle(answers)
    with mock_post({'result': 'SUCCESS', 'requests': answers}):
        process_batch(session, [refund_ok.id, refund_missed.id])
    session.flush()
    session.expire_all()

    hamcrest.assert_that(
        refund_ok,
        hamcrest.has_properties(status_code=InvoiceRefundStatus.oebs_transmitted)
    )
    hamcrest.assert_that(
        refund_missed,
        hamcrest.has_properties(status_code=InvoiceRefundStatus.exported)
    )


def test_delayed_cancel(session, service_ticket_mock):
    session.config.__dict__['INVOICE_REFUND_CANCEL_DELAY'] = 10
    refund = create_oebs_refund(session)
    refund.set_status(InvoiceRefundStatus.oebs_transmitted)
    refund.payload = {'entity_id': ob.get_big_number()}
    session.flush()
    session.execute('update bo.t_invoice_refund set update_dt = update_dt - 20 where id = :id', {'id': refund.id})
    session.expire(refund)

    answers = [
        {'request_id': int(refund.system_uid), 'status': 'TRANSMITTED', 'entity_id': refund.payload['entity_id']},
    ]
    with mock_post({'result': 'SUCCESS', 'requests': answers}):
        process_batch(session, [refund.id])
    session.flush()
    session.expire_all()

    hamcrest.assert_that(
        refund,
        hamcrest.has_properties(status_code=InvoiceRefundStatus.failed)
    )


def test_api_error(session, service_ticket_mock):
    refund = create_oebs_refund(session)

    with pytest.raises(exc.OEBS_API_CALL_ERROR) as exc_info:
        with mock_post({'status': 'ERROR', 'message': 'TNS не TNS'}):
            process_batch(session, [refund.id])

    assert exc_info.value.msg == u'Error while calling api: TNS не TNS'


def test_result_error(session, service_ticket_mock):
    refund = create_oebs_refund(session)

    with pytest.raises(exc.OEBS_API_CHECK_ERROR) as exc_info:
        with mock_post({'result': 'ERROR', 'errors': ['Не пихайте в меня эту гадость!']}):
            process_batch(session, [refund.id])

    assert exc_info.value.msg == u'Error for export check: Не пихайте в меня эту гадость!'
