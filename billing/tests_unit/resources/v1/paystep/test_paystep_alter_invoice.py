# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import hamcrest as hm
import http.client as http
import datetime

from balance import mapper, constants as cst, exc

from tests import object_builder as ob

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from yb_snout_api.tests_unit.fixtures.client import create_client
from yb_snout_api.tests_unit.fixtures.person import create_person
from yb_snout_api.tests_unit.fixtures.contract import create_contract
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice

# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import (
    create_admin_role,
    create_support_role,
)

@pytest.mark.smoke
@pytest.mark.permissions
class TestCaseAlterInvoice(TestCaseApiAppBase):
    BASE_API = '/v1/paystep/alter-invoice'

    @pytest.mark.parametrize(
        'perms, who_is, total_act_sum, pcp_alter, res',
        [
            ([], 'owner',  0, [True, False, False],  http.OK),
            ([], 'owner',  0, [False, True, False],  http.FORBIDDEN),
            ([], 'owner',  0, [False, False, True],  http.FORBIDDEN),
            ([], 'client', 0, [True, False, False], http.FORBIDDEN),
            ([cst.PermissionCode.ALTER_INVOICE_PAYSYS, cst.PermissionCode.ALTER_INVOICE_PERSON, cst.PermissionCode.ALTER_INVOICE_CONTRACT], 'admin', 0, [True, True, True],  http.OK),
            ([cst.PermissionCode.ALTER_INVOICE_PAYSYS, cst.PermissionCode.ALTER_INVOICE_PERSON],   'admin', 0, [True,  True,  True],  http.FORBIDDEN),
            ([cst.PermissionCode.ALTER_INVOICE_PAYSYS, cst.PermissionCode.ALTER_INVOICE_CONTRACT], 'admin', 0, [True,  True,  True],  http.FORBIDDEN),
            ([cst.PermissionCode.ALTER_INVOICE_PERSON, cst.PermissionCode.ALTER_INVOICE_CONTRACT], 'admin', 0, [True,  True,  True],  http.FORBIDDEN),
            ([cst.PermissionCode.ALTER_INVOICE_PAYSYS, cst.PermissionCode.ALTER_INVOICE_PERSON],   'admin', 0, [True,  False, True],  http.OK),
            ([cst.PermissionCode.ALTER_INVOICE_PAYSYS, cst.PermissionCode.ALTER_INVOICE_CONTRACT], 'admin', 0, [True,  True,  False], http.OK),
            ([cst.PermissionCode.ALTER_INVOICE_PERSON, cst.PermissionCode.ALTER_INVOICE_CONTRACT], 'admin', 0, [False, True,  True],  http.OK),
            ([cst.PermissionCode.ALTER_INVOICE_PAYSYS],   'admin', 0, [True,  False, False], http.OK),
            ([cst.PermissionCode.ALTER_INVOICE_CONTRACT], 'admin', 0, [False, True,  False], http.OK),
            ([cst.PermissionCode.ALTER_INVOICE_PERSON],   'admin', 0, [False, False, True],  http.OK),
            ([cst.PermissionCode.ALTER_INVOICE_PAYSYS], 'admin', 1, [True, False, False], http.OK),
            ([cst.PermissionCode.ALTER_INVOICE_PAYSYS], 'billing-support', 1, [True, False, False], http.OK),
            ([cst.PermissionCode.ALTER_INVOICE_CONTRACT], 'admin', 1, [False, True, False], http.INTERNAL_SERVER_ERROR),
            ([cst.PermissionCode.ALTER_INVOICE_CONTRACT], 'billing-support', 1, [False, True, False], http.OK),
            ([cst.PermissionCode.ALTER_INVOICE_PERSON], 'admin', 1, [False, False, True], http.INTERNAL_SERVER_ERROR),
            ([cst.PermissionCode.ALTER_INVOICE_PERSON], 'billing-support', 1, [False, False, True], http.OK),
        ],
        ids=lambda x: str(x),
    )
    def test_access(self, perms, who_is, total_act_sum, pcp_alter, res):
        roles = [ob.create_role(self.test_session, *perms)]
        if who_is == 'admin':
            roles.append(create_admin_role())
        elif who_is == 'billing-support':
            roles.append(create_support_role())
        security.set_roles(roles)
        client = create_client()
        if who_is == 'owner':
            security.set_passport_client(client)

        invoice = create_invoice(
            client=client,
            paysys=ob.Getter(mapper.Paysys, 1000).build(self.test_session).obj
        )
        invoice.total_act_sum = total_act_sum

        # altering paysys
        paysys = ob.Getter(mapper.Paysys, 1002).build(self.test_session).obj if pcp_alter[0] else invoice.paysys

        # altering person
        person = ob.PersonBuilder(client=client).build(self.test_session).obj if pcp_alter[2] else invoice.person

        # altering contract
        contract = create_contract(
            client=client,
            person=person,
            services={7},
            firm_id=1,
            is_signed=self.test_session.now()
        ) if pcp_alter[1] else invoice.contract

        iso_currency, payment_method_id, paysys_group_id, firm_id = paysys.iso_currency, paysys.payment_method_id, paysys.paysys_group.id, paysys.firm_id
        paysys_id, contract_id, person_id = paysys.id, contract.id if contract else None, person.id

        response = self.test_client.secure_post_json(
            self.BASE_API,
            data={
                'invoice_id': invoice.id,
                'contract_id': contract_id,
                'person_id': person_id,
                'iso_currency': iso_currency,
                'payment_method_id': payment_method_id,
                'paysys_group_id': paysys_group_id,
                'firm_id': firm_id,
            },
            is_admin=False,
        )

        hm.assert_that(response.status_code, hm.equal_to(res))

        if res == http.OK:
            hm.assert_that(response.get_json().get('data'), hm.has_entries({'id': invoice.id}))
            hm.assert_that(invoice.paysys_id, hm.equal_to(paysys_id))
            hm.assert_that(invoice.contract_id, hm.equal_to(contract_id))
            hm.assert_that(invoice.person_id, hm.equal_to(person.id))
        else:
            hm.assert_that(invoice.paysys_id,     hm.is_not(hm.equal_to(paysys_id)) if pcp_alter[0] else hm.equal_to(paysys_id))
            hm.assert_that(invoice.contract_id, hm.is_not(hm.equal_to(contract_id)) if pcp_alter[1] else hm.equal_to(contract_id))
            hm.assert_that(invoice.person_id,     hm.is_not(hm.equal_to(person_id)) if pcp_alter[2] else hm.equal_to(person_id))

    def test_w_ticket_id(self):
        invoice = create_invoice(order_count=1, turn_on=True)
        client = invoice.client
        order = invoice.consumes[0].order
        order.calculate_consumption(
            dt=datetime.datetime.today() - datetime.timedelta(days=1),
            stop=0,
            shipment_info={'Bucks': 1},
        )
        act, = invoice.generate_act(force=True)
        new_person = create_person(client=client)

        res = self.test_client.secure_post_json(
            self.BASE_API,
            {
                'invoice_id': invoice.id,
                'contract_id': invoice.contract_id,
                'person_id': new_person.id,
                'iso_currency': invoice.iso_currency,
                'payment_method_id': invoice.payment_method_id,
                'paysys_group_id': 0,
                'firm_id': invoice.firm_id,
                'ticket_id': 'ABC-1234',
            }
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        self.test_session.expire_all()
        assert invoice.person_id == new_person.id
        assert act.hidden == 4
        assert act.jira_id == 'ABC-1234'
