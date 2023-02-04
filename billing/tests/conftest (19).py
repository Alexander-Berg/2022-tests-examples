import hashlib
import logging
import string
from typing import Dict, Tuple

import aiohttp.pytest_plugin
import pytest

from sendr_qlog import LoggerContext

from billing.yandex_pay.tools.fim.lib.checker import CheckResult
from billing.yandex_pay.tools.fim.lib.stats import fim_check

pytest_plugins = ['aiohttp.pytest_plugin']
CYRRILIC = 'абвгдеёжзийклмнопрстуфхцчшщъыьэюя'
del aiohttp.pytest_plugin.loop


@pytest.fixture(autouse=True)
def reset_stats():
    yield

    for result in CheckResult:
        fim_check.remove(result.value)


@pytest.fixture
def loop(event_loop):
    return event_loop


@pytest.fixture
def dummy_files_dict() -> Dict[str, Tuple[bytes, str]]:
    result = {}

    printable = (string.printable + CYRRILIC).encode()

    for i in range(16):
        content = bytes(range(i)) + printable + bytes(range(i, 16))
        checksum = hashlib.sha256(content, usedforsecurity=False).hexdigest()

        if i % 2 == 0:
            keys = (CYRRILIC[i].upper(), string.ascii_uppercase[i])
        else:
            keys = (CYRRILIC[i], string.ascii_lowercase[i])
        result.update({k: (content, checksum) for k in keys})

    return result


@pytest.fixture
def dummy_dir(dummy_files_dict, tmp_path):
    d = tmp_path / "sub"
    d.mkdir()
    for name, content in dummy_files_dict.items():
        (d / name).write_bytes(content[0])

    return d


@pytest.fixture
def dummy_logger(caplog) -> LoggerContext:
    logger = LoggerContext(logging.getLogger('dummy_logger'))
    caplog.set_level(logging.INFO, logger=logger.logger.name)
    return logger
