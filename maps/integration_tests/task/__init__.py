import subprocess

from maps.infra.sandbox import SedemManagedMixin, ReleaseSpec, get_binary
from sandbox import sdk2


class SolverIntegrationTest(SedemManagedMixin, sdk2.Task):
    class Parameters(SedemManagedMixin.Parameters):
        sync_solver_url = sdk2.parameters.String('Sync solver url', required=True)
        solver_api_endpoint = sdk2.parameters.String('Solver api endpoint', required=True)
        solver_auth_token = sdk2.parameters.YavSecret('Yav secret with solver_auth_token', required=True)

        @classmethod
        def release_spec(cls) -> ReleaseSpec:
            spec = super().release_spec()

            testing_template = spec.add_deploy_unit('template_testing')
            testing_template.sync_solver_url = 'http://b2bgeo-syncsolver.testing.maps.yandex.net'
            testing_template.solver_api_endpoint = 'https://test.courier.yandex.ru/vrs/api/v1'
            testing_template.solver_auth_token = 'sec-01ddnqw9am99fsy5dnmh7hcs1v#OAuth'

            stable_template = spec.add_deploy_unit('template_stable')
            stable_template.sync_solver_url = 'http://b2bgeo-syncsolver.maps.yandex.net'
            stable_template.solver_api_endpoint = 'https://courier.yandex.ru/vrs/api/v1'
            stable_template.solver_auth_token = 'sec-01ddnqw9am99fsy5dnmh7hcs1v#OAuth'

            return spec

    def on_execute(self) -> None:
        with get_binary('integration_tests_bin') as executable:
            env = {
                'SYNC_SOLVER_URL': self.Parameters.sync_solver_url,
                'SOLVER_API_ENDPOINT': self.Parameters.solver_api_endpoint,
                'SOLVER_AUTH_TOKEN': self.Parameters.solver_auth_token.value()
            }
            completed_process = subprocess.run([executable], env=env)
            completed_process.check_returncode()
