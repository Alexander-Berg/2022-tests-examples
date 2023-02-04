#-*- coding: utf-8 -*-
import datetime
import proxy_provider
##  1    Комиссионный
##  61   Оптовый агентский, премия
##  2    Партнерский
##  0    Не агентский
##  3    Без уч. в расчетах
##  4    Прямой агентский
##  6    Оптовый агентский
##  8    Казахстан
##  7    Оптовый клиентский
##  16   Оптовый агентский с РБ
##  17   Украина: оптовый клиентский
##  24   Украина: прямой агентский
##  27   Украина: оптовый агентский, премия
##  21   Украина: комиссионный
##  18   США: оптовый клиентский
##  19   США: оптовый агентский
##  20   Голландия (старая форма): Оптовый агентский
##  22   Швейцария: Оптовый клиентский
##  23   Швейцария: Оптовый агентский
##  25   Турция: Оптовый агентский
##  26   Турция: Оптовый клиентский
##  40   Гарантийное письмо Россия
##  41   Гарантийное письмо Беларусь
##  42   Гарантийное письмо Украина
##  43   Гарантийное письмо Казахстан
##  50   Соглашение о рекламном бренде
##  60   Доверенность
##  70   Авто.ру: Оптовый агентский, премия
##  71   Авто.ру: Не агентский

host = 'greed-tm1f'
#host = 'greed-dev1e'
##host = 'greed-ts1f'
##host = 'greed-load2e'
##host = 'greed-pt1f'
##host = 'greed-pt1g'
# host = ('greed-dev4f', 'ashvedunov')

rpc = proxy_provider.GetServiceProxy(host, 0)
test_rpc = proxy_provider.GetServiceProxy(host, 1)
http_api = proxy_provider.GetServiceProxy(host, 2)

# For data format convertation
sql_date_format = "%Y-%m-%dT00:00:00"
log_align = 30
passport_uid = 16571028

def convert_date (dt):
    mn = {1:'янв',2:'фев',3:'мар',4:'апр',5:'май',6:'июн',7:'июл',8:'авг',9:'сен',10:'окт',11:'ноя',12:'дек'}
    dt = '{0} {1} {2} г.'.format(int(str(dt)[8:10]), mn[int(str(dt)[5:7])] , str(dt)[:4])
    return dt

