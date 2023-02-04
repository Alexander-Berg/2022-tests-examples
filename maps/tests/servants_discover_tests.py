import pytest
from pathlib import Path

from maps.infra.baseimage.template_generator.lib.servants import (
    app_generators, YACARE_SERVANTS_DIR, PYCARE_SERVANTS_DIR
)
from maps.infra.pycare.scripts.make_configs.lib import PycareAppSpec, PycareConfigsGenerator
from maps.infra.yacare.scripts.make_configs.lib import YcrAppSpec, YcrConfigsGenerator


@pytest.fixture(autouse=True)
def mock_ycr_generator_from_binary(monkeypatch) -> None:
    monkeypatch.setattr(
        YcrConfigsGenerator,
        'from_app_binary',
        lambda path: YcrConfigsGenerator(
            app_name=path.name,
            app_spec=YcrAppSpec(alarms=[], services=[], builtins=[])
        )
    )


@pytest.fixture(autouse=True)
def mock_pycare_generator_from_binary(monkeypatch) -> None:
    monkeypatch.setattr(
        PycareConfigsGenerator,
        'from_app_binary',
        lambda path: PycareConfigsGenerator(
            app=path.name, app_spec=PycareAppSpec(services=[])
        )
    )


def test_yacare_apps(fs):
    # Two yacare apps available
    for mock_yacare_app in ['one', 'two']:
        fs.create_file(Path(YACARE_SERVANTS_DIR) / mock_yacare_app)
    # Check both apps discovered
    assert ['one', 'two'] == [generator.app for generator in app_generators(Path('.'))]


def test_pycare_apps(fs):
    # Two pycare apps available
    for mock_pycare_app in ['three', 'four']:
        fs.create_file(Path(PYCARE_SERVANTS_DIR) / mock_pycare_app)
    # Check both apps discovered
    assert ['three', 'four'] == [generator.app for generator in app_generators(Path('.'))]


def test_apps_invalid_path(fs):
    # Non existing path yields neither apps no errors
    assert 0 == len(list(app_generators(Path('doesnt-exist'))))

    # Error if file instead of directory
    fs.create_file(Path(YACARE_SERVANTS_DIR))
    with pytest.raises(NotADirectoryError):
        list(app_generators(Path('.')))
