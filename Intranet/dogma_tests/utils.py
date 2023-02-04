import vcr
import yatest

path = yatest.common.source_path('intranet/dogma/fixtures/cassettes')
test_vcr = vcr.VCR(
    cassette_library_dir=path,
)
