import pytest

import yt.wrapper as yt

from billing.library.python import yql_utils

from billing.hot.nirvana.act_o_tron import act_o_tron
from billing.hot.nirvana.tests.test_lib import utils as u


DATA = [
    {'s': '+1', 't': u.Client1.payout | u.debit(33.48) | u.info(
        **u.tariffer_payload(product_mdh_id="p-1", amount_wo_vat="27.9"))},
    {'s': '+1', 't': u.Client1.payout | u.debit(33.48) | u.info(
        **u.tariffer_payload(product_mdh_id="p-1", amount_wo_vat="27.9"))},
    {'s': '+1', 't': u.Client1.payout | u.debit(33.48) | u.info(
        txt='skip dry run', **u.tariffer_payload(dry_run=True, product_mdh_id="p-1", amount_wo_vat="27.9"))},
    {'s': '-1', 't': u.Client1.payout | u.debit(33.48) | u.info(
        **u.tariffer_payload(product_mdh_id="p-1", amount_wo_vat="27.9"))},
    {'s': '-1', 't': u.Client1.payout | u.debit(33.48) | u.info(
        txt='skip negative product sum', **u.tariffer_payload(product_mdh_id="p-3", amount_wo_vat="27.9"))},
    {'s': '+1', 't': u.Client1.payout | u.debit(33.48) | {'dt': 1634026099} | u.info(
        txt='skip next period', **u.tariffer_payload(product_mdh_id="p-1", amount_wo_vat="11.1"))},
    {'s': '+1', 't': u.Client1.payout | {'contract_id': '2'} | u.debit(33.48) | u.info(
        **u.tariffer_payload(product_mdh_id="p-1", amount_wo_vat="27.9"))},
    {'s': '+1', 't': u.Client1.payout | u.debit(33.48) | u.info(
        **u.tariffer_payload(product_mdh_id="p-2", amount_wo_vat="27.9"))},
]


@pytest.fixture()
def reference_firm_tax(yt_client, yt_root):
    schema = [
        {'name': 'id', 'type': 'int64'},
        {'name': 'tax_policies', 'type_v3': {'type_name': 'optional', 'item': 'yson'}},
    ]
    table_path = yt.ypath_join(yt_root, 'reference_firm_tax')
    yt_client.create('table', table_path, recursive=True, attributes={'schema': schema})
    yt_client.write_table(
        table_path,
        [{
            'id': 13,
            'tax_policies': [
                {
                    "default_tax": 1,
                    "hidden": 0,
                    "id": 1,
                    "mdh_id": "93fb537f-e424-49b6-8b3a-b735f9164708",
                    "name": "Россия, резидент, НДС облагается",
                    "percents": [
                        {
                            "dt": "2019-01-01T00:00:00+03:00",
                            "hidden": 0,
                            "id": 281,
                            "mdh_id": "91f77dca-81d8-4f6e-b3d1-04fa78ce6b50",
                            "nds_pct": 20,
                            "nsp_pct": 0
                        },
                        {
                            "dt": "2003-01-01T00:00:00+03:00",
                            "hidden": 0,
                            "id": 2,
                            "mdh_id": "9feab74e-4252-4507-a572-124304d9b9eb",
                            "nds_pct": 20,
                            "nsp_pct": 5
                        },
                        {
                            "dt": "2004-01-01T00:00:00+03:00",
                            "hidden": 0,
                            "id": 1,
                            "mdh_id": "cd174c94-b588-451a-81df-78d765bd258f",
                            "nds_pct": 18,
                            "nsp_pct": 0
                        }
                    ],
                    "region_id": 225,
                    "resident": 1,
                    "spendable_nds_id": 18
                }
            ]
        }],
    )
    yield table_path
    yt_client.remove(table_path, force=True)


@pytest.fixture()
def accrualer_events(yt_client, yt_root, lbexport_formater, actentity_formater):
    schema = [
        {'name': 'data', 'type': 'string'},
    ]
    table_path = yt.ypath_join(yt_root, 'accrualer_events')
    yt_client.create('table', table_path, recursive=True, attributes={'schema': schema})
    ready_data = []
    for n, dd in enumerate(DATA):
        ready_data.append({
            'data': actentity_formater({
                'sign': dd['s'],
                'transaction': lbexport_formater({'id': n, 'seq_id': n, 'dt': 1627465336 + n} | dd['t'])
            })
        })
    yt_client.write_table(
        table_path,
        ready_data,
    )
    yield table_path
    yt_client.remove(table_path, force=True)


@pytest.mark.parametrize('dry_run', [pytest.param(True), pytest.param(False)])
def test_act_o_tron_run_yql(dry_run, yql_client, yt_client, yt_root, reference_firm_tax, accrualer_events, ns_config):
    output_unacted_events = yt.ypath_join(yt_root, 'output_unacted_events')
    output_acted_events = yt.ypath_join(yt_root, 'output_acted_events')
    output_act_rows = yt.ypath_join(yt_root, 'output_act_rows')

    with yt_client.Transaction() as transaction:
        query_result = act_o_tron.run_yql(
            yql_client=yql_client,
            accrualer_events_subquery=f"""
                $accrualer_events =
                SELECT * FROM `{accrualer_events}`
                ;
            """,
            reference_firm_tax=reference_firm_tax,
            act_dt='2021-09-30',
            namespace='ns',
            dry_run=dry_run,
            location_attrs_config=ns_config['location_attrs_config'],
            prev_unacted_events=accrualer_events,
            output_unacted_events=output_unacted_events,
            output_acted_events=output_acted_events,
            output_act_rows=output_act_rows,
            transaction=transaction,

        )

    ret = {
        'output_unacted_events': list(yt_client.read_table(output_unacted_events, format="yson")),
        'output_acted_events': list(yt_client.read_table(output_acted_events, format="yson")),
        'output_act_rows': list(yt_client.read_table(output_act_rows, format="yson")),
        'metrics': yt.yson.convert.yson_to_json(
            yql_utils.query_metrics.extract_metrics(query_result)[output_act_rows]
        ),
    }

    return ret
