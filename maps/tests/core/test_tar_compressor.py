import os
import tarfile
import tempfile
from typing import AsyncIterable, Tuple

import pytest

from maps_adv.export.lib.core.utils import tar_compressor

pytestmark = [pytest.mark.asyncio]


async def test_returns_tar_gz_archive_with_expected_structure(faker):
    with tempfile.TemporaryDirectory() as tmp_dir:
        filename = os.path.join(tmp_dir, "".join(faker.random_letters()) + ".tgz")

        async def _gen() -> AsyncIterable[Tuple[str, bytes]]:
            yield "file1", b"file1content"
            yield "file2", b"file2 content"
            yield "dir/file3", b"file3"

        await tar_compressor(_gen(), filename=filename)

        tar = tarfile.open(filename, "r:gz")

        assert tar.getnames() == ["file1", "file2", "dir/file3"]
        assert tar.getmember("file1").size == 12
        assert tar.getmember("file2").size == 13
