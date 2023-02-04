from paysys.sre.tools.monitorings.lib.notifications import Notifications
from paysys.sre.tools.monitorings.lib.util.helpers import merge, ttl
from paysys.sre.tools.monitorings.configs.configshop.testing import configshop

defaults = merge(
    ttl(620, 60)
)

notifications = {
    'default': Notifications().noop,
    'by_host': {
        configshop.host: configshop.notifications,
    }
}

CONFIGS = [
    "configshop"
]
