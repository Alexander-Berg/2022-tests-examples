import pytest
import torch
from torch.autograd import Variable
import sys
import os
import time
from random import randint
import numpy as np
from copy import deepcopy

from ads_pytorch.cpp_lib import libcpp_lib
from ads_pytorch.model_calcer.concat_wrapper import wrap_model_with_concat_wrapper
from ads_pytorch.nn.module.universal_normalizer.universal_normalizer import (
    QuantizedNormalizerParameterServerOptimizer,
    _QuantizedNormalizer,
    QuantizedNormalizerParameter,
    validate_ps_universal_normalizer
)
from ads_pytorch.core import BaseParameterServerModule, ParameterServerOptimizer
from ads_pytorch.model_calcer.factory import MinibatchPoolFactory
from ads_pytorch.model_calcer.minibatch_record import MinibatchRecord

DEVICES = ["cuda:0", "cpu"]

# check if the algorithm is trained correctly both on cpu and cuda
class MyModel(BaseParameterServerModule):
    def __init__(self, safe_nan=True):
        super(MyModel, self).__init__()
        self.a = randint(100, 500)
        self.b = randint(100, 1024)
        self.c = randint(10, 50)
        self.dims_to_reduce = [0]
        self.shape = [self.a]
        self.bins_number = 128
        self.compression = 1000.0
        self.occurence_threshold = 0
        self.scaler = _QuantizedNormalizer(self.dims_to_reduce, self.shape, self.bins_number, self.compression,
                occurence_threshold=self.occurence_threshold, interpolate=False, use_cdf=False, use_double_binarization=False, safe_nan=safe_nan)

    def async_forward(self, inputs):
        return inputs

    def sync_forward(self, async_outputs):
        res = (self.scaler(async_outputs)).sum()
        return res


@pytest.mark.parametrize("use_inf", [True, False])
@pytest.mark.parametrize("use_nan", [True, False])
@pytest.mark.parametrize("device", DEVICES)
@pytest.mark.asyncio
async def test_run_with_model_calcer(device, use_nan, use_inf, safe_nan=False):
    model = MyModel(safe_nan=safe_nan)

    model.parameter_server_mode = True

    optimizer = QuantizedNormalizerParameterServerOptimizer(model.parameters())

    worker_pool = MinibatchPoolFactory(
        model=model,
        loss=torch.nn.MSELoss(),
        deep_optimizers=[optimizer],
        hash_embedding_optimizers=[],
        calcer_results_handlers=[],
        train_mode=True,
        get_predictions=True,
        devices=frozenset({torch.device(device)}),
        # enable_time_count=False,
        # max_workers_per_gpu=1,
        # max_scheduler_workers=1
    )()

    data_list = [(torch.rand((model.b, model.a))) for _ in range(model.c)]

    data_list_changed = deepcopy(data_list)

    if use_nan:
        data_list_changed[0][0] = np.nan
        data_list[0][0] = -((1 << 63) - 1)
    if use_inf:
        data_list_changed[1][0] = np.inf
        data_list_changed[2][0] = -np.inf

        data_list[1][0] = (1 << 63) - 1
        data_list[2][0] = -((1 << 63) - 1)

    targets_list = [torch.rand(model.b) for _ in range(model.c)]

    async with worker_pool:
        for data, targets in zip(data_list_changed, targets_list):
            record = MinibatchRecord(inputs=data, targets=targets)
            await worker_pool.assign_job(record)

    concatted = torch.cat(data_list, dim=0).T

    q = torch.tensor([i / model.bins_number
                      for i in range(1, model.bins_number + 1)])

    quantile_lst_torch = torch.quantile(concatted, q, dim=1).T
    un = libcpp_lib.UniversalNormalizer(model.scaler.dims_to_reduce,
                                        model.scaler.not_reduce_shape_lst,
                                        model.scaler.compression,
                                        model.scaler.bins_number,
                                        model.scaler.TDigestArray)

    quantile_lst_class = un.get_quantiles().cpu()

    error = q * (1 - q)
    # for users to see if the quantiles are not the same with their own eyes
    print('quantile_lst_torch = ', quantile_lst_torch)
    print('quantile_lst_class = ', quantile_lst_class)

    assert torch.all(
        torch.abs(quantile_lst_torch - quantile_lst_class) - error <= torch.zeros_like(quantile_lst_class)) == True


@pytest.mark.parametrize('use_wrapper', [True, False], ids=['ConcatWrapper', 'Usual'])
def test_validator_ok(use_wrapper):
    model = MyModel()
    if use_wrapper:
        model = wrap_model_with_concat_wrapper(model, fixed_sorted_parameters_order=True)
    model.parameter_server_mode = True
    params = list(model.buffer_parameters()) if use_wrapper else list(model.parameters())
    optimizer = QuantizedNormalizerParameterServerOptimizer(params)
    validate_ps_universal_normalizer(
        model=model,
        optimizer=ParameterServerOptimizer(optimizer),
        loss=torch.nn.MSELoss()
    )


def test_validator_none_optimized():
    model = MyModel()
    with pytest.raises(ValueError):
        validate_ps_universal_normalizer(
            model=model,
            optimizer=ParameterServerOptimizer(),
            loss=torch.nn.MSELoss()
        )


def test_validator_not_all_optimized():
    model = MyModel()
    optimizer = QuantizedNormalizerParameterServerOptimizer(model.scaler.parameters())

    validate_ps_universal_normalizer(
        model=model,
        optimizer=ParameterServerOptimizer(optimizer),
        loss=torch.nn.MSELoss()
    )

    assert True


@pytest.mark.parametrize('use_wrapper', [True, False], ids=['ConcatWrapper', 'Usual'])
def test_validator_wrong_optimizer(use_wrapper):
    model = MyModel()
    if use_wrapper:
        model = wrap_model_with_concat_wrapper(model, fixed_sorted_parameters_order=True)
    model.parameter_server_mode = True
    params = list(model.buffer_parameters()) if use_wrapper else list(model.parameters())
    optimizer = torch.optim.Adam(params)
    with pytest.raises(ValueError):
        validate_ps_universal_normalizer(
            model=model,
            optimizer=ParameterServerOptimizer(optimizer),
            loss=torch.nn.MSELoss()
        )

