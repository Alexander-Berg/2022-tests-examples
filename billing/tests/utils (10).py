from datetime import datetime, timezone
from uuid import UUID

from bs4 import BeautifulSoup

from sendr_pytest.matchers import convert_then_match

from hamcrest import has_property, match_equality, not_none


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


def to_utc(dt: datetime) -> datetime:
    assert dt.tzinfo is not None
    return dt.astimezone(tz=timezone.utc)


def beautiful_soup(html) -> BeautifulSoup:
    """
    Можно и XML парсер использовать, например:
    import xml.etree.ElementTree as ET
    parsed = ET.fromstring('<html></html>')

    Но xml парсеры не могут распарсить вот такое:
    <script>
        window.addEventListener("message", function(ev) {
            if (parent !== window && parent.postMessage) {
                parent.postMessage(ev.data, "*");
            }
        });
    </script>
    """
    return BeautifulSoup(html, features='lxml')


is_uuid4 = match_equality(
    convert_then_match(
        UUID,
        has_property('version', 4),
    )
)

is_datetime_with_tz = match_equality(
    convert_then_match(
        datetime.fromisoformat,
        has_property('tzinfo', not_none()),
    )
)
