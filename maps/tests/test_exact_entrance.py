from nile.api.v1 import (
    clusters,
    local,
)

from maps.geoq.hypotheses.flats.lib import exact_entrance

from data_exact_entrance import (
    ENTRANCE_FLAT_RANGE,
    FT_ADDR,
    FT_CENTER,
    FT_NM,
    HYPOTHESIS_DATA,
    NODE,
)
from utils import (
    prepare_source,
    to_records,
)


def test_prepare_exact_entrance_hypothesis():
    cluster = clusters.MockCluster()
    job = cluster.job()
    exact_entrance.prepare(job, '')

    hypothesis = []
    job.local_run(
        sources={
            'entrance_flat_range': prepare_source(ENTRANCE_FLAT_RANGE),
            'ft_addr': prepare_source(FT_ADDR),
            'ft_center': prepare_source(FT_CENTER),
            'ft_nm': prepare_source(FT_NM),
            'node': prepare_source(NODE),
        },
        sinks={
            'output_table': local.ListSink(hypothesis),
        }
    )

    assert hypothesis == to_records(HYPOTHESIS_DATA)
