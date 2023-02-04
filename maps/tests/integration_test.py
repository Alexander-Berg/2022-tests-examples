import os
import yaml
import yatest
from unittest import mock

from maps.infra.baseimage.template_generator.lib import template_generator as tg


@mock.patch.dict(os.environ, {'NANNY_SERVICE_ID': 'maps_core_teapot_stable', 'CPU_GUARANTEE': '2'}, clear=True)
def test_load_config():
    return yaml.dump(tg.load_config(
        tg.enumerate_config_files(
            [],
            yatest.common.source_path('maps/infra/baseimage/template_generator/tests/test-data')),
        default_settings=tg.load_environment()
    ).dict())
