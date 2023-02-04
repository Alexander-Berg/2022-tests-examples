# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import http.client as http
import pytest
from decimal import Decimal as D
from hamcrest import (
    assert_that,
    equal_to,
)

from balance.constants import ServiceId, FirmId
from yb_snout_api.tests_unit.base import TestCaseApiAppBase

# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import (
    create_client,
    create_client_with_intercompany,
    create_agency,
)


@pytest.mark.smoke
class TestSetOverdraft(TestCaseApiAppBase):
    BASE_API = u'/assessor/client/set-overdraft'
    OVERDRAFT_LIMIT = D('10013')

    def test_set_overdraft(self, client):
        response = self.test_client.secure_post(self.BASE_API,
                                                data={
                                                    'service_id': ServiceId.DIRECT,
                                                    'client_id': client.id,
                                                    'limit': self.OVERDRAFT_LIMIT,
                                                    'firm_id': FirmId.YANDEX_OOO})

        assert_that(response.status_code, equal_to(http.OK), 'response code must be OK')
        assert_that(client.overdraft_limit, equal_to(self.OVERDRAFT_LIMIT))
