import dataclasses
import pickle
import tempfile
import time
import os
import torch.nn
import torch.nn.functional
import pytest
import hashlib
from ads_pytorch.yt.file_system_adapter_mock import CypressAdapterMock
from ads_pytorch.core import BaseParameterServerModule, ParameterServerOptimizer
from ads_pytorch.hash_embedding import HashEmbedding
from ads_pytorch.hash_embedding.hash_embedding import (
    create_hash_table,
    create_item
)
from ads_pytorch.hash_embedding.optim import EmbeddingAdamOptimizer
from ads_pytorch.core.model_serializer import ModelSaver, ModelLoader
from ads_pytorch.tools.async_worker_pool import BaseAsyncWorkerPool, BaseAsyncWorker


from ads_pytorch.online_learning.production.artifact.folder import (
    branch_path,
    artifact_values_path,
    state_path,
    snapshot_path,
    BranchDirectoryArtifactStorage
)
from ads_pytorch.online_learning.production.callbacks.save_state import (
    FullStateSnapshotter,
    AbstractSnapshotInfo
)
from ads_pytorch.online_learning.production.uri import ProdURI
from ads_pytorch.online_learning.production.dataset import DatetimeURI
from ads_pytorch.tools.buffered_parallel_reader import BufferedParallelReader


@dataclasses.dataclass(frozen=True)
class MySnapshotInfo(AbstractSnapshotInfo):
    call_id: int

    def dumps(self) -> bytes:
        return pickle.dumps(self.call_id)

    @classmethod
    def loads(cls, string: bytes):
        return MySnapshotInfo(pickle.loads(string))


class MyFullStateSnapshotter(FullStateSnapshotter):
    def __init__(self, *args, **kwargs):
        super(MyFullStateSnapshotter, self).__init__(*args, **kwargs)
        self._call_id = 0

    async def _create_snapshot_info(
            self,
            model: BaseParameterServerModule,
            optimizer: ParameterServerOptimizer,
            loss: torch.nn.Module,
            uri: ProdURI
    ) -> AbstractSnapshotInfo:
        try:
            return MySnapshotInfo(call_id=self._call_id)
        finally:
            self._call_id += 1


class Model(BaseParameterServerModule):
    def __init__(self):
        super(Model, self).__init__()
        self.hash1 = HashEmbedding(create_hash_table("adam", 100))
        self.hash2 = HashEmbedding(create_hash_table("adam_half", 103))
        self.net = torch.nn.Sequential(
            torch.nn.Linear(203, 10),
            torch.nn.ReLU(inplace=True),
            torch.nn.Linear(10, 1)
        )
        self._init_params()

    def _init_params(self):
        for i in range(10):
            item = create_item("adam", 100)
            item.w = torch.randn(100)
            self.hash1.insert_item(i, item)

            item = create_item("adam_half", 103)
            item.w = torch.randn(103)
            self.hash2.insert_item(i, item)

    def async_forward(self, inputs):
        return self.hash1(*inputs["hash1"]), self.hash2(*inputs["hash2"])

    def sync_forward(self, async_outputs):
        return self.net(torch.cat(async_outputs, dim=1))


#################################################
#                SAVE/LOAD POOLS                #
#################################################


class DiskSaveWorker(BaseAsyncWorker):
    async def _do_job(self, data_stream, path, tx):
        assert tx is not None
        if not os.path.exists(os.path.dirname(path)):
            os.makedirs(os.path.dirname(path))
        with open(path, 'wb') as f:
            if isinstance(data_stream, (str, bytes)):
                f.write(data_stream)
            else:
                async for data in data_stream:
                    f.write(data)


class DiskSavePool(BaseAsyncWorkerPool):
    def create_new_worker(self, new_rank: int):
        return DiskSaveWorker(new_rank)


class DiskLoadWorker(BaseAsyncWorker):
    async def _do_job(self, path):
        with open(path, 'rb') as f:
            return f.read()


class DiskLoadPool(BaseAsyncWorkerPool):
    def create_new_worker(self, new_rank: int):
        return DiskLoadWorker(new_rank)


def make_train_state():
    model = Model()
    optimizer = ParameterServerOptimizer(
        EmbeddingAdamOptimizer([model.hash1.parameter_with_hash_table]),
        EmbeddingAdamOptimizer([model.hash2.parameter_with_hash_table]),
        torch.optim.Adam(model.deep_parameters())
    )
    loss = torch.nn.MSELoss()
    return model, optimizer, loss


BRANCH_NAME = "b1"


def build_dir_and_branch(model_dir):
    os.makedirs(os.path.join(model_dir, "branches"))
    os.makedirs(branch_path(fs=CypressAdapterMock(), model_yt_dir=model_dir, branch_name=BRANCH_NAME))
    os.makedirs(state_path(fs=CypressAdapterMock(), model_yt_dir=model_dir, branch_name=BRANCH_NAME))


