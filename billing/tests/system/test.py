import pytest
from prettytable import PrettyTable

import yt.wrapper as yt

from billing.hot.nirvana.detalization import detalization
from billing.hot.nirvana.mark_o_tron import mark_o_tron
from billing.hot.nirvana.tests.test_lib import utils as u


DATA_storno_transaction = u.DataCollector(
).add(
    group='step-01', batch=u.EventBatch(u.Client1.agent_rewards | u.dt(0))(
        u.credit(100) | u.info('Первое'),
        u.credit(123) | u.info('Второе'),
    )
).add(
    group='step-01', batch=u.EventBatch(u.dt(123) | u.info('Дата выплаты Второго'))(
        u.Client1.agent_rewards | u.debit(123),
        u.Client1.agent_rewards_payable | u.credit(123),
    )
).add(
    group='step-02', batch=u.EventBatch(u.dt(50) | u.info('Дата выплаты Первого'))(
        u.Client1.agent_rewards | u.debit(100),
        u.Client1.agent_rewards_payable | u.credit(100),
    )
).add(
    group='step-02', batch=u.EventBatch(u.info('Смена даты Второго'))(
        u.Client1.agent_rewards_payable | u.credit(-123) | u.dt(123),
        u.Client1.agent_rewards_payable | u.credit(123) | u.dt(40),
    )
).add(
    group='step-03', batch=u.EventBatch(u.batch(ext_id='payout/1/0002') | u.info('Выплата Второго') | u.dt(45))(
        u.Client1.agent_rewards_payable | u.debit(123),
        u.Client1.payout | u.credit(123),
    )
).add(
    group='step-03', batch=u.EventBatch(u.info('Отправка Второго') | u.dt(45))(
        u.Client1.payout | u.debit(123),
        u.Client1.payout_sent | u.credit(123),
    )
).add(
    group='step-03', batch=[
        u.Client1.payout_sent | u.debit(0) | u.info('Платёжка Второго', netting=['payout/1/0002'], oebs_payment_id=2) | u.batch(type_='register payout') | u.dt(47),
    ]
).add(
    group='step-04', batch=u.EventBatch(u.batch(ext_id='payout/1/0001') | u.info('Выплата Первого') | u.dt(55))(
        u.Client1.agent_rewards_payable | u.debit(100),
        u.Client1.payout | u.credit(100),
    )
).add(
    group='step-04', batch=u.EventBatch(u.info('Отправка Первого') | u.dt(56))(
        u.Client1.payout | u.debit(100),
        u.Client1.payout_sent | u.credit(100),
    )
).add(
    group='step-04', batch=[
        u.Client1.payout_sent | u.debit(0) | u.info('Платёжка Первого', netting=['payout/1/0001'], oebs_payment_id=1) | u.batch(type_='register payout') | u.dt(57),
    ]
)


DATA_simple_transaction = u.DataCollector(
).add(
    group='step-01', batch=u.EventBatch(u.Client1.agent_rewards | u.dt(0))(
        u.credit(100) | u.info('Первое'),
        u.credit(123) | u.info('Второе'),
    )
).add(
    group='step-01', batch=u.EventBatch(u.dt(123) | u.info('Дата выплаты Второго'))(
        u.Client1.agent_rewards | u.debit(123),
        u.Client1.agent_rewards_payable | u.credit(123),
    )
).add(
    group='step-02', batch=u.EventBatch(u.dt(50) | u.info('Дата выплаты Первого'))(
        u.Client1.agent_rewards | u.debit(100),
        u.Client1.agent_rewards_payable | u.credit(100),
    )
).add(
    group='step-02', batch=u.EventBatch(u.info('Смена даты Второго'))(
        u.Client1.agent_rewards_payable | u.debit(123) | u.dt(123),
        u.Client1.agent_rewards_payable | u.credit(123) | u.dt(40),
    )
).add(
    group='step-03', batch=u.EventBatch(u.batch(ext_id='payout/1/0002') | u.info('Выплата Второго') | u.dt(45))(
        u.Client1.agent_rewards_payable | u.debit(123),
        u.Client1.payout | u.credit(123),
    )
).add(
    group='step-03', batch=u.EventBatch(u.info('Отправка Второго') | u.dt(45))(
        u.Client1.payout | u.debit(123),
        u.Client1.payout_sent | u.credit(123),
    )
).add(
    group='step-03', batch=[
        u.Client1.payout_sent | u.debit(0) | u.info('Платёжка Второго', netting=['payout/1/0002'], oebs_payment_id=2) | u.batch(type_='register payout') | u.dt(47),
    ]
).add(
    group='step-04', batch=u.EventBatch(u.batch(ext_id='payout/1/0001') | u.info('Выплата Первого') | u.dt(55))(
        u.Client1.agent_rewards_payable | u.debit(100),
        u.Client1.payout | u.credit(100),
    )
).add(
    group='step-04', batch=u.EventBatch(u.info('Отправка Первого') | u.dt(56))(
        u.Client1.payout | u.debit(100),
        u.Client1.payout_sent | u.credit(100),
    )
).add(
    group='step-04', batch=[
        u.Client1.payout_sent | u.debit(0) | u.info('Платёжка Первого', netting=['payout/1/0001'], oebs_payment_id=1) | u.batch(type_='register payout') | u.dt(57),
    ]
)


