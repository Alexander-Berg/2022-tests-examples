import pytest
import os
from ads_pytorch.core import BaseParameterServerModule

import torch
from typing import Dict, Optional, Tuple, Any
from ads_pytorch.tools.multiprocessing import get_multiprocessing_context
from ads_pytorch.model_calcer.factory import MinibatchPoolFactory, MinibatchWorkerPool
from ads_pytorch.model_calcer.minibatch_record import MinibatchRecord


def get_calcer_and_params(
    model: torch.nn.Module,
    loss: torch.nn.Module,
    optimizer_factory: Optional[Any] = lambda params: [torch.optim.Adam(params)]
) -> Tuple[MinibatchWorkerPool, Dict[str, torch.nn.Parameter]]:
    model.parameter_server_mode = True

    factory = MinibatchPoolFactory(
        model=model,
        loss=loss,
        deep_optimizers=optimizer_factory(model.parameters()),
        hash_embedding_optimizers=[],
        calcer_results_handlers=[],
        mp_ctx=get_multiprocessing_context(),
        get_predictions=False,
        train_mode=True,
        num_workers=1,
        allow_async_gpu_update=False,
    )
    minibatch_pool = factory()

    return minibatch_pool, factory.deep_updated_parameters


class ToCatch(Exception):
    pass


class ExceptionModule(torch.nn.Module):
    def __init__(self):
        super(ExceptionModule, self).__init__()
        self.net = torch.nn.Linear(1, 1)

    def forward(self, *input: Any, **kwargs: Any):
        raise ToCatch


class ExceptionPSModule(BaseParameterServerModule):
    def __init__(self):
        super(ExceptionPSModule, self).__init__()
        self.net = torch.nn.Linear(1, 1)

    def async_forward(self, inputs):
        raise NotImplementedError("This should not be called")

    def sync_forward(self, async_outputs):
        raise ToCatch


class ExceptionLoss(torch.nn.Module):
    def __init__(self):
        super(ExceptionLoss, self).__init__()

    def forward(self, *input: Any, **kwargs: Any):
        raise ToCatch


class ExceptionDeepOptimizer(torch.optim.Optimizer):
    def __init__(self, params):
        super(ExceptionDeepOptimizer, self).__init__(params, dict())
        # we want to test only multiprocessing errors
        # Moreover, we expect this optimizer to be called only once in main process
        # when initializing buffers
        self._pid = os.getpid()
        self._called = False

    def step(self, closure=...):
        if hasattr(self, "_pid") and os.getpid() == self._pid:
            self._called = True
        raise ToCatch


@pytest.mark.parametrize(
    "model,loss",
    [
        (ExceptionModule(), torch.nn.MSELoss()),
        (ExceptionPSModule(), torch.nn.MSELoss()),
        (torch.nn.Linear(1, 1), ExceptionLoss()),
    ],
    ids=['Module', 'PSModule', 'Loss']
)
@pytest.mark.asyncio
async def test_module_exceptions(model, loss):
    calcer, updated_params = get_calcer_and_params(
        model=model,
        loss=loss
    )

    with pytest.raises(Exception):
        async with calcer:
            await calcer.assign_job(MinibatchRecord(inputs=torch.FloatTensor([1]), targets=torch.FloatTensor([1])))


@pytest.mark.asyncio
async def test_optimizer_exceptions():
    model = torch.nn.Linear(1, 1)
    loss = torch.nn.MSELoss()

    inputs = torch.FloatTensor([1])
    targets = torch.FloatTensor([1])

    calcer, updated_params = get_calcer_and_params(
        model=model,
        loss=loss,
        optimizer_factory=lambda params: [ExceptionDeepOptimizer(params)]
    )

    with pytest.raises(Exception):
        async with calcer:
            await calcer.assign_job(MinibatchRecord(inputs=inputs, targets=targets))
