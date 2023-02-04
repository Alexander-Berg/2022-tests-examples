from nile.api.v1 import (
    clusters,
    local,
    Record,
)

from maps.geoq.alerts_statistics.lib import (
    schema,
    statistics,
)

from data_statistics import (
    EMPTY_LOGS,
    EMPTY_RESULT,
    NON_EMPTY_LOGS,
    NON_EMPTY_RESULT,
)


def to_records(iterable):
    return [Record(**d) for d in iterable]


def prepare_source(iterable, schema):
    return local.StreamSource(to_records(iterable), schema=schema)


def run_test_extract_from_logs(logs, expected_result):
    cluster = clusters.MockYQLCluster()
    job = cluster.job()
    statistics.extract_from_logs(job.table(''), 'dummy-user')

    extracted_result = []
    job.local_run(
        sources={
            'logs': prepare_source(logs, schema.LOGS),
        },
        sinks={
            'output': local.ListSink(extracted_result),
        }
    )

    for row in extracted_result:
        row['alerts'].sort()

    assert extracted_result == to_records(expected_result)


def test_extract_from_empty_logs():
    run_test_extract_from_logs(EMPTY_LOGS, EMPTY_RESULT)


def test_extract_from_non_empty_logs():
    run_test_extract_from_logs(NON_EMPTY_LOGS, NON_EMPTY_RESULT)
