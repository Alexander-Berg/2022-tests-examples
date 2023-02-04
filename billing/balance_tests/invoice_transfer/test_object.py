# -*- coding: utf-8 -*-

import pickle
import uuid

import pytest

from balance import exc
from balance import mapper
from balance.constants import (
    InvoiceTransferStatus,
    PermissionCode,
    GENERIC_MAKO_CREATOR_MESSAGE_OPCODE,
)

from tests import object_builder as ob
from tests.balance_tests.invoice_transfer.common import (
    create_invoice,
)

PAYSYS_ID_BANK = 1001


pytestmark = [
    pytest.mark.invoice_transfer,
]


def create_invoice_transfer(session, amount=100):
    client = ob.ClientBuilder().build(session).obj
    src_invoice = create_invoice(client, PAYSYS_ID_BANK, 100)
    dst_invoice = create_invoice(client, PAYSYS_ID_BANK, 100, person=src_invoice.person)

    invoice_transfer = ob.InvoiceTransferBuilder(
        src_invoice=src_invoice,
        dst_invoice=dst_invoice,
        amount=amount
    ).build(session).obj
    invoice_transfer.set_status(InvoiceTransferStatus.exported)
    session.flush()
    return invoice_transfer


class TestUnlockAllowed(object):
    @pytest.mark.parametrize(
        'status_code, result',
        [
            (InvoiceTransferStatus.not_exported, False),
            (InvoiceTransferStatus.exported, False),
            (InvoiceTransferStatus.export_failed, True),
            (InvoiceTransferStatus.in_progress, False),
            (InvoiceTransferStatus.successful, False),
            (InvoiceTransferStatus.failed_unlocked, False),
        ]
    )
    def test_status(self, session, status_code, result):
        invoice_transfer = create_invoice_transfer(session)
        invoice_transfer.set_status(status_code)

        if result:
            assert invoice_transfer.check_unlock_allowed() is True
        else:
            with pytest.raises(exc.INVOICE_TRANSFER_UNLOCK_FORBIDDEN) as exc_info:
                invoice_transfer.check_unlock_allowed()
            assert 'is not allowed to unlock due to incorrect status' in str(exc_info.value)

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'perm, result',
        [
            (PermissionCode.ADDITIONAL_FUNCTIONS, False),
            (PermissionCode.DO_INVOICE_TRANSFER, True),
        ]
    )
    def test_permission(self, session, perm, result):
        invoice_transfer = create_invoice_transfer(session)
        invoice_transfer.set_status(InvoiceTransferStatus.export_failed)

        role = ob.create_role(session, perm)
        ob.create_passport(session, role, patch_session=True)

        if result:
            assert invoice_transfer.check_unlock_allowed() is True
        else:
            with pytest.raises(exc.PERMISSION_DENIED) as exc_info:
                invoice_transfer.check_unlock_allowed()
            assert 'no permission DoInvoiceTransfer' in str(exc_info.value)


class TestStatusChange(object):
    @pytest.mark.parametrize(
        'status_code, status_descr, has_msg',
        [
            (InvoiceTransferStatus.not_exported, None, False),
            (InvoiceTransferStatus.exported, None, False),
            (InvoiceTransferStatus.export_failed, 'Перенос поломатый', True),
            (InvoiceTransferStatus.in_progress, None, False),
            (InvoiceTransferStatus.successful, None, False),
            (InvoiceTransferStatus.failed_unlocked, 'Перенос поломатый', True),  # useless test
        ]
    )
    def test_fail_notify(self, session, status_code, status_descr, has_msg):
        invoice_transfer = create_invoice_transfer(session)
        session.expire_all()

        invoice_transfer.set_status(status_code, status_descr)
        session.flush()
        assert invoice_transfer.status_code == status_code
        assert invoice_transfer.status_descr == status_descr

        msgs = (
            session.query(mapper.EmailMessage)
                .filter_by(opcode=GENERIC_MAKO_CREATOR_MESSAGE_OPCODE,
                           object_id=invoice_transfer.id)
                .all()
        )
        if has_msg:
            assert len(msgs) == 1
            data = pickle.loads(msgs[0].data)
            assert data == (
                u'Ошибка переноса со счета %s на счет %s' % (
                    invoice_transfer.src_invoice.external_id, invoice_transfer.dst_invoice.external_id
                ),
                None,
                None,
                'invoice_transfer/fail.mako',
                {'src_invoice': invoice_transfer.src_invoice.external_id,
                 'dst_invoice': invoice_transfer.dst_invoice.external_id,
                 'reason': status_descr,
                 'status': status_code,
                 'amount': '100'}
            )
        else:
            assert not msgs
