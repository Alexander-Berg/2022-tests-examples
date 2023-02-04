from billing.library.python.logmeta_utils.constants import LOG_INTERVAL_KEY
from billing.library.python.logmeta_utils import meta
from datetime import datetime

from yt.wrapper import YtClient, TablePath

from billing.balance_yt_jobs.contract_notify.main import run_job
from billing.balance_yt_jobs.contract_notify.tests.common import ContractJSONBuilder


def create_meta(first_offset, next_offset, with_run_id=False):
    new_meta = {
        'is_updating': False,
        LOG_INTERVAL_KEY: {
            'topics': [{
                'topic': 'balance/prod/contract',
                'cluster': 'kafka-bs',
                'partitions': [{
                    'partition': 0,
                    'first_offset': first_offset,
                    'next_offset': next_offset,
                }]
            }]
        }
    }

    if with_run_id:
        new_meta['run_id'] = datetime.now().replace(microsecond=0).isoformat()

    return new_meta


def create_table_with_inverval(yt_client, table_path, log_meta):
    yt_client.create('table', table_path)
    meta.set_log_tariff_meta(yt_client, table_path, log_meta)


def get_job_folders(yt_client: YtClient, yt_root: str):
    updates_path = yt_client.find_free_subpath(yt_root)
    yt_client.create('map_node', updates_path)

    result_path = yt_client.find_free_subpath(yt_root)
    yt_client.create('map_node', result_path)

    return updates_path, result_path


def test_wrong_configs(yt_client: YtClient, yt_root: str):
    updates_path, result_path = get_job_folders(yt_client, yt_root)

    new_meta = create_meta(0, 10, with_run_id=True)
    create_table_with_inverval(yt_client, f'{updates_path}/test', new_meta)

    contract_config = {}
    notify_config = [{
        'name': 'booked',
        'changed_to': {'is_booked': True}
    }]

    try:
        run_job(yt_client, updates_path, result_path, new_meta,
                contract_config, None, notify_config, None)
    except Exception as e:
        if 'Not set filter config' not in str(e):
            raise Exception(str(e))

    contract_config = {
        'include': {
            'kind': ['GENERAL']
        }
    }
    notify_config = []

    try:
        run_job(yt_client, updates_path, result_path, new_meta,
                contract_config, None, notify_config, None)
    except Exception as e:
        if 'Not set notify config' not in str(e):
            raise Exception(str(e))


def test_filter_contract(yt_client: YtClient, yt_root: str):
    updates_path, result_path = get_job_folders(yt_client, yt_root)

    new_meta = create_meta(0, 10, with_run_id=True)
    # upate_table = f'{updates_path}/test'
    upate_table = TablePath(
        f'{updates_path}/test',
        schema=[
            {'name': 'ID', 'type': 'int64'},
            {'name': 'Version', 'type': 'int64'},
            {'name': 'Object', 'type': 'any'},
            {'name': '__prev_Object', 'type': 'any'},
        ]
    )
    create_table_with_inverval(yt_client, upate_table, new_meta)

    table_data = []

    # suitable contract
    prev_contract = ContractJSONBuilder('GENERAL', id=1)
    prev_contract.add_collateral(is_booked=0)

    contract = ContractJSONBuilder('GENERAL', id=1)
    contract.add_collateral(is_booked=1, manager_code=666)

    table_data.append({'ID': 1, 'Version': 0, 'Object': contract.json, '__prev_Object': prev_contract.json})

    # disable booked
    prev_contract = ContractJSONBuilder('GENERAL', id=2)
    prev_contract.add_collateral(is_booked=0)

    contract = ContractJSONBuilder('GENERAL', id=2, version_id=1)
    contract.add_collateral(is_booked=0)

    table_data.append({'ID': 2, 'Version': 1, 'Object': contract.json, '__prev_Object': prev_contract.json})

    # different type
    prev_contract = ContractJSONBuilder('DISTRIBUTION', id=3)
    prev_contract.add_collateral(is_booked=0)

    contract = ContractJSONBuilder('DISTRIBUTION', id=3, version_id=1)
    contract.add_collateral(is_booked=1)

    table_data.append({'ID': 3, 'Version': 1, 'Object': contract.json, '__prev_Object': prev_contract.json})

    yt_client.write_table(upate_table, table_data)

    contract_config = {
        'include': {
            'kind': ['GENERAL']
        }
    }
    notify_config = [{
        'name': 'booked',
        'changed_to': {'is_booked': True}
    }]

    result_table = run_job(yt_client, updates_path, result_path, new_meta, contract_config, None, notify_config, None)

    assert new_meta == meta.get_log_tariff_meta(yt_client, result_table)

    result_data = list(yt_client.read_table(result_table))

    assert len(result_data) == 1
    assert result_data[0]['ID'] == 1


def test_filter_wrong_contract(yt_client: YtClient, yt_root: str):
    updates_path, result_path = get_job_folders(yt_client, yt_root)

    new_meta = create_meta(0, 10, with_run_id=True)
    upate_table = TablePath(
        f'{updates_path}/test',
        schema=[
            {'name': 'ID', 'type': 'int64'},
            {'name': 'Version', 'type': 'int64'},
            {'name': 'Object', 'type': 'any'},
            {'name': '__prev_Object', 'type': 'any'},
        ]
    )
    create_table_with_inverval(yt_client, upate_table, new_meta)

    table_data = []

    # wrong collateral_type_id
    prev_contract = ContractJSONBuilder('AFISHA', id=1)
    prev_contract.add_collateral(num='11', collateral_type_id=4060)

    contract = ContractJSONBuilder('AFISHA', id=1)
    contract.add_collateral(num='11', collateral_type_id=4060, end_dt="2021-09-23T10:00:00")

    table_data.append({'ID': 1, 'Version': 0, 'Object': contract.json, '__prev_Object': prev_contract.json})

    # right collateral_type_id
    prev_contract2 = ContractJSONBuilder('AFISHA', id=2)
    prev_contract2.add_collateral(num='11', collateral_type_id=6050)

    contract2 = ContractJSONBuilder('AFISHA', id=2)
    contract2.add_collateral(num='11', collateral_type_id=6050, end_dt="2021-09-23T10:00:00")

    table_data.append({'ID': 2, 'Version': 1, 'Object': contract2.json, '__prev_Object': prev_contract2.json})

    yt_client.write_table(upate_table, table_data)

    contract_config = {'kind': 'AFISHA'}
    notify_config = [{'name': 'booked'}]

    result_table = run_job(yt_client, updates_path, result_path, new_meta, contract_config, None, notify_config, None)

    assert new_meta == meta.get_log_tariff_meta(yt_client, result_table)

    result_data = list(yt_client.read_table(result_table))

    assert len(result_data) == 1
    assert result_data[0]['ID'] == 2
