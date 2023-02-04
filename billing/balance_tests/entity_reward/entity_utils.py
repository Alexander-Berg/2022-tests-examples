# -*- coding: utf-8 -*-

from balance.queue_processor import process_object
from balance import mapper

import json


def run_calculator(contract, month_dt):

    Export, ContractMonth = mapper.Export, mapper.ContractMonth

    # Для дистрибуционных договоров ContractMonth создаётся при подписании для корректного учёта головы
    if contract.type == 'DISTRIBUTION':

        tasks = (contract.session
                 .query(Export)
                 .filter_by(type='ENTITY_CALC')
                 .filter(Export.state == 0)
                 .join(ContractMonth, Export.object_id == ContractMonth.id)
                 .filter(ContractMonth.contract_id == contract.id)
                 .filter(ContractMonth.month_dt <= month_dt)
                 ).all()

        for t in tasks:
            process_object(contract.session, 'ENTITY_CALC', 'ContractMonth', t.object_id)

    # Для расходного договора ContractMonth создаётся простановщиком, но в тесте он не применим
    # поэтому нужно создать вручную
    elif contract.type == 'SPENDABLE':
        cm = mapper.ContractMonth(contract_id=contract.id, month_dt=month_dt)
        contract.session.add(cm)
        contract.session.flush()
        process_object(contract.session, 'ENTITY_CALC', 'ContractMonth', cm.id)


def get_tarification_entity_id(session, product_id,
                               key_num_1=-1, key_num_2=-1, key_num_3=-1,
                               key_num_4=-1, key_num_5=-1, key_num_6=-1):
    def select_entity_id():
        sql = ''' select id from bo.t_tarification_entity
                  where product_id=:product_id and
                      key_num_1=:key_num_1 and key_num_2=:key_num_2 and key_num_3=:key_num_3 and
                      key_num_4=:key_num_4 and key_num_5=:key_num_5 and key_num_6=:key_num_6 '''
        eid = session.execute(sql, dict(product_id=product_id,
                                        key_num_1=key_num_1, key_num_2=key_num_2, key_num_3=key_num_3,
                                        key_num_4=key_num_4, key_num_5=key_num_5, key_num_6=key_num_6)
                              ).fetchone()
        return eid['id'] if eid else None

    def create_entity():
        sql = ''' insert into bo.t_tarification_entity
                  (product_id, key_num_1, key_num_2, key_num_3, key_num_4, key_num_5, key_num_6)
                  values
                  (:product_id, :key_num_1, :key_num_2, :key_num_3, :key_num_4, :key_num_5, :key_num_6) '''
        session.execute(sql, dict(product_id=product_id,
                                  key_num_1=key_num_1, key_num_2=key_num_2, key_num_3=key_num_3,
                                  key_num_4=key_num_4, key_num_5=key_num_5, key_num_6=key_num_6)
                        )
    eid = select_entity_id()
    if not eid:
        create_entity()
        eid = select_entity_id()
    return eid


def get_src_id(session, product_id):
    metadata = session.query(mapper.PageData).get(product_id).product_metadata
    return json.loads(metadata)['sources'][0]


def create_completion(session, dt, product_id, key_tuple, fact_tuple):

    key = {('key_num_' + str(i)): k for (i, k) in enumerate(key_tuple, start=1)}
    key['product_id'] = product_id

    entity_id = get_tarification_entity_id(session, **key)

    values = {('val_num_' + str(i)): k for (i, k) in enumerate(fact_tuple, start=1)}
    src_id = get_src_id(session, product_id)

    session.execute(''' insert into bo.t_entity_completion
                        (dt, product_id, entity_id, src_id, val_num_1, val_num_2, val_num_3, val_num_4)
                        values (:dt, :pid, :eid, :src_id, :val_num_1, :val_num_2, :val_num_3, :val_num_4) 
                    ''',
                    dict(dt=dt, pid=product_id, eid=entity_id, src_id=src_id, **values)
                    )
    session.flush()
