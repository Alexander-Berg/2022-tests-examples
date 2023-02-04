import datetime

import search.plutonium.deploy.proto.sources_pb2 as sources  # noqa
import infra.callisto.protos.deploy.tables_pb2 as tables  # noqa

import helper


def test_static_target():
    content = 'hello, world @{}'.format(datetime.datetime.now())
    with helper.Helper() as h:
        h.add_static_target('local/path', content)
        h.run_deployer()
        assert h.get_content('local/path') == content
        assert h.get_status('local/path') == tables.EDownloadState.PREPARED


def test_compound():
    content = 'hello, world @{}'.format(datetime.datetime.now())
    static_source1 = sources.TSource(Static=sources.TStaticSource(Content=content))
    static_source2 = sources.TSource(Static=sources.TStaticSource(Content=content))

    with helper.Helper() as h:
        target = tables.TPodTarget(
            PodId=h.pod_id,
            Namespace=h.namespace,
            LocalPath='local/path',
            ResourceSpec=sources.TSource(Compound=sources.TCompoundSource(
                Sources=[
                    sources.TCompoundSource.TInternalResource(Source=static_source1, Path='compound/path/1'),
                    sources.TCompoundSource.TInternalResource(Source=static_source2, Path='compound/path/2')
                ]))
        )

        h.add_target(target)
        h.run_deployer()
        assert h.get_content('local/path/compound/path/1') == content
        assert h.get_content('local/path/compound/path/2') == content
        assert h.get_status('local/path') == tables.EDownloadState.PREPARED
