import pytest

from maps.analyzer.pylibs.time_estimator.model import PyModelConfig

from .common import get_test_data


def test_from_file():
    config = PyModelConfig.from_file(get_test_data('model_config.meta'))
    assert config.total_features() == 841


def test_from_string():
    with open(get_test_data('model_config.meta')) as f:
        config_str = f.read()

    config = PyModelConfig.from_string(config_str)
    assert config.total_features() == 841


def test_uninitialized():
    with pytest.raises(Exception):
        cfg = PyModelConfig()
        cfg.total_features()
