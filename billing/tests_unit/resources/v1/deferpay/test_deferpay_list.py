# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library
from future.builtins import str as text

standard_library.install_aliases()

import pytest
import itertools
import http.client as http
from decimal import Decimal as D
from flask.helpers import url_quote
from hamcrest import (
    assert_that,
    contains,
    contains_inanyorder,
    contains_string,
    equal_to,
    has_entry,
    has_entries,
    has_item,
    has_items,
    not_,
)

from balance import constants as cst
from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.resources import enums
from yb_snout_api.utils import clean_dict

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import (
    create_admin_role,
    get_client_role,
    create_role,
    create_view_client_role,
)
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice, create_custom_invoice
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.resource import mock_client_resource


RECEIPT_SUM = D('12345.67')


def make_deferpay(session, invoice):
    invoice.receipt_sum = RECEIPT_SUM
    session.flush()
    invoice.turn_on_req(request=invoice.request)
    return invoice


class TestCaseDeferpayWithOrders(TestCaseApiAppBase):
    BASE_API = '/v1/deferpay/list'

    def test_get_list(self, client):
        from yb_snout_api.resources.v1.deferpay import enums as deferpay_enums

        service = ob.ServiceBuilder.construct(self.test_session, cc='test_service_cc')

        invoice1 = make_deferpay(self.test_session, create_invoice(client, service_id=service.id))
        invoice2 = make_deferpay(self.test_session, create_invoice(client, service_id=service.id))
        invoice3 = make_deferpay(self.test_session, create_invoice(client, service_id=service.id))

        response = self.test_client.get(
            self.BASE_API,
            params={
                'service_cc': service.cc,
                'client_id': client.id,
                'sort_key': deferpay_enums.DeferpaySortKey.ISSUE_DT.name,
                'sort_order': enums.SortOrderType.DESC.name,
            },
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data', {})

        deferpay_matches = []
        deferpays = sorted(
            itertools.chain(*[i.deferpays for i in [invoice1, invoice2, invoice3]]),
            key=lambda d: d.issue_dt,
            reverse=True,
        )
        days = []
        for deferpay in deferpays:
            invoice = deferpay.invoice
            for co in invoice.consumes:
                deferpay_matches.append(
                    has_entries({
                        'issue_dt': text(deferpay.issue_dt.isoformat()),
                        'client_id': invoice.client_id,
                        'deferpay_id': deferpay.id,
                        'invoice_eid': invoice.external_id,
                        'invoice_id': invoice.id,
                        'person_id': deferpay.person.id,
                        'person_name': deferpay.person.name,
                        'person_phone': deferpay.person.phone,
                        'service_cc': co.order.service.cc,
                        'service_order_id': co.order.service_order_id,
                        'service_id': co.order.service.id,
                    }),
                )
                days.append(text(deferpay.issue_dt.isoformat()))
        assert_that(data.get('items', []), contains_inanyorder(*deferpay_matches))
        assert_that(
            [item['issue_dt'] for item in data.get('items')],
            contains(*days),
        )

    def test_search_by_service_order_id(self, client):
        service = ob.ServiceBuilder.construct(self.test_session, cc='test_service_cc')
        invoice = make_deferpay(self.test_session, create_invoice(client, service_id=service.id))
        order = invoice.consumes[0].order

        response = self.test_client.get(
            self.BASE_API,
            params={
                'service_order_id': '%s-%s' % (service.id, order.service_order_id),
            },
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data', {})
        assert_that(data['total_row_count'], equal_to(1))
        assert_that(
            data['items'],
            contains_inanyorder(*[
                has_entries({
                    'invoice_eid': invoice.external_id,
                    'deferpay_id': invoice.deferpays[0].id,
                }),
            ]),
        )


@pytest.mark.permissions
@pytest.mark.slow
@pytest.mark.smoke
class TestCaseDeferpayWithOrdersPermissions(TestCaseApiAppBase):
    BASE_API = '/v1/deferpay/list'
    role_map = {
        'client': lambda: get_client_role(),
        'view_invoices': lambda: create_role((cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: None})),
    }

    @pytest.mark.permissions
    @pytest.mark.slow
    @pytest.mark.parametrize(
        'inv_firm_ids, role_firm_ids',
        (
            ((cst.FirmId.YANDEX_OOO, cst.FirmId.MARKET),
             set()),
            ((cst.FirmId.YANDEX_OOO, cst.FirmId.MARKET),
             (cst.FirmId.YANDEX_OOO,)),
            ((cst.FirmId.YANDEX_OOO, cst.FirmId.YANDEX_OOO, cst.FirmId.MARKET),
             (cst.FirmId.YANDEX_OOO,)),
            ((cst.FirmId.YANDEX_OOO, cst.FirmId.MARKET, cst.FirmId.AUTORU),
             (cst.FirmId.YANDEX_OOO, cst.FirmId.MARKET)),
        ),
    )
    def test_filtering_by_user_constraints_firm_id(self, client, admin_role, inv_firm_ids, role_firm_ids):
        """Фильтр по фирме, указанной в роли"""
        session = self.test_session
        role = ob.create_role(session, (cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: None}))
        invoices = []
        required_invoices = []

        if role_firm_ids:
            roles = [
                (role, {cst.ConstraintTypes.firm_id: firm_id})
                for firm_id in role_firm_ids
            ]
        else:
            roles = [role]
        roles.append(admin_role)

        security.set_passport_client(client)
        security.set_roles(roles)

        for firm_id in inv_firm_ids:
            order = ob.OrderBuilder(client=client)
            invoice = create_custom_invoice({order: D('50')}, client, firm_id)
            make_deferpay(session, invoice)
            invoices.append(invoice)
            if firm_id in role_firm_ids:
                required_invoices.append(invoice)
        session.flush()

        if not role_firm_ids:
            required_invoices = invoices  # нет ограничения по фирме

        response = self.test_client.get(self.BASE_API, {'client_id': client.id})
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        assert_that(data['total_row_count'], equal_to(len(required_invoices)))
        assert_that(
            data['items'],
            contains_inanyorder(*[
                has_entries({
                    'invoice_eid': inv.external_id,
                    'deferpay_id': inv.deferpays[0].id,
                })
                for inv in required_invoices
            ]),
        )

    @pytest.mark.parametrize(
        'role_builders',
        [
            [role_map['client']],
            [role_map['client'], role_map['view_invoices']],  # даже с админскими правами нельзя получить не свой счёт

        ],
    )
    @mock_client_resource('yb_snout_api.resources.v1.deferpay.routes.get_list.DeferpayWithOrdersList')
    def test_client_owns_invoice(self, client_role, role_builders):
        session = self.test_session
        firm_id = cst.FirmId.YANDEX_OOO
        from_dt = session.now()

        client1 = create_client()
        client2 = create_client()

        roles = [builder() for builder in role_builders]
        security.set_passport_client(client1)
        security.set_roles(roles)

        order1 = ob.OrderBuilder(client=client1)
        invoice1 = create_custom_invoice({order1: D('50')}, client1, firm_id)
        make_deferpay(session, invoice1)
        order2 = ob.OrderBuilder(client=client2)
        invoice2 = create_custom_invoice({order2: D('50')}, client2, firm_id)
        make_deferpay(session, invoice2)
        session.flush()
        to_dt = session.now()

        response = self.test_client.get(
            self.BASE_API,
            {
                'from_dt': from_dt.strftime('%Y-%m-%dT%H:%M:%S'),
                'to_dt': to_dt.strftime('%Y-%m-%dT%H:%M:%S'),
                'firm_id': firm_id,
            },
            is_admin=False,
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        assert_that(
            data['items'],
            [has_entries({'invoice_eid': invoice1.external_id, 'client_id': client1.id})],
        )

    @mock_client_resource('yb_snout_api.resources.v1.deferpay.routes.get_list.DeferpayWithOrdersList')
    def test_client_ui_admin_user(self, admin_role, view_client_role, client):
        session = self.test_session
        role = ob.create_role(session, (cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: None}))

        from_dt = session.now()
        roles = [
            admin_role,
            view_client_role,
            (role, {cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO}),
        ]
        security.set_roles(roles)

        invoice1 = create_custom_invoice(client=client, firm_id=cst.FirmId.YANDEX_OOO)
        make_deferpay(session, invoice1)
        invoice2 = create_custom_invoice(client=client, firm_id=cst.FirmId.CLOUD)
        make_deferpay(session, invoice2)
        session.flush()
        to_dt = session.now()

        response = self.test_client.get(
            self.BASE_API,
            {
                'from_dt': from_dt.strftime('%Y-%m-%dT%H:%M:%S'),
                'to_dt': to_dt.strftime('%Y-%m-%dT%H:%M:%S'),
                'client_id': client.id,
            },
            is_admin=False,
        )
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        assert_that(data['items'], has_item(has_entry('invoice_eid', invoice1.external_id)))
        assert_that(data['items'], not_(has_item(has_entry('invoice_eid', invoice2.external_id))))

    @pytest.mark.permissions
    def test_permission_denied_for_direct_limited(self, client):
        security.update_limited_role(client)
        response = self.test_client.get(self.BASE_API)
        assert_that(response.status_code, equal_to(http.FORBIDDEN), 'response code must be FORBIDDEN')


