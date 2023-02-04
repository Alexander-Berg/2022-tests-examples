# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import http.client as http
import hamcrest as hm

from balance import constants as cst
from tests import object_builder as ob
from tests.tutils import has_exact_entries

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.person import create_person
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_role_client, create_client
from yb_snout_api.tests_unit.fixtures.permissions import create_role


@pytest.fixture(name='view_person_role')
def create_view_person_role():
    return create_role(
        (
            cst.PermissionCode.VIEW_PERSONS,
            {cst.ConstraintTypes.client_batch_id: None},
        ),
    )


class TestCasePersonListSimple(TestCaseApiAppBase):
    BASE_API = u'/v1/person/list-simple'

    def test_base(self, client):
        fias = ob.FiasBuilder.construct(self.test_session, formal_name='Село Пупкино', postcode='654321')
        person = create_person(
            client=client,
            type='ph',
            name='Yandex Employee',
            fias_guid=fias.guid,
            street='Солнечная',
            postsuffix='ул.',
            postcode=fias.postcode,
            email='nowar@ya.ru',
        )
        res = self.test_client.get(
            self.BASE_API,
            [('person_ids', person.id)],
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'items': hm.contains(
                    has_exact_entries({
                        'id': person.id,
                        'type': 'ph',
                        'name': 'Yandex Employee',
                        'envelope_address': 'Солнечная, ул.',
                        'postcode': '654321',
                        'email': 'nowar@ya.ru',
                    }),
                ),
                'total_count': 1,
            }),
        )

    @pytest.mark.parametrize(
        'w_role, w_client, owner, w_res',
        [
            pytest.param(True, False, False, False, id='w role wo client'),
            pytest.param(True, True, False, True, id='w role w client'),
            pytest.param(False, False, True, True, id='owner'),
            pytest.param(False, False, False, False, id='nobody'),
        ],
    )
    def test_access(self, client, view_person_role, w_role, w_client, owner, w_res):
        roles = []
        if w_role:
            client_batch_id = create_role_client(client=client if w_client else None).client_batch_id
            roles.append((
                view_person_role,
                {cst.ConstraintTypes.client_batch_id: client_batch_id},
            ))
        security.set_roles(roles)
        if owner:
            security.set_passport_client(client)

        person = create_person(client=client)
        res = self.test_client.get(
            self.BASE_API,
            [('person_ids', person.id)],
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']

        if w_res:
            match_res = {'items': hm.contains(hm.has_entries({'id': person.id})), 'total_count': 1}
        else:
            match_res = {'items': hm.empty(), 'total_count': 0}

        hm.assert_that(
            data,
            hm.has_entries(match_res),
        )
