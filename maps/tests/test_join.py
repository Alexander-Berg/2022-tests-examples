import data
from maps.wikimap.feedback.pushes.addresses.make_pushes.lib.prepare_pushes import (
    join_full_data,
    ADDRESS_ADD,
)
from maps.wikimap.feedback.pushes.helpers import helpers
from nile.api.v1 import (
    clusters,
    local,
)
import pytest


@pytest.fixture
def job():
    cluster = clusters.MockYQLCluster()
    job = cluster.job()
    join_full_data(
        job,
        "fake_dwells",
        "fake_buildings",
        "fake_uuids",
        max_distance=200,
        assignment_type=ADDRESS_ADD,
    )
    return job


def test_join_dwells_with_bld(job):
    dwellplaces_with_bld = []
    job.local_run(
        sources={
            "dwellplaces":          local.StreamSource(data.DWELLPLACES),
            "buildings":            local.StreamSource(data.BUILDINGS),
        },
        sinks={
            "dwellplaces_with_bld": local.ListSink(dwellplaces_with_bld),
        }
    )
    helpers.compare_records_lists(
        sorted(dwellplaces_with_bld),
        sorted(data.DWELLPLACES_WITH_BLD)
    )


def test_filter_by_distance(job):
    filtered = []
    job.local_run(
        sources={
            "dwellplaces_with_bld": local.StreamSource(data.DWELLPLACES_WITH_BLD),
        },
        sinks={
            "filtered":             local.ListSink(filtered),
        }
    )
    helpers.compare_records_lists(
        sorted(filtered),
        sorted(data.FILTERED)
    )


def test_join_with_crypta(job):
    dwellplaces_with_devids = []
    job.local_run(
        sources={
            "filtered":                local.StreamSource(data.FILTERED),
            "gaid":                    local.StreamSource(data.GAID_CRYPTAID),
            "idfa":                    local.StreamSource(data.IDFA_CRYPTAID),
            "mmdevice":                local.StreamSource(data.MMDEVICE_CRYPTAID),
            "puid":                    local.StreamSource(data.PUID_CRYPTAID),
        },
        sinks={
            "dwellplaces_with_devids": local.ListSink(dwellplaces_with_devids),
        }
    )
    helpers.compare_records_lists(
        sorted(dwellplaces_with_devids),
        sorted(data.DWELLPLACES_WITH_DEVIDS)
    )


def test_join_with_uuids(job):
    result = []
    job.local_run(
        sources={
            "dwellplaces_with_devids": local.StreamSource(data.DWELLPLACES_WITH_DEVIDS),
            "uuids":                   local.StreamSource(data.UUIDS),
        },
        sinks={
            "full_data":               local.ListSink(result),
        }
    )
    helpers.compare_records_lists(
        sorted(result),
        sorted(data.RESULT)
    )
