from paysys.sre.tools.monitorings.lib.util.helpers import merge, ttl
from paysys.sre.tools.monitorings.configs.billing30.testing import accounts

defaults = merge(
    ttl(620, 60)
)

notifications = {
    'default': accounts.notifications.noop,
    'by_host': {
        accounts.host: accounts.notifications.telegram
    }
}

CONFIGS = [
    "accounts",
    "payout",
    "accrualer"
]
