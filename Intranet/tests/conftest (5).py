import vcr
import yatest
import pytest


@pytest.fixture
def test_vcr():
    path = yatest.common.source_path('intranet/compositor/vcr_cassettes')
    return vcr.VCR(
        cassette_library_dir=path,
    )
