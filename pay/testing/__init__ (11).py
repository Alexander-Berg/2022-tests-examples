from paysys.sre.tools.monitorings.configs.diehard.base import notifications_object
from paysys.sre.tools.monitorings.lib.util.helpers import merge, ttl

defaults = merge(
    {"namespace": "diehard.test"},
    ttl(620, 60),
)

notifications = {
    "default": notifications_object.noop,
    "by_service": {
        "check-certs": notifications_object.telegram,
    },
    "by_host": {
        "diehard.outbproxy": notifications_object.telegram,
        "diehard.L7": notifications_object.telegram,
    }
}

CONFIGS = [
    "back",
    "bckp",
    "duckgo_front",
    "front",
    "galera",
    "mgmt",
    "proxy",
    "L7",
    "outbproxy"
]
