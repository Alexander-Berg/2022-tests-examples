# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()


import http.client as http
from hamcrest import assert_that, equal_to
import pytest

from balance import mapper

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.export import create_export_for_payment, ExportPaymentParam
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice


class TestCaseExportSetState(TestCaseApiAppBase):
    BASE_API = '/v1/export/set-state'
    INVOICE_EXPORT_TYPE = 'OEBS'

    @pytest.mark.parametrize(
        'state',
        ['WAITING', 'EXPORTED'],
    )
    def test_set_state(self, invoice, state):
        from yb_snout_api.resources import enums

        session = self.test_session

        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'classname': invoice.__class__.__name__,
                'object_id': invoice.id,
                'queue_type': self.INVOICE_EXPORT_TYPE,
                'state': state,
            },
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        export = session.query(mapper.Export).getone(
            classname=invoice.__class__.__name__,
            type=self.INVOICE_EXPORT_TYPE,
            object_id=invoice.id,
        )
        required_state = getattr(enums.ExportState, state).value
        assert_that(export.state, equal_to(required_state))

    def test_fail_export_type(self, export_payment_id):
        response = self.test_client.secure_post(
            self.BASE_API,
            data={
                'classname': ExportPaymentParam.classname,
                'object_id': export_payment_id,
                'queue_type': ExportPaymentParam.fail_queue_type,
            },
        )
        assert_that(response.status_code, equal_to(http.NOT_FOUND), 'response code must be NOT_FOUND')
