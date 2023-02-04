# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals
from builtins import range

from future import standard_library

standard_library.install_aliases()

import itertools
import pytest
import hamcrest as hm
import http.client as http
from decimal import Decimal as D

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
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice


@pytest.fixture(name='view_client_role')
def create_view_client_role():
    return create_role(
        (
            cst.PermissionCode.VIEW_CLIENTS,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.fixture(name='view_invoices_role')
def create_view_invoices_role():
    return create_role(
        (
            cst.PermissionCode.VIEW_INVOICES,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.mark.smoke
class TestCaseClientUnusedFunds(TestCaseApiAppBase):
    BASE_API = '/v1/client/unused-funds'

    @staticmethod
    def _get_unused_rub_sum(invoices):
        res = {
            'unused_sum': D(0),
            'unused_rub_sum': D(0),
            'invoice_count': 0,
        }
        for inv in invoices:
            if inv.credit == 1 or inv.receipt_sum <= inv.consume_sum:
                continue
            res['unused_sum'] += inv.unused_funds
            res['unused_rub_sum'] += inv.unused_funds_rub
            res['invoice_count'] += 1
        for f in ['unused_sum', 'unused_rub_sum']:
            res[f] = '{:.2f}'.format(res[f])
        return res

    @staticmethod
    def _split_integer_into_parts(num, parts):  # Don't use big num
        res = [0 for _ in range(parts)]
        while num > 0:
            num -= 1
            res[ob.RANDOM.randint(0, parts - 1)] += 1
        return res

    def _create_invoices(self, client, unused_fish, used_invoices=3, unused_invoices=1, firm_id=cst.FirmId.YANDEX_OOO):
        invoices = []
        splitted_fish = list(
            map(lambda num: num / D('100.0'), self._split_integer_into_parts(int(unused_fish * 100), used_invoices)),
        )
        for i in range(used_invoices):
            fish_offset = D('666.66')
            inv = create_invoice(
                client=client,
                receipt_sum=fish_offset + splitted_fish[i],
                consume_sum=fish_offset,
                firm_id=firm_id,
            )
            invoices.append(inv)
        for _ in range(unused_invoices):
            rand_money = D('100500.45')
            invoices.append(
                create_invoice(
                    client=client,
                    receipt_sum=rand_money,
                    consume_sum=rand_money,
                ),
            )
        return invoices

    @pytest.mark.parametrize(
        'unused_fish',
        [D('1.23'), D('0.98'), D('0.0')],
    )
    def test_unused_funds(self, client, unused_fish):
        invoices = self._create_invoices(client, unused_fish, used_invoices=3, unused_invoices=1)
        res = self.test_client.get(self.BASE_API, {'client_id': client.id})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        unused_funds = self._get_unused_rub_sum(invoices)
        data = res.get_json().get('data', [])
        hm.assert_that(
            data,
            hm.has_entries(unused_funds),
        )

    def test_client_not_found(self):
        not_existing_id = self.test_session.execute("select bo.s_client_id.nextval from dual").scalar()
        res = self.test_client.get(self.BASE_API, {'client_id': not_existing_id})
        hm.assert_that(res.status_code, hm.equal_to(http.NOT_FOUND))

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'match_client',
        [None, True, False],
    )
    @pytest.mark.parametrize(
        'match_firm',
        [True, False],
    )
    def test_permission(self, match_client, match_firm, view_client_role, view_invoices_role, admin_role, client):
        unused_fish = D('0.05')
        roles = [admin_role, view_client_role]
        if match_client is not None:
            role_client = create_role_client(client if match_client else create_client())
            roles.append(
                (
                    view_invoices_role,
                    {
                        cst.ConstraintTypes.client_batch_id: role_client.client_batch_id,
                        cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO if match_firm else cst.FirmId.MARKET,
                    },
                ),
            )
        security.set_roles(roles)
        invoices = self._create_invoices(
            client,
            unused_fish,
            used_invoices=1,
            unused_invoices=0,
            firm_id=cst.FirmId.YANDEX_OOO,
        )
        response = self.test_client.get(
            self.BASE_API,
            {'client_id': client.id},
        )
        hm.assert_that(response.status_code, hm.equal_to(http.OK))
        data = response.get_json().get('data', {})
        if match_firm and match_client:
            hm.assert_that(data, hm.has_entries(self._get_unused_rub_sum(invoices)))
        else:
            hm.assert_that(data, hm.has_entry('unused_rub_sum', '{:.2f}'.format(D('0.0'))))

    def test_client(self, client):
        unused_fish = D('1.23')

        security.set_roles([])
        security.set_passport_client(client)

        invoices = self._create_invoices(client, unused_fish, used_invoices=3, unused_invoices=1)

        res = self.test_client.get(self.BASE_API, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        unused_funds = self._get_unused_rub_sum(invoices)
        data = res.get_json().get('data', [])
        hm.assert_that(
            data,
            hm.has_entries(unused_funds),
        )
