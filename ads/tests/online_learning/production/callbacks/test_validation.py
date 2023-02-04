import pytest
import tempfile
import json
import torch.nn
import torch.nn.functional
import datetime
from typing import Dict, Any, List
from ads_pytorch.model_calcer.minibatch_worker import MinibatchWorkerPool
from ads_pytorch.model_calcer.minibatch_record import MinibatchRecord
from ads_pytorch.yt.file_system_adapter_mock import CypressAdapterMock
from ads_pytorch.tools.async_worker_pool import BaseAsyncWorkerPool, BaseAsyncWorker

from ads_pytorch.online_learning.production.artifact.folder import (
    BranchDirectoryArtifactStorage
)

from ads_pytorch.online_learning.production.callbacks.validation import (
    Validator,
    AbstractValidatorPredictionFormatter,
    BaseStreamReader
)
from ads_pytorch.model_calcer.calcer import AbstractCalcer, CalcerResults
from ads_pytorch.yt.prediction import PredictionUnit
from ads_pytorch.online_learning.production.uri import ProdURI, DatetimeURI
from ads_pytorch.yt.table_path import TablePath
from ads_pytorch.highlevel_interface import YTMinibatchRecordStreamReaderTable

EVAL_SHIFT = 1e7
DOWNLOAD_SHIFT = 1e5


class FakePredictionsFormatter(AbstractValidatorPredictionFormatter):
    async def __call__(
        self,
        uri,
        prediction_result_uri,
        all_predictions: List[PredictionUnit]
    ):
        return all_predictions, None


class FakeCalcer(AbstractCalcer):
    async def __call__(self, inputs, targets, data_identity: int = 0) -> CalcerResults:
        return CalcerResults(
            metrics={"metric": int(inputs["x1"]) % 10},
            input_gradients=None,
            predictions={"x1": inputs["x1"] + EVAL_SHIFT},
            losses={"loss": int(inputs["x1"]) % 10}
        )

    async def send_command(self, name: str, value: Any):
        pass

    def get_device(self) -> torch.device:
        return torch.device("cpu")


# Fake upload worker: just remember what we've scheduled to upload


class FakeUploadWorker(BaseAsyncWorker):
    def __init__(self, rank, recorder):
        super(FakeUploadWorker, self).__init__(rank)
        self.recorder = recorder

    async def _do_job(self, data_stream, path, metadata, tx):
        with open(path, "wt") as f:
            f.write("111")
        self.recorder.append([data_stream, path, metadata, tx])


class FakeUploadPool(BaseAsyncWorkerPool):
    def __init__(self, workers_count):
        super(FakeUploadPool, self).__init__(workers_count, )
        self.recorder = list()

    def create_new_worker(self, new_rank: int):
        return FakeUploadWorker(new_rank, self.recorder)


# Fake download worker: just add a suffix


class FakeDownloadWorker(BaseAsyncWorker):
    def __init__(self, rank):
        super(FakeDownloadWorker, self).__init__(rank)

    async def _do_job(self, chunk: int):
        return MinibatchRecord(
            inputs={"x1": torch.tensor(chunk + DOWNLOAD_SHIFT)},
            targets=torch.tensor(0),
            keys={"k1": torch.tensor(chunk + DOWNLOAD_SHIFT)}
        )


class FakeDownloadPool(BaseAsyncWorkerPool):
    def __init__(self, workers_count):
        super(FakeDownloadPool, self).__init__(workers_count=workers_count, )

    def create_new_worker(self, new_rank: int):
        return FakeDownloadWorker(new_rank)


# StreamReader


class FakeStreamReader(BaseStreamReader):
    def __init__(self, reader_pool, uri_data: Dict[str, List[int]]):
        super(FakeStreamReader, self).__init__(reader_pool=reader_pool)
        self.uri_data = uri_data

    async def _split_to_chunks(self, job: YTMinibatchRecordStreamReaderTable):
        data = self.uri_data[str(job.table)]
        for x in data:
            yield x, 0


@pytest.mark.asyncio
async def test_validate_call():
    download_pool = FakeDownloadPool(workers_count=5)
    upload_predictions_pool = FakeUploadPool(workers_count=2)
    upload_metrics_pool = FakeUploadPool(workers_count=2)
    worker_pool = MinibatchWorkerPool(
        calcer_results_handlers=[],
        calcer_factory=lambda x: FakeCalcer(),
        num_workers=3
    )

    uri_data = {
        "//home/AHAHA_URI": [2, 3, 4]
    }
    stream_reader = FakeStreamReader(reader_pool=download_pool, uri_data=uri_data)

    with tempfile.TemporaryDirectory() as model_yt_dir:
        async with Validator(
            download_pool=download_pool,
            upload_predictions_pool=upload_predictions_pool,
            upload_metrics_pool=upload_metrics_pool,
            worker_pool=worker_pool,
            stream_reader=stream_reader,
            predictions_formatter=FakePredictionsFormatter(),
            cypress_adapter=CypressAdapterMock(),
            artifact_storage=BranchDirectoryArtifactStorage(
                model_yt_dir=model_yt_dir,
                branch_name="ahaha",
                fs_adapter=CypressAdapterMock()
            ),
            features=("x1", ),
            targets=(),
            join_keys=("k1", )
        ) as validator:
            await validator(
                uri=ProdURI(DatetimeURI(TablePath("//home/AHAHA_URI"), date=datetime.datetime.now())),
                metrics_artifact_name="metrics",
                predictions_artifact_name="predictions",
            )

    assert json.loads(upload_metrics_pool.recorder[0][0]) == {"metric": 3.0, "loss": 3.0}
    predictions_reference = [
        (PredictionUnit(predictions={'x1': torch.tensor(10100002.)}, keys={'k1': torch.tensor(100002.)})),
        (PredictionUnit(predictions={'x1': torch.tensor(10100003.)}, keys={'k1': torch.tensor(100003.)})),
        (PredictionUnit(predictions={'x1': torch.tensor(10100004.)}, keys={'k1': torch.tensor(100004.)}))
    ]
    assert upload_predictions_pool.recorder[0][0] == predictions_reference
