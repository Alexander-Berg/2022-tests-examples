# -*- coding: utf-8 -*-

import balance.balance_db as db


def get_tarification_entity_id(product_id,
                               key_num_1=-1, key_num_2=-1, key_num_3=-1,
                               key_num_4=-1, key_num_5=-1, key_num_6=-1):

    u""" Возвращает entity_id для переданного продукта и ключа. Если не сущесвует - предварительно создаст его. """

    def select_entity_id():
        sql = ''' select id from bo.t_tarification_entity
                  where product_id=:product_id and
                      key_num_1=:key_num_1 and key_num_2=:key_num_2 and key_num_3=:key_num_3 and
                      key_num_4=:key_num_4 and key_num_5=:key_num_5 and key_num_6=:key_num_6 '''

        eid = db.balance().execute(sql, dict(product_id=product_id,
                                             key_num_1=key_num_1, key_num_2=key_num_2, key_num_3=key_num_3,
                                             key_num_4=key_num_4, key_num_5=key_num_5, key_num_6=key_num_6)
                                   )
        return eid[0]['id'] if len(eid) else None

    def create_entity():
        sql = ''' insert into bo.t_tarification_entity
                  (product_id, key_num_1, key_num_2, key_num_3, key_num_4, key_num_5, key_num_6)
                  values
                  (:product_id, :key_num_1, :key_num_2, :key_num_3, :key_num_4, :key_num_5, :key_num_6) '''
        db.balance().execute(sql, dict(product_id=product_id,
                                       key_num_1=key_num_1, key_num_2=key_num_2, key_num_3=key_num_3,
                                       key_num_4=key_num_4, key_num_5=key_num_5, key_num_6=key_num_6)
                             )
    eid = select_entity_id()
    if not eid:
        create_entity()
        eid = select_entity_id()
    return eid

