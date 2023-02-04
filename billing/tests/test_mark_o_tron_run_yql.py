import yt.wrapper as yt

from billing.hot.nirvana.mark_o_tron import mark_o_tron
from billing.hot.nirvana.tests.test_lib import utils as u


DATA = [
    # Разное
    [
        u.Client1.common | {'account_id': 0, 'account_type': 'unknown_account_type_89hnufr6u'} | u.info('Всегда output_unmarked'),
        u.Client1.cashless | {'namespace': 'unknown_ns'} | u.info('НИКОГДА НЕ ПОПАДАЕТ В OUTPUT'),
        u.Client1.cashless | u.credit(100500) | {'already_marked_amount': '100500'} | u.info('Старнная штука, но за кейс прокатит'),
    ],
    # BILLING-440 1. 1 взаимозачёт в выплате
    [
        u.Client1.cashless | u.credit(100),
    ],
    [
        u.Client1.commissions_with_vat | u.debit(10),
    ],
    u.EventBatch(u.batch(ext_id='payout/1/0001'))(
        u.Client1.cashless | u.debit(100),
        u.Client1.commissions_with_vat | u.credit(10),
        u.Client1.payout | u.credit(90),
    ),
    [
        u.Client1.payout | u.debit(90),
        u.Client1.payout_sent | u.credit(90) | u.info('Событие ALPHA'),
    ],
    [
        u.Client1.payout_sent | u.debit(0) | u.info('Маркерует ALPHA', netting=['payout/1/0001'], oebs_payment_id=1) | u.batch(type_='register payout'),
    ],
    [
        u.Client1.payout_sent | u.debit(90) | u.info('Маркерует ALPHA', netting=['payout/1/0001'], oebs_payment_id=1) | u.batch(type_='confirm payout'),
    ],
    # BILLING-440 2. 2 взаимозачета в выплате, остаток комиссий первого ВЗ переносится в второй
    [
        u.Client1.cashless | u.credit(150),
    ],
    [
        u.Client1.commissions_with_vat | u.debit(200) | u.info('150 в payout/1/0002, а 50 в payout/1/0003'),
    ],
    u.EventBatch(u.batch(ext_id='payout/1/0002'))(
        u.Client1.cashless | u.debit(150),
        u.Client1.commissions_with_vat | u.credit(150),
        u.Client1.payout | u.credit(0),
    ),
    [
        u.Client1.cashless | u.credit(160),
    ],
    [
        u.Client1.commissions_with_vat | u.debit(100),
    ],
    u.EventBatch(u.batch(ext_id='payout/1/0003'))(
        u.Client1.cashless | u.debit(160),
        u.Client1.commissions_with_vat | u.credit(150),
        u.Client1.payout | u.credit(10),
    ),
    [
        u.Client1.payout | u.debit(0),
        u.Client1.payout_sent | u.credit(0),
        u.Client1.payout | u.debit(10),
        u.Client1.payout_sent | u.credit(10),
    ],
    [
        u.Client1.payout_sent | u.debit(0) | u.info('Дубликат в netting', netting=['payout/1/0002', 'payout/1/0002'], oebs_payment_id=2) | u.batch(type_='register payout'),
    ],
    [
        u.Client1.payout_sent | u.debit(0) | u.info(netting=['payout/1/0003'], oebs_payment_id=2) | u.batch(type_='register payout'),
    ],
    [
        u.Client1.payout_sent | u.debit(0) | u.info(netting=['payout/1/0002'], oebs_payment_id=2) | u.batch(type_='confirm payout'),
    ],
    [
        u.Client1.payout_sent | u.debit(10) | u.info(netting=['payout/1/0003'], oebs_payment_id=2) | u.batch(type_='confirm payout'),
    ],
    # BILLING-440 7. ОК от ARD, получение id платёжки, отмена платёжки и попадание выплат в другую платёжку
    [
        u.Client1.payout_sent | u.credit(90) | u.info(netting=['payout/1/0001'], oebs_payment_id=1) | u.batch(type_='cancel payout'),
    ],
    [
        u.Client1.payout_sent | u.debit(0) | u.info(netting=['payout/1/0001'], oebs_payment_id=3) | u.batch(type_='register payout'),
    ],
    [
        u.Client1.payout_sent | u.debit(90) | u.info(netting=['payout/1/0001'], oebs_payment_id=3) | u.batch(type_='confirm payout'),
    ],
    # BILLING-440 12. Наличие корректирующих событий
    [
        u.Client1.cashless | u.credit(1500),
    ],
    [
        u.Client1.compensations | u.credit(150),
    ],
    u.EventBatch(u.batch(ext_id='payout/1/0004'))(
        u.Client1.cashless | u.debit(1650) | u.info('Списано больше, чем нужно') | u.batch(count=2),
        u.Client1.cashless | u.debit(-1650) | u.info('Ручная корректировка, сторно'),
        u.Client1.cashless | u.debit(1500) | u.info('Ручная корректировка, правильная сумма'),
        u.Client1.compensations | u.debit(150) | u.info('Ручная корректировка'),
        u.Client1.payout | u.credit(1650) | u.batch(count=2),
    ),
    [
        u.Client1.payout | u.debit(1650),
        u.Client1.payout_sent | u.credit(1650),
    ],
    [  # Платёжка без ВЗ, к текущему кейсу не относится
        u.Client1.payout_sent | u.debit(0) | u.info(netting=['payout/1/9999'], oebs_payment_id=999) | u.batch(type_='register payout'),
    ],
    [
        u.Client1.payout_sent | u.debit(0) | u.info(netting=['payout/1/0004'], oebs_payment_id=4) | u.batch(type_='register payout'),
    ],
    [
        u.Client1.payout_sent | u.debit(1650) | u.info(netting=['payout/1/0004'], oebs_payment_id=4) | u.batch(type_='confirm payout'),
    ],

    # BILLING-440 4. взаимозачёт ещё не попавший в выплату, с остатком already_marked_amount в unmarked
    [
        u.Client1.cashless | u.credit(10),
    ],
    [
        u.Client1.commissions_with_vat | u.debit(100) | u.info('already_marked_amount=30, payout/1/1000 + payout/1/1001 + payout/1/1002'),
    ],
    u.EventBatch(u.batch(ext_id='payout/1/1000'))(
        u.Client1.cashless | u.debit(10),
        u.Client1.commissions_with_vat | u.credit(10),
        u.Client1.payout | u.credit(0),
    ),
    # BILLING-440 3.1 Взаимозачёт с остатками комиссии из предыдущего
    [
        u.Client1.cashless | u.credit(10),
    ],
    u.EventBatch(u.batch(ext_id='payout/1/1001'))(
        u.Client1.cashless | u.debit(10),
        u.Client1.commissions_with_vat | u.credit(10),
        u.Client1.payout | u.credit(0),
    ),
    # BILLING-440 3.2 Взаимозачёт с остатками комиссии с предыдущих 2-х взаимозачетов
    [
        u.Client1.cashless | u.credit(10),
    ],
    u.EventBatch(u.batch(ext_id='payout/1/1002'))(
        u.Client1.cashless | u.debit(10),
        u.Client1.commissions_with_vat | u.credit(10),
        u.Client1.payout | u.credit(0),
    ),
    # BILLING-440 5. события не попавшие во взаимозачёт
    [
        u.Client1.cashless | u.credit(123),
    ],
]


