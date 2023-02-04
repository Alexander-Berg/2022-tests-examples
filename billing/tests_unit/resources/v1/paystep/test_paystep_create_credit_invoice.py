# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import hamcrest as hm
import http.client as http

from balance import mapper, constants as cst, exc
from muzzle.security import sauth
from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
from yb_snout_api.tests_unit.fixtures.contract import create_credit_contract
from yb_snout_api.tests_unit.fixtures.person import create_person
from yb_snout_api.tests_unit.fixtures.request import create_request
from yb_snout_api.tests_unit.fixtures.common import not_existing_id
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role, get_client_role


@pytest.fixture(name='issue_inv_role')
def create_issue_inv_role():
    return create_role(
        (
            cst.PermissionCode.ISSUE_INVOICES,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


def prepare_request_params(client=None, person=None, **kwargs):
    client = client or create_client()
    person = person or create_person(client=client)
    contract = create_credit_contract(client=client, person=person, credit_limit_single=1000000)
    request = create_request(client=contract.client, firm_id=contract.col0.firm)
    request_params = {
        'request_id': request.id,
        'person_id': contract.person.id,
        'contract_id': contract.id,
        'firm_id': request.firm_id,
        'iso_currency': 'RUB',
        'payment_method_id': cst.PaymentMethodIDs.bank,
        'paysys_group_id': cst.PaysysGroupIDs.default
    }
    for k, v in kwargs.iteritems():
        request_params[k] = v

    return request_params


@pytest.mark.smoke
class TestPaystepCreateCreditInvoice(TestCaseApiAppBase):
    BASE_API = '/v1/paystep/create-credit-invoice'

    def get_cookies(self, w_code=False):
        cookies = {
            'Session_id': 'ABC',
            'sessionid2': 'DEF',
            'yandexuid': 'XYZ',
        }
        if w_code:
            cookies[cst.SmsCookieName.paystep] = sauth.get_secret_key(
                self.test_session.oper_id,
                cookies['Session_id'],
                cookies['sessionid2'],
                cookies['yandexuid'],
            )
        return cookies

    def check_invoice_params_consistence(self, invoice, request_params):
        request = self.test_session.query(mapper.Request).getone(request_params['request_id'])
        contract = self.test_session.query(mapper.Contract).getone(request_params['contract_id'])
        hm.assert_that(invoice, hm.has_properties(
            request_id=hm.any_of(None, request.id),
            client_id=request.client.id,
            person_id=contract.person.id,
            contract_id=contract.id,
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

    def test_personal_account(self, client):
        request_params = prepare_request_params(client)
        contract = self.test_session.query(mapper.Contract).getone(request_params['contract_id'])
        contract.col0.personal_account_fictive = 0
        self.test_session.flush()
        res = self.test_client.secure_post_json(self.BASE_API, request_params)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data', {})
        hm.assert_that(
            data,
            hm.has_entries({
                'id': hm.not_none(),
                'payment_term_dt': None,
                'postpay': 1,
                'credit': 0,
                'external_id': hm.starts_with(u'ЛС'),
                'overdraft': False,
                'manager': hm.has_entries({
                    'manager_code': hm.not_none(),
                }),
                'receipt_email': None,
                'client': hm.has_entries({'id': client.id, 'name': client.name, 'is_agency': False}),
                'person': hm.has_entries({'id': request_params['person_id']}),
                'payment_purpose': hm.starts_with(u'ЛС'),
                'is_for_single_account': False,
                'dt': hm.not_none(),
                'iso_currency': 'RUB',
                'type': 'personal_account',
                'money_product': False,
            }),
        )

        invoice = self.test_session.query(mapper.PersonalAccount).getone(data['id'])
        hm.assert_that(len(invoice.consumes), hm.equal_to(1))
        self.check_invoice_params_consistence(invoice, request_params)

    def test_personal_account_fictive(self):
        request_params = prepare_request_params()
        res = self.test_client.secure_post_json(self.BASE_API, request_params)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data', {})
        hm.assert_that(
            data,
            hm.has_entries({
                'id': hm.not_none(),
                'payment_term_dt': None,
                'postpay': 1,
                'credit': 0,
                'external_id': hm.starts_with(u'ЛС'),
                'overdraft': False,
                'manager': hm.has_entries({
                    'manager_code': hm.not_none(),
                }),
                'receipt_email': None,
                'person': hm.has_entries({'id': request_params['person_id']}),
                'payment_purpose': hm.starts_with(u'ЛС'),
                'is_for_single_account': False,
                'dt': hm.not_none(),
                'iso_currency': 'RUB',
                'type': 'fictive_personal_account',
                'money_product': False,
                'paysys': hm.has_entries({'id': 1001, 'cc': 'ph'}),
                'contract': hm.has_entries({'id': request_params['contract_id']}),
                'firm_id': 1,
                'need_receipt': False,
            }),
        )

        invoice = self.test_session.query(mapper.FictivePersonalAccount).getone(data['id'])
        hm.assert_that(len(invoice.consumes), hm.equal_to(1))
        self.check_invoice_params_consistence(invoice, request_params)

    def test_fictive(self):
        request_params = prepare_request_params()
        contract = self.test_session.query(mapper.Contract).getone(request_params['contract_id'])
        contract.col0.personal_account = 0
        contract.col0.personal_account_fictive = 0
        self.test_session.flush()
        res = self.test_client.secure_post_json(self.BASE_API, request_params)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        hm.assert_that(res.get_json().get('data', {}), hm.contains('client_id'))

        invoice = self.test_session.query(mapper.FictiveInvoice).filter_by(
            client_id=res.get_json()['data']['client_id']
        ).one()
        hm.assert_that(len(invoice.consumes), hm.equal_to(1))
        self.check_invoice_params_consistence(invoice, request_params)

    @pytest.mark.parametrize(
        'w_code',
        [False, True],
    )
    def test_ur_w_code(self, client, w_code):
        request_params = prepare_request_params(client)
        person = self.test_session.query(mapper.Person).getone(request_params['person_id'])
        category = self.test_session.query(mapper.PersonCategory).getone(category='ur')
        person.person_category = category
        self.test_session.flush()

        self.test_session.config.__dict__['SAUTH_REQUIRED_REGIONS'] = 1
        cookies = self.get_cookies(w_code=w_code)

        security.set_roles([])
        security.set_passport_client(client)

        res = self.test_client.secure_post_json(
            self.BASE_API,
            request_params,
            cookies=cookies,
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if w_code else http.BAD_REQUEST))

        if w_code:
            data = res.get_json()['data']
            invoice = self.test_session.query(mapper.FictivePersonalAccount).getone(data['id'])
            self.check_invoice_params_consistence(invoice, request_params)

        else:
            hm.assert_that(
                res.get_json(),
                hm.has_entries({
                    'error': 'INVALID_OVERDRAFT_VERIFICATION_CODE',
                }),
            )

    @pytest.mark.parametrize('w_code', [True, False])
    def test_ph_w_code(self, client, client_role, w_code):
        request_params = prepare_request_params(client)
        self.test_session.config.__dict__['SAUTH_REQUIRED_REGIONS'] = 1
        cookies = self.get_cookies(w_code=w_code)

        security.set_roles([client_role])
        security.set_passport_client(client)

        res = self.test_client.secure_post_json(
            self.BASE_API,
            request_params,
            cookies=cookies,
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if w_code else http.BAD_REQUEST))

        if w_code:
            data = res.get_json()['data']
            invoice = self.test_session.query(mapper.FictivePersonalAccount).getone(data['id'])
            self.check_invoice_params_consistence(invoice, request_params)

        else:
            hm.assert_that(
                res.get_json(),
                hm.has_entries({
                    'error': 'INVALID_OVERDRAFT_VERIFICATION_CODE',
                }),
            )

    @pytest.mark.parametrize(
        'person_category, w_role, regions_config, ans',
        [
            pytest.param('ph', True, 1, True, id='ph_w_role'),
            pytest.param('ph', False, 1, False, id='ph_wo_role'),
            pytest.param('ur', True, 1, True, id='ur_w_role'),
            pytest.param('ur', False, 0, True, id='ur_wo_regions'),
            pytest.param('ur', False, 1, False, id='ur_w_regions'),
        ],
    )
    def test_skip_verification(self, client, issue_inv_role, person_category, w_role, regions_config, ans):
        request_params = prepare_request_params(client)
        person = self.test_session.query(mapper.Person).getone(request_params['person_id'])
        category = self.test_session.query(mapper.PersonCategory).getone(category=person_category)
        person.person_category = category
        self.test_session.flush()

        self.test_session.config.__dict__['SAUTH_REQUIRED_REGIONS'] = regions_config
        cookies = self.get_cookies(w_code=False)

        roles = [issue_inv_role] if w_role else []
        security.set_roles(roles)
        security.set_passport_client(client)

        res = self.test_client.secure_post_json(
            self.BASE_API,
            request_params,
            cookies=cookies,
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if ans else http.BAD_REQUEST))

    def test_flag(self, client, client_role):
        # как временное решение, пока фронт не добавят пробрасывание параметра,
        # юзаем флаг, чтобы не падать
        request_params = prepare_request_params(client)
        self.test_session.config.__dict__['SKIP_VERIFICATION_SMS_FOR_CREDIT_IN_SNOUT'] = 1

        security.set_roles([client_role])
        security.set_passport_client(client)

        res = self.test_client.secure_post_json(
            self.BASE_API,
            request_params,
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))


