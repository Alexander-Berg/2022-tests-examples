import asyncio
from typing import Any, List

import pytest
import torch
import dataclasses
import torch.nn

from ads_pytorch.core.runner import Runner, BaseRunnerCallback
from ads_pytorch.core import BaseParameterServerModule, ParameterServerOptimizer
from ads_pytorch.model_calcer.minibatch_worker import MinibatchWorkerPool
from ads_pytorch.model_calcer.minibatch_record import MinibatchRecord
from ads_pytorch.model_calcer.calcer import AbstractCalcer, CalcerResults
from ads_pytorch.core.data_loader import DataLoader
from ads_pytorch.tools.progress import ProgressLogger, StringProgressLogger
from ads_pytorch.tools.async_worker_pool import BaseAsyncWorker, BaseAsyncWorkerPool
from ads_pytorch.tools.stream_reader import BaseStreamReader
from ads_pytorch.tools.nested_structure import apply


@dataclasses.dataclass
class DummyCalcer(AbstractCalcer):
    model: BaseParameterServerModule
    loss: torch.nn.Module
    optimizer: ParameterServerOptimizer

    commands_called: List[Any] = dataclasses.field(default_factory=list, init=False)

    async def __call__(self, inputs, targets, data_identity: int = 0) -> CalcerResults:
        predictions = self.model(inputs)
        loss_val = self.loss(predictions, targets)
        apply(loss_val, fn=lambda x: x.backward() if x.requires_grad else None)
        input_gradients = apply(inputs, fn=lambda x: x.grad if x is not None and x.grad is not None else None)
        return CalcerResults(
            metrics={},
            input_gradients=input_gradients,
            predictions=predictions,
            losses={"Loss": torch.tensor([1.0])}
        )

    async def send_command(self, name: str, value: Any):
        self.commands_called.append((name, value))

    def get_device(self) -> torch.device:
        return torch.device("cpu")


class MyModel(BaseParameterServerModule):
    def __init__(self):
        super(MyModel, self).__init__()
        self.bias = torch.nn.Parameter(torch.zeros(1))

    def async_forward(self, inputs):
        return inputs

    def sync_forward(self, async_outputs):
        return self.bias


class FakeProgressLogger(StringProgressLogger):
    def __init__(self, *args, **kwargs):
        super(FakeProgressLogger, self).__init__(*args, **kwargs)
        self.log_called = False

    def _log(self, string: str):
        self.log_called = True
        print(string)


class FakeLoss(torch.nn.modules.loss._Loss):
    def forward(self, *input):
        return torch.zeros(1)


class FakeDownloadWorker(BaseAsyncWorker):
    async def _do_job(self, chunk: str):
        return MinibatchRecord(inputs=torch.FloatTensor([1]), targets=torch.FloatTensor([1]))


class FakeDownloadPool(BaseAsyncWorkerPool):
    def create_new_worker(self, new_rank: int):
        return FakeDownloadWorker(new_rank)


async def _iter_uris(lst):
    for x in lst:
        yield x


@pytest.fixture
def runner_kwargs():
    model = MyModel()
    optimizer = ParameterServerOptimizer(torch.optim.Adam(model.deep_parameters()))
    loss = FakeLoss()
    worker_pool = MinibatchWorkerPool(
        calcer_factory=lambda worker_id: DummyCalcer(model=model, loss=loss, optimizer=optimizer),
        calcer_results_handlers=[],
        num_workers=1
    )
    download_pool = FakeDownloadPool()
    stream_reader = BaseStreamReader(
        reader_pool=download_pool
    )
    data_loader = DataLoader(
        stream_reader=stream_reader,
        downloader_pool=download_pool,
        uri_iterable=_iter_uris(["uri1"])
    )
    sublogger = FakeProgressLogger()
    progress_logger = ProgressLogger([sublogger], frequency=1)

    return dict(
        model=model,
        optimizer=optimizer,
        loss=loss,
        worker_pool=worker_pool,
        data_loader=data_loader,
        logger=progress_logger
    ), sublogger


