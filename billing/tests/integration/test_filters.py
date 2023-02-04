import hamcrest as hm

from billing.contract_iface.constants import ContractTypeId
from billing.balance_yt_jobs.contract_notify.filters import fetch_filter_config_from_table, fetch_notify_config_from_table


def test_fetch_filter_config_from_table(yt_client, yt_root):
    table_name = f'{yt_root}/filter_config'
    yt_client.create('table', table_name)

    yt_client.write_table(table_name, [
        {'parameter': 'include', 'value': {'kind': 'GENERAL', 'exclude': {
            'type': [ContractTypeId.PARTNERSHIP]}}},
        {'parameter': 'include', 'value': {'kind': 'SPENDABLE', 'is_offer': True}},
        {'parameter': 'firm', 'value': [1, 2, 3, 4, 5]}
    ])

    config = fetch_filter_config_from_table(yt_client, table_name)

    hm.assert_that(
        config,
        hm.has_entries({
            'include': hm.contains_inanyorder(
                hm.has_entries({'kind': 'GENERAL', 'exclude': hm.has_entries({'type': [ContractTypeId.PARTNERSHIP]})}),
                hm.has_entries({'kind': 'SPENDABLE', 'is_offer': True})
            ),
            'firm': [1, 2, 3, 4, 5]
        }),
    )


def test_fetch_notify_config_from_table(yt_client, yt_root):
    table_name = f'{yt_root}/notify_config'
    yt_client.create('table', table_name)

    yt_client.write_table(table_name, [
        {'name': 'booked', 'current': {'is_booked': True}, 'prev': None, 'changed_to': None},
        {'name': 'faxed', 'current': {'is_faxed': False}, 'prev': {'is_faxed': True}, 'changed_to': None},
        {'name': 'signed', 'current': None, 'prev': None, 'changed_to': {'is_signed': True}},
    ])

    config = fetch_notify_config_from_table(yt_client, table_name)

    assert len(config) == 3

    hm.assert_that(
        config,
        hm.contains_inanyorder(
            hm.has_entries({'name': 'booked', 'current': hm.has_entries({'is_booked': True})}),
            hm.has_entries({'name': 'faxed', 'current': hm.has_entries(
                {'is_faxed': False}), 'prev': hm.has_entries({'is_faxed': True})}),
            hm.has_entries({'name': 'signed', 'changed_to': hm.has_entries({'is_signed': True})})
        )
    )
