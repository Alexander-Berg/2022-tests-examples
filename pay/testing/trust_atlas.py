from paysys.sre.tools.monitorings.configs.trust.base import trust_atlas
from paysys.sre.tools.monitorings.lib.checks.active.http import merge

host = "trust-test.trust-atlas"
children = ['trust-atlas@stage=trust-atlas-test-stage;deploy_unit=api']


def checks():
    return merge(
        trust_atlas.get_checks(
            children
        )
    )
