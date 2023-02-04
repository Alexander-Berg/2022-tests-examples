import datetime
import tempfile
import pytest
import contextlib
import os
import re
from ads_pytorch.online_learning.production.dataset.datetime_dataset import (
    DatetimeURI,
    get_new_uris
)
from ads_pytorch.highlevel_interface import TablePath
import itertools
from ads_pytorch.yt.file_system_adapter_mock import CypressAdapterMock


@pytest.mark.asyncio
async def test_no_directory():
    adapter = CypressAdapterMock()
    with pytest.raises(Exception):
        await get_new_uris(
            fs=adapter,
            datetime_mask="",
            datetime_regex="",
            folder="some_folder"
        )


START = datetime.datetime(2019, 9, 11)


@contextlib.asynccontextmanager
async def make_folder():
    adapter = CypressAdapterMock()
    datetime_mask = "%Y-%m-%d-%H"
    async with adapter.temporary_directory() as tmp:
        for i in itertools.chain(range(48), range(48, 96, 2)):
            date_str = (START + datetime.timedelta(hours=i)).strftime(datetime_mask)
            await adapter.create_table(adapter.path_join(tmp, date_str))

        yield tmp, datetime_mask


@pytest.mark.asyncio
async def test_no_match():
    adapter = CypressAdapterMock()
    async with make_folder() as (folder, datetime_mask):
        regex = folder[:-4] + "eirhejncj"
        # sanity check for test
        filenames = os.listdir(folder)
        assert len(re.findall(regex, filenames[0])) == 0
        with pytest.raises(RuntimeError):
            await get_new_uris(
                fs=adapter,
                datetime_regex=regex,
                datetime_mask=datetime_mask,
                folder=folder
            )


@pytest.mark.asyncio
async def test_multiple_match():
    adapter = CypressAdapterMock()
    async with make_folder() as (folder, datetime_mask):
        regex = "[0-9]*"
        # sanity check for test
        filenames = os.listdir(folder)
        assert len(re.findall(regex, filenames[0])) > 1
        with pytest.raises(RuntimeError):
            await get_new_uris(
                fs=adapter,
                datetime_regex=regex,
                datetime_mask=datetime_mask,
                folder=folder
            )


@pytest.mark.asyncio
async def test_match_ok():
    adapter = CypressAdapterMock()
    async with make_folder() as (folder, datetime_mask):
        regex = "\d{4}-\d{2}-\d{2}-\d{2}"
        uris = await get_new_uris(
            fs=adapter,
            datetime_regex=regex,
            datetime_mask=datetime_mask,
            folder=folder
        )
        assert len(uris) == len(os.listdir(folder))
        for hour, uri in zip(itertools.chain(range(48), range(48, 96, 2)), uris):
            assert uri.date == START + datetime.timedelta(hours=hour)
            assert uri.uri == TablePath(os.path.join(folder, uri.date.strftime(datetime_mask)))


@pytest.mark.asyncio
async def test_match_with_start():
    adapter = CypressAdapterMock()
    async with make_folder() as (folder, datetime_mask):
        regex = "\d{4}-\d{2}-\d{2}-\d{2}"
        uris = await get_new_uris(
            fs=adapter,
            datetime_regex=regex,
            datetime_mask=datetime_mask,
            folder=folder,
            skip_until_date=START + datetime.timedelta(days=1)
        )
        assert len(uris) == len(list(itertools.chain(range(25, 48), range(48, 96, 2))))
        # 24 for start shift
        for hour, uri in zip(itertools.chain(range(25, 48), range(48, 96, 2)), uris):
            assert uri.date == START + datetime.timedelta(hours=hour)
            assert uri.uri == TablePath(os.path.join(folder, uri.date.strftime(datetime_mask)))


@pytest.mark.asyncio
async def test_match_one_uri_not_ok():
    adapter = CypressAdapterMock()
    async with make_folder() as (folder, datetime_mask):
        regex = "\d{4}-\d{2}-\d{2}-\d{2}"
        await adapter.create_table(adapter.path_join(folder, "ahaha"))
        with pytest.raises(RuntimeError):
            await get_new_uris(
                fs=adapter,
                datetime_regex=regex,
                datetime_mask=datetime_mask,
                folder=folder
            )
