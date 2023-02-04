# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
import hamcrest as hm

from balance import constants as cst
from balance.actions import promocodes as promo_actions

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client, create_agency
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice, create_custom_invoice
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.promocode import create_legacy_promocode
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, get_client_role, create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.resource import mock_client_resource


@pytest.fixture(name='support_role')
def create_support_role():
    return create_role(
        (
            cst.PermissionCode.BILLING_SUPPORT,
            {cst.ConstraintTypes.client_batch_id: None},
        ),
    )


class TestReservePromocode(TestCaseApiAppBase):
    BASE_API = u'/v1/promocode/reserve'

    def test_ok(self, client, legacy_promocode):
        security.set_passport_client(client)
        res = self.test_client.secure_post(
            self.BASE_API,
            {'client_id': client.id, 'promocode': legacy_promocode.code},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        assert legacy_promocode.get_current_reservation(self.test_session.now(), client=client)

    def test_reserved_earlier_same_client(self, legacy_promocode):
        client = create_client()
        promo_actions.reserve_promo_code(client, legacy_promocode)
        self.test_session.flush()
        security.set_passport_client(client)

        res = self.test_client.secure_post(
            self.BASE_API,
            {'client_id': client.id, 'promocode': legacy_promocode.code},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        self.test_session.refresh(client)
        hm.assert_that(
            client.promocode_reservations,
            hm.contains(
                hm.has_properties(promocode_id=legacy_promocode.id),
            ),
        )

    def test_reserved_earlier_other_client(self, legacy_promocode):
        client_1 = create_client()
        client_2 = create_client()
        promo_actions.reserve_promo_code(client_1, legacy_promocode)
        self.test_session.flush()
        security.set_passport_client(client_2)

        res = self.test_client.secure_post(
            self.BASE_API,
            {'client_id': client_2.id, 'promocode': legacy_promocode.code},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'INVALID_PC_RESERVED_ON_ANOTHER_CLIENT',
                'description': "Invalid promo code: ID_PC_RESERVED_ON_ANOTHER_CLIENT",
            }),
        )

    def test_agency(self, agency, legacy_promocode):
        res = self.test_client.secure_post(
            self.BASE_API,
            {'client_id': agency.id, 'promocode': legacy_promocode.code},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'INVALID_PC_NON_DIRECT_CLIENT',
                'description': "Invalid promo code: ID_PC_NON_DIRECT_CLIENT",
            }),
        )

    @pytest.mark.parametrize(
        'valid_request',
        [True, False],
    )
    def test_request(self, valid_request):
        legacy_promocode = create_legacy_promocode(service_ids=[cst.ServiceId.DIRECT])
        request_ = create_invoice(service_id=cst.ServiceId.DIRECT if valid_request else cst.ServiceId.MARKET).request

        res = self.test_client.secure_post(
            self.BASE_API,
            {
                'client_id': request_.client_id,
                'promocode': legacy_promocode.code,
                'request_id': request_.id,
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if valid_request else http.BAD_REQUEST))

        if not valid_request:
            hm.assert_that(
                res.get_json(),
                hm.has_entries({
                    'error': 'INVALID_PC_NO_MATCHING_ROWS',
                    'description': "Invalid promo code: ID_PC_NO_MATCHING_ROWS",
                }),
            )

    def test_request_w_other_client(self, client):
        legacy_promocode = create_legacy_promocode(service_ids=[cst.ServiceId.DIRECT])
        request_ = create_invoice(service_id=cst.ServiceId.MARKET).request

        res = self.test_client.secure_post(
            self.BASE_API,
            {
                'client_id': client.id,
                'promocode': legacy_promocode.code,
                'request_id': request_.id,
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))
        hm.assert_that(
            legacy_promocode.reservations,
            hm.contains(
                hm.has_properties(client_id=client.id),
            ),
        )

    def test_not_found(self, client):
        security.set_passport_client(client)
        res = self.test_client.secure_post(
            self.BASE_API,
            {'client_id': client.id, 'promocode': 'yellow-submarine'},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.BAD_REQUEST))

        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'PROMOCODE_NOT_FOUND',
                'description': 'Invalid promo code: ID_PC_UNKNOWN',
            }),
        )


@pytest.mark.permissions
class TestReservePromocodePermissions(TestCaseApiAppBase):
    BASE_API = u'/v1/promocode/reserve'

    @pytest.mark.parametrize('owns', [True, False])
    def test_owning(self, owns, client, legacy_promocode):
        security.set_roles([])
        if owns:
            security.set_passport_client(client)
        res = self.test_client.secure_post(
            self.BASE_API,
            {'client_id': client.id, 'promocode': legacy_promocode.code},
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if owns else http.FORBIDDEN))

    @pytest.mark.parametrize(
        'has_role, match_client, allowed',
        [
            pytest.param(True, None, True, id='role_wo_constrants'),
            pytest.param(True, True, True, id='match_client'),
            pytest.param(True, False, False, id='wrong_client'),
            pytest.param(False, None, False, id='wo_role'),
        ],
    )
    def test_perm(self, admin_role, support_role, client, legacy_promocode, has_role, match_client, allowed):
        roles = [admin_role]
        if has_role:
            role = support_role
            if match_client is not None:
                client_batch_id = create_role_client(client if match_client else None).client_batch_id
                role = (role, {cst.ConstraintTypes.client_batch_id: client_batch_id})
            roles.append(role)
        security.set_roles(roles)

        res = self.test_client.secure_post(
            self.BASE_API,
            {'client_id': client.id, 'promocode': legacy_promocode.code},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if allowed else http.FORBIDDEN))
