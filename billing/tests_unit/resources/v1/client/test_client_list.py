# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import datetime
import pytest
import hamcrest as hm
import http.client as http

from balance import constants as cst
from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.utils import clean_dict
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.resources.v1.client.enums import ClientOrAgency
from yb_snout_api.tests_unit.fixtures.resource import mock_client_resource
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import (
    create_admin_role,
    get_client_role,
    create_role,
    create_passport,
    create_view_client_role,
)
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import (
    create_client,
    create_client_with_intercompany,
    create_agency,
    create_role_client_group,
    create_manager,
)


@pytest.mark.smoke
class TestCaseClientList(TestCaseApiAppBase):
    BASE_API = '/v1/client/list'

    def test_get_by_id(self, client):
        response = self.test_client.get(self.BASE_API, {'client_id': client.id})

        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'Response code should be OK')

        data_items = response.get_json()['data']['items']
        hm.assert_that(
            data_items,
            hm.has_items(hm.has_entry('id', client.id)),
            'Not one unique client with corresponding id',
        )

    @pytest.mark.parametrize(
        'login',
        ['coronavirus_2019', 'коронавирус_2019'],
    )
    def test_get_by_login(self, login, client):
        passport = create_passport(login=login)
        passport.link_to_client(client)
        self.test_session.flush()

        res = self.test_client.get(
            self.BASE_API,
            {'login': login.replace('_', '-')},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data')['items']
        hm.assert_that(
            data,
            hm.contains(hm.has_entry('id', client.id)),
        )

    def test_search_options(self, client, agency):
        """Ищем клиента и агентство с именем начинающимся на 'test_client',
        в зависимости от значения флага agency_select_policy должны вернуться:
        ALL - клиент и агентство;
        CLIENT - только клиент;
        AGENCY - только агентство.
        """
        params = {'name': 'test_client', 'agency_select_policy': ClientOrAgency.ALL.name}
        # Возвращаются клиент и агентство
        response = self.test_client.get(
            self.BASE_API,
            params,
        )
        hm.assert_that(
            response.get_json()['data']['items'],
            hm.has_items(hm.has_entry('id', client.id), hm.has_entry('id', agency.id)),
            'The client and agency should be found',
        )

        # Возвращается только клиент
        params['agency_select_policy'] = ClientOrAgency.CLIENT.name
        response = self.test_client.get(self.BASE_API, params)
        hm.assert_that(
            response.get_json()['data']['items'],
            hm.has_items(hm.has_entry('id', client.id)),
            'Only the client should be found',
        )

        # Возвращается только агентство
        params['agency_select_policy'] = ClientOrAgency.AGENCY.name
        response = self.test_client.get(self.BASE_API, params)
        hm.assert_that(
            response.get_json()['data']['items'],
            hm.has_items(hm.has_entry('id', agency.id)),
            'Only the agency should be found',
        )

    def test_search_client_by_intercompany(self, client_with_intercompany):
        """Ищем клиента c параметром поиска - интеркомпания
        """
        params = {
            u"client_id": client_with_intercompany.id,
            u"intercompany": client_with_intercompany.intercompany,
        }
        response = self.test_client.get(self.BASE_API, params)

        # Должны найти одного клиента с соответствующей интеркомпанией
        hm.assert_that(
            response.get_json()['data']['items'],
            hm.contains(hm.has_entry(u"id", client_with_intercompany.id)),
            u"One client should be found",
        )

        params[u"intercompany"] = u"NOT_EXISTS"
        response = self.test_client.get(self.BASE_API, params)

        # Должен вернуться пустой список, интеркомпания не соответствует клиенту с указанным id
        hm.assert_that(
            response.get_json()['data']['items'],
            hm.empty(),
            u"No clients should be found",
        )

        params = {u"intercompany": u"NOT_EXISTS"}
        response = self.test_client.get(self.BASE_API, params)

        # Должен вернуться пустой список, нет клиентов с такой интеркомпанией
        hm.assert_that(
            response.get_json()['data']['items'],
            hm.empty(),
            u"No clients should be found",
        )

    def test_search_by_single_account_number(self):
        client = create_client(
            name='snout_test_%s' % ob.get_big_number(),
            single_account_number=ob.get_big_number(),
        )
        res = self.test_client.get(
            self.BASE_API,
            params={'single_account_number': client.single_account_number},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        data = res.get_json().get('data', {})
        hm.assert_that(
            data['items'],
            hm.contains(
                hm.has_entry('id', client.id),
            ),
        )

    @pytest.mark.parametrize(
        'reliable_cc_payer',
        [False, None, True],
    )
    def test_reliable_cc_payer(self, reliable_cc_payer):
        client_reliable = create_client(passport=create_passport(login='stay_home_%s' % ob.get_big_number()), reliable_cc_payer=1)
        client_not_reliable = create_client(passport=create_passport(login='stay_home_%s' % ob.get_big_number()), reliable_cc_payer=0)
        client_none = create_client(passport=create_passport(login='stay_home_%s' % ob.get_big_number()))
        client_none.reliable_cc_payer = None
        self.test_session.flush()

        res = self.test_client.get(
            self.BASE_API,
            clean_dict({
                'login': 'stay_home_',
                'reliable_client': reliable_cc_payer,
            }),
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        clients_match = []
        if reliable_cc_payer in (None, True):
            clients_match += [
                hm.has_entry('id', client_reliable.id),
            ]
        if reliable_cc_payer in (None, False):
            clients_match += [
                hm.has_entry('id', client_none.id),
                hm.has_entry('id', client_not_reliable.id),
            ]

        data = res.get_json()['data']
        hm.assert_that(
            data.get('items', []),
            hm.contains_inanyorder(*clients_match),
        )

    @pytest.mark.parametrize(
        'sort_key, sort_order, reversed_',
        [
            pytest.param('DT', 'ASC', False, id='by dt'),
            pytest.param('NAME', 'ASC', True, id='by name'),
            pytest.param('DT', 'DESC', True, id='desc sort'),
        ],
    )
    def test_sorting(self, sort_key, sort_order, reversed_):
        session = self.test_session
        name_prefix = 'client_sort_%s_' % ob.get_big_number()
        clients = [
            create_client(name=name_prefix + '2', dt=session.now()),
            create_client(name=name_prefix + '1', dt=session.now() + datetime.timedelta(days=1)),
        ]

        res = self.test_client.get(
            self.BASE_API,
            {
                'name': name_prefix,
                'sort_key': sort_key,
                'sort_order': sort_order,
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data', {})
        hm.assert_that(
            data,
            hm.has_entries({
                'total_count': 2,
                'items': hm.contains(*[
                    hm.has_entries(id=c.id)
                    for c in sorted(clients, key=lambda c: c.dt, reverse=reversed_)
                ]),
            }),
        )

    def test_managers(self, client):
        managers = [
            create_manager(client=client, w_mv=True),
            create_manager(client=client, w_mv=True),
        ]
        res = self.test_client.get(
            self.BASE_API,
            {'client_id': client.id, 'hide_managers': False},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'row_count': 1,
                'items': hm.contains(
                    hm.has_entries({
                        'id': client.id,
                        'managers': hm.contains(*[
                            hm.has_entries({'manager_code': m.manager_code})
                            for m in sorted(managers, key=lambda m: m.manager_code)
                        ]),
                    }),
                ),
            }),
        )


@pytest.mark.permissions
class TestClientListPermission(TestCaseApiAppBase):
    BASE_API = '/v1/client/list'

    @mock_client_resource('yb_snout_api.resources.v1.client.routes.client_list.ClientFinder')
    def test_own_client(self, client_role):
        client = create_client(name='snout_test_%s' % ob.get_big_number())
        create_client(name='snout_test_%s' % ob.get_big_number())

        security.set_roles([client_role])
        security.set_passport_client(client)
        res = self.test_client.get(
            self.BASE_API,
            params={'name': 'snout_test'},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        data = res.get_json().get('data', {})

        hm.assert_that(
            data['items'],
            hm.contains(
                hm.has_entry('id', client.id),
            ),
        )

    def test_wo_permission(self, admin_role, client):
        security.set_roles([admin_role])
        res = self.test_client.get(
            self.BASE_API,
            params={'name': client.name},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        data = res.get_json().get('data', {})
        hm.assert_that(data['items'], hm.empty())

    def test_w_permission(
            self,
            admin_role,
            view_client_role,
    ):
        passport_client = create_client(name='snout_test_%s' % ob.get_big_number())
        client_1 = create_client(name='snout_test_%s' % ob.get_big_number())
        client_2 = create_client(name='snout_test_%s' % ob.get_big_number())
        _client = create_client(name='snout_test_%s' % ob.get_big_number())

        role_client_group = create_role_client_group(clients=[client_1, client_2])
        create_role_client_group(clients=[_client])
        roles = [
            admin_role,
            (view_client_role, {cst.ConstraintTypes.client_batch_id: role_client_group.client_batch_id}),
        ]

        security.set_roles(roles)
        security.set_passport_client(passport_client)
        res = self.test_client.get(
            self.BASE_API,
            params={'name': 'snout_test'},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        data = res.get_json().get('data', {})

        hm.assert_that(
            data['items'],
            hm.contains_inanyorder(
                hm.has_entry('id', client_1.id),
                hm.has_entry('id', client_2.id),
                hm.has_entry('id', passport_client.id),
            ),
        )
