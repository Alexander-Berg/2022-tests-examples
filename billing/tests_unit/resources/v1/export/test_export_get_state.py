# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import datetime
import http.client as http
from hamcrest import assert_that, equal_to, has_entries

from yb_snout_api.resources.enums import ExportState
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.export import (
    create_export_for_payment,
    ExportPaymentParam,
)
import tests.object_builder as ob


class TestCaseExportGetState(TestCaseApiAppBase):
    BASE_API = u'/v1/export/get-state'

    def test_get_export_state(self, export_payment_id):
        from yb_snout_api.resources import enums

        response = self.test_client.get(
            self.BASE_API,
            {
                'classname': ExportPaymentParam.classname,
                'queue_type': ExportPaymentParam.queue_type,
                'object_id': export_payment_id,
            },
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json()['data']
        assert_that(data['state'], enums.ExportState.EXPORTED.value)
        assert_that(data['export_dt'], ExportPaymentParam.export_dt)

    def test_not_found(self, not_existing_id):
        response = self.test_client.get(
            self.BASE_API,
            {
                'classname': ExportPaymentParam.classname,
                'queue_type': ExportPaymentParam.queue_type,
                'object_id': not_existing_id,
            },
        )
        assert_that(
            response.get_json()['data'],
            has_entries('export_dt', None, 'state', ExportState.NOT_FOUND.name),
            'Response should contain NOT_FOUND state',
        )

    def test_export_person_oebs_api(self):
        from yb_snout_api.resources import enums
        session = self.get_test_session()
        session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Person': 1}
        person = ob.PersonBuilder.construct(session)
        person.exports['OEBS_API'].export_dt = datetime.datetime.now()
        assert not person.exports.get('OEBS')
        response = self.test_client.get(
            self.BASE_API,
            {
                'classname': 'Person',
                'queue_type': 'OEBS',
                'object_id': person.id,
            },
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json()['data']
        assert_that(data['state'], enums.ExportState.EXPORTED.value)
        assert_that(data['export_dt'], person.exports['OEBS_API'].export_dt)
