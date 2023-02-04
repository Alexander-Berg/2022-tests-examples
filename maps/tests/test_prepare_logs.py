from nile.api.v1 import clusters, local, Record

from maps.geoq.hypotheses.entrance.lib import prepare_logs as p


def to_records(rows):
    return [Record(**row) for row in rows]


def to_stream_resource(rows):
    return local.StreamSource(to_records(rows))


def generate_log_key(value):
    return {
        k: value
        for k in p.LOGS_KEY
    }


def test_prepare_unmatched_orders():
    data = [
        {**generate_log_key(b'1'), 'reason': b'no_entrance'},
        {**generate_log_key(b'b'), 'reason': b'something_else'}
    ]

    target = [
        generate_log_key(b'1')
    ]

    cluster = clusters.MockCluster()
    job = cluster.job()

    result = []
    p.prepare_unmatched_orders(job, '')
    job.local_run(
        sources={
            'matcher_cache': to_stream_resource(data)
        },
        sinks={
            'unmatched_orders': local.ListSink(result)
        }
    )

    assert to_records(target) == result


def test_entrance_filter():
    assert p.entrance_filter('2') is True
    assert p.entrance_filter(b'2') is True

    assert p.entrance_filter('ДВОР ЗАХОДИТЬ НАДО') is False
    assert p.entrance_filter('ДВОР ЗАХОДИТЬ НАДО'.encode()) is False

    assert p.entrance_filter('0') is False
    assert p.entrance_filter('9999') is False
