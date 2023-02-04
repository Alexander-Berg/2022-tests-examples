from datetime import datetime
from typing import Any, Optional
from uuid import UUID

import jmespath

from sendr_pytest.matchers import convert_then_match

from hamcrest import assert_that, equal_to, has_property, match_equality, not_none


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


def replace_payload(payload, full_path, value):
    full_path = '@.' + full_path
    path, name = full_path.rsplit('.', 1)
    obj = jmespath.search(path, payload)
    obj[name] = value
    return payload


async def check_error(response, full_path, expected_error):
    assert_that(response.status, equal_to(400))
    data = await response.json()
    assert_that(
        data,
        equal_to(
            {
                'code': 400,
                'status': 'fail',
                'data': {
                    'message': 'SCHEMA_VALIDATION_ERROR',
                    'params': match_equality(not_none()),
                },
            }
        ),
    )
    assert_that(
        jmespath.search(f'data.params | {full_path} | [0]', data),
        equal_to(expected_error),
    )


def _maybe_convert_uuid(value: Any) -> UUID:
    return value if isinstance(value, UUID) else UUID(value)


def _maybe_convert_dt(value: Any) -> datetime:
    return value if isinstance(value, datetime) else datetime.fromisoformat(value)


def is_uuid(version: Optional[int] = 4):
    return match_equality(convert_then_match(_maybe_convert_uuid, has_property('version', equal_to(version))))


def is_datetime_with_tz():
    return match_equality(convert_then_match(_maybe_convert_dt, has_property('tzinfo', not_none())))
