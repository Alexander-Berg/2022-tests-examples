from wiki.unistat.counter import RedisCounter
from django.core.cache import caches
from django_redis.cache import RedisCache


def test_redis_cache():
    CACHE_UNISTAT_METRICS = caches['unistat_metrics']
    assert CACHE_UNISTAT_METRICS.__class__ == RedisCache


def test_redis_counter():
    q = RedisCounter('test_counter')
    q.reset()
    q.increase()
    q.increase()
    q.increase()
    assert q.produce_unistat_metric() == [['test_counter_ammm', 3]]
    assert q.get() == 0
