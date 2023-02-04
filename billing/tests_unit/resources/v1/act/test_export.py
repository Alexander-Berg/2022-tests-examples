# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
from datetime import timedelta
from hamcrest import assert_that, equal_to, has_entries

from balance import mapper, multilang_support
from muzzle.api.act import ExportStatus
from yb_snout_api.resources.enums import SortOrderType
from yb_snout_api.resources.v1.act.enums import ActsSortKeyType
from yb_snout_api.utils.ma_fields import DT_FMT
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.act import create_act


def check_dict(d, params):
    d1 = {
        'method': 'send_email_with_acts',
        'need_fic': str(int(params.get('need_fictive_invoice', 0))),
        'sf': params.get('sort_key', ActsSortKeyType.ACT_DT.value).lower(),
        'so': str(int(params.get('sort_order', SortOrderType.ASC.value) == SortOrderType.ASC.value))
    }
    if params.get('dt_from', None):
        d1['act_dt_from'] = params['dt_from']
    if params.get('dt_to', None):
        d1['act_dt_to'] = params['dt_to']
    if params.get('client_id', None):
        d1['client_id'] = str(params['client_id'])
    if params.get('contract_eid', None):
        d1['contract_eid'] = params['contract_eid']

    if params.get('act_eid', None):
        d1['external_id'] = params['act_eid']
    if params.get('factura', None):
        d1['factura'] = params['factura']
    if params.get('firm_id', None):
        d1['firm_id'] = str(params['firm_id'])
    if params.get('invoice_eid', None):
        d1['invoice_eid'] = params['invoice_eid']
    if params.get('manager_code', None):
        d1['manager_code'] = str(params['manager_code'])
    if params.get('person_id', None):
        d1['person_id'] = str(params['person_id'])
    if params.get('service_id', None):
        d1['service_id'] = str(params['service_id'])

    assert_that(d, has_entries(d1))


def check_state_obj_is_valid(state_obj, params):
    state_dict = state_obj.to_dict(prefix='req_', strip=True)
    domain = state_obj.domain
    check_dict(state_dict, params)


def check_request_obj_is_valid(request_obj, params):
    request_dict = request_obj.to_dict(prefix="req_", strip=True)
    check_dict(request_dict, params)


@pytest.mark.smoke
class TestCaseActExportEmail(TestCaseApiAppBase):
    BASE_API = u'/v1/act/export/email'

    @pytest.mark.parametrize("chosen_params", [
        (),
        ('client_id',),
        ('client_id', 'act_eid', 'contract_eid', 'currency_code', 'dt_from', 'dt_to', 'factura', 'firm_id',
         'invoice_eid', 'manager_code', 'need_fictive_invoice', 'person_id', 'service_id', 'sort_key', 'sort_order',)
    ])
    def test_export_email(self, act, chosen_params):
        default_params = {
            'client_id': act.client_id,
            'act_eid': act.external_id,
            'contract_eid': '123456',
            'currency_code': None,
            'dt_from': (act.dt - timedelta(days=1)).strftime(DT_FMT),
            'dt_to': (act.dt + timedelta(days=1)).strftime(DT_FMT),
            'factura': act.factura,
            'firm_id': act.invoice.firm_id,
            'invoice_eid': act.invoice.external_id,
            'manager_code': act.invoice.manager_code,
            'need_fictive_invoice': True,
            'person_id': act.invoice.person_id,
            'service_id': act.invoice.service_id,
            'sort_key': ActsSortKeyType.CURRENCY.name,
            'sort_order': SortOrderType.DESC.name,
        }
        params = {k: default_params[k] for k in default_params if k in chosen_params}

        response = self.test_client.secure_post(
            self.BASE_API,
            data=params,
        )

        assert_that(response.status_code, equal_to(http.OK))
        assert_that(response.get_json()['data'], equal_to('ENQUEUED'))

        export_obj = act.session.query(mapper.Export).getone(
            object_id=act.session.oper_id,
            type='EMAIL_DOCUMENT',
            classname='Passport'
        )
        is_admin, state_obj, request_obj = export_obj.input['args']

        check_state_obj_is_valid(state_obj, params)
        check_request_obj_is_valid(request_obj, params)

    def test_is_processing(self, act):
        response = self.test_client.secure_post(
            self.BASE_API,
            data={},
        )
        assert_that(response.status_code, equal_to(http.OK))
        assert_that(response.get_json()['data'], equal_to(ExportStatus.ENQUEUED))

        response = self.test_client.secure_post(
            self.BASE_API,
            data={},
        )
        assert_that(response.status_code, equal_to(http.OK))
        assert_that(response.get_json()['data'], equal_to(ExportStatus.PROCESSING))
