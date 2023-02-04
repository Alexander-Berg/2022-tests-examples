from paysys.sre.tools.monitorings.lib.util.helpers import merge
from paysys.sre.tools.monitorings.configs.apikeys.base.apikeys_mdb import cpu_usage_mdb


host = 'apikeys.testing'
children = [
    'man-90z53qqtjev93es3.db.yandex.net',
    'man-efycbhavs39q99am.db.yandex.net',
    'man-y56rtnxtg2fi9er1.db.yandex.net',
    'sas-9vfj5j7eyl64agjf.db.yandex.net',
    'sas-dwlrur9e3w217y1q.db.yandex.net',
    'sas-iw6ors0d9qq104lw.db.yandex.net',
    'vla-7yukzxdo3tbymtpq.db.yandex.net',
    'vla-9g0z6h3boa9rh6kq.db.yandex.net',
    'vla-weunm5gnclrigjpb.db.yandex.net',
]


def checks():
    return merge(
        cpu_usage_mdb('mongod', children),
    )
