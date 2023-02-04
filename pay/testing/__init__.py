from paysys.sre.tools.monitorings.lib.util.helpers import merge, ttl
from paysys.sre.tools.monitorings.lib.notifications import Notifications

defaults = merge(
    ttl(620, 60)
)

notifications = {
    'default': Notifications().noop,
}

CONFIGS = [
    "agency_rewards"
]
