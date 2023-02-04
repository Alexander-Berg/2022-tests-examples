import pytest
import asyncio
import contextlib
import dataclasses
import torch
from typing import List, Dict, AsyncContextManager, Any
from ads_pytorch.tools.async_worker_pool import BaseAsyncWorker, BaseAsyncWorkerPool
from ads_pytorch.tools.stream_reader import BaseStreamReader
from ads_pytorch.core.evaluator import (
    ModelEvaluator,
    ModelEvaluatorPool,
    ModelEvaluatorUriDescriptor,
    BasePredictionsFormatter
)
from ads_pytorch.model_calcer.minibatch_worker import (
    MinibatchWorkerPool,
    AbstractCalcer, CalcerResults
)
from ads_pytorch.model_calcer.minibatch_record import MinibatchRecord

EVAL_SHIFT = 1e7
DOWNLOAD_SHIFT = 1e5


class FakePredictionsFormatter(BasePredictionsFormatter):
    @property
    def name(self) -> str:
        return "___kmewckjnwmcewkcj___"

    async def __call__(self, uri_descr: ModelEvaluatorUriDescriptor, all_predictions):
        all_predictions = sorted(p[1] for p in all_predictions)
        return [int(x) for x in all_predictions], uri_descr


class FakeCalcer(AbstractCalcer):
    async def __call__(self, inputs, targets, data_identity: int = 0) -> CalcerResults:
        return CalcerResults(
            metrics={},
            input_gradients=None,
            predictions=inputs + EVAL_SHIFT,
            losses={}
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

    async def _do_job(self, predictions, path, metadata):
        self.recorder.append([predictions, path, metadata])


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
            inputs=torch.tensor(chunk + DOWNLOAD_SHIFT),
            targets=torch.tensor(0)
        )


class FakeDownloadPool(BaseAsyncWorkerPool):
    def __init__(self, workers_count):
        super(FakeDownloadPool, self).__init__(workers_count=workers_count, )

    def create_new_worker(self, new_rank: int):
        return FakeDownloadWorker(new_rank)


# StreamReader


class FakeStreamReader(BaseStreamReader):
    def __init__(self, reader_pool, uri_data: Dict[str, str]):
        super(FakeStreamReader, self).__init__(reader_pool=reader_pool)
        self.uri_data = uri_data

    async def _split_to_chunks(self, job: str):
        data = self.uri_data[job]
        for x in data:
            yield x, 0


# Fake evaluator


class FakeModelEvaluator(ModelEvaluator):
    async def __call__(self, uri):
        return await super(FakeModelEvaluator, self).__call__(model=None, optimizer=None, loss=None, uri=uri)

    async def wait_upload_futures(self):
        if not len(self._currently_running_futures):
            return

        done, _ = await asyncio.wait(self._currently_running_futures)
        for f in done:
            await f


class FakeModelEvaluatorPool(ModelEvaluatorPool):
    async def wait_upload_futures(self):
        for evaluator in self._model_evaluators:
            await evaluator.wait_upload_futures()


@contextlib.asynccontextmanager
async def model_evaluator(
    upload_pool,
    download_pool,
    predictions_formatter,
    stream_reader,
    eval_worker_pool,
    uri_descriptors: List[ModelEvaluatorUriDescriptor],
    use_model_evaluator_pool: bool,
) -> AsyncContextManager[FakeModelEvaluator]:
    evaluator = FakeModelEvaluator(
        download_stream_reader=stream_reader,
        upload_pool=upload_pool,
        worker_pool=eval_worker_pool,
        predictions_formatter=predictions_formatter,
        descriptors=uri_descriptors,
    )
    if use_model_evaluator_pool:
        async with FakeModelEvaluatorPool(
            upload_pool=upload_pool,
            download_pool=download_pool,
            download_stream_reader=stream_reader,
            model_evaluators=[evaluator],
            name="TestEvaluator"
        ) as eval_pool:
            yield eval_pool
    else:
        async with upload_pool:
            async with download_pool:
                async with stream_reader:
                    async with evaluator as e:
                        yield e


