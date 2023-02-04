from paysys.sre.tools.monitorings.lib.util.helpers import merge, ttl
from paysys.sre.tools.monitorings.configs.paysys.base import notifications_object
from paysys.sre.tools.monitorings.configs.balance_app.base import yt_notifications_object

defaults = merge(
    ttl(620, 60),
    {'namespace': 'balance'},
)

notifications = {
    'default': notifications_object.noop,
    'by_service': {
        'mem-free': {},
        'watchdog': {},
        'nginx_errors': {},
        'nginx_499': {},
    },
    'by_tag': {
        'postgres': {},
        'http_workhours': {},
        'http_main': {},
        'https_workhours': {},
        'https_main': {},
        'https_cert': {},
    },
    'by_host': {
        'balance-test.yt': yt_notifications_object.telegram_daytime,
    },
}

CONFIGS = [
    "backup",
    "balance",
    "reactor",
    "yt",
]
