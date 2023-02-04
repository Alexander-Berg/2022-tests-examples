import pytest
from maps.infra.ecstatic.common.experimental_worker.proto import worker_pb2_grpc, worker_pb2
from maps.infra.ecstatic.common.experimental_worker.lib.data_storage import DataStorageProxy, Torrent
from maps.infra.ecstatic.common.experimental_worker.lib.hooks_job import HooksJobQueue, HooksJobAction
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Dataset


@pytest.mark.asyncio
async def test_show_datasets(grpc_stub: worker_pb2_grpc.WorkerStub, data_storage: DataStorageProxy):
    data_storage.add_torrent(Torrent('hash', b'content'), {Dataset('lol', 'kek')})
    resp = await grpc_stub.showDatasets(worker_pb2.ShowDatasetsRequest(active=False))
    assert len(resp.datasets)
    assert resp.datasets[0].name == 'lol'
    assert resp.datasets[0].version == 'kek'


@pytest.mark.asyncio
async def test_show_active_datasets(grpc_stub: worker_pb2_grpc.WorkerStub, data_storage: DataStorageProxy):
    data_storage.add_torrent(Torrent('hash', b'content'), {Dataset('lol', 'kek')})
    resp = await grpc_stub.showDatasets(worker_pb2.ShowDatasetsRequest(active=True))
    assert len(resp.datasets) == 0

    data_storage.set_active_version(Dataset('lol', 'kek'))
    resp = await grpc_stub.showDatasets(worker_pb2.ShowDatasetsRequest(active=True))
    assert len(resp.datasets)
    assert resp.datasets[0].name == 'lol'
    assert resp.datasets[0].version == 'kek'


@pytest.mark.asyncio
async def test_manual_activation(grpc_stub: worker_pb2_grpc.WorkerStub, hooks_queue: HooksJobQueue):
    req = worker_pb2.ManualActivationRequest()
    ds = req.datasets.add()
    ds.name, ds.version = 'lol', 'kek'
    await grpc_stub.activateDatasets(req)

    assert hooks_queue.qsize() == 1
    event: HooksJobQueue.SwitchHooks = hooks_queue.get_nowait()
    assert event.type == HooksJobAction.SWITCH
    assert event.manual_activation_versions == {Dataset('lol', 'kek')}