def create_con(type_, params, mode = 'Contract',ccy=None):
    type_con_mapper={
    'commission' :        1      ##Комиссионный
    ,'opt_agency_prem':   61     ##Оптовый агентский, премия
    ,'partner'    :  	   2      ##Партнерский
    ,'no_agency'   :  	   0      ##Не агентский
    ,'w_o_count'  :        3     ##Без уч. в расчетах
    ,'pryam_agency':       4     ##Прямой агентский
    ,'opt_client':         6     ##Оптовый агентский
    ,'kzt':                8     ##Казахстан
    ,'opt_client':         7     ##Оптовый клиентский
    ,'opt_agency_rb':      16    ##Оптовый агентский с РБ
    ,'ua_opt_client':      17    ##Украина: оптовый клиентский
    ,'ua_pryam_agency':    24    ##Украина: прямой агентский
    ,'ua_opt_agency_prem': 27    ##Украина: оптовый агентский, премия
    ,'ua_commission':      21    ##Украина: комиссионный
    ,'usa_opt_client':     18    ##США: оптовый клиентский
    ,'usa_opt_agency':     19    ##США: оптовый агентский
    ,'holland':            20    ##Голландия (старая форма): Оптовый агентский
    ,'sw_opt_client':      22    ##Швейцария: Оптовый клиентский
    ,'sw_opt_agency':      23    ##Швейцария: Оптовый агентский
    ,'tr_opt_agency':      25    ##Турция: Оптовый агентский
    ,'tr_opt_client':      26    ##Турция: Оптовый клиентский
    ,'guar_ru':            40    ##Гарантийное письмо Россия
    ,'guar_bel':           41    ##Гарантийное письмо Беларусь
    ,'guar_ua':            42    ##Гарантийное письмо Украина
    ,'guar_kzt':           43    ##Гарантийное письмо Казахстан
    ,'brand':              50    ##Соглашение о рекламном бренде
    ,'dover':              60    ##Доверенность
    ,'autoru_opt_agency':  70    ##Авто.ру: Оптовый агентский, премия
    ,'autiru_no_agency':   71    ##Авто.ру: Не агентский
    }

    final_string='external-id='
    defaults = ['num='
                ,'print-form-type=0'
                ,'memo='
                ,'brand-type=70'
                ,'account-type=0'
                ,'manager-bo-code='
                ,'atypical-conditions-checkpassed=1'
                ,'calc-termination='
                ,'attorney-agency-id='
                ,'id='
                ,'partner-commission-type=1'
                ,'supercommission-bonus=1'
                ,'supercommission=0'
                ,'linked-contracts=1'
                ,'force-direct-migration-checkpassed=1'
                ,'commission-payback-pct='
                ,'commission-charge-type=1'
                ,'minimal-payment-commission='
                ,'commission-payback-type=2'
                ,'named-client-declared-sum='
                ,'commission-declared-sum='
                ,'partner-commission-sum='
                ,'limitlinked-contracts='
                ,'partner-max-commission-sum='
                ,'partner-min-commission-sum='
                ,'discard-nds=0'
                ,'declared-sum='
                ,'belarus-budget-price='
                ,'ukr-budget='
                ,'rit-discount='
                ,'budget-discount-pct='
                ,'kz-budget='
                ,'federal-annual-program-budget='
                ,'federal-budget='
                ,'belarus-budget='
                ,'federal-declared-budget='
                ,'kzt-budget='
                ,'year-product-discount='
                ,'year-planning-discount='
                ,'year-planning-discount-custom='
                ,'use-ua-cons-discount-checkpassed=1'
                ,'consolidated-discount='
                ,'use-consolidated-discount-checkpassed=1'
                ,'regional-budget='
                ,'use-regional-cons-discount-checkpassed=1'
                ,'button-submit=%D0%A1%D0%BE%D1%85%D1%80%D0%B0%D0%BD%D0%B8%D1%82%D1%8C'
                ,'discount-findt='
                ,'payment-term-max='
                ,'calc-defermant=0'
                ,'limitcredit-currency-limit='
                ,'limitcredit-limit='
                ,'limitturnover-forecast='
                ,'partner-credit-checkpassed=1'
                ,'discount-commission='
                ,'pp-1137-checkpassed=1'
                ,'new-commissioner-report-checkpassed=1'
                ,'service-min-cost='
                ,'test-period-duration='
                ,'discard-media-discount-checkpassed=1'
                ,'deal-passport-checkpassed=1'
                ##могут меняться! надо ли с ними что-то делать?
                ,'adfox-products=%5B%7B%22id%22%3A%221%22%2C%22num%22%3A504400%2C%22name%22%3A%22ADFOX.Sites1+default%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%222%22%2C%22num%22%3A504401%2C%22name%22%3A%22ADFOX.Nets+default%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%223%22%2C%22num%22%3A504402%2C%22name%22%3A%22ADFOX.Mobile+default%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%224%22%2C%22num%22%3A504403%2C%22name%22%3A%22ADFOX.Exchange+default%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%225%22%2C%22num%22%3A504404%2C%22name%22%3A%22ADFOX.Adv+%28standart%29+default%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%226%22%2C%22num%22%3A504405%2C%22name%22%3A%22ADFOX.Sites1+%28shows%29%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%227%22%2C%22num%22%3A504406%2C%22name%22%3A%22ADFOX.Sites2+%28requests%29%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%228%22%2C%22num%22%3A504407%2C%22name%22%3A%22ADFOX.Sites%2Bmobile+%28shows%29%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%229%22%2C%22num%22%3A504408%2C%22name%22%3A%22ADFOX.Nets%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2210%22%2C%22num%22%3A504409%2C%22name%22%3A%22ADFOX.Mobile%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2211%22%2C%22num%22%3A504410%2C%22name%22%3A%22ADFOX.Exchange%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2212%22%2C%22num%22%3A504411%2C%22name%22%3A%22ADFOX.Adv+%28standart%29%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2213%22%2C%22num%22%3A504412%2C%22name%22%3A%22%D0%92%D1%8B%D0%B3%D1%80%D1%83%D0%B7%D0%BA%D0%B0+%D0%BB%D0%BE%D0%B3%D0%BE%D0%B2+%D0%B8%D0%B7+%D0%9F%D1%80%D0%BE%D0%B3%D1%80%D0%B0%D0%BC%D0%BC%D1%8B%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2214%22%2C%22num%22%3A504413%2C%22name%22%3A%22%D0%A2%D0%B0%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%9C%D0%B0%D1%82%D0%B5%D1%80%D0%B8%D0%B0%D0%BB%D0%BE%D0%B2+%D0%BF%D0%BE+%D0%BF%D0%BE%D0%BB%D1%83+%D0%B8+%D0%B2%D0%BE%D0%B7%D1%80%D0%B0%D1%81%D1%82%D1%83+%D0%B4%D0%BB%D1%8F+ADFOX.Sites%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2215%22%2C%22num%22%3A504414%2C%22name%22%3A%22%D0%A2%D0%B0%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%9C%D0%B0%D1%82%D0%B5%D1%80%D0%B8%D0%B0%D0%BB%D0%BE%D0%B2+%D0%BF%D0%BE+%D0%BF%D0%BE%D0%BB%D1%83+%D0%B8+%D0%B2%D0%BE%D0%B7%D1%80%D0%B0%D1%81%D1%82%D1%83+%D0%B4%D0%BB%D1%8F+ADFOX.Sites%2BMobile%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2216%22%2C%22num%22%3A504415%2C%22name%22%3A%22%D0%A2%D0%B0%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%9C%D0%B0%D1%82%D0%B5%D1%80%D0%B8%D0%B0%D0%BB%D0%BE%D0%B2+%D0%BF%D0%BE+%D0%BF%D0%BE%D0%BB%D1%83+%D0%B8+%D0%B2%D0%BE%D0%B7%D1%80%D0%B0%D1%81%D1%82%D1%83+%D0%B4%D0%BB%D1%8F+ADFOX.Nets%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2217%22%2C%22num%22%3A504416%2C%22name%22%3A%22%D0%A2%D0%B0%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%9C%D0%B0%D1%82%D0%B5%D1%80%D0%B8%D0%B0%D0%BB%D0%BE%D0%B2+%D0%BF%D0%BE+%D0%BF%D0%BE%D0%BB%D1%83+%D0%B8+%D0%B2%D0%BE%D0%B7%D1%80%D0%B0%D1%81%D1%82%D1%83+%D0%B4%D0%BB%D1%8F+ADFOX.Mobile%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2218%22%2C%22num%22%3A504417%2C%22name%22%3A%22%D0%A2%D0%B0%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%9C%D0%B0%D1%82%D0%B5%D1%80%D0%B8%D0%B0%D0%BB%D0%BE%D0%B2+%D0%BF%D0%BE+%D0%BF%D0%BE%D0%BB%D1%83+%D0%B8+%D0%B2%D0%BE%D0%B7%D1%80%D0%B0%D1%81%D1%82%D1%83+%D0%B4%D0%BB%D1%8F+ADFOX.Adv%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2219%22%2C%22num%22%3A504418%2C%22name%22%3A%22%D0%A0%D0%B0%D0%B7%D1%80%D0%B0%D0%B1%D0%BE%D1%82%D0%BA%D0%B0+%D0%BD%D0%B5%D1%81%D1%82%D0%B0%D0%BD%D0%B4%D0%B0%D1%80%D1%82%D0%BD%D0%BE%D0%B9+%D1%81%D1%82%D0%B0%D1%82%D0%B8%D1%81%D1%82%D0%B8%D1%87%D0%B5%D1%81%D0%BA%D0%BE%D0%B9+%D0%BE%D1%82%D1%87%D0%B5%D1%82%D0%BD%D0%BE%D1%81%D1%82%D0%B8%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2220%22%2C%22num%22%3A504419%2C%22name%22%3A%22%D0%9D%D0%B5%D1%81%D1%82%D0%B0%D0%BD%D0%B4%D0%B0%D1%80%D1%82%D0%BD%D0%B0%D1%8F+%D1%81%D1%82%D0%B0%D1%82%D0%B8%D1%81%D1%82%D0%B8%D1%87%D0%B5%D1%81%D0%BA%D0%B0%D1%8F+%D0%BE%D1%82%D1%87%D0%B5%D1%82%D0%BD%D0%BE%D1%81%D1%82%D1%8C+%28%D0%BF%D0%BE%D0%B4%D0%B4%D0%B5%D1%80%D0%B6%D0%BA%D0%B0%29%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2221%22%2C%22num%22%3A504420%2C%22name%22%3A%22%D0%97%D0%B0%D0%B2%D0%B5%D0%B4%D0%B5%D0%BD%D0%B8%D0%B5+%D1%80%D0%B5%D0%BA%D0%BB%D0%B0%D0%BC%D0%BD%D0%BE%D0%B9+%D0%BA%D0%B0%D0%BC%D0%BF%D0%B0%D0%BD%D0%B8%D0%B8%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2222%22%2C%22num%22%3A504421%2C%22name%22%3A%22%D0%97%D0%B0%D0%B2%D0%B5%D0%B4%D0%B5%D0%BD%D0%B8%D0%B5+%D1%80%D0%B5%D0%BA%D0%BB%D0%B0%D0%BC%D0%BD%D0%BE%D0%B9+%D0%BA%D0%B0%D0%BC%D0%BF%D0%B0%D0%BD%D0%B8%D0%B8+Adv%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2223%22%2C%22num%22%3A504422%2C%22name%22%3A%22%D0%98%D1%81%D0%BF%D0%BE%D0%BB%D1%8C%D0%B7%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F+%5C%22%D0%A2%D0%B0%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%BF%D0%BE+%D0%BF%D0%BE%D0%B2%D0%B5%D0%B4%D0%B5%D0%BD%D0%B8%D1%8E%5C%22+%D0%B4%D0%BB%D1%8F+ADFOX.Sites1+%28shows%29%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2224%22%2C%22num%22%3A504423%2C%22name%22%3A%22%D0%98%D1%81%D0%BF%D0%BE%D0%BB%D1%8C%D0%B7%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F+%C2%AB%D0%A2%D0%B0%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%BF%D0%BE+%D0%BF%D0%BE%D0%B2%D0%B5%D0%B4%D0%B5%D0%BD%D0%B8%D1%8E%C2%BB+%D0%B4%D0%BB%D1%8F+ADFOX.Sites%2BMobile%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2225%22%2C%22num%22%3A504424%2C%22name%22%3A%22%D0%9A%D0%B0%D1%81%D1%82%D0%BE%D0%BC%D0%B8%D0%B7%D0%B0%D1%86%D0%B8%D1%8F+%D0%B0%D0%BA%D0%BA%D0%B0%D1%83%D0%BD%D1%82%D0%B0%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2226%22%2C%22num%22%3A504440%2C%22name%22%3A%22ADFOX.Sites%2Bmobile+%28requests%29%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2227%22%2C%22num%22%3A504441%2C%22name%22%3A%22ADFOX.Sites%2Bmobile+default%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2228%22%2C%22num%22%3A504642%2C%22name%22%3A%22ADFOX.Adv+%28agency%29+default%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2229%22%2C%22num%22%3A504643%2C%22name%22%3A%22ADFOX.Adv+%28agency%29%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2230%22%2C%22num%22%3A504644%2C%22name%22%3A%22%D0%98%D1%81%D0%BF%D0%BE%D0%BB%D1%8C%D0%B7%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%BC%D0%BE%D0%B4%D1%83%D0%BB%D1%8F+%5C%22%D0%A2%D0%B0%D1%80%D0%B3%D0%B5%D1%82%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%BF%D0%BE+%D0%BF%D0%BE%D0%B2%D0%B5%D0%B4%D0%B5%D0%BD%D0%B8%D1%8E%5C%22+%D0%B4%D0%BB%D1%8F+ADFOX.Sites2+%28requests%29%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2231%22%2C%22num%22%3A504653%2C%22name%22%3A%22AdFox.Sites%2BMobile%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2232%22%2C%22num%22%3A504654%2C%22name%22%3A%22%D0%9D%D0%B5%D1%81%D1%82%D0%B0%D0%BD%D0%B4%D0%B0%D1%80%D1%82%D0%BD%D0%B0%D1%8F+%D1%81%D1%82%D0%B0%D1%82%D0%B8%D1%81%D1%82%D0%B8%D1%87%D0%B5%D1%81%D0%BA%D0%B0%D1%8F+%D0%BE%D1%82%D1%87%D0%B5%D1%82%D0%BD%D0%BE%D1%81%D1%82%D1%8C%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%2C%7B%22id%22%3A%2233%22%2C%22num%22%3A504655%2C%22name%22%3A%22%D0%92%D1%8B%D0%B3%D1%80%D1%83%D0%B7%D0%BA%D0%B0+%D0%BB%D0%BE%D0%B3%D0%BE%D0%B2+%D0%B8%D0%B7+%D0%9F%D1%80%D0%BE%D0%B3%D1%80%D0%B0%D0%BC%D0%BC%D1%8B%22%2C%22scale%22%3A%22%22%2C%22account%22%3A%22%22%7D%5D'
                ,'apikeys-tariffs=%5B%7B%22group_id%22%3A%221%22%2C%22group_cc%22%3A%22test_1%22%2C%22group_name%22%3A%22Test+1%22%2C%22member%22%3A%22%22%2C%22id%22%3A%221%22%7D%2C%7B%22group_id%22%3A%2217%22%2C%22group_cc%22%3A%22apikeys_atom%22%2C%22group_name%22%3A%22API+%D0%90%D1%82%D0%BE%D0%BC%D0%B0%22%2C%22member%22%3A%22%22%2C%22id%22%3A%222%22%7D%2C%7B%22group_id%22%3A%2218%22%2C%22group_cc%22%3A%22apikeys_speechkit%22%2C%22group_name%22%3A%22Yandex+SpeechKit%22%2C%22member%22%3A%22%22%2C%22id%22%3A%223%22%7D%2C%7B%22group_id%22%3A%2219%22%2C%22group_cc%22%3A%22apikeys_market%22%2C%22group_name%22%3A%22API+%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81.%D0%9C%D0%B0%D1%80%D0%BA%D0%B5%D1%82%D0%B0%22%2C%22member%22%3A%22%22%2C%22id%22%3A%224%22%7D%2C%7B%22group_id%22%3A%2220%22%2C%22group_cc%22%3A%22apikeys_city%22%2C%22group_name%22%3A%22API+%D0%9F%D0%BE%D0%B8%D1%81%D0%BA%D0%B0+%D0%BF%D0%BE+%D0%BE%D1%80%D0%B3%D0%B0%D0%BD%D0%B8%D0%B7%D0%B0%D1%86%D0%B8%D1%8F%D0%BC%22%2C%22member%22%3A%22%22%2C%22id%22%3A%225%22%7D%2C%7B%22group_id%22%3A%2221%22%2C%22group_cc%22%3A%22apikeys_staticmaps%22%2C%22group_name%22%3A%22Static+API+%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81.%D0%9A%D0%B0%D1%80%D1%82+%28%D0%BF%D0%BB%D0%B0%D1%82%D0%BD%D0%B0%D1%8F+%D0%B2%D0%B5%D1%80%D1%81%D0%B8%D1%8F%29%22%2C%22member%22%3A%22%22%2C%22id%22%3A%226%22%7D%2C%7B%22group_id%22%3A%2222%22%2C%22group_cc%22%3A%22apikeys_rabota%22%2C%22group_name%22%3A%22API+%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81.%D0%A0%D0%B0%D0%B1%D0%BE%D1%82%D0%B0%22%2C%22member%22%3A%22%22%2C%22id%22%3A%227%22%7D%2C%7B%22group_id%22%3A%2223%22%2C%22group_cc%22%3A%22apikeys_realty%22%2C%22group_name%22%3A%22API+%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81.%D0%9D%D0%B5%D0%B4%D0%B2%D0%B8%D0%B6%D0%B8%D0%BC%D0%BE%D1%81%D1%82%D0%B8%22%2C%22member%22%3A%22%22%2C%22id%22%3A%228%22%7D%2C%7B%22group_id%22%3A%2224%22%2C%22group_cc%22%3A%22apikeys_auto%22%2C%22group_name%22%3A%22API+%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81.%D0%90%D0%B2%D1%82%D0%BE%22%2C%22member%22%3A%22%22%2C%22id%22%3A%229%22%7D%2C%7B%22group_id%22%3A%2225%22%2C%22group_cc%22%3A%22apikeys_aviatickets%22%2C%22group_name%22%3A%22API+%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81.%D0%90%D0%B2%D0%B8%D0%B0%D0%B1%D0%B8%D0%BB%D0%B5%D1%82%D0%BE%D0%B2%22%2C%22member%22%3A%22%22%2C%22id%22%3A%2210%22%7D%2C%7B%22group_id%22%3A%2226%22%2C%22group_cc%22%3A%22apikeys_apimaps%22%2C%22group_name%22%3A%22API+%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81.%D0%9A%D0%B0%D1%80%D1%82+%28%D0%BF%D0%BB%D0%B0%D1%82%D0%BD%D0%B0%D1%8F+%D0%B2%D0%B5%D1%80%D1%81%D0%B8%D1%8F%29%22%2C%22member%22%3A%22%22%2C%22id%22%3A%2211%22%7D%2C%7B%22group_id%22%3A%2227%22%2C%22group_cc%22%3A%22apikeys_microtest%22%2C%22group_name%22%3A%22API+%D0%B2%D0%B0%D0%BB%D0%B8%D0%B4%D0%B0%D1%82%D0%BE%D1%80%D0%B0+%D0%BC%D0%B8%D0%BA%D1%80%D0%BE%D1%80%D0%B0%D0%B7%D0%BC%D0%B5%D1%82%D0%BA%D0%B8%22%2C%22member%22%3A%22%22%2C%22id%22%3A%2212%22%7D%2C%7B%22group_id%22%3A%2228%22%2C%22group_cc%22%3A%22apikeys_text_rec%22%2C%22group_name%22%3A%22API+%D0%A0%D0%B0%D1%81%D0%BF%D0%BE%D0%B7%D0%BD%D0%B0%D0%B2%D0%B0%D0%BD%D0%B8%D1%8F+%D1%82%D0%B5%D0%BA%D1%81%D1%82%D0%B0%22%2C%22member%22%3A%22%22%2C%22id%22%3A%2213%22%7D%2C%7B%22group_id%22%3A%2229%22%2C%22group_cc%22%3A%22apikeys_rasp%22%2C%22group_name%22%3A%22API+%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81.%D0%A0%D0%B0%D1%81%D0%BF%D0%B8%D1%81%D0%B0%D0%BD%D0%B8%D0%B9%22%2C%22member%22%3A%22%22%2C%22id%22%3A%2214%22%7D%2C%7B%22group_id%22%3A%2230%22%2C%22group_cc%22%3A%22apikeys_raspmobile%22%2C%22group_name%22%3A%22API+%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81.%D0%A0%D0%B0%D1%81%D0%BF%D0%B8%D1%81%D0%B0%D0%BD%D0%B8%D0%B9+%D0%B4%D0%BB%D1%8F+%D0%BC%D0%BE%D0%B1%D0%B8%D0%BB%D1%8C%D0%BD%D1%8B%D1%85+%D0%BF%D1%80%D0%B8%D0%BB%D0%BE%D0%B6%D0%B5%D0%BD%D0%B8%D0%B9%22%2C%22member%22%3A%22%22%2C%22id%22%3A%2215%22%7D%2C%7B%22group_id%22%3A%2231%22%2C%22group_cc%22%3A%22apikeys_partner%22%2C%22group_name%22%3A%22API+%D0%9F%D0%B0%D1%80%D1%82%D0%BD%D0%B5%D1%80%D1%81%D0%BA%D0%BE%D0%B3%D0%BE+%D0%B8%D0%BD%D1%82%D0%B5%D1%80%D1%84%D0%B5%D0%B9%D1%81%D0%B0%22%2C%22member%22%3A%22%22%2C%22id%22%3A%2216%22%7D%2C%7B%22group_id%22%3A%2241%22%2C%22group_cc%22%3A%22apikeys_pogoda%22%2C%22group_name%22%3A%22API+%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81.%D0%9F%D0%BE%D0%B3%D0%BE%D0%B4%D1%8B%22%2C%22member%22%3A%22%22%2C%22id%22%3A%2217%22%7D%5D'
                ,'regional-budget='
                ,'use-regional-cons-discount-checkpassed=1'
    ]
    today = datetime.datetime.now()
    changed = { 'dt': today.replace(year=today.year-1).strftime(sql_date_format)
               ,'finish-dt':today.replace(year=today.year+1).strftime(sql_date_format)
               ,'commission-type': 57 ##для комиссионного
               ,'bank-details-id':21   ###в договорах сша этого нет
               ,'unilateral':1     ###односторонние акты(зависит от фирмы? в основном параметр = 1, в сша  =0
               ,'credit-limit':17  ###(везде 17 ??)   лимит в рублях
               ,'turnover-forecast':17   ### прогнозируемые среднемесячные обороты  (почему 17???)
               ,'commission-categories':''
               ,'sent-dt-checkpassed':1
               ,'is-suspended-checkpassed':1
               ,'is-booked-checkpassed':1
               ,'is-booked-dt':''
               ,'is-faxed-checkpassed':1
               ,'is-signed-checkpassed':1
               ,'firm':7
               }

