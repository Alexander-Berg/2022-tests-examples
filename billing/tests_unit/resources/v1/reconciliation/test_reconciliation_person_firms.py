# -*- coding: utf-8 -*-

from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import datetime
import pytest
import hamcrest as hm
import http.client as http

from balance import constants as cst

from brest.core.tests import security
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_role_client
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.person import create_person
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_invoice, create_custom_invoice
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.contract import create_general_contract


@pytest.fixture(name='view_inv_role')
def create_view_inv_role():
    return create_role(
        (
            cst.PermissionCode.VIEW_INVOICES,
            {cst.ConstraintTypes.firm_id: None, cst.ConstraintTypes.client_batch_id: None},
        ),
    )


@pytest.mark.smoke
class TestReconciliationPersonFirmsFromInvoices(TestCaseApiAppBase):
    BASE_API = '/v1/reconciliation/person-firms'

    def test_base_owning(self, client):
        security.set_roles([])
        security.set_passport_client(client)

        invoice = create_invoice(client=client)
        person = invoice.person
        firm = invoice.firm

        self.test_session.config.__dict__['RECONCILIATION_REPORT_FIRMS'] = [firm.id]

        res = self.test_client.get(self.BASE_API, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'items': hm.contains(
                    hm.has_entries({
                        'person': hm.has_entries({
                            'id': person.id,
                            'name': person.name,
                            'inn': person.inn,
                        }),
                        'firm': hm.has_entries({
                            'id': firm.id,
                            'label': firm.title,
                        }),
                        'contract': None,
                    }),
                ),
                'total_count': 1,
            }),
        )

    def test_w_contracts(self, client):
        security.set_roles([])
        security.set_passport_client(client)

        person = create_person(client=client)
        contract = create_general_contract(
            client=client,
            person=person,
            postpay=0,
            comission=0,
            firm_id=cst.FirmId.YANDEX_OOO,
            services={cst.ServiceId.DIRECT: 1},
            is_signed=self.test_session.now() - datetime.timedelta(days=1),
        )
        invoice = create_custom_invoice(
            client=client,
            person=person,
            contract=contract,
            firm_id=cst.FirmId.YANDEX_OOO,
            postpay=0,
        )
        invoice_wo_contract = create_invoice(client=client)

        self.test_session.config.__dict__['RECONCILIATION_REPORT_FIRMS'] = [cst.FirmId.YANDEX_OOO]

        res = self.test_client.get(
            self.BASE_API,
            is_admin=False,
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'items': hm.contains(
                    hm.has_entries({
                        'person': hm.has_entries({
                            'id': invoice_wo_contract.person.id,
                            'name': invoice_wo_contract.person.name,
                            'inn': invoice_wo_contract.person.inn,
                        }),
                        'contract': None,
                    }),
                    hm.has_entries({
                        'person': hm.has_entries({
                            'id': invoice.person.id,
                            'name': invoice.person.name,
                            'inn': invoice.person.inn,
                        }),
                        'contract': hm.has_entries({'id': contract.id, 'external_id': contract.external_id}),
                    }),
                ),
                'total_count': 2,
            }),
        )

    @pytest.mark.parametrize(
        'is_suspended',
        [False, True],
    )
    def test_suspended_contract(self, client, is_suspended):
        security.set_roles([])
        security.set_passport_client(client)

        person = create_person(client=client)
        contract = create_general_contract(
            client=client,
            person=person,
            postpay=0,
            comission=0,
            firm_id=cst.FirmId.YANDEX_OOO,
            services={cst.ServiceId.DIRECT: 1},
            is_signed=self.test_session.now() - datetime.timedelta(days=1),
        )
        invoice = create_custom_invoice(
            client=client,
            person=person,
            contract=contract,
            firm_id=cst.FirmId.YANDEX_OOO,
            postpay=0,
        )
        if is_suspended:
            contract.current_state().finish_dt = self.test_session.now()
            self.test_session.flush()

        self.test_session.config.__dict__['RECONCILIATION_REPORT_FIRMS'] = [cst.FirmId.YANDEX_OOO]

        res = self.test_client.get(self.BASE_API, {'w_contracts': True}, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'items': (
                    hm.contains(
                        hm.has_entries({
                            'contract': (
                                hm.has_entries({'id': contract.id, 'external_id': contract.external_id})
                            ),
                        }),
                    )
                    if not is_suspended else
                    hm.empty()
                ),
            }),
        )

    def test_wo_config(self, invoice):
        security.set_passport_client(invoice.client)
        self.test_session.config.__dict__['RECONCILIATION_REPORT_FIRMS'] = [cst.FirmId.MARKET]

        res = self.test_client.get(self.BASE_API, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'items': hm.empty(),
                'total_count': 0,
            }),
        )

    def test_person_config(self, client):
        security.set_passport_client(client)
        self.test_session.config.__dict__['RECONCILIATION_REPORT_FIRMS'] = [cst.FirmId.YANDEX_OOO]

        person_ph = create_person(client=client, type='ph')
        person_ur = create_person(client=client, type='ur')
        persons = [person_ur, person_ph]

        invs = [create_invoice(client=client, person=p) for p in persons]

        params = [
            (None, 2),
            ([], 0),
            (['ur'], 1),
        ]

        for person_types, person_count in params:
            self.test_session.config.__dict__['RECONCILIATION_REPORT_PERSON_TYPES'] = person_types

            res = self.test_client.get(self.BASE_API, is_admin=False)
            hm.assert_that(res.status_code, hm.equal_to(http.OK))

            data = res.get_json()['data']
            hm.assert_that(
                data,
                hm.has_entries({
                    'items': (
                        hm.contains_inanyorder(*[
                            hm.has_entries({'person': hm.has_entries({'id': p.id})})
                            for p in persons[:person_count]
                        ])
                        if person_count else
                        hm.empty()
                    ),
                    'total_count': person_count,
                }),
                'Error happened w params: types=%s, count=%s' % (person_types, person_count),
            )

    def test_wo_client(self):
        res = self.test_client.get(self.BASE_API, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.NOT_FOUND))

    @pytest.mark.permissions
    @pytest.mark.parametrize(
        'w_role, match_client, ans',
        [
            pytest.param(False, False, False, id='wo_role'),
            pytest.param(True, False, False, id='wrong_client'),
            pytest.param(True, True, True, id='match_client'),
        ],
    )
    def test_permission(self, client, view_inv_role, w_role, match_client, ans):
        roles = []
        if w_role:
            client_batch_id = create_role_client(client=client if match_client else None).client_batch_id
            roles.append((view_inv_role, {cst.ConstraintTypes.client_batch_id: client_batch_id}))
        security.set_roles(roles)

        invoice = create_invoice(client=client)
        person = invoice.person
        firm = invoice.firm

        self.test_session.config.__dict__['RECONCILIATION_REPORT_FIRMS'] = [firm.id]

        res = self.test_client.get(self.BASE_API, {'client_id': client.id}, is_admin=False)
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        if ans:
            hm.assert_that(
                data,
                hm.has_entries({
                    'items': hm.contains(
                        hm.has_entries({
                            'person': hm.has_entries({
                                'id': person.id,
                                'name': person.name,
                                'inn': person.inn,
                            }),
                            'firm': hm.has_entries({
                                'id': firm.id,
                                'label': firm.title,
                            }),
                        }),
                    ),
                    'total_count': 1,
                }),
            )
        else:
            hm.assert_that(
                data,
                hm.has_entries({
                    'items': hm.empty(),
                    'total_count': 0,
                }),
            )
