# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import hamcrest as hm
import http.client as http

from balance import (
    core,
    constants as cst,
    mapper,
)
from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.firm import create_firm
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role
from yb_snout_api.tests_unit.fixtures.contract import create_credit_contract


@pytest.fixture(name='view_contract_role')
def create_view_contract_role():
    return create_role(
        (
            cst.PermissionCode.VIEW_CONTRACTS,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.mark.slow
class TestCaseClientAvailableCredits(TestCaseApiAppBase):
    BASE_API = u'/v1/client/available-credits'

    def _create_clients(self):
        agency = create_client()
        agency.set_currency(
            cst.ServiceId.DIRECT,
            'RUB',
            self.test_session.now(),
            cst.CONVERT_TYPE_COPY,
        )
        agency.assign_agency_status(is_agency=True)
        client = create_client(agency=agency)
        return agency, client

    def _create_credit_contracts(self, agency, client, firms):
        common_contract_params = dict(
            commission=0,
            commission_type=None,
            credit_type=2,
            credit_limit_single=666,
            personal_account=1,
            personal_account_fictive=1,
            client_limits=None,
            client=agency,
            person=ob.PersonBuilder(client=agency, type='ur'),
        )

        contracts = []
        for firm in firms:
            contract = create_credit_contract(firm=firm.id, **common_contract_params)
            contracts.append(contract)

        for contract in contracts:
            paysys = ob.PaysysBuilder.construct(
                self.test_session,
                firm_id=contract.firm.id,
                payment_method_id=cst.PaymentMethodIDs.bank,
                iso_currency='RUB',
                currency=mapper.fix_crate_base_cc('RUB'),
                extern=1,
            )
            request = ob.RequestBuilder.construct(
                self.test_session,
                basket=ob.BasketBuilder(
                    client=agency,
                    rows=[ob.BasketItemBuilder(
                        quantity=100,
                        order=ob.OrderBuilder(
                            client=client,
                            agency=agency,
                            product=ob.Getter(mapper.Product, cst.DIRECT_PRODUCT_RUB_ID),
                        ),
                    )],
                ),
            )
            invoice, = core.Core(self.test_session).pay_on_credit(request.id, paysys.id, contract.person_id, contract.id)
            invoice.close_invoice(self.test_session.now())

        return contracts

    @pytest.mark.permissions
    def test_get_clients_credit_available_permissions(self, admin_role, view_contract_role):
        agency, client = self._create_clients()
        client_batch_id = create_role_client(client=agency).client_batch_id

        firm1 = create_firm(postpay=1, default_currency='RUR')
        firm2 = create_firm(postpay=1, default_currency='RUR')
        firm3 = create_firm(postpay=1, default_currency='RUR')

        roles = [
            admin_role,
            (view_contract_role, {cst.ConstraintTypes.firm_id: firm1.id, cst.ConstraintTypes.client_batch_id: client_batch_id}),
            (view_contract_role, {cst.ConstraintTypes.firm_id: firm2.id, cst.ConstraintTypes.client_batch_id: client_batch_id}),
        ]
        security.set_roles(roles)

        contracts = self._create_credit_contracts(agency, client, [firm1, firm2, firm3])

        invoice1 = filter(lambda i: isinstance(i, mapper.YInvoice), contracts[0].invoices)[0]
        invoice2 = filter(lambda i: isinstance(i, mapper.YInvoice), contracts[1].invoices)[0]

        res = self.test_client.get(
            self.BASE_API,
            {'client_id': agency.id},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data')
        hm.assert_that(
            data,
            hm.contains_inanyorder(*[
                hm.has_entries({
                    'nearly_exceeded': True,
                    'id': c.id,
                    'external_id': c.external_id,
                    'has_limits': True,
                    'exceeded': False,
                    'exceeded_invoices': hm.empty(),
                    'nearly_exceeded_invoices': hm.contains(
                        hm.has_entries({
                            'id': inv.id,
                            'external_id': inv.external_id,
                        }),
                    ),
                })
                for c, inv in zip(contracts[:2], [invoice1, invoice2])
            ]),
        )

    @pytest.mark.permissions
    def test_get_clients_credit_prohibited_by_client(self, admin_role, view_contract_role):
        agency, client = self._create_clients()
        create_role_client(client=agency).client_batch_id
        client_batch_id_2 = create_role_client().client_batch_id
        firm = create_firm(postpay=1, default_currency='RUR')

        roles = [
            admin_role,
            (view_contract_role, {cst.ConstraintTypes.firm_id: firm.id, cst.ConstraintTypes.client_batch_id: client_batch_id_2}),
        ]
        security.set_roles(roles)

        self._create_credit_contracts(agency, client, [firm])

        res = self.test_client.get(
            self.BASE_API,
            {'client_id': agency.id},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data')
        hm.assert_that(data, hm.empty())
