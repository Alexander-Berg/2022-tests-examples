from paysys.sre.tools.monitorings.lib.util.helpers import merge, ttl
from paysys.sre.tools.monitorings.configs.paysys.base import notifications_object

defaults = merge(
    ttl(620, 60),
    {'namespace': 'psadmin.testing'},
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
}

CONFIGS = [
    "ark",
    "backup",
    "salt",
]
