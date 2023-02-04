import os
from unittest.mock import patch

import pytest

from maps_adv.config_loader import Config, ConfigValueNotSet


@pytest.fixture(autouse=True)
def set_env():
    def _set_env(key, value):
        os.environ[key] = value

    with patch.dict(os.environ, os.environ.copy()):
        yield _set_env


class TempDotenv:
    def __init__(self, tmpdir: str):
        self.path = tmpdir.join(".env")

    def write(self, content: str):
        with open(self.path, "w") as f:
            f.write(content)

    def clear(self):
        os.remove(self.path)


@pytest.fixture
def dotenv(tmpdir):
    dotenv = TempDotenv(tmpdir)
    yield dotenv
    dotenv.clear()


@pytest.fixture
def config():
    return Config(
        {
            "WITHOUT_DEFAULT": {},
            "WITH_DEFAULT": {"default": "default_value"},
            "WITH_CONVERSION": {"default": "100500", "converter": int},
        }
    )


def test_raises_for_option_without_default_value(config):
    with pytest.raises(ConfigValueNotSet, match="WITHOUT_DEFAULT"):
        config.init()


def test_will_load_option_from_env(set_env, config):
    set_env("WITHOUT_DEFAULT", "valuetaschemta")

    config.init()

    assert config.WITHOUT_DEFAULT == "valuetaschemta"


def test_default_value_provided_if_does_not_set(set_env, config):
    set_env("WITHOUT_DEFAULT", "valuetaschemta")

    config.init()

    assert config.WITH_DEFAULT == "default_value"


def test_env_value_will_override_default(set_env, config):
    set_env("WITHOUT_DEFAULT", "valuetaschemta")
    set_env("WITH_DEFAULT", "lolkekmakarek")

    config.init()

    assert config.WITH_DEFAULT == "lolkekmakarek"


def test_option_with_converter_will_be_converted(set_env, config):
    set_env("WITHOUT_DEFAULT", "valuetaschemta")
    set_env("WITH_CONVERSION", "300500")

    config.init()

    assert config.WITH_CONVERSION == 300500


def test_default_value_does_not_converted(set_env, config):
    set_env("WITHOUT_DEFAULT", "valuetaschemta")

    config.init()

    assert config.WITH_CONVERSION == "100500"


def test_option_also_available_as_item(set_env, config):
    set_env("WITHOUT_DEFAULT", "valuetaschemta")

    config.init()

    assert config["WITHOUT_DEFAULT"] == "valuetaschemta"


def test_get_returns_option_value_if_exists(set_env, config):
    set_env("WITHOUT_DEFAULT", "valuetaschemta")

    config.init()

    assert config.get("WITHOUT_DEFAULT") == "valuetaschemta"


def test_get_returns_none_if_value_not_exists(set_env, config):
    set_env("WITHOUT_DEFAULT", "valuetaschemta")

    config.init()

    assert config.get("UNEXISTENCE") is None


@pytest.mark.parametrize(
    "content, expected",
    (
        (
            "WITHOUT_DEFAULT=123kek\nWITH_DEFAULT=makarek",
            {"WITHOUT_DEFAULT": "123kek", "WITH_DEFAULT": "makarek"},
        ),
        (
            "WITHOUT_DEFAULT==aza=\nWITH_CONVERSION=123",
            {"WITHOUT_DEFAULT": "=aza=", "WITH_CONVERSION": 123},
        ),
        (
            "WITHOUT_DEFAULT==aza=\nWITH_DEFAULT=azaza\nWITH_CONVERSION=123",
            {
                "WITHOUT_DEFAULT": "=aza=",
                "WITH_DEFAULT": "azaza",
                "WITH_CONVERSION": 123,
            },
        ),
    ),
)
def test_will_load_values_from_dotenv(content, expected, dotenv, config):
    dotenv.write(content)

    config.init(dotenv.path)

    for option, value in expected.items():
        assert getattr(config, option) == value


@pytest.mark.parametrize(
    "content, expected",
    (
        (
            "WITHOUT_DEFAULT=123kek\nWITH_DEFAULT=makarek",
            {"WITHOUT_DEFAULT": "123kek", "WITH_DEFAULT": "makarek"},
        ),
        (
            "WITHOUT_DEFAULT==aza=\nWITH_CONVERSION=123",
            {"WITHOUT_DEFAULT": "=aza=", "WITH_CONVERSION": 123},
        ),
        (
            "WITHOUT_DEFAULT==aza=\nWITH_DEFAULT=azaza\nWITH_CONVERSION=123",
            {
                "WITHOUT_DEFAULT": "=aza=",
                "WITH_DEFAULT": "azaza",
                "WITH_CONVERSION": 123,
            },
        ),
    ),
)
def test_will_load_values_from_dotenv_in_cwd_by_default(
    content, expected, dotenv, config
):
    dotenv.path = os.path.join(os.getcwd(), ".env")
    dotenv.write(content)

    config.init()

    for option, value in expected.items():
        assert getattr(config, option) == value


@pytest.mark.parametrize(
    "content, expected",
    (
        (
            "WITHOUT_DEFAULT=123kek\nWITH_KEK=makarek",
            {"WITHOUT_DEFAULT": "123kek", "WITH_KEK": "makarek"},
        ),
        (
            "WITHOUT_DEFAULT==aza=\nKEKMAKAREKT=123",
            {"WITHOUT_DEFAULT": "=aza=", "KEKMAKAREKT": "123"},
        ),
        (
            "WITHOUT_DEFAULT==aza=\nSTRING=azaza\nINT=123",
            {"WITHOUT_DEFAULT": "=aza=", "STRING": "azaza", "INT": "123"},
        ),
    ),
)
def test_will_set_all_values_from_dotenv_to_env_var(
    content, expected, dotenv, config
):
    dotenv.path = os.path.join(os.getcwd(), ".env")
    dotenv.write(content)

    config.init(dotenv.path)

    for option, value in expected.items():
        assert os.getenv(option) == value


def test_env_var_has_higher_priority_than_dotenv(dotenv, set_env, config):
    dotenv.write("WITHOUT_DEFAULT=pauknaprimer")
    set_env("WITHOUT_DEFAULT", "valuetaschemta")

    config.init(dotenv.path)

    assert config.WITHOUT_DEFAULT == "valuetaschemta"


def test_commented_option_will_not_loaded(dotenv, config):
    dotenv.write("WITHOUT_DEFAULT=pauknaprimer\n#WITH_DEFAULT=unexpected")

    config.init(dotenv.path)

    assert config.WITH_DEFAULT == "default_value"
    assert "#WITH_DEFAULT" not in os.environ


def test_empty_lines_are_ignored(dotenv, config):
    dotenv.write("WITHOUT_DEFAULT=pauk\n\n   \n  \nWITH_DEFAULT=naprimer")

    config.init(dotenv.path)

    assert config.WITHOUT_DEFAULT == "pauk"
    assert config.WITH_DEFAULT == "naprimer"
