import os
from datetime import datetime, timezone

import pytest

from maps_adv.common.config_loader import ConfigLoader, Option, OptionNotFound
from smb.common.multiruntime.lib.basics import is_arcadia_python

if is_arcadia_python:
    from library.python.vault_client.errors import ClientError
else:
    from vault_client.errors import ClientError


@pytest.fixture
def setup_yav_adapter(set_os_env, mock_yav):
    set_os_env("YAV_TOKEN", "oauth_token")
    set_os_env("YAV_SECRET_ID", "sec-01e6zwtfg5g4b727yq4w3qvnz3")
    mock_yav.return_value = yav_result


@pytest.fixture
def setup_dotenv_adapter(set_os_env, write_dotenv):
    set_os_env("DOTENV_PATH", write_dotenv.path)
    write_dotenv("")


@pytest.mark.usefixtures("setup_dotenv_adapter", "setup_yav_adapter")
@pytest.mark.parametrize(
    "env, kw", (["EXISTING", {}], ["ANOTHER", {"load_from": "ANOTHER"}])
)
def test_will_load_from_os_env(env, kw, set_os_env):
    set_os_env(env, "YES")

    loader = ConfigLoader(Option("EXISTING", **kw))
    loader.init()

    assert loader["EXISTING"] == "YES"


@pytest.mark.usefixtures("setup_dotenv_adapter", "setup_yav_adapter")
@pytest.mark.parametrize(
    "key, kw", (["EXISTING", {}], ["ANOTHER", {"load_from": "ANOTHER"}])
)
def test_will_load_from_dotenv(key, kw, write_dotenv):
    write_dotenv(f"{key}=YESYES")

    loader = ConfigLoader(Option("EXISTING", **kw))
    loader.init()

    assert loader["EXISTING"] == "YESYES"


@pytest.mark.parametrize("source", ("osenv", "dotenv"))
@pytest.mark.parametrize(
    "extra_res, kw",
    (
        [{"value": {"EXISTING": "1"}}, {}],
        [{"value": {"ANOTHER": "1"}}, {"load_from": "ANOTHER"}],
    ),
)
@pytest.mark.usefixtures("setup_dotenv_adapter")
def test_will_load_from_yav(extra_res, kw, source, set_os_env, write_dotenv, mock_yav):
    mock_yav.return_value = {**yav_result, **extra_res}
    if source == "osenv":
        set_os_env("YAV_TOKEN", "oauth_token")
        set_os_env("YAV_SECRET_ID", "sec-01e6zwtfg5g4b727yq4w3qvnz3")
    elif source == "dotenv":
        write_dotenv(
            "YAV_TOKEN=oauth_token\nYAV_SECRET_ID=sec-01e6zwtfg5g4b727yq4w3qvnz3"
        )

    loader = ConfigLoader(Option("EXISTING", **kw))
    loader.init()

    assert loader["EXISTING"] == "1"


def test_dotenv_loads_from_cwd_by_default(write_dotenv):
    write_dotenv.path = os.path.join(os.getcwd(), ".env")
    write_dotenv("EXISTING=YESYES")

    loader = ConfigLoader(Option("EXISTING"))
    loader.init()

    assert loader["EXISTING"] == "YESYES"


@pytest.mark.usefixtures("setup_dotenv_adapter", "setup_yav_adapter")
def test_os_env_has_highes_priority(set_os_env, write_dotenv):
    write_dotenv("EXISTING=DOTENV")
    set_os_env("EXISTING", "OSENV")

    loader = ConfigLoader(Option("EXISTING"))
    loader.init()

    assert loader["EXISTING"] == "OSENV"


@pytest.mark.usefixtures("setup_dotenv_adapter", "setup_yav_adapter")
def test_dotenv_has_higher_priority_then_yav(set_os_env, write_dotenv):
    write_dotenv("EXISTING=DOTENV")

    loader = ConfigLoader(Option("EXISTING"))
    loader.init()

    assert loader["EXISTING"] == "DOTENV"


@pytest.mark.usefixtures("setup_dotenv_adapter", "setup_yav_adapter")
@pytest.mark.parametrize("kw", ({}, {"load_from": "ANOTHER"}))
def test_raises_if_option_not_present(kw):
    loader = ConfigLoader(Option("UNEXISTING", **kw))

    with pytest.raises(OptionNotFound, match="UNEXISTING"):
        loader.init()


@pytest.mark.usefixtures("setup_dotenv_adapter", "setup_yav_adapter")
def test_uses_default_if_option_not_present():
    loader = ConfigLoader(Option("UNEXISTING", default="DEFAULT VALUE"))
    loader.init()

    assert loader["UNEXISTING"] == "DEFAULT VALUE"


@pytest.mark.usefixtures("setup_dotenv_adapter", "setup_yav_adapter")
@pytest.mark.parametrize("source", ("osenv", "dotenv", "yav"))
def test_uses_converter_if_specified(source, set_os_env, write_dotenv):
    if source == "osenv":
        set_os_env("EXISTING", "1")
    elif source == "dotenv":
        write_dotenv("EXISTING=1")

    loader = ConfigLoader(Option("EXISTING", converter=int))
    loader.init()

    assert loader["EXISTING"] == 1


@pytest.mark.usefixtures("setup_dotenv_adapter", "setup_yav_adapter")
def test_converter_not_applied_for_default_value():
    loader = ConfigLoader(Option("UNEXISTING", default="1", converter=int))
    loader.init()

    assert loader["UNEXISTING"] == "1"


def test_logs_if_dotenv_path_not_found(set_os_env, caplog):
    set_os_env("EXISTING", "1")
    loader = ConfigLoader(Option("EXISTING"))

    loader.init()

    cwd = os.path.join(os.getcwd(), ".env")
    assert (
        "Adapter DotEnvAdapter omitted because initialization errored with message "
        f'"{cwd}"' in caplog.messages
    )


@pytest.mark.usefixtures("setup_yav_adapter")
def test_logs_if_yav_adapter_errored(set_os_env, mock_yav):
    set_os_env("EXISTING", "1")
    mock_yav.side_effect = ClientError(lol="kek", maka="rek")
    loader = ConfigLoader(Option("EXISTING"))

    with pytest.raises(ClientError, match="{'lol': 'kek', 'maka': 'rek'}"):
        loader.init()


def test_logs_if_yav_adapter_not_configured(set_os_env, mock_yav, caplog):
    set_os_env("EXISTING", "1")
    loader = ConfigLoader(Option("EXISTING"))

    loader.init()

    assert (
        "Adapter YavAdapter omitted because dependency YAV_TOKEN not found"
        in caplog.messages
    )


yav_result = {
    "comment": "",
    "created_at": datetime(2020, 4, 28, 8, 0, 31, 246000, tzinfo=timezone.utc),
    "created_by": 1120000000098712,
    "creator_login": "sivakov512",
    "secret_name": "aioyav-example",
    "secret_uuid": "sec-01e6zwtfg5g4b727yq4w3qvnz3",
    "value": {"EXISTING": "1"},
    "version": "ver-01e6zwtfgen5w925z9qztr9hcv",
}