@contextlib.asynccontextmanager
async def basic_model_evaluator(
        uri_data: Dict[str, str],
        uri_descriptors: List[ModelEvaluatorUriDescriptor],
        use_model_evaluator_pool: bool,
) -> AsyncContextManager[FakeModelEvaluator]:
    upload_pool = FakeUploadPool(workers_count=10)
    download_pool = FakeDownloadPool(workers_count=10)
    predictions_formatter = FakePredictionsFormatter()
    stream_reader = FakeStreamReader(reader_pool=download_pool, uri_data=uri_data)
    eval_worker_pool = MinibatchWorkerPool(
        calcer_factory=lambda worker_id: FakeCalcer(),
        calcer_results_handlers=[],
        num_workers=10
    )
    async with model_evaluator(
            upload_pool=upload_pool,
            download_pool=download_pool,
            predictions_formatter=predictions_formatter,
            stream_reader=stream_reader,
            eval_worker_pool=eval_worker_pool,
            uri_descriptors=uri_descriptors,
            use_model_evaluator_pool=use_model_evaluator_pool,
    ) as evaluator:
        yield evaluator


def get_inner_evaluator(evaluator) -> FakeModelEvaluator:
    return evaluator if not isinstance(evaluator, ModelEvaluatorPool) else evaluator._model_evaluators[0]


@pytest.mark.asyncio
async def test_evaluator_skip():
    uri = ModelEvaluatorUriDescriptor(
        train_uri="x1",
        prediction_uri="p1",
        prediction_result_uri="r1"
    )
    async with basic_model_evaluator({}, [uri], False) as evaluator:
        await evaluator("something_not_presented")
        await evaluator.wait_upload_futures()

    assert evaluator.upload_pool.recorder == []


@pytest.mark.parametrize('pool', [True, False], ids=['EvaluatorPool', 'Evaluator'])
@pytest.mark.asyncio
async def test_evaluator_upload_one(pool):
    uri = ModelEvaluatorUriDescriptor(
        train_uri="x1",
        prediction_uri="p1",
        prediction_result_uri="r1"
    )
    uri_data = {
        "p1": [1, 2, 3, 4]
    }
    async with basic_model_evaluator(uri_data, [uri], pool) as evaluator:
        await evaluator("x1")
        await evaluator.wait_upload_futures()

    # here and at other tests we check that we have eval and donwload shift

    assert evaluator.upload_pool.recorder == [
        [[10100001, 10100002, 10100003, 10100004], 'r1', uri]
    ]


@pytest.mark.parametrize('pool', [True, False], ids=['EvaluatorPool', 'Evaluator'])
@pytest.mark.asyncio
async def test_evaluator_upload_multiple(pool):
    uris = [
        ModelEvaluatorUriDescriptor(
            train_uri=f"x{i}",
            prediction_uri=f"p{i}",
            prediction_result_uri=f"r{i}"
        )
        for i in (2, 3)
    ]

    uri_data = {
        "p2": [1, 2, 3, 4],
        "p3": [7, 8]
    }
    async with basic_model_evaluator(uri_data, uris, pool) as evaluator:
        inner_evaluator = get_inner_evaluator(evaluator)
        await evaluator("x1")
        await evaluator.wait_upload_futures()

        assert evaluator.upload_pool.recorder == []

        await evaluator("x2")
        await evaluator.wait_upload_futures()
        assert evaluator.upload_pool.recorder == [
            [[10100001, 10100002, 10100003, 10100004], 'r2', uris[0]],
        ]

        await evaluator("x3")
        await evaluator.wait_upload_futures()

        assert evaluator.upload_pool.recorder == [
            [[10100001, 10100002, 10100003, 10100004], 'r2', uris[0]],
            [[10100007, 10100008], 'r3', uris[1]]
        ]


@pytest.mark.parametrize('pool', [True, False], ids=['EvaluatorPool', 'Evaluator'])
@pytest.mark.asyncio
async def test_evaluator_upload_multiple_with_skips(pool):
    uris = [
        ModelEvaluatorUriDescriptor(
            train_uri=f"x{i}",
            prediction_uri=f"p{i}",
            prediction_result_uri=f"r{i}"
        )
        for i in (2, 3, 5, 7)
    ]

    uri_data = {
        "p2": [1, 2, 3, 4],
        "p3": [7, 8],
        "p5": [10, 20],
        "p7": [30, 40]
    }
    async with basic_model_evaluator(uri_data, uris, pool) as evaluator:
        for i in range(10):
            await evaluator(f"x{i}")
            await evaluator.wait_upload_futures()

    assert evaluator.upload_pool.recorder == [
        [[10100001, 10100002, 10100003, 10100004], 'r2', uris[0]],
        [[10100007, 10100008], 'r3', uris[1]],
        [[10100010, 10100020], 'r5', uris[2]],
        [[10100030, 10100040], 'r7', uris[3]]
    ]


