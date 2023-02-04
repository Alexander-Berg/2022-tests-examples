from paysys.sre.tools.monitorings.configs.bi.base import notifications_nonprod
from paysys.sre.tools.monitorings.lib.util.helpers import merge, ttl

defaults = merge(
    ttl(620, 300),
    {'namespace': 'oebs.bi.testing'},
)

notifications = notifications_nonprod

CONFIGS = [
    "trgt",
    "db",
    "appl",
    "md",
]
