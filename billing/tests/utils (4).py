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
