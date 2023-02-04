import pytest
from unittest.mock import create_autospec

from maps.infra.ecstatic.common.experimental_worker.lib.data_storage import DataStorageProxy
from maps.infra.ecstatic.common.experimental_worker.lib.hook_controller import HookFailedError
from maps.infra.ecstatic.common.experimental_worker.lib.hooks_job import HooksJob, HooksJobQueue
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import LockBusy, Coordinator, Dataset


@pytest.mark.asyncio
async def test_lock_failed(coordinator: Coordinator, hooks_job: HooksJob, data_storage: DataStorageProxy):
    data_storage.set_switch_list([Dataset('ds1', None)])
    coordinator.lock.side_effect = LockBusy
    hooks_job._hook_controller.has_checks.return_value = True
    with pytest.raises(LockBusy):
        await hooks_job._run_switches(HooksJobQueue.SwitchHooks())
    assert hooks_job._hook_controller.trigger_checks.call_count == 0
    assert hooks_job._hook_controller.trigger_switch.call_count == 0


@pytest.mark.asyncio
async def test_check_hook_success(coordinator: Coordinator, hooks_job: HooksJob, data_storage: DataStorageProxy):
    data_storage.set_switch_list([Dataset('ds1', None)])
    hooks_job._hook_controller.has_checks.return_value = True
    lock_obect = create_autospec(Coordinator.Lock, instance=True)
    lock_obect.__aenter__.return_value = lock_obect
    coordinator.lock.return_value = lock_obect
    await hooks_job._run_switches(HooksJobQueue.SwitchHooks())
    assert lock_obect.__aenter__.call_count == 1
    assert lock_obect.__aexit__.call_count == 1
    assert lock_obect.release.call_count == 1
    assert hooks_job._hook_controller.trigger_checks.call_count == 1
    assert hooks_job._hook_controller.trigger_switch.call_count == 1


@pytest.mark.asyncio
async def test_check_hook_failed(coordinator: Coordinator, hooks_job: HooksJob, data_storage: DataStorageProxy):
    data_storage.set_switch_list([Dataset('ds1', None)])
    hooks_job._hook_controller.has_checks.return_value = True
    hooks_job._hook_controller.trigger_checks.side_effect = HookFailedError('error')
    lock_obect = create_autospec(Coordinator.Lock, instance=True)
    lock_obect.__aenter__.return_value = lock_obect
    coordinator.lock.return_value = lock_obect
    await hooks_job._run_switches(HooksJobQueue.SwitchHooks())
    assert lock_obect.__aenter__.call_count == 1
    assert lock_obect.release.call_count == 0
    assert hooks_job._hook_controller.trigger_checks.call_count == 1
    assert hooks_job._hook_controller.trigger_switch.call_count == 1
