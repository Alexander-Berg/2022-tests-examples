# -*- coding: utf-8 -*-

import pytest
import hamcrest as hm
import mock

from balance import constants as cst, mapper
from muzzle.muzzle_logic import MuzzleLogic

from tests.base import MuzzleTest
from tests import object_builder as ob


@pytest.fixture(name='request_shop_role')
def create_request_shop_role(session):
    return ob.create_role(session, (cst.PermissionCode.CREATE_REQUESTS_SHOP, {cst.ConstraintTypes.firm_id: None}))


class TestGetSourceMuzzle(object):

    def test_get_firms_for_shop(self, session, muzzle_logic, request_shop_role):
        firm_ids = [
            cst.FirmId.YANDEX_OOO,
            cst.FirmId.CLOUD,
        ]
        roles = [
            (request_shop_role, {cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO}),
            (request_shop_role, {cst.ConstraintTypes.firm_id: cst.FirmId.CLOUD}),
        ]
        ob.set_roles(session, session.passport, roles)

        response = muzzle_logic.get_firms_for_shop(session)
        assert response.tag == 'firms'
        res_ids = [int(item.attrib['id']) for item in response._children]
        hm.assert_that(res_ids, hm.contains_inanyorder(*firm_ids))

    @pytest.mark.parametrize(
        'test_env, env_type, is_ok',
        [
            (0, 'prod', True),
            (0, 'test', True),
            (1, 'prod', False),
            (1, 'test', True),
        ]
    )
    def test_get_firms_for_shop_test_env(self, session, app, muzzle_logic, test_env, env_type, is_ok):
        firm = ob.FirmBuilder.construct(session, test_env=test_env)

        with mock.patch.object(app, 'get_current_env_type', return_value=env_type):
            response = muzzle_logic.get_firms_for_shop(session)

        assert response.tag == 'firms'
        res_ids = [int(item.attrib['id']) for item in response._children]
        if is_ok:
            assert firm.id in res_ids
        else:
            assert firm.id not in res_ids
