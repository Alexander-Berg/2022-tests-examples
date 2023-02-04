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
from tests.tutils import mock_transactions

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client_group
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_role, create_admin_role


@pytest.fixture(name='recalc_role')
def create_recalc_role():
    return create_role((cst.PermissionCode.RECALCULATE_OVERDRAFT, {cst.ConstraintTypes.client_batch_id: None}))


@pytest.mark.smoke
class TestCaseRecalculateOverdraft(TestCaseApiAppBase):
    BASE_API = '/v1/client/recalculate-overdraft'
    EXPORT_TYPE = 'OVERDRAFT'

    @pytest.mark.parametrize(
        'match_client, ans',
        [
            pytest.param(True, http.OK, id='all clients in role'),
            pytest.param(False, http.FORBIDDEN, id='forbidden'),
        ],
    )
    def test_recalc(self, admin_role, recalc_role, client, match_client, ans):
        aliases = [create_client() for _i in range(2)]
        client.class_.aliases.extend(aliases)
        client_batch_id = create_role_client_group(clients=aliases + [client] if match_client else []).client_batch_id
        roles = [
            admin_role,
            (recalc_role, {cst.ConstraintTypes.client_batch_id: client_batch_id}),
        ]

        security.set_roles(roles)
        with mock_transactions():
            res = self.test_client.secure_post(
                self.BASE_API,
                {'client_id': client.id},
            )
        hm.assert_that(res.status_code, hm.equal_to(ans))

        if ans is http.OK:
            export_count = (
                self.test_session.query(mapper.Export)
                .filter(
                    mapper.Export.classname == 'Client',
                    mapper.Export.type == self.EXPORT_TYPE,
                    mapper.Export.object_id.in_(a.id for a in aliases + [client]),
                )
                .count()
            )
            hm.assert_that(export_count, hm.equal_to(3))
