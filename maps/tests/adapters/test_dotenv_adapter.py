import pytest

from maps_adv.common.config_loader import (
    DotEnvAdapter,
    InitializationError,
    OptionNotFound,
)


def test_raises_if_dotenv_file_not_found():
    with pytest.raises(InitializationError, match="/tmp/.env"):
        DotEnvAdapter("/tmp/.env")


def test_raises_if_key_not_found(write_dotenv):
    write_dotenv("")
    adapter = DotEnvAdapter(write_dotenv.path)

    with pytest.raises(OptionNotFound, match="NONEXISTENT"):
        adapter.load("NONEXISTENT")


@pytest.mark.parametrize(
    "content, expected",
    (
        ["EXISTING=value", "value"],
        ["EXISTING=value\nKEY_TWO=dssss", "value"],
        ["KEY_ONE=123kek\nEXISTING=value", "value"],
        ["KEY_ONE=123kek\n\n\nEXISTING=value\n\n\n\n", "value"],
        ["KEY_ONE=123kek\nEXISTING=value\nKEY_TWO=dssss", "value"],
        ["KEY_ONE=123kek\n\nEXISTING=value\n\nKEY_TWO=dssss", "value"],
        ["KEY_ONE=123kek\n\nEXISTING==value=\n\nKEY_TWO=dssss", "=value="],
        ["KEY_ONE=123kek\n\nEXISTING=\n\nKEY_TWO=dssss", ""],
    ),
)
def test_returns_value(content, expected, write_dotenv):
    write_dotenv(content)
    adapter = DotEnvAdapter(write_dotenv.path)

    got = adapter.load("EXISTING")

    assert got == expected


def test_lines_are_stripped(write_dotenv):
    write_dotenv("KEY_ONE=123kek\n\n EXISTING=value  \n\nKEY_TWO=dssss")
    adapter = DotEnvAdapter(write_dotenv.path)

    got = adapter.load("EXISTING")

    assert got == "value"


@pytest.mark.parametrize(
    "content",
    (
        "#EXISTING=value",
        "#EXISTING=value\nKEY_TWO=dssss",
        "KEY_ONE=123kek\n#EXISTING=value",
        "KEY_ONE=123kek\n\n\n#EXISTING=value\n\n\n\n",
    ),
)
def test_commented_parameter_is_not_loadable(content, write_dotenv):
    write_dotenv(content)
    adapter = DotEnvAdapter(write_dotenv.path)

    with pytest.raises(OptionNotFound):
        adapter.load("EXISTING")
