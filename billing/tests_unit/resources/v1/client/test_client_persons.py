# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
import hamcrest as hm

from balance import constants as cst, exc

from brest.core.tests import security

from yb_snout_api.utils import clean_dict
from yb_snout_api.resources.v1.client.enums import PersonModeType
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_role_client, create_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.person import create_person
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import (
    create_admin_role,
    get_client_role,
    create_role,
)


@pytest.fixture(name='view_client_role')
def create_view_client_role():
    return create_role(
        (
            cst.PermissionCode.VIEW_CLIENTS,
            {cst.ConstraintTypes.client_batch_id: None},
        ),
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
@pytest.mark.permissions
class TestCaseClientPersonPermission(TestCaseApiAppBase):
    BASE_API = u'/v1/client/person'

    def test_get_person_CI(self, client_role, client):
        """The resource must return client data linked with passport"""
        security.set_passport_client(client)
        security.set_roles([client_role])

        person = create_person(client)
        response = self.test_client.get(self.BASE_API, {'client_id': person.client_id}, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'Response code must be OK')

        data = response.get_json().get('data', {})
        hm.assert_that(
            data,
            [hm.has_entries({
                'id': person.id,
                'name': person.name,
                'email': person.email,
                'type': person.type,
                'person_category': hm.has_entries({'ur': int(person.person_category.ur)}),
            })],
        )

    def test_get_person_CI_w_role(self, client, view_client_role, view_person_role):
        security.set_roles([view_client_role, view_person_role])
        person = create_person(client)
        response = self.test_client.get(self.BASE_API, {'client_id': person.client_id}, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'Response code must be OK')
        data = response.get_json().get('data', {})
        hm.assert_that(
            data,
            [hm.has_entry('id', person.id)],
        )

    def test_get_person_CI_forbidden(self, client, client_role):
        security.set_roles([client_role])
        create_person(client)
        response = self.test_client.get(self.BASE_API, {'client_id': client.id}, is_admin=False)
        hm.assert_that(response.status_code, hm.equal_to(http.FORBIDDEN))

    def test_not_found(self):
        not_existing_id = self.test_session.execute('select bo.S_CLIENT_ID.nextval from dual').scalar()
        response = self.test_client.get(self.BASE_API, {'client_id': not_existing_id}, is_admin=True)
        hm.assert_that(response.status_code, hm.equal_to(http.NOT_FOUND), 'Response code must be 404(NOT_FOUND)')

    @pytest.mark.parametrize(
        'w_perm',
        [True, False],
    )
    def test_permission(self, client, admin_role, view_client_role, view_person_role, w_perm):
        roles = [admin_role, view_client_role]
        if w_perm:
            roles.append(view_person_role)
        security.set_roles(roles)

        person = create_person(client)
        response = self.test_client.get(self.BASE_API, {'client_id': person.client_id})
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        data = response.get_json().get('data', {})
        if w_perm:
            person_match = [hm.has_entries({
                'id': person.id,
                'name': person.name,
                'email': person.email,
                'type': person.type,
                'person_category': hm.has_entries({'ur': int(person.person_category.ur)}),
            })]
        else:
            person_match = hm.empty()
        hm.assert_that(data, person_match)

    @pytest.mark.parametrize(
        'match_client',
        [
            None,
            False,
            True,
        ],
    )
    def test_client_constraint(
            self,
            client,
            admin_role,
            view_client_role,
            view_person_role,
            match_client,
    ):
        roles = [admin_role, view_client_role]

        client_batch_id = None
        if match_client is not None:
            client_batch_id = create_role_client(client=client if match_client else None).client_batch_id
        roles.append((view_person_role, clean_dict({cst.ConstraintTypes.client_batch_id: client_batch_id})))
        security.set_roles(roles)

        person = create_person(client)
        response = self.test_client.get(self.BASE_API, {'client_id': person.client_id})
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        data = response.get_json().get('data', {})
        person_match = [hm.has_entries({'id': person.id})] if match_client in (None, True) else hm.empty()
        hm.assert_that(data, person_match)

    def test_with_history(self, person):
        date = self.test_session.now().strftime('%Y-%m-%dT')
        person.email = 'test1@ya.com'
        person.local_name = 'Local name 1'
        person.type = 'kzu'
        self.test_session.flush()

        person.email = 'test2@ya.com'
        person.local_name = 'Local name 2'
        person.fax = '+9888990099'
        person.swift = 'abcd'
        self.test_session.flush()

        res = self.test_client.get(
            self.BASE_API,
            {'client_id': person.client_id, 'with_history': True},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.contains(
                hm.has_entries({
                    'id': person.id,
                    'client_id': person.client_id,
                    'email': 'test2@ya.com',
                    'fax': '+9888990099',
                    'swift': 'abcd',
                    'history': hm.has_entries({
                        'email': hm.has_entries({'update_by': 'Pupkin pup', 'update_dt': hm.starts_with(date)}),
                    }),
                    'allow_archive': True,
                    'full_refusal': False,
                    'archive_refusal_reason': None,
                }),
            ),
        )

    @pytest.mark.parametrize(
        'is_partner',
        [False, True, None],
    )
    def test_is_partner(self, client, is_partner):
        person = create_person(client)
        partner_person = create_person(client, is_partner=True)

        res = self.test_client.get(
            self.BASE_API,
            clean_dict({'client_id': client.id, 'is_partner': is_partner}),
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        person_match = hm.has_item(hm.has_entries({'id': person.id}))
        if is_partner is True:
            person_match = hm.not_(person_match)
        partner_person_match = hm.has_item(hm.has_entries({'id': partner_person.id}))
        if is_partner is False:
            partner_person_match = hm.not_(partner_person_match)
        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.all_of(
                person_match,
                partner_person_match,
            ),
        )

    @pytest.mark.parametrize(
        'legal_entity',
        [None, True, False],
    )
    def test_legal_entity(self, client, legal_entity):
        ph_person = create_person(client, type='ph')
        ur_person = create_person(client, type='ur')

        res = self.test_client.get(
            self.BASE_API,
            clean_dict({'client_id': client.id, 'legal_entity': legal_entity}),
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        ph_person_match = hm.has_item(hm.has_entries({'id': ph_person.id}))
        if legal_entity is True:
            ph_person_match = hm.not_(ph_person_match)

        ur_person_match = hm.has_item(hm.has_entries({'id': ur_person.id}))
        if legal_entity is False:
            ur_person_match = hm.not_(ur_person_match)

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.all_of(
                ph_person_match,
                ur_person_match,
            ),
        )

    @pytest.mark.parametrize(
        'pn, ps, so, from_, to_',
        [
            pytest.param(1, 10, 'ASC', 0, 10, id='all'),
            pytest.param(1, 10, 'DESC', 0, 10, id='all desc'),
            pytest.param(1, 5, 'DESC', 0, 5, id='first page desc'),
            pytest.param(5, 2, 'ASC', 8, 10, id='last page asc'),
            pytest.param(3, 3, 'DESC', 6, 9, id='page desc'),
            pytest.param(None, None, None, 0, 10, id='wo pagination and sorting'),
        ],
    )
    def test_pagination_sorting(self, client, pn, ps, so, from_, to_):
        persons = [create_person(client=client, name='Name_%s' % i) for i in range(10)]
        res = self.test_client.get(
            self.BASE_API,
            clean_dict({
                'client_id': client.id,
                'pagination_ps': ps,
                'pagination_pn': pn,
                'sort_key': 'NAME',
                'sort_order': so,
            }),
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        if so == 'DESC':
            persons = persons[::-1]

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.contains(*[
                hm.has_entries({
                    # 'id': p.id,
                    'name': p.name,
                })
                for p in persons[from_:to_]
            ])
        )

    @pytest.mark.parametrize(
        'is_admin, w_client',
        [
            pytest.param(True, False, id='admin wo client_id'),
            pytest.param(True, True, id='admin w client_id'),
            pytest.param(False, False, id='client wo client_id'),
            pytest.param(False, True, id='client w client_id'),
        ],
    )
    def test_client_choices(self, is_admin, w_client):
        client = create_client()
        passport_client = create_client()
        self.test_session.passport.client = passport_client

        person = create_person(client=client)
        passport_person = create_person(client=passport_client)

        res = self.test_client.get(
            self.BASE_API,
            {'client_id': client.id} if w_client else {},
            is_admin=is_admin,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.contains(
                hm.has_entries({'id': passport_person.id if not w_client else person.id}),
            ),
        )

    def test_show_totals(self, person):
        res = self.test_client.get(
            self.BASE_API,
            {
                'client_id': person.client_id,
                'show_totals': True,
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'items': hm.contains(hm.has_entries({'id': person.id})),
                'total_count': 1,
                'request_params': hm.has_entries({'client_id': person.client_id, 'show_totals': True}),
            }),
        )

    @pytest.mark.parametrize(
        'mode',
        [
            PersonModeType.DEFAULT,
            PersonModeType.ARCHIVED,
            PersonModeType.ALL,
        ],
    )
    def test_archived(self, client, mode):
        person = create_person(client=client, name='Active person')
        hidden_person = create_person(client=client, name='Archived person', hidden=1)

        security.set_roles([])
        security.set_passport_client(client)

        res = self.test_client.get(self.BASE_API, {'mode': mode.name, 'show_totals': True}, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        match_person = hm.has_entries({'id': person.id, 'name': 'Active person'})
        match_hidden_person = hm.has_entries({'id': hidden_person.id, 'name': 'Archived person'})
        total_count = 0
        res_items = []
        if mode in [PersonModeType.ALL, PersonModeType.DEFAULT]:
            res_items.append(match_person)
            total_count += 1
        if mode in [PersonModeType.ALL, PersonModeType.ARCHIVED]:
            res_items.append(match_hidden_person)
            total_count += 1

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'items': hm.contains_inanyorder(*res_items),
                'total_count': total_count,
            }),
        )
