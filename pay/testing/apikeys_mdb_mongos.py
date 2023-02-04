from paysys.sre.tools.monitorings.lib.util.helpers import merge
from paysys.sre.tools.monitorings.configs.apikeys.base.apikeys_mdb import cpu_usage_mdb


host = 'apikeys.testing'
children = [
    'man-ptngiv6aeq94u70k.db.yandex.net',
    'sas-aigbfjdwmrfs81q1.db.yandex.net',
    'vla-os0c0f1euaaheauz.db.yandex.net',
]


def checks():
    return merge(
        cpu_usage_mdb('mongos', children),
    )
