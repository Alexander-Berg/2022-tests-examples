import json

import pytest
import yatest.common


def make_config_path(env: str) -> str:
    return yatest.common.source_path(f"maps/bizdir/sps/config/sps.{env}.json")


@pytest.mark.parametrize(
    "env", ["default", "development", "testing", "stable"]
)
def test_configs(env: str) -> None:
    with open(make_config_path(env), "r") as f:
        json.load(f)