@pytest.mark.parametrize(
    'data',
    [
        pytest.param(DATA_storno_transaction, id='storno_transaction'),
        pytest.param(DATA_simple_transaction, id='simple_transaction'),
    ]
)
def test_mark_o_tron_complex(yql_client, yt_client, yt_root, lbexport_formater, ns_config, data):
    input_table_dir = yt.ypath_join(yt_root, 'input_table')
    marked_dir = yt.ypath_join(yt_root, 'marked')
    published_marked_dir = yt.ypath_join(yt_root, 'published_marked')
    unmarked_dir = yt.ypath_join(yt_root, 'unmarked')
    detalization_dir = yt.ypath_join(yt_root, 'detalization')
    undetalized_dir = yt.ypath_join(yt_root, 'undetalized')

    prev_unmarked_path = None
    prev_undetalized_path = None

    for step in data.groups():
        input_table_path = yt.ypath_join(input_table_dir, step)
        marked_path = yt.ypath_join(marked_dir, step)
        published_marked_path = yt.ypath_join(published_marked_dir, step)
        unmarked_path = yt.ypath_join(unmarked_dir, step)
        detalization_path = yt.ypath_join(detalization_dir, step)
        undetalized_path = yt.ypath_join(undetalized_dir, step)

        data.make_table(yt_client, input_table_path, lbexport_formater, group=step)

        with yt_client.Transaction() as transaction:
            # Stage 1: mark events
            mark_o_tron.run_yql(
                yql_client=yql_client,
                accounter_event_subquery=f"""
                    $accounter_events =
                    SELECT * FROM `{input_table_path}`
                    ;
                """,
                account_types_config=ns_config['account_types_config'],
                namespace='ns',
                dry_run=False,
                prev_unmarked=prev_unmarked_path,
                output_marked=marked_path,
                output_unmarked=unmarked_path,
                transaction=transaction,
            )
            # Stage 2: join events with payments
            detalization.run_yql(
                yql_client=yql_client,
                detalization_sign_config=detalization.format_sign_config(ns_config['detalization_sign_config']),
                location_attrs_config=ns_config['location_attrs_config'],
                extract_fields_config=detalization.format_fields_config(ns_config['extract_fields_config']),
                input_marked_events_path=marked_path,
                input_prev_undetalized_payments_path=prev_undetalized_path,
                input_published_events_dir=published_marked_dir,
                input_published_events_range_from='',
                input_published_events_range_to=step,
                output_detalization=detalization_path,
                output_undetalized_payments=undetalized_path,
                transaction=transaction,
            )
            # Stage 3: publish data
            yt_client.copy(marked_path, published_marked_path, recursive=True)

        prev_unmarked_path = unmarked_path
        prev_undetalized_path = undetalized_path

    ret = {}
    for t in yt_client.list(marked_dir):
        ret[t] = {'marked': u.sort_yson(yt_client.read_table(yt.ypath_join(marked_dir, t), format="yson"))}
    for t in yt_client.list(unmarked_dir):
        ret[t]['unmarked'] = u.sort_json(yt_client.read_table(yt.ypath_join(unmarked_dir, t), format="json"))
    for t in yt_client.list(detalization_dir):
        ret[t]['detalization'] = sorted(
            list(yt_client.read_table(yt.ypath_join(detalization_dir, t), format="yson")),
            key=lambda o: (o['payment_id'], o['payout_id'], o['original_accounter_internal_id'], o['mark_accounter_internal_id'])
        )
    for t in yt_client.list(undetalized_dir):
        ret[t]['undetalized'] = u.sort_yson(yt_client.read_table(yt.ypath_join(undetalized_dir, t), format="yson"))

    _marked = PrettyTable(
        title='Marked events',
        field_names=[
            'account_id',  # 1
            'client_id',
            'contract_id',
            'currency',
            'type',
            'mark_amount',
            'partition_amount',
            'original_amount',
            'mark_id',
            'original_id',  # 10
            'mark_ext_id',
            'original_ext_id',
            'mark_dt',
            'original_dt',  # 14
        ],
        sortby='account_id',
        sort_key=lambda o: (o[1], o[14], o[10]),
    )
    _detalization = PrettyTable(
        title='Detalization of payments',
        field_names=[
            'client_id',
            'contract_ext_id',
            'contract_id',
            'currency',
            'sign',
            'amount_full',
            'amount_payout',
            'event_type',
            'payment_id',
            'payout_id',
            'event_id',
            'event_dt',
        ],
    )
    for s in ret.values():
        _marked.add_rows([[
            r['mark_data']['event']['account_id'],
            r['mark_data']['event']['loc']['client_id'],
            r['mark_data']['event']['loc']['contract_id'],
            r['mark_data']['event']['loc']['currency'],
            r['mark_data']['event']['loc']['type'],
            r['mark_data']['event']['amount'],
            r['partition_amount'],
            r['original_amount'],
            r['mark_data']['id'],
            r['original_data']['id'],
            r['mark_data']['event_batch']['external_id'],
            r['original_data']['event_batch']['external_id'],
            r['mark_data']['event_batch']['dt'],
            r['original_data']['event_batch']['dt'],
        ] for r in s['marked']])
        _detalization.add_rows([[
            r['client_id'],
            r['contract_external_id'],
            r['contract_id'],
            r['currency'],
            r['detalization_sign'],
            r['original_event_amount_full'],
            r['original_event_amount_payout'],
            r['original_event_type'],
            r['payment_id'],
            r['payout_id'],
            r['original_event_data']['id'],
            r['original_event_data']['event']['dt'],
        ] for r in s['detalization']])
    ret['pretty_print'] = f'\n{_marked.get_string()}\n{_detalization.get_string()}'

    return ret
