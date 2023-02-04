from paysys.sre.tools.monitorings.configs.trust.base import trust_lpm_deploy
from paysys.sre.tools.monitorings.lib.checks.active.http import merge

host = "trust-test.trust-lpm-deploy-test"
children = ['trust-lpm@stage=trust-lpm-test-stage;deploy_unit=trust-lpm']


def checks():
    return merge(
        trust_lpm_deploy.get_checks(
            children
        )
    )
