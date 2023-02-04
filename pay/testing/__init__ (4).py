from paysys.sre.tools.monitorings.lib.util.helpers import merge, ttl
from paysys.sre.tools.monitorings.configs.balance_fop.base import notifications_object

defaults = merge(
    ttl(620, 60),
    {'namespace': 'balance_fop'},
)

notifications = {
    'default': notifications_object.noop
}

CONFIGS = [
    "fop"
]
