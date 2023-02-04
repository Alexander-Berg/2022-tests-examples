# coding: utf-8

import os
import sys
import json

from yt import wrapper as yt

from balance import mapper
from balance.constants import NirvanaProcessingTaskState
from balance.application import Application
from balance.actions.process_completions.log_tariff import get_order_state
from butils.application.logger import get_logger


log = get_logger()


def update_task_meta(session, type_id, new_meta_path):
    task = session.query(mapper.LogTariffTask).\
        filter(mapper.LogTariffTask.state != NirvanaProcessingTaskState.ARCHIVED,
               mapper.LogTariffTask.type_id == type_id).\
        one()

    with open(new_meta_path) as fh:
        meta = json.load(fh)

    task.metadata = meta
    task.state = NirvanaProcessingTaskState.FINISHED
    session.flush()


def check_tablet_state(yt_client, node_path, expected_tablet_state):
    """
    После последней переналивки скрипт падал несколько раз по непонятным причинам из-за неправильного состояния
    таблетов. После расстановки проверок скрипт отработал, поэтому проверки я оставляю, на случай, если в
    следующий раз проблема проявится, и проверки помог лучше понять причину.
    """
    tablet_state = yt_client.get(node_path + '/@tablet_state')
    assert tablet_state == expected_tablet_state, (expected_tablet_state, tablet_state)


def dump_metadata(session, tariff_meta_json_path, acts_meta_json_path, **kw):
    with session.begin():
        update_task_meta(session, 'direct', tariff_meta_json_path)
        update_task_meta(session, 'direct_act', acts_meta_json_path)


def load_orders(session, cluster, **kw):
    yt_client = yt.YtClient('hahn')
    rows = []
    for row in yt_client.read_table('//home/balance/prod/log_tariff/shadow/income/bs/tariff/orders'):
        o = session.query(mapper.Order).getone(
            service_id=row['ServiceID'],
            service_order_id=row['EffectiveServiceOrderID']
        )
        row['state'] = get_order_state(o, 0)
        rows.append(row)

    orders_table_path = '//home/balance-test/test/log_tariff/shadow/income/bs/tariff/orders'

    yt_client = yt.YtClient(cluster)
    meta = yt_client.get(yt.ypath_join(str(orders_table_path), '@log_tariff_meta'))
    assert not meta['is_updating']

    rpc_config = yt.common.update(
        yt.config.config, {'backend': 'rpc'}
    )
    rpc_config["proxy"]["url"] = '{}.yt.yandex.net'.format(cluster)
    rpc_yt_client = yt.YtClient(config=rpc_config)

    check_tablet_state(yt_client, orders_table_path, 'frozen')
    yt_client.unfreeze_table(orders_table_path, sync=True)
    check_tablet_state(yt_client, orders_table_path, 'mounted')
    with rpc_yt_client.Transaction(type='tablet'):
        check_tablet_state(yt_client, orders_table_path, 'mounted')

        # Предварительно удаляем все строки из тестовой таблицы с заказами,
        # потому что их может не быть в новой базе
        current_rows = [
            {
                'ServiceID': row['ServiceID'],
                'EffectiveServiceOrderID': row['EffectiveServiceOrderID']
            }
            for row in yt_client.read_table(orders_table_path)
        ]
        rpc_yt_client.delete_rows(orders_table_path, current_rows)

        rpc_yt_client.insert_rows(orders_table_path, rows, update=True)
        check_tablet_state(yt_client, orders_table_path, 'mounted')
    yt_client.freeze_table(orders_table_path, sync=True)
    check_tablet_state(yt_client, orders_table_path, 'frozen')


def main(args):
    # В файл нужно записать данные из последнего инстанса соответствующего артефакта
    # из тестового реактора (там должнен быть артефакт от последнего цикла):
    # * tariff_meta_json_path: /billing/balance/log_tariff/core/tariff/tariffed_meta_hahn
    # * acts_meta_json_path: /billing/balance/log_tariff/core/acts/processed_acts_meta_hahn
    # TODO: научиться получать данные последнего инстанса артефакта
    func_name = args[0]

    funcs = {
        'dump_metadata': [dump_metadata],
        'load_orders': [load_orders],
        '-': [dump_metadata, load_orders],
    }

    (tariff_meta_json_path, acts_meta_json_path, cluster) = args[1:]

    assert 'YANDEX_XML_CONFIG' in os.environ, 'You must set YANDEX_XML_CONFIG env var'
    app = Application()
    session = app.new_session(database_id='balance')

    kwargs = {
        'session': session,
        'cluster': cluster,
        'tariff_meta_json_path': tariff_meta_json_path,
        'acts_meta_json_path': acts_meta_json_path,
    }

    for func in funcs[func_name]:
        func(**kwargs)


if __name__ == '__main__':
    main(sys.argv[1:])
