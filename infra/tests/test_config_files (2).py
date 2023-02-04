import glob
import pytest
import typing
import yatest

from os.path import join, relpath

from infra.rtc_sla_tentacles.backend.lib.config.interface import ConfigInterface
from infra.rtc_sla_tentacles.backend.lib.config.raw_config_storage import RawConfigStorage
from infra.rtc_sla_tentacles.backend.lib.config.secrets_config import SecretsConfig
from infra.rtc_sla_tentacles.backend.lib.tests.config_example1 import cli_args_example1
from infra.rtc_sla_tentacles.backend.lib.util import read_yaml_file


def _get_config_files_paths() -> typing.List[str]:
    # Returns path to configuration files used in this test.
    # Also see DATA section in `ya.make` file.
    data_dir = yatest.common.test_source_path(join("..", "..", "..", "conf"))
    glob_pattern = join(data_dir, "*.yaml")
    root = yatest.common.source_path()
    yaml_files = [
        relpath(path, root)
        for path in glob.iglob(glob_pattern)
    ]
    return yaml_files


@pytest.fixture(params=_get_config_files_paths())
def raw_config_storage_on_actual_config_files(monkeypatch, patched_base_config, request) -> RawConfigStorage:
    """
        Creates and returns an instance of 'RawConfigStorage'.
        Configs are taken from actual config files, passed in `params`
        list of this fixture.
    """
    # Always return some value for secret names.
    def mock_get_secret(_, secret_name):
        return secret_name + "_VALUE"

    monkeypatch.setattr(SecretsConfig, "get_secret", mock_get_secret)

    file_cfg = read_yaml_file(yatest.common.source_path(request.param))

    # Change directory name of all handlers' filenames in logger
    # config to `/tmp`, so that logging.config.dictConfig will
    # be able to configure itself regardless of actual path to
    # log file.
    for _h in file_cfg["logging"]["handlers"].values():
        if "filename" in _h:
            _h["filename"] = "/tmp/some_log_file_name.log"

    yield RawConfigStorage(cli_args=cli_args_example1, file_cfg=file_cfg)


def test_config_file(raw_config_storage_on_actual_config_files: RawConfigStorage) -> None:
    """
        Creates and returns an instance of 'ConfigInterface'
        based on valid config from 'raw_config_storage' and
        'secrets_config' fixtures.
    """
    ConfigInterface(raw_config_storage=raw_config_storage_on_actual_config_files)