# test empty formatting


class EmptyPredictionsFormatter(BasePredictionsFormatter):
    def __init__(self):
        self.call_counter = 0
        super(EmptyPredictionsFormatter, self).__init__()

    async def __call__(self, *args, **kwargs):
        self.call_counter += 1
        return ..., None


@pytest.mark.parametrize('pool', [True, False], ids=['EvaluatorPool', 'Evaluator'])
@pytest.mark.asyncio
async def test_evaluator_upload_empty(pool):
    uri = ModelEvaluatorUriDescriptor(
        train_uri="x1",
        prediction_uri="p1",
        prediction_result_uri="r1"
    )
    uri_data = {
        "p1": [1, 2, 3, 4]
    }
    upload_pool = FakeUploadPool(workers_count=10)
    download_pool = FakeDownloadPool(workers_count=10)
    predictions_formatter = EmptyPredictionsFormatter()
    stream_reader = FakeStreamReader(reader_pool=download_pool, uri_data=uri_data)
    eval_worker_pool = MinibatchWorkerPool(
        num_workers=10,
        calcer_factory=lambda x: FakeCalcer(),
        calcer_results_handlers=[]
    )
    async with model_evaluator(
            upload_pool=upload_pool,
            download_pool=download_pool,
            predictions_formatter=predictions_formatter,
            stream_reader=stream_reader,
            eval_worker_pool=eval_worker_pool,
            uri_descriptors=[uri],
            use_model_evaluator_pool=pool,
    ) as evaluator:
        await evaluator("x1")
        await evaluator.wait_upload_futures()

    assert evaluator.upload_pool.recorder == [[..., 'r1', None]]
    assert predictions_formatter.call_counter == 1

######################################################
#                     EXCEPTIONS                     #
######################################################


class InfiniteUploadWorker(BaseAsyncWorker):
    async def _do_job(self, predictions, path, metadata):
        await asyncio.sleep(100000)


class InfiniteUploadPool(BaseAsyncWorkerPool):
    def __init__(self, workers_count):
        super(InfiniteUploadPool, self).__init__(workers_count, )

    def create_new_worker(self, new_rank: int):
        return InfiniteUploadWorker(new_rank)


class MyException(Exception):
    pass


class ExceptionPredictionsFormatter(FakePredictionsFormatter):
    def __init__(self):
        self.call_counter = 0
        super(ExceptionPredictionsFormatter, self).__init__()

    async def __call__(self, uri_descr, all_predictions):
        self.call_counter += 1
        if self.call_counter > 2:
            raise MyException
        return await super(ExceptionPredictionsFormatter, self).__call__(uri_descr=uri_descr, all_predictions=all_predictions)


@pytest.mark.parametrize('pool', [True, False], ids=['EvaluatorPool', 'Evaluator'])
@pytest.mark.asyncio
async def test_evaluator_format_exception(pool):
    uris = [
        ModelEvaluatorUriDescriptor(
            train_uri=f"x{i}",
            prediction_uri=f"p{i}",
            prediction_result_uri=f"r{i}"
        )
        for i in range(5)
    ]
    uri_data = {f"p{i}": [i] for i in range(5)}

    upload_pool = InfiniteUploadPool(workers_count=10)
    download_pool = FakeDownloadPool(workers_count=10)
    predictions_formatter = ExceptionPredictionsFormatter()
    stream_reader = FakeStreamReader(reader_pool=download_pool, uri_data=uri_data)
    eval_worker_pool = MinibatchWorkerPool(
        num_workers=10,
        calcer_factory=lambda x: FakeCalcer(),
        calcer_results_handlers=[]
    )

    # On exiting of the evaluator context, we must obtain the inner exception, not
    # pack of CancelledError
    with pytest.raises(MyException):
        async with model_evaluator(
                upload_pool=upload_pool,
                download_pool=download_pool,
                predictions_formatter=predictions_formatter,
                stream_reader=stream_reader,
                eval_worker_pool=eval_worker_pool,
                uri_descriptors=uris,
                use_model_evaluator_pool=pool,
        ) as evaluator:
            await evaluator("x0")
            await evaluator("x1")

            inner_evaluator = get_inner_evaluator(evaluator)
            futures = inner_evaluator._currently_running_futures

            for i in range(2, 5):
                with pytest.raises(MyException):
                    await evaluator(f"x{i}")

            await evaluator("x2")

    for f in futures:
        with pytest.raises((asyncio.CancelledError, MyException)):
            await f


