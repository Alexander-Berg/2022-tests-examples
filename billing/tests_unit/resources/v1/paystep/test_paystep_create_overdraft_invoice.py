# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import datetime

import mock
import pytest
import hamcrest as hm
import http.client as http

from balance import mapper, constants as cst, exc
from muzzle.security import sauth
from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
from yb_snout_api.tests_unit.fixtures.person import create_person
from yb_snout_api.tests_unit.fixtures.request import create_request
from yb_snout_api.tests_unit.fixtures.common import not_existing_id
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role, get_client_role
from yb_snout_api.tests_unit.fixtures.passport_sms_api import create_overdraft_verification_code, DEFAULT_SMS_CODE


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
    client.set_currency(cst.ServiceId.DIRECT, 'RUB', datetime.datetime.now(), cst.CONVERT_TYPE_COPY)
    client.set_overdraft_limit(cst.ServiceId.DIRECT, cst.FirmId.YANDEX_OOO, 1000, 'RUB')
    person = person or create_person(client)
    request = create_request(client=client, firm_id=cst.FirmId.YANDEX_OOO)
    ov = create_overdraft_verification_code(request)
    request_params = {
        'request_id': request.id,
        'person_id': person.id,
        'firm_id': request.firm_id,
        'iso_currency': 'RUB',
        'payment_method_id': cst.PaymentMethodIDs.credit_card,
        'paysys_group_id': cst.PaysysGroupIDs.default,
        'sms_notify': cst.SMSNotifyChoice.ACCEPTED
    }
    for k, v in kwargs.iteritems():
        request_params[k] = v

    return request_params


@pytest.mark.smoke
class TestPaystepCreateOverdraftInvoice(TestCaseApiAppBase):
    BASE_API = '/v1/paystep/create-overdraft-invoice'

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
                payment_method_id=cst.PaymentMethodIDs.credit_card
            )
        ))

    def get_cookies(self, w_code=False):
        cookies = {
            'Session_id': 'XXX',
            'sessionid2': 'XXX2',
            'yandexuid': 'YYY',
        }
        if w_code:
            cookies[cst.SmsCookieName.paystep] = sauth.get_secret_key(
                self.test_session.oper_id,
                cookies['Session_id'],
                cookies['sessionid2'],
                cookies['yandexuid'],
            )
        return cookies


    @pytest.mark.parametrize(
        'param_name, param_value, description_pattern',
        [
            ('request_id', ob.RequestBuilder(), 'Request with ID %s not found in DB'),
            ('person_id', ob.PersonBuilder(), 'Person with ID %s not found in DB'),
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
            data = res.get_json().get('data', {})
            hm.assert_that(
                data,
                hm.has_entries({
                    'id': hm.not_none(),
                    'payment_term_dt': hm.not_none(),
                    'postpay': 0,
                    'credit': 0,
                    'external_id': hm.starts_with(u'Б-'),
                    'overdraft': True,
                    'manager': hm.has_entries({
                        'manager_code': hm.not_none(),
                    }),
                    'receipt_email': None,
                    'person': hm.has_entries({'id': request_params['person_id']}),
                    'payment_purpose': hm.starts_with(u'Б-'),
                    'is_for_single_account': False,
                    'dt': hm.not_none(),
                    'iso_currency': 'RUB',
                    'type': 'overdraft',
                    'money_product': False,
                    'paysys': hm.has_entries({'id': 1033, 'cc': 'cc_ur'}),
                    'contract': None,
                    'firm_id': 1,
                    'need_receipt': True,
                }),
            )

            invoice = self.test_session.query(mapper.OverdraftInvoice).getone(res.get_json()['data']['id'])
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
            data = res.get_json().get('data', {})
            hm.assert_that(
                data,
                hm.has_entries({
                    'id': hm.not_none(),
                    'payment_term_dt': hm.not_none(),
                    'postpay': 0,
                    'credit': 0,
                    'external_id': hm.starts_with(u'Б-'),
                    'overdraft': True,
                    'manager': hm.has_entries({
                        'manager_code': hm.not_none(),
                    }),
                    'receipt_email': None,
                    'person': hm.has_entries({'id': request_params['person_id']}),
                    'payment_purpose': hm.starts_with(u'Б-'),
                    'is_for_single_account': False,
                    'dt': hm.not_none(),
                    'iso_currency': 'RUB',
                    'type': 'overdraft',
                    'money_product': False,
                    'paysys': hm.has_entries({'id': 1002, 'cc': 'as'}),
                    'contract': None,
                    'firm_id': 1,
                    'need_receipt': True,
                }),
            )

            invoice = self.test_session.query(mapper.OverdraftInvoice).getone(res.get_json()['data']['id'])
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

    def test_w_verification_code(self, client):
        # поддержим логику, что можно без куки передать код и всё будет ОК
        request_params = prepare_request_params(client)
        request_params['verification_code'] = str(DEFAULT_SMS_CODE)

        security.set_roles([])
        security.set_passport_client(client)

        res = self.test_client.secure_post_json(
            self.BASE_API,
            request_params,
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        invoice = self.test_session.query(mapper.OverdraftInvoice).getone(res.get_json()['data']['id'])
        self.check_invoice_params_consistence(invoice, request_params)


@pytest.mark.smoke
@pytest.mark.permissions
class TestPaystepCreateOverdraftInvoiceAccess(TestCaseApiAppBase):
    BASE_API = '/v1/paystep/create-overdraft-invoice'

    @mock.patch('balance.actions.passport_sms_verification.OverdraftVerificator.check', return_value=True)
    def test_client_owns_request(self, _mock_check, client_role):
        request_params = prepare_request_params()
        request_params['verification_code'] = str(DEFAULT_SMS_CODE)
        request = self.test_session.query(mapper.Request).getone(request_params['request_id'])

        security.set_passport_client(request.client)
        security.set_roles([client_role])

        response = self.test_client.secure_post_json(self.BASE_API, request_params, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(http.OK))
        hm.assert_that(response.get_json().get('data', {}), hm.has_item('id'))

    @mock.patch('balance.actions.passport_sms_verification.OverdraftVerificator.check', return_value=True)
    def test_alien_invoice(self, _mock_check, client_role):
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
    @mock.patch('balance.actions.passport_sms_verification.OverdraftVerificator.check', return_value=True)
    def test_w_role(self, _mock_check, firm_id, status_code):
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
    @mock.patch('balance.actions.passport_sms_verification.OverdraftVerificator.check', return_value=True)
    def test_w_client_role(self, _mock_check, client, is_allowed, status_code):
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

    @mock.patch('balance.actions.passport_sms_verification.OverdraftVerificator.check', return_value=True)
    def test_wo_role(self, _mock_check, admin_role):
        security.set_roles([admin_role])
        request_params = prepare_request_params()
        response = self.test_client.secure_post_json(self.BASE_API, request_params)
        hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN))
