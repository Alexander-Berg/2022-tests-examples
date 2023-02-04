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

from balance import (
    constants as cst,
)

from brest.core.tests import security

from yb_snout_api.resources.v1.paystep.enums import OverdraftExpiringType
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, get_client_role, create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_agency, create_role_client
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice


@pytest.fixture(name='view_inv_role')
def create_view_inv_role():
    return create_role(
        (
            cst.PermissionCode.VIEW_INVOICES,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.mark.smoke
class TestExpiredOverdrafts(TestCaseApiAppBase):
    BASE_API = '/v1/paystep/expired-overdrafts'

    def test_base(self, client):
        # тест может падать из-за праздничных дней,
        # т.к. payment_term будет зависеть от календаря праздников
        self.test_session.__dict__['OVERDRAFT_EXCEEDED_DELAY'] = 10
        now = self.test_session.now()

        expired_dt = now - datetime.timedelta(days=25)
        nearly_expired_dt = now - datetime.timedelta(days=15)
        ok_dt = now - datetime.timedelta(days=1)

        common_params = dict(client=client, overdraft=True, turn_on=True)
        expired_invoice = create_invoice(dt=expired_dt, **common_params)
        nearly_expired_invoice = create_invoice(dt=nearly_expired_dt, **common_params)
        ok_invoice = create_invoice(dt=ok_dt, **common_params)

        res = self.test_client.get(self.BASE_API, {'client_id': client.id})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        req_data = [
            (expired_invoice, expired_dt, OverdraftExpiringType.EXPIRED.name, expired_invoice.payment_term.term - 25),
            (nearly_expired_invoice, nearly_expired_dt, OverdraftExpiringType.NEARLY_EXPIRED.name, nearly_expired_invoice.payment_term.term - 15),
            (ok_invoice, ok_dt, OverdraftExpiringType.OK.name, ok_invoice.payment_term.term - 1),
        ]
        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'total_sum': '9000.00',
                'expired_sum': '3000.00',
                'nearly_expired_sum': '3000.00',
                'invoices': hm.contains_inanyorder(*[
                    hm.has_entries({
                        'invoice_id': inv.id,
                        'invoice_eid': inv.external_id,
                        'debt': '3000.00',
                        'paysys': hm.has_entries({'cc': 'ph'}),
                        'deadline_dt': hm.not_none(),
                        'time_to_live': time_to_live,
                        'iso_currency': 'RUB',
                        'expired': expired,
                    })
                    for inv, dt, expired, time_to_live in req_data
                ]),
            }),
        )


@pytest.mark.permissions
class TestExpiredOverdraftsPermissions(TestCaseApiAppBase):
    BASE_API = '/v1/paystep/expired-overdrafts'

    @pytest.mark.parametrize(
        'allow_type',
        [
            'forbidden_admin_ui',
            'forbidden_client_ui',
            'from_admin_ui',
            'from_client_ui',
        ],
    )
    def test_get_expired_overdrafts(self, admin_role, client, view_inv_role, allow_type):
        allow = 'forbidden' not in allow_type
        is_admin = 'admin_ui' in allow_type

        if not is_admin:
            security.set_roles([])
            security.set_passport_client(client if allow else None)

        else:
            security.set_passport_client(None)
            client_batch_id = create_role_client(client=client if allow else None).client_batch_id
            security.set_roles([
                admin_role,
                (view_inv_role, {cst.ConstraintTypes.client_batch_id: client_batch_id}),
            ])

        invoice = create_invoice(client=client, overdraft=True)
        invoice.turn_on_rows()

        res = self.test_client.get(
            self.BASE_API,
            {'client_id': client.id, 'service_id': cst.ServiceId.DIRECT},
            is_admin=is_admin,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK if allow else http.FORBIDDEN))

        if allow:
            data = res.get_json()['data']
            hm.assert_that(
                data,
                hm.has_entries({
                    'total_sum': '3000.00',
                    'expired_sum': '0',
                    'nearly_expired_sum': '0',
                    'invoices': hm.contains(
                        hm.has_entries({
                            'invoice_id': invoice.id,
                            'invoice_eid': invoice.external_id,
                            'debt': '3000.00',
                            'paysys': hm.has_entries({'cc': 'ph'}),
                            'deadline_dt': hm.not_none(),
                            'time_to_live': invoice.payment_term.term,
                            'iso_currency': 'RUB',
                            'expired': OverdraftExpiringType.OK.name,
                        }),
                    ),
                }),
            )
