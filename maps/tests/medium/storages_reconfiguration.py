import pytest
import typing as tp
from dataclasses import dataclass
from pymongo.database import Database

from maps.infra.ecstatic.sandbox.reconfigurer.lib.reconfigure import ReconfigureError


@dataclass
class UpdateStoragesCase:
    name: str
    config: str
    expected_storages: list[set[str]]
    initial_config: tp.Optional[str] = None
    error_mesage: tp.Optional[str] = None

    def __str__(self) -> str:
        return self.name


CASES = [
    UpdateStoragesCase(
        name='not_crossing_shards',
        config='[{"hosts": ["storage11", "storage12", "storage13"]},'
               '{"hosts": ["storage21", "storage22", "storage13"]}]',
        expected_storages=[{"storage11", "storage12", "storage13"}],
        error_mesage='One host must be in only one shard',
    ),
    UpdateStoragesCase(
        name='remove_all_hosts_from_shard',
        config='[{"hosts": []}]',
        expected_storages=[{"storage11", "storage12", "storage13"}],
        error_mesage='In testing and stable shards must be 3 or more hosts',
    ),
    UpdateStoragesCase(
        name='remove_shard',
        config='[]',
        expected_storages=[{"storage11", "storage12", "storage13"}],
        error_mesage='Cannot remove shard',
    ),
    UpdateStoragesCase(
        name='less_than_3_storage_in_shard',
        config='[{"hosts": ["storage11", "storage12"]}]',
        expected_storages=[{"storage11", "storage12", "storage13"}],
        error_mesage='In testing and stable shards must be 3 or more hosts',
    ),
    UpdateStoragesCase(
        name='have_hosts_with_one_name_in_shard',
        config='[{"hosts": ["storage11", "storage12", "storage12", "storage13"]}]',
        expected_storages=[{"storage11", "storage12", "storage13"}],
        error_mesage='One host must be in only one shard',
    ),
    UpdateStoragesCase(
        name='remove_more_than_1_host_from_shard',
        config='[{"hosts": ["storage11"]}]',
        expected_storages=[{"storage11", "storage12", "storage13"}],
        error_mesage='In testing and stable shards must be 3 or more hosts',
    ),
    UpdateStoragesCase(
        name='add_and_remove_2_storages_from_shard',
        config='[{"hosts": ["storage11", "storage14", "storage15"]}]',
        expected_storages=[{"storage11", "storage12", "storage13"}],
        error_mesage='',
    ),
    UpdateStoragesCase(
        name='add_host_to_shard',
        config='[{"hosts": ["storage11", "storage12", "storage13", "storage14"]}]',
        expected_storages=[{"storage11", "storage12", "storage13", "storage14"}],
        error_mesage=None,
    ),
    UpdateStoragesCase(
        name='add_2_hosts_to_shard',
        config='[{"hosts": ["storage11", "storage12", "storage13", "storage14", "storage15"]}]',
        expected_storages=[{"storage11", "storage12", "storage13", "storage14", "storage15"}],
        error_mesage=None,
    ),
    UpdateStoragesCase(
        name='add_new_shard',
        config='[{"hosts": ["storage11", "storage12", "storage13"]}, '
               '{"hosts": ["storage21", "storage22", "storage23"]}]',
        expected_storages=[{"storage11", "storage12", "storage13"}, {"storage21", "storage22", "storage23"}],
        error_mesage=None,
    ),
    UpdateStoragesCase(
        name='swap_storages',
        initial_config='[{"hosts": ["storage11", "storage12", "storage13"]}, '
                       '{"hosts": ["storage21", "storage22", "storage23"]}]',
        config='[{"hosts": ["storage11", "storage12", "storage21"]}, '
               '{"hosts": ["storage13", "storage22", "storage23"]}]',
        expected_storages=[{"storage11", "storage12", "storage21"}, {"storage13", "storage22", "storage23"}],
        error_mesage=None,
    ),
    UpdateStoragesCase(
        name='move_storage_to_new_shard',
        initial_config='[{"hosts": ["storage11", "storage12", "storage13", "storage14"]}]',
        config='[{"hosts": ["storage11", "storage12", "storage13"]}, '
               '{"hosts": ["storage14", "storage15", "storage16"]}]',
        expected_storages=[{"storage11", "storage12", "storage13"}, {"storage14", "storage15", "storage16"}],
        error_mesage=None,
    ),
    UpdateStoragesCase(
        name='big_update',
        initial_config='[{"hosts": ["storage11", "storage12", "storage13", "storage14"]}, '
                       '{"hosts": ["storage21", "storage22", "storage23"]}]',
        config='[{"hosts": ["storage11", "storage12", "storage13"]}, '
               '{"hosts": ["storage14", "storage22", "storage23"]},'
               '{"hosts": ["storage21", "storage32", "storage33"]}]',
        expected_storages=[{"storage11", "storage12", "storage13"},
                           {"storage14", "storage22", "storage23"},
                           {"storage21", "storage32", "storage33"}],
        error_mesage=None,
    ),

]


@pytest.mark.parametrize('case', CASES, ids=str)
def test_update_storages(mongo_client: Database, reconfigurer, case: UpdateStoragesCase):
    if case.initial_config is not None:
        reconfigurer(storages_config=case.initial_config, installation='testing')
    if case.error_mesage is None:
        reconfigurer(storages_config=case.config, installation='testing')
    else:
        with pytest.raises(ReconfigureError, match=case.error_mesage):
            reconfigurer(storages_config=case.config, installation='testing')
    found_storages = []
    for shard in mongo_client['storages'].find({}, {'hosts': 1}):
        if shard['_id'] != '__SENTINEL__':
            found_storages.append(set(shard['hosts']))
    assert sorted(found_storages) == sorted(case.expected_storages)
