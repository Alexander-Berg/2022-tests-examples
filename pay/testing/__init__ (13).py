from paysys.sre.tools.monitorings.configs.hyperion.base import notifications_nonprod
from paysys.sre.tools.monitorings.lib.util.helpers import merge, ttl

defaults = merge(
    ttl(620, 300),
    {'namespace': 'oebs.hyperion.testing'},
)

notifications = notifications_nonprod

CONFIGS = [
    "ess",
    "db",
    "appl",
]
