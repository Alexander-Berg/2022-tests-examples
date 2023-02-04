# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import datetime
import pytest
import hamcrest as hm

import http.client as http

from balance import constants as cst

from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.client import create_client, create_client_service_data


@pytest.mark.smoke
class TestCaseClientServiceData(TestCaseApiAppBase):
    BASE_API = u'/v1/client/service-data'

    def test_get_data(self, client):
        now = self.test_session.now()
        day_ago = now - datetime.timedelta(days=1)
        two_days_ago = now - datetime.timedelta(days=2)

        create_client_service_data(
            client=client,
            service_id=cst.ServiceId.MARKET,
            currency='RUB',
            migrate_to_currency_dt=two_days_ago,
        )
        create_client_service_data(
            client=client,
            service_id=cst.ServiceId.DISK,
            currency='USD',
            migrate_to_currency_dt=day_ago,
        )
        create_client_service_data(  # invalid service data
            client=client,
            service_id=-666,
            currency='EUR',
            migrate_to_currency_dt=now,
        )

        res = self.test_client.get(self.BASE_API, {'client_id': client.id})
        hm.assert_that(res.status_code, hm.equal_to(http.OK))

        data = res.get_json().get('data')
        hm.assert_that(
            data,
            hm.contains(
                hm.has_entries({
                    'name': 'Яндекс-Маркет',
                    'iso_currency': 'RUB',
                    'migrate_dt': two_days_ago.isoformat(),
                }),
                hm.has_entries({
                    'name': 'Yandex.Disk',
                    'iso_currency': 'USD',
                    'migrate_dt': day_ago.isoformat(),
                }),
            ),
        )
