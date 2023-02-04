from copy import deepcopy
from typing import Any, List, Tuple


def dummy_async_context_manager(value):
    class _Inner:
        async def __aenter__(self):
            return value

        async def __aexit__(self, *args):
            pass

        async def _await_mock(self):
            return value

        def __await__(self):
            return self._await_mock().__await__()

    return _Inner()


def dummy_async_function(result=None, exc=None, calls=[]):
    async def _inner(*args, **kwargs):
        nonlocal calls
        calls.append((args, kwargs))

        if exc:
            raise exc
        return result

    return _inner


def locate_key(d: dict, locator: str) -> Tuple[dict, str]:
    ref = d
    last_part, *path = locator.split('.')
    for part in path:
        ref.setdefault(last_part, {})
        ref = ref[last_part]
        last_part = part
    return ref, last_part


def remove_keys(d: dict, *keys: List[str]) -> dict:
    d = deepcopy(d)
    for locator in keys:
        ref, key = locate_key(d, locator)
        del ref[key]
    return d


def set_key(d: dict, locator: str, value: Any) -> dict:
    d = deepcopy(d)
    ref, key = locate_key(d, locator)
    ref[key] = value
    return d
