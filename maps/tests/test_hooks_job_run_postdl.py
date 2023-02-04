import asyncio
import functools
import pytest
from dataclasses import dataclass
from unittest import mock

from maps.infra.ecstatic.common.experimental_worker.lib.coordinator_push_job import CoordinatorPushQueue
from maps.infra.ecstatic.common.experimental_worker.lib.hooks_job import HooksJob, HooksJobQueue, HookFailedError
from maps.infra.ecstatic.common.experimental_worker.lib.data_storage import DataStorageProxy, Torrent
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Dataset
from maps.infra.ecstatic.ymtorrent.proto import torrent_pb2


@dataclass
class RunPostdlCase:
    name: str
    error: bool = False

    def __str__(self):
        return self.name


CASES = [
    RunPostdlCase(name='ok'),
    RunPostdlCase(name='error', error=True),
]


@pytest.mark.asyncio
@pytest.mark.parametrize('case', CASES, ids=str)
async def test_run_postdl(hooks_job: HooksJob, data_storage: DataStorageProxy, case: RunPostdlCase):
    dataset = Dataset(name='ds1',
                      version='1'
                      )
    data_storage.add_torrent(
        Torrent(
            hash='hash',
            content=b'content',
            priority=1
        ),
        {dataset}
    )
    if case.error:
        hooks_job._hook_controller.trigger_postdl.side_effect = HookFailedError('postdl')
    await hooks_job._run_postdl(
        HooksJobQueue.PostdlHook(Dataset(
            name='ds1',
            version='1'
        ))
    )
    assert hooks_job._hook_controller.trigger_postdl.call_count == 1
    assert hooks_job._hook_controller.trigger_postdl.call_args_list[0] == mock.call(
        dataset
    )
    push_content = hooks_job._coordinator_push_queue.get()
    if case.error:
        assert len(push_content) == 1
        assert push_content[0].dataset == dataset
    else:
        assert push_content == [CoordinatorPushQueue.PostdlDone(dataset)]


@pytest.mark.asyncio
async def test_do_not_run_twice(hooks_job: HooksJob, data_storage: DataStorageProxy):
    dataset = Dataset(name='ds1',
                      version='1'
                      )
    data_storage.add_torrent(
        Torrent(
            hash='hash',
            content=b'content',
            priority=1
        ),
        {dataset}
    )
    for _ in range(2):
        await hooks_job._run_postdl(
            HooksJobQueue.PostdlHook(Dataset(
                name='ds1',
                version='1'
            ))
        )
    assert hooks_job._hook_controller.trigger_postdl.call_count == 1
    assert hooks_job._hook_controller.trigger_postdl.call_args_list[0] == mock.call(
        dataset
    )


@pytest.mark.asyncio
async def test_async_postdl(hooks_job: HooksJob, data_storage: DataStorageProxy):
    datasets = {
        Dataset(name='ds1', version='1'),
        Dataset(name='ds1', version='2'),

    }
    data_storage.add_torrent(
        Torrent(
            hash='hash',
            content=b'content',
            priority=1
        ),
        datasets
    )
    work_cycle = asyncio.create_task(hooks_job.run())
    hooks_job._queue.put_nowait(HooksJobQueue.PostdlHook(
        Dataset(
            name='ds1',
            version='1'
        )
    ))
    hooks_job._queue.put_nowait(HooksJobQueue.PostdlHook(
        Dataset(
            name='ds1',
            version='2'
        )
    ))
    hooks_job._hook_controller.has_async_postdl.return_value = True
    hooks_job._hook_controller.trigger_postdl.side_effect = functools.partial(asyncio.sleep, 3)
    await asyncio.sleep(2)
    assert hooks_job._queue.qsize() == 0
    assert hooks_job._hook_controller.trigger_postdl.call_count == 2
    hooks_job.stop()
    await work_cycle


@pytest.mark.asyncio
async def test_postdl_not_downloaded(hooks_job: HooksJob, data_storage: DataStorageProxy, ymtorrent_stub):
    ds = Dataset('ds1', '1')
    data_storage.add_torrent(Torrent('hash', b'content', 1), {ds})
    ymtorrent_stub.isDownloaded.return_value = torrent_pb2.IsDownloadedResponse(downloaded=False)
    await hooks_job._run_postdl(HooksJobQueue.PostdlHook(ds))
    assert hooks_job._hook_controller.trigger_postdl.call_count == 0


@pytest.mark.asyncio
async def test_call_hook_for_removed_dataset(hooks_job: HooksJob, data_storage: DataStorageProxy):
    ds = Dataset('ds1', '1')
    await hooks_job._run_postdl(HooksJobQueue.PostdlHook(ds))
    assert hooks_job._hook_controller.trigger_postdl.call_count == 0
    assert ds not in data_storage
