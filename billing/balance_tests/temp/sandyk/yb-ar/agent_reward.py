# -*- coding: utf-8 -*-

import xlrd

from balance import balance_db as db

tables = [
    'x_t_contract2',
    'x_t_contract_collateral',
    'x_mv_contract_signed_attr_hist',
    'x_t_invoice',
    'x_t_person',
    'x_t_extprops',
    'x_t_act_internal', 'x_t_act_trans',
    'x_t_consume',
    'x_t_order',
    'x_mv_ui_contract_apex',
    'x_mv_contract_signed_attr',
    'x_yt_ar_direct_domain_stats',
    'x_yt_fin_docs',
    'x_mv_oebs_receipts_2',
    'x_mv_currency_rate',
    'x_t_ar_invoice_reward',
    'x_t_ar_paid_periods',
]

currencies = [
    'RUR',
    'USD',
    'KZT',
    'BYR',
]

book = 'C:\\balance-tests-new\\temp\\sandyk\\yb-ar\\NewCommissionTypes_2019_test.xlsx'


def named(row):
    l = len(row)
    data = dict()

    for idx in xrange(1, l):

        d = idx // 26
        m = idx % 26

        if m == 0:
            d -= 1
            m = 26

        column = chr(64 + m)
        if d:
            column = chr(64 + d) + column

        data[column] = row[idx - 1]

    return data


def read_sheet(book, sheet):
    b = xlrd.open_workbook(book)
    current = b.sheet_by_name(sheet)
    case_contracts = dict()
    data = dict()
    for rownum in xrange(1, current.nrows):
        row = current.row_values(rownum)
        nm = row[0]
        if nm:
            if nm == 1000:
                const = [row]
            else:
                if nm in data:
                    data[nm].append(row)
                else:
                    data[nm] = [row]
                    case_contracts[nm] = [row[2]]
                #генерим мапинг кейс - список договоров
                if nm not in case_contracts.keys():
                    case_contracts[nm] = [row[2]]
                else:
                    if row[2] not in case_contracts[nm]:
                        case_contracts[nm].append(row[2])

    return data, case_contracts, const if const else None


def get_etalon_results(book, sheet):
    b = xlrd.open_workbook(book)
    current = b.sheet_by_name(sheet)
    data = dict()
    for rownum in xrange(1, current.nrows):
        header = current.row_values(0)
        row = current.row_values(rownum)
        named_row = dict()
        for i in xrange(0, len(header)):
            if header[i] in ('from_dt', 'till_dt'):
                from datetime import datetime
                row[i] = datetime(*xlrd.xldate_as_tuple(row[i], 0))
            else:
                if type(row[i]) is unicode:
                    row[i] = row[i].replace(",", ".")
                    row[i] = None if len(row[i]) == 0 else row[i]
            ##распределяем договоры по кейсам
            if header[i] == 'contract_id':
                case = int(str(int(row[i]))[-3:])
            named_row[header[i]] = row[i]
        if case in data:
            data[case].append(named_row.copy())
        else:
            data[case] = [named_row.copy()]
    return data


def insert_query(query, row):
    query = query.format(**row)
    print query
    db.meta().execute(query)


def clear_data_in_tables(tables=tables):
    for table in tables:
        db.meta().execute("delete from {}".format(table))
    pass


def insert_currencies(currencies=currencies):
    for currency in currencies:
        db.meta().execute(u"insert into x_mv_currency_rate (tmp_id, cc, rate, rate_dt) " \
                          u"select s_x_mv_currency_rate.NEXTVAL, cr.cc, cr.rate, dates.rate_dt from " \
                          u"((select '{currency}' as cc, 1 as rate from dual) cr cross join " \
                          u"(select distinct trunc(dt) as rate_dt from x_t_act_internal)dates)".format(
            currency=currency))


