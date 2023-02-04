# -*- coding: utf-8 -*-

import xmlrpclib
import pprint
import datetime
import urlparse
import subprocess

from temp.MTestlib import MTestlib


def Print(obj):
    print pprint.pformat(obj).decode('unicode_escape')


TM_url = "http://greed-tm1f.yandex.ru:8002/xmlrpc"
TS_url = "http://greed-ts1f.yandex.ru:8002/xmlrpc"
TEST_url = 'http://xmlrpc.balance.greed-tm1f.yandex.ru:30702/xmlrpc'

XMLRPC_URL = TM_url
XMLRPC_URL_1 = TEST_url

tm = xmlrpclib.ServerProxy(XMLRPC_URL, allow_none=1, use_datetime=1)
test = xmlrpclib.ServerProxy(XMLRPC_URL_1, allow_none=1, use_datetime=1)
##------------------------------------------------------------------------------

uid = 'ulrich666'
##Заказ  / Реквест
##service_id = 7
##product_id = 1475
qty = 100
begin_dt = datetime.datetime.now()
request_dt = datetime.datetime.now()  ##не меняется
invoice_dt = datetime.datetime.now()
##paysys_id    = 1003
##Оплата счета
payment_dt = datetime.datetime(2014, 10, 2)
##Дата открутки
qty2 = 200
campaigns_dt = datetime.datetime(2014, 12, 29)
campaigns_dt2 = datetime.datetime(2014, 10, 14)
act_dt = datetime.datetime(2014, 10, 4)
migrate_dt = datetime.datetime(2014, 10, 5)


