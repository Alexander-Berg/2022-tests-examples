# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import datetime
import pytest
import mock
import hamcrest as hm
import http.client as http

from balance import (
    constants as cst,
    core,
    mapper,
)
from tests import object_builder as ob

from brest.core.tests import security

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, get_client_role, create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_agency, create_role_client
from yb_snout_api.tests_unit.fixtures.firm import create_firm
from yb_snout_api.tests_unit.fixtures.common import create_paysys
from yb_snout_api.tests_unit.fixtures.contract import create_credit_contract
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice


@pytest.fixture(name='view_inv_role')
def create_view_inv_role():
    return create_role(
        (
            cst.PermissionCode.VIEW_INVOICES,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


def create_credit_contract_(agency, firm):
    contract = create_credit_contract(
        client=agency,
        commission=cst.ContractTypeId.NON_AGENCY,
        commission_type=None,
        credit_type=2,
        credit_limit_single=666,
        personal_account=1,
        personal_account_fictive=1,
        client_limits=None,
        firm=firm.id,
    )
    return contract


def create_credit_invoice(
    session,
    contract,
    client,
    paysys,
    qty,
    close_dt,
):
    request = ob.RequestBuilder.construct(
        session,
        basket=ob.BasketBuilder(
            client=contract.client,
            rows=[ob.BasketItemBuilder(
                quantity=qty,
                order=ob.OrderBuilder(
                    client=client,
                    agency=contract.client,
                    product=ob.Getter(mapper.Product, cst.DIRECT_PRODUCT_RUB_ID),
                ),
            )],
        ),
    )
    inv, = core.Core(session).pay_on_credit(request.id, paysys.id, contract.person_id, contract.id)
    inv.close_invoice(close_dt)

    y_invoice = filter(lambda i: isinstance(i, mapper.YInvoice), contract.invoices)[0]
    session.flush()

    return inv, y_invoice


@pytest.mark.smoke
class TestClientDebts(TestCaseApiAppBase):
    BASE_API = '/v1/client/debts'

    def test_overdraft(self, client):
        self.test_session.__dict__['OVERDRAFT_EXCEEDED_DELAY'] = 10
        now = self.test_session.now()
        nearly_expired_dt = now - datetime.timedelta(days=15)
        expired_dt = now - datetime.timedelta(days=45)

        security.set_roles([])
        security.set_passport_client(client)

        common_params = dict(client=client, overdraft=True, turn_on=True)
        expired_invoice = create_invoice(dt=expired_dt, **common_params)
        nearly_expired_invoice = create_invoice(dt=nearly_expired_dt, **common_params)
        _ok_invoice = create_invoice(dt=now, **common_params)

        res = self.test_client.get(self.BASE_API, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'overdraft_expired': hm.has_entries({
                    'items': hm.contains(hm.has_entries({
                        'invoice_id': expired_invoice.id,
                        'invoice_eid': expired_invoice.external_id,
                        'amount_to_pay': '3000.00',
                        'deadline_dt': hm.not_none(),
                        'time_to_live': hm.less_than(0),
                        'iso_currency': 'RUB',
                    })),
                    'total_count': 1,
                }),
                'overdraft_nearly_expired': hm.has_entries({
                    'items': hm.contains(hm.has_entries({
                        'invoice_id': nearly_expired_invoice.id,
                        'invoice_eid': nearly_expired_invoice.external_id,
                        'amount_to_pay': '3000.00',
                        'deadline_dt': hm.not_none(),
                        'time_to_live': 0,
                        'iso_currency': 'RUB',
                    })),
                    'total_count': 1,
                }),
                'credit_expired': hm.has_entries({
                    'items': hm.empty(),
                    'total_count': 0,
                }),
                'credit_nearly_expired': hm.has_entries({
                    'items': hm.empty(),
                    'total_count': 0,
                }),
            }),
        )

    @mock.patch('balance.mncloselib.get_task_last_status', return_value='resolved')  # не падать первого числа
    @pytest.mark.parametrize(
        'expired_type, expired_days',
        [
            pytest.param('credit_nearly_expired', 2, id='nearly_expired'),
            pytest.param('credit_expired', -1, id='expired'),
        ],
    )
    def test_credits(self, _mock_mnclose, agency, expired_type, expired_days):
        agency.set_currency(cst.ServiceId.DIRECT, 'RUB', self.test_session.now(), cst.CONVERT_TYPE_COPY)
        client = create_client(agency=agency)
        firm = create_firm(postpay=1, default_currency='RUR')

        security.set_roles([])
        security.set_passport_client(agency)

        exp_dt = self.test_session.now() + datetime.timedelta(days=expired_days)

        contract = create_credit_contract_(agency, firm)
        paysys = create_paysys(contract.firm.id)
        _, exp_inv = create_credit_invoice(self.test_session, contract, client, paysys, qty=10, close_dt=exp_dt)

        res = self.test_client.get(self.BASE_API, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        unexpired_type = list({'credit_expired', 'credit_nearly_expired'} - {expired_type})[0]

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                expired_type: hm.has_entries({
                    'items': hm.contains(hm.has_entries({
                        'invoice_id': exp_inv.id,
                        'invoice_eid': exp_inv.external_id,
                        'amount_to_pay': '10.00',
                        'deadline_dt': hm.not_none(),
                        'time_to_live': expired_days - 1,
                        'iso_currency': 'RUB',
                    })),
                    'total_count': 1,
                }),
                unexpired_type: hm.has_entries({
                    'items': hm.empty(),
                    'total_count': 0,
                }),
            }),
        )


