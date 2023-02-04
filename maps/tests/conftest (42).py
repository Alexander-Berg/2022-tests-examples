from unittest.mock import Mock

import pytest

from maps_adv.geosmb.logoped.server.lib import Application

pytest_plugins = [
    "aiohttp.pytest_plugin",
    "maps_adv.common.lasagna.pytest.plugin",
]


@pytest.fixture
def app():
    return Application({})


@pytest.fixture(autouse=True)
def onotole_mock(mocker):
    return mocker.patch(
        "maps_adv.geosmb.logoped.server.lib.domain.analyze_word",
        return_value=[
            Mock(Lemma="онотол", Weight=1.0),
            Mock(Lemma="онотоле", Weight=1.0),
            Mock(Lemma="онотоле", Weight=0.0),
            Mock(Lemma="онотола", Weight=0.0),
        ],
    )
