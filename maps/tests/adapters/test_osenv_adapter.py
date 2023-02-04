import pytest

from maps_adv.common.config_loader import OptionNotFound, OsEnvAdapter


@pytest.fixture
def adapter():
    return OsEnvAdapter()


def test_raises_if_env_var_not_set(adapter):
    with pytest.raises(OptionNotFound, match="NONEXISTENT"):
        adapter.load("NONEXISTENT")


@pytest.mark.parametrize("value", ("YES", "", "1"))
def test_returns_value(value, adapter, set_os_env):
    set_os_env("EXISTING", value)

    got = adapter.load("EXISTING")

    assert got == value
