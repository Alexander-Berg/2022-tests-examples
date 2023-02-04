from balance import balance_steps as steps
import balance.balance_db as db
from btestlib.utils import XmlRpc, Date, TestsError
import collections
from datetime import datetime
from dateutil.relativedelta import relativedelta
from btestlib.constants import NdsNew

from btestlib.data.partner_contexts import BLUE_MARKET_PAYMENTS,BLUE_MARKET_SUBSIDY

CONTRACT_START_DT = Date.first_day_of_month(datetime.now() - relativedelta(months=2))


ExportClass = collections.namedtuple('ExportClass', ('oebs_kwarg', 'classname'))
export_class = {
    'partner_id': ExportClass('client_id', 'Client'),
    'person_id': ExportClass('person_id', 'Person'),
    'contract_id': ExportClass('contract_id', 'Contract'),
    'id': ExportClass('transaction_id', 'ThirdPartyTransaction')
}


def detailed(row):
    return 'tpt_id=`{}`, trust_payment_id=`{}`, contract=`{}`, person=`{}`, client=`{}`, service=`{}`, paysys_type_cc=`{}`, payment_type=`{}`, amount=`{}`, internal=`{}`'.format(
        row['id'], row['trust_id'], row['contract_id'], row['person_id'], row['partner_id'], row['service_id'],
        row['paysys_type_cc'], row['payment_type'], row['amount'], row['internal']
    )


def get_export_error_rows(classname, object_id):
    return db.balance().execute(
        """
        select *
        from BO.T_EXPORT
        where CLASSNAME=:class 
          and OBJECT_ID=:object_id 
          and type like '%OEBS%'
          and state != 1
        """,
        {'class': classname, 'object_id': object_id}
    )


def export_payments(payments):
    #payments = [5674883236, 5674883277, 5674886873, 5674887012]
    # payments = [5674886873]
    #payments = [5674887012]

    rows = db.balance().execute(
        """
        select * 
        from T_THIRDPARTY_TRANSACTIONS 
        where PAYMENT_ID in ({})
        """.format(', '.join(str(n) for n in payments))
    )
    errors = []

    for row in rows:
        try:
            steps.ExportSteps.export_oebs(
                **{
                    tup.oebs_kwarg: row[key]
                    for key, tup in export_class.items()
                }
            )
        except (XmlRpc.XmlRpcError, TestsError) as e:
            errors.append(row)

    for row in rows:
        if row in errors:
            print '- ERROR: {}'.format(detailed(row))
            for key, tup in export_class.items():
                for export in get_export_error_rows(tup.classname, row[key]):
                    print u'   {} {}: {}'.format(
                        export['classname'],
                        export['object_id'],
                        export['error']
                    )
        else:
            print '- OK: {}'.format(detailed(row))


def get_payment(**kwargs):
    query = '''
    select *
    from t_payment
    where {}'''.format(
        ' and '.join(
            "{}='{}'".format(k, v)
            for k, v in kwargs.items()))
    return db.balance().execute(query)[0]


def main():
    # token = input('purchase_token please: ')
    token = '873131b2c374aaded282511bc16e0346'
    # token = '5bdedab8e27eef7440692094c16ba953'
    p = get_payment(purchase_token=token)
    steps.CommonPartnerSteps.export_payment(p['id'])
    export_payments([p['id']])


if __name__ == '__main__':
    # steps.ContractSteps.create_partner_contract(BLUE_MARKET_PAYMENTS,
    #                                             client_id=1349796050,
    #                                             person_id=16034893,
    #                                             is_offer=1,
    #                                             additional_params={
    #                                                 'start_dt': CONTRACT_START_DT
    #                                             })

    # _, spendable_person_id, spendable_contract_id, spendable_contract_eid = \
    #     steps.ContractSteps.create_partner_contract(BLUE_MARKET_SUBSIDY, client_id=1349796050, is_offer=1,
    #                                                 additional_params={'nds': NdsNew.ZERO.nds_id})
    #main()

    #steps.CommonPartnerSteps.export_payment(5675842001)

    export_payments([5675841987, 5675842001])

    # steps.ExportSteps.export_oebs(
    #     #client_id=39989801,
    #     #person_id=5731570,
    #     contract_id=391296,
    #     transaction_id=60097562381149
    # )
    # steps.ExportSteps.export_oebs(
    #     transaction_id=60097562381159
    # )