@pytest.mark.smoke
@pytest.mark.permissions
class TestPaystepCreateCreditInvoiceAccess(TestCaseApiAppBase):
    BASE_API = '/v1/paystep/create-credit-invoice'

    def test_client_owns_request(self, client_role):
        request_params = prepare_request_params()
        person = self.test_session.query(mapper.Person).getone(request_params['person_id'])
        category = self.test_session.query(mapper.PersonCategory).getone(category='ur')
        person.person_category = category
        self.test_session.flush()
        self.test_session.config.__dict__['SAUTH_REQUIRED_REGIONS'] = 0

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
    def test_w_role(self, firm_id, status_code):
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
    def test_w_client_role(self, client, is_allowed, status_code):
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

    @pytest.mark.parametrize('firm_id', [None, cst.FirmId.YANDEX_OOO, cst.FirmId.MARKET])
    def test_w_role_no_direct_limited(self, firm_id):
        role = create_role((cst.PermissionCode.ISSUE_INVOICES, {cst.ConstraintTypes.firm_id: None}))
        roles = [
            (role, {cst.ConstraintTypes.firm_id: firm_id}),
        ]
        security.set_roles(roles)
        request_params = prepare_request_params()
        security.update_limited_role(create_client())
        response = self.test_client.secure_post_json(self.BASE_API, request_params, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN))

    @pytest.mark.parametrize(
        'firm_id, status_code',
        [
            (None, http.OK),
            (cst.FirmId.YANDEX_OOO, http.OK),
            (cst.FirmId.MARKET, http.FORBIDDEN)
        ]
    )
    def test_w_role_direct_limited(self, firm_id, status_code):
        role = create_role((cst.PermissionCode.ISSUE_INVOICES, {cst.ConstraintTypes.firm_id: None}))
        roles = [
            (role, {cst.ConstraintTypes.firm_id: firm_id}),
        ]
        security.set_roles(roles)
        request_params = prepare_request_params()
        security.update_limited_role(
            self.test_session.query(mapper.Request).getone(request_params['request_id']).client
        )
        response = self.test_client.secure_post_json(self.BASE_API, request_params, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(status_code))

    def test_wo_role(self, admin_role):
        security.set_roles([admin_role])
        request_params = prepare_request_params()
        response = self.test_client.secure_post_json(self.BASE_API, request_params)
        hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN))