async def save_model(model, optimizer, loss, model_dir, branch_name, artifact_names):
    # unfortunately, we can't use asyncio fixtures

    save_pool = DiskSavePool(workers_count=5)

    model_saver = ModelSaver(
        folder="rfgecnf",
        file_system_adapter=CypressAdapterMock(),
        save_pool=save_pool
    )

    snapshotter = MyFullStateSnapshotter(
        model_saver=model_saver,
        file_system_adapter=CypressAdapterMock(),
        save_pool=save_pool,
        artifact_storage=BranchDirectoryArtifactStorage(
            fs_adapter=CypressAdapterMock(),
            model_yt_dir=model_dir,
            branch_name=branch_name
        ),
        state_path=state_path(fs=CypressAdapterMock(), model_yt_dir=model_dir, branch_name=branch_name),
        artifact_names=artifact_names
    )

    await snapshotter(
        model=model,
        optimizer=optimizer,
        loss=loss,
        uri=ProdURI(DatetimeURI("//home/AHAHA_URI", date=datetime.datetime.now()), force_skip=False)
    )


def calc_file_hash(fname):
    hash_md5 = hashlib.sha256()
    with open(fname, "rb") as f:
        for chunk in iter(lambda: f.read(4096), b""):
            hash_md5.update(chunk)
    return hash_md5.hexdigest()


@pytest.mark.parametrize(
    "artifact_names",
    [
        [],
        ["art1"],
        ["art2", "art3"]
    ],
    ids=['NoArt', 'OneArt', 'TwoArt']
)
@pytest.mark.asyncio
async def test_all_snapshots_equal(artifact_names):
    model, optimizer, loss = make_train_state()
    with tempfile.TemporaryDirectory() as model_dir:
        build_dir_and_branch(model_dir=model_dir)
        fs = CypressAdapterMock()
        branch_name = BRANCH_NAME

        await save_model(
            model=model,
            optimizer=optimizer,
            loss=loss,
            model_dir=model_dir,
            branch_name=branch_name,
            artifact_names=artifact_names
        )

        # test that all data in all folders are equal
        folders = [
            fs.path_join(
                artifact_values_path(fs=fs, model_yt_dir=model_dir, branch_name=branch_name, artifact_name=art),
                '0'
            )
            for art in artifact_names
        ]
        folders.append(state_path(fs=fs, model_yt_dir=model_dir, branch_name=branch_name))

        files = sorted(os.listdir(folders[0]))
        assert all(sorted(os.listdir(folder)) == files for folder in folders)

        for filename in files:
            assert len(set(calc_file_hash(os.path.join(folder, filename)) for folder in folders)) == 1


@pytest.mark.asyncio
async def test_clean_save_state():
    model, optimizer, loss = make_train_state()
    with tempfile.TemporaryDirectory() as model_dir:
        build_dir_and_branch(model_dir=model_dir)
        branch_name = BRANCH_NAME
        fs = CypressAdapterMock()
        # Add a fake file to test that each save model to the state dir will clean previous files
        state_folder = state_path(fs=fs, model_yt_dir=model_dir, branch_name=branch_name)
        if not await fs.exists(state_folder):
            await fs.create_directory(state_folder)
        with open(fs.path_join(state_folder, "ahaha"), "wt") as f:
            f.write("111")

        branch_name = BRANCH_NAME

        await save_model(
            model=model,
            optimizer=optimizer,
            loss=loss,
            model_dir=model_dir,
            branch_name=branch_name,
            artifact_names=[]
        )

        # Test clean save
        assert not await fs.exists(fs.path_join(state_folder, "ahaha"))


@pytest.mark.asyncio
async def test_snapshot_info():
    model, optimizer, loss = make_train_state()
    with tempfile.TemporaryDirectory() as model_dir:
        build_dir_and_branch(model_dir=model_dir)
        fs = CypressAdapterMock()
        branch_name = BRANCH_NAME

        await save_model(
            model=model,
            optimizer=optimizer,
            loss=loss,
            model_dir=model_dir,
            branch_name=branch_name,
            artifact_names=[]
        )

        snap_path = snapshot_path(fs, state_path(fs=fs, model_yt_dir=model_dir, branch_name=branch_name))
        assert await fs.exists(snap_path)

        with open(snap_path, "rb") as f:
            info = MySnapshotInfo.loads(f.read())
        assert info.call_id == 0


# it's part of interface, we test it. Somebody may use with defaults
def test_default_frequency_settings():
    model, optimizer, loss = make_train_state()
    with tempfile.TemporaryDirectory() as model_dir:
        build_dir_and_branch(model_dir=model_dir)
        branch_name = BRANCH_NAME

        save_pool = DiskSavePool(workers_count=5)

        model_saver = ModelSaver(
            folder="rfgecnf",
            file_system_adapter=CypressAdapterMock(),
            save_pool=save_pool
        )

        snapshotter = MyFullStateSnapshotter(
            model_saver=model_saver,
            file_system_adapter=CypressAdapterMock(),
            save_pool=save_pool,
            artifact_storage=BranchDirectoryArtifactStorage(
                fs_adapter=CypressAdapterMock(),
                model_yt_dir=model_dir,
                branch_name=branch_name
            ),
            state_path=state_path(fs=CypressAdapterMock(), model_yt_dir=model_dir, branch_name=branch_name),
            artifact_names=[]
        )

        assert snapshotter.min_frequency == 0
        assert snapshotter.force_uri_frequency == 24


