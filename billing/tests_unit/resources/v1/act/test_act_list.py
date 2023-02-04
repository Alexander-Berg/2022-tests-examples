# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

from decimal import Decimal as D
import http.client as http
from flask.helpers import url_quote
import pytest
from hamcrest import (
    assert_that,
    contains,
    contains_inanyorder,
    contains_string,
    equal_to,
    empty,
    has_entries,
    has_entry,
    has_item,
    has_items,
    not_,
)

from balance import constants as cst, mapper
from tests import object_builder as ob

from brest.core.tests import security
from brest.core.tests import utils as test_utils
from yb_snout_api.resources import enums
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.firm import create_firm
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.act import create_act, create_client_acts
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, get_client_role, create_role


@pytest.fixture(name='view_inv_role')
def create_view_inv_role():
    return create_role(
        (
            cst.PermissionCode.VIEW_INVOICES,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.mark.smoke
@pytest.mark.slow
class TestCaseActList(TestCaseApiAppBase):
    BASE_API = '/v1/act/list'

    def test_get_act_list(self, client_acts):
        client_id, acts = client_acts
        acts_ids = sorted([act.id for act in acts])

        response = self.test_client.get(self.BASE_API, {'client_id': client_id})
        assert_that(response.status_code, equal_to(http.OK), 'Response code should be OK')

        items = response.get_json()['data']['items']

        assert_that(
            items,
            contains_inanyorder(*[has_entry('act_id', act_id) for act_id in acts_ids]),
            'Couldn\'t find all acts',
        )

    def test_by_external_id(self, client_acts):
        client_id, acts = client_acts

        response = self.test_client.get(self.BASE_API, {'act_eid': acts[0].external_id})
        assert_that(response.status_code, equal_to(http.OK), 'Response code should be OK')

        items = response.get_json()['data']

        assert_that(
            items,
            has_entries({
                'items': contains(has_entry('act_id', acts[0].id)),
                'total_row_count': 1,
            }),
        )

    def test_by_subclient_login(self):
        client_id1, acts1 = create_client_acts()
        client_id2, acts2 = create_client_acts()
        passport = acts1[0].invoice.person.passport
        passport.client_id = client_id1
        self.test_session.flush()
        response = self.test_client.get(self.BASE_API, {'subclient_login': passport.login})
        assert_that(response.status_code, equal_to(http.OK), 'Response code should be OK')

        items = response.get_json()['data']

        # acts2 are filtered
        assert_that(
            items,
            has_entries({
                'items': contains_inanyorder(*[
                    has_entry('act_id', act.id)
                    for act in acts1
                ]),
                'total_row_count': 3,
            }),
        )

    def test_act_list_currency_filter(self, client_acts):
        client_id, acts = client_acts
        act_id = acts[0].id
        act_iso_currency_code = acts[0].invoice.iso_currency

        params = {
            'client_id': client_id,
            'currency_code': act_iso_currency_code,
        }

        # Ищем акты по id клиента и iso коду вылюты
        response = self.test_client.get(self.BASE_API, params)

        assert_that(response.status_code, equal_to(http.OK), 'Response code should be OK')
        items = response.get_json()['data']['items']
        assert_that(
            items,
            has_items(has_entry('act_id', act_id)),
            'Couldn\'t find test act',
        )

        differ_iso_currency_code = (
            test_utils.get_test_session()
                .query(mapper.Currency)
                .filter(mapper.Currency.iso_code != act_iso_currency_code)
                .first().iso_code
        )
        params['currency_code'] = differ_iso_currency_code

        # Ищем акты по id клиента с не верным iso кодом вылюты
        response = self.test_client.get(self.BASE_API, params)

        assert_that(response.status_code, equal_to(http.OK), 'Response code should be OK')
        items = response.get_json()['data']['items']
        assert_that(
            items,
            not_(has_items(has_entry('act_id', act_id))),
            'Shouldn\'t find test act',
        )

    def test_acts_totals(self, client_acts):
        """Проверяем расчет итоговых значений.
        Если получаем все данные одним запросом, то итоговые значения для страницы и всех данных должны быть равны.
        Если получаем страницу меньшего размера, то проверяем отдельно
        итоговые значения для страницы и для всего набора данных.
        """
        client_id, client_acts = client_acts

        all_acts_response = self.test_client.get(
            self.BASE_API,
            {
                'client_id': client_id,
                'show_totals': True,
                'pagination_ps': len(client_acts),
            },
        )
        assert_that(all_acts_response.status_code, equal_to(http.OK), 'Response code should be OK')

        one_act_response = self.test_client.get(
            self.BASE_API,
            {
                'client_id': client_id,
                'show_totals': True,
                'pagination_ps': 1,
            },
        )
        assert_that(one_act_response.status_code, equal_to(http.OK), 'Response code should be OK')

        one_act_data = one_act_response.get_json().get('data')
        one_act_data_items = one_act_data['items']

        all_acts_data = all_acts_response.get_json().get('data')
        all_acts_items = all_acts_data['items']

        expected_one_act_totals = {
            'amount': str(D(one_act_data_items[0]['amount'])),
            'amount_nds': str(D(one_act_data_items[0]['amount_nds'])),
            'invoice_amount': str(D(one_act_data_items[0]['invoice_amount'])),
            'nds_pct': str(D(one_act_data_items[0]['nds_pct'])),
            'paid_amount': str(D(one_act_data_items[0]['paid_amount'])),
            'row_count': 1,
        }

        expected_gtobals = {
            'amount': str(sum(D(a['amount']) for a in all_acts_items)),
            'amount_nds': str(sum(D(a['amount_nds']) for a in all_acts_items)),
            'invoice_amount': str(sum(D(a['invoice_amount']) for a in all_acts_items)),
            'nds_pct': str(sum(D(a['nds_pct']) for a in all_acts_items)),
            'paid_amount': str(sum(D(a['paid_amount']) for a in all_acts_items)),
            'row_count': len(all_acts_items),
        }

        assert_that(
            all_acts_data['gtotals'],
            has_entries(expected_gtobals),
            'Incorrect gtotals for all acts request!',
        )

        assert_that(
            all_acts_data['totals'],
            has_entries(expected_gtobals),
            'Incorrect totals for all acts request!',
        )

        assert_that(
            one_act_data['gtotals'],
            has_entries(expected_gtobals),
            'Incorrect gtotals for one act request!',
        )

        assert_that(
            one_act_data['totals'],
            has_entries(expected_one_act_totals),
            'Incorrect totals for one act request!',
        )

    def test_acts_sorting(self, client_acts):
        client_id, _ = client_acts

        params = {
            'sort_key': 'INVOICE_EID',
            'sort_order': 'ASC',
            'client_id': client_id,
        }

        response = self.test_client.get(self.BASE_API, params)
        assert_that(response.status_code, equal_to(http.OK), 'Response code should be OK')

        items = response.get_json().get('data')['items']
        asc_sorted_items = sorted(items, key=lambda x: x['invoice_eid'])

        # Проверяем, что результат упорядочен по возрастанию
        assert_that(items, contains(*[has_entries(item) for item in asc_sorted_items]), 'Incorrect order')

        params['sort_order'] = 'DESC'
        url = '{}?{}'.format(self.BASE_API, params)

        response = self.test_client.get(url)
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        items = response.get_json().get('data')['items']
        desc_sorted_items = sorted(items, key=lambda x: x['invoice_eid'], reverse=True)

        # Проверяем, что результат упорядочен по убыванию
        assert_that(items, contains(*[has_entries(item) for item in desc_sorted_items]), 'Incorrect order')


@pytest.mark.smoke
@pytest.mark.slow
@pytest.mark.permissions
class TestActListPermissions(TestCaseApiAppBase):
    BASE_API = '/v1/act/list'
    role_map = {
        'client': lambda: get_client_role(),
        'view_invoices': lambda: create_role((cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: None})),
    }

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
        """Фильтр актов по фирме указанной в роли"""
        session = self.test_session
        role = ob.create_role(session, (cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: None}))
        acts = []
        required_acts = []

        if not role_firm_ids:
            required_acts = acts  # нет ограничения по фирме

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
            act = create_act(firm_id=firm_id, client=client)
            acts.append(act)
            if firm_id in role_firm_ids:
                required_acts.append(act)
        session.flush()

        response = self.test_client.get(self.BASE_API, {'client_id': client.id})
        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        assert_that(data['total_row_count'], equal_to(len(required_acts)))
        assert_that(
            data['items'],
            contains_inanyorder(*[
                has_entries({'act_id': req_act.id, 'firm_id': req_act.invoice.firm_id})
                for req_act in required_acts
            ]),
        )

    @pytest.mark.parametrize(
        'match_client',
        [True, False],
    )
    def test_client_constraint(self, admin_role, view_inv_role, client, match_client):
        """Фильтр актов по группе клиентов указанной в роли"""
        client_batch_id = create_role_client(client=client if match_client else None).client_batch_id
        roles = [
            admin_role,
            (view_inv_role, {cst.ConstraintTypes.client_batch_id: client_batch_id}),
        ]
        security.set_roles(roles)

        act = create_act(client=client)
        response = self.test_client.get(self.BASE_API, {'client_id': act.client_id})
        assert_that(response.status_code, equal_to(http.OK))

        data = response.get_json().get('data')
        res_match = contains(has_entry('act_id', act.id)) if match_client else empty()
        assert_that(data['items'], res_match)

    @pytest.mark.parametrize(
        'role_builders',
        [
            [role_map['client']],
            [role_map['client'], role_map['view_invoices']],  # даже с админскими правами нельзя получить не свой счёт

        ],
    )
    def test_client_owns_act(self, client_role, role_builders):
        """Получаем только акты, которые принадлежат клиенту из паспорта"""
        session = self.test_session
        firm_id = cst.FirmId.YANDEX_OOO
        from_dt = session.now()

        client1 = create_client()
        client2 = create_client()

        roles = [builder() for builder in role_builders]
        security.set_passport_client(client1)
        security.set_roles(roles)

        act1 = create_act(firm_id=firm_id, client=client1)
        create_act(firm_id=firm_id, client=client2)
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
            [has_entries({'act_id': act1.id, 'client_id': client1.id})],
        )

    def test_client_ui_admin_user(self, client, admin_role):
        """В КИ ищем счета под админом. Да, так можно."""
        session = self.test_session
        role = ob.create_role(session, (cst.PermissionCode.VIEW_INVOICES, {cst.ConstraintTypes.firm_id: None}))
        roles = [
            admin_role,
            (role, {cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO}),
        ]
        security.set_roles(roles)

        from_dt = session.now()
        act1 = create_act(client=client, firm_id=cst.FirmId.YANDEX_OOO)
        act2 = create_act(client=client, firm_id=cst.FirmId.CLOUD)
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
        assert_that(data['items'], has_item(has_entry('act_id', act1.id)))
        assert_that(data['items'], not_(has_item(has_entry('act_id', act2.id))))


@pytest.mark.smoke
class TestCaseXLSActList(TestCaseApiAppBase):
    BASE_API = '/v1/act/list/xls'

    @pytest.mark.parametrize(
        'test_fname, expected_fname_in_context',
        [
            ('тестовое_имя_файла.xls', 'filename*=UTF-8\'\'' + url_quote('тестовое_имя_файла.xls')),
            ('тестовое_имя_файла', 'filename*=UTF-8\'\'' + url_quote('тестовое_имя_файла.xls')),
            ('test_name.xls', 'filename=test_name.xls'),
            ('test_name', 'filename=test_name.xls'),
        ],
    )
    def test_load_xls_file(self, client_acts, test_fname, expected_fname_in_context):
        client_id, _ = client_acts

        params = {
            'client_id': client_id,
            'filename': test_fname.encode('utf-8'),
        }

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
