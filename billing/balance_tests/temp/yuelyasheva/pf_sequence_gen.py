
from balance import balance_db as db

stats = [[4564, 7, 'fictive_personal_account', 'USD', 7, 'sw_yt'],
[81, 7, 'fictive_personal_account', 'TRY', 110, 'sw_yt'],
[6, 7, 'fictive_personal_account', 'EUR', 35, 'sw_yt'],
[9, 7, 'fictive_personal_account', 'EUR', 70, 'sw_yt'],
[3, 7, 'fictive_personal_account', 'USD', 70, 'sw_yt'],
[1, 7, 'fictive_personal_account', 'USD', 70, 'sw_ur'],
[10, 7, 'fictive_personal_account', 'USD', 7, 'sw_ur'],
[10, 7, 'fictive_personal_account', 'EUR', 7, 'sw_ur'],
[54, 7, 'fictive_personal_account', 'CHF', 7, 'sw_ur'],
[150, 7, 'fictive_personal_account', 'TRY', 7, 'sw_yt'],
[12, 7, 'fictive_personal_account', 'TRY', 35, 'sw_yt'],
[1, 7, 'fictive_personal_account', 'EUR', 110, 'sw_yt'],
[2709, 7, 'fictive_personal_account', 'EUR', 7, 'sw_yt'],
[4, 16, 'fictive_personal_account', 'EUR', 35, 'sw_yt'],
[7, 7, 'fictive_personal_account', 'USD', 35, 'sw_yt']]

sql = ''
for i in range(len(stats)):
    sql += 'select {5}, ii.id, ii.firm_id, ii.type, ii.currency, oo.SERVICE_ID, pp.TYPE ' \
           'from bo.t_invoice ii ' \
           'join bo.t_consume cc on ii.id=cc.invoice_id ' \
           'join bo.t_order oo on cc.parent_order_id=oo.id ' \
           'join bo.t_person pp on ii.person_id=pp.id ' \
           'join bo.t_invoice_repayment rp on ii.id=rp.invoice_id ' \
           'where (ii.type = \'{0}\') ' \
           'and ii.firm_id = {1} ' \
           'and ii.currency = \'{2}\' ' \
           'and oo.SERVICE_ID = {3} ' \
           'and pp.type = \'{4}\' and rownum< 2 ' \
           'union \n'.format(stats[i][2],stats[i][1],stats[i][3],stats[i][4],stats[i][5],stats[i][0])
print sql

