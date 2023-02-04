# -*- coding: utf-8 -*-

import xlrd

from balance import balance_db as db


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

    data = dict()
    for rownum in xrange(1, current.nrows):
        row = current.row_values(rownum)
        nm = row[0]
        if nm:
            if nm in data:
                data[nm].append(row)
            else:
                data[nm] = [row]

    return data


# Количество отступов сверху таблицы = 1
# Убрать пропуски строк

book = 'C:\\torvald\_TEST_TOOLS\\balance-tests\\temp\\torvald\NewCommissionTypes_2019.xlsx'


def prof_2019_month_other_KO_inv(data):
    for test_case in data:
        for row in data[test_case]:

            row = named(row)
            print row

            query = u"insert into xxxx_new_comm_contract_basic (ID,TEST_CASE_ID,CONTRACT_ID,CONTRACT_EID,CONTRACT_FROM_DT,CONTRACT_TILL_DT,CONTRACT_COMMISSION_TYPE,INVOICE_ID,INVOICE_EID,INVOICE_DT,PAYMENT_TYPE,COMMISSION_TYPE,DISCOUNT_TYPE,CURRENCY,NDS,NDS_PCT,INVOICE_FIRST_PAYMENT,INVOICE_FIRST_ACT,ACT_ID,HIDDEN,ACT_DT,AMOUNT,AMOUNT_NDS,AMOUNT_NSP,CLIENT_ID,LOYAL_CLIENT,cfo,COMMISSION_PAYBACK_PCT,IS_LOYAL,ACT_PAYMENT_TERM_DT,AGENCY_ID,ACT_EID) " \
                    u"values (s_xxxx_new_comm_contract_basic.NEXTVAL, {A}, {C}, '{D}', date'{E}', date'{F}', {G}, {H}, '{I}',date'{O}',{J}, {K}, {K}, '{L}', {M}, {N}, date'{P}', date'{O}', {R}, {S}, to_date('{T}','YYYY-MM-DD HH24:MI:SS'), {U}, {W}, {X}, {Y}, {AM}, '{AO}', {AH}, {AN}, to_date('{AV}','YYYY-MM-DD HH24:MI:SS'), {BO}, '{BE}')"
            # "values (s_xxxx_new_comm_contract_basic.NEXTVAL, "&A3&", "&C3&", '"&D3&"', date'"&E3&"', date'"&F3&"', "&G3&", "&H3&", '"&I3&"',date'"&O3&"',"&J3&", "&K3&", "&K3&", '"&L3&"', "&M3&", "&N3&", date'"&P3&"', date'"&Q3&"', "&R3&", "&S3&", to_date('"&T3&"','YYYY-MM-DD HH24:MI:SS'), "&U3&", "&W3&", "&X3&", "&Y3&", "&AM3&", '"&AO3&"', "&AH3&","&AN3&",to_date('"&AV3&"','YYYY-MM-DD HH24:MI:SS'), "&BO3&",'"&BE3&"')"
            query = query.format(**row)
            print query
            db.meta().execute(query)

            if row['AZ'] == 1:
                query = u"insert into xxxx_extprops (ID, CLASSNAME,ATTRNAME,OBJECT_ID,VALUE_NUM) values (s_xxxx_extprops .NEXTVAL, 'PersonalAccount', 'subclient_id', {AY} , {AU})"
                # "insert into xxxx_extprops (ID, CLASSNAME,ATTRNAME,OBJECT_ID,VALUE_NUM) values (s_xxxx_extprops .NEXTVAL, 'PersonalAccount', 'subclient_id'," & AY3 & " , " & AU3 & ");"
                query = query.format(l**row)
                print query
                db.meta().execute(query)

            # if row['BE']:
            # query = u"insert into xxxx_acts (ID,ACT_ID,CONTRACT_ID,INVOICE_ID,DT,HIDDEN,CLIENT_ID) values (s_xxxx_acts.NEXTVAL, '{R}', {C}, {H}, to_date('{T}','YYYY-MM-DD HH24:MI:SS'),'{S}', {BO})"
            # # ЕСЛИ(ДЛСТР(BE3)>0; "insert into xxxx_acts (ID,ACT_ID,CONTRACT_ID,INVOICE_ID,DT,HIDDEN,CLIENT_ID) values(s_xxxx_acts.NEXTVAL, '"&R3&"', "&C3&", "&H3&", to_date('"&T3&"','YYYY-MM-DD HH24:MI:SS'),'"&S3&"',"&BO3&");";"")&
            # query = query.format(**row)
            # print query
            # db.balance().execute(query)

            # ЕСЛИ(AZ3=1;"insert into xxxx_extprops (ID, CLASSNAME,ATTRNAME,OBJECT_ID,VALUE_NUM) values (s_xxxx_extprops .NEXTVAL, 'PersonalAccount', 'subclient_id',"&AY3&" , "&AU3&");";"")&
            # ЕСЛИ(ДЛСТР(BE3)>0; "insert into xxxx_acts (ID,ACT_ID,CONTRACT_ID,INVOICE_ID,DT,HIDDEN,CLIENT_ID) values(s_xxxx_acts.NEXTVAL, '"&R3&"', "&C3&", "&H3&", to_date('"&T3&"','YYYY-MM-DD HH24:MI:SS'),'"&S3&"',"&BO3&");";"")&
            # ЕСЛИ(ДЛСТР(BN3)>0; "insert into xxxx_ar_direct_domain_stats (ID,IS_GRAY,DOMAIN,IS_BLACKLIST,SERVICE_ID,COST,BILLING_EXPORT_ID,FROM_DT,TILL_DT,SERVICE_ORDER_ID) values(s_xxxx_ar_direct_domain_stats.NEXTVAL, "&BI3&", '"&BN3&"', "&BJ3&","&BG3&","&BM3&","&BH3&", to_date('"&BK3&"','YYYY-MM-DD HH24:MI:SS'), to_date('"&BL3&"','YYYY-MM-DD HH24:MI:SS'),"&BH3&");";"")&
            # ЕСЛИ(ДЛСТР(BE3)>0;"insert into xxxx_act_trans (ID, ACT_ID, COMMISSION_TYPE, AMOUNT, AMOUNT_NDS,AMOUNT_NSP,PARENT_ORDER_ID) values (s_xxxx_act_trans.NEXTVAL, "&R3&",  "&K3&", "&U3&", "&W3&",      "&X3&","&BF3&" );";"")&ЕСЛИ(((ДЛСТР(BF3)>0)*(BF3<>BF2))=1;"insert into xxxx_order(ID, ORDER_ID, CLIENT_ID,SERVICE_ID,SERVICE_ORDER_ID) values (s_xxxx_order.NEXTVAL, "&BF3&",  "&Y3&",  "&BG3&",  "&BH3&" );";"")&
            # ЕСЛИ(ДЛСТР(AD3)>0;"insert into xxxx_oebs_cash_payment_test (ID, INVOICE_ID, OEBS_PAYMENT, DOC_DATE, COMISS_DATE) values (s_xxxx_oebs_cash_payment_test.NEXTVAL, "&H3&", "&AD3&", to_date('"&T3&"','YYYY-MM-DD HH24:MI:SS'), "&AG3&" );";"")&
            # ЕСЛИ(ДЛСТР(AE3)>0;"insert into xxxx_oebs_cash_payment_test (ID, INVOICE_ID, OEBS_PAYMENT, DOC_DATE, COMISS_DATE) values (s_xxxx_oebs_cash_payment_test.NEXTVAL, "&H3&", "&AE3&", to_date('"&T3&"','YYYY-MM-DD HH24:MI:SS'), integer('"&AG3&"') );";"")&
            # ЕСЛИ(ДЛСТР(AF3)>0;"insert into xxxx_oebs_cash_payment_test (ID, INVOICE_ID, OEBS_PAYMENT, DOC_DATE, COMISS_DATE) values (s_xxxx_oebs_cash_payment_test.NEXTVAL, "&H3&", "&AF3&", to_date('"&T3&"','YYYY-MM-DD HH24:MI:SS'), "&AG3&" );";"")&
            # ЕСЛИ(ДЛСТР(AI3)>0;"insert into xxxx_client_discount_m  (ID,CLIENT_ID,CLIENT_AVG_DISCOUNT,DT) values (s_xxxx_client_discount_m.NEXTVAL,"&Y3&", "&AI3&", "&AJ3&");";"")&
            # ЕСЛИ(ДЛСТР(AK3)>0;"insert into xxxx_client_discount_m  (ID,CLIENT_ID,CLIENT_AVG_DISCOUNT,DT) values (s_xxxx_client_discount_m.NEXTVAL,"&Y3&", "&AK3&", "&AL3&");";"")&
            # ЕСЛИ(ДЛСТР(AP3)>0;"insert into xxxx_contract_signed_attr (ID, CONTRACT_ID, CODE, KEY_NUM) values (s_xxxx_contract_signed_attr.NEXTVAL, "&C3&", 'SUPERCOMMISSION_BONUS' , "&AP3&" )";"")&
            # ЕСЛИ(ДЛСТР(AY3)>0;"insert into xxxx_invoice_repayment (ID,INVOICE_ID,REPAYMENT_INVOICE_ID) values (s_xxxx_invoice_repayment.NEXTVAL, "&AY3&", "&H3&" );";"")&
            # ЕСЛИ(ДЛСТР(AW3)>0;"insert into xxxx_ui_contract_apex (ID,CONTRACT_ID,START_DT,FINISH_DT,MAIN_CLIENT_ID) values (s_xxxx_ui_contract_apex.NEXTVAL, "&AX3&", to_date('"&BB3&"','YYYY-MM-DD HH24:MI:SS'), to_date('"&AT3&"','YYYY-MM-DD HH24:MI:SS'),"&AW3&" );";"")&
            # ЕСЛИ(ДЛСТР(AW3)>0;"insert into xxxx_contract_signed_attr (ID, CONTRACT_ID, CODE, KEY_NUM,START_DT) values (s_xxxx_contract_signed_attr.NEXTVAL, "&AX3&", 'BRAND_CLIENTS' , "&Y3&", to_date('"&BB3&"','YYYY-MM-DD HH24:MI:SS') );";"")&
            # ЕСЛИ(ДЛСТР(BP3)>0;"insert into xxxx_contract_signed_attr (ID, CONTRACT_ID, COLLATERAL_ID, CODE, VALUE_NUM,START_DT) values (s_xxxx_contract_signed_attr.NEXTVAL, "&C3&", 1000+"&C3&", 'AR_PAYMENT_CONTROL_TYPE' , "&BQ3&", to_date('"&BB3&"','YYYY-MM-DD HH24:MI:SS') );";"")&
            # ЕСЛИ(ДЛСТР(AQ3)>0;"insert into xxxx_loyal_clients_contr_attr (ID, CONTRACT_ID, CLIENT_ID, LC_TURNOVER, COLLATERAL_FIRST_DT, COLLATERAL_END_DT) values (s_xxxx_loyal_clients_contr_atr.NEXTVAL, "&C3&", "&Y3&", "&AQ3&", "&AR3&", "&AS3&" );";"")&"insert into xxxx_invoice (ID, INV_ID,INVOICE_TYPE,TOTAL_SUM) values (s_xxxx_invoice.NEXTVAL, "&H3&", '"&BA3&"',"&V3&" );"&
            # ЕСЛИ(ДЛСТР(BC3)>0;"insert into xxxx_contract_signed_attr (ID,CONTRACT_ID,CODE,KEY_NUM,START_DT,STAMP) values (s_xxxx_contract_signed_attr.NEXTVAL, "&C3&", 'PAYMENT_TYPE', "&J3&", to_date('"&BB3&"','YYYY-MM-DD HH24:MI:SS'),"&BC3&");";"")


