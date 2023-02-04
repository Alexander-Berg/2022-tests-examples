# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import hamcrest as hm
import pytest

from balance import constants as cst
from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.utils import clean_dict
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.person import create_person
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import (
    create_client,
    create_role_client,
    create_role_client_group,
)


@pytest.fixture(name='view_person_role')
def create_view_person_role():
    return create_role(
        (
            cst.PermissionCode.VIEW_PERSONS,
            {cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.mark.smoke
class TestCasePersonList(TestCaseApiAppBase):
    BASE_API = u'/v1/person/list'

    def test_get_persons_by_id(self, admin_role, view_person_role):
        security.set_roles([admin_role, view_person_role])
        person = create_person()
        response = self.test_client.get(self.BASE_API, {'person_id': person.id})
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

        data = response.get_json()['data']
        person_match = hm.has_entries({
            'client_name': person.client.name,
            'hidden': person.hidden,
            'id': person.id,
            'inn': person.inn,
            'is_partner': False,
            'kpp': person.kpp,
            'name': person.name,
            'type': person.type,
        })
        hm.assert_that(data.get('total_row_count', 0), hm.equal_to(1))
        hm.assert_that(data.get('items', []), hm.contains(person_match))

    def test_vip_only(self, admin_role, client, view_person_role):
        security.set_roles([admin_role, view_person_role])
        vip_person = create_person(client=client, vip=True)
        non_vip_person = create_person(client=client, vip=False)

        for vip_only in [True, False]:
            response = self.test_client.get(
                self.BASE_API,
                {'client_id': client.id, 'vip_only': vip_only},
            )
            hm.assert_that(response.status_code, hm.equal_to(http.OK))

            persons = [vip_person]
            if not vip_only:
                persons.append(non_vip_person)

            data = response.get_json()['data']
            hm.assert_that(
                data.get('items', []),
                hm.contains_inanyorder(*[
                    hm.has_entries({'id': p.id, 'client_id': client.id})
                    for p in persons
                ]),
                'Wrong answer for vip_only=%s' % vip_only,
            )

    def test_is_partner(self, admin_role, client, view_person_role):
        security.set_roles([admin_role, view_person_role])
        partner_person = create_person(client=client, is_partner=True)
        not_partner_person = create_person(client=client, is_partner=False)

        for is_partner in [True, False, None]:
            response = self.test_client.get(
                self.BASE_API,
                clean_dict({'client_id': client.id, 'is_partner': is_partner}),
            )
            hm.assert_that(response.status_code, hm.equal_to(http.OK))

            persons = []
            if is_partner or is_partner is None:
                persons.append(partner_person)
            if not is_partner:
                persons.append(not_partner_person)

            data = response.get_json()['data']
            hm.assert_that(
                data.get('items', []),
                hm.contains_inanyorder(*[
                    hm.has_entries({'id': p.id, 'client_id': client.id})
                    for p in persons
                ]),
                'Wrong answer for is_partner=%s' % is_partner,
            )

    @pytest.mark.parametrize(
        'person_type, field_name, search_field',
        [
            pytest.param('ph', 'inn', 'inn', id='ph'),
            pytest.param('ur', 'kpp', 'kpp', id='ur'),
            pytest.param('kzp', 'inn', 'inn', id='kzp w inn'),
            pytest.param('kzp', 'kz_in', 'inn', id='kzp w kz_in'),
            pytest.param('kzu', 'kz_in', 'inn', id='kzu w kz_in'),
            pytest.param('kzu', 'rnn', 'inn', id='kzu w rnn'),
            pytest.param('yt_kzu', 'rnn', 'inn', id='yt_kzu'),
            pytest.param('yt_kzp', 'kz_in', 'inn', id='yt_kzp'),
        ],
    )
    def test_search_by_inn(self, admin_role, view_person_role, person_type, field_name, search_field):
        security.set_roles([admin_role, view_person_role])

        inn = 666
        person_params = {'type': person_type, field_name: inn}
        person = create_person(**person_params)
        self.test_session.flush()
        response = self.test_client.get(self.BASE_API, {search_field: inn})
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

        data = response.get_json()['data']
        hm.assert_that(
            data.get('items', []),
            hm.contains(hm.has_entry('id', person.id)),
        )

    def test_by_type(self, admin_role, view_person_role):
        security.set_roles([admin_role, view_person_role])

        country = ob.CountryBuilder.construct(self.test_session)
        person_type = ob.PersonCategoryBuilder.construct(self.test_session, country=country)
        person = create_person(type=person_type.category, skip_category_check=True)
        self.test_session.flush()

        response = self.test_client.get(self.BASE_API, {'person_type': person_type.category})
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        data = response.get_json()['data']
        hm.assert_that(
            data.get('items', []),
            hm.contains(hm.has_entry('id', person.id)),
        )


@pytest.mark.permissions
class TestCasePersonListPermission(TestCaseApiAppBase):
    BASE_API = u'/v1/person/list'

    def test_wo_role(self, admin_role):
        security.set_roles([admin_role])
        person = create_person()
        res = self.test_client.get(self.BASE_API, {'client_id': person.client_id})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        hm.assert_that(res.get_json()['data']['items'], hm.empty())

    def test_permission(self, admin_role, view_person_role):
        name = 'common test name %s' % ob.get_big_number()

        client1 = create_client()
        client2 = create_client()
        client3 = create_client()
        client4 = create_client()

        client_batch_1 = create_role_client_group(clients=[client1, client3]).client_batch_id
        client_batch_2 = create_role_client().client_batch_id

        roles = [
            admin_role,
            (view_person_role, {cst.ConstraintTypes.client_batch_id: client_batch_1}),
            (view_person_role, {cst.ConstraintTypes.client_batch_id: client_batch_2}),
        ]

        security.set_roles(roles)
        security.set_passport_client(client2)

        person11 = create_person(client=client1, name=name)
        person12 = create_person(client=client1, name=name)
        person21 = create_person(client=client2, name=name)
        person31 = create_person(client=client3, name=name)
        create_person(client=client4, name=name)

        res = self.test_client.get(self.BASE_API, {'name': name})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']['items']
        hm.assert_that(
            data,
            hm.contains_inanyorder(
                hm.has_entry('id', person11.id),
                hm.has_entry('id', person12.id),
                hm.has_entry('id', person21.id),
                hm.has_entry('id', person31.id),
            ),
        )

    def test_wo_filters(self, admin_role, view_person_role):
        """Не надо пытаться искать всех плательщов при отсутвие фильтров"""
        security.set_roles([admin_role, view_person_role])
        create_person()  # нужен хотя бы 1 в базе
        response = self.test_client.get(self.BASE_API)
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        data = response.get_json()['data']
        hm.assert_that(data.get('total_row_count', 0), hm.equal_to(0))
        hm.assert_that(data.get('items', []), hm.empty())

    def test_wo_filters_w_client(self):
        """Для клиента, принадлежащего паспорту можно показать его плательщиков"""
        security.set_roles([])
        person = create_person()
        security.set_passport_client(person.client)
        response = self.test_client.get(self.BASE_API, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        data = response.get_json()['data']
        hm.assert_that(data.get('total_row_count', 0), hm.equal_to(1))
        hm.assert_that(
            data.get('items', []),
            hm.contains(hm.has_entry('id', person.id)),
        )
