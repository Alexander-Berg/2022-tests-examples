# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import hamcrest as hm
import pytest

from balance import constants as cst, mapper
from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_role, create_support_role, create_passport

PASSPORT_LOGIN = 'freedom_%s' % ob.get_big_number()
CLIENT_DATA = {
    'by_completion': True,
    'client_type': 'PH',
    'iso_currency_payment': 'RUB',
    'deny_cc': False,
    'direct25': True,
    'email': 'stay_home_2020@freedom.com',
    'fax': '12AB34HN',
    'fraud_desc': 'Описание фрода',
    'fraud_flag': True,
    'fraud_type': 'SUSPICIUOS_OPERATIONS',
    'fullname': 'Свободу самоизолирующимся!',
    'intercompany': 'AZ35',
    'is_acquiring': True,
    'is_agency': True,
    'is_non_resident': True,
    'login': PASSPORT_LOGIN,
    'manual_suspect': True,
    'manual_suspect_comment': 'Заканчивался первый месяц карантина....',
    'name': 'Следы на луне',
    'only_manual_name_update': True,
    'phone': '+79998887788',
    'region_id': 225,  # Russia
    'reliable_cc_payer': True,
    'retpath': 'Retpath',
    'url': 'https://www.kinopoisk.ru/',
}


@pytest.fixture
def passport():
    return create_passport(login=PASSPORT_LOGIN)


@pytest.fixture(name='additional_role')
def create_additional_role():
    return create_role(cst.PermissionCode.ADDITIONAL_FUNCTIONS)


@pytest.fixture(name='create_client_role')
def create_create_client_role():
    return create_role((cst.PermissionCode.CREATE_CLIENT, {cst.ConstraintTypes.client_batch_id: None}))


@pytest.fixture(name='edit_client_role')
def create_edit_client_role():
    return create_role((cst.PermissionCode.EDIT_CLIENTS, {cst.ConstraintTypes.client_batch_id: None}))


