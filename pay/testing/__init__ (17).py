from paysys.sre.tools.monitorings.configs.oebs.base import notifications_nonprod
from paysys.sre.tools.monitorings.lib.util.helpers import merge, ttl

calendar_id = 27924

defaults = merge(
    ttl(620, 300),
    {'namespace': 'oebs.oebs.testing'},
)

notifications = notifications_nonprod

CONFIGS = [
    "clones",
]
