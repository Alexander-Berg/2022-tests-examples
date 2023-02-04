import paysys.sre.tools.monitorings.configs.billing30.base.accrualer as accrualer
from paysys.sre.tools.monitorings.lib.util.helpers import empty_children, merge
from paysys.sre.tools.monitorings.lib.checks.postgres import postgres

defaults = {'namespace': 'billing30.accrualer'}
host = "billing30-test.accrualer-test"

children = empty_children
children_tasks = ['billing-accrualer@stage=billing-accrualer-test-stage;deploy_unit=tasks']


def checks():
    return merge(
        accrualer.get_checks(
            children_tasks,
        ),
        postgres("mdbbe2q753bu7pdn9580", "accrualerdb"),
    )