def get_results(data):
    results = dict()

    for test_case in data:
        amount = 0
        for row in data[test_case]:
            row = named(row)
            print row['U']

            amount += row['U'] if row['U'] != u'null' else 0

        results[test_case] = amount

    return results


# sheet = 'prof_2019(month_other)_KO_inv'
# data = read_sheet(book, sheet)
# prof_2019_month_other_KO_inv(data)

def base_2019_month(data):
    for test_case in data:
        for row in data[test_case]:
            row = named(row)

            query = u"insert into xxxx_new_comm_contract_basic (ID,TEST_CASE_ID,CONTRACT_ID,CONTRACT_EID,CONTRACT_FROM_DT,CONTRACT_TILL_DT,CONTRACT_COMMISSION_TYPE,INVOICE_ID,INVOICE_EID,INVOICE_DT,PAYMENT_TYPE,COMMISSION_TYPE,DISCOUNT_TYPE,CURRENCY,NDS,NDS_PCT,INVOICE_FIRST_PAYMENT,INVOICE_FIRST_ACT,ACT_ID,HIDDEN,ACT_DT,AMOUNT,AMOUNT_NDS,AMOUNT_NSP,CLIENT_ID,LOYAL_CLIENT,cfo,COMMISSION_PAYBACK_PCT,IS_LOYAL,ACT_PAYMENT_TERM_DT) " \
                    u"values (s_xxxx_new_comm_contract_basic.NEXTVAL, {A}, {C}, '{D}', date'{E}', date'{F}', {G}, {H}, '{I}',date'{O}',{J}, {K}, {K}, '{L}', {M}, {N}, date'{P}', date'{O}', {R}, {S}, to_date('{T}','YYYY-MM-DD HH24:MI:SS'), {U}, {W}, {X}, {Y}, {AM}, '{AO}', {AH}, {AN}, to_date('{AV}','YYYY-MM-DD HH24:MI:SS'))"
            # "values (s_xxxx_new_comm_contract_basic.NEXTVAL, "&A3&", "&C3&", '"&D3&"', date'"&E3&"', date'"&F3&"', "&G3&", "&H3&", '"&I3&"',date'"&O3&"',"&J3&", "&K3&", "&K3&", '"&L3&"', "&M3&", "&N3&", date'"&P3&"', date'"&Q3&"', "&R3&", "&S3&", to_date('"&T3&"','YYYY-MM-DD HH24:MI:SS'), "&U3&", "&W3&", "&X3&", "&Y3&", "&AM3&", '"&AO3&"', "&AH3&","&AN3&",to_date('"&AV3&"','YYYY-MM-DD HH24:MI:SS'))"
            query = query.format(**row)
            # print query
            db.meta().execute(query)

            # if row['AD']:
            # query = u"insert into xxxx_oebs_cash_payment_test (ID, INVOICE_ID, OEBS_PAYMENT, DOC_DATE, COMISS_DATE) values (s_xxxx_oebs_cash_payment_test.NEXTVAL, {H}, {AD}, to_date('{T}','YYYY-MM-DD HH24:MI:SS'), {AG})"
            # "insert into xxxx_oebs_cash_payment_test (ID, INVOICE_ID, OEBS_PAYMENT, DOC_DATE, COMISS_DATE) values (s_xxxx_oebs_cash_payment_test.NEXTVAL, " & H7 & ", " & AD7 & ", to_date('" & T7 & "','YYYY-MM-DD HH24:MI:SS'), " & AG7 & " );";

            # ="insert into xxxx_new_comm_contract_basic (ID,TEST_CASE_ID,CONTRACT_ID,CONTRACT_EID,CONTRACT_FROM_DT,CONTRACT_TILL_DT,CONTRACT_COMMISSION_TYPE,INVOICE_ID,INVOICE_EID,INVOICE_DT,PAYMENT_TYPE,COMMISSION_TYPE,DISCOUNT_TYPE,CURRENCY,NDS,"&"NDS_PCT,INVOICE_FIRST_PAYMENT,INVOICE_FIRST_ACT,ACT_ID,HIDDEN,ACT_DT,AMOUNT,AMOUNT_NDS,AMOUNT_NSP,CLIENT_ID,LOYAL_CLIENT,cfo,COMMISSION_PAYBACK_PCT,IS_LOYAL,ACT_PAYMENT_TERM_DT) values ("&
            # "s_xxxx_new_comm_contract_basic.NEXTVAL, "&A7&", "&C7&", '"&D7&"', date'"&E7&"', date'"&F7&"', "&G7&", "&H7&", '"&I7&"',date'"&O7&"',"&J7&", "&K7&", "&K7&", '"&L7&"', "&M7&", "&N7&", date'"&P7&"', date'"&Q7&"', "&R7&", "&S7&", to_date('"&T7&"','YYYY-MM-DD HH24:MI:SS'), "&U7&", "&W7&", "&X7&", "&Y7&", "&AM7&", '"&AO7&"', "&AH7&","&AN7&",to_date('"&AV7&"','YYYY-MM-DD HH24:MI:SS'));"&                                                                                                                                                                                                           ЕСЛИ(AZ7=1;"insert into xxxx_extprops (ID, CLASSNAME,ATTRNAME,OBJECT_ID,VALUE_NUM) values (s_xxxx_extprops .NEXTVAL, 'PersonalAccount', 'subclient_id',"&AY7&" , "&AU7&");";"")&
            # ЕСЛИ(ДЛСТР(AD7)>0;"insert into xxxx_oebs_cash_payment_test (ID, INVOICE_ID, OEBS_PAYMENT, DOC_DATE, COMISS_DATE) values (s_xxxx_oebs_cash_payment_test.NEXTVAL, "&H7&", "&AD7&", to_date('"&T7&"','YYYY-MM-DD HH24:MI:SS'), "&AG7&" );";"")&
            # ЕСЛИ(ДЛСТР(AE7)>0;"insert into xxxx_oebs_cash_payment_test (ID, INVOICE_ID, OEBS_PAYMENT, DOC_DATE, COMISS_DATE) values (s_xxxx_oebs_cash_payment_test.NEXTVAL, "&H7&", "&AE7&", to_date('"&T7&"','YYYY-MM-DD HH24:MI:SS'), integer('"&AG7&"') );";"")&
            # ЕСЛИ(ДЛСТР(AF7)>0;"insert into xxxx_oebs_cash_payment_test (ID, INVOICE_ID, OEBS_PAYMENT, DOC_DATE, COMISS_DATE) values (s_xxxx_oebs_cash_payment_test.NEXTVAL, "&H7&", "&AF7&", to_date('"&T7&"','YYYY-MM-DD HH24:MI:SS'), "&AG7&" );";"")&
            # ЕСЛИ(ДЛСТР(AI7)>0;"insert into xxxx_client_discount_m  (ID,CLIENT_ID,CLIENT_AVG_DISCOUNT,DT) values (s_xxxx_client_discount_m.NEXTVAL,"&Y7&", "&AI7&", "&AJ7&");";"")&
            # ЕСЛИ(ДЛСТР(AK7)>0;"insert into xxxx_client_discount_m  (ID,CLIENT_ID,CLIENT_AVG_DISCOUNT,DT) values (s_xxxx_client_discount_m.NEXTVAL,"&Y7&", "&AK7&", "&AL7&");";"")&
            # ЕСЛИ(ДЛСТР(AP7)>0;"insert into xxxx_contract_signed_attr (ID, CONTRACT_ID, CODE, KEY_NUM) values (s_xxxx_contract_signed_attr.NEXTVAL, "&C7&", 'SUPERCOMMISSION_BONUS' , "&AP7&" )";"")&                                                                                                                                                                                                         ЕСЛИ(ДЛСТР(AY7)>0;"insert into xxxx_invoice_repayment (ID,INVOICE_ID,REPAYMENT_INVOICE_ID) values (s_xxxx_invoice_repayment.NEXTVAL, "&AY7&", "&H7&" );";"")&                   ЕСЛИ(ДЛСТР(AW7)>0;"insert into xxxx_ui_contract_apex (ID,CONTRACT_ID,START_DT,FINISH_DT,MAIN_CLIENT_ID) values (s_xxxx_ui_contract_apex.NEXTVAL, "&AX7&", to_date('"&BB7&"','YYYY-MM-DD HH24:MI:SS'), to_date('"&AT7&"','YYYY-MM-DD HH24:MI:SS'),"&AW7&" );";"")&                                                                                                                                                                                                         ЕСЛИ(ДЛСТР(AW7)>0;"insert into xxxx_contract_signed_attr (ID, CONTRACT_ID, CODE, KEY_NUM,START_DT) values (s_xxxx_contract_signed_attr.NEXTVAL, "&AX7&", 'BRAND_CLIENTS' , "&Y7&", to_date('"&BB7&"','YYYY-MM-DD HH24:MI:SS') );";"")&
            # ЕСЛИ(ДЛСТР(AQ7)>0;"insert into xxxx_loyal_clients_contr_attr (ID, CONTRACT_ID, CLIENT_ID, LC_TURNOVER, COLLATERAL_FIRST_DT, COLLATERAL_END_DT) values (s_xxxx_loyal_clients_contr_atr.NEXTVAL, "&C7&", "&Y7&", "&AQ7&", "&AR7&", "&AS7&" );";"")&                                                                                                               "insert into xxxx_invoice (ID, INV_ID,INVOICE_TYPE,TOTAL_SUM) values (s_xxxx_invoice.NEXTVAL, "&H7&", '"&BA7&"',"&V7&" );"&                                                                                      ЕСЛИ(ДЛСТР(BC7)>0;"insert into xxxx_contract_signed_attr (ID,CONTRACT_ID,CODE,KEY_NUM,START_DT,STAMP) values (s_xxxx_contract_signed_attr.NEXTVAL, "&C7&", 'PAYMENT_TYPE', "&J7&", to_date('"&BB7&"','YYYY-MM-DD HH24:MI:SS'),"&BC7&");";"")

# sheet = 'base_2019(month)'
# data = read_sheet(book, sheet)
# prof_2019_month_other_KO_inv(data)
