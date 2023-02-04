import logging
import os

from maps.infra.sandbox import SedemManagedMixin, ReleaseSpec
from sandbox import sdk2
from sandbox.projects.common.arcadia.sdk import mount_arc_path
from sandbox.sdk2.helpers import subprocess as sp
import sandbox.common.types.misc as ctm


FRONTEND_DIR = 'maps/b2bgeo/frontend'
MOBILE_TOKEN_VAULT_TESTING_SUPER_USER = 'b2bgeo_ya_courier_backend_courier_app_token_super_user'
MOBILE_TOKEN_VAULT_TESTING_APP_USER = 'b2bgeo_ya_courier_backend_courier_app_token'
MOBILE_TOKEN_VAULT_TESTING_APP_UNREGISTERED_USER = 'b2bgeo_ya_courier_backend_courier_app_token_unregistered_user'


class YcbApplicationTest(SedemManagedMixin, sdk2.Task):
    class Requirements(sdk2.Task.Requirements):
        # NPM has troubles with IPv6.
        dns = ctm.DnsType.DNS64
        container_resource = 3111137427

    class Parameters(SedemManagedMixin.Parameters):
        arcadia_url = sdk2.parameters.String('Arcadia url to mount', required=True)
        kill_timeout = 60 * 60  # 60 minutes

        @classmethod
        def release_spec(cls) -> ReleaseSpec:
            spec = super().release_spec()
            spec.add_deploy_unit('template_testing')

            return spec

    def on_execute(self) -> None:
        with sdk2.helpers.ProcessLog(self, logger=logging.getLogger("ya-courier-app-test")) as process_log:
            arcadia_url = self.Parameters.arcadia_url
            with mount_arc_path(arcadia_url) as arcadia_path:
                os.chdir(arcadia_path + FRONTEND_DIR)

                sp.check_call("npm run ci:bootstrap",
                              shell=True, stdout=process_log.stdout, stderr=process_log.stdout)

                os.chdir("projects/courier-app")

                sp.check_call("npm run ci:install",
                              shell=True, stdout=process_log.stdout, stderr=process_log.stdout)

                super_user_token = sdk2.Vault.data(
                    MOBILE_TOKEN_VAULT_TESTING_SUPER_USER)
                app_user_token = sdk2.Vault.data(
                    MOBILE_TOKEN_VAULT_TESTING_APP_USER)
                unregistered_user_token = sdk2.Vault.data(
                    MOBILE_TOKEN_VAULT_TESTING_APP_UNREGISTERED_USER
                )

                command = "SUPER_USER_TOKEN={} " \
                          "APP_USER_TOKEN={} " \
                          "UNREGISTERED_USER_TOKEN={} " \
                          "CONFIG=testing " \
                          "npm run api-tests" \
                          .format(super_user_token, app_user_token, unregistered_user_token)

                sp.check_call(command, shell=True, stdout=process_log.stdout, stderr=process_log.stdout)
