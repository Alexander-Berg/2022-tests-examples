import pytest
import json
import jsonschema
import tempfile

import torch
from ads_pytorch.online_learning.production.trainer import (
    OnlineLearningModelFactory,
    ProductionOnlineLearner
)
from ads_pytorch.core import BaseParameterServerModule, ParameterServerOptimizer


class FakeModel(BaseParameterServerModule):
    def __init__(self):
        super(FakeModel, self).__init__()
        self.net = torch.nn.Linear(100, 1)

    def async_forward(self, inputs):
        return inputs

    def sync_forward(self, async_outputs):
        return self.net(async_outputs)


class FakeFactory(OnlineLearningModelFactory):
    def create_loss(self) -> torch.nn.Module:
        return torch.nn.MSELoss()

    def create_optimizer(self, model: BaseParameterServerModule) -> ParameterServerOptimizer:
        return ParameterServerOptimizer(
            torch.optim.Adam(model.deep_parameters())
        )

    def create_model(self) -> BaseParameterServerModule:
        return FakeModel()


def test_empty_config():
    with tempfile.NamedTemporaryFile() as tmp:
        with open(tmp.name, "wt"):
            pass

        with pytest.raises(Exception):
            ProductionOnlineLearner(
                token="regecnf",
                yt_proxy="rfgecnf",
                config_path=tmp.name,
                model_factory=FakeFactory(tmp.name)
            )


def make_good_config():
    return {
        "model_yt_dir": "//home/bs/model_dir",
        "system": {
            "max_scheduler_workers": 24,
            "gpu_scheduler_memory_threshold": 1024,
            "max_workers_per_gpu": 3,
            "allow_nonblock_sync": False,
            "wait_deep_process": 360,
            "enable_time_count": False,
        },
        "yt": {
            "retry": {
                "count": 5,
                "cooldown": 1
            },
            "transaction_timeout": 600,
            "ping_delay": 15,
            "proxy_get_frequency": 10
        },
        "train_ctl": {
            "yt": {
                "retry": {
                    "count": 5,
                    "cooldown": 1
                },
                "transaction_timeout": 600,
                "ping_delay": 15,
                "proxy_get_frequency": 10
            },
            "subprocess_wait_time": 600
        },
        "train_data": {
            "folder": "//home/bs/folder",
            "datetime_regex": "\\d{12}",
            "datetime_mask": "%Y%m%d%H%M",
            "num_downloaders": 6,
            "num_parsers": 2,
            "max_cache_size": 100500,
            "force_skip_limit": 5,
            "log_skip_limit": 5,
            "uri_timedelta": 3600,
            "features": ["feature"],
            "targets": ["target"]
        },
        "snapshotter": {
            "min_frequency": 0,
            "names": [],
            "num_uploaders": 5
        },
        "ttl_filter": {
            "min_frequency": 0,
            "num_threads": 10
        },
        "hardware": {
            "cpu_guarantee": 6000,
            "max_ram": 200000,
            "max_disk": 10000,
            "gpu_count": 0,
            "gpu_type": "NO_GPU",
            "gpu_max_ram": 0
        },
        "logger": {
            "frequency": 1
        },
        "load_model": {
            "num_downloaders": 10
        }
    }


def test_good_config():
    cfg = make_good_config()

    with tempfile.NamedTemporaryFile() as tmp:
        with open(tmp.name, "wt") as f:
            json.dump(cfg, f)

        ProductionOnlineLearner(
            token="regecnf",
            yt_proxy="rfgecnf",
            config_path=tmp.name,
            model_factory=FakeFactory(tmp.name)
        )


def test_bad_config():
    cfg = make_good_config()
    cfg.pop("model_yt_dir")

    with tempfile.NamedTemporaryFile() as tmp:
        with open(tmp.name, "wt") as f:
            json.dump(cfg, f)

        with pytest.raises(jsonschema.exceptions.ValidationError):
            ProductionOnlineLearner(
                token="regecnf",
                yt_proxy="rfgecnf",
                config_path=tmp.name,
                model_factory=FakeFactory(tmp.name)
            )
