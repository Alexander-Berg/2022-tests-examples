from infra.rtc.rebootctl.lib import job, script
# from google.protobuf import json_format
# import yaml
import yatest.common

# from infra.rtc.rebootctl.proto import reboot_pb2


def test_jobs_has_filter():
    for sc in job.Task.__subclasses__():
        assert getattr(sc, 'filter', None), 'Task {} has no filter'.format(sc.__name__)


def test_task_selection():
    for s in script.iter_scripts(yatest.common.source_path('infra/rtc/rebootctl/lib/tests/all-tasks.yaml')):
        task, opts = script.get_task_and_options(s.spec)
        assert issubclass(task, job.Task)


def test_filter_Sleep():
    filter = job.Sleep.filter
    assert not filter({'name': 'test'})
    assert not filter({'name': 'test', 'health': 'ok', 'status': 'dead'})
    assert filter({'name': 'test', 'health': {'check_statuses': {'walle_clocksource': 'failed'}}, 'status': 'ready'})


def test_filter_KernelUpdate():
    filter = job.KernelUpdate.filter
    assert not filter({'name': 'test'})
    assert not filter({'name': 'test', 'health': 'ok', 'status': 'dead'})
    assert not filter({'name': 'test', 'health': {'check_statuses': {'walle_clocksource': 'failed'}}, 'status': 'ready'})
    assert filter({'name': 'test', 'health': {'check_statuses': {'need_reboot_kernel': 'failed'}}, 'status': 'ready'})


def test_filter_FirmwareUpdate():
    filter = job.FirmwareUpdate.filter
    assert not filter({'name': 'test'})
    assert not filter({'name': 'test', 'health': 'ok', 'status': 'dead'})
    assert not filter({'name': 'test', 'health': {'check_statuses': {'walle_clocksource': 'failed'}}, 'status': 'ready'})
    assert filter({'name': 'test', 'health': {'check_statuses': {'walle_firmware': 'failed'}}, 'status': 'ready'})
