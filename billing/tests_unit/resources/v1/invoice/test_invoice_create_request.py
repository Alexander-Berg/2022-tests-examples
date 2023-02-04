# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import datetime
import http.client as http
import hamcrest as hm
from decimal import Decimal as D

from balance import constants as cst, mapper

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_agency, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.manager import create_manager
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role
from yb_snout_api.tests_unit.fixtures.resource import mock_client_resource


@pytest.fixture(name='create_req_role')
def create_create_req_role():
    return create_role(
        (
            cst.PermissionCode.CREATE_REQUESTS_SHOP,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


class TestCaseCreateRequest(TestCaseApiAppBase):
    BASE_API = '/v1/invoice/create-request'

    @pytest.mark.smoke
    def test_base(self, client):
        docs_dt = (datetime.datetime.now() + datetime.timedelta(66)).replace(microsecond=0)

        params = {
            'client_id': client.id,
            'firm_id': cst.FirmId.YANDEX_OOO,
            'documents_dt': docs_dt.strftime('%Y-%m-%dT%H:%M:%S'),
            'raw_order_data': [
                {
                    'product_id': cst.DIRECT_PRODUCT_ID,
                    'quantity': '1.56',
                    'discount': '10.5',
                    'memo': 'абыр',
                    'order_client_id': client.id,
                },
                {
                    'product_id': cst.DIRECT_PRODUCT_RUB_ID,
                    'quantity': '30.3333',
                    'discount': '20.6',
                    'memo': 'валг',
                    'order_client_id': client.id,
                },
            ],
        }
        response = self.test_client.secure_post_json(self.BASE_API, params)
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        request_id = response.get_json()['data']['request_id']
        request = self.test_session.query(mapper.Request).getone(request_id)

        hm.assert_that(
            request,
            hm.has_properties(
                client_id=client.id,
                desired_invoice_dt=docs_dt,
                firm_id=cst.FirmId.YANDEX_OOO,
                rows=hm.contains_inanyorder(
                    hm.has_properties(
                        quantity=D('1.56'),
                        u_discount_pct=D('10.5'),
                        discount_pct=0,
                        order=hm.has_properties(
                            service_id=cst.ServiceId.ONE_TIME_SALE,
                            manager_code=None,
                            client_id=client.id,
                            agency_id=None,
                            service_code=cst.DIRECT_PRODUCT_ID,
                            text='абыр',
                        ),
                    ),
                    hm.has_properties(
                        quantity=D('30.3333'),
                        u_discount_pct=D('20.6'),
                        discount_pct=0,
                        order=hm.has_properties(
                            service_id=cst.ServiceId.ONE_TIME_SALE,
                            manager_code=None,
                            client_id=client.id,
                            agency_id=None,
                            service_code=cst.DIRECT_PRODUCT_RUB_ID,
                            text='валг',
                        ),
                    ),
                ),
            ),
        )

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'perm_firm_id, match_client, ans',
        [
            pytest.param(None, True, http.OK, id='wo firm_id'),
            pytest.param(cst.FirmId.YANDEX_OOO, True, http.OK, id='right firm'),
            pytest.param(cst.FirmId.DRIVE, True, http.FORBIDDEN, id='wrong firm'),
            pytest.param(cst.FirmId.YANDEX_OOO, False, http.FORBIDDEN, id='wrong client_id'),
        ],
    )
    def test_perms_ok(self, client, admin_role, create_req_role, perm_firm_id, match_client, ans):
        client_batch_id = create_role_client(client=client if match_client else None).client_batch_id
        roles = [
            admin_role,
            (create_req_role, {cst.ConstraintTypes.firm_id: perm_firm_id, cst.ConstraintTypes.client_batch_id: client_batch_id}),
        ]
        security.set_roles(roles)

        response = self.test_client.secure_post_json(
            self.BASE_API,
            {
                'client_id': client.id,
                'firm_id': cst.FirmId.YANDEX_OOO,
                'raw_order_data': [
                    {
                        'product_id': cst.DIRECT_PRODUCT_ID,
                        'quantity': 1,
                        'discount': 10,
                        'memo': 'абыр',
                        'order_client_id': client.id,
                    },
                ],
            },
        )
        hm.assert_that(response.status_code, hm.equal_to(ans))

    def test_nobody(self, client, admin_role):
        security.set_roles([admin_role])
        response = self.test_client.secure_post_json(
            self.BASE_API,
            {
                'client_id': client.id,
                'firm_id': cst.FirmId.YANDEX_OOO,
                'raw_order_data': [
                    {
                        'product_id': cst.DIRECT_PRODUCT_ID,
                        'quantity': 1,
                        'discount': 10,
                        'memo': 'абыр',
                        'order_client_id': client.id,
                    },
                ],
            },
        )
        hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN))

    @pytest.mark.permissions
    @mock_client_resource('yb_snout_api.resources.v1.invoice.routes.create_request.CreateRequest')
    def test_owner(self, client):
        security.set_roles([])
        security.set_passport_client(client)

        response = self.test_client.secure_post_json(
            self.BASE_API,
            {
                'client_id': client.id,
                'firm_id': cst.FirmId.YANDEX_OOO,
                'raw_order_data': {
                    'product_id': cst.DIRECT_PRODUCT_ID,
                    'quantity': 1,
                    'discount': 10,
                    'memo': 'абыр',
                    'order_client_id': client.id,
                },
            },
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN))

    def test_agency(self, agency, client):
        client.agency = agency
        self.test_session.flush()

        response = self.test_client.secure_post_json(
            self.BASE_API,
            {
                'client_id': agency.id,
                'firm_id': cst.FirmId.YANDEX_OOO,
                'raw_order_data': [
                    {
                        'product_id': cst.DIRECT_PRODUCT_ID,
                        'quantity': 1,
                        'discount': 10,
                        'memo': 'абыр',
                        'order_client_id': client.id,
                    },
                ],
            },
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        request_id = response.get_json()['data']['request_id']
        request = self.test_session.query(mapper.Request).get(request_id)
        hm.assert_that(
            request,
            hm.has_properties(
                client_id=agency.id,
                rows=hm.contains_inanyorder(
                    hm.has_properties(
                        order=hm.has_properties(
                            client_id=client.id,
                            agency_id=agency.id,
                        ),
                    ),
                ),
            ),
        )

    def test_manager(self, client, manager):
        response = self.test_client.secure_post_json(
            self.BASE_API,
            data={
                'client_id': client.id,
                'manager_code': manager.manager_code,
                'firm_id': cst.FirmId.YANDEX_OOO,
                'raw_order_data': {
                    'product_id': cst.DIRECT_PRODUCT_ID,
                    'quantity': 1,
                    'discount': 10,
                    'memo': 'абыр',
                    'order_client_id': client.id,
                },
            },
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        request_id = response.get_json()['data']['request_id']
        request = self.test_session.query(mapper.Request).get(request_id)
        hm.assert_that(
            request,
            hm.has_properties(
                client_id=client.id,
                rows=hm.contains_inanyorder(
                    hm.has_properties(
                        order=hm.has_properties(
                            manager=manager,
                        ),
                    ),
                ),
            ),
        )

    @pytest.mark.parametrize(
        'raw_order_data, error_msg',
        [
            pytest.param(cst.SENTINEL, 'REQUIRED_FIELD_VALIDATION_ERROR', id='require'),
            pytest.param(None, 'EMPTY_FIELD_VALIDATION_ERROR', id='null'),
            pytest.param([], 'RANGE_FIELD_VALIDATION_ERROR', id='length'),
        ],
    )
    def test_wo_rows(self, client, raw_order_data, error_msg):
        params = {
            'client_id': client.id,
            'firm_id': cst.FirmId.YANDEX_OOO,
        }
        if raw_order_data is not cst.SENTINEL:
            params['raw_order_data'] = raw_order_data
        response = self.test_client.secure_post_json(self.BASE_API, params)
        hm.assert_that(response.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            response.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    'raw_order_data': hm.contains(hm.has_entries({'error': error_msg})),
                }),
            }),
        )
