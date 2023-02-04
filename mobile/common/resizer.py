from urllib import quote_plus

import yenv
from django.conf import settings
from hashlib import md5

from yaphone.advisor.common.tools import rewrite_url

DEFAULT_TYPEMAP = 'gif:gif;png:png;*:jpeg;'
ENLARGE = 'yes'


def make_scaled_image_url(url, width=0, height=0, crop=False, quality=92, typemap=DEFAULT_TYPEMAP,
                          host=None, path_prefix='/resizer'):
    """ makes target url to get scaled image according passed size(s)
        see: https://wiki.yandex-team.ru/mds/resizer/
        use crop=True parameter to get resulting image exactly specified sizes.

        Testing resizer is not accessible from internet, so we are proxying connection through testing launcher backend.
        Specify host and path prefix for such proxy
    """
    # must be specified for scaling operation
    crop_value = 'yes' if crop else ''

    # order of args is important! don't change here and url_args definition.
    key = make_key(url, width, height, typemap, crop_value, ENLARGE)

    # make all params. the order is important.
    url_args = (
        ('url', quote_plus(url)),
        ('width', width),
        ('height', height),
        ('typemap', quote_plus(typemap)),
        ('crop', crop_value),
        ('enlarge', ENLARGE),
        ('quality', quality),
        ('key', key),
    )

    url = '%s?%s' % (settings.RESIZER_HOST, '&'.join("%s=%s" % arg for arg in url_args))
    if yenv.type != 'production' and host is not None:
        url = rewrite_url(url=url, host=host, path_prefix=path_prefix)
    return url


def make_key(*args):
    """calc md5 hash in hex string representation on passed arguments"""
    data = ''.join(str(v) for v in args) + settings.RESIZER_SECRET_KEY
    return md5(data).hexdigest()
