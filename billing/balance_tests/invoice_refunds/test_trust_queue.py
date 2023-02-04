# -*- coding: utf-8 -*-

import uuid
import json

import pytest
import hamcrest
import httpretty
import mock

from balance import mapper
from balance.queue_processor import QueueProcessor
from balance.constants import (
    InvoiceRefundStatus,
    ServiceId,
)

from butils.application.plugins.components_cfg import get_component_cfg

from tests import object_builder as ob
from tests.base import httpretty_ensure_enabled
from tests.tutils import mock_transactions
from tests.balance_tests.invoice_refunds.common import (
    create_order,
    create_invoice,
    create_trust_payment,
    create_payment_ocpf,
)

pytestmark = [
    pytest.mark.invoice_refunds,
]


# TODO: remove with httpretty>=0.9.4
@pytest.fixture(autouse=True)
def mock_url_http(app):
    real_cfg = get_component_cfg

    def mock_cfg(cfg, component_id, *args, **kwargs):
        mocked_components = {
            'yb_trust_payments_ng',
        }

        res = real_cfg(cfg, component_id, *args, **kwargs)
        if component_id in mocked_components:
            return {k: v.replace('https://', 'http://') for k, v in res.iteritems()}
        else:
            return res

    with mock.patch('butils.application.plugins.components_cfg.get_component_cfg', mock_cfg):
        yield


@pytest.fixture(autouse=True)
def mock_processor_session(session):
    oper_id = session.oper_id
    old_process = QueueProcessor.process_one

    def _mock_process_one(*args, **kwargs):
        session.oper_id = None
        del session._passport
        res = old_process(*args, **kwargs)
        session.oper_id = oper_id
        return res

    QueueProcessor.process_one = _mock_process_one
    yield
    QueueProcessor.process_one = old_process


def create_trust_refund(session, amount=100):
    client = ob.ClientBuilder().build(session).obj
    order = create_order(client)
    invoice = create_invoice(client, order, 1001, amount)
    payment = create_trust_payment(invoice)
    invoice.create_receipt(amount)
    cpf = create_payment_ocpf(payment, amount)
    payment.transaction_id = uuid.uuid4().hex

    refund = ob.TrustInvoiceRefundBuilder(
        invoice=cpf.invoice,
        payment_id=cpf.id,
        amount=amount,
    ).build(session).obj
    refund.export()
    session.flush()
    return refund


@pytest.fixture
def refund(session):
    return create_trust_refund(session)


@pytest.fixture
def export_obj(refund):
    return refund.exports['TRUST_API']


@pytest.fixture
def api_url(app):
    component = app.get_component_cfg('yb_trust_payments_ng')
    return component['URL'].rstrip('/')


@pytest.fixture
def purchase_token(session, refund):
    payment = session.query(mapper.Payment).get(refund.cash_payment_fact.orig_id)
    return payment.transaction_id


