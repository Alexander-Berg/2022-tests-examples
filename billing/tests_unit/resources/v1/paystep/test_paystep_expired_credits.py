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
from yb_snout_api.resources.v1.paystep import enums

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, get_client_role, create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_agency
from yb_snout_api.tests_unit.fixtures.firm import create_firm
from yb_snout_api.tests_unit.fixtures.common import create_paysys
from yb_snout_api.tests_unit.fixtures.contract import create_credit_contract


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


def create_invoice(
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
class TestExpiredCredits(TestCaseApiAppBase):
    BASE_API = '/v1/paystep/expired-credits'

    @mock.patch('balance.mncloselib.get_task_last_status', return_value='resolved')  # не падать первого числа
    @pytest.mark.parametrize(
        'expired_type, expired_days, expired_sum, nearly_expired_sum',
        [
            pytest.param(enums.CreditExpiringType.OK, 30, None, None, id='ok'),
            pytest.param(enums.CreditExpiringType.NEARLY_EXPIRED, 2, None, '100.00', id='nearly_expired'),
            pytest.param(enums.CreditExpiringType.EXPIRED, -1, '100.00', None, id='expired'),
        ],
    )
    def test_base(self, _mock_mnclose, agency, expired_type, expired_days, expired_sum, nearly_expired_sum):
        agency.set_currency(cst.ServiceId.DIRECT, 'RUB', self.test_session.now(), cst.CONVERT_TYPE_COPY)
        client = create_client(agency=agency)
        firm = create_firm(postpay=1, default_currency='RUR')

        dt = self.test_session.now() + datetime.timedelta(days=expired_days)
        contract = create_credit_contract_(agency, firm)
        paysys = create_paysys(contract.firm.id)
        inv, y_inv = create_invoice(self.test_session, contract, client, paysys, qty=100, close_dt=dt)
        res = self.test_client.get(self.BASE_API, {'contract_id': contract.id, 'expired_type': expired_type.name})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'total_sum': '100.00',
                'expired_sum': expired_sum,
                'nearly_expired_sum': nearly_expired_sum,
                'invoices': hm.contains(
                    hm.has_entries({
                        'invoice_id': y_inv.id,
                        'invoice_eid': y_inv.external_id,
                        'deadline_dt': hm.not_none(),
                        'debt': '100.00',
                        'time_to_live': expired_days - 1,
                        'iso_currency': 'RUB',
                        'expired': (
                            expired_type.name
                            if expired_type is not enums.CreditExpiringType.ALL
                            else enums.CreditExpiringType.EXPIRED.name
                        ),
                    }),
                ),
            }),
        )

    def test_all_types(self, agency):
        agency.set_currency(cst.ServiceId.DIRECT, 'RUB', self.test_session.now(), cst.CONVERT_TYPE_COPY)
        client = create_client(agency=agency)
        firm = create_firm(postpay=1, default_currency='RUR')

        now = self.test_session.now()
        contract = create_credit_contract_(agency, firm)
        paysys = create_paysys(contract.firm.id)
        create_invoice(self.test_session, contract, client, paysys, qty=1, close_dt=now)
        create_invoice(self.test_session, contract, client, paysys, qty=10, close_dt=now + datetime.timedelta(days=2))
        create_invoice(self.test_session, contract, client, paysys, qty=100, close_dt=now + datetime.timedelta(days=30))

        y_inv_expired, y_inv_nearly, y_inv_ok = filter(lambda i: isinstance(i, mapper.YInvoice), contract.invoices)

        res = self.test_client.get(
            self.BASE_API,
            {'contract_id': contract.id, 'expired_type': enums.CreditExpiringType.ALL.name},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'total_sum': '111.00',
                'expired_sum': '1.00',
                'nearly_expired_sum': '10.00',
                'invoices': hm.contains_inanyorder(
                    hm.has_entries({
                        'invoice_id': y_inv_ok.id,
                        'debt': '100.00',
                        'time_to_live': 29,
                        'expired': enums.CreditExpiringType.OK.name,
                    }),
                    hm.has_entries({
                        'invoice_id': y_inv_nearly.id,
                        'debt': '10.00',
                        'time_to_live': 1,
                        'expired': enums.CreditExpiringType.NEARLY_EXPIRED.name,
                    }),
                    hm.has_entries({
                        'invoice_id': y_inv_expired.id,
                        'debt': '1.00',
                        'time_to_live': -1,
                        'expired': enums.CreditExpiringType.EXPIRED.name,
                    }),
                ),
            }),
        )


@pytest.mark.permissions
class TestExpiredCreditsPermissions(TestCaseApiAppBase):
    BASE_API = '/v1/paystep/expired-credits'

    @pytest.mark.parametrize(
        'allow_type',
        [
            'forbidden_admin_ui',
            'forbidden_client_ui',
            'from_admin_ui',
            'from_client_ui',
        ],
    )
    def test_get_expired_credits_permission(self, admin_role, view_inv_role, agency, allow_type):
        allow = 'forbidden' not in allow_type
        is_admin = 'admin_ui' in allow_type

        agency.set_currency(cst.ServiceId.DIRECT, 'RUB', self.test_session.now(), cst.CONVERT_TYPE_COPY)
        client = create_client(agency=agency)
        firm = create_firm(postpay=1, default_currency='RUR')

        if not is_admin:
            security.set_roles([])
            security.set_passport_client(agency if allow else None)

        else:
            security.set_passport_client(None)
            security.set_roles([
                admin_role,
                (view_inv_role, {cst.ConstraintTypes.firm_id: firm.id if allow else cst.FirmId.YANDEX_OOO}),
            ])

        contract = create_credit_contract_(agency, firm)
        paysys = create_paysys(contract.firm.id)
        inv, y_inv = create_invoice(self.test_session, contract, client, paysys, qty=100, close_dt=self.test_session.now())

        res = self.test_client.get(self.BASE_API, {'contract_id': contract.id}, is_admin=is_admin)
        hm.assert_that(res.status_code, hm.equal_to(http.OK if allow else http.FORBIDDEN))

        if allow:
            data = res.get_json()['data']
            hm.assert_that(
                data,
                hm.has_entries({
                    'total_sum': '100.00',
                    'expired_sum': '100.00',
                    'nearly_expired_sum': None,
                    'invoices': hm.contains(
                        hm.has_entries({
                            'invoice_id': y_inv.id,
                            'invoice_eid': y_inv.external_id,
                            'deadline_dt': hm.not_none(),
                            'debt': '100.00',
                            'time_to_live': -1,
                            'iso_currency': 'RUB',
                            'expired': enums.CreditExpiringType.EXPIRED.name,
                        }),
                    ),
                }),
            )
