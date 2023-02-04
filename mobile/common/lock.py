import redis
from django.conf import settings


class RedisLocker(object):
    def __init__(self, url):
        self.client = redis.StrictRedis.from_url(url)

    def lock(self, name, timeout, blocking_timeout=0.5):
        lock = self.client.lock(name, timeout, blocking_timeout=blocking_timeout)
        if lock.acquire():
            return lock

    def unlock(self, lock):
        lock.release()


redis_distributed_lock = RedisLocker(settings.REDIS_URL_SHARED)
