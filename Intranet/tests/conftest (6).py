import pytest
import yatest
import vcr


@pytest.fixture
def test_vcr():
    path = yatest.common.source_path('intranet/compositor_processors/vcr_cassettes')
    return vcr.VCR(
        cassette_library_dir=path,
    )
