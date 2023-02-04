from infra.walle.server.tests.lib.util import monkeypatch_config

import pytest

MOCKED_RACK_MODEL = "rack-01"
MOCK_SYSTEM = "system"


@pytest.fixture
def mp_rack_map(mp):
    monkeypatch_config(
        mp,
        "rack_map.{}".format(MOCKED_RACK_MODEL),
        {"systems": [MOCK_SYSTEM], "slot_ranges": [{"min": 1, "max": 21}, {"min": 28, "max": 48}]},
    )
