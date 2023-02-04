# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import http.client as http
import hamcrest as hm

from tests import object_builder as ob

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.common import create_bank


@pytest.mark.smoke
class TestCaseCheckBik(TestCaseApiAppBase):
    BASE_API = '/v1/common/check-bik'

    def test_get_bank_data(self, bank):
        res = self.test_client.get(
            self.BASE_API,
            {'bik': bank.bik},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        hm.assert_that(
            res.get_json()['data'],
            hm.has_entries({
                'bik': bank.bik,
                'city': bank.city,
                'cor_acc': bank.cor_acc,
                'hidden': 0,
                'id': bank.id,
                'info': bank.info,
                'name': bank.name,
                'swift': bank.swift,
                'update_dt': bank.update_dt.isoformat(),
            }),
        )

    def test_not_found(self):
        bik = ob.get_big_number()
        res = self.test_client.get(
            self.BASE_API,
            {'bik': bik},
        )
        hm.assert_that(res.status_code, hm.equal_to(http.NOT_FOUND))
        hm.assert_that(
            res.get_json(),
            hm.has_entries({
                'error': 'NOT_FOUND',
                'description': 'Object not found: Bank with bik=%s not found.' % bik,
            }),
        )
