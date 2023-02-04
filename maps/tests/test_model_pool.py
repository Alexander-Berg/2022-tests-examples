import json
import os
import tempfile

from maps.analyzer.pylibs.time_estimator.model_pool import PyModelPoolConfig, model_pool_config_name, model_pool_config_path


CFG = {
    "defaults": {"taxi": "taxi", "users": "users", "fixprice": "taxi", "future": "future"},
    "models": {
        "users": {"model": "model_users_name", "config": "config_users_name", "version": "version_users"},
        "taxi": {"model": "model_taxi_name", "config": "config_taxi_name", "version": "version_taxi"},
        "future": {"model": "model_future_name", "config": "config_future_name", "version": "version_future"}
    }
}


def test_add():
    py_config = PyModelPoolConfig()
    for k, v in CFG["defaults"].items():
        py_config.add_default_model(k, v)
    assert len(py_config.default_models().keys()) == len(CFG["defaults"].keys())
    py_config.add_model_description("users", {"modelName": "model_users_name", "configName": "config_users_name", "version": "version_users"})
    assert sorted(py_config.model_descriptions().keys()) == sorted(["users"])
    assert sorted(py_config.model_descriptions()["users"].values()) == sorted(["model_users_name", "config_users_name", "version_users"])


def test_from_string():
    py_config = PyModelPoolConfig.from_string(json.dumps(CFG))
    assert len(py_config.default_models().keys()) == len(CFG["defaults"].keys())
    assert len(py_config.model_descriptions().keys()) == len(CFG["models"].keys())


def test_config_name():
    assert os.path.dirname(model_pool_config_path("/root_path")) == "/root_path"
    assert model_pool_config_name()


def test_dump_load():
    py_config_init = PyModelPoolConfig.from_string(json.dumps(CFG))
    with tempfile.NamedTemporaryFile() as config_path:
        py_config_init.dump_to_file(config_path.name)
        py_config = PyModelPoolConfig.from_file(config_path.name)
        assert len(py_config.default_models().keys()) == len(CFG["defaults"].keys())
        assert len(py_config.model_descriptions().keys()) == len(CFG["models"].keys())
