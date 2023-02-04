import yatest.common

from maps.carparks.renderer.config.ecstatic_hooks.lib import config


def test_config_readability():
    config.read_config(yatest.common.source_path(
        'maps/carparks/renderer/config/lots/config.json'))
