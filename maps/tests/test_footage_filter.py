from nile.api.v1 import (
    clusters,
    local,
)

from maps.geoq.hypotheses.flats.lib import footage_filter

from data_footage_filter import (
    STREETVIEW,
    MIRROR,
    DATA,
    RESULT,
)
from utils import (
    prepare_source,
    to_records,
)


def test_footage_filter():
    cluster = clusters.MockCluster()
    job = cluster.job()
    footage_filter.filter_by_nearby_footage(
        job, '', '', '',
    )

    result = []
    job.local_run(
        sources={
            'streetview': prepare_source(STREETVIEW),
            'mirror': prepare_source(MIRROR),
            'input_data': prepare_source(DATA),
        },
        sinks={
            'footage_output': local.ListSink(result),
        }
    )

    assert sorted(result) == sorted(to_records(RESULT))
