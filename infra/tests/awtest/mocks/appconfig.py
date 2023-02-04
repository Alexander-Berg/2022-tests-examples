import copy
from contextlib import contextmanager

import decorator
from sepelib.core import config


@contextmanager
def modified_appconfig(key, value):
    saved_config = copy.deepcopy(config.get())
    config.set_value(key, value)
    try:
        yield
    finally:
        config._CONFIG = saved_config


def with_modified_appconfig(key, value):
    def dec(f):
        def wrapper(f, *args, **kwargs):
            with modified_appconfig(key, value):
                f(*args, **kwargs)

        # we have to use decorator module to not break py.test introspection
        return decorator.decorator(wrapper, f)

    return dec
