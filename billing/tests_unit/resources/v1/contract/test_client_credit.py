# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
import hamcrest
from decimal import Decimal as D

from balance import (
    core,
    constants as cst,
    mapper,
)
from tests import object_builder as ob
from tests.balance_tests.invoices.invoice_common import (
    create_credit_contract,
)

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.client import create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role, create_role


BANK_PAYSYS_ID = 1003
CARD_PAYSYS_ID = 1033


@pytest.fixture(name='view_contracts_role')
def create_view_contracts_role():
    return create_role((
        cst.PermissionCode.VIEW_CONTRACTS,
        {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
    ))


def create_contract(
        session,
        limit,
        personal_account=None,
        personal_account_fictive=None,
        client_limits=None,
        firm=cst.FirmId.YANDEX_OOO,
        client=None,
):
    client = client or ob.ClientBuilder.construct(session)
    person = ob.PersonBuilder.construct(session, client=client, type='ur')
    return create_credit_contract(
        session,
        commission=0,
        commission_type=None,
        credit_type=2,
        credit_limit_single=limit,
        personal_account=personal_account,
        personal_account_fictive=personal_account_fictive,
        client_limits=client_limits,
        firm=firm,
        client=client,
        person=person,
    )


def create_request(session, client, qty, agency=None):
    return ob.RequestBuilder(
        basket=ob.BasketBuilder(
            client=agency or client,
            rows=[ob.BasketItemBuilder(
                quantity=qty,
                order=ob.OrderBuilder(
                    client=client,
                    agency=agency,
                    product=ob.Getter(mapper.Product, cst.DIRECT_PRODUCT_RUB_ID),
                ),
            )],
        ),
    ).build(session).obj


def pay_on_credit(contract, request, paysys_id=BANK_PAYSYS_ID):
    invoice, = core.Core(contract.session).pay_on_credit(request.id, paysys_id, contract.person_id, contract.id)
    return invoice


class TestClientCreditLimit(TestCaseApiAppBase):
    BASE_API = '/v1/contract/client-credit/limit'

    def test_empty(self):
        client = ob.ClientBuilder.construct(self.test_session)

        response = self.test_client.get(self.BASE_API, data={'client_id': client.id})
        assert response.status_code == http.OK
        assert response.json['data'] == []

    def test_fictive(self):
        session = self.test_session

        contract = create_contract(session, 666, None, None)
        contract.client.assign_agency_status(is_agency=True)
        subclient = ob.ClientBuilder.construct(session, agency=contract.client)
        request = create_request(session, subclient, D('100.10'), contract.client)
        pay_on_credit(contract, request)

        response = self.test_client.get(self.BASE_API, data={'client_id': contract.client_id})
        hamcrest.assert_that(response.status_code, http.OK)
        hamcrest.assert_that(
            response.json['data'],
            hamcrest.contains(
                hamcrest.has_entries(
                    contract=hamcrest.has_entries(
                        id=contract.id,
                    ),
                    contract_credit_invoice_type=3,
                    contract_personal_account_invoice_id=None,
                    contract_credit_limit=[{'limit': '666.00', 'id': 0, 'spent': '100.10', 'available': '565.90'}],
                    contract_clients_credit_limit=[],
                ),
            ),
        )

    @pytest.mark.parametrize(
        'personal_account_fictive, invoice_type',
        [
            pytest.param(0, 7, id='old'),
            pytest.param(1, 8, id='new'),
        ],
    )
    def test_single_pa(self, personal_account_fictive, invoice_type):
        session = self.test_session

        contract = create_contract(session, 666, 1, personal_account_fictive)
        contract.client.assign_agency_status(is_agency=True)
        subclient = ob.ClientBuilder.construct(session, agency=contract.client)
        request = create_request(session, subclient, D('100.10'), contract.client)
        invoice = pay_on_credit(contract, request)

        response = self.test_client.get(self.BASE_API, data={'client_id': contract.client_id})
        hamcrest.assert_that(response.status_code, http.OK)
        hamcrest.assert_that(
            response.json['data'],
            hamcrest.contains(
                hamcrest.has_entries(
                    contract=hamcrest.has_entries(
                        id=contract.id,
                    ),
                    contract_credit_invoice_type=invoice_type,
                    contract_personal_account_invoice_id=invoice.id,
                    contract_credit_limit=[{'limit': '666.00', 'id': 0, 'spent': '100.10', 'available': '565.90'}],
                    contract_clients_credit_limit=[],
                ),
            ),
        )

    def test_multiple_pas(self):
        session = self.test_session

        contract = create_contract(session, 666, 1, 1)
        contract.client.assign_agency_status(is_agency=True)
        subclient = ob.ClientBuilder.construct(session, agency=contract.client)

        for paysys_id in [BANK_PAYSYS_ID, CARD_PAYSYS_ID]:
            request = create_request(session, subclient, D('100.33'), contract.client)
            pay_on_credit(contract, request, paysys_id)

        response = self.test_client.get(self.BASE_API, data={'client_id': contract.client_id})
        hamcrest.assert_that(response.status_code, http.OK)
        hamcrest.assert_that(
            response.json['data'],
            hamcrest.contains(
                hamcrest.has_entries(
                    contract=hamcrest.has_entries(
                        id=contract.id,
                    ),
                    contract_credit_invoice_type=8,
                    contract_personal_account_invoice_id=None,
                    contract_credit_limit=[{'limit': '666.00', 'id': 0, 'spent': '200.66', 'available': '465.34'}],
                    contract_clients_credit_limit=[],
                ),
            ),
        )

    def test_client_limits(self):
        session = self.test_session

        contract = create_contract(session, 666, 1, 1)
        contract.client.assign_agency_status(is_agency=True)
        subclient1 = ob.ClientBuilder.construct(session, agency=contract.client)
        subclient2 = ob.ClientBuilder.construct(session, agency=contract.client)
        contract.col0.client_limits = {
            subclient1.id: {'currency': 'RUR', 'client_limit': 6666},
            subclient2.id: {'currency': 'RUR', 'client_limit': 7777},
        }
        session.flush()

        invoice = pay_on_credit(
            contract,
            create_request(session, subclient1, D('100.10'), contract.client),
            BANK_PAYSYS_ID,
        )
        pay_on_credit(
            contract,
            create_request(session, subclient2, D('30.55'), contract.client),
            BANK_PAYSYS_ID,
        )
        pay_on_credit(
            contract,
            create_request(session, subclient2, D('80.35'), contract.client),
            CARD_PAYSYS_ID,
        )

        response = self.test_client.get(self.BASE_API, data={'client_id': contract.client_id})
        hamcrest.assert_that(response.status_code, http.OK)
        hamcrest.assert_that(
            response.json['data'],
            hamcrest.contains(
                hamcrest.has_entries(
                    contract=hamcrest.has_entries(
                        id=contract.id,
                    ),
                    contract_credit_invoice_type=8,
                    contract_personal_account_invoice_id=None,
                    contract_credit_limit=[{'limit': '666.00', 'id': 0, 'spent': '0.00', 'available': '666.00'}],
                    contract_clients_credit_limit=hamcrest.contains_inanyorder(
                        hamcrest.has_entries(
                            available='6565.90',
                            client=hamcrest.has_entries(id=subclient1.id),
                            credit_invoice_type=8,
                            limit=hamcrest.has_entries(
                                currency='RUR',
                                value='6666.00',
                            ),
                            spent='100.10',
                            personal_account_invoice_id=invoice.id,
                        ),
                        hamcrest.has_entries(
                            available='7666.10',
                            client=hamcrest.has_entries(id=subclient2.id),
                            credit_invoice_type=8,
                            limit=hamcrest.has_entries(
                                currency='RUR',
                                value='7777.00',
                            ),
                            spent='110.90',
                            personal_account_invoice_id=None,
                        ),
                    ),
                ),
            ),
        )

    @pytest.mark.permissions
    def test_permissions(self, admin_role, view_contracts_role):
        session = self.test_session

        client = ob.ClientBuilder.construct(session)
        client_batch_id = create_role_client(client).client_batch_id

        client.assign_agency_status(is_agency=True)
        subclient = ob.ClientBuilder.construct(session, agency=client)

        roles = [
            admin_role,
            (
                view_contracts_role,
                {
                    cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO,
                    cst.ConstraintTypes.client_batch_id: client_batch_id,
                },
            ),
            (
                view_contracts_role,
                {
                    cst.ConstraintTypes.firm_id: cst.FirmId.MARKET,
                    cst.ConstraintTypes.client_batch_id: client_batch_id,
                },
            ),
        ]
        security.set_roles(roles)

        contract1 = create_contract(session, 666, firm=cst.FirmId.YANDEX_OOO, client=client)
        contract2 = create_contract(session, 666, firm=cst.FirmId.MARKET, client=client)
        contract3 = create_contract(session, 666, firm=cst.FirmId.DRIVE, client=client)
        for contract in [contract1, contract2, contract3]:
            request = create_request(session, subclient, 100, client)
            pay_on_credit(contract, request)

        response = self.test_client.get(self.BASE_API, data={'client_id': client.id})

        hamcrest.assert_that(response.status_code, http.OK)
        hamcrest.assert_that(
            response.json['data'],
            hamcrest.contains_inanyorder(
                hamcrest.has_entries(
                    contract=hamcrest.has_entries(id=contract1.id),
                ),
                hamcrest.has_entries(
                    contract=hamcrest.has_entries(id=contract2.id),
                ),
            ),
        )

    @pytest.mark.permissions
    def test_wrong_client_constraint(self, admin_role, view_contracts_role, role_client):
        session = self.test_session
        client = role_client.client
        client_batch_id = create_role_client().client_batch_id
        roles = [
            admin_role,
            (
                view_contracts_role,
                {
                    cst.ConstraintTypes.firm_id: cst.FirmId.YANDEX_OOO,
                    cst.ConstraintTypes.client_batch_id: client_batch_id,
                },
            ),
        ]
        security.set_roles(roles)

        contract = create_contract(session, 666, firm=cst.FirmId.YANDEX_OOO, client=role_client.client)

        client.assign_agency_status(is_agency=True)
        subclient = ob.ClientBuilder.construct(session, agency=client)
        request = create_request(session, subclient, 100, client)
        pay_on_credit(contract, request)

        response = self.test_client.get(self.BASE_API, data={'client_id': client.id})
        hamcrest.assert_that(response.status_code, http.FORBIDDEN)