def get_actual_results(res_table_name):
    results = db.meta().execute(
        "select CONTRACT_ID,CONTRACT_EID,FROM_DT,TILL_DT,REWARD_TYPE,DISCOUNT_TYPE,NDS,CURRENCY,TURNOVER_TO_CHARGE,REWARD_TO_CHARGE,DELKREDERE_TO_CHARGE,DKV_TO_CHARGE,TURNOVER_TO_PAY_W_NDS,TURNOVER_TO_PAY,REWARD_TO_PAY,REWARD_TO_PAY_SRC,DELKREDERE_TO_PAY,DKV_TO_PAY"
        " from t_comm_{calc}_src where contract_id like '%00000%' order by "
        "CONTRACT_ID,CONTRACT_EID,FROM_DT,TILL_DT,REWARD_TYPE,DISCOUNT_TYPE,NDS,CURRENCY,TURNOVER_TO_CHARGE,REWARD_TO_CHARGE,DELKREDERE_TO_CHARGE,DKV_TO_CHARGE,TURNOVER_TO_PAY_W_NDS,"
        "TURNOVER_TO_PAY,REWARD_TO_PAY,REWARD_TO_PAY_SRC,DELKREDERE_TO_PAY,DKV_TO_PAY".format(calc=res_table_name))
    named_row = dict()
    data = dict()
    for contract_string in xrange(0, len(results)):
        for header in results[contract_string].keys():
            if header == 'contract_id':
                case = int(str(int(results[contract_string][header]))[-3:])
            named_row[header] = results[contract_string][header]
        if case in data:
            data[case].append(named_row.copy())
        else:
            data[case] = [named_row.copy()]
    return data

# для тестирования вставляем "посчитанные" данные сразу в таблицу
def insert_test_data():
    db.meta().execute("delete  from t_comm_belarus_src where contract_id like '%00000%'")
    db.meta().execute(
        "Insert into t_comm_belarus_src (CONTRACT_ID,CONTRACT_EID,FROM_DT,TILL_DT,NDS,CURRENCY,DISCOUNT_TYPE,REWARD_TYPE,TURNOVER_TO_CHARGE,REWARD_TO_CHARGE,DELKREDERE_TO_CHARGE,DKV_TO_CHARGE,TURNOVER_TO_PAY_W_NDS,TURNOVER_TO_PAY,REWARD_TO_PAY,DELKREDERE_TO_PAY,DKV_TO_PAY,INSERT_DT,REWARD_TO_PAY_SRC) "
        "values (1201000001,'1201000001/19',to_date('01.06.2019 00:00:00','DD.MM.YYYY HH24:MI:SS'),to_date('30.06.2019 23:59:59','DD.MM.YYYY HH24:MI:SS'),'1','RUR','1','301',16666.66666666667,2166.6666666666671,null,null,'0','0','0',null,null,to_date('15.07.2019 14:15:12','DD.MM.YYYY HH24:MI:SS'),null)")
    db.meta().execute(
        "Insert into t_comm_belarus_src (CONTRACT_ID,CONTRACT_EID,FROM_DT,TILL_DT,NDS,CURRENCY,DISCOUNT_TYPE,REWARD_TYPE,TURNOVER_TO_CHARGE,REWARD_TO_CHARGE,DELKREDERE_TO_CHARGE,DKV_TO_CHARGE,TURNOVER_TO_PAY_W_NDS,TURNOVER_TO_PAY,REWARD_TO_PAY,DELKREDERE_TO_PAY,DKV_TO_PAY,INSERT_DT,REWARD_TO_PAY_SRC)"
        " values (1201000001,'1201000001/19',to_date('01.06.2019 00:00:00','DD.MM.YYYY HH24:MI:SS'),to_date('30.06.2019 23:59:59','DD.MM.YYYY HH24:MI:SS'),'1','RUR','7','301',158333.33333333,7916.66666667,null,null,null,null,'0',null,null,to_date('15.07.2019 14:15:12','DD.MM.YYYY HH24:MI:SS'), 0)")
    db.meta().execute(
        "Insert into t_comm_belarus_src (CONTRACT_ID,CONTRACT_EID,FROM_DT,TILL_DT,NDS,CURRENCY,DISCOUNT_TYPE,REWARD_TYPE,TURNOVER_TO_CHARGE,REWARD_TO_CHARGE,DELKREDERE_TO_CHARGE,DKV_TO_CHARGE,TURNOVER_TO_PAY_W_NDS,TURNOVER_TO_PAY,REWARD_TO_PAY,DELKREDERE_TO_PAY,DKV_TO_PAY,INSERT_DT,REWARD_TO_PAY_SRC)"
        " values (1201000002,'1201000002/19',to_date('01.06.2019 00:00:00','DD.MM.YYYY HH24:MI:SS'),to_date('30.06.2019 23:59:59','DD.MM.YYYY HH24:MI:SS'),'1','RUR','7','301',234234,577,null,null,null,null,65,null,null,to_date('15.07.2019 14:15:12','DD.MM.YYYY HH24:MI:SS'), 2)")


