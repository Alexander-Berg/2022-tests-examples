from paysys.sre.tools.monitorings.lib.util.helpers import merge, ttl
from paysys.sre.tools.monitorings.configs.trust.base import (
    notifications_object,
    sentry_notifications_object
)

defaults = merge(
    ttl(620, 60),
    {'namespace': 'trust.test'},
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
        'trust_sentry_postgres': sentry_notifications_object.startrek_and_telegram,
    },
}

CONFIGS = [
    "fraud_admin",
    "fraud_admin_postgres",
    "rpslimiter",
    "scrooge",
    "s3_mds",
    "scheduler",
    "timeline",
    "timeline_sox",
    "trust",
    "trust_atlas",
    "trust_postgres",
    "trust_mongo",
    "trust_mongobckp",
    "trust_l7",
    "trust_lpm_deploy"
]