##    if params['dt'] is not None:
##        changed['dt'] = params['dt']
    may_changed = {
    'manager-code':20453
    ,'non-resident-clients-checkpassed':0  ##  работа с нерезидентами
    ,'non-resident-clients':''
    ,'discount-policy-type':0    ##в основном, тип скидочной политики
    ,'discount-fixed': 0        ##фиксированная скидка
    ,'fixed-discount-pct':''   ##тоже фиксированная скидка?
    ,'contract-discount':''   ##еще фиксированная скидка
    ,'client-limits':''      ##лимиты по клиентам
    ,'brand-clients':''     ##объединенные клиенты        brand-clients=%5B%5D  (в строке с параметрами передается так)
    ,'loyal-clients':''     ##объединенные клиенты
    ,'rit-limit': ''         ##лимит для РИТ (вроде не обязат.)
    ,'autoru-q-plan':''       ##квартальный план для авто.ру (вроде вообще не используется)
    ,'advance-payment-sum':''     ##передается значение, в остальных пусто   (авансовый платеж) - для такси
    ,'wholesale-agent-premium-awards-scale-type':''   ## шкала премий ( нужна для оптового премиального)  в остальных случаях передается 1
    ,'pda-budget':''
    ,'autoru-budget':''
    ,'retro-discount':''
    ,'discount-pct':''
    ,'personal-account-checkpassed':1
    ,'lift-credit-on-payment-checkpassed':1
    ,'auto-credit-checkpassed':1
    ,'personal-account-fictive-checkpassed':1
    ,'repayment-on-consume-checkpassed':1
    }

    data_for_payment_type={}
    if params['payment_type']==2:
        data_for_payment_type={
        'payment-term':''    ###(срок оплаты)  для постоплаты
        ,'credit-type':0        ###для предоплаты 1(по сроку)/2(по сроку и сумме) для постоплаты   (вид кредита)
        ,'credit-currency-limit':980  ###(меняется когда-нибудь? везде рубли)  лимит в валюте
        ,'credit-limit-single':'' ### значение или пусто   (лимит кредита)
        }
