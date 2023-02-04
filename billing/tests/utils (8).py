import datetime
import uuid


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


def correct_uuid4(guid: str, version: int = 4) -> bool:
    try:
        val = uuid.UUID(guid, version=version)
    except Exception:
        return False
    return val.hex == guid.replace('-', '')


def patch_storage_in_action(action, storage, mocker):
    # this hackery is needed since the action object generates a brand new storage
    # instance on the fly for every call of 'run()', which makes it harder to patch
    mock_context_cls = mocker.patch.object(action, 'storage_context_cls')
    mock_context_cls.return_value.__aenter__.return_value = storage


async def helper_get_db_now(storage) -> datetime.datetime:
    return await (await storage.conn.execute('select now()')).scalar()
