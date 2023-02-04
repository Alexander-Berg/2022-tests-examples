# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import mock
import pytest
import hamcrest as hm
import http.client as http

from balance import mapper, constants as cst, exc
from balance.actions import promocodes as promo_actions

from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
from yb_snout_api.tests_unit.fixtures.person import create_person
from yb_snout_api.tests_unit.fixtures.request import create_request
from yb_snout_api.tests_unit.fixtures.common import not_existing_id
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role, get_client_role
from yb_snout_api.tests_unit.fixtures.promocode import create_legacy_promocode


class Region(object):
    def __init__(self, id, name, en_name, type):
        self.id = id
        self.name = name
        self.ename = en_name
        self.type = type


class Geolookup(mock.MagicMock):
    regions = {
        None: Region(0, 'Яндекс.KP', 'Yandex.KR', -1),
        1: Region(1, 'Россия', 'Russia', 0),
        2: Region(2, 'Исландия', 'Iceland', 0),
        3: Region(3, 'Япония', 'Japan', 0),
    }

    def region_by_id(self, id):
        return self.regions[id]


def prepare_request_params(client=None, person=None, **kwargs):
    client = client or create_client()
    person = person or create_person(client)
    request = create_request(client=client, firm_id=cst.FirmId.YANDEX_OOO)
    request_params = {
        'request_id': request.id,
        'person_id': person.id,
        'firm_id': request.firm_id,
        'iso_currency': 'RUB',
        'payment_method_id': cst.PaymentMethodIDs.bank,
        'paysys_group_id': cst.PaysysGroupIDs.default
    }
    for k, v in kwargs.iteritems():
        request_params[k] = v

    return request_params


