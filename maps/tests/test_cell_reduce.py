from nile.api.v1 import (
    clusters,
    local,
)

from maps.geoq.hypotheses.flats.lib import cell_reduce

from data_cell_reduce import (
    TABLE_0,
    TABLE_1,
    TABLE_2,
    RESULT,
)
from utils import (
    prepare_source,
    to_records,
)


def mock_reducer(groups):
    for key, records in groups:
        for r in records:
            yield r


def test_cell_reduce():
    cluster = clusters.MockCluster()
    job = cluster.job()
    test_tables = {'0': '', '1': '', '2': ''}
    cell_reduce.cell_reduce(job, mock_reducer, test_tables, 2.0, 0.5)

    result = []
    job.local_run(
        sources={
            'input_0': prepare_source(TABLE_0),
            'input_1': prepare_source(TABLE_1),
            'input_2': prepare_source(TABLE_2),
        },
        sinks={
            'output_table': local.ListSink(result),
        }
    )

    assert sorted(result) == sorted(to_records(RESULT))