def make_inserts(data, const):
    ## clear и insert_currencies надо будет перенести в пре- / пост- действия
    ## иначе сейчас делаем это для каждого кейса, должны единожды для всех
    clear_data_in_tables()
    for row in data:
        row = named(row)
        row['firm_id'] = int(const[0][4])
        row['scheme'] = const[0][6]

        if row['C']:
            insert_query(u"insert into x_t_contract2(TMP_ID,ID,EXTERNAL_ID,CLIENT_ID) " \
                         u"values(s_x_t_contract2.NEXTVAL, {C}, '{D}',{G})", row)
            insert_query(u"insert into x_t_contract_collateral(TMP_ID,ID,CONTRACT2_ID,DT,IS_SIGNED) " \
                         u"values(s_x_t_contract_collateral.NEXTVAL, {AL}, {C}, date'{E}', date'{E}')", row)
            insert_query(
                u"insert into x_mv_contract_signed_attr (TMP_ID,CONTRACT_ID,COLLATERAL_ID,CODE,VALUE_DT) " \
                u"values (s_x_mv_contract_signed_attr.NEXTVAL, {C}, {AL},'FINISH_DT', date'{F}')", row)
            insert_query(
                u"insert into x_mv_contract_signed_attr_hist(TMP_ID,VALUE_NUM,UPDATE_DT,CONTRACT2_ID,CODE,CL_DT,STAMP,DT) " \
                u"values(s_x_mv_contract_signed_attr_hist.NEXTVAL, {firm_id}, date'{E}', {C},'FIRM',date'{E}',{AS},date'{E}')",
                row)

            if row['scheme'] == 'opt':
                insert_query(
                    u"insert into x_mv_contract_signed_attr_hist(TMP_ID,VALUE_NUM,UPDATE_DT,CONTRACT2_ID,CODE,CL_DT,STAMP,DT) " \
                    u"values(s_x_mv_contract_signed_attr_hist.NEXTVAL, {H}, date'{E}', {C},'WHOLESALE_AGENT_PREMIUM_AWARDS_SCALE_TYPE',date'{E}',{AS},date'{E}')",
                    row)
                insert_query(
                    u"insert into x_mv_contract_signed_attr_hist(TMP_ID,VALUE_NUM,UPDATE_DT,CONTRACT2_ID,CODE,CL_DT,STAMP,DT) " \
                    u"values(s_x_mv_contract_signed_attr_hist.NEXTVAL, {I}, date'{E}', {C},'PAYMENT_TYPE',date'{E}',{AS},date'{E}')",
                    row)
            if row['scheme'] == 'comm':
                insert_query(
                    u"insert into x_mv_contract_signed_attr(TMP_ID,VALUE_NUM,CONTRACT_ID,CODE,DT,COLLATERAL_ID) " \
                    u"values(s_x_mv_contract_signed_attr.NEXTVAL, {H},{C},'COMMISSION_TYPE',date'{E}', {AL})",
                    row)
                insert_query(
                    u"insert into x_mv_contract_signed_attr(TMP_ID,VALUE_NUM,CONTRACT_ID,CODE,DT,COLLATERAL_ID)" \
                    u" values(s_x_mv_contract_signed_attr.NEXTVAL,  {I},{C},'PAYMENT_TYPE',date'{E}', {AL})",
                    row)
        if row['BG']:
            insert_query(
                u"insert into x_mv_contract_signed_attr_hist(TMP_ID,VALUE_NUM,UPDATE_DT,CONTRACT2_ID,CODE,CL_DT,STAMP,DT) " \
                u"values(s_x_mv_contract_signed_attr_hist.NEXTVAL, {BG}, date'{E}', {C},'AR_PAYMENT_CONTROL_TYPE',date'{E}',{AS},date'{E}')",
                row)
        if row['J'] and row['N'] != u'null':
            if row['C']:
                insert_query(
                    u"insert into x_t_invoice (TMP_ID,ID,DT, CLIENT_ID,EXTERNAL_ID,CONTRACT_ID,TOTAL_SUM,CURRENCY,DISCOUNT_TYPE,COMMISSION_TYPE,TYPE,LOYAL_CLIENTS,NDS,NDS_PCT,PERSON_ID) " \
                    u"values (s_x_t_invoice.NEXTVAL, {J}, date'{L}',{G},'{K}',{C},{N},'{O}',{R},{R},'{M}',{AD},{P},{Q},{AX})",
                    row)
            else:
                insert_query(
                    u"insert into x_t_invoice (TMP_ID,ID,DT, CLIENT_ID,EXTERNAL_ID,CONTRACT_ID,TOTAL_SUM,CURRENCY,DISCOUNT_TYPE,COMMISSION_TYPE,TYPE,LOYAL_CLIENTS,NDS,NDS_PCT,PERSON_ID) " \
                    u"values (s_x_t_invoice.NEXTVAL, {J}, date'{L}',{G},'{K}', null, {N},'{O}',{R},{R},'{M}',{AD},{P},{Q},{AX})",
                    row)
        if row['AX']:
            insert_query(u"insert into x_t_person (TMP_ID,ID) values (s_x_t_person.NEXTVAL, {AX})", row)

        if row['AQ']:
            insert_query(u"insert into x_t_extprops(TMP_ID, CLASSNAME,ATTRNAME,OBJECT_ID,VALUE_NUM) " \
                         u"values (s_x_t_extprops.NEXTVAL, 'PersonalAccount', 'subclient_id',{AR} , {AE})", row)
            insert_query(u"insert into x_t_invoice_repayment(TMP_ID, INVOICE_ID,REPAYMENT_INVOICE_ID) " \
                         u"values (s_x_t_invoice_repayment.NEXTVAL,{AR} , {J})", row)
        if row['T']:
            if row['X']:
                insert_query(
                    u"insert into x_t_act_internal(TMP_ID,ID,DT,CLIENT_ID,INVOICE_ID,HIDDEN,AMOUNT,EXTERNAL_ID,PAYMENT_TERM_DT,IS_LOYAL,TYPE) " \
                    u"values (s_x_t_act_internal.NEXTVAL, {T},date'{W}',{G}, {J}    ,{V},  {AC}  ,'{U}', date'{X}', {AD},'generic')",
                    row)
            else:
                insert_query(
                    u"insert into x_t_act_internal(TMP_ID,ID,DT,CLIENT_ID,INVOICE_ID,HIDDEN,AMOUNT,EXTERNAL_ID,PAYMENT_TERM_DT,IS_LOYAL,TYPE) " \
                    u"values (s_x_t_act_internal.NEXTVAL, {T},date'{W}',{G}, {J}    ,{V},  {AC}  ,'{U}', null, {AD},'generic')",
                    row)
            if row['Y'] != u'null':
                insert_query(
                    u"insert into x_t_act_trans(TMP_ID, ACT_ID, CONSUME_ID, AMOUNT, AMOUNT_NDS, AMOUNT_NSP, NETTING, COMMISSION_TYPE) " \
                    u"values (s_x_t_act_trans.NEXTVAL, {T},  {AT}, {Y}, {Z},  {AA},{AB}, {S})", row)
        if row['AT']:
            insert_query(
                u"insert into x_t_consume (TMP_ID, ID, PARENT_ORDER_ID) values (s_x_t_consume.NEXTVAL, {AT}, {AU})",
                row)
        if row['AU']:
            insert_query(u"insert into x_t_order(TMP_ID, ID, SERVICE_ID, SERVICE_ORDER_ID, CLIENT_ID)" \
                         u" values (s_x_t_order.NEXTVAL, {AU},  {AV},  {AW},  {AE} )", row)
        if row['AI']:
            insert_query(
                u"insert into x_mv_oebs_receipts_2(TMP_ID, INVOICE_EID, SUM, DT, DOC_DATE, COMISS_DATE, PAYMENT_NUMBER, SOURCE_TYPE)" \
                u" values (s_x_mv_oebs_receipts_2.NEXTVAL, '{K}', {AI}, date'{AJ}',date'{AJ}', null, '{AK}' ,'PAYCASH')",
                row)
        if row['AP'] != u'null':

            if row['AM']:
                insert_query(
                    u"insert into x_mv_contract_signed_attr (TMP_ID,CONTRACT_ID,COLLATERAL_ID,CODE,KEY_NUM,COLLATERAL_DT,VALUE_NUM)" \
                    u"values (s_x_mv_contract_signed_attr.NEXTVAL, {C}, {AL},'BRAND_CLIENTS' , {AE}, date'{AM}', {AO})",
                    row)
                if row['AN']:
                    insert_query(
                        u"insert into x_mv_ui_contract_apex (TMP_ID,CONTRACT_ID,CONTRACT_EID,DT,FINISH_DT,CLIENT_ID,AGENCY_ID) " \
                        u"values (s_x_mv_ui_contract_apex.NEXTVAL, {C}, '{D}',date'{AM}',  date'{AN}',{AP},{G} )",
                        row)
                else:
                    insert_query(
                        u"insert into x_mv_ui_contract_apex (TMP_ID,CONTRACT_ID,CONTRACT_EID,DT,FINISH_DT,CLIENT_ID,AGENCY_ID) " \
                        u"values (s_x_mv_ui_contract_apex.NEXTVAL, {C}, '{D}',date'{AM}',  null,{AP},{G} )",
                        row)
            else:
                insert_query(
                    u"insert into x_mv_contract_signed_attr (TMP_ID,CONTRACT_ID,COLLATERAL_ID,CODE,KEY_NUM,COLLATERAL_DT,VALUE_NUM)" \
                    u"values (s_x_mv_contract_signed_attr.NEXTVAL, {C}, {AL},'BRAND_CLIENTS' , {AE}, null, {AO})",
                    row)
                if row['AN']:
                    insert_query(
                        u"insert into x_mv_ui_contract_apex (TMP_ID,CONTRACT_ID,CONTRACT_EID,DT,FINISH_DT,CLIENT_ID,AGENCY_ID) " \
                        u"values (s_x_mv_ui_contract_apex.NEXTVAL, {C}, '{D}', null,  date'{AN}',{AP},{G} )",
                        row)
                else:
                    insert_query(
                        u"insert into x_mv_ui_contract_apex (TMP_ID,CONTRACT_ID,CONTRACT_EID,DT,FINISH_DT,CLIENT_ID,AGENCY_ID) " \
                        u"values (s_x_mv_ui_contract_apex.NEXTVAL, {C}, '{D}', null,  null,{AP},{G} )", row)

        if row['BA']:
            insert_query(
                u"insert into x_yt_ar_direct_domain_stats (ID,IS_GRAY,DOMAIN,IS_BLACKLIST,SERVICE_ID,COST,BILLING_EXPORT_ID,FROM_DT,TILL_DT,SERVICE_ORDER_ID) " \
                u"values(s_x_yt_ar_direct_domain_stats.NEXTVAL, {AY}, '{BD}', {AZ},{AV},{BC},{AW},  date'{BA}',to_date('{BB}'||' '||'23:59:59','YYYY-MM-DD HH24:MI:SS'),{AW})",
                row)
        if row['BE']:
            insert_query(u"insert into x_yt_fin_docs (ID, CONTRACT_EID, AGENCY_ID, FROM_DT, RECEIVE_DT)" \
                         u" values (s_yt_x_fin_docs.NEXTVAL, '{D}', {G}, date'{BE}', date'{BF}')", row)
        if row['BH']:
            if row['BI']:
                insert_query(
                    u"insert into x_mv_contract_signed_attr_hist(TMP_ID,VALUE_DT,CODE,COLLATERAL_ID,CL_DT,CONTRACT2_ID) " \
                    u"values(s_x_mv_contract_signed_attr_hist.NEXTVAL, date'{AN}', 'CONSOLIDATION_FINISH_DT', {AL},date'{AM}',{C})",
                    row)
            insert_query(
                u"insert into x_mv_contract_signed_attr_hist(TMP_ID,VALUE_NUM,CODE,COLLATERAL_ID,CL_DT,CONTRACT2_ID,KEY_NUM) " \
                u"values(s_x_mv_contract_signed_attr_hist.NEXTVAL, {AO}, 'LINKED_REWARD_CONTRACTS', {AL},date'{AM}',{C},{BH})",
                row)
        if row['BI']:
            insert_query(
                u"insert into x_mv_contract_signed_attr_hist(TMP_ID,VALUE_NUM,CL_DT,CODE,COLLATERAL_ID,CONTRACT2_ID) " \
                u"values(s_x_mv_contract_signed_attr_hist.NEXTVAL,{BI}, date'{AM}','REWARD_CONSOLIDATION_PERIOD', {AL},{C})",
                row)
        if row['BJ']:
            insert_query(
                u"insert into x_mv_contract_signed_attr_hist(TMP_ID,VALUE_NUM,CL_DT,CODE,COLLATERAL_ID,CONTRACT2_ID) " \
                u"values(s_x_mv_contract_signed_attr_hist.NEXTVAL,{BJ}, date'{AM}','REWARD_CONSOLIDATION_TYPE', {AL},{C})",
                row)

    insert_currencies()
