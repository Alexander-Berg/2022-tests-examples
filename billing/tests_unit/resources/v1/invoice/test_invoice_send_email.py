# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
import mock
import httpretty
import hamcrest as hm

from balance import mapper
from balance import muzzle_util as ut
from balance.constants import (
    PermissionCode,
    ConstraintTypes,
    FirmId,
)
from muzzle.captcha import URL as CAPTCHA_URL

from yb_snout_api.resources.v1.invoice import enums
from yb_snout_api.utils import context_managers as ctx_util
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_manager
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import get_client_role, create_admin_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice
from brest.core.tests import security
from tests import object_builder as ob


@pytest.mark.smoke
@pytest.mark.usefixtures('httpretty_enabled_fixture')
class TestCaseInvoiceSendEmail(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/send/email'
    recepient_name = 'client'
    recepient_address = u'ру_test111@qCWF.rKU'
    messages_count = 2

    @staticmethod
    def _register_captcha_checking():
        httpretty.register_uri(
            httpretty.GET,
            CAPTCHA_URL + '/check',
            '''<?xml version="1.0"?><image_check>ok</image_check>''',
            status=200,
        )

    @pytest.mark.parametrize(
        'opcode',
        [enums.EmailOpcodeType.FORM, enums.EmailOpcodeType.MSWORD],
    )
    @pytest.mark.parametrize(
        'memo',
        ['English Text', u'Русский текст'],
    )
    def test_send_email(
            self,
            manager,
            memo,
            invoice,
            opcode,
    ):
        session = self.test_session
        invoice.manager = manager  # прикрепляем к счёту менеджера
        session.flush()

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
                'manager_cc': True,
                'memo': memo,
                'opcode': opcode.name,
                'recipient_address': self.recepient_address,
            },
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

        messages = session.query(
            mapper.EmailMessage.opcode,
            mapper.EmailMessage.object_id,
            mapper.EmailMessage.memo_c,
            mapper.EmailMessage.recepient_name,
            mapper.EmailMessage.recepient_address,
        ).filter_by(
            opcode=opcode.value,
            object_id=invoice.id,
        ).all()

        hm.assert_that(messages, hm.has_length(self.messages_count), u'Must be 2 letters')
        hm.assert_that(
            messages,
            hm.contains_inanyorder(
                (opcode.value, invoice.id, ut.utf8(memo), self.recepient_name, self.recepient_address),
                (opcode.value, invoice.id, ut.utf8(memo), self.recepient_name, manager.email),
            ),
        )

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'is_admin',
        [True, False],
    )
    def test_access_perm_ok(self, client, is_admin):
        data = {
            'manager_cc': True,
            'memo': '123',
            'opcode': enums.EmailOpcodeType.FORM.name,
            'recipient_address': self.recepient_address,
        }

        if is_admin:
            role = ob.create_role(
                self.test_session,
                PermissionCode.ADMIN_ACCESS,
                (PermissionCode.SEND_INVOICES, {ConstraintTypes.firm_id: None}),
            )
            security.set_roles([role])

        else:
            security.set_roles([])
            security.set_passport_client(client)
            data.update({
                '_captcha_key': 'key',
                '_captcha_rep': 'rep',
            })
            self._register_captcha_checking()

        invoice = create_invoice(client=client)
        data['invoice_id'] = invoice.id

        response = self.test_client.secure_post(
            self.BASE_API,
            data=data,
            is_admin=is_admin,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

    @pytest.mark.permissions
    @mock.patch('yb_snout_api.utils.context_managers._new_transactional_session', ctx_util.new_rollback_session)
    def test_access_perm_fail(self):
        role = ob.create_role(
            self.test_session,
            PermissionCode.ADMIN_ACCESS,
            (PermissionCode.SEND_INVOICES, {ConstraintTypes.firm_id: [FirmId.TAXI]}),
        )
        security.set_roles([role])

        invoice = create_invoice()
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
                'manager_cc': True,
                'memo': '123',
                'opcode': enums.EmailOpcodeType.FORM.name,
                'recipient_address': self.recepient_address,
            },
        )
        hm.assert_that(
            response,
            hm.has_properties(
                status_code=http.FORBIDDEN,
                json=hm.has_entries(
                    description='User %s has no permission SendInvoices.' % self.test_session.oper_id,
                    error='PERMISSION_DENIED',
                ),
            ),
        )

    @pytest.mark.permissions
    def test_nobody(self, client, client_role):
        self._register_captcha_checking()
        security.set_passport_client(client)
        security.set_roles([client_role])

        invoice = create_invoice()
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
                'recipient_address': self.recepient_address,
                '_captcha_key': 'key',
                '_captcha_rep': 'rep',
            },
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN))

    def test_wo_captcha(self, invoice):
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
                'recipient_address': self.recepient_address,
            },
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            response.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    '_captcha_key': hm.contains(hm.has_entries({'error': 'REQUIRED_FIELD_VALIDATION_ERROR'})),
                    '_captcha_rep': hm.contains(hm.has_entries({'error': 'REQUIRED_FIELD_VALIDATION_ERROR'})),
                }),
            }),
        )

    def test_w_captcha(self, invoice):
        opcode = enums.EmailOpcodeType.FORM
        memo = 'Aaaaa'
        self._register_captcha_checking()

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
                'manager_cc': True,
                'memo': memo,
                'opcode': opcode.name,
                'recipient_address': self.recepient_address,
                '_captcha_key': 'key',
                '_captcha_rep': 'rep',
            },
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        messages = (
            self.test_session
            .query(mapper.EmailMessage)
            .filter_by(
                opcode=opcode.value,
                object_id=invoice.id,
            )
            .all()
        )

        hm.assert_that(
            messages,
            hm.contains_inanyorder(
                hm.has_properties({
                    'opcode': opcode.value,
                    'object_id': invoice.id,
                    'memo_c': ut.utf8(memo),
                    'recepient_name': self.recepient_name,
                    'recepient_address': self.recepient_address,
                }),
            ),
        )
