import asyncio
import pytest
from typing import Dict
import pickle
import os
import tarfile
import tempfile


from ads_pytorch.tools.tensorboard_progress import (
    TensorBoardProgressLogger,
    TensorBoardToYTCallback,
    ProgressEntity
)


class MockYtClient:
    def __init__(self):
        self.uploaded_files = []
        self._tb_tar_path = None
        self._create_fake_init_tensorboard()

    def _create_fake_init_tensorboard(self):
        self.tmp_dir = tempfile.TemporaryDirectory()
        os.mkdir(os.path.join(self.tmp_dir.name, "tensorboard"))
        summary_writer = MockTensorBoard(
            log_dir=os.path.join(self.tmp_dir.name, "tensorboard")
        )
        summary_writer.add_scalars(
            main_tag='',
            tag_scalar_dict={"loss": 1899, "accuracy": 344, "#BatchTotal": 1},
            global_step=1
        )
        summary_writer.flush()
        self._tb_tar_path = os.path.join(self.tmp_dir.name, "archive")
        with tarfile.open(self._tb_tar_path, "w") as tar_handle:
            tar_handle.add(os.path.join(self.tmp_dir.name, "tensorboard"), arcname='.')

    def __exit__(self, exc_type, exc_value, traceback):
        self.tmp_dir.cleanup()

    def write_file(self, path, stream):
        self.uploaded_files.append((path, stream.read(), ))

    def read_file(self, path):
        return open(self._tb_tar_path, "rb")

    def exists(self, path):
        return path in ["//home/test_one_dot_with_initial"]


SOME_FILE = 'some_file'


class MockTensorBoard:
    def __init__(self, log_dir, *args, **kwargs):
        self.log_dir = log_dir
        self.log_path = os.path.join(log_dir, SOME_FILE)
        self.vals = []
        if os.path.isfile(self.log_path):
            with open(self.log_path, 'rb') as f:
                self.vals = pickle.load(f)

    def add_scalars(self, main_tag, tag_scalar_dict: Dict[str, float], global_step: int):
        self.vals.append((global_step, tag_scalar_dict))

    def flush(self):
        pickle.dump(self.vals, open(self.log_path, 'wb'))


@pytest.fixture(scope='function')
def yt_client():
    return MockYtClient()


def read_values(values):
    open('uploaded.tar', 'wb').write(values)
    tar = tarfile.open("uploaded.tar")
    tar.extractall()
    tar.close()
    print(os.listdir('.'))
    values = open(SOME_FILE, 'rb').read()
    return pickle.loads(values)


@pytest.mark.asyncio
async def test_one_dot(yt_client):
    tmp_dir = tempfile.mkdtemp()
    yt_tb_log_path = '//home/test_one_dot'
    callback = TensorBoardToYTCallback(yt_tb_log_path, yt_client)
    callback.init_logger(
        local_folder=tmp_dir,
        tb_class=MockTensorBoard
    )
    tb_logger = callback.get_logger()

    await tb_logger.log_progress([
        ProgressEntity(name='loss', value=1),
        ProgressEntity(name='accuracy', value=0.5),
        ProgressEntity(name="#BatchTotal", value=1)
    ])
    await callback(None, None, None, None)
    await asyncio.sleep(0.05)

    assert len(yt_client.uploaded_files) == 1

    upload_path, upload_values = yt_client.uploaded_files[-1]
    assert upload_path == yt_tb_log_path
    assert tb_logger._summary_writer.vals == [(1, {'loss': 1, 'accuracy': 0.5, '#BatchTotal': 1}, )]
    assert read_values(upload_values) == tb_logger._summary_writer.vals


@pytest.mark.asyncio
async def test_one_dot_with_existing_tensorboard(yt_client):
    tmp_dir = tempfile.mkdtemp()
    yt_tb_log_path = '//home/test_one_dot_with_initial'
    callback = TensorBoardToYTCallback(yt_tb_log_path, yt_client)
    callback.init_logger(
        local_folder=tmp_dir,
        tb_class=MockTensorBoard
    )
    tb_logger = callback.get_logger()

    await tb_logger.log_progress([
        ProgressEntity(name='loss', value=1),
        ProgressEntity(name='accuracy', value=0.5),
        ProgressEntity(name="#BatchTotal", value=2)
    ])
    await callback(None, None, None, None)
    await asyncio.sleep(0.05)

    assert len(yt_client.uploaded_files) == 1

    upload_path, upload_values = yt_client.uploaded_files[-1]
    assert upload_path == yt_tb_log_path
    assert tb_logger._summary_writer.vals == [(1, {"loss": 1899, "accuracy": 344, '#BatchTotal': 1}, ),
                                              (2, {'loss': 1, 'accuracy': 0.5, '#BatchTotal': 2}, )]
    assert read_values(upload_values) == tb_logger._summary_writer.vals


