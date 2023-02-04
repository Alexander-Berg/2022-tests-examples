import pytest
import time
from typing import Callable, Any
from .config import e2e_settings


def e2e(fn):
    return pytest.mark.skipif(e2e_settings is None or not e2e_settings.is_local, reason='E2E-test')(fn)


def must_be_true_within(seconds=5):
    def _true_in_time(f: Callable[[Any], bool]):
        def inner(*args, **kwargs):
            now = time.time()
            while time.time() < now + seconds:
                if f(*args, **kwargs):
                    return True

                time.sleep(0.25)
            return False

        return inner

    return _true_in_time
