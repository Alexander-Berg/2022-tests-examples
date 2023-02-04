# -*- coding: utf-8 -*-

import pprint

from temp.MTestlib import MTestlib as mtl


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


rpc = mtl.rpc
test_rpc = mtl.test_rpc
commission_pct = 0.01


def main(trust_payment_ids):
    payment_list = []
    for item in trust_payment_ids:
        sql1 = 'select p.id, p.amount, vpt.trust_payment_id from t_payment p join v_payment_trust vpt on p.id = vpt.id and vpt.trust_payment_id in (:trust_payment_ids)'
        sql1_params = {'trust_payment_ids': item}
        a = test_rpc.ExecuteSQL(sql1, sql1_params)
        payment_list.append(test_rpc.ExecuteSQL(sql1, sql1_params)[0])
    register_amount = 0
    register_commission = 0
    for item in payment_list:
        register_amount += float(item['amount'])
        item['commission'] = round(float(item['amount']) * commission_pct, 2)
        register_commission += item['commission']
    ##    payment_dict = map(lambda x: dict(zip(('payment_id', 'amount', 'trust_payment_id', 'commission'),x)), payment_list)
    print('DONE: list')

    sql2 = '''Insert into t_payment_register (ID,DT,PAYSYS_CODE,REGISTER_DT,AMOUNT,COMMISSION,FILE_NAME,INCOMING_MAIL_ID) values (s_payment_register_id.nextval,sysdate,'TRUST',trunc(sysdate),:register_amount,:commission,null,null)'''
    sql2_params = {'register_amount': register_amount, 'commission': register_commission}

    status = test_rpc.ExecuteSQL(sql2, sql2_params)
    # проверка status
    test_rpc.ExecuteSQL('commit')
    print('DONE: register')

    register_id = \
        test_rpc.ExecuteSQL('balance', 'select * from (select * from t_payment_register order by dt desc) where rownum = 1')[0][
            'id']

    for payment in payment_list:
        sql3 = "Insert into t_payment_register_line (ID,REGISTER_ID,PAYSYS_CODE,PAYMENT_DT,AMOUNT,COMMISSION) values (s_payment_register_line_id.nextval,:register_id,'TRUST',sysdate,:amount,:commission)"
        sql3_params = {'register_id': register_id, 'amount': payment['amount'], 'commission': payment['commission']}
        status = test_rpc.ExecuteSQL(sql3, sql3_params)
    test_rpc.ExecuteSQL('commit')
    print('DONE: lines')

    sql4 = "select id, amount from t_payment_register_line where register_id = :register_id"
    sql4_params = {'register_id': register_id}
    register_line_list = test_rpc.ExecuteSQL(sql4, sql4_params)
    print (register_line_list)

    for item in payment_list:
        for line in register_line_list:
            if item['amount'] == line['amount']:
                item['line_id'] = line['id']
                register_line_list.remove(line)
                break

    # debug
    print payment_list

    for item in payment_list:
        sql6 = "update t_payment set register_id = :register_id, register_line_id = :register_line_id where id = (select id from t_ccard_bound_payment where trust_payment_id = :trust_payment_id)"
        sql6_params = (
            {'register_id': register_id, 'register_line_id': item['line_id'],
             'trust_payment_id': item['trust_payment_id']})
        test_rpc.ExecuteSQL(sql6, sql6_params)
    test_rpc.ExecuteSQL('commit')
    print('DONE: payments')

    print register_id


if __name__ == '__main__':
    trust_payment_ids = [
        '558a764f795be2710b60adf7',
        '558a9862795be270f660ad2b',
        '558a9dbb795be2710b60aed6',
        '558ac12f795be270f660ae04'
    ]
    main(trust_payment_ids)