##------------------------------------------------------------------------------
def test_client():
    ## Агенство
    agency_id = MTestlib.create_client({'IS_AGENCY': 1, 'NAME': u'UL Агенство для КП с нерезидентами'})
    ## Привязка к UID
    MTestlib.link_client_uid(agency_id, uid)
    ## Плательщки
    person_id = MTestlib.create_person(agency_id, 'ur', {'phone': '234'})
    ## Договор
    invoice_owner = agency_id
    ttt = urlparse.parse_qsl('external-id=&num=&commission=1&print-form-type=0&brand-type=70&client-id=' + str(
        invoice_owner) + '&person-id=' + str(person_id) +
                             '&account-type=0&bank-details-id=21&manager-code=1139&manager-bo-code=&dt=2014-12-01T00%3A00%3A00&finish-dt=2015-12-31T00%3A00%3A00&payment-type=3&unilateral=1&services=1&services-7=7&services-11=11&services-70=70&memo=&atypical-conditions-checkpassed=1&calc-termination=&attorney-agency-id=&commission-charge-type=1&commission-payback-type=2&commission-payback-pct=8+%25&commission-type=48&supercommission-bonus=1&partner-commission-type=1&partner-commission-pct=&named-client-declared-sum=&commission-declared-sum=&supercommission=0&partner-commission-sum=&linked-contracts=1&limitlinked-contracts=&partner-min-commission-sum=&advance-payment-sum=&discard-nds=0&discount-policy-type=3&discount-fixed=12&declared-sum=&fixed-discount-pct=&belarus-budget-price=&ukr-budget=&budget-discount-pct=&federal-declared-budget=&kz-budget=&federal-annual-program-budget=&federal-budget=&kzt-budget=&belarus-budget=&year-product-discount=&year-planning-discount=&year-planning-discount-custom=&use-ua-cons-discount-checkpassed=1&consolidated-discount=&use-consolidated-discount-checkpassed=1&regional-budget=&use-regional-cons-discount-checkpassed=1&pda-budget=&autoru-budget=&contract-discount=&retro-discount=&discount-pct=&discount-findt=&credit-type=2&payment-term=25&payment-term-max=&calc-defermant=0&personal-account-checkpassed=1&lift-credit-on-payment-checkpassed=1&auto-credit-checkpassed=1&personal-account-fictive-checkpassed=1&repayment-on-consume-checkpassed=1&credit-currency-limit=810&limitcredit-currency-limit=&credit-limit=17&limitcredit-limit=&turnover-forecast=17&limitturnover-forecast=&credit-limit-single=500000&partner-credit-checkpassed=1&discount-commission=&pp-1137-checkpassed=1&non-resident-clients=&non-resident-clients-checkpassed=1&new-commissioner-report=&new-commissioner-report-checkpassed=1&service-min-cost=&test-period-duration=&commission-categories=%5B%5D&client-limits=%5B%5D&brand-clients=%5B%5D&loyal-clients=%5B%5D&discard-media-discount-checkpassed=1&is-booked-checkpassed=1&is-faxed-checkpassed=1&is-signed=&is-signed-checkpassed=1&is-signed-date=26+%D0%B4%D0%B5%D0%BA+2014+%D0%B3.&is-signed-dt=2014-12-26T00%3A00%3A00&deal-passport-checkpassed=1&sent-dt-checkpassed=1&is-suspended-checkpassed=1&button-submit=%D0%A1%D0%BE%D1%85%D1%80%D0%B0%D0%BD%D0%B8%D1%82%D1%8C&collateral-form=&id=',
                             True)
    print {k: v.decode('utf-8') for k, v in ttt}
    contract = tm.Balance.CreateContract(16571028, {k: v.decode('utf-8') for k, v in ttt})
    print 'Contract: ' + str(contract['EXTERNAL_ID']) + ' (' + str(contract['ID']) + ')'

    col = urlparse.parse_qsl('''col-new-num=01&col-new-collateral-type=1033&col-new-print-form-type=0&col-new-dt=2014-12-05T00%3A00%3A00&col-new-memo=&col-new-group02-grp-10-pp-1137-checkpassed=1&col-new-group02-grp-1000-commission-payback-pct=8+%25&col-new-group02-grp-1000-commission-type=48&
    col-new-group02-grp-1000-commission-declared-sum=&col-new-group02-grp-1000-supercommission=0&col-new-group02-grp-1014-supercommission-bonus=1&col-new-group02-grp-50-supercommission=0&col-new-group02-grp-80-finish-dt=2015-12-31T00%3A00%3A00&col-new-group02-grp-1022-fixed-market-discount-pct=&
    col-new-group02-grp-1021-commission-charge-type=1&col-new-group02-grp-1021-finish-dt=2015-12-31T00%3A00%3A00&col-new-group02-grp-1021-commission-payback-type=2&col-new-group02-grp-1021-commission-type=48&col-new-group02-grp-1021-supercommission-bonus=1&col-new-group02-grp-1021-supercommission=0&
    col-new-group02-grp-1021-commission-declared-sum=&col-new-group02-grp-1021-named-client-declared-sum=&col-new-group02-grp-1021-linked-contracts=1&limitcol-new-group02-grp-1021-linked-contracts=&col-new-group02-grp-1021-payment-type=3&col-new-group02-grp-1021-credit-type=2&col-new-group02-grp-1021-payment-term=25&
    col-new-group02-grp-1021-payment-term-max=&col-new-group02-grp-1021-credit-limit=17&limitcol-new-group02-grp-1021-credit-limit=&col-new-group02-grp-1021-credit-limit-single=500000&col-new-group02-grp-1021-turnover-forecast=17&limitcol-new-group02-grp-1021-turnover-forecast=&col-new-group02-grp-1021-services=1&col-new-group02-grp-1021-services-7=7&
    col-new-group02-grp-1021-services-11=11&col-new-group02-grp-1021-services-70=70&col-new-group02-grp-1006-finish-dt=2015-12-31T00%3A00%3A00&col-new-group02-grp-1008-finish-dt=2015-12-31T00%3A00%3A00&col-new-group02-grp-1008-commission-type=48&col-new-group02-grp-1008-supercommission=0&col-new-group02-grp-1008-commission-declared-sum=&col-new-group02-grp-1009-finish-dt=2015-12-31T00%3A00%3A00&
    col-new-group02-grp-1009-commission-type=48&col-new-group02-grp-1009-supercommission=0&col-new-group02-grp-1009-commission-declared-sum=&col-new-group02-grp-1009-payment-term=25&col-new-group02-grp-1009-credit-limit=17&limitcol-new-group02-grp-1009-credit-limit=&col-new-group02-grp-1009-turnover-forecast=17&limitcol-new-group02-grp-1009-turnover-forecast=&col-new-group02-grp-1009-credit-limit-single=500000&
    col-new-group02-grp-1010-finish-dt=2015-12-31T00%3A00%3A00&col-new-group02-grp-1010-payment-term=25&col-new-group02-grp-1010-credit-limit=17&limitcol-new-group02-grp-1010-credit-limit=&col-new-group02-grp-1010-turnover-forecast=17&limitcol-new-group02-grp-1010-turnover-forecast=&col-new-group02-grp-1010-credit-limit-single=500000&col-new-group02-grp-1013-commission-charge-type=1&col-new-group02-grp-1013-finish-dt=2015-12-31T00%3A00%3A00&
    col-new-group02-grp-1013-commission-payback-type=2&col-new-group02-grp-1013-commission-type=48&col-new-group02-grp-1013-commission-declared-sum=&col-new-group02-grp-1013-supercommission=0&col-new-group02-grp-1019-finish-dt=2015-12-31T00%3A00%3A00&col-new-group02-grp-1019-discount-policy-type=0&col-new-group02-grp-1019-discount-fixed=12&col-new-group02-grp-1019-declared-sum=&col-new-group02-grp-1019-fixed-discount-pct=&col-new-group02-grp-1019-budget-discount-pct=&
    col-new-group02-grp-1019-discount-pct=&col-new-group02-grp-1019-discount-findt=&col-new-group02-grp-1032-commission-payback-pct=8+%25&col-new-group02-grp-1020-finish-dt=2015-12-31T00%3A00%3A00&col-new-group02-grp-1020-credit-type=2&col-new-group02-grp-1020-payment-term=25&col-new-group02-grp-1020-payment-term-max=&col-new-group02-grp-1020-repayment-on-consume-checkpassed=1&col-new-group02-grp-1020-credit-limit=17&limitcol-new-group02-grp-1020-credit-limit=&col-new-group02-grp-1020-turnover-forecast=17&limitcol-new-group02-grp-1020-turnover-forecast=&col-new-group02-grp-1020-credit-limit-single=500000&col-new-group02-grp-90-finish-dt=2015-12-31T00%3A00%3A00&col-new-group02-grp-100-personal-account-checkpassed=1&col-new-group02-grp-100-credit-limit=17&limitcol-new-group02-grp-100-credit-limit=&col-new-group02-grp-100-turnover-forecast=17&limitcol-new-group02-grp-100-turnover-forecast=&col-new-group02-grp-100-credit-limit-single=500000&col-new-group02-grp-110-commission-payback-pct=8+%25&col-new-group02-grp-110-credit-type=2&col-new-group02-grp-110-payment-term=25&col-new-group02-grp-110-payment-term-max=&col-new-group02-grp-110-calc-defermant=0&col-new-group02-grp-110-repayment-on-consume-checkpassed=1&col-new-group02-grp-110-credit-limit=17&limitcol-new-group02-grp-110-credit-limit=&col-new-group02-grp-110-credit-limit-single=500000&col-new-group02-grp-110-turnover-forecast=17&limitcol-new-group02-grp-110-turnover-forecast=&col-new-group02-grp-1031-commission-payback-pct=8+%25&col-new-group02-grp-1031-supercommission-bonus=1&col-new-group02-grp-1031-credit-type=2&col-new-group02-grp-1031-payment-term=25&col-new-group02-grp-1031-payment-term-max=&col-new-group02-grp-1031-calc-defermant=0&col-new-group02-grp-1031-repayment-on-consume-checkpassed=1&col-new-group02-grp-1031-credit-limit=17&limitcol-new-group02-grp-1031-credit-limit=&col-new-group02-grp-1031-credit-limit-single=500000&col-new-group02-grp-1031-turnover-forecast=17&limitcol-new-group02-grp-1031-turnover-forecast=&col-new-group02-grp-1034-finish-dt=2015-12-31T00%3A00%3A00&col-new-group02-grp-1034-supercommission-bonus=1&col-new-group02-grp-1036-calc-termination=&col-new-group02-grp-1033-finish-dt=2015-12-31T00%3A00%3A00&col-new-group02-grp-1033-supercommission-bonus=1&col-new-group02-grp-1033-payment-term=25&col-new-group02-grp-1033-credit-limit=17&limitcol-new-group02-grp-1033-credit-limit=&col-new-group02-grp-1033-turnover-forecast=17&limitcol-new-group02-grp-1033-turnover-forecast=&col-new-group02-grp-1033-credit-limit-single=500000&col-new-group02-grp-1035-client-limits=%5B%5D&col-new-group02-grp-1037-commission-categories=%5B%5D&col-new-group02-grp-1039-adfox-products=%5B%7B%22id%22%3A%221%22%2C%22num%22%3A504400%2C%22name%22%3A%22ADFOX.Sites1+default%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%222%22%2C%22num%22%3A504401%2C%22name%22%3A%22ADFOX.Nets+default%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%223%22%2C%22num%22%3A504402%2C%22name%22%3A%22ADFOX.Mobile+default%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%224%22%2C%22num%22%3A504403%2C%22name%22%3A%22ADFOX.Exchange+default%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%225%22%2C%22num%22%3A504404%2C%22name%22%3A%22ADFOX.Adv+default%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%226%22%2C%22num%22%3A504405%2C%22name%22%3A%22ADFOX.Sites1+%28shows%29%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%227%22%2C%22num%22%3A504406%2C%22name%22%3A%22ADFOX.Sites1+%28requests%29%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%228%22%2C%22num%22%3A504407%2C%22name%22%3A%22Sites%2BMobile%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%229%22%2C%22num%22%3A504408%2C%22name%22%3A%22ADFOX.Nets%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2210%22%2C%22num%22%3A504409%2C%22name%22%3A%22ADFOX.Mobile%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2211%22%2C%22num%22%3A504410%2C%22name%22%3A%22ADFOX.Exchange%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2212%22%2C%22num%22%3A504411%2C%22name%22%3A%22ADFOX.Adv%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2213%22%2C%22num%22%3A504412%2C%22name%22%3A%22%D0%92%D1%8B%D0%B3%D1%80%D1%83%D0%B7%D0%BA%D0%B0+%D0%BB%D0%BE%D0%B3%D0%BE%D0%B2+%D0%B8%D0%B7+%D0%9F%D1%80%D0%BE%D0%B3%D1%80%D0%B0%D0%BC%D0%BC%D1%8B%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2214%22%2C%22num%22%3A504413%2C%22name%22%3A%22%D0%A2%D0%B0%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%9C%D0%B0%D1%82%D0%B5%D1%80%D0%B8%D0%B0%D0%BB%D0%BE%D0%B2+%D0%BF%D0%BE+%D0%BF%D0%BE%D0%BB%D1%83+%D0%B8+%D0%B2%D0%BE%D0%B7%D1%80%D0%B0%D1%81%D1%82%D1%83+%D0%B4%D0%BB%D1%8F+ADFOX.Sites%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2215%22%2C%22num%22%3A504414%2C%22name%22%3A%22%D0%A2%D0%B0%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%9C%D0%B0%D1%82%D0%B5%D1%80%D0%B8%D0%B0%D0%BB%D0%BE%D0%B2+%D0%BF%D0%BE+%D0%BF%D0%BE%D0%BB%D1%83+%D0%B8+%D0%B2%D0%BE%D0%B7%D1%80%D0%B0%D1%81%D1%82%D1%83+%D0%B4%D0%BB%D1%8F+ADFOX.Sites%2BMobile%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2216%22%2C%22num%22%3A504415%2C%22name%22%3A%22%D0%A2%D0%B0%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%9C%D0%B0%D1%82%D0%B5%D1%80%D0%B8%D0%B0%D0%BB%D0%BE%D0%B2+%D0%BF%D0%BE+%D0%BF%D0%BE%D0%BB%D1%83+%D0%B8+%D0%B2%D0%BE%D0%B7%D1%80%D0%B0%D1%81%D1%82%D1%83+%D0%B4%D0%BB%D1%8F+ADFOX.Nets%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2217%22%2C%22num%22%3A504416%2C%22name%22%3A%22%D0%A2%D0%B0%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%9C%D0%B0%D1%82%D0%B5%D1%80%D0%B8%D0%B0%D0%BB%D0%BE%D0%B2+%D0%BF%D0%BE+%D0%BF%D0%BE%D0%BB%D1%83+%D0%B8+%D0%B2%D0%BE%D0%B7%D1%80%D0%B0%D1%81%D1%82%D1%83+%D0%B4%D0%BB%D1%8F+ADFOX.Mobile%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2218%22%2C%22num%22%3A504417%2C%22name%22%3A%22%D0%A2%D0%B0%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%9C%D0%B0%D1%82%D0%B5%D1%80%D0%B8%D0%B0%D0%BB%D0%BE%D0%B2+%D0%BF%D0%BE+%D0%BF%D0%BE%D0%BB%D1%83+%D0%B8+%D0%B2%D0%BE%D0%B7%D1%80%D0%B0%D1%81%D1%82%D1%83+%D0%B4%D0%BB%D1%8F+ADFOX.Adv%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2219%22%2C%22num%22%3A504418%2C%22name%22%3A%22%D0%A0%D0%B0%D0%B7%D1%80%D0%B0%D0%B1%D0%BE%D1%82%D0%BA%D0%B0+%D0%BD%D0%B5%D1%81%D1%82%D0%B0%D0%BD%D0%B4%D0%B0%D1%80%D1%82%D0%BD%D0%BE%D0%B9+%D1%81%D1%82%D0%B0%D1%82%D0%B8%D1%81%D1%82%D0%B8%D1%87%D0%B5%D1%81%D0%BA%D0%BE%D0%B9+%D0%BE%D1%82%D1%87%D0%B5%D1%82%D0%BD%D0%BE%D1%81%D1%82%D0%B8%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2220%22%2C%22num%22%3A504419%2C%22name%22%3A%22%D0%9D%D0%B5%D1%81%D1%82%D0%B0%D0%BD%D0%B4%D0%B0%D1%80%D1%82%D0%BD%D0%B0%D1%8F+%D1%81%D1%82%D0%B0%D1%82%D0%B8%D1%81%D1%82%D0%B8%D1%87%D0%B5%D1%81%D0%BA%D0%B0%D1%8F+%D0%BE%D1%82%D1%87%D0%B5%D1%82%D0%BD%D0%BE%D1%81%D1%82%D1%8C+%28%D0%BF%D0%BE%D0%B4%D0%B4%D0%B5%D1%80%D0%B6%D0%BA%D0%B0%29%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2221%22%2C%22num%22%3A504420%2C%22name%22%3A%22%D0%97%D0%B0%D0%B2%D0%B5%D0%B4%D0%B5%D0%BD%D0%B8%D0%B5+%D1%80%D0%B5%D0%BA%D0%BB%D0%B0%D0%BC%D0%BD%D0%BE%D0%B9+%D0%BA%D0%B0%D0%BC%D0%BF%D0%B0%D0%BD%D0%B8%D0%B8%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2222%22%2C%22num%22%3A504421%2C%22name%22%3A%22%D0%97%D0%B0%D0%B2%D0%B5%D0%B4%D0%B5%D0%BD%D0%B8%D0%B5+%D1%80%D0%B5%D0%BA%D0%BB%D0%B0%D0%BC%D0%BD%D0%BE%D0%B9+%D0%BA%D0%B0%D0%BC%D0%BF%D0%B0%D0%BD%D0%B8%D0%B8+Adv%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2223%22%2C%22num%22%3A504422%2C%22name%22%3A%22%D0%98%D1%81%D0%BF%D0%BE%D0%BB%D1%8C%D0%B7%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F+%C2%AB%D0%A2%D0%B0%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%BF%D0%BE+%D0%BF%D0%BE%D0%B2%D0%B5%D0%B4%D0%B5%D0%BD%D0%B8%D1%8E%C2%BB+%D0%B4%D0%BB%D1%8F+ADFOX.Sites%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2224%22%2C%22num%22%3A504423%2C%22name%22%3A%22%D0%98%D1%81%D0%BF%D0%BE%D0%BB%D1%8C%D0%B7%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F+%C2%AB%D0%A2%D0%B0%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%BF%D0%BE+%D0%BF%D0%BE%D0%B2%D0%B5%D0%B4%D0%B5%D0%BD%D0%B8%D1%8E%C2%BB+%D0%B4%D0%BB%D1%8F+ADFOX.Sites%2BMobile%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2225%22%2C%22num%22%3A504424%2C%22name%22%3A%22%D0%9A%D0%B0%D1%81%D1%82%D0%BE%D0%BC%D0%B8%D0%B7%D0%B0%D1%86%D0%B8%D1%8F+%D0%B0%D0%BA%D0%BA%D0%B0%D1%83%D0%BD%D1%82%D0%B0%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%5D&col-new-group02-grp-1004-credit-type=2&col-new-group02-grp-1004-payment-term=25&col-new-group02-grp-1004-payment-term-max=&col-new-group02-grp-1004-credit-limit=17&limitcol-new-group02-grp-1004-credit-limit=&col-new-group02-grp-1004-credit-limit-single=500000&col-new-group02-grp-1004-turnover-forecast=17&limitcol-new-group02-grp-1004-turnover-forecast=&col-new-group02-grp-1017-payment-term-max=&col-new-group02-grp-1005-credit-type=2&col-new-group02-grp-1005-payment-term=25&col-new-group02-grp-1005-payment-term-max=&col-new-group02-grp-1005-repayment-on-consume-checkpassed=1&col-new-group02-grp-1005-credit-limit=17&limitcol-new-group02-grp-1005-credit-limit=&col-new-group02-grp-1005-turnover-forecast=17&limitcol-new-group02-grp-1005-turnover-forecast=&col-new-group02-grp-1005-credit-limit-single=500000&col-new-group02-grp-1012-calc-defermant=0&col-new-group02-grp-115-declared-sum=&col-new-group02-grp-115-discount-pct=&col-new-group02-grp-115-discount-findt=&col-new-group02-grp-1011-discount-policy-type=0&col-new-group02-grp-1011-discount-fixed=12&col-new-group02-grp-1011-declared-sum=&col-new-group02-grp-1011-fixed-discount-pct=&col-new-group02-grp-1011-budget-discount-pct=&col-new-group02-grp-1011-discount-pct=&col-new-group02-grp-1011-discount-findt=&col-new-group02-grp-1015-federal-budget=&col-new-group02-grp-1015-ukr-budget=&col-new-group02-grp-1015-federal-declared-budget=&col-new-group02-grp-1015-federal-annual-program-budget=&col-new-group02-grp-1015-belarus-budget=&col-new-group02-grp-1015-year-planning-discount=&col-new-group02-grp-1015-year-product-discount=&col-new-group02-grp-1015-consolidated-discount=&col-new-group02-grp-1015-use-consolidated-discount-checkpassed=1&col-new-group02-grp-1015-use-ua-cons-discount-checkpassed=1&col-new-group02-grp-1015-regional-budget=&col-new-group02-grp-1015-use-regional-cons-discount-checkpassed=1&col-new-group02-grp-1015-pda-budget=&col-new-group02-grp-1015-autoru-budget=&col-new-group02-grp-1001-supercommission-bonus=1&col-new-group02-grp-1001-services=1&col-new-group02-grp-1001-services-7=7&col-new-group02-grp-1001-services-11=11&col-new-group02-grp-1001-services-70=70&col-new-group02-grp-1023-currency=810&col-new-group02-grp-1023-bank-details-id=21&col-new-group02-grp-1024-loyal-clients=%5B%5D&col-new-group02-grp-1025-pp-1137-checkpassed=1&col-new-group02-grp-1026-brand-clients=%5B%5D&col-new-group02-grp-1027-retro-discount=&col-new-group02-grp-1028-partner-min-commission-sum=&col-new-group02-grp-2222-advance-payment-sum=&col-new-group02-grp-1030-partner-commission-type=1&col-new-group02-grp-1030-partner-commission-pct=&col-new-group02-grp-1030-partner-commission-sum=&col-new-group02-grp-1030-partner-min-commission-sum=&col-new-group02-grp-1038-service-min-cost=&col-new-is-booked-checkpassed=1&col-new-is-faxed-checkpassed=1&col-new-is-signed=&col-new-is-signed-checkpassed=1&col-new-is-signed-date=26+%D1%8F%D0%BD%D0%B2+2015+%D0%B3.&col-new-is-signed-dt=2015-01-26T00%3A00%3A00&col-new-sent-dt-checkpassed=1&
    col-new-collateral-form=&id=''' + str(contract['ID']))
    collateral_id = tm.Balance.CreateContract(16571028, {k: v.decode('utf-8') for k, v in col})
    print 'Collateral: ' + str(collateral_id) + ')'

    sql = "update t_contract_collateral set is_signed = date'2014-12-05' where contract2_id = %s and collateral_type_id = 1033" % str(
        contract['ID'])
    test.ExecuteSQL(sql);
    test.ExecuteSQL('commit')

    ## Клиент
    client_id = MTestlib.create_client({'IS_AGENCY': 0, 'NAME': u'UL КП Директ резидент без скидки'})

    service_id = 7
    product_id = 1475
    paysys_id = 1003
    ## Счет одним вызовом (+external_id)
    campaigns_list = [
        {'service_id': service_id, 'product_id': product_id, 'qty': qty, 'begin_dt': begin_dt},
        {'service_id': service_id, 'product_id': product_id, 'qty': qty2, 'begin_dt': begin_dt}
    ]
    invoice_id, orders_list = MTestlib.create_force_invoice(client_id, person_id, campaigns_list, paysys_id,
                                                            invoice_dt=invoice_dt, agency_id=agency_id
                                                            , credit=1
                                                            , contract_id=contract['ID']
                                                            )
    ##Открутки для случая с несколькими заказами в счёте
    MTestlib.do_campaigns(orders_list[0]['ServiceID'], orders_list[0]['ServiceOrderID'], {'Bucks': 10, 'Money': 0}, 0,
                          None)

    ## -----------------------------------------------------------------

    ## Клиент
    client_id = MTestlib.create_client({'IS_AGENCY': 0, 'NAME': u'UL КП Директ резидент со скидкой'})
    ## Бюджет
    test.ExecuteSQL('''Insert into t_client_direct_budget (ID,CLIENT_ID,END_DT,CLASSNAME,BUDGET,CURRENCY,UPDATE_DT)
        values (S_CLIENT_DIRECT_BUDGET_ID.nextval,:client_id,TRUNC(SYSDATE, 'MONTH'),'DirectDiscountCalculator','13374',null,sysdate) ''',
                    {'client_id': client_id})
    ## Счет одним вызовом (+external_id)
    campaigns_list = [
        {'service_id': service_id, 'product_id': product_id, 'qty': qty, 'begin_dt': begin_dt}
    ]
    invoice_id, orders_list = MTestlib.create_force_invoice(client_id, person_id, campaigns_list, paysys_id,
                                                            invoice_dt=invoice_dt, agency_id=agency_id
                                                            , credit=1
                                                            , contract_id=contract['ID']
                                                            )
    ##Открутки для случая с несколькими заказами в счёте
    MTestlib.do_campaigns(orders_list[0]['ServiceID'], orders_list[0]['ServiceOrderID'], {'Bucks': 60, 'Money': 0}, 0,
                          None)

    ## -----------------------------------------------------------------

    ## Клиент
    client_id = MTestlib.create_client({'IS_AGENCY': 0, 'NAME': u'UL КП Директ нерезидент USD'})
    ## Нерезидент
    test.ExecuteSQL('''Update t_client set FULLNAME = u'UL КП Директ нерезидент USD полное', CURRENCY_PAYMENT = 'USD',
     ISO_CURRENCY_PAYMENT = 'USD', IS_NON_RESIDENT = 1 where ID = :client_id ''',
                    {'client_id': client_id})
    ## Счет одним вызовом (+external_id)
    paysys_id = 1026
    campaigns_list = [
        {'service_id': service_id, 'product_id': product_id, 'qty': qty, 'begin_dt': begin_dt}
    ]
    invoice_id, orders_list = MTestlib.create_force_invoice(client_id, person_id, campaigns_list, paysys_id,
                                                            invoice_dt=invoice_dt, agency_id=agency_id
                                                            , credit=1
                                                            , contract_id=contract['ID']
                                                            )
    ##Открутки для случая с несколькими заказами в счёте
    MTestlib.do_campaigns(orders_list[0]['ServiceID'], orders_list[0]['ServiceOrderID'], {'Bucks': 30, 'Money': 0}, 0,
                          None)

    ##----------------------------------------------------------------

    ## Клиент
    client_id = MTestlib.create_client({'IS_AGENCY': 0, 'NAME': u'UL КП Директ нерезидент EUR'})
    ## Нерезидент
    test.ExecuteSQL('''Update t_client set FULLNAME = u'UL КП Директ нерезидент EUR полное', CURRENCY_PAYMENT = 'EUR', 
    ISO_CURRENCY_PAYMENT = 'EUR', IS_NON_RESIDENT = 1  where ID = :client_id ''',
                    {'client_id': client_id})
    ## Счет одним вызовом (+external_id)
    paysys_id = 1027
    campaigns_list = [
        {'service_id': service_id, 'product_id': product_id, 'qty': qty, 'begin_dt': begin_dt}
    ]
    invoice_id, orders_list = MTestlib.create_force_invoice(client_id, person_id, campaigns_list, paysys_id,
                                                            invoice_dt=invoice_dt, agency_id=agency_id
                                                            , credit=1
                                                            , contract_id=contract['ID']
                                                            )
    ##Открутки для случая с несколькими заказами в счёте
    MTestlib.do_campaigns(orders_list[0]['ServiceID'], orders_list[0]['ServiceOrderID'], {'Bucks': 40, 'Money': 0}, 0,
                          None)

    ##----------------------------------------------------------------

    ## Клиент
    client_id = MTestlib.create_client({'IS_AGENCY': 0, 'NAME': u'UL КП Директ нерезидент RUR'})
    ## Нерезидент
    test.ExecuteSQL('''Update t_client set FULLNAME = u'UL КП Директ нерезидент RUR полное', CURRENCY_PAYMENT = 'RUR',
           ISO_CURRENCY_PAYMENT = 'RUB', IS_NON_RESIDENT = 1 where ID = :client_id ''',
                    {'client_id': client_id})
    ## Счет одним вызовом (+external_id)
    paysys_id = 1025
    campaigns_list = [
        {'service_id': service_id, 'product_id': product_id, 'qty': qty, 'begin_dt': begin_dt}
    ]
    invoice_id, orders_list = MTestlib.create_force_invoice(client_id, person_id, campaigns_list, paysys_id,
                                                            invoice_dt=invoice_dt, agency_id=agency_id
                                                            , credit=1
                                                            , contract_id=contract['ID']
                                                            )
    ##Открутки для случая с несколькими заказами в счёте
    MTestlib.do_campaigns(orders_list[0]['ServiceID'], orders_list[0]['ServiceOrderID'], {'Bucks': 50, 'Money': 0}, 0,
                          None)

    ##----------------------------------------------------------------

    ####Генерация актов
    ##    act_dt = datetime.datetime(2015,2,28)
    ##    force = 1
    ##    p = MTestlib.act_accounter (agency_id, act_dt, force)

    ## Путь до браузера
    opera = 'C:\Program Files (x86)\Opera\launcher.exe'
    opera_args = 'https://balance-admin.greed-tm1f.yandex.ru/passports.xml?tcl_id=%s' % agency_id
    spOpera = subprocess.Popen(opera + ' ' + opera_args)
    print opera_args


test_client()