@pytest.mark.smoke
class TestPaystepCreateInvoice(TestCaseApiAppBase):
    BASE_API = '/v1/paystep/create-invoice'

    def check_invoice_params_consistence(self, invoice, request_params):
        request = self.test_session.query(mapper.Request).getone(request_params['request_id'])
        person = self.test_session.query(mapper.Person).getone(request_params['person_id'])
        hm.assert_that(invoice, hm.has_properties(
            request_id=request.id,
            client_id=request.client.id,
            person_id=person.id,
            firm_id=request.firm_id,
            iso_currency='RUB',
            paysys=hm.has_properties(
                group_id=cst.PaysysGroupIDs.default,
                payment_method_id=cst.PaymentMethodIDs.bank
            )
        ))

    @pytest.mark.parametrize(
        'param_name, param_value, description_pattern',
        [
            ('request_id', ob.RequestBuilder(), 'Request with ID %s not found in DB'),
            ('person_id', ob.PersonBuilder(), 'Person with ID %s not found in DB'),
            ('contract_id', ob.ContractBuilder(), 'Contract id=%s not found'),
            ('payment_method_id', 1, 'Object not found: PaymentMethod: primary keys: (%s,)'),
        ]
    )
    def test_object_id_not_found(self, param_name, param_value, description_pattern):
        if issubclass(type(param_value), ob.ObjectBuilder):
            request_params = prepare_request_params(**{param_name: not_existing_id(type(param_value))})
            error = getattr(type(param_value)._class, 'not_found_exception', exc.NOT_FOUND).__name__
        else:
            request_params = prepare_request_params(**{param_name: param_value})
            error = 'NOT_FOUND'
        res = self.test_client.secure_post_json(self.BASE_API, request_params)
        hm.assert_that(res.status_code, hm.equal_to(http.NOT_FOUND))
        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': error,
                'description': description_pattern % request_params[param_name],
            }),
        )

    def test_no_payment_method(self):
        request_params = prepare_request_params(firm_id=10000)
        res = self.test_client.secure_post_json(self.BASE_API, request_params)
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))
        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'PAYSTEP_INVALID_PAYMENT_CHOICE',
            }),
        )

    def test_ok(self, client):
        request_params = prepare_request_params(client)
        res = self.test_client.secure_post_json(self.BASE_API, request_params)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data', {})
        hm.assert_that(
            data,
            hm.has_entries({
                'id': hm.not_none(),
                'payment_term_dt': None,
                'postpay': 0,
                'credit': 0,
                'external_id': u'Б-%s-1' % request_params['request_id'],
                'overdraft': False,
                'manager': hm.has_entries({
                    'manager_code': hm.not_none(),
                }),
                'receipt_email': None,
                'client': hm.has_entries({'id': client.id, 'name': client.name, 'is_agency': False}),
                'person': hm.has_entries({'id': request_params['person_id']}),
                'payment_purpose': u'Б-%s-1' % request_params['request_id'],
                'is_for_single_account': False,
                'dt': hm.not_none(),
                'iso_currency': 'RUB',
                'type': 'prepayment',
                'money_product': False,
                'paysys': hm.has_entries({'id': 1001, 'cc': 'ph'}),
                'contract': None,
                'firm_id': 1,
                'need_receipt': False,
                'for_direct_client': False,
            }),
        )

        invoice = self.test_session.query(mapper.Invoice).getone(data['id'])
        self.check_invoice_params_consistence(invoice, request_params)

    @mock.patch('yb_snout_api.utils.plugins.get_geolookup', return_value=Geolookup())
    @pytest.mark.parametrize('param_passed', [None, False, True])
    def test_endbuyer_id(self, _mock_geolookup, param_passed):
        if param_passed is None:
            request_params = prepare_request_params(endbuyer_id=None)
        elif param_passed:
            request_params = prepare_request_params(endbuyer_id=ob.PersonBuilder.construct(self.test_session).id)
        else:
            request_params = prepare_request_params()
        res = self.test_client.secure_post_json(self.BASE_API, request_params)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        hm.assert_that(res.get_json().get('data', {}), hm.has_item('id'))

        invoice = self.test_session.query(mapper.Invoice).getone(res.get_json()['data']['id'])
        self.check_invoice_params_consistence(invoice, request_params)
        if not param_passed:
            hm.assert_that(invoice.endbuyer_id, hm.equal_to(None))
        elif param_passed:
            hm.assert_that(invoice.endbuyer_id, hm.equal_to(request_params['endbuyer_id']))

    @pytest.mark.parametrize('param_passed', [None, False, True])
    def test_receipt_email(self, param_passed):
        if param_passed is None:
            request_params = prepare_request_params(receipt_email=None)
        elif param_passed:
            request_params = prepare_request_params(receipt_email='test@test.test')
        else:
            request_params = prepare_request_params()
        res = self.test_client.secure_post_json(self.BASE_API, request_params)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        hm.assert_that(res.get_json().get('data', {}), hm.has_item('id'))

        invoice = self.test_session.query(mapper.Invoice).getone(res.get_json()['data']['id'])
        self.check_invoice_params_consistence(invoice, request_params)
        if not param_passed:
            hm.assert_that(invoice.receipt_email, hm.equal_to(None))
        elif param_passed:
            hm.assert_that(invoice.receipt_email, hm.equal_to(request_params['receipt_email']))

    @pytest.mark.parametrize('param_passed', [None, False, True])
    def test_region_id(self, param_passed):
        if param_passed is None:
            request_params = prepare_request_params(region_id=None)
        elif param_passed:
            request_params = prepare_request_params(region_id=1)
        else:
            request_params = prepare_request_params()
        res = self.test_client.secure_post_json(self.BASE_API, request_params)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        hm.assert_that(res.get_json().get('data', {}), hm.has_item('id'))

        invoice = self.test_session.query(mapper.Invoice).getone(res.get_json()['data']['id'])
        self.check_invoice_params_consistence(invoice, request_params)
        if not param_passed:
            hm.assert_that(invoice.person.region, hm.equal_to(None))
        elif param_passed:
            hm.assert_that(invoice.person.region, hm.equal_to(request_params['region_id']))

    def test_priority_promocode(self, client):
        legacy_promocode_1 = create_legacy_promocode(
            calc_params={'adjust_quantity': True, 'discount_pct': '66', 'minimal_qty': 11},
            minimal_amounts={'RUB': '6.66'},
            apply_on_create=True,
        )
        legacy_promocode_2 = create_legacy_promocode(
            calc_params={'adjust_quantity': True, 'discount_pct': '66', 'minimal_qty': 11},
            minimal_amounts={'RUB': '6.66'},
            apply_on_create=True,
        )

        promo_actions.reserve_promo_code(client, legacy_promocode_1)
        promo_actions.reserve_promo_code(client, legacy_promocode_2)

        person = create_person(client)
        request = create_request(client=client, firm_id=cst.FirmId.YANDEX_OOO)

        request_params = {
            'request_id': request.id,
            'person_id': person.id,
            'firm_id': cst.FirmId.YANDEX_OOO,
            'iso_currency': 'RUB',
            'payment_method_id': cst.PaymentMethodIDs.credit_card,
            'paysys_group_id': cst.PaysysGroupIDs.default,
            'promocode': legacy_promocode_2.code,
        }
        res = self.test_client.secure_post_json(self.BASE_API, request_params)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(res.get_json().get('data', {}), hm.has_item('id'))
        invoice = self.test_session.query(mapper.Invoice).getone(res.get_json()['data']['id'])
        assert invoice.promo_code == legacy_promocode_2


