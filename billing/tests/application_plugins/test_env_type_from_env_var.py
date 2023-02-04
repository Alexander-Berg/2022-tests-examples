import logging

from butils.application.plugins import env_type_from_env_var as plugin


def test_find_env_type(monkeypatch):
    env_vars = ("A", "B", "C")

    env_var, env_type = plugin.find_env_type(env_vars)
    assert env_var is None and env_type == "dev"

    monkeypatch.setenv("C", "prod")
    env_var, env_type = plugin.find_env_type(env_vars)
    assert env_var == "C" and env_type == "prod"

    monkeypatch.setenv("B", "dev")
    env_var, env_type = plugin.find_env_type(env_vars)
    assert env_var == "B" and env_type == "dev"

    monkeypatch.setenv("A", "test")
    env_var, env_type = plugin.find_env_type(env_vars)
    assert env_var == "A" and env_type == "test"


def test_normalize_env_type():
    assert plugin.normalize_env_type("development") == "dev"
    assert plugin.normalize_env_type("testing") == "test"
    assert plugin.normalize_env_type("production") == "prod"
    assert plugin.normalize_env_type("exotic") == "exotic"


def get_Application():
    class ApplicationMock:
        def get_logger(self, name):
            return logging.getLogger(name)

    return ApplicationMock


def test_create(monkeypatch):
    AppCls = get_Application()
    app_obj = AppCls()
    plugin.create(app_obj, None)
    assert app_obj.get_current_env_type() == "dev"

    monkeypatch.setenv("QLOUD_ENVIRONMENT", "production")
    AppCls = get_Application()
    app_obj = AppCls()
    plugin.create(app_obj, None)
    assert app_obj.get_current_env_type() == "prod"

    monkeypatch.setenv("YB_ENV_TYPE", "test")
    AppCls = get_Application()
    app_obj = AppCls()
    plugin.create(app_obj, None)
    assert app_obj.get_current_env_type() == "test"

    import xml.etree.ElementTree as ET

    plugin_cfg = ET.fromstring("<Plugin><EnvVar>CUSTOM_ENV_TYPE</EnvVar></Plugin>")
    monkeypatch.setenv("CUSTOM_ENV_TYPE", "custom")
    AppCls = get_Application()
    app_obj = AppCls()
    plugin.create(app_obj, plugin_cfg)
    assert app_obj.get_current_env_type() == "custom"