class TestInitialize(object):
    def mock_payment_get(self, api_url, purchase_token):
        httpretty.register_uri(
            httpretty.GET,
            api_url + '/payments/%s' % purchase_token,
            json.dumps({
                "status": "success",
                "payment_result": "cleared",
                "currency": "RUB",
                "orders": [{"order_id": "666-666"}]
            })
        )

    def test_with_start_wait(self, session, api_url, purchase_token, refund, export_obj):
        trust_refund_id = uuid.uuid4().hex

        with httpretty_ensure_enabled():
            self.mock_payment_get(api_url, purchase_token)

            httpretty.register_uri(
                httpretty.POST,
                api_url + '/refunds',
                json.dumps({u'status': u'success', u'trust_refund_id': trust_refund_id})
            )
            httpretty.register_uri(
                httpretty.POST,
                api_url + '/refunds/%s/start' % trust_refund_id,
                json.dumps({u'status': u'wait_for_notification', u'status_desc': u'refund is in queue'})
            )

            QueueProcessor('TRUST_API').process_one(export_obj)
            session.flush()

            get_payment_request, create_request, start_request = httpretty.httpretty.latest_requests
            create_json = json.loads(create_request.body)

        service = session.query(mapper.Service).get(ServiceId.DIRECT)

        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=0,
                rate=0,
                input=None,
                output=None,
                error='Retrying TRUST_API processing: wait_for_notification'
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                system_uid=trust_refund_id,
                status_code=InvoiceRefundStatus.in_progress,
                status_descr=None,
            )
        )

        hamcrest.assert_that(
            get_payment_request,
            hamcrest.has_properties(
                method='GET',
                path='/trust-payments/v2/payments/%s' % purchase_token,
                headers=hamcrest.all_of(
                    hamcrest.has_entries({
                        'x-service-token': service.token,
                    }),
                    hamcrest.not_(hamcrest.has_key('x-uid')),
                ),
            )
        )
        hamcrest.assert_that(
            create_request,
            hamcrest.has_properties(
                method='POST',
                path='/trust-payments/v2/refunds',
                headers=hamcrest.all_of(
                    hamcrest.has_entries({
                        'x-service-token': service.token,
                    }),
                    hamcrest.not_(hamcrest.has_key('x-uid')),
                ),
            )
        )
        hamcrest.assert_that(
            start_request,
            hamcrest.has_properties(
                method='POST',
                path='/trust-payments/v2/refunds/%s/start' % trust_refund_id,
                headers=hamcrest.all_of(
                    hamcrest.has_entries({
                        'x-service-token': service.token,
                    }),
                    hamcrest.not_(hamcrest.has_key('x-uid')),
                ),
            )
        )
        hamcrest.assert_that(
            create_json,
            hamcrest.has_entries(
                purchase_token=purchase_token,
                orders=hamcrest.contains(hamcrest.has_entries(
                    order_id='666-666',
                    delta_amount=100
                ))
            )
        )
        hamcrest.assert_that(
            refund.invoice,
            hamcrest.has_properties(
                receipt_sum=100,
                locked_sum=100
            )
        )

    def test_with_start_success(self, session, api_url, purchase_token, refund, export_obj):
        trust_refund_id = uuid.uuid4().hex

        with httpretty_ensure_enabled():
            self.mock_payment_get(api_url, purchase_token)

            httpretty.register_uri(
                httpretty.POST,
                api_url + '/refunds',
                json.dumps({u'status': u'success', u'trust_refund_id': trust_refund_id})
            )
            httpretty.register_uri(
                httpretty.POST,
                api_url + '/refunds/%s/start' % trust_refund_id,
                json.dumps({u'status': u'success', u'status_desc': u'and we have a winner'})
            )

            QueueProcessor('TRUST_API').process_one(export_obj)
            session.flush()

        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=1,
                rate=0,
                input=None,
                output=None,
                error=None
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                system_uid=trust_refund_id,
                status_code=InvoiceRefundStatus.successful,
                status_descr=None,
            )
        )
        hamcrest.assert_that(
            refund.invoice,
            hamcrest.has_properties(
                receipt_sum=0,
                locked_sum=0
            )
        )

    def test_with_start_error(self, session, api_url, purchase_token, refund, export_obj):
        trust_refund_id = uuid.uuid4().hex

        with httpretty_ensure_enabled():
            self.mock_payment_get(api_url, purchase_token)

            httpretty.register_uri(
                httpretty.POST,
                api_url + '/refunds',
                json.dumps({u'status': u'success', u'trust_refund_id': trust_refund_id})
            )
            httpretty.register_uri(
                httpretty.POST,
                api_url + '/refunds/%s/start' % trust_refund_id,
                json.dumps({u'status': u'error', u'status_desc': u'GTFO'}),
                status=500
            )

            with mock_transactions():
                QueueProcessor('TRUST_API').process_one(export_obj)
            session.flush()

        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=0,
                rate=0,
                input=None,
                output=None,
                error=u'Retrying TRUST_API processing: error - GTFO'
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                system_uid=trust_refund_id,
                status_code=InvoiceRefundStatus.initialized,
                status_descr=None,
            )
        )
        hamcrest.assert_that(
            refund.invoice,
            hamcrest.has_properties(
                receipt_sum=100,
                locked_sum=100
            )
        )

    def test_error(self, session, api_url, purchase_token, refund, export_obj):
        with httpretty_ensure_enabled():
            self.mock_payment_get(api_url, purchase_token)

            httpretty.register_uri(
                httpretty.POST,
                api_url + '/refunds',
                json.dumps({u'status': u'error', u'status_desc': 'qwefgwouihrgqeouhouhrgewohrg'}),
                status=500
            )

            with mock_transactions():
                QueueProcessor('TRUST_API').process_one(export_obj)
            session.flush()

        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=0,
                rate=1,
                input=None,
                output=None,
                error=u'Error initializing refund - error: qwefgwouihrgqeouhouhrgewohrg'
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                system_uid=None,
                status_code=InvoiceRefundStatus.uninitialized,
                status_descr=None,
            )
        )
        hamcrest.assert_that(
            refund.invoice,
            hamcrest.has_properties(
                receipt_sum=100,
                locked_sum=100
            )
        )

    def test_final_error(self, session, api_url, purchase_token, refund, export_obj):
        export_obj.rate = 9
        session.flush()

        with httpretty_ensure_enabled():
            self.mock_payment_get(api_url, purchase_token)

            httpretty.register_uri(
                httpretty.POST,
                api_url + '/refunds',
                json.dumps({u'status': u'error', u'status_desc': 'qqq'}),
                status=500
            )

            qp = QueueProcessor('TRUST_API')
            qp.max_rate = 10
            with mock_transactions():
                qp.process_one(export_obj)
            session.flush()

        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=2,
                rate=10,
                input=None,
                output=None,
                error=u'Error initializing refund - error: qqq'
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                system_uid=None,
                status_code=InvoiceRefundStatus.failed_trust,
                status_descr='qqq',
            )
        )
        hamcrest.assert_that(
            refund.invoice,
            hamcrest.has_properties(
                receipt_sum=100,
                locked_sum=100
            )
        )

    def test_api_error(self, session, api_url, purchase_token, refund, export_obj):
        with httpretty_ensure_enabled():
            self.mock_payment_get(api_url, purchase_token)

            httpretty.register_uri(
                httpretty.POST,
                api_url + '/refunds',
                u'Да идите вы все в жопу',
                status=200
            )

            with mock_transactions():
                QueueProcessor('TRUST_API').process_one(export_obj)
            session.flush()

        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=0,
                rate=0,
                input=None,
                output=None,
                error=u'Retrying TRUST_API processing: technical issues'
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                system_uid=None,
                status_code=InvoiceRefundStatus.uninitialized,
                status_descr=None,
            )
        )

    def test_invalid_orders(self, session, api_url, purchase_token, refund, export_obj):
        with httpretty_ensure_enabled():
            httpretty.register_uri(
                httpretty.GET,
                api_url + '/payments/%s' % purchase_token,
                json.dumps({
                    "status": "success",
                    "payment_result": "cleared",
                    "currency": "RUB",
                    "orders": [{"order_id": "666-666"}, {"order_id": "6666-6666"}]
                })
            )

            with mock_transactions():
                QueueProcessor('TRUST_API').process_one(export_obj)
            session.flush()

        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=0,
                rate=1,
                input=None,
                output=None,
                error=u'Invalid refund: invalid orders in payment'
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                system_uid=None,
                status_code=InvoiceRefundStatus.uninitialized,
                status_descr=None,
            )
        )

    def test_unknown_refund_type(self, session, refund, export_obj):
        refund.cash_payment_fact.operation_type = 'TUPILITY'
        session.flush()

        with httpretty_ensure_enabled():
            with mock_transactions():
                QueueProcessor('TRUST_API').process_one(export_obj)
            session.flush()

        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=0,
                rate=1,
                input=None,
                output=None,
                error=u'Invalid refund: no payment handler for refund'
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                system_uid=None,
                status_code=InvoiceRefundStatus.uninitialized,
                status_descr=None,
            )
        )

    def test_wrong_refund_type(self, session, refund, export_obj):
        refund.cash_payment_fact.operation_type = 'ONLINE'
        session.flush()

        with httpretty_ensure_enabled():
            with mock_transactions():
                QueueProcessor('TRUST_API').process_one(export_obj)
            session.flush()

        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=0,
                rate=1,
                input=None,
                output=None,
                error=u'Invalid refund: invalid payment handler for refund'
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                system_uid=None,
                status_code=InvoiceRefundStatus.uninitialized,
                status_descr=None,
            )
        )


