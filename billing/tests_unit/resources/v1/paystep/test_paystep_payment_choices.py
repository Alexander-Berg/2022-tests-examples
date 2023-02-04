# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import hamcrest as hm
import http.client as http

from balance import constants as cst, core
from tests import object_builder as ob

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_agency
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.person import create_person
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.invoice import create_request
from yb_snout_api.tests_unit.fixtures.contract import create_general_contract


@pytest.mark.smoke
class TestPaystepPaymentChoices(TestCaseApiAppBase):
    BASE_API = '/v1/paystep/payment-choices'

    def test_agency_w_contract(self):
        agency = create_agency()
        client = create_client(agency=agency)
        contract = create_general_contract(client=agency, services={cst.ServiceId.DIRECT}, firm_id=cst.FirmId.YANDEX_OOO)
        request = create_request(agency=agency, client=client)

        res = self.test_client.get(
            self.BASE_API,
            {
                'request_id': request.id,
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'invoice': None,
                'request': hm.has_entries({
                    'id': request.id,
                    'total_qty': '30',
                    'client': hm.has_entries({
                        'id': agency.id,
                        'name': agency.name,
                        'is_agency': True,
                    }),
                    'direct_payment': True,
                    'is_unmoderated': False,
                    'is_available_promo_codes': False,
                }),
                'choices': hm.has_item(
                    hm.has_entries({
                        'has_europe_ag_alert': False,
                        'payment_method': hm.has_entries({
                            'payment_limit': None,
                            'is_trust_api': False,
                            'code': 'bank',
                            'id': 1001,
                            'name': 'Bank Payment',
                        }),
                        'disable_reason': None,
                        'offer_id': None,
                        'contract': hm.has_entries({
                            'can_have_endbuyer': False,
                            'needs_offer_confirmation': False,
                            'payment_type': 3,
                            'external_id': contract.external_id,
                            'id': contract.id,
                        }),
                        'paysys_group_id': 0,
                        'person': hm.has_entries({
                            'id': contract.person.id,
                        }),
                        'firm_id': cst.FirmId.YANDEX_OOO,
                        'need_receipt': False,
                        'iso_currency': 'RUB',
                        'trust_paymethods': None,
                    }),
                ),
                'disabled_contract_choices': hm.empty(),
                'disabled_person_choices': hm.empty(),
                'disabled_payment_method_choices': hm.empty(),
                'is_auto_overdraft_available': None,
                'additional_info': hm.has_entries({
                    'is_agency_w_active_contracts': True,
                }),
            }),
        )

    def test_default_choice_wo_person(self, client):
        order = ob.OrderBuilder.construct(self.test_session, client=client)
        core.Core(self.test_session).pay_order_cc(order.id, cst.CERT_PAYSYS_CC, 100)
        person = create_person(client=client)
        request = create_request(client=client)
        res = self.test_client.get(
            self.BASE_API,
            {
                'request_id': request.id,
            },
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'request': hm.has_entries({'id': request.id}),
                'choices': hm.has_item(
                    hm.has_entries({
                        'has_europe_ag_alert': False,
                        'payment_method': hm.has_entries({
                            'payment_limit': None,
                            'is_trust_api': False,
                            'code': 'bank',
                            'id': 1001,
                            'name': 'Bank Payment',
                        }),
                        'disable_reason': None,
                        'offer_id': 38,
                        'contract': None,
                        'paysys_group_id': 0,
                        'person': hm.has_entries({
                            'id': person.id,
                        }),
                        'firm_id': cst.FirmId.YANDEX_OOO,
                        'need_receipt': False,
                        'iso_currency': 'RUB',
                        'trust_paymethods': None,
                    }),
                ),
                'disabled_contract_choices': hm.empty(),
                'disabled_person_choices': hm.empty(),
                'disabled_payment_method_choices': hm.empty(),
            }),
        )

    def test_person(self, client):
        person = create_person(client=client, email='test@snout-api.ru;Pypkin@yandex.net', inn='990099')
        request = create_request(client=client)
        res = self.test_client.get(self.BASE_API, {'request_id': request.id})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json()['data']
        hm.assert_that(
            data,
            hm.has_entries({
                'request': hm.has_entries({'id': request.id}),
                'choices': hm.has_item(
                    hm.has_entries({
                        'iso_currency': 'RUB',
                        'firm_id': cst.FirmId.YANDEX_OOO,
                        'contract': None,
                        'payment_method': hm.has_entries({'code': 'bank'}),
                        'person': hm.has_entries({
                            'id': person.id,
                            'name': person.name,
                            'inn': person.inn,
                            'legal_entity': False,
                            'resident': True,
                            'type': 'ph',
                            'region_id': cst.RegionId.RUSSIA,
                            'type_name': person.person_category.name,
                            'invalid_address': False,
                            'email': 'test@snout-api.ru',
                        }),
                    }),
                ),
            }),
        )
