# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()

import pytest
from hamcrest import (
    assert_that,
    has_entries,
    same_instance,
    is_not,
)

from yb_snout_api.tests_unit.base import TestCaseApiAppBase


class TestCaseAuth(TestCaseApiAppBase):
    PASSPORT_UID = 3000455966

    @pytest.mark.failing_locally
    def test_tvm2_user_ticket(self, mocker):
        from brest.core.auth.tvm2_base import Tvm2BaseAuthMethod
        from brest.core.auth.tvm2_user_ticket import Tvm2UserTicketAuthMethod
        from brest.utils.config import get_tvm2_config
        from tvmauth import BlackboxTvmId as BlackboxClientId
        from tvmauth import TvmClient, BlackboxEnv
        import tvmauth.deprecated as tad

        mocker.patch.object(Tvm2BaseAuthMethod, '_get_blackbox_client', return_value=BlackboxClientId.Test)

        conf_snout_api = get_tvm2_config('SnoutApi', 'paysys-balance-snout-api')
        conf_snout_proxy = get_tvm2_config('SnoutApi', 'paysys-balance-snout-proxy')

        tvm_service_ticket = self._cmd_tvm_knife(
            'service',
            '-s', conf_snout_proxy['id'],
            '-d', conf_snout_api['id'],
        )
        tvm_user_ticket = self._cmd_tvm_knife(
            'user',
            '-u', self.PASSPORT_UID,
        )

        headers = {
            'X-Ya-Service-Ticket': tvm_service_ticket,
            'X-Ya-User-Ticket': tvm_user_ticket,
        }

        mocker.patch.object(
            TvmClient,
            'check_service_ticket',
            return_value=tad.ServiceContext(
                int(conf_snout_api['id']),
                None,
                self._cmd_tvm_knife('public_keys')
            ).check(tvm_service_ticket)
        )

        mocker.patch.object(
            TvmClient,
            'check_user_ticket',
            return_value=tad.UserContext(
                BlackboxEnv.Test,
                self._cmd_tvm_knife('public_keys')
            ).check(tvm_user_ticket)
        )

        auth_method = Tvm2UserTicketAuthMethod(conf_snout_api, conf_snout_proxy)
        flask_app = self._get_flask_app()

        with flask_app.test_request_context('/fake', headers=headers):
            auth_result = auth_method.get()

            assert_that(auth_result, has_entries({
                'method': 'Tvm2UserTicketAuthMethod',
                'passport_id': self.PASSPORT_UID,
            }))

    def test_different_tvm2_clients_for_different_blackbox_clients(self, mocker):
        from brest.core.auth.tvm2_base import Tvm2BaseAuthMethod
        from brest.core.auth.tvm2_user_ticket import Tvm2UserTicketAuthMethod
        from brest.utils.config import get_tvm2_config
        from tvmauth import BlackboxTvmId as BlackboxClientId

        conf_snout_api = get_tvm2_config('SnoutApi', 'paysys-balance-snout-api')
        conf_snout_proxy = get_tvm2_config('SnoutApi', 'paysys-balance-snout-proxy')

        mocker.patch.object(Tvm2BaseAuthMethod, '_get_blackbox_client', return_value=BlackboxClientId.Test)

        user_tvm_client_1 = Tvm2UserTicketAuthMethod(conf_snout_api, conf_snout_proxy)._get_tvm_client()
        user_tvm_client_2 = Tvm2UserTicketAuthMethod(conf_snout_api, conf_snout_proxy)._get_tvm_client()

        mocker.patch.object(Tvm2BaseAuthMethod, '_get_blackbox_client', return_value=BlackboxClientId.TestYateam)

        user_tvm_client_3 = Tvm2UserTicketAuthMethod(conf_snout_api, conf_snout_proxy)._get_tvm_client()

        assert_that(
            user_tvm_client_1,
            same_instance(user_tvm_client_2),
            "Should be the same instance of tvm client for calls with equal values of _get_blackbox_client()",
        )

        assert_that(
            user_tvm_client_1,
            is_not(same_instance(user_tvm_client_3)),
            "Sould be different instances of tvm clients for different _get_blackbox_client() values",
        )

    @staticmethod
    def _cmd_tvm_knife(cmd, *args):
        import subprocess

        cmd_args = ['tvmknife', 'unittest', cmd]
        cmd_args.extend(args)

        return subprocess.check_output(map(str, cmd_args)).strip()
