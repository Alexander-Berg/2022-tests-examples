from smb.common.multiruntime.lib.io import setup_filesystem

pytest_plugins = ["maps_adv.common.pg_engine.pytest.plugin"]

setup_filesystem("maps_adv/common/pg_engine/tests/", "tests")
