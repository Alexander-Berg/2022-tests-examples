# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import itertools
import pytest
import hamcrest as hm
import http.client as http

from tests import object_builder as ob

from balance import constants as cst
from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import (
    create_admin_role,
    create_role,
    create_view_client_role,
    create_passport,
)
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client, create_agency


@pytest.mark.smoke
class TestCaseClientAgencySubclients(TestCaseApiAppBase):
    BASE_API = '/v1/client/agency-subclients'

    @pytest.mark.parametrize(
        'ps, clients_count',
        [(1, 4), (2, 5), (10, 5)],
    )
    def test_pagination(self, agency, ps, clients_count):
        clients = [create_client() for _ in range(clients_count)]

        for client in clients:
            ob.OrderBuilder(client=client, agency=agency).build(self.test_session).obj
        client_ids = [client.id for client in clients]
        got_ids = []
        # Getting all clients page by page
        pn = 0
        while pn * ps <= clients_count:
            pn += 1
            res = self.test_client.get(
                self.BASE_API,
                {'agency_id': agency.id, 'pagination_pn': pn, 'pagination_ps': ps},
            )
            hm.assert_that(res.status_code, hm.equal_to(http.OK))
            data = res.get_json().get('data', [])
            hm.assert_that(
                data,
                hm.has_entries({
                    'items': hm.has_length(hm.less_than_or_equal_to(ps)),
                    'total_count': clients_count,
                }),
            )
            got_ids.extend(c.get('client', {}).get('id') for c in data['items'])
        hm.assert_that(got_ids, hm.contains_inanyorder(*client_ids))

    @pytest.mark.parametrize('search_type', ['login', 'id', 'name'])
    def test_search(self, agency, client, search_type):
        searched_name = u'%'  # Test right handle of special symbols with sql `LIKE`
        searched_login = u'xXx_grEaTLogin_xXx'
        similar_login = u'xXx1grEaTLogin1xXx'
        searched_client = create_client(name=searched_name)
        create_passport(login=searched_login, client=searched_client)
        create_passport(login=similar_login, client=client)
        for cl in (client, searched_client):
            ob.OrderBuilder(client=cl, agency=agency).build(self.test_session)
        search_text_by_type = {
            'login': searched_login,
            'id': str(searched_client.id),
            'name': searched_name,
        }
        res = self.test_client.get(
            self.BASE_API,
            {'agency_id': agency.id, 'search_text': search_text_by_type[search_type]},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        data = res.get_json().get('data', [])
        hm.assert_that(
            data,
            hm.has_entry(
                'items',
                hm.contains(hm.has_entries({
                    'client': hm.has_entry('id', searched_client.id),
                })),
            ),
        )
        hm.assert_that(
            data,
            hm.has_entry('total_count', 1),
        )

    def test_multiple_main_login(self, agency, client):
        ob.OrderBuilder(client=client, agency=agency).build(self.test_session)
        logins = [ob.generate_character_string() for _ in range(3)]

        for login in logins:
            create_passport(login=login, client=client, is_main=1)
        res = self.test_client.get(self.BASE_API, {'agency_id': agency.id})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        data = res.get_json().get('data', [])
        hm.assert_that(
            data,
            hm.has_entry(
                'items',
                hm.contains(hm.has_entries({
                    'client': hm.has_entry('id', client.id),
                    'client_login': hm.is_in(logins),
                })),
            ),
        )
        hm.assert_that(data, hm.has_entry('total_count', 1))

    def test_without_search_text(self, agency):
        clients = [create_client() for _ in range(3)]

        clients_with_login = [create_client() for _ in range(2)]
        logins = []
        for client in clients_with_login:
            login = ob.generate_character_string()
            logins.append(login)
            create_passport(login=login, client=client, is_main=1)

        for client in itertools.chain(clients, clients_with_login):
            ob.OrderBuilder(client=client, agency=agency).build(self.test_session)

        # Create not main login (just for check)
        create_passport(login=ob.generate_character_string(), client=clients[0])

        total_clients = len(clients) + len(clients_with_login)
        res = self.test_client.get(self.BASE_API, {'agency_id': agency.id, 'pagination_ps': total_clients})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        data = res.get_json().get('data', [])
        subclient_matchers = (
            hm.has_entries({
                'client': hm.has_entry('id', client.id),
                'client_login': hm.equal_to(None),
            })
            for client in clients
        )
        subclient_with_login_matchers = (
            hm.has_entries({
                'client': hm.has_entry('id', client.id),
                'client_login': hm.equal_to(login),
            })
            for client, login in zip(clients_with_login, logins)
        )
        hm.assert_that(
            data,
            hm.has_entry(
                'items',
                hm.contains_inanyorder(*itertools.chain(subclient_matchers, subclient_with_login_matchers)),
            ),
        )
        hm.assert_that(
            data,
            hm.has_entry(
                'total_count',
                total_clients,
            ),
        )

    def test_not_found(self, not_existing_id):
        response = self.test_client.get(self.BASE_API, {'agency_id': not_existing_id})
        hm.assert_that(response.status_code, hm.equal_to(http.NOT_FOUND), 'Response code must be 404(NOT_FOUND)')

    @pytest.mark.permissions
    @pytest.mark.parametrize('is_admin', [True, False])
    @pytest.mark.parametrize(
        'match_client, status_code',
        [
            (None, http.FORBIDDEN),
            (True, http.OK),
            (False, http.FORBIDDEN),
        ],
    )
    def test_permission(self, admin_role, match_client, status_code, is_admin, view_client_role, agency):
        roles = [admin_role] if is_admin else []
        if match_client is not None:
            role_client = create_role_client(agency if match_client else create_client())
            roles.append(
                (view_client_role, {cst.ConstraintTypes.client_batch_id: role_client.client_batch_id}),
            )
        security.set_roles(roles)
        response = self.test_client.get(
            self.BASE_API,
            {'agency_id': agency.id},
            is_admin=is_admin,
        )
        hm.assert_that(response.status_code, hm.equal_to(status_code))

    @pytest.mark.permissions
    def test_owner(self, agency):
        security.set_passport_client(agency)
        response = self.test_client.get(
            self.BASE_API,
            {'agency_id': agency.id},
            is_admin=False,
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))
