from maps.qa.coverage_quality.lib.eval_report import (
    make_job,
    make_testable_job
)
from nile.api.v1 import (
    clusters,
    datetime as nd,
    statface as ns
)
from nile.api.v1.local import ListSink, FileSource
from nile.api.v1.record import Record
from yatest.common import source_path


def data_path(filename):
    return source_path('maps/qa/coverage_quality/ut/data/' + filename)


def test_smoke():
    cluster = clusters.MockCluster()
    job = cluster.job()
    make_job(
        job,
        list(nd.date_range('2019-10-01', '2019-10-01', step=1, stringify=False)),
        'tmp/stub',
        ['aq-login-00'],
        ns.StatfaceBetaClient(),
        '//tmp'
    )
    job.local_run(
        sources={},
        sinks={},
    )


def test_denormalized():
    cluster = clusters.MockCluster()
    job = cluster.job()
    make_testable_job(
        job,
        list(nd.date_range('2019-10-01', '2019-10-01', step=1, stringify=False)),
        ['qa-login-00'],
        '//tmp'
    )
    output = []
    job.local_run(
        sources={
            'in_types': FileSource(data_path('types.yson'), format='yson'),
            'in_resolutions': FileSource(data_path('resolutions.yson'), format='yson'),
            'in_users': FileSource(data_path('users.yson'), format='yson'),
            'in_priorities': FileSource(data_path('priorities.yson'), format='yson'),
            'in_issues': FileSource(data_path('in_issues-denormalized.yson'), format='yson'),
        },
        sinks={'out_denormalized': ListSink(output)},
    )
    correct = [Record(
        author=112000,
        author_login='mih',
        created=1470665456842,
        customFields={},
        issue_key='NMAPS-4072',
        priority='blocker',
        priority_id='priority-1',
        resolution='fixed',
        resolution_id='resolution-1',
        type='bug',
        type_id='type-1')]
    assert output == correct
