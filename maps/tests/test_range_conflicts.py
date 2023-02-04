from nile.api.v1 import (
    clusters,
    local,
)

from maps.geoq.hypotheses.flats.lib import range_conflicts

from data_range_conflicts import (
    FT_ADDR,
    FT_NM,
    GEOQ_RANGES,
    HYPOTHESIS,
    MATCHER_OUTPUT,
    NMAPS_RANGES,
)
from utils import (
    prepare_source,
    to_records,
)


def test_prepare_range_conflicts_hypothesis():
    cluster = clusters.MockCluster()
    job = cluster.job()
    range_conflicts.prepare(job, [''])

    hypothesis = []
    job.local_run(
        sources={
            'ft_addr': prepare_source(FT_ADDR),
            'ft_nm': prepare_source(FT_NM),
            'geoq_ranges': prepare_source(GEOQ_RANGES),
            'matcher_output': prepare_source(MATCHER_OUTPUT),
            'nmaps_ranges': prepare_source(NMAPS_RANGES),
        },
        sinks={
            'hypothesis': local.ListSink(hypothesis),
        }
    )

    assert hypothesis == to_records(HYPOTHESIS)
