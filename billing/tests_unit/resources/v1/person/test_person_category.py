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

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.person import create_person
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_role, create_admin_role


@pytest.fixture(name='view_persons_role')
def create_view_persons_role():
    return create_role(
        (
            cst.PermissionCode.VIEW_PERSONS,
            {cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.mark.smoke
class TestCasePersonCategory(TestCaseApiAppBase):
    BASE_API = '/v1/person/person-category'

    @pytest.mark.parametrize(
        'is_admin',
        [False, True],
    )
    def test_base(self, client, admin_role, view_persons_role, is_admin):
        if is_admin:
            client_batch_id = create_role_client(client=client).client_batch_id
            security.set_roles([
                admin_role,
                (view_persons_role, {cst.ConstraintTypes.client_batch_id: client_batch_id}),
            ])
        else:
            security.set_passport_client(client)
            security.set_roles([])

        person = create_person(client=client)
        res = self.test_client.get(
            self.BASE_API,
            params={'person_id': person.id},
            is_admin=is_admin,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'category': 'ph',
                'is_partner': False,
                'name': 'ID_Individual',
                'caption': 'ID_Individual_ex',
                'legal_entity': False,
            }),
        )

    def test_nobody(self):
        security.set_roles([])
        person = create_person()
        res = self.test_client.get(
            self.BASE_API,
            params={'person_id': person.id},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.FORBIDDEN))
