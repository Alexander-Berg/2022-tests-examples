from nile.api.v1 import (
    clusters,
    local,
)

from maps.geoq.hypotheses.flats.lib import entrance_coverage

from data_entrance_coverage import (
    ENTRANCE_FLAT_RANGE,
    FT_ADDR,
    FT_NM,
    HYPOTHESIS_DATA,
    NODE,
    ADDR,
)
from utils import (
    prepare_source,
    to_records,
)


def test_prepare_entrance_coverage_hypothesis():
    cluster = clusters.MockCluster()
    job = cluster.job()
    entrance_coverage.prepare(job, '', 0.65)

    hypothesis = []
    job.local_run(
        sources={
            'entrance_flat_range': prepare_source(ENTRANCE_FLAT_RANGE),
            'ft_addr': prepare_source(FT_ADDR),
            'ft_nm': prepare_source(FT_NM),
            'node': prepare_source(NODE),
            'addr': prepare_source(ADDR),
        },
        sinks={
            'output_table': local.ListSink(hypothesis),
        }
    )

    assert hypothesis == to_records(HYPOTHESIS_DATA)
