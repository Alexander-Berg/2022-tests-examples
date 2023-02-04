import os
import pytest
import yatest.common

from collections import namedtuple


Models = namedtuple('Models', 'path max_size')

spotter_models = [
    Models('smart_devices/platforms/yandexmini/data/spotter_models', 8_000_000),
    Models('smart_devices/platforms/yandexstation/data/spotter_models', 8_000_000),
]


@pytest.mark.parametrize('models', spotter_models)
def test_spotter_size(models):
    total_size = 0
    models_path = yatest.common.source_path(models.path)
    for dirpath, dirnames, filenames in os.walk(models_path):
        for f in filenames:
            fp = os.path.join(dirpath, f)
            # skip if it is symbolic link
            if not os.path.islink(fp):
                print(fp, os.path.getsize(fp))
                total_size += os.path.getsize(fp)

    assert models.max_size > total_size