def make_input_table(data, yt_client, table_path, lbexport_formater):
    schema = [
        {'name': 'data', 'type': 'string'},
        {'name': 'already_marked_amount', 'type': 'string'},
        {'name': '_lb_foo', 'type': 'uint64'},
    ]
    yt_client.create('table', table_path, recursive=True, attributes={'schema': schema})
    event_id = 0
    batch_id = 0
    ready_data = []
    for _batch in data:
        batch_id += 10
        common_data = {
            'dt': 1627465336 + batch_id,
        } | u.batch(id_=batch_id, ext_id=batch_id, count=len(_batch))
        for _event in _batch:
            event_id += 10
            ready_data.append({
                'already_marked_amount': _event.pop('already_marked_amount', None),
                'data': lbexport_formater({'id': event_id, 'seq_id': event_id} | common_data | _event),
            })
    yt_client.write_table(
        table_path,
        ready_data,
    )


def test_mark_o_tron_run_yql(yql_client, yt_client, yt_root, lbexport_formater, ns_config):
    input_table_path = yt.ypath_join(yt_root, 'input_table')
    marked_path = yt.ypath_join(yt_root, 'marked')
    unmarked_path = yt.ypath_join(yt_root, 'unmarked')

    make_input_table(DATA, yt_client, input_table_path, lbexport_formater)

    with yt_client.Transaction() as transaction:
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
            prev_unmarked=input_table_path,
            output_marked=marked_path,
            output_unmarked=unmarked_path,
            transaction=transaction,
        )

    ret = {
        'marked': u.sort_yson(yt_client.read_table(marked_path, format="yson")),
        'unmarked': u.sort_json(yt_client.read_table(unmarked_path, format="json")),
    }

    return ret