@pytest.mark.parametrize(
    'status_code, http_method, uri',
    [
        (InvoiceRefundStatus.initialized, httpretty.POST, '/refunds/%s/start'),
        (InvoiceRefundStatus.in_progress, httpretty.GET, '/refunds/%s'),
    ]
)
class TestCheckStart(object):
    @pytest.fixture
    def refund(self, refund, status_code):
        refund.system_uid = uuid.uuid4().hex
        refund.status_code = status_code
        refund.session.flush()
        return refund

    def test_wait(self, session, api_url, refund, export_obj, http_method, uri):
        with httpretty_ensure_enabled():
            httpretty.register_uri(
                http_method,
                api_url + uri % refund.system_uid,
                json.dumps({
                    u'status': u'wait_for_notification',
                    u'status_desc': u"don't you see that we are having lunch?"
                })
            )

            QueueProcessor('TRUST_API').process_one(export_obj)
            session.flush()

            api_request, = httpretty.httpretty.latest_requests

        service = session.query(mapper.Service).get(ServiceId.DIRECT)

        hamcrest.assert_that(
            api_request,
            hamcrest.has_properties(
                method=http_method,
                path='/trust-payments/v2' + uri % refund.system_uid,
                headers=hamcrest.all_of(
                    hamcrest.has_entries({
                        'x-service-token': service.token,
                    }),
                    hamcrest.not_(hamcrest.has_key('x-uid')),
                ),
            )
        )

        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=0,
                rate=0,
                input=None,
                output=None,
                error='Retrying TRUST_API processing: wait_for_notification'
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                system_uid=hamcrest.not_none(),
                status_code=InvoiceRefundStatus.in_progress,
                status_descr=None,
            )
        )
        hamcrest.assert_that(
            refund.invoice,
            hamcrest.has_properties(
                receipt_sum=100,
                locked_sum=100
            )
        )

    def test_finish(self, session, api_url, refund, export_obj, http_method, uri):
        with httpretty_ensure_enabled():
            httpretty.register_uri(
                http_method,
                api_url + uri % refund.system_uid,
                json.dumps({u'status': u'success', u'status_desc': u'meh'})
            )

            QueueProcessor('TRUST_API').process_one(export_obj)
            session.flush()

        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=1,
                rate=0,
                input=None,
                output=None,
                error=None
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                system_uid=hamcrest.not_none(),
                status_code=InvoiceRefundStatus.successful,
                status_descr=None,
            )
        )
        hamcrest.assert_that(
            refund.invoice,
            hamcrest.has_properties(
                receipt_sum=0,
                locked_sum=0
            )
        )

    def test_fail(self, session, api_url, refund, export_obj, http_method, uri):
        with httpretty_ensure_enabled():
            httpretty.register_uri(
                http_method,
                api_url + uri % refund.system_uid,
                json.dumps({u'status': u'failed', u'status_desc': u'nope'})
            )

            QueueProcessor('TRUST_API').process_one(export_obj)
            session.flush()

        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=1,
                rate=0,
                input=None,
                output=None,
                error=None
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                system_uid=hamcrest.not_none(),
                status_code=InvoiceRefundStatus.failed_trust,
                status_descr='nope',
            )
        )
        hamcrest.assert_that(
            refund.invoice,
            hamcrest.has_properties(
                receipt_sum=100,
                locked_sum=100
            )
        )

    def test_error(self, session, api_url, refund, status_code, export_obj, http_method, uri):
        with httpretty_ensure_enabled():
            httpretty.register_uri(
                http_method,
                api_url + uri % refund.system_uid,
                json.dumps({u'status': u'error', u'status_desc': u'HAIL SATAN'})
            )

            with mock_transactions():
                QueueProcessor('TRUST_API').process_one(export_obj)
            session.flush()

        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=0,
                rate=0,
                input=None,
                output=None,
                error='Retrying TRUST_API processing: error - HAIL SATAN'
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                system_uid=hamcrest.not_none(),
                status_code=status_code,
                status_descr=None,
            )
        )
        hamcrest.assert_that(
            refund.invoice,
            hamcrest.has_properties(
                receipt_sum=100,
                locked_sum=100
            )
        )

    def test_api_error(self, session, api_url, refund, status_code, export_obj, http_method, uri):
        with httpretty_ensure_enabled():
            httpretty.register_uri(
                http_method,
                api_url + uri % refund.system_uid,
                "json.dumps({u'status': u'error', u'status_desc': u'HAIL SATAN'})"
            )

            with mock_transactions():
                QueueProcessor('TRUST_API').process_one(export_obj)
            session.flush()

        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=0,
                rate=0,
                input=None,
                output=None,
                error='Retrying TRUST_API processing: technical issues'
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                system_uid=hamcrest.not_none(),
                status_code=status_code,
                status_descr=None,
            )
        )
        hamcrest.assert_that(
            refund.invoice,
            hamcrest.has_properties(
                receipt_sum=100,
                locked_sum=100
            )
        )

    def test_final_error(self, session, api_url, refund, status_code, export_obj, http_method, uri):
        export_obj.rate = 9
        session.flush()

        with httpretty_ensure_enabled():
            httpretty.register_uri(
                http_method,
                api_url + uri % refund.system_uid,
                json.dumps({u'status': u'error', u'status_desc': u'kthulhu fhtagn'})
            )

            qp = QueueProcessor('TRUST_API')
            qp.max_rate = 10
            with mock_transactions():
                qp.process_one(export_obj)
            session.flush()

        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=0,
                rate=9,
                input=None,
                output=None,
                error='Retrying TRUST_API processing: error - kthulhu fhtagn'
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                system_uid=hamcrest.not_none(),
                status_code=status_code,
                status_descr=None,
            )
        )

    @pytest.mark.usefixtures('http_method', 'uri')
    def test_uninitialized_refund(self, session, refund, status_code, export_obj):
        refund.system_uid = None
        session.flush()

        with httpretty_ensure_enabled():
            with mock_transactions():
                QueueProcessor('TRUST_API').process_one(export_obj)
            session.flush()

        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=0,
                rate=1,
                input=None,
                output=None,
                error='Invalid refund: No system_uid in supposedly initialized refund'
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                system_uid=None,
                status_code=status_code,
                status_descr=None,
            )
        )

    @pytest.mark.usefixtures('http_method', 'uri')
    def test_final_uninitialized_refund(self, session, refund, export_obj):
        refund.system_uid = None
        export_obj.rate = 9
        session.flush()

        with httpretty_ensure_enabled():
            qp = QueueProcessor('TRUST_API')
            qp.max_rate = 10
            with mock_transactions():
                qp.process_one(export_obj)
            session.flush()

        hamcrest.assert_that(
            export_obj,
            hamcrest.has_properties(
                state=2,
                rate=10,
                input=None,
                output=None,
                error='Invalid refund: No system_uid in supposedly initialized refund'
            )
        )
        hamcrest.assert_that(
            refund,
            hamcrest.has_properties(
                system_uid=None,
                status_code=InvoiceRefundStatus.failed_trust,
                status_descr=None,
            )
        )
