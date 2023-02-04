from unittest.mock import patch

import pytest

from maps_adv.common.helpers import coro_mock

pytestmark = [pytest.mark.asyncio]


class Keker:
    async def kek(self):
        return "kek"


class KekException(Exception):
    pass


async def test_return_value_works_well():
    with patch.object(Keker, "kek", new_callable=coro_mock) as patched:
        patched.coro.return_value = "mocked kek"
        keker = Keker()

        assert await keker.kek() == "mocked kek"


async def test_side_effect_works_well():
    with patch.object(Keker, "kek", new_callable=coro_mock) as patched:
        patched.coro.side_effect = KekException
        keker = Keker()

        with pytest.raises(KekException):
            await keker.kek()


async def test_mock_teardown_is_working():
    with patch.object(Keker, "kek", new_callable=coro_mock) as patched:
        patched.coro.return_value = "mocked kek"
        keker = Keker()

    assert await keker.kek() == "kek"
