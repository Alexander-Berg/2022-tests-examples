import pytest

from dataclasses import dataclass
from enum import Enum

from maps.infra.ecstatic.common.experimental_worker.lib.data_storage import DataStorageProxy, DatasetState
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Coordinator, Dataset
from maps.infra.ecstatic.common.experimental_worker.lib.coordinator_polling_job import CoordinatorPollingJob
from maps.infra.ecstatic.common.worker.lib.utils import Torrent
from maps.infra.ecstatic.common.experimental_worker.lib.hooks_job import HooksJobAction


class State(Enum):
    READY = 1
    ACTIVE = 2
    SWITCH_FAILED = 3


@dataclass
class UpdateSwitchesTestCase:
    name: str
    initial_states: dict[Dataset, State]
    coordinator_versions: list[Dataset]
    expected_hooks: list[Dataset]

    def __str__(self):
        return self.name


CASES = [
    UpdateSwitchesTestCase(
        name='simple',
        initial_states={Dataset('pkg-a', '1'): State.READY},
        coordinator_versions=[
            Dataset('pkg-a', '1')
        ],
        expected_hooks=[Dataset('pkg-a', '1')]
    ),
    UpdateSwitchesTestCase(
        name='empty_version',
        initial_states={Dataset('pkg-a', '1'): State.ACTIVE},
        coordinator_versions=[
            Dataset('pkg-a', '__NONE__')
        ],
        expected_hooks=[Dataset('pkg-a', None)]
    ),
    UpdateSwitchesTestCase(
        name='nothing_to_switch',
        initial_states={Dataset('pkg-a', '1'): State.ACTIVE},
        coordinator_versions=[
            Dataset('pkg-a', '__CURRENT__')
        ],
        expected_hooks=[]
    ),
    UpdateSwitchesTestCase(
        name='switch_failed',
        initial_states={Dataset('pkg-a', '1'): State.SWITCH_FAILED},
        coordinator_versions=[
            Dataset('pkg-a', '__NONE__')
        ],
        expected_hooks=[]
    ),
    UpdateSwitchesTestCase(
        name='recover_from_switch_failed',
        initial_states={Dataset('pkg-a', '1'): State.SWITCH_FAILED},
        coordinator_versions=[
            Dataset('pkg-a', '1')
        ],
        expected_hooks=[Dataset('pkg-a', '1')]
    ),
    UpdateSwitchesTestCase(
        name='version_upgrade',
        initial_states={
            Dataset('pkg-a', '1'): State.ACTIVE,
            Dataset('pkg-a', '2'): State.READY
        },
        coordinator_versions=[
            Dataset('pkg-a', '2')
        ],
        expected_hooks=[Dataset('pkg-a', '2')]
    ),
    UpdateSwitchesTestCase(
        name='unknown_dataset',
        initial_states={
            Dataset('pkg-a', '1'): State.ACTIVE,
            Dataset('pkg-a', '2'): State.READY
        },
        coordinator_versions=[
            Dataset('pkg-b', '__NONE__'), Dataset('pkg-a', '__CURRENT__')
        ],
        expected_hooks=[]
    ),
    UpdateSwitchesTestCase(
        name='remove_dataset',
        initial_states={
            Dataset('pkg-a', '1'): State.ACTIVE,
        },
        coordinator_versions=[
        ],
        expected_hooks=[Dataset('pkg-a', None)]
    ),
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', CASES, ids=str)
async def test_update_switches(coordinator_polling_job: CoordinatorPollingJob,
                               coordinator: Coordinator,
                               data_storage: DataStorageProxy,
                               case: UpdateSwitchesTestCase):
    for dataset, state in case.initial_states.items():
        data_storage.add_torrent(Torrent(
            hash=str(hash(dataset)),
            content=b'content',
            priority=1
        ), [dataset])
        data_storage._storages[0]._datasets_states[dataset] = DatasetState.POSTDL_DONE
        if state == State.READY:
            continue
        elif state == State.SWITCH_FAILED:
            data_storage.mark_switch_failed(dataset.name)
        elif state == State.ACTIVE:
            data_storage.set_active_version(dataset)

    coordinator.get_versions.return_value = case.coordinator_versions
    await coordinator_polling_job._update_switches()
    assert coordinator_polling_job._hooks_queue.qsize() == 1
    assert coordinator_polling_job._hooks_queue.get_nowait().type == HooksJobAction.SWITCH
    assert data_storage._switch_list == case.expected_hooks
