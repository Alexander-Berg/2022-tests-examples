from maps.automotive.qa.metrics.time_in_test.lib.eval_report import (
    make_job,
    make_testable_job,
)
from maps.automotive.qa.metrics.common.lib.ut_helpers import (
    read_file_as_stream
)
from nile.api.v1 import (
    clusters,
    datetime as nd,
    statface as ns,
    local as nl
)
from yatest.common import source_path
from freezegun import freeze_time


def data_path(filename):
    return source_path('maps/automotive/qa/metrics/time_in_test/ut/data/' + filename)


def test_smoke():
    cluster = clusters.MockCluster()
    job = cluster.job()
    make_job(
        job,
        list(nd.date_range('2019-10-01', '2019-10-01', step=1, stringify=False)),
        ns.StatfaceBetaClient(),
        []
    )
    job.local_run(
        sources={},
        sinks={},
    )


@freeze_time("2020-02-02 00:00:00", tz_offset=0)
def test_issue_testing_hours():
    cluster = clusters.MockCluster()
    job = cluster.job()
    make_testable_job(
        job,
        list(nd.date_range('2019-10-29', '2019-10-29', step=1, stringify=False)),
        ['mih']
    )
    output = []
    job.local_run(
        sources={
            'statuses': nl.FileSource(data_path('statuses.yson'), format='yson'),
            'issue_events': nl.FileSource(data_path('issue_testing_hours.issue_events.yson'), format='yson'),
        },
        sinks={'issue_testing_hours': nl.ListSink(output)},
    )
    correct = read_file_as_stream(data_path('issue_testing_hours.correct.yson'))
    assert output == correct


@freeze_time("2020-02-02 00:00:00", tz_offset=0)
def test_issue_testing_hours_joined():
    cluster = clusters.MockCluster()
    job = cluster.job()
    make_testable_job(
        job,
        list(nd.date_range('2019-10-29', '2019-10-29', step=1, stringify=False)),
        ['mih']
    )
    output = []
    job.local_run(
        sources={
            'statuses': nl.FileSource(data_path('statuses.yson'), format='yson'),
            'types': nl.FileSource(data_path('types.yson'), format='yson'),
            'priorities': nl.FileSource(data_path('priorities.yson'), format='yson'),
            'issues': nl.FileSource(data_path('issue_testing_hours_joined.issues.yson'), format='yson'),
            'issue_events': nl.FileSource(data_path('issue_testing_hours_joined.issue_events.yson'), format='yson'),
        },
        sinks={'issue_testing_hours_joined': nl.ListSink(output)},
    )
    correct = read_file_as_stream(data_path('issue_testing_hours_joined.correct.yson'))
    assert output == correct
