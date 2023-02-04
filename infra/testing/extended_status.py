import helper

import infra.callisto.protos.deploy.tables_pb2 as tables  # noqa
import search.plutonium.deploy.proto.sources_pb2 as sources  # noqa


def test_positive_notification():
    content = 'hello, world'
    with helper.Helper() as h:
        h.add_static_target('local/path', content)
        h.add_notification('local/path')
        h.run_workload()
        h.run_deployer()
        assert h.get_content('local/path') == content
        assert h.get_status('local/path') == tables.EDownloadState.ACTIVE


def test_negative_notification():
    content = 'hello, world'
    with helper.Helper() as h:
        h.add_static_target('local/path', content)
        h.add_notification('local/path')
        h.run_workload(active=False)
        h.run_deployer()
        assert h.get_content('local/path') == content
        assert h.get_status('local/path') == tables.EDownloadState.PREPARED


def test_extended_notification():
    content = 'hello, world'
    with helper.Helper() as h:
        h.add_static_target('local/path', content)
        h.add_notification('local/path', extended=True)
        h.run_workload(active=False, extended=True)
        h.run_deployer()
        assert h.get_content('local/path') == content
        assert h.get_status('local/path') == tables.EDownloadState.PREPARED