@pytest.mark.parametrize('save_count', [1, 3])
@pytest.mark.asyncio
async def test_save_load_works_ok(save_count):
    torch.manual_seed(12345)
    model, optimizer, loss = make_train_state()

    # Make a fake forward-backward-optimize step to initialize optimizer state
    data = torch.LongTensor(list(range(10)))
    data_len = torch.IntTensor([10])
    out = model({"hash1": [data, data_len], "hash2": [data, data_len]})
    torch.nn.functional.mse_loss(out, torch.FloatTensor([1])).backward()
    optimizer.step()

    with tempfile.TemporaryDirectory() as model_dir:
        build_dir_and_branch(model_dir=model_dir)
        branch_name = BRANCH_NAME
        fs = CypressAdapterMock()

        for _ in range(save_count):
            await save_model(
                model=model,
                optimizer=optimizer,
                loss=loss,
                model_dir=model_dir,
                branch_name=branch_name,
                artifact_names=[]
            )

        torch.manual_seed(54321)
        model2, optimizer2, loss2 = make_train_state()

        # sanity check
        for p1, p2 in zip(model.deep_parameters(), model2.deep_parameters()):
            assert not torch.allclose(p1, p2)

        stream_reader = BufferedParallelReader(
            reader_pool=DiskLoadPool(workers_count=5),
            max_memory=5 * 2 ** 20,
        )

        loader = ModelLoader(
            folder=state_path(fs=fs, model_yt_dir=model_dir, branch_name=branch_name),
            file_system_adapter=fs,
            buffered_reader=stream_reader
        )

        await loader.load_model_and_optimizer(model=model2, optimizer=optimizer2)

        for p1, p2 in zip(model.deep_parameters(), model2.deep_parameters()):
            assert torch.allclose(p1, p2)


#################################################
#                    DATETIME                   #
#################################################


import datetime
from ads_pytorch.online_learning.production.callbacks.datetime_save_state import (
    DatetimeFullStateSnapshotter,
    DatetimeSnapshotInfo,
    DatetimeURI
)


def test_datetime_snapshot_info_serialization():
    date = datetime.datetime(2019, 1, 17, 22, 34, 19)
    info = DatetimeSnapshotInfo(date=date)
    serialized = info.dumps()
    deserialized = DatetimeSnapshotInfo.loads(serialized)
    assert info == deserialized


def test_datetime_snapshotter_inheritance():
    # Extremely stupid test to signal that many expected features of the snapshotter
    # will be lost and developer must take care of supporting and testing them
    # (for example, infiltrate it to the tests above)
    assert issubclass(DatetimeFullStateSnapshotter, FullStateSnapshotter)


@pytest.mark.asyncio
async def test_datetime_snapshotter():
    torch.manual_seed(12345)
    model, optimizer, loss = make_train_state()

    # Make a fake forward-backward-optimize step to initialize optimizer state
    data = torch.LongTensor(list(range(10)))
    data_len = torch.IntTensor([10])
    out = model({"hash1": [data, data_len], "hash2": [data, data_len]})
    torch.nn.functional.mse_loss(out, torch.FloatTensor([1])).backward()
    optimizer.step()

    date_to_save = datetime.datetime(2019, 1, 17, 22, 34, 19)

    with tempfile.TemporaryDirectory() as model_dir:
        build_dir_and_branch(model_dir=model_dir)
        branch_name = BRANCH_NAME
        fs = CypressAdapterMock()

        save_pool = DiskSavePool(workers_count=5)

        model_saver = ModelSaver(
            folder="rfgecnf",
            file_system_adapter=CypressAdapterMock(),
            save_pool=save_pool
        )

        snapshotter = DatetimeFullStateSnapshotter(
            model_saver=model_saver,
            file_system_adapter=CypressAdapterMock(),
            save_pool=save_pool,
            artifact_storage=BranchDirectoryArtifactStorage(
                fs_adapter=CypressAdapterMock(),
                model_yt_dir=model_dir,
                branch_name=branch_name
            ),
            state_path=state_path(fs=CypressAdapterMock(), model_yt_dir=model_dir, branch_name=branch_name),
            artifact_names=[]
        )

        await snapshotter(
            model=model,
            optimizer=optimizer,
            loss=loss,
            uri=ProdURI(DatetimeURI(uri="//home/wlnckj", date=date_to_save), force_skip=False)
        )

        snap_path = snapshot_path(
            fs,
            state_path(fs=fs, model_yt_dir=model_dir, branch_name=branch_name)
        )
        assert await fs.exists(snap_path)

        with open(snap_path, "rb") as f:
            info = DatetimeSnapshotInfo.loads(f.read())
        assert info.date == date_to_save
