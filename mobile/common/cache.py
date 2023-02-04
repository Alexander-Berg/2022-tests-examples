# -*- coding: utf-8 -*-
import functools
import ujson

import base64
import cache_memoize
import hashlib
import logging
from django.core.cache import caches
from django.utils.encoding import smart_bytes, smart_text
from redis_cache.serializers import BaseSerializer

DEFAULT_CACHE_TIMEOUT = 24 * 3600
SHARED_CACHE_ALIAS = 'shared'

logger = logging.getLogger(__name__)


class UJSONSerializer(BaseSerializer):
    def __init__(self, **kwargs):
        super(UJSONSerializer, self).__init__(**kwargs)

    def serialize(self, value):
        return smart_bytes(ujson.dumps(value))

    def deserialize(self, value):
        return ujson.loads(smart_text(value))


class SharedCacheProxy(object):
    def __getattr__(self, name):
        return getattr(caches[SHARED_CACHE_ALIAS], name)

    def __setattr__(self, name, value):
        return setattr(caches[SHARED_CACHE_ALIAS], name, value)

    def __delattr__(self, name):
        return delattr(caches[SHARED_CACHE_ALIAS], name)

    def __contains__(self, key):
        return key in caches[SHARED_CACHE_ALIAS]

    def __eq__(self, other):
        return caches[SHARED_CACHE_ALIAS] == other

    def __ne__(self, other):
        return caches[SHARED_CACHE_ALIAS] != other


shared_cache = SharedCacheProxy()
shared_memoize = functools.partial(cache_memoize.cache_memoize, cache_alias=SHARED_CACHE_ALIAS)


# noinspection PyIncorrectDocstring
def make_cache_key(*args, **kwargs):
    """
    universal function for creating cache key by arguments
    :param prefix: - is optional. could be used to make scope in cache
    Examples:
        >> make_cache_key('123456789', prefix='crypta')
        'crypta:jzRQmFVqbrw1+7LD'

        >> make_cache_key('123456789')
        'jzRQmFVqbrw1+7LD'

        >> make_cache_key(uuid='1234567890', yuid='0987654321', prefix='loyalty')
        'loyalty:0rdmQDJbBowL8BRR'
    """
    prefix = kwargs.pop('prefix', None)
    cache_key = hashlib.md5()
    if args:
        cache_key.update(str(args))
    if kwargs:
        cache_key.update(str(kwargs))
    cache_key = base64.b64encode(cache_key.digest())[:16]
    return cache_key if not prefix else '%s:%s' % (prefix, cache_key)


# noinspection PyIncorrectDocstring
def is_value_in_cache(*args, **kwargs):
    """
    useful to determine values in the cache
    :param prefix: - is optional. could be used to make scope in cache
    :param cache: - cache object, shared_cache by default
    Examples:
        >> key = make_cache_key(uuid='1234567890', yuid='0987654321', prefix='loyalty')
        >> shared_cache.set(key, 'test data', 60)
        >> is_values_in_cache(uuid='1234567890', yuid='0987654321', prefix='loyalty')
        True
    """
    key = make_cache_key(*args, **kwargs)
    cache = kwargs.pop('cache', shared_cache)
    return key in cache