@dataclasses.dataclass
class CallCounter:
    x: int = 0


class ExceptionUploadWorker(BaseAsyncWorker):
    def __init__(self, rank, recorder, counter: CallCounter):
        super(ExceptionUploadWorker, self).__init__(rank)
        self.recorder = recorder
        self.counter = counter

    async def _do_job(self, predictions, path, metadata):
        self.counter.x += 1
        if self.counter.x > 2:
            raise MyException

        self.recorder.append([predictions, path, metadata])
        await asyncio.sleep(100000)


class ExceptionUploadPool(BaseAsyncWorkerPool):
    def __init__(self, workers_count):
        super(ExceptionUploadPool, self).__init__(workers_count, )
        self.recorder = list()
        self.counter = CallCounter()

    def create_new_worker(self, new_rank: int):
        return ExceptionUploadWorker(new_rank, self.recorder, self.counter)


@pytest.mark.parametrize('pool', [True, False], ids=['EvaluatorPool', 'Evaluator'])
@pytest.mark.asyncio
async def test_evaluator_upload_exception(pool):
    uris = [
        ModelEvaluatorUriDescriptor(
            train_uri=f"x{i}",
            prediction_uri=f"p{i}",
            prediction_result_uri=f"r{i}"
        )
        for i in range(5)
    ]
    uri_data = {f"p{i}": [i] for i in range(5)}

    upload_pool = ExceptionUploadPool(workers_count=10)
    download_pool = FakeDownloadPool(workers_count=10)
    predictions_formatter = FakePredictionsFormatter()
    stream_reader = FakeStreamReader(reader_pool=download_pool, uri_data=uri_data)
    eval_worker_pool = MinibatchWorkerPool(
        num_workers=10,
        calcer_factory=lambda x: FakeCalcer(),
        calcer_results_handlers=[]
    )

    # top-level exception: check the whole system
    with pytest.raises(MyException):
        async with model_evaluator(
                upload_pool=upload_pool,
                download_pool=download_pool,
                predictions_formatter=predictions_formatter,
                stream_reader=stream_reader,
                eval_worker_pool=eval_worker_pool,
                uri_descriptors=uris,
                use_model_evaluator_pool=pool,
        ) as evaluator:
            await evaluator("x0")
            await evaluator("x1")
            await evaluator("x3")
            inner_evaluator = get_inner_evaluator(evaluator)
            futures = inner_evaluator._currently_running_futures.copy()

            await evaluator("x4")

    for f in futures:
        with pytest.raises((asyncio.CancelledError, MyException)):
            await f

    assert upload_pool.recorder == [
        [[10100000], 'r0', uris[0]],
        [[10100001], 'r1', uris[1]]
    ]


class ExceptionCalcer(AbstractCalcer):
    def __init__(self, counter):
        self.counter = counter

    async def __call__(self, inputs, targets, data_identity: int = 0) -> CalcerResults:
        if self.counter.x > 2:
            raise MyException("OPS")
        self.counter.x += 1

        return CalcerResults(
            metrics={},
            input_gradients=None,
            predictions=inputs + EVAL_SHIFT,
            losses={}
        )

    async def send_command(self, name: str, value: Any):
        pass

    def get_device(self) -> torch.device:
        return torch.device("cpu")


