# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import hamcrest as hm
import http.client as http

from tests import object_builder as ob

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences


@pytest.mark.smoke
class TestCaseFPSBanksList(TestCaseApiAppBase):
    BASE_API = u'/v1/person/fps-banks'

    def test_fps_banks_list(self):
        fps_banks_params = [
            {
                'front_id': 10,
                'processing_bank': 'raif',
                'cc': 'SBER-PARADISE',
                'name': 'Райское отделение Cбера',
                'hidden': 1,
            },
            {
                'front_id': 10,
                'processing_bank': 'raif',
                'cc': 'SBER-HELL',
                'name': 'Адское отделение Cбера',
                'hidden': 0,
            },
            {
                'front_id': 11,
                'processing_bank': 'banka_3_litra',
                'cc': 'PARTNER-BALANCE-BANK',
                'name': 'Партнерское отделение банка Баланса',
                'hidden': 0,
            },
        ]

        for params in fps_banks_params:
            ob.FPSBankBuilder.construct(self.test_session, **params)
        res = self.test_client.get(
            self.BASE_API,
            params={'front_id': 10},
        )

        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        def construct_expected_row(in_row):
            expected_keys = ['cc', 'name', 'hidden']
            return {key: in_row[key] for key in expected_keys}

        # порядок важен
        expected_data = [construct_expected_row(param) for param in (fps_banks_params[1], fps_banks_params[0])]
        hm.assert_that(
            res.json['data'],
            hm.has_entries(
                total_count=len(expected_data),
                items=hm.contains(*[
                    hm.has_entries(ed) for ed in expected_data
                ]),
            ),
        )