##    source_dict[keylist[2]] = params[key]   ## is-signed-dt - real date
##                        mn = {1:'янв',2:'фев',3:'мар',4:'апр',5:'май',6:'июн',7:'июл',8:'авг',9:'сен',10:'окт',11:'ноя',12:'дек'}
##                        ## ...-date - date for display in format: 'DD MON YYYY г.'
##                        source_dict[keylist[1]] = '{0} {1} {2} г.'.format(int(params[key][8:10]), mn[int(params[key][5:7])] , params[key][:4])


    ##always changed
    for x in defaults:
            final_string=final_string+'&'+x
##    print final_string

    main_params='&client_id='+ str(params['client_id'])+'&person_id='+str(params['person_id'])+'&commission='+str(type_con_mapper[type_])+'&payment-type='+str(params['payment_type'])
    serv = '&services=1'
    for x in params['services']:
        serv = serv+'&services-'+str(x)+'='+str(x)

    final_string=final_string+main_params+serv
##    print final_string
##    print params['dt']

    for r in changed:
        final_string = final_string+'&'+str(r)+'='+str(changed[r])
    for r in may_changed:
        final_string = final_string+'&'+str(r)+'='+str(may_changed[r])
    for r in data_for_payment_type:
        final_string = final_string+'&'+str(r)+'='+str(data_for_payment_type[r])
    print final_string


    source_tmp = urlparse.parse_qsl(final_string,True)
    source_dict = {key:value.decode('utf-8') for (key, value) in source_tmp}
    contract = rpc.Balance.CreateContract(passport_uid, source_dict)
    print contract
##    2015-07-01T00%3A00%3A00

##    print final_string


create_con('opt_agency_prem', {'payment_type':2,'client_id':30504255, 'person_id':3825400,'services':[11,7], 'dt':datetime.datetime.now()},'Contract')