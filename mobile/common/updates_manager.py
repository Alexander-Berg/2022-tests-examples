import logging
from time import time

logger = logging.getLogger(__name__)


class UpdatesManager(object):
    def __init__(self, init_func, lifetime=60 * 60):
        self.update_ts = None
        self.lifetime = lifetime
        self.init_func = init_func

    def _reinit_required(self):
        return not self.initialized or int(time()) - self.update_ts > self.lifetime

    @property
    def initialized(self):
        return self.update_ts is not None

    def maybe_reinit(self):
        if self._reinit_required():
            # noinspection PyBroadException
            try:
                self.init_func()
            except Exception:
                logger.error('Failed to update cache', extra={'stack': True})
                return
            self.update_ts = int(time())
