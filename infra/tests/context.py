from kernel.util import logging


def initLogging():
    logger = getattr(initLogging, 'logger', None)
    if logger:
        return logger

    # Initialize logger
    logger = initLogging.logger = logging.initialize()
    # Capture all warnings.
    logging.captureWarnings(True)
    return logger


class Context(object):
    class Config(object):
        def __init__(self, data):
            self._data = data
            super(Context.Config, self).__init__()

        def __getattr__(self, name):
            if name.startswith('_'):
                return super(Context.Config, self).__getattr__(name)
            res = self._data.__getitem__(name)
            return Context.Config(res) if isinstance(res, dict) else res

        def __setattr__(self, name, item):
            if name.startswith('_'):
                return super(Context.Config, self).__setattr__(name, item)
            return self._data.__setitem__(name, item)

    def __init__(self, cfg):
        super(Context, self).__init__()
        self.cfg = cfg
        self.log = initLogging()
        self.log.name = 'test'
