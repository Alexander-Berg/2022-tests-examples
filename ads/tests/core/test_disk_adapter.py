import pytest
import tempfile
import os
import shutil
from uuid import uuid4


from ads_pytorch.core.disk_adapter import (
    DiskFileSystemAdapter,
    DiskFilePath,
    ChunkedDiskLoadPool,
    ChunkFileSplitter
)

from ads_pytorch.tools.buffered_parallel_reader import BufferedParallelReader


#####################################################
#              FILE SYSTEM ADAPTER TESTS            #
#####################################################


def _remove(path):
    if os.path.exists(path):
        shutil.rmtree(path) if os.path.isdir(path) else os.remove(path)


@pytest.mark.asyncio
async def test_create_file():
    fname = f"./{uuid4()}"
    try:
        assert not os.path.exists(fname)
        await DiskFileSystemAdapter().create_file(path=fname)
        assert os.path.exists(fname)
        assert not os.path.isdir(fname)
    finally:
        _remove(fname)


@pytest.mark.asyncio
async def test_create_directory():
    fname = f"./{uuid4()}"
    try:
        assert not os.path.exists(fname)
        await DiskFileSystemAdapter().create_directory(path=fname)
        assert os.path.exists(fname)
        assert os.path.isdir(fname)
    finally:
        _remove(fname)


def test_path_join():
    paths = ["home", "alxmopo3ov", "ahaha", "file1"]
    assert DiskFileSystemAdapter().path_join(*paths) == os.path.join(*paths)


@pytest.mark.asyncio
async def test_concatenate():
    files_count = 5
    paths = [str(uuid4()) for _ in range(files_count)]
    try:
        for i, fname in enumerate(paths):
            with open(fname, "wt") as f:
                f.write(str(i))

        with tempfile.NamedTemporaryFile() as tmp:
            await DiskFileSystemAdapter().concatenate(paths, tmp.name)
            with open(tmp.name, "rt") as f:
                readed = f.read()
            assert readed == "01234"
    finally:
        for path in paths:
            _remove(path)


@pytest.mark.asyncio
async def test_transaction_ctx():
    adapter = DiskFileSystemAdapter()
    async with adapter.transaction_ctx() as tx:
        pass
    async with adapter.transaction_ctx() as tx:
        async with adapter.transaction_ctx(parent_tx=tx) as tx2:
            pass


@pytest.mark.asyncio
async def test_rmtree():
    dirname = "./ahaha"
    try:
        os.makedirs(dirname)
        await DiskFileSystemAdapter().rmtree(dirname)
        assert not os.path.exists(dirname)
    finally:
        _remove(dirname)


@pytest.mark.asyncio
async def test_temp_dir():
    adapter = DiskFileSystemAdapter()

    async with adapter.temporary_directory() as tempdir:
        assert os.path.exists(tempdir)
        assert os.path.isdir(tempdir)

    assert not os.path.exists(tempdir)


#################################
#         CHUNK SPLITTER        #
#################################


@pytest.mark.parametrize("chunk_size", [0, -100], ids=["Zero", "Negative"])
@pytest.mark.asyncio
async def test_bad_chunk_size(chunk_size):
    with pytest.raises(ValueError):
        ChunkFileSplitter(chunk_size=chunk_size)


@pytest.mark.asyncio
async def test_chunker():
    splitter = ChunkFileSplitter(chunk_size=3)

    with tempfile.NamedTemporaryFile() as tmp:
        with open(tmp.name, "wt") as f:
            f.write("01234567")
        files = [x async for x in splitter(path=tmp.name)]
        assert files == [
            DiskFilePath(path=tmp.name, offset=0, size=3),
            DiskFilePath(path=tmp.name, offset=3, size=3),
            DiskFilePath(path=tmp.name, offset=6, size=2)
        ]


@pytest.mark.asyncio
async def test_chunker_big():
    splitter = ChunkFileSplitter(chunk_size=1 << 30)

    with tempfile.NamedTemporaryFile() as tmp:
        with open(tmp.name, "wt") as f:
            f.write("01234567")
        files = [x async for x in splitter(path=tmp.name)]
        assert files == [
            DiskFilePath(path=tmp.name, offset=0, size=os.path.getsize(tmp.name))
        ]


################################################
#                  DISK POOLS                  #
################################################


@pytest.mark.asyncio
async def test_load_pool():
    with tempfile.NamedTemporaryFile() as tmp:
        with open(tmp.name, "wb") as f:
            f.write(b"01234567")
        async with ChunkedDiskLoadPool() as pool:
            future = await pool.assign_job(DiskFilePath(path=tmp.name, offset=1, size=3))
            res = await future
            assert res == b'123'


@pytest.mark.asyncio
async def test_streamed_read():
    with tempfile.NamedTemporaryFile() as tmp:
        with open(tmp.name, "wb") as f:
            f.write(bytes(bytearray(range(255))))
        async with ChunkedDiskLoadPool(workers_count=10) as pool:
            reader = BufferedParallelReader(reader_pool=pool, max_memory=2)
            stream = await reader.schedule(chunks=[
                DiskFilePath(path=tmp.name, offset=i, size=1)
                for i in range(255)
            ])

            res = b''.join([x async for x in stream])
            assert res == bytes(bytearray(range(255)))