@pytest.mark.parametrize('pool', [True, False], ids=['EvaluatorPool', 'Evaluator'])
@pytest.mark.asyncio
async def test_evaluator_eval_pool_exception(pool):
    uris = [
        ModelEvaluatorUriDescriptor(
            train_uri=f"x{i}",
            prediction_uri=f"p{i}",
            prediction_result_uri=f"r{i}"
        )
        for i in range(5)
    ]
    uri_data = {f"p{i}": [i] for i in range(5)}

    upload_pool = InfiniteUploadPool(workers_count=10)
    download_pool = FakeDownloadPool(workers_count=10)
    predictions_formatter = FakePredictionsFormatter()
    stream_reader = FakeStreamReader(reader_pool=download_pool, uri_data=uri_data)
    counter = CallCounter()
    eval_worker_pool = MinibatchWorkerPool(
        calcer_factory=lambda x: ExceptionCalcer(counter=counter),
        num_workers=10,
        calcer_results_handlers=[]
    )

    # top-level exception: check the whole system
    with pytest.raises(MyException) as exc:
        async with model_evaluator(
                upload_pool=upload_pool,
                download_pool=download_pool,
                predictions_formatter=predictions_formatter,
                stream_reader=stream_reader,
                eval_worker_pool=eval_worker_pool,
                uri_descriptors=uris,
                use_model_evaluator_pool=pool,
        ) as evaluator:
            await evaluator("x0")
            await evaluator("x1")
            await evaluator("x3")
            await evaluator("x4")

    assert exc.value is eval_worker_pool._ex


class ExceptionDownloadWorker(BaseAsyncWorker):
    def __init__(self, rank, counter):
        super(ExceptionDownloadWorker, self).__init__(rank)
        self.counter = counter

    async def _do_job(self, chunk: int):
        self.counter.x += 1
        if self.counter.x < 3:
            return MinibatchRecord(
                inputs=torch.tensor(chunk + DOWNLOAD_SHIFT),
                targets=torch.tensor(0)
            )
        if self.counter.x == 3:
            raise MyException
        elif self.counter.x > 3:
            await asyncio.sleep(100500)


class ExceptionDownloadPool(BaseAsyncWorkerPool):
    def __init__(self, *args, **kwargs):
        super(ExceptionDownloadPool, self).__init__(*args, **kwargs)
        self.counter = CallCounter()

    def create_new_worker(self, new_rank: int):
        return ExceptionDownloadWorker(new_rank, self.counter)


@pytest.mark.parametrize('pool', [True, False], ids=['EvaluatorPool', 'Evaluator'])
@pytest.mark.asyncio
async def test_evaluator_download_pool_exception(pool):
    uris_count = 5
    uris = [
        ModelEvaluatorUriDescriptor(
            train_uri=f"x{i}",
            prediction_uri=f"p{i}",
            prediction_result_uri=f"r{i}"
        )
        for i in range(uris_count)
    ]
    uri_data = {f"p{i}": [i] for i in range(uris_count)}

    upload_pool = InfiniteUploadPool(workers_count=10)
    download_pool = ExceptionDownloadPool(workers_count=10, )
    predictions_formatter = FakePredictionsFormatter()
    stream_reader = FakeStreamReader(reader_pool=download_pool, uri_data=uri_data)
    eval_worker_pool = MinibatchWorkerPool(
        calcer_factory=lambda x: FakeCalcer(),
        num_workers=10,
        calcer_results_handlers=[]
    )

    # top-level exception: check the whole system
    with pytest.raises(MyException) as exc:
        async with model_evaluator(
                upload_pool=upload_pool,
                download_pool=download_pool,
                predictions_formatter=predictions_formatter,
                stream_reader=stream_reader,
                eval_worker_pool=eval_worker_pool,
                uri_descriptors=uris,
                use_model_evaluator_pool=pool,
        ) as evaluator:
            inner_evaluator = get_inner_evaluator(evaluator)
            await evaluator("x0")
            futures = inner_evaluator._currently_running_futures.copy()
            # wait for ahead of time download
            for i in range(1, uris_count):
                await evaluator(f"x{i}")
                futures = inner_evaluator._currently_running_futures.copy()

    # all futures from upload pool must die with CancelledError
    for f in futures:
        with pytest.raises(asyncio.CancelledError):
            await f

    assert exc.value is download_pool._ex
