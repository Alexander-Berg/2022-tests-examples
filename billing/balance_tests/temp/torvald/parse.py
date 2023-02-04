# -*- coding: utf-8 -*-

import xlrd

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

        data[column] = row[idx-1]

    return data

book = 'C:\\torvald\_TEST_TOOLS\\balance-tests\\temp\\torvald\NewCommissionTypes_2019.xlsx'
sheet = 'prof_2019(month_other)_KO_inv'

b = xlrd.open_workbook(book)
current = b.sheet_by_name(sheet)

data = dict()
for rownum in range(current.nrows):
    row = current.row_values(rownum)
    nm = row[0]

    if nm in data:
        data[nm].append(row)
    else:
        data[nm] = [row]


row = named(row)

query = u"insert into xxxx_new_comm_contract_basic (ID,TEST_CASE_ID,CONTRACT_ID,CONTRACT_EID,CONTRACT_FROM_DT,CONTRACT_TILL_DT,CONTRACT_COMMISSION_TYPE,INVOICE_ID,INVOICE_EID,INVOICE_DT,PAYMENT_TYPE,COMMISSION_TYPE,DISCOUNT_TYPE,CURRENCY,NDS,NDS_PCT,INVOICE_FIRST_PAYMENT,INVOICE_FIRST_ACT,ACT_ID,HIDDEN,ACT_DT,AMOUNT,AMOUNT_NDS,AMOUNT_NSP,CLIENT_ID,LOYAL_CLIENT,cfo,COMMISSION_PAYBACK_PCT,IS_LOYAL,ACT_PAYMENT_TERM_DT,AGENCY_ID,ACT_EID) " \
        u"values (s_xxxx_new_comm_contract_basic.NEXTVAL, {A}, {C}, '{D}', date'{E}', date'{F}', {G}, {H}, '{I}',date'{O}',{J}, {K}, {K}, '{L}', {M}, {N}, date'{P}', date'{O}', {R}, {S}, to_date('{T}','YYYY-MM-DD HH24:MI:SS'), {U}, {W}, {X}, {Y}, {AM}, '{AO}', {AH}, {AN}, to_date('{AV}','YYYY-MM-DD HH24:MI:SS'), {BO}, '{BE}')"
       # "values (s_xxxx_new_comm_contract_basic.NEXTVAL, "&A3&", "&C3&", '"&D3&"', date'"&E3&"', date'"&F3&"', "&G3&", "&H3&", '"&I3&"',date'"&O3&"',"&J3&", "&K3&", "&K3&", '"&L3&"', "&M3&", "&N3&", date'"&P3&"', date'"&Q3&"', "&R3&", "&S3&", to_date('"&T3&"','YYYY-MM-DD HH24:MI:SS'), "&U3&", "&W3&", "&X3&", "&Y3&", "&AM3&", '"&AO3&"', "&AH3&","&AN3&",to_date('"&AV3&"','YYYY-MM-DD HH24:MI:SS'), "&BO3&",'"&BE3&"')"
print query.format(**row)

if row['AZ'] == 1:
    query = u"insert into xxxx_extprops (ID, CLASSNAME,ATTRNAME,OBJECT_ID,VALUE_NUM) values (s_xxxx_extprops .NEXTVAL, 'PersonalAccount', 'subclient_id', {AY} , {AU})"
    # "insert into xxxx_extprops (ID, CLASSNAME,ATTRNAME,OBJECT_ID,VALUE_NUM) values (s_xxxx_extprops .NEXTVAL, 'PersonalAccount', 'subclient_id'," & AY3 & " , " & AU3 & ");"
    print query.format(**row)

if row['BE']:
    query = u"insert into xxxx_acts (ID,ACT_ID,CONTRACT_ID,INVOICE_ID,DT,HIDDEN,CLIENT_ID) values (s_xxxx_acts.NEXTVAL, '{R}', {C}, {H}, to_date('{T}','YYYY-MM-DD HH24:MI:SS'),'{S}', {BO})"
    steps.CommonSteps
    
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
