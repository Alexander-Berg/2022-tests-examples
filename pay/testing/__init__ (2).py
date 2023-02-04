from paysys.sre.tools.monitorings.lib.util.helpers import merge, ttl
from paysys.sre.tools.monitorings.configs.apikeys.base import notifications_object

defaults = merge(
    ttl(620, 60),
    {'namespace': 'apikeys'},
)

notifications = {
    'default': notifications_object.noop,
    'by_service': {
        'mem-free': {},
        'watchdog': {},
        'nginx_errors': {},
    },
    'by_tag': {
        'http_workhours': {},
        'http_main': {},
        'https_workhours': {},
        'https_main': {},
        'https_cert': {},
    },
}


CONFIGS = [
    'apikeys',
    'apikeys_l7',
    'apikeys_mdb_mongod',
    'apikeys_mdb_mongos',
    'balance_userapikeys_l7',
    'delete_later_apikeys_mongobckp',
    'delete_later_tech_mongo',
    'delete_later_tech_mongobckp',
    'tech',
]
