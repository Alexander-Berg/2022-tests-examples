# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from future import standard_library

standard_library.install_aliases()

import pytest
import mock
import http.client as http
import hamcrest as hm

from brest.core.tests import security
from yb_snout_api.utils import clean_dict
from brest.core.tests.base import yb_test_app
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.permissions import create_admin_role
# noinspection PyUnresolvedReferences
from yb_snout_api.tests_unit.fixtures.service import create_service


@pytest.mark.smoke
class TestCaseServiceList(TestCaseApiAppBase):
    BASE_API = '/v1/service/list'

    @pytest.mark.parametrize(
        'is_spendable',
        [True, False, None],
    )
    def test_services_spendable(self, admin_role, is_spendable):
        service = create_service(
            cc='newwww_snout_service',
            display_name='Новый тестовый сервис',
            show_to_user=1,
            is_spendable=False,
            url_orders='https://wiki.yandex-team.ru/users/natabers/snout-2020-03/',
        )
        spendable_service = create_service(
            cc='spendable_test_service',
            display_name='Boom baby!',
            show_to_user=1,
            is_spendable=True,
            url_orders='https://github.yandex-team.ru/Billing',
        )
        self.test_session.flush()

        security.set_roles([admin_role])
        response = self.test_client.get(self.BASE_API, clean_dict({'is_spendable': is_spendable}))
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        service_match = hm.has_item(hm.has_entries(
            {'id': service.id, 'cc': service.cc, 'name': service.display_name, 'url_orders': service.url_orders},
        ))
        spendable_service_match = hm.has_item(hm.has_entries(
            {'id': spendable_service.id, 'cc': spendable_service.cc, 'name': spendable_service.display_name, 'url_orders': spendable_service.url_orders},
        ))

        if is_spendable is True:
            service_match = hm.not_(service_match)
        elif is_spendable is False:
            spendable_service_match = hm.not_(spendable_service_match)

        hm.assert_that(
            data,
            hm.all_of(
                service_match,
                spendable_service_match,
            ),
        )

    @pytest.mark.parametrize(
        'is_thirdparty',
        [True, False, None],
    )
    def test_services_thirdparty(self, admin_role, is_thirdparty):
        service = create_service(
            cc='newwww_snout_service',
            display_name='Новый тестовый сервис',
            show_to_user=1,
            is_spendable=False,
            url_orders='https://wiki.yandex-team.ru/users/natabers/snout-2020-03/',
        )
        thirdparty_service = create_service(
            cc='spendable_test_service',
            display_name='Boom baby!',
            show_to_user=1,
            is_spendable=True,
            url_orders='https://github.yandex-team.ru/Billing',
            thirdparty={
                'enabled': 1,
            },
        )
        self.test_session.flush()

        security.set_roles([admin_role])
        response = self.test_client.get(self.BASE_API, clean_dict({'is_thirdparty': is_thirdparty}))
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        service_match = hm.has_item(hm.has_entries(
            {'id': service.id, 'cc': service.cc, 'name': service.display_name, 'url_orders': service.url_orders},
        ))
        thirdparty_service_match = hm.has_item(hm.has_entries(
            {'id': thirdparty_service.id, 'cc': thirdparty_service.cc, 'name': thirdparty_service.display_name, 'url_orders': thirdparty_service.url_orders},
        ))

        if is_thirdparty is True:
            service_match = hm.not_(service_match)
        elif is_thirdparty is False:
            thirdparty_service_match = hm.not_(thirdparty_service_match)

        hm.assert_that(
            data,
            hm.all_of(
                service_match,
                thirdparty_service_match,
            ),
        )

    @pytest.mark.parametrize(
        'test_env, env_type, is_ok',
        [
            (0, 'prod', True),
            (0, 'test', True),
            (1, 'prod', False),
            (1, 'test', True),
        ],
    )
    def test_get_services_test_env(self, service, test_env, env_type, is_ok):
        service.balance_service.test_env = test_env
        service.balance_service.show_to_user = 1
        service.balance_service.in_contract = 1
        self.test_session.flush()

        with mock.patch.object(yb_test_app, 'get_current_env_type', return_value=env_type):
            response = self.test_client.get(self.BASE_API)
        hm.assert_that(response.status_code, hm.equal_to(http.OK), 'response code must be OK')

        data = response.get_json().get('data')
        service_match = hm.has_item(hm.has_entries({'id': service.id}))
        if not is_ok:
            service_match = hm.not_(service_match)

        hm.assert_that(data, service_match)