@pytest.mark.asyncio
async def test_many_dots_one_callback(yt_client):
    tmp_dir = tempfile.mkdtemp()
    yt_tb_log_path = '//home/test_many_dots_one_callback'
    callback = TensorBoardToYTCallback(yt_tb_log_path, yt_client)
    callback.init_logger(
        local_folder=tmp_dir,
        tb_class=MockTensorBoard
    )
    tb_logger = callback.get_logger()

    for i in range(10):
        await tb_logger.log_progress([
            ProgressEntity(name='loss', value=10 - i),
            ProgressEntity(name='accuracy', value=i/10),
            ProgressEntity(name="#BatchTotal", value=i)
        ])
    await callback(None, None, None, None)
    await asyncio.sleep(0.05)

    assert len(yt_client.uploaded_files) == 1

    upload_path, upload_values = yt_client.uploaded_files[-1]
    assert upload_path == yt_tb_log_path
    assert read_values(upload_values) == tb_logger._summary_writer.vals


@pytest.mark.asyncio
async def test_one_dot_many_callbacks(yt_client):
    tmp_dir = tempfile.mkdtemp()
    yt_tb_log_path = '//home/test_one_dot_many_callbacks'
    callback = TensorBoardToYTCallback(yt_tb_log_path, yt_client)
    callback.init_logger(
        local_folder=tmp_dir,
        tb_class=MockTensorBoard
    )
    tb_logger = callback.get_logger()

    await tb_logger.log_progress([
        ProgressEntity(name='loss', value=1),
        ProgressEntity(name='accuracy', value=0.5),
        ProgressEntity(name="#BatchTotal", value=0)
    ])

    for i in range(1, 10):
        await callback(None, None, None, None)
        await asyncio.sleep(0.05)

        assert len(yt_client.uploaded_files) == i

        upload_path, upload_values = yt_client.uploaded_files[-1]
        assert upload_path == yt_tb_log_path
        assert read_values(upload_values) == tb_logger._summary_writer.vals


@pytest.mark.asyncio
async def test_many_dots_many_callbacks(yt_client):
    tmp_dir = tempfile.mkdtemp()
    yt_tb_log_path = '//home/test_many_dots_many_callbacks'
    callback = TensorBoardToYTCallback(yt_tb_log_path, yt_client)
    callback.init_logger(
        local_folder=tmp_dir,
        tb_class=MockTensorBoard
    )
    tb_logger = callback.get_logger()

    for i in range(1, 5):
        for j in range(1, 5):
            await tb_logger.log_progress([
                ProgressEntity(name='loss', value=i + j),
                ProgressEntity(name='accuracy', value=(16 - i * j) / 16),
                ProgressEntity(name="#BatchTotal", value=i)
            ])

        await callback(None, None, None, None)
        await asyncio.sleep(0.05)

        assert len(yt_client.uploaded_files) == i

        upload_path, upload_values = yt_client.uploaded_files[-1]
        assert upload_path == yt_tb_log_path
        assert read_values(upload_values) == tb_logger._summary_writer.vals


@pytest.mark.asyncio
async def test_many_callbacks_frequently(yt_client):
    tmp_dir = tempfile.mkdtemp()
    yt_tb_log_path = '//home/test_many_callbacks_frequently'
    callback = TensorBoardToYTCallback(yt_tb_log_path, yt_client)
    callback.init_logger(
        local_folder=tmp_dir,
        tb_class=MockTensorBoard
    )
    tb_logger = callback.get_logger()

    for i in range(1, 10000):
        await tb_logger.log_progress([
            ProgressEntity(name='loss', value=10000 - i),
            ProgressEntity(name='accuracy', value=i/10000),
            ProgressEntity(name="#BatchTotal", value=i)
        ])

    for j in range(100):
        await callback(None, None, None, None)

    assert 0 < len(yt_client.uploaded_files) < 100
    upload_path, upload_values = yt_client.uploaded_files[-1]
    assert upload_path == yt_tb_log_path
    assert read_values(upload_values) == tb_logger._summary_writer.vals
