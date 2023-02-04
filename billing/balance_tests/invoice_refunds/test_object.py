# -*- coding: utf-8 -*-

import pickle
import uuid

import pytest

from balance import exc
from balance import mapper
from balance.constants import (
    InvoiceRefundStatus,
    PermissionCode,
    GENERIC_MAKO_CREATOR_MESSAGE_OPCODE,
)

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


@pytest.mark.parametrize('strict', [False, True], ids=['not_strict', 'strict'])
class TestUnlockAllowed(object):
    @pytest.mark.parametrize(
        'status_code, result',
        [
            (InvoiceRefundStatus.not_exported, False),
            (InvoiceRefundStatus.exported, False),
            (InvoiceRefundStatus.export_failed, True),
            (InvoiceRefundStatus.oebs_transmitted, False),
            (InvoiceRefundStatus.oebs_reconciled, False),
            (InvoiceRefundStatus.successful, False),
            (InvoiceRefundStatus.successful_reconciled, False),
            (InvoiceRefundStatus.uninitialized, False),
            (InvoiceRefundStatus.initialized, False),
            (InvoiceRefundStatus.in_progress, False),
            (InvoiceRefundStatus.failed, True),
            (InvoiceRefundStatus.failed_trust, True),
        ]
    )
    def test_status(self, session, strict, status_code, result):
        refund = create_oebs_refund(session, 666)
        refund.set_status(status_code)

        assert refund.check_unlock_allowed(strict) is result

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'perm, result',
        [
            (PermissionCode.ADDITIONAL_FUNCTIONS, False),
            (PermissionCode.DO_INVOICE_REFUNDS, True),
        ]
    )
    def test_permission(self, session, perm, result, strict):
        refund = create_oebs_refund(session, 666)
        refund.set_status(InvoiceRefundStatus.failed)

        role = ob.create_role(session, perm)
        ob.create_passport(session, role, patch_session=True)

        if result:
            assert refund.check_unlock_allowed(strict) is True
        elif not strict:
            assert refund.check_unlock_allowed(strict) is False
        else:
            with pytest.raises(exc.PERMISSION_DENIED) as exc_info:
                refund.check_unlock_allowed(strict)
            assert 'no permission DoInvoiceRefunds' in str(exc_info.value)


class TestStatusChange(object):
    @pytest.mark.parametrize(
        'status_code, status_descr, has_msg',
        [
            pytest.param(InvoiceRefundStatus.exported, None, False, id='exported'),
            pytest.param(InvoiceRefundStatus.oebs_transmitted, None, False, id='oebs_transmitted'),
            pytest.param(InvoiceRefundStatus.oebs_reconciled, None, False, id='oebs_reconciled'),
            pytest.param(InvoiceRefundStatus.successful, None, False, id='successful'),
            pytest.param(InvoiceRefundStatus.export_failed, 'export failed', True, id='export_failed'),
            pytest.param(InvoiceRefundStatus.failed, 'failed', True, id='failed'),
            pytest.param(InvoiceRefundStatus.failed_trust, 'failed_trust', True, id='failed_trust'),
        ]
    )
    def test_fail_notify(self, session, status_code, status_descr, has_msg):
        refund = create_oebs_refund(session, 100)
        session.expire_all()

        refund.set_status(status_code, status_descr)
        session.flush()
        assert refund.status_code == status_code
        assert refund.status_descr == status_descr

        msgs = (
            session.query(mapper.EmailMessage)
                .filter_by(opcode=GENERIC_MAKO_CREATOR_MESSAGE_OPCODE,
                           object_id=refund.id)
                .all()
        )
        if has_msg:
            assert len(msgs) == 1
            data = pickle.loads(msgs[0].data)
            assert data == (
                u'Ошибка возврата по счету %s' % refund.invoice.external_id,
                None,
                None,
                'refunds/fail.mako',
                {'invoice': refund.invoice.external_id,
                 'reason': status_descr,
                 'status': status_code,
                 'amount': '100'},
            )
        else:
            assert not msgs