class TestCaseDeferpayWithOrdersXls(TestCaseApiAppBase):
    BASE_API = '/v1/deferpay/list/xls'

    @pytest.mark.parametrize(
        'test_fname, expected_fname_in_context',
        [
            ('тестовое_имя_файла.xls', 'filename*=UTF-8\'\'' + url_quote('тестовое_имя_файла.xls')),
            ('тестовое_имя_файла', 'filename*=UTF-8\'\'' + url_quote('тестовое_имя_файла.xls')),
            ('test_name.xls', 'filename=test_name.xls'),
            ('test_name', 'filename=test_name.xls'),
        ],
    )
    def test_get_list_xls(self, invoice, test_fname, expected_fname_in_context):
        make_deferpay(self.test_session, invoice)

        params = clean_dict({
            'service_cc': invoice.service.cc,
            'client_id': invoice.client_id,
            'filename': test_fname.encode('utf-8'),
        })

        response = self.test_client.get(self.BASE_API, params)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        headers = response.headers
        assert_that(response.content_type, equal_to(enums.Mimetype.XLS.value))
        assert_that(
            headers,
            has_items(
                contains(
                    'Content-Disposition',
                    contains_string(expected_fname_in_context),
                ),
                contains('Content-Type', enums.Mimetype.XLS.value),
            ),
        )
