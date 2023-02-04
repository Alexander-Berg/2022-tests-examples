import pytest
from pymongo.database import Database


@pytest.mark.parametrize('hosts_config', ['data/host-groups.json', 'data/host-groups-new.json'])
def test_save_tvm(mongo_client: Database, reconfigurer, hosts_config: str):
    reconfigurer(hosts_config=hosts_config)

    found = False

    for host in mongo_client['host_dc'].find({}):
        assert host['tvm_id'] == 1234
        found = True

    assert found
