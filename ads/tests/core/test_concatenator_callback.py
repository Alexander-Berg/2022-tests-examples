import pytest
import asyncio
import contextlib
from typing import List

from ads_pytorch.core.concatenator_callback import (
    UriConcatenator,
    DuplicateUriError,
    ExtraUriError
)
from ads_pytorch.core.file_system_adapter import BaseFileSystemAdapter


class MyFileSystemAdapter(BaseFileSystemAdapter[str, int]):
    def __init__(self):
        super(MyFileSystemAdapter, self).__init__()
        self._concat_calls_args = []
        self._concatenated = set()
        self._removed = set()
        self._call_count = 0
        self._created = 0

    async def concatenate(self, source_paths: List[str], destination_path: str, tx=None):
        assert tx is not None
        self._call_count += 1
        self._concatenated.update(set(source_paths))
        self._concat_calls_args.append((source_paths, destination_path))

    async def exists(self, path, tx=None):
        assert tx is None
        return self._created > 0

    async def remove(self, path, tx=None):
        assert tx is not None
        self._removed.add(path)

    async def create_file(self, path, tx=None):
        assert tx is None
        self._created += 1

    @contextlib.asynccontextmanager
    async def transaction_ctx(self, parent_tx=None):
        yield 1


@pytest.mark.asyncio
async def test_concatenate():
    adapter = MyFileSystemAdapter()
    async with UriConcatenator(
        file_system_adapter=adapter,
        uri_to_concatenate={1, 2, 3, 4, 5},
        concatenated_path=100500
    ) as concatenator:
        await concatenator(None, None, None, None)
        assert adapter._concatenated == set()
        assert adapter._call_count == 0

        concatenator.on_uri_ready(1)

        await asyncio.sleep(0.001)
        await concatenator(None, None, None, None)
        assert adapter._concatenated == {1, 100500}
        assert adapter._call_count == 1
        assert adapter._created == 1

        concatenator.on_uri_ready(2)
        concatenator.on_uri_ready(3)
        concatenator.on_uri_ready(4)

        await asyncio.sleep(0.001)
        await concatenator(None, None, None, None)
        assert adapter._concatenated == {1, 2, 3, 4, 100500}
        assert adapter._call_count == 2
        assert adapter._created == 1

        concatenator.on_uri_ready(5)

        await asyncio.sleep(0.001)
        await concatenator(None, None, None, None)
        assert adapter._concatenated == {1, 2, 3, 4, 5, 100500}
        assert adapter._call_count == 3
        assert adapter._created == 1

    # check the order of concatenation
    # it is important to 100500 (destination path) to be in the beginning
    # to preserve concatenation order
    assert adapter._concat_calls_args == [
        ([100500, 1], 100500),
        ([100500, 2, 3, 4], 100500),
        ([100500, 5], 100500)
    ]


@pytest.mark.asyncio
async def test_duplicate_uri():
    adapter = MyFileSystemAdapter()
    with pytest.raises(DuplicateUriError):
        async with UriConcatenator(
            file_system_adapter=adapter,
            uri_to_concatenate={1, 2, 3, 4},
            concatenated_path=100500
        ) as concatenator:
            concatenator.on_uri_ready(1)
            with pytest.raises(DuplicateUriError):
                concatenator.on_uri_ready(1)


@pytest.mark.asyncio
async def test_duplicate_multiple_uri():
    adapter = MyFileSystemAdapter()
    with pytest.raises(DuplicateUriError):
        async with UriConcatenator(
            file_system_adapter=adapter,
            uri_to_concatenate={1, 2, 3, 4},
            concatenated_path=100500
        ) as concatenator:
            concatenator.on_uri_ready(1, 2)
            with pytest.raises(DuplicateUriError):
                concatenator.on_uri_ready(2, 3)


@pytest.mark.asyncio
async def test_extra():
    adapter = MyFileSystemAdapter()
    with pytest.raises(ExtraUriError):
        async with UriConcatenator(
            file_system_adapter=adapter,
            uri_to_concatenate={1, 2, 3, 4},
            concatenated_path=100500
        ) as concatenator:
            with pytest.raises(ExtraUriError):
                concatenator.on_uri_ready(5)


@pytest.mark.asyncio
async def test_extra_multiple_uri():
    adapter = MyFileSystemAdapter()
    with pytest.raises(ExtraUriError):
        async with UriConcatenator(
            file_system_adapter=adapter,
            uri_to_concatenate={1, 2, 3, 4},
            concatenated_path=100500
        ) as concatenator:
            concatenator.on_uri_ready(1)
            await asyncio.sleep(0.01)
            with pytest.raises(ExtraUriError):
                concatenator.on_uri_ready(5, 3, 4)


@pytest.mark.asyncio
async def test_concatenator_exits_in_case_of_external_exception():
    adapter = MyFileSystemAdapter()
    with pytest.raises(LookupError):
        async with UriConcatenator(
            file_system_adapter=adapter,
            uri_to_concatenate={1, 2, 3, 4},
            concatenated_path=100500
        ) as concatenator:
            concatenator.on_uri_ready(1)
            await asyncio.sleep(0.01)
            raise LookupError


# Asynchronous testing


@pytest.mark.parametrize('remove', [True, False])
@pytest.mark.asyncio
async def test_concatenator_waits_until_all_concatenated(remove):

    async def _data_producer(concatenator):
        for i in range(10):
            concatenator.on_uri_ready(i)
            await asyncio.sleep(0.01)
        await asyncio.sleep(0.03)

    adapter = MyFileSystemAdapter()
    async with UriConcatenator(
        file_system_adapter=adapter,
        uri_to_concatenate=set(range(10)),
        concatenated_path=100500,
        remove_already_concatenated=remove
    ) as concatenator:
        loop = asyncio.get_event_loop()
        loop.create_task(_data_producer(concatenator))

    assert adapter._concatenated == set(range(10)).union({100500})
    if remove:
        assert adapter._removed == set(range(10))
