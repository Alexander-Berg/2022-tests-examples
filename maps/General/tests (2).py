from maps.qa.common.lib.ut_helpers import (
    read_file_as_stream,
)
from maps.qa.time_to_fix.lib.eval_report import (
    make_job,
    make_testable_job
)
from nile.api.v1 import (
    clusters,
    datetime as nd,
    statface as ns,
    local as nl,
)

from nile.api.v1.record import Record
from yatest.common import source_path

import mock

TEST_PRODUCTS = ["mob-navi", "web-nmaps"]


def data_path(filename):
    return source_path('maps/qa/time_to_fix/ut/data/' + filename)


def test_smoke():
    cluster = clusters.MockCluster()
    job = cluster.job()
    make_job(
        job,
        list(nd.date_range('2019-10-01', '2019-10-01', step=1, stringify=False)),
        ns.StatfaceBetaClient(),
        '//tmp',
    )
    job.local_run(
        sources={},
        sinks={},
    )


def test_bugs_before_date():
    cluster = clusters.MockCluster()
    job = cluster.job()
    make_testable_job(
        job,
        list(nd.date_range('2019-10-01', '2019-10-01', step=1, stringify=False)),
        '//tmp',
    )
    output = []
    job.local_run(
        sources={
            'types': nl.FileSource(data_path('types.yson'), format='yson'),
            'issues': nl.FileSource(data_path('bugs_before_date.issues.yson'), format='yson'),
        },
        sinks={'bugs_before_date': nl.ListSink(output)},
    )
    correct = [
        Record(
            created_ts_ms=1470665456842,
            fielddate='2019-10-01',
            issue='issue_id-1',
            issue_key='NMAPS-4072',
            type='bug'
        ),
        Record(
            created_ts_ms=1470665456842,
            fielddate='2019-10-01',
            issue='issue_id-2',
            issue_key='NMAPS-3072',
            type='task'
        )

    ]
    assert output == correct


def test_last_issue_resolution():
    cluster = clusters.MockCluster()
    job = cluster.job()
    make_testable_job(
        job,
        list(nd.date_range('2019-10-01', '2019-10-01', step=1, stringify=False)),
        '//tmp',
    )
    output = []
    job.local_run(
        sources={
            'resolutions': nl.FileSource(data_path('resolutions.yson'), format='yson'),
            'issue_events': nl.FileSource(data_path('last_issue_resolution.issue_events.yson'), format='yson'),
        },
        sinks={'last_issue_resolution': nl.ListSink(output)},
    )

    correct = [Record(
        fielddate='2019-10-01',
        issue='issue_id-1',
        resolution='fixed',
        resolution_timestamp_ms=1470665456842
    )]
    assert output == correct


def test_bugs_age_in_window():
    cluster = clusters.MockCluster()
    job = cluster.job()
    make_testable_job(
        job,
        list(nd.date_range('2019-10-01', '2019-10-01', step=1, stringify=False)),
        '//tmp',
    )
    output = []
    job.local_run(
        sources={
            'types': nl.FileSource(data_path('types.yson'), format='yson'),
            'resolutions': nl.FileSource(data_path('resolutions.yson'), format='yson'),
            'issues': nl.FileSource(data_path('bugs_age_in_window.issues.yson'), format='yson'),
            'issue_events': nl.FileSource(data_path('bugs_age_in_window.issue_events.yson'), format='yson'),
        },
        sinks={'bugs_age_in_window': nl.ListSink(output)},
    )
    correct = read_file_as_stream(data_path('bugs_age_in_window.correct.yson'))
    assert output == correct


def test_all_issues_with_measure():
    cluster = clusters.MockCluster()
    job = cluster.job()
    make_testable_job(
        job,
        list(nd.date_range('2019-10-01', '2019-10-01', step=1, stringify=False)),
        '//tmp',
    )
    output = []
    job.local_run(
        sources={
            'types': nl.FileSource(data_path('types.yson'), format='yson'),
            'resolutions': nl.FileSource(data_path('resolutions.yson'), format='yson'),
            'priorities': nl.FileSource(data_path('priorities.yson'), format='yson'),
            'issues': nl.FileSource(data_path('all_issues_with_measure.issues.yson'), format='yson'),
            'issue_events': nl.FileSource(data_path('all_issues_with_measure.issue_events.yson'), format='yson'),
        },
        sinks={'all_issues_with_measure': nl.ListSink(output)},
        # sinks={'all_issues_with_measure':
        #            nl.FileSink(data_path('out_tmp.yson'), format=nl.YsonFormat(format='text'))},
    )
    correct = read_file_as_stream(data_path('all_issues_with_measure.correct.yson'))
    assert output == correct


def test_percentile_interpolation():
    cluster = clusters.MockCluster()
    job = cluster.job()
    make_testable_job(
        job,
        list(nd.date_range('2019-10-01', '2019-10-01', step=1, stringify=False)),
        '//tmp',
    )
    output = []
    job.local_run(
        sources={
            'all_issues_with_measure':
                nl.FileSource(data_path('percentile_interpolation.issue_measured.yson'), format='yson'),
        },
        sinks={'age_percentiles': nl.ListSink(output)},
    )
    correct = [Record(
        count=5,
        fielddate='2019-10-01',
        hours_to_fix_p100=201.97,
        hours_to_fix_p50=94.81,
        hours_to_fix_p75=163.82,
        hours_to_fix_p90=201.97,
        priority='blocked',
        product='mob-navi',
        type='bug',
        window=30)]
    assert output == correct


@mock.patch("maps.qa.time_to_fix.lib.eval_report.PRODUCTS", TEST_PRODUCTS)
def test_zero_padding():
    cluster = clusters.MockCluster()
    job = cluster.job()
    make_testable_job(
        job,
        list(nd.date_range('2019-10-01', '2019-10-02', step=1, stringify=False)),
        '//tmp',
    )
    output = []
    job.local_run(
        sources={
            'priorities': nl.FileSource(data_path('priorities.yson'), format='yson'),
            'all_issues_with_measure':
                nl.FileSource(data_path('zero_padding.issue_measured.yson'), format='yson'),
        },
        sinks={'age_percentiles_padded': nl.ListSink(output)},
    )
    correct = read_file_as_stream(data_path('zero_padding.correct.yson'))
    assert set(output) == set(correct)
