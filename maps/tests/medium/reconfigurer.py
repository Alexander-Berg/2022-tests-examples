import pytest
import mock
from pymongo.database import Database

from maps.infra.ecstatic.sandbox.reconfigurer.lib.reconfigure import ReconfigureError


def test_fix_duplicated_hosts_in_host_dc(mongo_client: Database, reconfigurer):
    with mock.patch('maps.infra.ecstatic.sandbox.reconfigurer.lib.reconfigure.Reconfigurer._commit_write') as commit:
        commit.side_effect = mock.Mock()
        reconfigurer()

    for row in mongo_client.db['host_dc'].find({}):
        assert mongo_client.db['host_dc'].count({'host': row['host'], 'dc': row['dc'], 'group': row['group']}) == 3

    reconfigurer(hosts_config='data/host-groups-empty.conf')

    for row in mongo_client.db['host_dc'].find({}):
        assert mongo_client.db['host_dc'].count({'host': row['host'], 'dc': row['dc'], 'group': row['group']}) == 1


def test_revision_conflict(reconfigurer):
    with pytest.raises(ReconfigureError):
        reconfigurer(config_revision=0)


def test_missing_deploy(reconfigurer):
    with pytest.raises(ReconfigureError, match='No deploy lists found for datasets: pkg-b'):
        reconfigurer(config='data/ecstatic-missing-deploy.conf')


def test_restrict_to_download(reconfigurer, mongo_client: Database):
    reconfigurer()
    for row in mongo_client['acls'].find({'dataset': 'pkg-strict'}):
        assert row.get('forbid_download', False)