@pytest.mark.permissions
class TestClientDebtsPermission(TestCaseApiAppBase):
    BASE_API = '/v1/client/debts'

    @pytest.mark.parametrize(
        'w_role',
        [True, False],
    )
    def test_overdraft(self, client, w_role, view_inv_role):
        self.test_session.__dict__['OVERDRAFT_EXCEEDED_DELAY'] = 10
        now = self.test_session.now()
        expired_dt = now - datetime.timedelta(days=45)

        roles = []
        if w_role:
            roles.append((
                view_inv_role,
                {cst.ConstraintTypes.client_batch_id: create_role_client(client=client).client_batch_id},
            ))
        security.set_roles(roles)

        common_params = dict(client=client, overdraft=True, turn_on=True)
        expired_invoice = create_invoice(dt=expired_dt, **common_params)

        res = self.test_client.get(self.BASE_API, {'client_id': client.id}, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'overdraft_expired': (
                    hm.has_entries({
                        'items': hm.contains(hm.has_entries({
                            'invoice_id': expired_invoice.id,
                            'invoice_eid': expired_invoice.external_id,
                        })),
                        'total_count': 1,
                    })
                    if w_role else
                    hm.has_entries({
                        'items': hm.empty(),
                        'total_count': 0,
                    })
                ),
            }),
        )

    @mock.patch('balance.mncloselib.get_task_last_status', return_value='resolved')  # не падать первого числа
    @pytest.mark.parametrize(
        'w_role',
        [True, False],
    )
    def test_credits_2(self, _mock_mnclose, agency, w_role, view_inv_role):
        agency.set_currency(cst.ServiceId.DIRECT, 'RUB', self.test_session.now(), cst.CONVERT_TYPE_COPY)
        client = create_client(agency=agency)
        firm = create_firm(postpay=1, default_currency='RUR')

        roles = []
        if w_role:
            roles.append((
                view_inv_role,
                {cst.ConstraintTypes.client_batch_id: create_role_client(client=agency).client_batch_id},
            ))
        security.set_roles(roles)

        exp_dt = self.test_session.now() + datetime.timedelta(days=-1)

        contract = create_credit_contract_(agency, firm)
        paysys = create_paysys(contract.firm.id)
        _, exp_inv = create_credit_invoice(self.test_session, contract, client, paysys, qty=10, close_dt=exp_dt)

        res = self.test_client.get(self.BASE_API, {'client_id': agency.id}, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'credit_expired': (
                    hm.has_entries({
                        'items': hm.contains(hm.has_entries({
                            'invoice_id': exp_inv.id,
                            'invoice_eid': exp_inv.external_id,
                        })),
                        'total_count': 1,
                    })
                    if w_role else
                    hm.has_entries({
                        'items': hm.empty(),
                        'total_count': 0,
                    })
                ),
            }),
        )
