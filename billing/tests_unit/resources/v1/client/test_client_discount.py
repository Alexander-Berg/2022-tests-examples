# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library
standard_library.install_aliases()

import http.client as http
import hamcrest as hm
from decimal import Decimal as D

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_agency
from yb_snout_api.tests_unit.fixtures.contract import create_general_contract


class TestCaseClientDiscounts(TestCaseApiAppBase):
    BASE_API = '/v1/client/discount'

    def test_get_client_discounts(self, agency):
        contract = create_general_contract(
            client=agency,
            discount_commission=D('66'),
            services={7},
            firm_id=1,
            discount_policy_type=8,  # Фиксированная
            is_signed=self.test_session.now(),
        )

        response = self.test_client.get(self.BASE_API, {'client_id': agency.id})
        hm.assert_that(response.status_code, hm.equal_to(http.OK))

        data = response.get_json().get('data')
        hm.assert_that(
            data,
            hm.contains(
                hm.has_entries({
                    'discount': hm.has_entries({
                        'name': 'discount_commission',
                        'classname': 'ContractCommission',
                        'discount': '66.00',
                        'currency': 'RUR',
                        'type': 'fixed',
                        'without_taxes': True,
                    }),
                    'contract': hm.has_entries({
                        'type': 'GENERAL',
                        'client_id': agency.id,
                        'external_id': contract.external_id,
                        'id': contract.id,
                    }),
                }),
            ),
        )
