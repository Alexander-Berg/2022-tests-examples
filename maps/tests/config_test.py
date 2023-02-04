from pathlib import Path
from maps.pylibs.fixtures.matchers import Match
from maps.infra.quotateka.config_uploader.configs import read_providers_configs, ProviderConfig


def test_read_all_configs(colliding_tvm_configs_root):
    assert read_providers_configs(Path(colliding_tvm_configs_root), staging='stable') == Match.Contains(
        ProviderConfig(provider='maps-router', path=Match.Str(), content=Match.Dict()),
        ProviderConfig(provider='maps-teapot', path=Match.Str(), content=Match.Dict()),
    )


def test_read_all_configs_per_staging(colliding_tvm_configs_root):
    assert read_providers_configs(Path(colliding_tvm_configs_root), staging='stable') == Match.Contains(
        ProviderConfig(provider='maps-teapot',
                       path=Match.Str(),
                       content=Match.HasItems({'tvm_ids': [2010296, 2010798]}))
    )
    assert read_providers_configs(Path(colliding_tvm_configs_root), staging='testing') == Match.Contains(
        ProviderConfig(provider='maps-teapot',
                       path=Match.Str(),
                       content=Match.HasItems({'tvm_ids': [2010798]})),
    )