@pytest.mark.smoke
class TestCaseSetClient(TestCaseApiAppBase):
    BASE_API = '/v1/client/set-client'

    def test_create_new_client_w_enums(self):
        res = self.test_client.secure_post(self.BASE_API, {
            'name': 'test 1234',
            'client_type_id': 0,
            'by_completion': 'false',
            'fraud_flag': 'true',
            'printable_docs_type': 'ORDINARY',
            'domain_check_status': 'NONE',
            'fraud_type': 'FRS_VISA',
        })
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data', {})
        client_id = data.get('id')
        client = self.test_session.query(mapper.Client).getone(client_id)
        assert client.name == 'test 1234'
        assert client.printable_docs_type == 0
        assert client.domain_check_status == 0
        assert client.check_fraud_status() == 0
        assert client.fraud_status.fraud_flag == 1
        assert client.fraud_status.fraud_flag_type == 'FRS_VISA'

    def test_create_new_client(self, passport):
        res = self.test_client.secure_post(self.BASE_API, CLIENT_DATA)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data', {})
        client_id = data.get('id')
        client = self.test_session.query(mapper.Client).getone(client_id)

        assert passport.client == client
        hm.assert_that(
            client,
            hm.has_properties({
                'client_type_id': 0,  # PH
                'name': CLIENT_DATA['name'],
                'email': CLIENT_DATA['email'],
                'phone': CLIENT_DATA['phone'],
                'fax': CLIENT_DATA['fax'],
                'url': CLIENT_DATA['url'],
                'region_id': CLIENT_DATA['region_id'],
            }),
        )
        hm.assert_that(
            data,
            hm.has_entries({
                'id': client_id,
                'client_type_id': 0,  # PH
                'name': CLIENT_DATA['name'],
                'email': CLIENT_DATA['email'],
                'phone': CLIENT_DATA['phone'],
                'fax': CLIENT_DATA['fax'],
                'url': CLIENT_DATA['url'],
                'city': None,
                'is_agency': True,
                'full_repayment': False,
                'overdraft_ban': False,
                'manual_suspect': 1,
                'reliable_cc_payer': 1,
                'deny_cc': 0,
                'is_ctype_3': False,
                'parent_agencies': hm.empty(),
            }),
        )

    def test_update_client(self, client):
        client.email = 'https://www.youtube.com/watch?v=TTyaB41BIUk'
        new_email = 'https://www.youtube.com/watch?v=BOUTfUmI8vs'
        self.test_session.flush()

        res = self.test_client.secure_post(self.BASE_API, {'client_id': client.id, 'email': new_email})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        self.test_session.refresh(client)
        hm.assert_that(client.email, hm.equal_to(new_email))

    def test_delete_intercompany(self, client):
        client.intercompany = 'AZ35'
        self.test_session.flush()

        res = self.test_client.secure_post(
            self.BASE_API,
            {'client_id': client.id, 'intercompany': ''},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        self.test_session.refresh(client)
        assert client.intercompany is None

    def test_delete_region_id(self, client):
        client.region_id = 225  # Россия
        self.test_session.flush()

        res = self.test_client.secure_post(
            self.BASE_API,
            {'client_id': client.id, 'region_id': ''},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        self.test_session.refresh(client)
        assert client.region_id == 225  # в старом интерфейсе тоже так

    def test_delete_fullname(self, client):
        client.fullname = 'Hello world!'
        self.test_session.flush()

        res = self.test_client.secure_post(
            self.BASE_API,
            {'client_id': client.id, 'fullname': ''},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        self.test_session.refresh(client)
        assert client.fullname is None

    @pytest.mark.parametrize(
        'req_cur, res_cur, res_iso_cur',
        [
            pytest.param('USD', 'USD', 'USD', id='uds'),
            pytest.param('RUB', 'RUR', 'RUB', id='rur'),
            pytest.param('KZT', 'kzt', 'KZT', id='kzt'),
            pytest.param('', None, None, id='delete'),
        ],
    )
    def test_update_currency(self, client, req_cur, res_cur, res_iso_cur):
        client.currency_payment = 'EUR'
        client.iso_currency_payment = 'EUR'
        self.test_session.flush()

        res = self.test_client.secure_post(
            self.BASE_API,
            {'client_id': client.id, 'iso_currency_payment': req_cur},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        self.test_session.refresh(client)
        assert client.currency_payment == res_cur
        assert client.iso_currency_payment == res_iso_cur

    def test_invalid_currency(self, client):
        res = self.test_client.secure_post(
            self.BASE_API,
            {'client_id': client.id, 'iso_currency_payment': 'ABC'},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.INTERNAL_SERVER_ERROR))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'INVALID_PARAM',
                'description': 'Invalid parameter for function: Invalid iso_currency',
            }),
        )

    @pytest.mark.parametrize(
        'w_client_id',
        [True, False],
    )
    def test_required_in_data(self, client, w_client_id):
        params = {'client_id': client.id} if w_client_id else {}

        res = self.test_client.secure_post(self.BASE_API, params)
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    '?': hm.contains(hm.has_entries({'error': 'REQUIRED_FIELD_VALIDATION_ERROR'})),
                }),
            }),
        )

    def test_invalid_fields(self):
        res = self.test_client.secure_post(
            self.BASE_API,
            {
                'client_id': 0,
                'region_id': 'Aaaa',
                'direct25': 'B',
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'FORM_VALIDATION_ERROR',
                'description': 'Form validation error.',
                'form_errors': hm.has_entries({
                    'client_id': hm.contains(hm.has_entries({'error': 'POSITIVE_FIELD_VALIDATION_ERROR'})),
                    'direct25': hm.contains(hm.has_entries({'error': 'BOOLEAN_INVALID_FIELD_VALIDATION_ERROR'})),
                    'region_id': hm.contains(hm.has_entries({'error': 'INTEGER_INVALID_FIELD_VALIDATION_ERROR'})),
                }),
            }),
        )

    def test_unicode_name(self, client):
        res = self.test_client.secure_post(
            self.BASE_API,
            {'client_id': client.id, 'name': 'Ванёк'},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        self.test_session.refresh(client)
        assert client.name == 'Ванёк'

    def test_unicode_login(self):
        data = CLIENT_DATA.copy()
        data['login'] = 'превед медвед'

        res = self.test_client.secure_post(self.BASE_API, data)

        hm.assert_that(res.status_code, hm.equal_to(http.NOT_FOUND))
        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'PASSPORT_NOT_FOUND',
                'description': 'Passport with ID -1 not found in DB',
            }),
        )

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'w_role',
        [True, False],
    )
    def test_create_permission(self, passport, support_role, additional_role, create_client_role, w_role):
        roles = [support_role, additional_role]
        if w_role:
            roles.append(create_client_role)
        security.set_roles(roles)

        res = self.test_client.secure_post(self.BASE_API, CLIENT_DATA)
        hm.assert_that(res.status_code, hm.equal_to(http.OK if w_role else http.FORBIDDEN))

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'match_client',
        [None, False, True],
    )
    def test_edit_permission(self, client, support_role, additional_role, edit_client_role, match_client):
        roles = [support_role, additional_role]
        if match_client is not None:
            client_batch_id = create_role_client(client=client if match_client else None).client_batch_id
            roles.append((edit_client_role, {cst.ConstraintTypes.client_batch_id: client_batch_id}))
        security.set_roles(roles)

        res = self.test_client.secure_post(self.BASE_API, {'client_id': client.id, 'email': 'test@ya.ru'})
        hm.assert_that(res.status_code, hm.equal_to(http.OK if match_client else http.FORBIDDEN))

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'w_role',
        [True, False],
    )
    def test_enqueue_overdraft(self, client, support_role, additional_role, edit_client_role, w_role):
        session = self.test_session

        roles = [support_role, edit_client_role]
        if w_role:
            roles.append(additional_role)
        security.set_roles(roles)

        client.force_contractless_invoice = 0
        client.deny_overdraft = 0
        session.flush()

        res = self.test_client.secure_post(
            self.BASE_API,
            {
                'client_id': client.id,
                'force_contractless_invoice': True,
                'deny_overdraft': True,
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        session.refresh(client)
        assert client.force_contractless_invoice == (1 if w_role else 0)
        assert client.deny_overdraft == (1 if w_role else 0)
