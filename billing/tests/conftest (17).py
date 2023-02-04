from pathlib import Path
import os
from unittest.mock import AsyncMock, patch, MagicMock

import pytest

from billing.tasklets.nirvana.packing.impl.arc_client import ArcClient
from billing.tasklets.nirvana.packing.impl.nirvana_client import NirvanaClient


def get_ya_common():
    try:
        from yatest import common

        # Calling common.source_path in test's body, is, at the moment, the best reliable way to tell
        # that we are running under `ya make`. Particular file path here doesn't matter
        source_path_works = bool(
            common.source_path('billing/tasklets/nirvana/packing/tests/data/operations_with_autopack.py')
        )
        return common if source_path_works else None
    except (ModuleNotFoundError, AttributeError):
        return None


def in_ya_test():
    common = get_ya_common()
    return common is not None


class MockArcClient(ArcClient):
    def __init__(self):
        self._expected_reads = {}

    def expect_read(self, path, revision, result_file):
        self._expected_reads[(path, revision)] = result_file
        return self

    def read(self, path: Path, revision: int) -> bytes:
        if (path, revision) in self._expected_reads:
            return self.read_test_datafile(self._expected_reads[(path, revision)])
        else:
            raise Exception(f"Unexpected attempt to read path '{path}' and revision {revision}")

    def read_test_datafile(self, relative_path) -> bytes:
        path = "data/" + relative_path
        common = get_ya_common()
        if common:
            path = common.source_path("billing/tasklets/nirvana/packing/tests/" + path)
        else:
            path = os.path.dirname(__file__) + "/" + path
        with open(path, 'rb') as f:
            return f.read()


@pytest.fixture()
def mock_arc_client():
    yield MockArcClient()


@pytest.fixture()
def mock_nv_client():
    client = NirvanaClient('fake-token')
    client.session = MagicMock()
    with patch('billing.tasklets.nirvana.packing.impl.asyncio.sleep', AsyncMock()), patch(
        'billing.tasklets.nirvana.packing.impl.nirvana_client.asyncio.sleep', AsyncMock()
    ):
        yield client


@pytest.fixture()
def mock_env():
    pass


class MockResponse:
    def __init__(self, text, status, error=None):
        self._text = text
        self.status = status
        self.error = error

    async def text(self):
        return self._text

    def raise_for_status(self):
        if self.error:
            raise self.error

    async def __aexit__(self, exc_type, exc, tb):
        pass

    async def __aenter__(self):
        return self
