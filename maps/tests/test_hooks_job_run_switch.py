import pytest
import os
from dataclasses import dataclass
from unittest import mock
from maps.infra.ecstatic.common.experimental_worker.lib.hooks_job import HooksJob, HooksJobQueue
from maps.infra.ecstatic.common.experimental_worker.lib.coordinator_push_job import CoordinatorPushQueue, CoordinatorPushAction
from maps.infra.ecstatic.common.experimental_worker.lib.hook_controller import HookFailedError
from maps.infra.ecstatic.common.experimental_worker.lib.data_storage import DataStorageProxy
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Dataset
from maps.infra.ecstatic.common.worker.lib.utils import Torrent


@dataclass
class SwitchHookTestCase:
    name: str

    datasets: dict[Dataset, bool]  # dataset -> is success?


CASES = [
    SwitchHookTestCase(
        name='success',
        datasets={Dataset('ds1', '1'): True},
    ),
    SwitchHookTestCase(
        name='failed',
        datasets={Dataset('ds1', '1'): False},
    ),
    SwitchHookTestCase(
        name='1_success_1_failed',
        datasets={
            Dataset('ds1', '1'): False,
            Dataset('ds2', '1'): True,
        },
    ),
    SwitchHookTestCase(
        name='success_empty_version',
        datasets={Dataset('ds1', None): True},
    ),
    SwitchHookTestCase(
        name='failed_empty_version',
        datasets={Dataset('ds1', None): False},
    ),

]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', CASES, ids=str)
async def test_switch_hook_call(hooks_job: HooksJob, data_storage: DataStorageProxy, case: SwitchHookTestCase):
    failed_hooks_cnt = 0
    failed_hooks_real_version_cnt = 0
    for dataset, success in case.datasets.items():
        data_storage.add_torrent(Torrent('hash', b'content'), {dataset})
        data_storage.set_postdl_done(dataset)
        if not success:
            failed_hooks_cnt += 1
            if dataset.version:
                failed_hooks_real_version_cnt += 1
    data_storage.set_switch_list(list(case.datasets.keys()))

    async def trigger_switch(dataset: Dataset):
        if case.datasets[dataset]:
            return
        raise HookFailedError('error')
    hooks_job._hook_controller.trigger_switch.side_effect = trigger_switch
    await hooks_job._run_switches(HooksJobQueue.SwitchHooks())
    assert hooks_job._hook_controller.trigger_switch.call_count == len(case.datasets)
    for i, dataset in enumerate(case.datasets.keys()):
        assert hooks_job._hook_controller.trigger_switch.call_args_list[i] == mock.call(
            dataset
        )
    assert hooks_job._hook_controller.trigger_checks.call_count == 1
    assert hooks_job._coordinator.lock.call_count == 1
    push_queue = hooks_job._coordinator_push_queue.get()
    if failed_hooks_cnt:
        assert len(push_queue) == failed_hooks_real_version_cnt
        for action in push_queue:
            assert action.type == CoordinatorPushAction.SWITCH_FAILED
            assert push_queue.count(action) == 1
            assert not case.datasets[action.dataset]  # only failed hooks
    else:
        assert push_queue == [CoordinatorPushQueue.SwitchDone({
            dataset for dataset in case.datasets.keys() if dataset.version})]
    path = data_storage._storages[0]._path
    for dataset, success in case.datasets.items():
        data_link_path = os.path.join(path.data, dataset.name)
        if dataset.version:
            assert os.path.islink(data_link_path)
            if success:
                assert os.readlink(data_link_path) == f'../versions/{dataset.name}_{dataset.version}'
            else:
                assert os.readlink(data_link_path) == '_switch_failed'
        else:
            assert not os.path.exists(data_link_path)
            assert not os.path.islink(data_link_path)


@pytest.mark.asyncio
async def test_manual_activation(hooks_job: HooksJob, data_storage: DataStorageProxy):
    dataset = Dataset('ds', '1')
    data_storage.add_torrent(Torrent('hash', b'content'), {dataset})
    await hooks_job._run_switches(
        HooksJobQueue.SwitchHooks(
            manual_activation_versions={dataset}
        )
    )
    assert hooks_job._coordinator.lock.call_count == 0
    assert hooks_job._hook_controller.trigger_switch.call_count == 1