######################################################
#                     CALLBACKS                      #
######################################################


class ZeroCallback(BaseRunnerCallback):
    def __init__(self):
        super(ZeroCallback, self).__init__(call_after_train=False, call_before_train=False)
        self._call_counter = 0

    async def __call__(self, *args, **kwargs):
        self._call_counter += 1


class PreCallback(BaseRunnerCallback):
    def __init__(self, progress_logger):
        super(PreCallback, self).__init__(call_after_train=False, call_before_train=True)
        self._call_counter = 0
        self._progress_logger = progress_logger

    async def __call__(self, *args, **kwargs):
        assert not self._progress_logger.log_called
        self._call_counter += 1


class PostCallback(BaseRunnerCallback):
    def __init__(self, progress_logger):
        super(PostCallback, self).__init__(call_before_train=False, call_after_train=True)
        self._call_counter = 0
        self._progress_logger = progress_logger

    async def __call__(self, *args, **kwargs):
        assert self._progress_logger.log_called
        self._call_counter += 1


class BothCallback(BaseRunnerCallback):
    def __init__(self, progress_logger):
        super(BothCallback, self).__init__(call_before_train=True, call_after_train=True)
        self._call_counter = 0
        self._progress_logger = progress_logger

    async def __call__(self, *args, **kwargs):
        if self._call_counter == 1:
            assert self._progress_logger.log_called
        else:
            assert not self._progress_logger.log_called
        self._call_counter += 1


class ToCatchTestException(Exception):
    pass


class ExceptionAenterCallback(BaseRunnerCallback):
    async def __aenter__(self):
        raise ToCatchTestException


class ExceptionAexitCallback(BaseRunnerCallback):
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        raise ToCatchTestException


class ExceptionCallCallback(BaseRunnerCallback):
    async def __call__(self, *args, **kwargs):
        raise ToCatchTestException


@pytest.mark.asyncio
@pytest.mark.parametrize(
    'callback',
    [ExceptionAenterCallback(), ExceptionAexitCallback(), ExceptionCallCallback()],
    ids=['__aenter__', '__aexit__', '__call__']
)
async def test_runner_callback_fail_exception_raise(runner_kwargs, callback):
    kwargs, _ = runner_kwargs
    runner = Runner(
        uri_callbacks=[callback],
        start_callback=None,
        finish_callback=None,
        **kwargs
    )
    with pytest.raises(ToCatchTestException):
        await runner.run()


@pytest.mark.asyncio
async def test_runner_callback_call(runner_kwargs):
    kwargs, progress_logger = runner_kwargs
    callbacks = [
        ZeroCallback(),
        PreCallback(progress_logger),
        PostCallback(progress_logger),
        BothCallback(progress_logger)
    ]
    runner = Runner(
        uri_callbacks=callbacks,
        start_callback=None,
        finish_callback=None,
        **kwargs
    )

    await runner.run()
    assert callbacks[0]._call_counter == 0
    assert callbacks[1]._call_counter == 1
    assert callbacks[2]._call_counter == 1
    assert callbacks[3]._call_counter == 2


@pytest.mark.asyncio
async def test_runner_no_progress_loggers(runner_kwargs):
    kwargs, progress_logger = runner_kwargs
    kwargs["logger"] = ProgressLogger([])

    runner = Runner(
        start_callback=None,
        finish_callback=None,
        **kwargs
    )

    await runner.run()


@pytest.mark.asyncio
async def test_runner_pool_commands(runner_kwargs):
    kwargs, progress_logger = runner_kwargs
    kwargs["logger"] = ProgressLogger([])

    runner = Runner(
        start_callback=None,
        finish_callback=None,
        **kwargs
    )

    await runner.run()

    pool: MinibatchWorkerPool = kwargs["worker_pool"]
    assert len(pool.all_workers) == 1
    assert pool.all_workers[0]._calcer.commands_called == [
        ("set_get_predictions", False),
        ("set_train_mode", True)
    ]
