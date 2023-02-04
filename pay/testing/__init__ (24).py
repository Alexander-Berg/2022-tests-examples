from paysys.sre.tools.monitorings.lib.util.helpers import merge, ttl, flaps
from paysys.sre.tools.monitorings.lib.notifications import Notifications

defaults = merge(
    flaps(240, 0),
    ttl(620, 60),
    {'namespace': 'uzedo.testing'},
)

notifications_object = Notifications()

notifications = {
    'default': notifications_object.noop,
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
    "uzedo"
]