@pytest.mark.smoke
@pytest.mark.permissions
class TestPaystepCreateInvoiceAccess(TestCaseApiAppBase):
    BASE_API = '/v1/paystep/create-invoice'

    @mock.patch('yb_snout_api.utils.plugins.get_geolookup', return_value=Geolookup())
    def test_client_owns_request(self, _mock_geolookup, client_role):
        request_params = prepare_request_params()
        request = self.test_session.query(mapper.Request).getone(request_params['request_id'])
        security.set_passport_client(request.client)
        security.set_roles([client_role])
        response = self.test_client.secure_post_json(self.BASE_API, request_params, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(http.OK))
        hm.assert_that(response.get_json().get('data', {}), hm.has_item('id'))

    def test_alien_invoice(self, client_role):
        security.set_roles([client_role])
        request_params = prepare_request_params()
        response = self.test_client.secure_post_json(self.BASE_API, request_params, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN))
        hm.assert_that(
            response.get_json(),
            hm.has_entries({
                'error': 'PERMISSION_DENIED',
                'description': 'User %s has no permission IssueInvoices.' % self.test_session.oper_id,
            }),
        )

    @pytest.mark.parametrize(
        'firm_id, status_code',
        [
            (None, http.OK),
            (cst.FirmId.YANDEX_OOO, http.OK),
            (cst.FirmId.MARKET, http.FORBIDDEN)
        ]
    )
    @mock.patch('yb_snout_api.utils.plugins.get_geolookup', return_value=Geolookup())
    def test_w_role(self, _mock_geolookup, firm_id, status_code):
        role = create_role((cst.PermissionCode.ISSUE_INVOICES, {cst.ConstraintTypes.firm_id: None}))
        roles = [
            (role, {cst.ConstraintTypes.firm_id: firm_id}),
        ]
        security.set_roles(roles)
        request_params = prepare_request_params()
        response = self.test_client.secure_post_json(self.BASE_API, request_params, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(status_code))

    @pytest.mark.parametrize(
        'is_allowed, status_code',
        [
            (False, http.FORBIDDEN),
            (True, http.OK),
        ]
    )
    @mock.patch('yb_snout_api.utils.plugins.get_geolookup', return_value=Geolookup())
    def test_w_client_role(self, _mock_geolookup, client, is_allowed, status_code):
        role_client = create_role_client(client if is_allowed else None)
        role = create_role(
            (
                cst.PermissionCode.ISSUE_INVOICES,
                {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
            ),
        )
        roles = [
            (role, {cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO,
                    cst.ConstraintTypes.client_batch_id: role_client.client_batch_id}),
        ]
        security.set_roles(roles)

        request_params = prepare_request_params(client=client)
        response = self.test_client.secure_post_json(self.BASE_API, request_params, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(status_code))

    def test_wo_role(self, admin_role):
        security.set_roles([admin_role])
        request_params = prepare_request_params()
        response = self.test_client.secure_post_json(self.BASE_API, request_params, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN))
